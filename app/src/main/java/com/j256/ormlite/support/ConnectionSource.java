package com.j256.ormlite.support;

import java.io.Closeable;
import java.sql.SQLException;

import com.j256.ormlite.db.DatabaseType;

/**
 * A reduction of the SQL DataSource so we can implement its functionality outside of JDBC.
 * 
 * @author graywatson
 */
public interface ConnectionSource extends Closeable {

	/**
	 * Return a database connection suitable for read-only operations. After you are done, you should call
	 * {@link #releaseConnection(DatabaseConnection)}.
	 */
	public DatabaseConnection getReadOnlyConnection(String tableName) throws SQLException;

	/**
	 * Return a database connection suitable for read or write operations. After you are done, you should call
	 * {@link #releaseConnection(DatabaseConnection)}.
	 */
	public DatabaseConnection getReadWriteConnection(String tableName) throws SQLException;

	/**
	 * Release a database connection previously returned by {@link #getReadOnlyConnection(String)} or
	 * {@link #getReadWriteConnection(String)}.
	 */
	public void releaseConnection(DatabaseConnection connection) throws SQLException;

	/**
	 * Save this connection and return it for all calls to {@link #getReadOnlyConnection(String)} and
	 * {@link #getReadWriteConnection(String)} unless the {@link #clearSpecialConnection(DatabaseConnection)} method is
	 * called, all This is used by the transaction mechanism since since all operations within a transaction must
	 * operate on the same connection. It is also used by the Android code during initialization.
	 * 
	 * <p>
	 * <b> NOTE: </b> This should be a read-write connection since transactions and Android need it to be so.
	 * </p>
	 * 
	 * <p>
	 * <b> NOTE: </b> Saving a connection is usually accomplished using ThreadLocals so multiple threads should not be
	 * using connections in this scenario.
	 * </p>
	 * 
	 * @return True if the connection was saved or false if we were already inside of a saved connection.
	 */
	public boolean saveSpecialConnection(DatabaseConnection connection) throws SQLException;

	/**
	 * Clear the saved connection.
	 */
	public void clearSpecialConnection(DatabaseConnection connection);

	/**
	 * Return the currently saved connection or null if none.
	 */
	public DatabaseConnection getSpecialConnection(String tableName);

	/**
	 * Close any outstanding database connections.
	 */
	public void closeQuietly();

	/**
	 * Return the DatabaseTypre associated with this connection.
	 */
	public DatabaseType getDatabaseType();

	/**
	 * Return true if the connection source is open. Once {@link #close()} has been called, this should return false.
	 */
	public boolean isOpen(String tableName);

	/**
	 * Return true if there is only one connection to the database being used by this connection-sourse. If true then
	 * some synchronization will be enabled when using batch tasks. The user will also need to synchronize around some
	 * of the transaction and auto-commit calls.
	 */
	public boolean isSingleConnection(String tableName);
}
