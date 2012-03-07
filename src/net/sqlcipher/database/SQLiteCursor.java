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

package net.sqlcipher.database;

import net.sqlcipher.AbstractWindowedCursor;
import net.sqlcipher.CursorWindow;
import net.sqlcipher.SQLException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import android.database.CharArrayBuffer;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;

/**
 * A Cursor implementation that exposes results from a query on a
 * {@link SQLiteDatabase}.
 *
 * SQLiteCursor is not internally synchronized so code using a SQLiteCursor from multiple
 * threads should perform its own synchronization when using the SQLiteCursor.
 */
public class SQLiteCursor extends AbstractWindowedCursor {
    static final String TAG = "Cursor";
    static final int NO_COUNT = -1;

    /** The name of the table to edit */
    private String mEditTable;

    /** The names of the columns in the rows */
    private String[] mColumns;

    /** The query object for the cursor */
    private SQLiteQuery mQuery;

    /** The database the cursor was created from */
    private SQLiteDatabase mDatabase;

    /** The compiled query this cursor came from */
    private SQLiteCursorDriver mDriver;

    /** The number of rows in the cursor */
    private int mCount = NO_COUNT;

    /** A mapping of column names to column indices, to speed up lookups */
    private Map<String, Integer> mColumnNameMap;

    /** Used to find out where a cursor was allocated in case it never got released. */
    private Throwable mStackTrace;
    
    /** 
     *  mMaxRead is the max items that each cursor window reads 
     *  default to a very high value
     */
    private int mMaxRead = Integer.MAX_VALUE;
    private int mInitialRead = Integer.MAX_VALUE;
    private int mCursorState = 0;
    private ReentrantLock mLock = null;
    private boolean mPendingData = false;
    
    /**
     *  support for a cursor variant that doesn't always read all results
     *  initialRead is the initial number of items that cursor window reads 
     *  if query contains more than this number of items, a thread will be
     *  created and handle the left over items so that caller can show 
     *  results as soon as possible 
     * @param initialRead initial number of items that cursor read
     * @param maxRead leftover items read at maxRead items per time
     * @hide
     */
    public void setLoadStyle(int initialRead, int maxRead) {
        mMaxRead = maxRead;
        mInitialRead = initialRead;
        mLock = new ReentrantLock(true);
    }
    
    private void queryThreadLock() {
        if (mLock != null) {
            mLock.lock();            
        }
    }
    
    private void queryThreadUnlock() {
        if (mLock != null) {
            mLock.unlock();            
        }
    }
    
    
    /**
     * @hide
     */
    final private class QueryThread implements Runnable {
        private final int mThreadState;
        QueryThread(int version) {
            mThreadState = version;
        }
        private void sendMessage() {
            if (mNotificationHandler != null) {
                mNotificationHandler.sendEmptyMessage(1);
                mPendingData = false;
            } else {
                mPendingData = true;
            }
            
        }
        public void run() {
             // use cached mWindow, to avoid get null mWindow
            CursorWindow cw = mWindow;
            Process.setThreadPriority(Process.myTid(), Process.THREAD_PRIORITY_BACKGROUND);
            // the cursor's state doesn't change
            while (true) {
                mLock.lock();
                if (mCursorState != mThreadState) {
                    mLock.unlock();
                    break;
                }
                try {
                    int count = mQuery.fillWindow(cw, mMaxRead, mCount);
                    // return -1 means not finished
                    if (count != 0) {
                        if (count == NO_COUNT){
                            mCount += mMaxRead;
                            sendMessage();
                        } else {                                
                            mCount = count;
                            sendMessage();
                            break;
                        }
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    // end the tread when the cursor is close
                    break;
                } finally {
                    mLock.unlock();
                }
            }
        }        
    }
    
    
    /**
     * @hide
     */   
    protected class MainThreadNotificationHandler extends Handler {
        public void handleMessage(Message msg) {
            
        	notifyDataSetChange();
            
        }
    }
    
