package com.j256.ormlite.android;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Savepoint;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteStatement;

import com.j256.ormlite.dao.ObjectCache;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.misc.IOUtils;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.misc.VersionUtils;
import com.j256.ormlite.stmt.GenericRowMapper;
import com.j256.ormlite.stmt.StatementBuilder.StatementType;
import com.j256.ormlite.support.CompiledStatement;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.support.GeneratedKeyHolder;

/**
 * Database connection for Android.
 * 
 * @author kevingalligan, graywatson
 */
public class AndroidDatabaseConnection implements DatabaseConnection {

	private static final String ANDROID_VERSION = "VERSION__5.0__";

	private static Logger logger = LoggerFactory.getLogger(AndroidDatabaseConnection.class);
	private static final String[] NO_STRING_ARGS = new String[0];

	private final SQLiteDatabase db;
	private final boolean readWrite;
	private final boolean cancelQueriesEnabled;

	static {
		VersionUtils.checkCoreVersusAndroidVersions(ANDROID_VERSION);
	}

	public AndroidDatabaseConnection(SQLiteDatabase db, boolean readWrite) {
		this(db, readWrite, false);
	}

	public AndroidDatabaseConnection(SQLiteDatabase db, boolean readWrite, boolean cancelQueriesEnabled) {
		this.db = db;
		this.readWrite = readWrite;
		this.cancelQueriesEnabled = cancelQueriesEnabled;
		logger.trace("{}: db {} opened, read-write = {}", this, db, readWrite);
	}

	@Override
	public boolean isAutoCommitSupported() {
		return true;
	}

	@Override
	public boolean isAutoCommit() throws SQLException {
		try {
			boolean inTransaction = db.inTransaction();
			logger.trace("{}: in transaction is {}", this, inTransaction);
			// You have to explicitly commit your transactions, so this is sort of correct
			return !inTransaction;
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("problems getting auto-commit from database", e);
		}
	}

	@Override
	public void setAutoCommit(boolean autoCommit) {
		/*
		 * Sqlite does not support auto-commit. The various JDBC drivers seem to implement it with the use of a
		 * transaction. That's what we are doing here.
		 */
		if (autoCommit) {
			if (db.inTransaction()) {
				db.setTransactionSuccessful();
				db.endTransaction();
			}
		} else {
			if (!db.inTransaction()) {
				db.beginTransaction();
			}
		}
	}

	@Override
	public Savepoint setSavePoint(String name) throws SQLException {
		try {
			db.beginTransaction();
			logger.trace("{}: save-point set with name {}", this, name);
			return new OurSavePoint(name);
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("problems beginning transaction " + name, e);
		}
	}

	/**
	 * Return whether this connection is read-write or not (real-only).
	 */
	public boolean isReadWrite() {
		return readWrite;
	}

