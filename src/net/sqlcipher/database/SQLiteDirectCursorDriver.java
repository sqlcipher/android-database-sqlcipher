/*
 * Copyright (C) 2007 The Android Open Source Project
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

package net.sqlcipher.database;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase.CursorFactory;

/**
 * A cursor driver that uses the given query directly.
 * 
 * @hide
 */
public class SQLiteDirectCursorDriver implements SQLiteCursorDriver {
    private String mEditTable; 
    private SQLiteDatabase mDatabase;
    private Cursor mCursor;
    private String mSql;
    private SQLiteQuery mQuery;

    public SQLiteDirectCursorDriver(SQLiteDatabase db, String sql, String editTable) {
        mDatabase = db;
        mEditTable = editTable;
        mSql = sql;
    }

    public Cursor query(CursorFactory factory, String[] selectionArgs) {
        // Compile the query
        SQLiteQuery query = new SQLiteQuery(mDatabase, mSql, 0, selectionArgs);

        try {
            // Arg binding
            int numArgs = selectionArgs == null ? 0 : selectionArgs.length;
            for (int i = 0; i < numArgs; i++) {
                query.bindString(i + 1, selectionArgs[i]);
            }

            // Create the cursor
            if (factory == null) {
                mCursor = new SQLiteCursor(mDatabase, this, mEditTable, query);
                
            } else {
                mCursor = factory.newCursor(mDatabase, this, mEditTable, query);
            }

            mQuery = query;
            query = null;
            return mCursor;
        } finally {
            // Make sure this object is cleaned up if something happens
            if (query != null) query.close();
        }
    }

    public void cursorClosed() {
        mCursor = null;
    }

    public void setBindArguments(String[] bindArgs) {
        final int numArgs = bindArgs.length;
        for (int i = 0; i < numArgs; i++) {
            mQuery.bindString(i + 1, bindArgs[i]);
        }
    }

    @Override
    public void cursorDeactivated() {
        // Do nothing
    }

    @Override
    public void cursorRequeried(android.database.Cursor cursor) {
        // Do nothing
    }

    @Override
    public String toString() {
        return "SQLiteDirectCursorDriver: " + mSql;
    }
}
