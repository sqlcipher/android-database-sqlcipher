package com.j256.ormlite.support;

import java.io.Closeable;
import java.sql.SQLException;
import java.sql.Savepoint;

import com.j256.ormlite.dao.ObjectCache;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.stmt.GenericRowMapper;
import com.j256.ormlite.stmt.StatementBuilder.StatementType;

/**
 * A reduction of the SQL Connection so we can implement its functionality outside of JDBC.
 * 
 * @author graywatson
 */
public interface DatabaseConnection extends Closeable {

	/** returned by {@link #queryForOne} if more than one result was found by the query */
	public final static Object MORE_THAN_ONE = new Object();
	public final static int DEFAULT_RESULT_FLAGS = -1;

	/**
	 * Return if auto-commit is supported.
	 */
	public boolean isAutoCommitSupported() throws SQLException;

	/**
	 * Return if auto-commit is currently enabled.
	 */
	public boolean isAutoCommit() throws SQLException;

	/**
	 * Set the auto-commit to be on (true) or off (false). Setting auto-commit to true may or may-not cause a commit
	 * depending on the underlying database code.
	 */
	public void setAutoCommit(boolean autoCommit) throws SQLException;

	/**
	 * Start a save point with a certain name. It can be a noop if savepoints are not supported.
	 * 
	 * @param savePointName
	 *            to use for the Savepoint although it can be ignored.
	 * 
	 * @return A SavePoint object with which we can release or commit in the future or null if none.
	 */
	public Savepoint setSavePoint(String savePointName) throws SQLException;

	/**
	 * Commit all changes since the savepoint was created. If savePoint is null then commit all outstanding changes.
	 * 
	 * @param savePoint
	 *            That was returned by setSavePoint or null if none.
	 */
	public void commit(Savepoint savePoint) throws SQLException;

	/**
	 * Roll back all changes since the savepoint was created. If savePoint is null then roll back all outstanding
	 * changes.
	 * 
	 * @param savePoint
	 *            That was returned by setSavePoint previously or null if none.
	 */
	public void rollback(Savepoint savePoint) throws SQLException;

	/**
	 * Execute a statement directly on the connection.
	 * 
	 * @param resultFlags
	 *            Allows specification of some result flags. This is dependent on the backend and database type. Set to
	 *            {@link #DEFAULT_RESULT_FLAGS} for the internal default.
	 */
	public int executeStatement(String statementStr, int resultFlags) throws SQLException;

	/**
	 * Like compileStatement(String, StatementType, FieldType[]) except the caller can specify the result flags.
	 * 
	 * @param resultFlags
	 *            Allows specification of some result flags. This is dependent on the backend and database type. Set to
	 *            {@link #DEFAULT_RESULT_FLAGS} for the internal default.
	 * @param cacheStore
	 *            Cache can store results from this statement.
	 */
	public CompiledStatement compileStatement(String statement, StatementType type, FieldType[] argFieldTypes,
			int resultFlags, boolean cacheStore) throws SQLException;

	/**
	 * Perform a SQL update while with the associated SQL statement, arguments, and types. This will possibly return
	 * generated keys if kyeHolder is not null.
	 * 
	 * @param statement
	 *            SQL statement to use for inserting.
	 * @param args
	 *            Object arguments for the SQL '?'s.
	 * @param argfieldTypes
	 *            Field types of the arguments.
	 * @param keyHolder
	 *            The holder that gets set with the generated key value which may be null.
	 * @return The number of rows affected by the update. With some database types, this value may be invalid.
	 */
	public int insert(String statement, Object[] args, FieldType[] argfieldTypes, GeneratedKeyHolder keyHolder)
			throws SQLException;

	/**
	 * Perform a SQL update with the associated SQL statement, arguments, and types.
	 * 
	 * @param statement
	 *            SQL statement to use for updating.
	 * @param args
	 *            Object arguments for the SQL '?'s.
	 * @param argfieldTypes
	 *            Field types of the arguments.
	 * @return The number of rows affected by the update. With some database types, this value may be invalid.
	 */
	public int update(String statement, Object[] args, FieldType[] argfieldTypes) throws SQLException;

	/**
	 * Perform a SQL delete with the associated SQL statement, arguments, and types.
	 * 
	 * @param statement
	 *            SQL statement to use for deleting.
	 * @param args
	 *            Object arguments for the SQL '?'s.
	 * @param argfieldTypes
	 *            Field types of the arguments.
	 * @return The number of rows affected by the update. With some database types, this value may be invalid.
	 */
	public int delete(String statement, Object[] args, FieldType[] argfieldTypes) throws SQLException;

	/**
	 * Perform a SQL query with the associated SQL statement, arguments, and types and returns a single result.
	 * 
	 * @param statement
	 *            SQL statement to use for deleting.
	 * @param args
	 *            Object arguments for the SQL '?'s.
	 * @param argfieldTypes
	 *            Field types of the arguments.
	 * @param rowMapper
	 *            The mapper to use to convert the row into the returned object.
	 * @param objectCache
	 *            Any object cache associated with the query or null if none.
	 * @return The first data item returned by the query which can be cast to T, null if none, the object
	 *         {@link #MORE_THAN_ONE} if more than one result was found.
	 */
	public <T> Object queryForOne(String statement, Object[] args, FieldType[] argfieldTypes,
			GenericRowMapper<T> rowMapper, ObjectCache objectCache) throws SQLException;

	/**
	 * Perform a query whose result should be a single long-integer value.
	 * 
	 * @param statement
	 *            SQL statement to use for the query.
	 */
	public long queryForLong(String statement) throws SQLException;

	/**
	 * Perform a query whose result should be a single long-integer value.
	 * 
	 * @param statement
	 *            SQL statement to use for the query.
	 * @param args
	 *            Arguments to pass into the query.
	 * @param argFieldTypes
	 *            Field types that correspond to the args.
	 */
	public long queryForLong(String statement, Object[] args, FieldType[] argFieldTypes) throws SQLException;

	/**
	 * Close the connection to the database but swallow any exceptions.
	 */
	public void closeQuietly();

	/**
	 * Return if the connection has been closed either through a call to {@link #close()} or because of a fatal error.
	 */
	public boolean isClosed() throws SQLException;

	/**
	 * Return true if the table exists in the database.
	 */
	public boolean isTableExists(String tableName) throws SQLException;
}
