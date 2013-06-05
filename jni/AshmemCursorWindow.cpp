/*
 * Copyright (C) 2006-2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

#undef LOG_TAG
#define LOG_TAG "AshmemCursorWindow"

#include <cutils/log.h>
#include <utils/Errors.h>
#include <utils/String8.h>
#include "AshmemCursorWindow.h"

#include <cutils/ashmem.h>
#include <sys/mman.h>

#include <assert.h>
#include <string.h>
#include <stdlib.h>

using namespace android;

namespace sqlcipher {

AshmemCursorWindow::AshmemCursorWindow(const android::String8& name, int ashmemFd,
        void* data, size_t size, bool readOnly) :
        mName(name), mAshmemFd(ashmemFd), mData(data), mSize(size), mReadOnly(readOnly) {
    mHeader = static_cast<Header*>(mData);
}

AshmemCursorWindow::~AshmemCursorWindow() {
    ::munmap(mData, mSize);
    ::close(mAshmemFd);
}

status_t AshmemCursorWindow::create(const android::String8& name, size_t size, AshmemCursorWindow** outAshmemCursorWindow) {
    android::String8 ashmemName("AshmemCursorWindow: ");
    ashmemName.append(name);

    status_t result;
    int ashmemFd = ashmem_create_region(ashmemName.string(), size);
    if (ashmemFd < 0) {
        result = -errno;
    } else {
        result = ashmem_set_prot_region(ashmemFd, PROT_READ | PROT_WRITE);
        if (result >= 0) {
            void* data = ::mmap(NULL, size, PROT_READ | PROT_WRITE, MAP_SHARED, ashmemFd, 0);
            if (data == MAP_FAILED) {
                result = -errno;
            } else {
                result = ashmem_set_prot_region(ashmemFd, PROT_READ);
                if (result >= 0) {
                    AshmemCursorWindow* window = new AshmemCursorWindow(name, ashmemFd,
                            data, size, false /*readOnly*/);
                    result = window->clear();
                    if (!result) {
                        LOG_WINDOW("Created new AshmemCursorWindow: freeOffset=%d, "
                                "numRows=%d, numColumns=%d, mSize=%d, mData=%p",
                                window->mHeader->freeOffset,
                                window->mHeader->numRows,
                                window->mHeader->numColumns,
                                window->mSize, window->mData);
                        *outAshmemCursorWindow = window;
                        return OK;
                    }
                    delete window;
                }
            }
            ::munmap(data, size);
        }
        ::close(ashmemFd);
    }
    *outAshmemCursorWindow = NULL;
    return result;
}

status_t AshmemCursorWindow::createFromParcel(Parcel* parcel, AshmemCursorWindow** outCursorWindow) {
    android::String8 name = parcel->readString8();

    status_t result;
    int ashmemFd = parcel->readFileDescriptor();
    if (ashmemFd == int(BAD_TYPE)) {
        result = BAD_TYPE;
    } else {
        ssize_t size = ashmem_get_size_region(ashmemFd);
        if (size < 0) {
            result = UNKNOWN_ERROR;
        } else {
            int dupAshmemFd = ::dup(ashmemFd);
            if (dupAshmemFd < 0) {
                result = -errno;
            } else {
                void* data = ::mmap(NULL, size, PROT_READ, MAP_SHARED, dupAshmemFd, 0);
                if (data == MAP_FAILED) {
                    result = -errno;
                } else {
                    AshmemCursorWindow* window = new AshmemCursorWindow(name, dupAshmemFd,
                            data, size, true /*readOnly*/);
                    LOG_WINDOW("Created AshmemCursorWindow from parcel: freeOffset=%d, "
                            "numRows=%d, numColumns=%d, mSize=%d, mData=%p",
                            window->mHeader->freeOffset,
                            window->mHeader->numRows,
                            window->mHeader->numColumns,
                            window->mSize, window->mData);
                    *outCursorWindow = window;
                    return OK;
                }
                ::close(dupAshmemFd);
            }
        }
    }
    *outCursorWindow = NULL;
    return result;
}

status_t AshmemCursorWindow::writeToParcel(Parcel* parcel) {
    status_t status = parcel->writeString8(mName);
    if (!status) {
        status = parcel->writeDupFileDescriptor(mAshmemFd);
    }
    return status;
}

