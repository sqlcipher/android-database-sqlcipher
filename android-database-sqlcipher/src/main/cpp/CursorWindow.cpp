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
#define LOG_TAG "CursorWindow"

#include <assert.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <jni.h>
#include "CursorWindow.h"

namespace sqlcipher {

CursorWindow::CursorWindow(size_t initialSize, size_t growthPaddingSize, size_t maxSize)
{
  mInitialSize = initialSize;
  mGrowthPaddingSize = growthPaddingSize;
  mMaxSize = maxSize;
  LOG_WINDOW("CursorWindow::CursorWindow initialSize:%d growBySize:%d maxSize:%d\n",
             initialSize, growthPaddingSize, maxSize);
}

bool CursorWindow::initBuffer(bool localOnly)
{
  void* data = malloc(mInitialSize);
  if(data){
    mData = (uint8_t *) data;
    mHeader = (window_header_t *) mData;
    mSize = mInitialSize;
    clear();
    LOG_WINDOW("Created CursorWindow with new MemoryDealer: mFreeOffset = %d, mSize = %d, mInitialSize = %d, mGrowthPaddingSize = %d, mMaxSize = %d, mData = %p\n",
               mFreeOffset, mSize, mInitialSize, mGrowthPaddingSize, mMaxSize, mData);
    return true;
  }
  return false;
}

CursorWindow::~CursorWindow()
{
  if(mData){
    free(mData);
  }
}

void CursorWindow::clear()
{
    mHeader->numRows = 0;
    mHeader->numColumns = 0;
    mFreeOffset = sizeof(window_header_t) + ROW_SLOT_CHUNK_SIZE;
    // Mark the first chunk's next 'pointer' as null
    *((uint32_t *)(mData + mFreeOffset - sizeof(uint32_t))) = 0;
    mChunkNumToNextChunkOffset.clear();
    mLastChunkPtrOffset = 0;
}

int32_t CursorWindow::freeSpace()
{
    int32_t freeSpace = mSize - mFreeOffset;
    if (freeSpace < 0) {
        freeSpace = 0;
    }
    return freeSpace;
}

field_slot_t * CursorWindow::allocRow()
{
    // Fill in the row slot
    row_slot_t * rowSlot = allocRowSlot();
    if (rowSlot == NULL) {
        return NULL;
    }

    // Record the original offset of the rowSlot prior to allocation of the field directory
    uint32_t rowSlotOffset = (uint8_t*)rowSlot - mData;

    // Allocate the slots for the field directory
    size_t fieldDirSize = mHeader->numColumns * sizeof(field_slot_t);
    uint32_t fieldDirOffset = alloc(fieldDirSize);
    if (!fieldDirOffset) {
        mHeader->numRows--;
        LOGE("The row failed, so back out the new row accounting from allocRowSlot %d", mHeader->numRows);
        return NULL;
    }
    field_slot_t * fieldDir = (field_slot_t *)offsetToPtr(fieldDirOffset);
    memset(fieldDir, 0x0, fieldDirSize);

    // Reset the rowSlot pointer relative to mData
    // If the last alloc relocated mData this will be rowSlot's new address, otherwise the value will not change
    rowSlot = (row_slot_t*)(mData + rowSlotOffset);

    LOG_WINDOW("Allocated row %u, rowSlot is at offset %u, fieldDir is %d bytes at offset %u\n", (mHeader->numRows - 1), ((uint8_t *)rowSlot) - mData, fieldDirSize, fieldDirOffset);
    rowSlot->offset = fieldDirOffset;

    return fieldDir;
}

uint32_t CursorWindow::alloc(size_t requestedSize, bool aligned)
{
    size_t size = 0, new_allocation_sz = 0;
    uint32_t padding;
    void *tempData = NULL;
    if (aligned) {
        // 4 byte alignment
        padding = 4 - (mFreeOffset & 0x3);
    } else {
        padding = 0;
    }
    size = requestedSize + padding;
    if (size > freeSpace()) {
      new_allocation_sz = mSize + size - freeSpace() + mGrowthPaddingSize;
      LOGE("need to grow: mSize = %d, size = %d, freeSpace() = %d, numRows = %d new_allocation_sz:%d\n",
           mSize, size, freeSpace(), mHeader->numRows, new_allocation_sz);
      if(mMaxSize == 0 || new_allocation_sz <= mMaxSize) {
        tempData = realloc((void *)mData, new_allocation_sz);
        if(tempData == NULL) return 0;
        mData = (uint8_t *)tempData;
        mHeader = (window_header_t *)mData;
        LOGE("allocation grew to:%d", new_allocation_sz);
        mSize = new_allocation_sz;
      } else {
        return 0;
      }
    }
    uint32_t offset = mFreeOffset + padding;
    mFreeOffset += size;
    return offset;
}

row_slot_t * CursorWindow::getRowSlot(int row)
{
  LOG_WINDOW("getRowSlot entered: requesting row:%d, current row num:%d", row, mHeader->numRows);
    unordered_map<int, uint32_t>::iterator result;
    int chunkNum = row / ROW_SLOT_CHUNK_NUM_ROWS;
    int chunkPos = row % ROW_SLOT_CHUNK_NUM_ROWS;
    int chunkPtrOffset = sizeof(window_header_t) + ROW_SLOT_CHUNK_SIZE - sizeof(uint32_t);
    uint8_t * rowChunk = mData + sizeof(window_header_t);

    // check for chunkNum in cache
    result = mChunkNumToNextChunkOffset.find(chunkNum);
    if(result != mChunkNumToNextChunkOffset.end()){
      rowChunk = offsetToPtr(result->second);
      LOG_WINDOW("Retrieved chunk offset from cache for row:%d", row);
      return (row_slot_t *)(rowChunk + (chunkPos * sizeof(row_slot_t)));
    }

    // walk the list, this shouldn't occur
    LOG_WINDOW("getRowSlot walking list %d times to find rowslot for row:%d", chunkNum, row);
    for (int i = 0; i < chunkNum; i++) {
        rowChunk = offsetToPtr(*((uint32_t *)(mData + chunkPtrOffset)));
        chunkPtrOffset = rowChunk - mData + (ROW_SLOT_CHUNK_NUM_ROWS * sizeof(row_slot_t));
    }
    return (row_slot_t *)(rowChunk + (chunkPos * sizeof(row_slot_t)));
    LOG_WINDOW("exit getRowSlot current row num %d, this row %d", mHeader->numRows, row);
}

row_slot_t * CursorWindow::allocRowSlot()
{
    int chunkNum = mHeader->numRows / ROW_SLOT_CHUNK_NUM_ROWS;
    int chunkPos = mHeader->numRows % ROW_SLOT_CHUNK_NUM_ROWS;
    int chunkPtrOffset = sizeof(window_header_t) + ROW_SLOT_CHUNK_SIZE - sizeof(uint32_t);
    uint8_t * rowChunk = mData + sizeof(window_header_t);
    LOG_WINDOW("allocRowSlot entered: Allocating row slot, mHeader->numRows is %d, chunkNum is %d, chunkPos is %d",
           mHeader->numRows, chunkNum, chunkPos);

    if(mLastChunkPtrOffset != 0){
      chunkPtrOffset = mLastChunkPtrOffset;
    }
    if(chunkNum > 0) {
        uint32_t nextChunkOffset = *((uint32_t *)(mData + chunkPtrOffset));
        LOG_WINDOW("nextChunkOffset is %d", nextChunkOffset);
        if (nextChunkOffset == 0) {
            mLastChunkPtrOffset = chunkPtrOffset;
            // Allocate a new row chunk
            nextChunkOffset = alloc(ROW_SLOT_CHUNK_SIZE, true);
            mChunkNumToNextChunkOffset.insert(make_pair(chunkNum, nextChunkOffset));
            if (nextChunkOffset == 0) {
                return NULL;
            }
            rowChunk = offsetToPtr(nextChunkOffset);
            LOG_WINDOW("allocated new chunk at %d, rowChunk = %p", nextChunkOffset, rowChunk);
            *((uint32_t *)(mData + chunkPtrOffset)) = rowChunk - mData;
            // Mark the new chunk's next 'pointer' as null
            *((uint32_t *)(rowChunk + ROW_SLOT_CHUNK_SIZE - sizeof(uint32_t))) = 0;
        } else {
          LOG_WINDOW("follwing 'pointer' to next chunk, offset of next pointer is %d", chunkPtrOffset);
            rowChunk = offsetToPtr(nextChunkOffset);
            chunkPtrOffset = rowChunk - mData + (ROW_SLOT_CHUNK_NUM_ROWS * sizeof(row_slot_t));
            if(chunkPos == ROW_SLOT_CHUNK_NUM_ROWS - 1){
              // prepare to allocate new rowslot_t now at end of row
              mLastChunkPtrOffset = chunkPtrOffset;
            }
        }
    }
    mHeader->numRows++;
    return (row_slot_t *)(rowChunk + (chunkPos * sizeof(row_slot_t)));
}

field_slot_t * CursorWindow::getFieldSlotWithCheck(int row, int column)
{
  LOG_WINDOW("getFieldSlotWithCheck entered: row:%d column:%d", row, column);
  if (row < 0 || row >= mHeader->numRows || column < 0 || column >= mHeader->numColumns) {
      LOGE("Bad request for field slot %d,%d. numRows = %d, numColumns = %d", row, column, mHeader->numRows, mHeader->numColumns);
      return NULL;
  }
  row_slot_t * rowSlot = getRowSlot(row);
  if (!rowSlot) {
      LOGE("Failed to find rowSlot for row %d", row);
      return NULL;
  }
  if (rowSlot->offset == 0 || rowSlot->offset >= mSize) {
      LOGE("Invalid rowSlot, offset = %d", rowSlot->offset);
      return NULL;
  }
  int fieldDirOffset = rowSlot->offset;
  return ((field_slot_t *)offsetToPtr(fieldDirOffset)) + column;
}

uint32_t CursorWindow::read_field_slot(int row, int column, field_slot_t * slotOut)
{
    LOG_WINDOW("read_field_slot entered: row:%d, column:%d, slotOut:%p", row, column, slotOut);
    if (row < 0 || row >= mHeader->numRows || column < 0 || column >= mHeader->numColumns) {
        LOGE("Bad request for field slot %d,%d. numRows = %d, numColumns = %d", row, column, mHeader->numRows, mHeader->numColumns);
        return -1;
    }
    row_slot_t * rowSlot = getRowSlot(row);
    if (!rowSlot) {
        LOGE("Failed to find rowSlot for row %d", row);
        return -1;
    }
    if (rowSlot->offset == 0 || rowSlot->offset >= mSize) {
        LOGE("Invalid rowSlot, offset = %d", rowSlot->offset);
        return -1;
    }
    LOG_WINDOW("Found field directory for %d,%d at rowSlot %d, offset %d", row, column, (uint8_t *)rowSlot - mData, rowSlot->offset);
    field_slot_t * fieldDir = (field_slot_t *)offsetToPtr(rowSlot->offset);
    LOG_WINDOW("Read field_slot_t %d,%d: offset = %d, size = %d, type = %d", row, column, fieldDir[column].data.buffer.offset, fieldDir[column].data.buffer.size, fieldDir[column].type);

    // Copy the data to the out param
    slotOut->data.buffer.offset = fieldDir[column].data.buffer.offset;
    slotOut->data.buffer.size = fieldDir[column].data.buffer.size;
    slotOut->type = fieldDir[column].type;
    return 0;
}

void CursorWindow::copyIn(uint32_t offset, uint8_t const * data, size_t size)
{
    assert(offset + size <= mSize);
    memcpy(mData + offset, data, size);
}

void CursorWindow::copyIn(uint32_t offset, int64_t data)
{
    assert(offset + sizeof(int64_t) <= mSize);
    memcpy(mData + offset, (uint8_t *)&data, sizeof(int64_t));
}

void CursorWindow::copyIn(uint32_t offset, double data)
{
    assert(offset + sizeof(double) <= mSize);
    memcpy(mData + offset, (uint8_t *)&data, sizeof(double));
}

void CursorWindow::copyOut(uint32_t offset, uint8_t * data, size_t size)
{
    assert(offset + size <= mSize);
    memcpy(data, mData + offset, size);
}

int64_t CursorWindow::copyOutLong(uint32_t offset)
{
    int64_t value;
    assert(offset + sizeof(int64_t) <= mSize);
    memcpy(&value, mData + offset, sizeof(int64_t));
    return value;
}

double CursorWindow::copyOutDouble(uint32_t offset)
{
    double value;
    assert(offset + sizeof(double) <= mSize);
    memcpy(&value, mData + offset, sizeof(double));
    return value;
}

bool CursorWindow::putLong(unsigned int row, unsigned int col, int64_t value)
{
    field_slot_t * fieldSlot = getFieldSlotWithCheck(row, col);
    if (!fieldSlot) {
        return false;
    }

#if WINDOW_STORAGE_INLINE_NUMERICS
    fieldSlot->data.l = value;
#else
    int offset = alloc(sizeof(int64_t));
    if (!offset) {
        return false;
    }

    copyIn(offset, value);

    fieldSlot->data.buffer.offset = offset;
    fieldSlot->data.buffer.size = sizeof(int64_t);
#endif
    fieldSlot->type = FIELD_TYPE_INTEGER;
    return true;
}

bool CursorWindow::putDouble(unsigned int row, unsigned int col, double value)
{
    field_slot_t * fieldSlot = getFieldSlotWithCheck(row, col);
    if (!fieldSlot) {
        return false;
    }

#if WINDOW_STORAGE_INLINE_NUMERICS
    fieldSlot->data.d = value;
#else
    int offset = alloc(sizeof(int64_t));
    if (!offset) {
        return false;
    }

    copyIn(offset, value);

    fieldSlot->data.buffer.offset = offset;
    fieldSlot->data.buffer.size = sizeof(double);
#endif
    fieldSlot->type = FIELD_TYPE_FLOAT;
    return true;
}

bool CursorWindow::putNull(unsigned int row, unsigned int col)
{
    field_slot_t * fieldSlot = getFieldSlotWithCheck(row, col);
    if (!fieldSlot) {
        return false;
    }

    fieldSlot->type = FIELD_TYPE_NULL;
    fieldSlot->data.buffer.offset = 0;
    fieldSlot->data.buffer.size = 0;
    return true;
}

bool CursorWindow::getLong(unsigned int row, unsigned int col, int64_t * valueOut)
{
    field_slot_t * fieldSlot = getFieldSlotWithCheck(row, col);
    if (!fieldSlot || fieldSlot->type != FIELD_TYPE_INTEGER) {
        return false;
    }

#if WINDOW_STORAGE_INLINE_NUMERICS
    *valueOut = fieldSlot->data.l;
#else
    *valueOut = copyOutLong(fieldSlot->data.buffer.offset);
#endif
    return true;
}

bool CursorWindow::getDouble(unsigned int row, unsigned int col, double * valueOut)
{
    field_slot_t * fieldSlot = getFieldSlotWithCheck(row, col);
    if (!fieldSlot || fieldSlot->type != FIELD_TYPE_FLOAT) {
        return false;
    }

#if WINDOW_STORAGE_INLINE_NUMERICS
    *valueOut = fieldSlot->data.d;
#else
    *valueOut = copyOutDouble(fieldSlot->data.buffer.offset);
#endif
    return true;
}

bool CursorWindow::getNull(unsigned int row, unsigned int col, bool * valueOut)
{
    field_slot_t * fieldSlot = getFieldSlotWithCheck(row, col);
    if (!fieldSlot) {
        return false;
    }

    if (fieldSlot->type != FIELD_TYPE_NULL) {
        *valueOut = false;
    } else {
        *valueOut = true;
    }
    return true;
}

}; // namespace sqlcipher
