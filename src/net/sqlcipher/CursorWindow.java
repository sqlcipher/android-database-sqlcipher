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

import android.database.CharArrayBuffer;

import android.content.res.Resources;
import android.database.sqlite.SQLiteClosable;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.util.Log;
import android.util.SparseIntArray;

/**
 * A buffer containing multiple cursor rows.
 */
public class CursorWindow extends android.database.CursorWindow implements Parcelable {
    /** The pointer to the native window class */
    @SuppressWarnings("unused")

    /** The pointer to the native window class. set by the native methods in
     * android_database_CursorWindow.cpp
     */
    private int nWindow;

    private int mStartPos;

    /**
     * Creates a new empty window.
     *
     * @param localWindow true if this window will be used in this process only
     */
    public CursorWindow(boolean localWindow) {
    	super(localWindow);
        mStartPos = 0;
        native_init(localWindow);
    }

    /**
     * Returns the starting position of this window within the entire
     * Cursor's result set.
     *
     * @return the starting position of this window within the entire
     * Cursor's result set.
     */
    public int getStartPosition() {
        return mStartPos;
    }

    /**
     * Set the start position of cursor window
     * @param pos
     */
    public void setStartPosition(int pos) {
        mStartPos = pos;
    }    
 
    /**
     * Returns the number of rows in this window.
     * 
     * @return the number of rows in this window.
     */
    public int getNumRows() {
        acquireReference();
        try {
            return getNumRows_native();
        } finally {
            releaseReference();
        }
    }
    
    private native int getNumRows_native();
    /**
     * Set number of Columns 
     * @param columnNum
     * @return true if success
     */
    public boolean setNumColumns(int columnNum) {
        acquireReference();
        try {
            return setNumColumns_native(columnNum);
        } finally {
            releaseReference();
        }
    }
    
    private native boolean setNumColumns_native(int columnNum);
    
    /**
     * Allocate a row in cursor window
     * @return false if cursor window is out of memory
     */
    public boolean allocRow(){
        acquireReference();
        try {
            return allocRow_native();
        } finally {
            releaseReference();
        }
    }
    
    private native boolean allocRow_native();    
    
    /**
     * Free the last row
     */
    public void freeLastRow(){
        acquireReference();
        try {
            freeLastRow_native();
        } finally {
            releaseReference();
        }
    }
    
    private native void freeLastRow_native();

