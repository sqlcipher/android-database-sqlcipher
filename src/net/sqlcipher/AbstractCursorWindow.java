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

import android.os.Parcelable;

/**
 * A buffer containing multiple cursor rows.
 */
public abstract class AbstractCursorWindow extends android.database.CursorWindow implements Parcelable {
    public static int BINDER_CURSOR_WINDOW = 1;
    public static int ASHMEM_CURSOR_WINDOW = 1;

    /**
     * Creates a new empty window.
     *
     * @param localWindow true if this window will be used in this process only
     */
    public AbstractCursorWindow(boolean localWindow) {
        super(localWindow);
    }

    abstract public int getType(int row, int column);
    abstract public int getCursorWindowType();
}
