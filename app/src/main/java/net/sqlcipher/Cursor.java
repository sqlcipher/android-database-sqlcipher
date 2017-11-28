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
 * Extension of android.database.Cursor to support getType() for API < 11.
 */
public interface Cursor extends android.database.Cursor {
    /*
     * Values returned by {@link #getType(int)}.
     * These should be consistent with the corresponding types defined in CursorWindow.h
     */
    /** Value returned by {@link #getType(int)} if the specified column is null */
    static final int FIELD_TYPE_NULL = 0;

    /** Value returned by {@link #getType(int)} if the specified  column type is integer */
    static final int FIELD_TYPE_INTEGER = 1;

    /** Value returned by {@link #getType(int)} if the specified column type is float */
    static final int FIELD_TYPE_FLOAT = 2;

    /** Value returned by {@link #getType(int)} if the specified column type is string */
    static final int FIELD_TYPE_STRING = 3;

    /** Value returned by {@link #getType(int)} if the specified column type is blob */
    static final int FIELD_TYPE_BLOB = 4;

    /**
     * Returns data type of the given column's value.
     * The preferred type of the column is returned but the data may be converted to other types
     * as documented in the get-type methods such as {@link #getInt(int)}, {@link #getFloat(int)}
     * etc.
     *<p>
     * Returned column types are
     * <ul>
     *   <li>{@link #FIELD_TYPE_NULL}</li>
     *   <li>{@link #FIELD_TYPE_INTEGER}</li>
     *   <li>{@link #FIELD_TYPE_FLOAT}</li>
     *   <li>{@link #FIELD_TYPE_STRING}</li>
     *   <li>{@link #FIELD_TYPE_BLOB}</li>
     *</ul>
     *</p>
     *
     * @param columnIndex the zero-based index of the target column.
     * @return column value type
     */
    int getType(int columnIndex);
}
