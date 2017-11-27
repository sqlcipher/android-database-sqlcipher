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
 * An exception that indicates that the SQLite program is done.
 * Thrown when an operation that expects a row (such as {@link
 * SQLiteStatement#simpleQueryForString} or {@link
 * SQLiteStatement#simpleQueryForLong}) does not get one.
 */
public class SQLiteDoneException extends SQLiteException {
    public SQLiteDoneException() {}

    public SQLiteDoneException(String error) {
        super(error);
    }
}