    /**
     * @hide
     */
    protected MainThreadNotificationHandler mNotificationHandler;    
    
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
        if ((Integer.MAX_VALUE != mMaxRead || Integer.MAX_VALUE != mInitialRead) && 
                mNotificationHandler == null) {
            queryThreadLock();
            try {
                mNotificationHandler = new MainThreadNotificationHandler();
                if (mPendingData) {
                	notifyDataSetChange();
                    mPendingData = false;
                }
            } finally {
                queryThreadUnlock();
            }
        }
        
    }
    
    /**
     * Execute a query and provide access to its result set through a Cursor
     * interface. For a query such as: {@code SELECT name, birth, phone FROM
     * myTable WHERE ... LIMIT 1,20 ORDER BY...} the column names (name, birth,
     * phone) would be in the projection argument and everything from
     * {@code FROM} onward would be in the params argument. This constructor
     * has package scope.
     *
     * @param db a reference to a Database object that is already constructed
     *     and opened
     * @param editTable the name of the table used for this query
     * @param query the rest of the query terms
     *     cursor is finalized
     */
    public SQLiteCursor(SQLiteDatabase db, SQLiteCursorDriver driver,
            String editTable, SQLiteQuery query) {
        // The AbstractCursor constructor needs to do some setup.
        super();
        mStackTrace = new DatabaseObjectNotClosedException().fillInStackTrace();
        mDatabase = db;
        mDriver = driver;
        mEditTable = editTable;
        mColumnNameMap = null;
        mQuery = query;

        try {
            db.lock();

            // Setup the list of columns
            int columnCount = mQuery.columnCountLocked();
            mColumns = new String[columnCount];

            // Read in all column names
            for (int i = 0; i < columnCount; i++) {
                String columnName = mQuery.columnNameLocked(i);
                mColumns[i] = columnName;
                if (Config.LOGV) {
                    Log.v("DatabaseWindow", "mColumns[" + i + "] is "
                            + mColumns[i]);
                }
    
                // Make note of the row ID column index for quick access to it
                if ("_id".equals(columnName)) {
                    mRowIdColumnIndex = i;
                }
            }
        } finally {
            db.unlock();
        }
    }

    /**
     * @return the SQLiteDatabase that this cursor is associated with.
     */
    public SQLiteDatabase getDatabase() {
        return mDatabase;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        // Make sure the row at newPosition is present in the window
        if (mWindow == null || newPosition < mWindow.getStartPosition() ||
                newPosition >= (mWindow.getStartPosition() + mWindow.getNumRows())) {
            fillWindow(newPosition);
        }

        return true;
    }

    @Override
    public int getCount() {
        if (mCount == NO_COUNT) {
            fillWindow(0);
        }
        return mCount;
    }

    private void fillWindow (int startPos) {
        if (mWindow == null) {
            // If there isn't a window set already it will only be accessed locally
            mWindow = new CursorWindow(true /* the window is local only */);
        } else {
            mCursorState++;
                queryThreadLock();
                try {
                    mWindow.clear();
                } finally {
                    queryThreadUnlock();
                }
        }
        mWindow.setStartPosition(startPos);
        mCount = mQuery.fillWindow(mWindow, mInitialRead, 0);
        // return -1 means not finished
        if (mCount == NO_COUNT){
            mCount = startPos + mInitialRead;
            Thread t = new Thread(new QueryThread(mCursorState), "query thread");
            t.start();
        } 
    }

    @Override
    public int getColumnIndex(String columnName) {
        // Create mColumnNameMap on demand
        if (mColumnNameMap == null) {
            String[] columns = mColumns;
            int columnCount = columns.length;
            HashMap<String, Integer> map = new HashMap<String, Integer>(columnCount, 1);
            for (int i = 0; i < columnCount; i++) {
                map.put(columns[i], i);
            }
            mColumnNameMap = map;
        }

        // Hack according to bug 903852
        final int periodIndex = columnName.lastIndexOf('.');
        if (periodIndex != -1) {
            Exception e = new Exception();
            Log.e(TAG, "requesting column name with table name -- " + columnName, e);
            columnName = columnName.substring(periodIndex + 1);
        }

        Integer i = mColumnNameMap.get(columnName);
        if (i != null) {
            return i.intValue();
        } else {
            return -1;
        }
    }

