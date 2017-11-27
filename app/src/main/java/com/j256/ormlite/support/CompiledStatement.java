package com.j256.ormlite.support;

import java.io.Closeable;
import java.sql.SQLException;

import com.j256.ormlite.dao.ObjectCache;
import com.j256.ormlite.field.SqlType;

/**
 * An internal reduction of the SQL PreparedStatement so we can implement its functionality outside of JDBC.
 * 
 * @author graywatson
 */
public interface CompiledStatement extends Closeable {

	/**
	 * Returns the number of columns in this statement.
	 */
	public int getColumnCount() throws SQLException;

	/**
	 * Get the designated column's name.
	 */
	public String getColumnName(int columnIndex) throws SQLException;

	/**
	 * Run the prepared update statement returning the number of rows affected.
	 */
	public int runUpdate() throws SQLException;

	/**
	 * Run the prepared query statement returning the results.
	 */
	public DatabaseResults runQuery(ObjectCache objectCache) throws SQLException;

	/**
	 * Run the prepared execute statement returning the number of rows affected.
	 */
	public int runExecute() throws SQLException;

	/**
	 * Close the statement but swallows any SQLExceptions.
	 */
	public void closeQuietly();

	/**
	 * Cancel a currently running query associated with this statement. Support for this is highly architecture and
	 * database dependent.
	 */
	public void cancel() throws SQLException;

	/**
	 * Set the parameter specified by the index and type to be an object.
	 * 
	 * @param parameterIndex
	 *            Index of the parameter with 0 being the first parameter, etc..
	 * @param obj
	 *            Object that we are setting. Can be null.
	 * @param sqlType
	 *            SQL type of the parameter.
	 */
	public void setObject(int parameterIndex, Object obj, SqlType sqlType) throws SQLException;

	/**
	 * Set the number of rows to return in the results.
	 */
	public void setMaxRows(int max) throws SQLException;

	/**
	 * Set the query timeout in milliseconds. This may or may not be supported by all database types. Although this is
	 * in milliseconds, the underlying timeout resolution may be in seconds.
	 * 
	 * <p>
	 * <b> WARNING: </b> This will stop the query connection but it will _not_ terminate the query if it is already be
	 * running by the database.
	 * </p>
	 */
	public void setQueryTimeout(long millis) throws SQLException;
}