status_t AshmemCursorWindow::clear() {
    if (mReadOnly) {
        return INVALID_OPERATION;
    }

    mHeader->freeOffset = sizeof(Header) + sizeof(RowSlotChunk);
    mHeader->firstChunkOffset = sizeof(Header);
    mHeader->numRows = 0;
    mHeader->numColumns = 0;

    RowSlotChunk* firstChunk = static_cast<RowSlotChunk*>(offsetToPtr(mHeader->firstChunkOffset));
    firstChunk->nextChunkOffset = 0;
    return OK;
}

status_t AshmemCursorWindow::setNumColumns(uint32_t numColumns) {
    if (mReadOnly) {
        return INVALID_OPERATION;
    }

    uint32_t cur = mHeader->numColumns;
    if ((cur > 0 || mHeader->numRows > 0) && cur != numColumns) {
        LOG_WINDOW("Trying to go from %d columns to %d", cur, numColumns);
        return INVALID_OPERATION;
    }
    mHeader->numColumns = numColumns;
    return OK;
}

status_t AshmemCursorWindow::allocRow() {
    if (mReadOnly) {
        return INVALID_OPERATION;
    }

    // Fill in the row slot
    RowSlot* rowSlot = allocRowSlot();
    if (rowSlot == NULL) {
        return NO_MEMORY;
    }

    // Allocate the slots for the field directory
    size_t fieldDirSize = mHeader->numColumns * sizeof(FieldSlot);
    uint32_t fieldDirOffset = alloc(fieldDirSize, true /*aligned*/);
    if (!fieldDirOffset) {
        mHeader->numRows--;
        LOG_WINDOW("The row failed, so back out the new row accounting "
                "from allocRowSlot %d", mHeader->numRows);
        return NO_MEMORY;
    }
    FieldSlot* fieldDir = static_cast<FieldSlot*>(offsetToPtr(fieldDirOffset));
    memset(fieldDir, 0, fieldDirSize);

    LOG_WINDOW("Allocated row %u, rowSlot is at offset %u, fieldDir is %d bytes at offset %u\n",
            mHeader->numRows - 1, offsetFromPtr(rowSlot), fieldDirSize, fieldDirOffset);
    rowSlot->offset = fieldDirOffset;
    return OK;
}

status_t AshmemCursorWindow::freeLastRow() {
    if (mReadOnly) {
        return INVALID_OPERATION;
    }

    if (mHeader->numRows > 0) {
        mHeader->numRows--;
    }
    return OK;
}

uint32_t AshmemCursorWindow::alloc(size_t size, bool aligned) {
    uint32_t padding;
    if (aligned) {
        // 4 byte alignment
        padding = (~mHeader->freeOffset + 1) & 3;
    } else {
        padding = 0;
    }

    uint32_t offset = mHeader->freeOffset + padding;
    uint32_t nextFreeOffset = offset + size;
    if (nextFreeOffset > mSize) {
        LOG_WINDOW("Window is full: requested allocation %d bytes, "
                "free space %d bytes, window size %d bytes",
                size, freeSpace(), mSize);
        return 0;
    }

    mHeader->freeOffset = nextFreeOffset;
    return offset;
}

AshmemCursorWindow::RowSlot* AshmemCursorWindow::getRowSlot(uint32_t row) {
    uint32_t chunkPos = row;
    RowSlotChunk* chunk = static_cast<RowSlotChunk*>(
            offsetToPtr(mHeader->firstChunkOffset));
    while (chunkPos >= NEW_ROW_SLOT_CHUNK_NUM_ROWS) {
        chunk = static_cast<RowSlotChunk*>(offsetToPtr(chunk->nextChunkOffset));
        chunkPos -= NEW_ROW_SLOT_CHUNK_NUM_ROWS;
    }
    return &chunk->slots[chunkPos];
}

