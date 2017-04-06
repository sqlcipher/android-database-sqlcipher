/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.io.File;
import java.util.List;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import android.util.Log;
import android.util.Pair;

/**
 * Default class used to define the actions to take when the database corruption is reported
 * by sqlite.
 * <p>
 * If null is specified for DatabaeErrorHandler param in the above calls, then this class is used
 * as the default {@link DatabaseErrorHandler}.
 */
public final class DefaultDatabaseErrorHandler implements DatabaseErrorHandler {

    private final String TAG = getClass().getSimpleName();

    /**
     * defines the default method to be invoked when database corruption is detected.
     * @param dbObj the {@link SQLiteDatabase} object representing the database on which corruption
     * is detected.
     */
    public void onCorruption(SQLiteDatabase dbObj) {
        // NOTE: Unlike the AOSP, this version does NOT attempt to delete any attached databases.
        // TBD: Are we really certain that the attached databases would really be corrupt?
        Log.e(TAG, "Corruption reported by sqlite on database, deleting: " + dbObj.getPath());

        if (dbObj.isOpen()) {
            Log.e(TAG, "Database object for corrupted database is already open, closing");

            try {
                dbObj.close();
            } catch (Exception e) {
                /* ignored */
                Log.e(TAG, "Exception closing Database object for corrupted database, ignored", e);
            }
        }

        deleteDatabaseFile(dbObj.getPath());
    }

    private void deleteDatabaseFile(String fileName) {
        if (fileName.equalsIgnoreCase(":memory:") || fileName.trim().length() == 0) {
            return;
        }
        Log.e(TAG, "deleting the database file: " + fileName);
        try {
            new File(fileName).delete();
        } catch (Exception e) {
            /* print warning and ignore exception */
            Log.w(TAG, "delete failed: " + e.getMessage());
        }
    }
}
