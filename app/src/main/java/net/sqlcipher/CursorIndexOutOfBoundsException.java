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
 * An exception indicating that a cursor is out of bounds.
 */
public class CursorIndexOutOfBoundsException extends IndexOutOfBoundsException {

    public CursorIndexOutOfBoundsException(int index, int size) {
        super("Index " + index + " requested, with a size of " + size);
    }

    public CursorIndexOutOfBoundsException(String message) {
        super(message);
    }
}