AshmemCursorWindow::RowSlot* AshmemCursorWindow::allocRowSlot() {
    uint32_t chunkPos = mHeader->numRows;
    RowSlotChunk* chunk = static_cast<RowSlotChunk*>(
            offsetToPtr(mHeader->firstChunkOffset));
    while (chunkPos > NEW_ROW_SLOT_CHUNK_NUM_ROWS) {
        chunk = static_cast<RowSlotChunk*>(offsetToPtr(chunk->nextChunkOffset));
        chunkPos -= NEW_ROW_SLOT_CHUNK_NUM_ROWS;
    }
    if (chunkPos == NEW_ROW_SLOT_CHUNK_NUM_ROWS) {
        if (!chunk->nextChunkOffset) {
            chunk->nextChunkOffset = alloc(sizeof(RowSlotChunk), true /*aligned*/);
            if (!chunk->nextChunkOffset) {
                return NULL;
            }
        }
        chunk = static_cast<RowSlotChunk*>(offsetToPtr(chunk->nextChunkOffset));
        chunk->nextChunkOffset = 0;
        chunkPos = 0;
    }
    mHeader->numRows += 1;
    return &chunk->slots[chunkPos];
}

AshmemCursorWindow::FieldSlot* AshmemCursorWindow::getFieldSlot(uint32_t row, uint32_t column) {
    if (row >= mHeader->numRows || column >= mHeader->numColumns) {
        LOG_WINDOW("Failed to read row %d, column %d from a AshmemCursorWindow which "
                "has %d rows, %d columns.",
                row, column, mHeader->numRows, mHeader->numColumns);
        return NULL;
    }
    RowSlot* rowSlot = getRowSlot(row);
    if (!rowSlot) {
        LOG_WINDOW("Failed to find rowSlot for row %d.", row);
        return NULL;
    }
    FieldSlot* fieldDir = static_cast<FieldSlot*>(offsetToPtr(rowSlot->offset));
    return &fieldDir[column];
}

status_t AshmemCursorWindow::putBlob(uint32_t row, uint32_t column, const void* value, size_t size) {
    return putBlobOrString(row, column, value, size, NEW_FIELD_TYPE_BLOB);
}

status_t AshmemCursorWindow::putString(uint32_t row, uint32_t column, const char* value,
        size_t sizeIncludingNull) {
    return putBlobOrString(row, column, value, sizeIncludingNull, NEW_FIELD_TYPE_STRING);
}

status_t AshmemCursorWindow::putBlobOrString(uint32_t row, uint32_t column,
        const void* value, size_t size, int32_t type) {
    if (mReadOnly) {
        return INVALID_OPERATION;
    }

    FieldSlot* fieldSlot = getFieldSlot(row, column);
    if (!fieldSlot) {
        return BAD_VALUE;
    }

    uint32_t offset = alloc(size);
    if (!offset) {
        return NO_MEMORY;
    }

    memcpy(offsetToPtr(offset), value, size);

    fieldSlot->type = type;
    fieldSlot->data.buffer.offset = offset;
    fieldSlot->data.buffer.size = size;
    return OK;
}

status_t AshmemCursorWindow::putLong(uint32_t row, uint32_t column, int64_t value) {
    if (mReadOnly) {
        return INVALID_OPERATION;
    }

    FieldSlot* fieldSlot = getFieldSlot(row, column);
    if (!fieldSlot) {
        return BAD_VALUE;
    }

    fieldSlot->type = NEW_FIELD_TYPE_INTEGER;
    fieldSlot->data.l = value;
    return OK;
}

status_t AshmemCursorWindow::putDouble(uint32_t row, uint32_t column, double value) {
    if (mReadOnly) {
        return INVALID_OPERATION;
    }

    FieldSlot* fieldSlot = getFieldSlot(row, column);
    if (!fieldSlot) {
        return BAD_VALUE;
    }

    fieldSlot->type = NEW_FIELD_TYPE_FLOAT;
    fieldSlot->data.d = value;
    return OK;
}

status_t AshmemCursorWindow::putNull(uint32_t row, uint32_t column) {
    if (mReadOnly) {
        return INVALID_OPERATION;
    }

    FieldSlot* fieldSlot = getFieldSlot(row, column);
    if (!fieldSlot) {
        return BAD_VALUE;
    }

    fieldSlot->type = NEW_FIELD_TYPE_NULL;
    fieldSlot->data.buffer.offset = 0;
    fieldSlot->data.buffer.size = 0;
    return OK;
}

}; // namespace android
