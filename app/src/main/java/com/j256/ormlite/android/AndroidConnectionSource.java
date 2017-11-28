package com.j256.ormlite.android;

import java.sql.SQLException;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.db.SqliteAndroidDatabaseType;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.BaseConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.support.DatabaseConnectionProxyFactory;

/**
 * Android version of the connection source. Takes a standard Android {@link SQLiteOpenHelper}. For best results, use
 * {@link OrmLiteSqliteOpenHelper}. You can also construct with a {@link SQLiteDatabase}.
 * 
 * @author kevingalligan, graywatson
 */
public class AndroidConnectionSource extends BaseConnectionSource implements ConnectionSource {

	private static final Logger logger = LoggerFactory.getLogger(AndroidConnectionSource.class);

	private final SQLiteOpenHelper helper;
	private final SQLiteDatabase sqliteDatabase;
	private DatabaseConnection connection = null;
	private volatile boolean isOpen = true;
	private final DatabaseType databaseType = new SqliteAndroidDatabaseType();
	private static DatabaseConnectionProxyFactory connectionProxyFactory;
	private boolean cancelQueriesEnabled = false;

	public AndroidConnectionSource(SQLiteOpenHelper helper) {
		this.helper = helper;
		this.sqliteDatabase = null;
	}

	public AndroidConnectionSource(SQLiteDatabase sqliteDatabase) {
		this.helper = null;
		this.sqliteDatabase = sqliteDatabase;
	}

	@Override
	public DatabaseConnection getReadOnlyConnection(String tableName) throws SQLException {
		/*
		 * We have to use the read-write connection because getWritableDatabase() can call close on
		 * getReadableDatabase() in the future. This has something to do with Android's SQLite connection management.
		 * 
		 * See android docs: http://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html
		 */
		return getReadWriteConnection(tableName);
	}

	@Override
	public DatabaseConnection getReadWriteConnection(String tableName) throws SQLException {
		DatabaseConnection conn = getSavedConnection();
		if (conn != null) {
			return conn;
		}
		if (connection == null) {
			SQLiteDatabase db;
			if (sqliteDatabase == null) {
				try {
					db = helper.getWritableDatabase();
				} catch (android.database.SQLException e) {
					throw SqlExceptionUtil.create("Getting a writable database from helper " + helper + " failed", e);
				}
			} else {
				db = sqliteDatabase;
			}
			connection = new AndroidDatabaseConnection(db, true, cancelQueriesEnabled);
			if (connectionProxyFactory != null) {
				connection = connectionProxyFactory.createProxy(connection);
			}
			logger.trace("created connection {} for db {}, helper {}", connection, db, helper);
		} else {
			logger.trace("{}: returning read-write connection {}, helper {}", this, connection, helper);
		}
		return connection;
	}

	@Override
	public void releaseConnection(DatabaseConnection connection) {
		// noop since connection management is handled by AndroidOS
	}

	@Override
	public boolean saveSpecialConnection(DatabaseConnection connection) throws SQLException {
		return saveSpecial(connection);
	}

	@Override
	public void clearSpecialConnection(DatabaseConnection connection) {
		clearSpecial(connection, logger);
	}

	@Override
	public void close() {
		// the helper is closed so it calls close here, so this CANNOT be a call back to helper.close()
		isOpen = false;
	}

	@Override
	public void closeQuietly() {
		close();
	}

	@Override
	public DatabaseType getDatabaseType() {
		return databaseType;
	}

	@Override
	public boolean isOpen(String tableName) {
		return isOpen;
	}

	@Override
	public boolean isSingleConnection(String tableName) {
		return true;
	}

	/**
	 * Set to enable connection proxying. Set to null to disable.
	 */
	public static void setDatabaseConnectionProxyFactory(DatabaseConnectionProxyFactory connectionProxyFactory) {
		AndroidConnectionSource.connectionProxyFactory = connectionProxyFactory;
	}

	public boolean isCancelQueriesEnabled() {
		return cancelQueriesEnabled;
	}

	/**
	 * Set to true to enable the canceling of queries.
	 * 
	 * <p>
	 * <b>NOTE:</b> This will incur a slight memory increase for all Cursor based queries -- even if cancel is not
	 * called for them.
	 * </p>
	 */
	public void setCancelQueriesEnabled(boolean cancelQueriesEnabled) {
		this.cancelQueriesEnabled = cancelQueriesEnabled;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(super.hashCode());
	}
}
