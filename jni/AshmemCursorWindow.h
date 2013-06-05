/*
 * Copyright (C) 2006 The Android Open Source Project
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

#ifndef _ANDROID__DATABASE_NEWWINDOW_H
#define _ANDROID__DATABASE_NEWWINDOW_H

#include <jni.h>

#include <cutils/log.h>
#include <stddef.h>
#include <stdint.h>

#include <binder/Parcel.h>
#include <utils/String8.h>

#if LOG_NDEBUG

#define IF_LOG_WINDOW() if (false)
#define LOG_WINDOW(...)

#else

#define IF_LOG_WINDOW() IF_LOG(LOG_DEBUG, "AshmemCursorWindow")
#define LOG_WINDOW(...) LOG(LOG_DEBUG, "AshmemCursorWindow", __VA_ARGS__)

#endif

using namespace android;

namespace sqlcipher {

/**
 * This class stores a set of rows from a database in a buffer. The begining of the
 * window has first chunk of RowSlots, which are offsets to the row directory, followed by
 * an offset to the next chunk in a linked-list of additional chunk of RowSlots in case
 * the pre-allocated chunk isn't big enough to refer to all rows. Each row directory has a
 * FieldSlot per column, which has the size, offset, and type of the data for that field.
 * Note that the data types come from sqlite3.h.
 *
 * Strings are stored in UTF-8.
 */
class AshmemCursorWindow {
    AshmemCursorWindow(const String8& name, int ashmemFd,
            void* data, size_t size, bool readOnly);

public:
    /* Field types. */
    enum {
        NEW_FIELD_TYPE_NULL = 0,
        NEW_FIELD_TYPE_INTEGER = 1,
        NEW_FIELD_TYPE_FLOAT = 2,
        NEW_FIELD_TYPE_STRING = 3,
        NEW_FIELD_TYPE_BLOB = 4,
    };

    /* Opaque type that describes a field slot. */
    struct FieldSlot {
    private:
        int32_t type;
        union {
            double d;
            int64_t l;
            struct {
                uint32_t offset;
                uint32_t size;
            } buffer;
        } data;

        friend class AshmemCursorWindow;
    } __attribute((packed));

    ~AshmemCursorWindow();

    static status_t create(const String8& name, size_t size, AshmemCursorWindow** outCursorWindow);
    static status_t createFromParcel(Parcel* parcel, AshmemCursorWindow** outCursorWindow);

    status_t writeToParcel(Parcel* parcel);

    inline String8 name() { return mName; }
    inline size_t size() { return mSize; }
    inline size_t freeSpace() { return mSize - mHeader->freeOffset; }
    inline uint32_t getNumRows() { return mHeader->numRows; }
    inline uint32_t getNumColumns() { return mHeader->numColumns; }

    status_t clear();
    status_t setNumColumns(uint32_t numColumns);

    /**
     * Allocate a row slot and its directory.
     * The row is initialized will null entries for each field.
     */
    status_t allocRow();
    status_t freeLastRow();

    status_t putBlob(uint32_t row, uint32_t column, const void* value, size_t size);
    status_t putString(uint32_t row, uint32_t column, const char* value, size_t sizeIncludingNull);
    status_t putLong(uint32_t row, uint32_t column, int64_t value);
    status_t putDouble(uint32_t row, uint32_t column, double value);
    status_t putNull(uint32_t row, uint32_t column);

    /**
     * Gets the field slot at the specified row and column.
     * Returns null if the requested row or column is not in the window.
     */
    FieldSlot* getFieldSlot(uint32_t row, uint32_t column);

    inline int32_t getFieldSlotType(FieldSlot* fieldSlot) {
        return fieldSlot->type;
    }

    inline int64_t getFieldSlotValueLong(FieldSlot* fieldSlot) {
        return fieldSlot->data.l;
    }

    inline double getFieldSlotValueDouble(FieldSlot* fieldSlot) {
        return fieldSlot->data.d;
    }

    inline const char* getFieldSlotValueString(FieldSlot* fieldSlot,
            size_t* outSizeIncludingNull) {
        *outSizeIncludingNull = fieldSlot->data.buffer.size;
        return static_cast<char*>(offsetToPtr(fieldSlot->data.buffer.offset));
    }

    inline const void* getFieldSlotValueBlob(FieldSlot* fieldSlot, size_t* outSize) {
        *outSize = fieldSlot->data.buffer.size;
        return offsetToPtr(fieldSlot->data.buffer.offset);
    }

private:
    static const size_t NEW_ROW_SLOT_CHUNK_NUM_ROWS = 100;

    struct Header {
        // Offset of the lowest unused byte in the window.
        uint32_t freeOffset;

        // Offset of the first row slot chunk.
        uint32_t firstChunkOffset;

        uint32_t numRows;
        uint32_t numColumns;
    };

    struct RowSlot {
        uint32_t offset;
    };

    struct RowSlotChunk {
        RowSlot slots[NEW_ROW_SLOT_CHUNK_NUM_ROWS];
        uint32_t nextChunkOffset;
    };

    String8 mName;
    int mAshmemFd;
    void* mData;
    size_t mSize;
    bool mReadOnly;
    Header* mHeader;

    inline void* offsetToPtr(uint32_t offset) {
        return static_cast<uint8_t*>(mData) + offset;
    }

    inline uint32_t offsetFromPtr(void* ptr) {
        return static_cast<uint8_t*>(ptr) - static_cast<uint8_t*>(mData);
    }

    /**
     * Allocate a portion of the window. Returns the offset
     * of the allocation, or 0 if there isn't enough space.
     * If aligned is true, the allocation gets 4 byte alignment.
     */
    uint32_t alloc(size_t size, bool aligned = false);

    RowSlot* getRowSlot(uint32_t row);
    RowSlot* allocRowSlot();

    status_t putBlobOrString(uint32_t row, uint32_t column,
            const void* value, size_t size, int32_t type);
};

}; // namespace android

#endif
