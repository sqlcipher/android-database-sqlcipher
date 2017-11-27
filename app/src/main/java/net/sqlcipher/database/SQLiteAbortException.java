/*
 * Copyright (C) 2008 The Android Open Source Project
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

/**
 * An exception that indicates that the SQLite program was aborted.
 * This can happen either through a call to ABORT in a trigger,
 * or as the result of using the ABORT conflict clause.
 */
public class SQLiteAbortException extends SQLiteException {
    public SQLiteAbortException() {}

    public SQLiteAbortException(String error) {
        super(error);
    }
}