    /**
     * copy byte array to cursor window
     * @param value
     * @param row
     * @param col
     * @return false if fail to copy
     */
    public boolean putBlob(byte[] value, int row, int col) {
        acquireReference();
        try {
            return putBlob_native(value, row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }
    
    private native boolean putBlob_native(byte[] value, int row, int col);    

    /**
     * Copy String to cursor window
     * @param value
     * @param row
     * @param col
     * @return false if fail to copy
     */
    public boolean putString(String value, int row, int col) {
        acquireReference();
        try {
            return putString_native(value, row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }
    
    private native boolean putString_native(String value, int row, int col);    
    
    /**
     * Copy integer to cursor window
     * @param value
     * @param row
     * @param col
     * @return false if fail to copy
     */
    public boolean putLong(long value, int row, int col) {
        acquireReference();
        try {
            return putLong_native(value, row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }
    
    private native boolean putLong_native(long value, int row, int col);
    

    /**
     * Copy double to cursor window 
     * @param value
     * @param row
     * @param col
     * @return false if fail to copy
     */
    public boolean putDouble(double value, int row, int col) {
        acquireReference();
        try {
            return putDouble_native(value, row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }
    
    private native boolean putDouble_native(double value, int row, int col);    

    /**
     * Set the [row, col] value to NULL
     * @param row
     * @param col
     * @return false if fail to copy
     */
    public boolean putNull(int row, int col) {
        acquireReference();
        try {
            return putNull_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }
    
    private native boolean putNull_native(int row, int col);
    

    /**
     * Returns {@code true} if given field is {@code NULL}.
     * 
     * @param row the row to read from, row - getStartPosition() being the actual row in the window
     * @param col the column to read from
     * @return {@code true} if given field is {@code NULL}
     */
    public boolean isNull(int row, int col) {
        acquireReference();
        try {
            return isNull_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }

    private native boolean isNull_native(int row, int col);

    /**
     * Returns a byte array for the given field.
     *
     * @param row the row to read from, row - getStartPosition() being the actual row in the window
     * @param col the column to read from
     * @return a String value for the given field
     */
    public byte[] getBlob(int row, int col) {
        acquireReference();
        try {
            return getBlob_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }

    /**
     * Returns the value at (<code>row</code>, <code>col</code>) as a <code>byte</code> array.
     *
     * <p>If the value is null, then <code>null</code> is returned. If the
     * type of column <code>col</code> is a string type, then the result
     * is the array of bytes that make up the internal representation of the
     * string value. If the type of column <code>col</code> is integral or floating-point,
     * then an {@link SQLiteException} is thrown.
     */
    private native byte[] getBlob_native(int row, int col);

    /**
     * Returns data type of the given column's value.
     *<p>
     * Returned column types are
     * <ul>
     *   <li>{@link Cursor#FIELD_TYPE_NULL}</li>
     *   <li>{@link Cursor#FIELD_TYPE_INTEGER}</li>
     *   <li>{@link Cursor#FIELD_TYPE_FLOAT}</li>
     *   <li>{@link Cursor#FIELD_TYPE_STRING}</li>
     *   <li>{@link Cursor#FIELD_TYPE_BLOB}</li>
     *</ul>
     *</p>
     *
     * @param row the row to read from, row - getStartPosition() being the actual row in the window
     * @param col the column to read from
     * @return the value type
     */
    public int getType(int row, int col) {
        acquireReference();
        try {
            return getType_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }

    /**
     * Checks if a field contains either a blob or is null.
     *
     * @param row the row to read from, row - getStartPosition() being the actual row in the window
     * @param col the column to read from
     * @return {@code true} if given field is {@code NULL} or a blob
     * @deprecated use {@link #getType(int, int)} instead
     */
    public boolean isBlob(int row, int col) {
        acquireReference();
        try {
            return isBlob_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }

    /**
     * Checks if a field contains a long
     *
     * @param row the row to read from, row - getStartPosition() being the actual row in the window
     * @param col the column to read from
     * @return {@code true} if given field is a long
     * @deprecated use {@link #getType(int, int)} instead
     */
    public boolean isLong(int row, int col) {
        acquireReference();
        try {
            return isInteger_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }

    /**
     * Checks if a field contains a float.
     *
     * @param row the row to read from, row - getStartPosition() being the actual row in the window
     * @param col the column to read from
     * @return {@code true} if given field is a float
     * @deprecated use {@link #getType(int, int)} instead
     */
    public boolean isFloat(int row, int col) {
        acquireReference();
        try {
            return isFloat_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }

    /**
     * Checks if a field contains either a String or is null.
     *
     * @param row the row to read from, row - getStartPosition() being the actual row in the window
     * @param col the column to read from
     * @return {@code true} if given field is {@code NULL} or a String
     * @deprecated use {@link #getType(int, int)} instead
     */
    public boolean isString(int row, int col) {
        acquireReference();
        try {
            return isString_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }

    private native boolean isBlob_native(int row, int col);
    private native boolean isString_native(int row, int col);
    private native boolean isInteger_native(int row, int col);
    private native boolean isFloat_native(int row, int col);

    private native int getType_native(int row, int col);

    /**
     * Returns a String for the given field.
     * 
     * @param row the row to read from, row - getStartPosition() being the actual row in the window 
     * @param col the column to read from
     * @return a String value for the given field
     */
    public String getString(int row, int col) {
        acquireReference();
        try {
            return getString_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }
    
    /**
     * Returns the value at (<code>row</code>, <code>col</code>) as a <code>String</code>.
     *
     * <p>If the value is null, then <code>null</code> is returned. If the
     * type of column <code>col</code> is integral, then the result is the string
     * that is obtained by formatting the integer value with the <code>printf</code>
     * family of functions using format specifier <code>%lld</code>. If the
     * type of column <code>col</code> is floating-point, then the result is the string
     * that is obtained by formatting the floating-point value with the
     * <code>printf</code> family of functions using format specifier <code>%g</code>.
     * If the type of column <code>col</code> is a blob type, then an
     * {@link SQLiteException} is thrown.
     */
    private native String getString_native(int row, int col);

    /**
     * copy the text for the given field in the provided char array.
     * 
     * @param row the row to read from, row - getStartPosition() being the actual row in the window 
     * @param col the column to read from
     * @param buffer the CharArrayBuffer to copy the text into,      
     * If the requested string is larger than the buffer 
     * a new char buffer will be created to hold the string. and assigne to
     * CharArrayBuffer.data
      */
    public void copyStringToBuffer(int row, int col, CharArrayBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("CharArrayBuffer should not be null");
        }
        if (buffer.data == null) {
            buffer.data = new char[64];
        }
        acquireReference();
        try {
            char[] newbuf = copyStringToBuffer_native(
                    row - mStartPos, col, buffer.data.length, buffer);
            if (newbuf != null) {
                buffer.data = newbuf;
            }
        } finally {
            releaseReference();
        }
    }
    
    private native char[] copyStringToBuffer_native(
            int row, int col, int bufferSize, CharArrayBuffer buffer);
    
    /**
     * Returns a long for the given field.
     * row is 0 based
     * 
     * @param row the row to read from, row - getStartPosition() being the actual row in the window 
     * @param col the column to read from
     * @return a long value for the given field
     */
    public long getLong(int row, int col) {
        acquireReference();
        try {
            return getLong_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }
    
    /**
     * Returns the value at (<code>row</code>, <code>col</code>) as a <code>long</code>.
     *
     * <p>If the value is null, then <code>0L</code> is returned. If the
     * type of column <code>col</code> is a string type, then the result
     * is the <code>long</code> that is obtained by parsing the string value with
     * <code>strtoll</code>. If the type of column <code>col</code> is
     * floating-point, then the result is the floating-point value casted to a <code>long</code>.
     * If the type of column <code>col</code> is a blob type, then an
     * {@link SQLiteException} is thrown.
     */
    private native long getLong_native(int row, int col);

    /**
     * Returns a double for the given field.
     * row is 0 based
     * 
     * @param row the row to read from, row - getStartPosition() being the actual row in the window 
     * @param col the column to read from
     * @return a double value for the given field
     */
    public double getDouble(int row, int col) {
        acquireReference();
        try {
            return getDouble_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }
    
    /**
     * Returns the value at (<code>row</code>, <code>col</code>) as a <code>double</code>.
     *
     * <p>If the value is null, then <code>0.0</code> is returned. If the
     * type of column <code>col</code> is a string type, then the result
     * is the <code>double</code> that is obtained by parsing the string value with
     * <code>strtod</code>. If the type of column <code>col</code> is
     * integral, then the result is the integer value casted to a <code>double</code>.
     * If the type of column <code>col</code> is a blob type, then an
     * {@link SQLiteException} is thrown.
     */
    private native double getDouble_native(int row, int col);

    /**
     * Returns a short for the given field.
     * row is 0 based
     * 
     * @param row the row to read from, row - getStartPosition() being the actual row in the window 
     * @param col the column to read from
     * @return a short value for the given field
     */
    public short getShort(int row, int col) {
        acquireReference();
        try {
            return (short) getLong_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }

    /**
     * Returns an int for the given field.
     * 
     * @param row the row to read from, row - getStartPosition() being the actual row in the window 
     * @param col the column to read from
     * @return an int value for the given field
     */
    public int getInt(int row, int col) {
        acquireReference();
        try {
            return (int) getLong_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    }
    
    /**
     * Returns a float for the given field.
     * row is 0 based
     * 
     * @param row the row to read from, row - getStartPosition() being the actual row in the window 
     * @param col the column to read from
     * @return a float value for the given field
     */
    public float getFloat(int row, int col) {
        acquireReference();
        try {
            return (float) getDouble_native(row - mStartPos, col);
        } finally {
            releaseReference();
        }
    } 
    
    /**
     * Clears out the existing contents of the window, making it safe to reuse
     * for new data. Note that the number of columns in the window may NOT
     * change across a call to clear().
     */
    public void clear() {
        acquireReference();
        try {
            mStartPos = 0;        
            native_clear();
        } finally {
            releaseReference();
        }
    }

    /** Clears out the native side of things */
    private native void native_clear();

    /**
     * Cleans up the native resources associated with the window.
     */
    public void close() {
        releaseReference();
    }
    
    private native void close_native();

    @Override
    protected void finalize() {
        // Just in case someone forgot to call close...
        if (nWindow == 0) {
            return;
        }
        close_native();
    }
    
    public static final Parcelable.Creator<CursorWindow> CREATOR
            = new Parcelable.Creator<CursorWindow>() {
        public CursorWindow createFromParcel(Parcel source) {
            return new CursorWindow(source,0);
        }

        public CursorWindow[] newArray(int size) {
            return new CursorWindow[size];
        }
    };

    public static CursorWindow newFromParcel(Parcel p) {
        return CREATOR.createFromParcel(p);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(native_getBinder());
        dest.writeInt(mStartPos);
    }

    public CursorWindow(Parcel source,int foo) {
    	
    	super(true);
    	
        IBinder nativeBinder = source.readStrongBinder();
        mStartPos = source.readInt();

        native_init(nativeBinder);
    }

    /** Get the binder for the native side of the window */
    private native IBinder native_getBinder();

    /** Does the native side initialization for an empty window */
    private native void native_init(boolean localOnly);

    /** Does the native side initialization with an existing binder from another process */
    private native void native_init(IBinder nativeBinder);

    @Override
    protected void onAllReferencesReleased() {
        close_native();

        super.onAllReferencesReleased();
    }
}
