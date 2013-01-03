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

import net.sqlcipher.database.SQLiteDatabase.CursorFactory;
import net.sqlcipher.*;

/**
 * A driver for SQLiteCursors that is used to create them and gets notified
 * by the cursors it creates on significant events in their lifetimes.
 */
public interface SQLiteCursorDriver {
    /**
     * Executes the query returning a Cursor over the result set.
     * 
     * @param factory The CursorFactory to use when creating the Cursors, or
     *         null if standard SQLiteCursors should be returned.
     * @return a Cursor over the result set
     */
    Cursor query(CursorFactory factory, String[] bindArgs);

    /**
     * Called by a SQLiteCursor when it is released.
     */
    void cursorDeactivated();

    /**
     * Called by a SQLiteCursor when it is requeryed.
     * 
     * @return The new count value.
     */
    void cursorRequeried(android.database.Cursor cursor);

    /**
     * Called by a SQLiteCursor when it it closed to destroy this object as well.
     */
    void cursorClosed();

    /**
     * Set new bind arguments. These will take effect in cursorRequeried().
     * @param bindArgs the new arguments
     */
    public void setBindArguments(String[] bindArgs);
}