    /**
     * @hide
     * @deprecated
     */
   // @Override
    public boolean deleteRow() {
        checkPosition();

        // Only allow deletes if there is an ID column, and the ID has been read from it
        if (mRowIdColumnIndex == -1 || mCurrentRowID == null) {
            Log.e(TAG,
                    "Could not delete row because either the row ID column is not available or it" +
                    "has not been read.");
            return false;
        }

        boolean success;

        /*
         * Ensure we don't change the state of the database when another
         * thread is holding the database lock. requery() and moveTo() are also
         * synchronized here to make sure they get the state of the database
         * immediately following the DELETE.
         */
        mDatabase.lock();
        try {
            try {
                mDatabase.delete(mEditTable, mColumns[mRowIdColumnIndex] + "=?",
                        new String[] {mCurrentRowID.toString()});
                success = true;
            } catch (SQLException e) {
                success = false;
            }

            int pos = mPos;
            requery();

            /*
             * Ensure proper cursor state. Note that mCurrentRowID changes
             * in this call.
             */
            moveToPosition(pos);
        } finally {
            mDatabase.unlock();
        }

        if (success) {
            onChange(true);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String[] getColumnNames() {
        return mColumns;
    }

    /**
     * @hide
     * @deprecated
     */
  //  @Override
    public boolean supportsUpdates() {
       // return super.supportsUpdates() && !TextUtils.isEmpty(mEditTable);
    	return  !TextUtils.isEmpty(mEditTable);
    }

    /**
     * @hide
     * @deprecated
     */
  //  @Override
    public boolean commitUpdates(Map<? extends Long,
            ? extends Map<String, Object>> additionalValues) {
        if (!supportsUpdates()) {
            Log.e(TAG, "commitUpdates not supported on this cursor, did you "
                    + "include the _id column?");
            return false;
        }

        /*
         * Prevent other threads from changing the updated rows while they're
         * being processed here.
         */
        synchronized (mUpdatedRows) {
            if (additionalValues != null) {
                mUpdatedRows.putAll(additionalValues);
            }

            if (mUpdatedRows.size() == 0) {
                return true;
            }

            /*
             * Prevent other threads from changing the database state while
             * we process the updated rows, and prevents us from changing the
             * database behind the back of another thread.
             */
            mDatabase.beginTransaction();
            try {
                StringBuilder sql = new StringBuilder(128);

                // For each row that has been updated
                for (Map.Entry<Long, Map<String, Object>> rowEntry :
                        mUpdatedRows.entrySet()) {
                    Map<String, Object> values = rowEntry.getValue();
                    Long rowIdObj = rowEntry.getKey();

                    if (rowIdObj == null || values == null) {
                        throw new IllegalStateException("null rowId or values found! rowId = "
                                + rowIdObj + ", values = " + values);
                    }

                    if (values.size() == 0) {
                        continue;
                    }

                    long rowId = rowIdObj.longValue();

                    Iterator<Map.Entry<String, Object>> valuesIter =
                            values.entrySet().iterator();

                    sql.setLength(0);
                    sql.append("UPDATE " + mEditTable + " SET ");

                    // For each column value that has been updated
                    Object[] bindings = new Object[values.size()];
                    int i = 0;
                    while (valuesIter.hasNext()) {
                        Map.Entry<String, Object> entry = valuesIter.next();
                        sql.append(entry.getKey());
                        sql.append("=?");
                        bindings[i] = entry.getValue();
                        if (valuesIter.hasNext()) {
                            sql.append(", ");
                        }
                        i++;
                    }

                    sql.append(" WHERE " + mColumns[mRowIdColumnIndex]
                            + '=' + rowId);
                    sql.append(';');
                    mDatabase.execSQL(sql.toString(), bindings);
                    mDatabase.rowUpdated(mEditTable, rowId);
                }
                mDatabase.setTransactionSuccessful();
            } finally {
                mDatabase.endTransaction();
            }

            mUpdatedRows.clear();
        }

        // Let any change observers know about the update
        onChange(true);

        return true;
    }

    private void deactivateCommon() {
        if (Config.LOGV) Log.v(TAG, "<<< Releasing cursor " + this);
        mCursorState = 0;
        if (mWindow != null) {
            mWindow.close();
            mWindow = null;
        }
        if (Config.LOGV) Log.v("DatabaseWindow", "closing window in release()");
    }

    @Override
    public void deactivate() {
        super.deactivate();
        deactivateCommon();
        mDriver.cursorDeactivated();
    }

    @Override
    public void close() {
        super.close();
        deactivateCommon();
        mQuery.close();
        mDriver.cursorClosed();
    }

    @Override
    public boolean requery() {
        if (isClosed()) {
            return false;
        }
        long timeStart = 0;
        if (Config.LOGV) {
            timeStart = System.currentTimeMillis();
        }
        /*
         * Synchronize on the database lock to ensure that mCount matches the
         * results of mQuery.requery().
         */
        mDatabase.lock();
        try {
            if (mWindow != null) {
                mWindow.clear();
            }
            mPos = -1;
            // This one will recreate the temp table, and get its count
            mDriver.cursorRequeried(this);
            mCount = NO_COUNT;
            mCursorState++;
            queryThreadLock();
            try {
                mQuery.requery();
            } finally {
                queryThreadUnlock();
            }
        } finally {
            mDatabase.unlock();
        }

        if (Config.LOGV) {
            Log.v("DatabaseWindow", "closing window in requery()");
            Log.v(TAG, "--- Requery()ed cursor " + this + ": " + mQuery);
        }

        boolean result = super.requery();
        if (Config.LOGV) {
            long timeEnd = System.currentTimeMillis();
            Log.v(TAG, "requery (" + (timeEnd - timeStart) + " ms): " + mDriver.toString());
        }
        return result;
    }

    @Override
    public void setWindow(CursorWindow window) {        
        if (mWindow != null) {
            mCursorState++;
            queryThreadLock();
            try {
                mWindow.close();
            } finally {
                queryThreadUnlock();
            }
            mCount = NO_COUNT;
        }
        mWindow = window;
    }

    /**
     * Changes the selection arguments. The new values take effect after a call to requery().
     */
    public void setSelectionArguments(String[] selectionArgs) {
        mDriver.setBindArguments(selectionArgs);
    }

    /**
     * Release the native resources, if they haven't been released yet.
     */
    @Override
    protected void finalize() {
        try {
            // if the cursor hasn't been closed yet, close it first
            if (mWindow != null) {
                int len = mQuery.mSql.length();
                Log.e(TAG, "Finalizing a Cursor that has not been deactivated or closed. " +
                        "database = " + mDatabase.getPath() + ", table = " + mEditTable +
                        ", query = " + mQuery.mSql.substring(0, (len > 100) ? 100 : len),
                        mStackTrace);
                close();
                SQLiteDebug.notifyActiveCursorFinalized();
            } else {
                if (Config.LOGV) {
                    Log.v(TAG, "Finalizing cursor on database = " + mDatabase.getPath() +
                            ", table = " + mEditTable + ", query = " + mQuery.mSql);
                }
            }
        } finally {
            super.finalize();
        }
    }


	
	@Override
	public void fillWindow(int startPos, android.database.CursorWindow window) {
	
		/*
		window.setStartPosition(startPos);
        mCount = mQuery.fillWindow((net.sqlcipher.database.CursorWindow)window, mInitialRead, 0);
        // return -1 means not finished
        if (mCount == NO_COUNT){
            mCount = startPos + mInitialRead;
            Thread t = new Thread(new QueryThread(mCursorState), "query thread");
            t.start();
        } */
        
        if (mWindow == null) {
            // If there isn't a window set already it will only be accessed locally
            mWindow = new CursorWindow(true /* the window is local only */);
        } else {
            mCursorState++;
                queryThreadLock();
                try {
                    mWindow.clear();
                } finally {
                    queryThreadUnlock();
                }
        }
        mWindow.setStartPosition(startPos);
        mCount = mQuery.fillWindow(mWindow, mInitialRead, 0);
        // return -1 means not finished
        if (mCount == NO_COUNT){
            mCount = startPos + mInitialRead;
            Thread t = new Thread(new QueryThread(mCursorState), "query thread");
            t.start();
        }

		
	}

}
