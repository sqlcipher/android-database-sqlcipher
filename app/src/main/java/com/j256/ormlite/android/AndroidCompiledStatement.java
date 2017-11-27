package com.j256.ormlite.android;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteStatement;

import com.j256.ormlite.android.compat.ApiCompatibility;
import com.j256.ormlite.android.compat.ApiCompatibility.CancellationHook;
import com.j256.ormlite.android.compat.ApiCompatibilityUtils;
import com.j256.ormlite.dao.ObjectCache;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.stmt.StatementBuilder.StatementType;
import com.j256.ormlite.support.CompiledStatement;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Android implementation of the compiled statement.
 * 
 * @author kevingalligan, graywatson
 */
public class AndroidCompiledStatement implements CompiledStatement {

	private static Logger logger = LoggerFactory.getLogger(AndroidCompiledStatement.class);

	private static final String[] NO_STRING_ARGS = new String[0];
	private static final ApiCompatibility apiCompatibility = ApiCompatibilityUtils.getCompatibility();

	private final String sql;
	private final SQLiteDatabase db;
	private final StatementType type;
	private final boolean cancelQueriesEnabled;
	private final boolean cacheStore;

	private Cursor cursor;
	private List<Object> args;
	private Integer max;
	private CancellationHook cancellationHook;

	public AndroidCompiledStatement(String sql, SQLiteDatabase db, StatementType type, boolean cancelQueriesEnabled,
			boolean cacheStore) {
		this.sql = sql;
		this.db = db;
		this.type = type;
		this.cancelQueriesEnabled = cancelQueriesEnabled;
		this.cacheStore = cacheStore;
	}

	@Override
	public int getColumnCount() throws SQLException {
		return getCursor().getColumnCount();
	}

	@Override
	public String getColumnName(int column) throws SQLException {
		return getCursor().getColumnName(column);
	}

	@Override
	public DatabaseResults runQuery(ObjectCache objectCache) throws SQLException {
		// this could come from DELETE or UPDATE, just not a SELECT
		if (!type.isOkForQuery()) {
			throw new IllegalArgumentException("Cannot call query on a " + type + " statement");
		}
		return new AndroidDatabaseResults(getCursor(), objectCache, cacheStore);
	}

	@Override
	public int runUpdate() throws SQLException {
		if (!type.isOkForUpdate()) {
			throw new IllegalArgumentException("Cannot call update on a " + type + " statement");
		}
		String finalSql;
		if (max == null) {
			finalSql = sql;
		} else {
			finalSql = sql + " " + max;
		}
		return execSql(db, "runUpdate", finalSql, getArgArray());
	}

	@Override
	public int runExecute() throws SQLException {
		if (!type.isOkForExecute()) {
			throw new IllegalArgumentException("Cannot call execute on a " + type + " statement");
		}
		return execSql(db, "runExecute", sql, getArgArray());
	}

	@Override
	public void close() throws IOException {
		if (cursor != null && !cursor.isClosed()) {
			try {
				cursor.close();
			} catch (android.database.SQLException e) {
				throw new IOException("Problems closing Android cursor", e);
			}
		}
		cancellationHook = null;
	}

	@Override
	public void closeQuietly() {
		if (cursor != null) {
			cursor.close();
		}
	}

	@Override
	public void cancel() {
		if (cancellationHook != null) {
			cancellationHook.cancel();
		}
	}

	@Override
	public void setObject(int parameterIndex, Object obj, SqlType sqlType) throws SQLException {
		isInPrep();
		if (args == null) {
			args = new ArrayList<Object>();
		}
		if (obj == null) {
			args.add(parameterIndex, null);
			return;
		}

		switch (sqlType) {
			case STRING:
			case LONG_STRING:
			case DATE:
			case BOOLEAN:
			case CHAR:
			case BYTE:
			case SHORT:
			case INTEGER:
			case LONG:
			case FLOAT:
			case DOUBLE:
				args.add(parameterIndex, obj.toString());
				break;
			case BYTE_ARRAY:
			case SERIALIZABLE:
				args.add(parameterIndex, obj);
				break;
			case BLOB:
				// this is only for derby serializable
			case BIG_DECIMAL:
				// this should be handled as a STRING
				throw new SQLException("Invalid Android type: " + sqlType);
			case UNKNOWN:
			default:
				throw new SQLException("Unknown sql argument type: " + sqlType);
		}
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		isInPrep();
		this.max = max;
	}

	@Override
	public void setQueryTimeout(long millis) {
		// as far as I could tell this is not supported by Android API
	}

	/***
	 * This is mostly an internal class but is exposed for those people who need access to the Cursor itself.
	 * 
	 * <p>
	 * NOTE: This is not thread safe. Not sure if we need it, but keep that in mind.
	 * </p>
	 */
	public Cursor getCursor() throws SQLException {
		if (cursor == null) {
			String finalSql = null;
			try {
				if (max == null) {
					finalSql = sql;
				} else {
					finalSql = sql + " LIMIT " + max;
				}
				if (cancelQueriesEnabled) {
					cancellationHook = apiCompatibility.createCancellationHook();
				}
				cursor = apiCompatibility.rawQuery(db, finalSql, getStringArray(), cancellationHook);
				cursor.moveToFirst();
				logger.trace("{}: started rawQuery cursor for: {}", this, finalSql);
			} catch (android.database.SQLException e) {
				throw SqlExceptionUtil.create("Problems executing Android query: " + finalSql, e);
			}
		}

		return cursor;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(super.hashCode());
	}

	/**
	 * Execute some SQL on the database and return the number of rows changed.
	 */
	static int execSql(SQLiteDatabase db, String label, String finalSql, Object[] argArray) throws SQLException {
		try {
			db.execSQL(finalSql, argArray);
		} catch (android.database.SQLException e) {
			throw SqlExceptionUtil.create("Problems executing " + label + " Android statement: " + finalSql, e);
		}
		int result;
		SQLiteStatement stmt = null;
		try {
			// ask sqlite how many rows were just changed
			stmt = db.compileStatement("SELECT CHANGES()");
			result = (int) stmt.simpleQueryForLong();
		} catch (android.database.SQLException e) {
			// ignore the exception and just return 1 if it failed
			result = 1;
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
		logger.trace("executing statement {} changed {} rows: {}", label, result, finalSql);
		return result;
	}

	private void isInPrep() throws SQLException {
		if (cursor != null) {
			throw new SQLException("Query already run. Cannot add argument values.");
		}
	}

	private Object[] getArgArray() {
		if (args == null) {
			// this will work for Object[] as well as String[]
			return NO_STRING_ARGS;
		} else {
			return args.toArray(new Object[args.size()]);
		}
	}

	private String[] getStringArray() {
		if (args == null) {
			return NO_STRING_ARGS;
		} else {
			// we assume we have Strings in args
			return args.toArray(new String[args.size()]);
		}
	}
}