	@Override
	public void commit(Savepoint savepoint) throws SQLException {
		try {
			db.setTransactionSuccessful();
			db.endTransaction();
			if (savepoint == null) {
				logger.trace("{}: transaction is successfuly ended", this);
			} else {
				logger.trace("{}: transaction {} is successfuly ended", this, savepoint.getSavepointName());
			}
		} catch (android.database.SQLException e) {
			if (savepoint == null) {
				throw SqlExceptionUtil.create("problems commiting transaction", e);
			} else {
				throw SqlExceptionUtil.create("problems commiting transaction " + savepoint.getSavepointName(), e);
			}
		}
	}

	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		try {
			// no setTransactionSuccessful() means it is a rollback
			db.endTransaction();
			if (savepoint == null) {
				logger.trace("{}: transaction is ended, unsuccessfuly", this);
			} else {
				logger.trace("{}: transaction {} is ended, unsuccessfuly", this, savepoint.getSavepointName());
			}
		} catch (android.database.SQLException e) {
			if (savepoint == null) {
				throw SqlExceptionUtil.create("problems rolling back transaction", e);
			} else {
				throw SqlExceptionUtil.create("problems rolling back transaction " + savepoint.getSavepointName(), e);
			}
		}
	}

	@Override
	public int executeStatement(String statementStr, int resultFlags) throws SQLException {
		return AndroidCompiledStatement.execSql(db, statementStr, statementStr, NO_STRING_ARGS);
	}

	@Override
	public CompiledStatement compileStatement(String statement, StatementType type, FieldType[] argFieldTypes,
			int resultFlags, boolean cacheStore) {
		// resultFlags argument is not used in Android-land since the {@link Cursor} is bi-directional.
		CompiledStatement stmt = new AndroidCompiledStatement(statement, db, type, cancelQueriesEnabled, cacheStore);
		logger.trace("{}: compiled statement got {}: {}", this, stmt, statement);
		return stmt;
	}

	@Override
	public int insert(String statement, Object[] args, FieldType[] argFieldTypes, GeneratedKeyHolder keyHolder)
			throws SQLException {
		SQLiteStatement stmt = null;
		try {
			stmt = db.compileStatement(statement);
			bindArgs(stmt, args, argFieldTypes);
			long rowId = stmt.executeInsert();
			if (keyHolder != null) {
				keyHolder.addKey(rowId);
			}
			/*
			 * I've decided to not do the CHANGES() statement here like we do down below in UPDATE because we know that
			 * it worked (since it didn't throw) so we know that 1 is right.
			 */
			int result = 1;
			logger.trace("{}: insert statement is compiled and executed, changed {}: {}", this, result, statement);
			return result;
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("inserting to database failed: " + statement, e);
		} finally {
			closeQuietly(stmt);
		}
	}

	@Override
	public int update(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		return update(statement, args, argFieldTypes, "updated");
	}

	@Override
	public int delete(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		// delete is the same as update
		return update(statement, args, argFieldTypes, "deleted");
	}

	@Override
	public <T> Object queryForOne(String statement, Object[] args, FieldType[] argFieldTypes,
			GenericRowMapper<T> rowMapper, ObjectCache objectCache) throws SQLException {
		Cursor cursor = null;
		AndroidDatabaseResults results = null;
		try {
			cursor = db.rawQuery(statement, toStrings(args));
			results = new AndroidDatabaseResults(cursor, objectCache, true);
			logger.trace("{}: queried for one result: {}", this, statement);
			if (!results.first()) {
				return null;
			} else {
				T first = rowMapper.mapRow(results);
				if (results.next()) {
					return MORE_THAN_ONE;
				} else {
					return first;
				}
			}
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("queryForOne from database failed: " + statement, e);
		} finally {
			IOUtils.closeQuietly(results);
			closeQuietly(cursor);
		}
	}

	@Override
	public long queryForLong(String statement) throws SQLException {
		SQLiteStatement stmt = null;
		try {
			stmt = db.compileStatement(statement);
			long result = stmt.simpleQueryForLong();
			logger.trace("{}: query for long simple query returned {}: {}", this, result, statement);
			return result;
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("queryForLong from database failed: " + statement, e);
		} finally {
			closeQuietly(stmt);
		}
	}

	@Override
	public long queryForLong(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		Cursor cursor = null;
		AndroidDatabaseResults results = null;
		try {
			cursor = db.rawQuery(statement, toStrings(args));
			results = new AndroidDatabaseResults(cursor, null, false);
			long result;
			if (results.first()) {
				result = results.getLong(0);
			} else {
				result = 0L;
			}
			logger.trace("{}: query for long raw query returned {}: {}", this, result, statement);
			return result;
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("queryForLong from database failed: " + statement, e);
		} finally {
			closeQuietly(cursor);
			IOUtils.closeQuietly(results);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			db.close();
			logger.trace("{}: db {} closed", this, db);
		} catch (android.database.SQLException e) {
			throw new IOException("problems closing the database connection", e);
		}
	}

	@Override
	public void closeQuietly() {
		IOUtils.closeQuietly(this);
	}

	@Override
	public boolean isClosed() throws SQLException {
		try {
			boolean isOpen = db.isOpen();
			logger.trace("{}: db {} isOpen returned {}", this, db, isOpen);
			return !isOpen;
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("problems detecting if the database is closed", e);
		}
	}

	@Override
	public boolean isTableExists(String tableName) {
		Cursor cursor =
				db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = '" + tableName + "'", null);
		try {
			boolean result;
			if (cursor.getCount() > 0) {
				result = true;
			} else {
				result = false;
			}
			logger.trace("{}: isTableExists '{}' returned {}", this, tableName, result);
			return result;
		} finally {
			cursor.close();
		}
	}

	private int update(String statement, Object[] args, FieldType[] argFieldTypes, String label) throws SQLException {
		SQLiteStatement stmt = null;
		try {
			stmt = db.compileStatement(statement);
			bindArgs(stmt, args, argFieldTypes);
			stmt.execute();
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("updating database failed: " + statement, e);
		} finally {
			closeQuietly(stmt);
			stmt = null;
		}
		int result;
		try {
			stmt = db.compileStatement("SELECT CHANGES()");
			result = (int) stmt.simpleQueryForLong();
		} catch (android.database.SQLException e) {
			// ignore the exception and just return 1
			result = 1;
		} finally {
			closeQuietly(stmt);
		}
		logger.trace("{} statement is compiled and executed, changed {}: {}", label, result, statement);
		return result;
	}

	private void bindArgs(SQLiteStatement stmt, Object[] args, FieldType[] argFieldTypes) throws SQLException {
		if (args == null) {
			return;
		}
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (arg == null) {
				stmt.bindNull(i + 1);
			} else {
				SqlType sqlType = argFieldTypes[i].getSqlType();
				switch (sqlType) {
					case STRING :
					case LONG_STRING :
					case CHAR :
						stmt.bindString(i + 1, arg.toString());
						break;
					case BOOLEAN :
					case BYTE :
					case SHORT :
					case INTEGER :
					case LONG :
						stmt.bindLong(i + 1, ((Number) arg).longValue());
						break;
					case FLOAT :
					case DOUBLE :
						stmt.bindDouble(i + 1, ((Number) arg).doubleValue());
						break;
					case BYTE_ARRAY :
					case SERIALIZABLE :
						stmt.bindBlob(i + 1, (byte[]) arg);
						break;
					case DATE :
						// this is mapped to a STRING under Android
					case BLOB :
						// this is only for derby serializable
					case BIG_DECIMAL :
						// this should be handled as a STRING
						throw new SQLException("Invalid Android type: " + sqlType);
					case UNKNOWN :
					default :
						throw new SQLException("Unknown sql argument type: " + sqlType);
				}
			}
		}
	}

	private String[] toStrings(Object[] args) {
		if (args == null || args.length == 0) {
			return null;
		}
		String[] strings = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			if (arg == null) {
				strings[i] = null;
			} else {
				strings[i] = arg.toString();
			}
		}

		return strings;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(super.hashCode());
	}

	/**
	 * We can't use IOUtils here because older versions didn't implement Closeable.
	 */
	private void closeQuietly(Cursor cursor) {
		if (cursor != null) {
			cursor.close();
		}
	}

	/**
	 * We can't use IOUtils here because older versions didn't implement Closeable.
	 */
	private void closeQuietly(SQLiteStatement statement) {
		if (statement != null) {
			statement.close();
		}
	}

	private static class OurSavePoint implements Savepoint {

		private String name;

		public OurSavePoint(String name) {
			this.name = name;
		}

		@Override
		public int getSavepointId() {
			return 0;
		}

		@Override
		public String getSavepointName() {
			return name;
		}
	}
}
