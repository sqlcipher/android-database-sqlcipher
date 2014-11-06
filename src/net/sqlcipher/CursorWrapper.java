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

package net.sqlcipher;

/**
 * Extension of android.database.CursorWrapper to support getType() for API < 11.
 */
public class CursorWrapper extends android.database.CursorWrapper implements Cursor {

    private final Cursor mCursor;

    public CursorWrapper(Cursor cursor) {
        super(cursor);
        mCursor = cursor;
    }

    public int getType(int columnIndex) {
        return mCursor.getType(columnIndex);
    }

    public Cursor getWrappedCursor() {
      return mCursor;
    }
}

