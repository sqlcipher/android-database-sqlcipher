package com.j256.ormlite.stmt;

import java.sql.SQLException;

import com.j256.ormlite.stmt.StatementBuilder.StatementType;
import com.j256.ormlite.support.CompiledStatement;
import com.j256.ormlite.support.DatabaseConnection;

/**
 * Parent interface for the {@link PreparedQuery}, {@link PreparedUpdate}, and {@link PreparedDelete} interfaces.
 */
public interface PreparedStmt<T> extends GenericRowMapper<T> {

	/**
	 * Create and return the associated compiled statement. You must call {@link CompiledStatement#close()} after you
	 * are done with the statement so any database connections can be freed.
	 */
	public CompiledStatement compile(DatabaseConnection databaseConnection, StatementType type) throws SQLException;

	/**
	 * Like compile(DatabaseConnection, StatementType) but allows to specify the result flags.
	 * 
	 * @param resultFlags
	 *            Set to -1 for default.
	 */
	public CompiledStatement compile(DatabaseConnection databaseConnection, StatementType type, int resultFlags)
			throws SQLException;

	/**
	 * Return the associated SQL statement string for logging purposes.
	 */
	public String getStatement() throws SQLException;

	/**
	 * Return the type of the statement for internal consistency checking.
	 */
	public StatementType getType();

	/**
	 * If any argument holder's have been set in this prepared statement then this is a convenience method to be able to
	 * set them.
	 * 
	 * <p>
	 * <b>NOTE</b> This method is for folks who know what they are doing. Unfortunately the index of the argument holder
	 * is dependent on how the query was built which for complex queries may be difficult to determine. Also, certain
	 * field types (such as a Date) allocate an argument internally so you will need to take this into account.
	 * </p>
	 * 
	 * @param index
	 *            The index of the holder you are going to set, 0 based. See NOTE above.
	 * @param value
	 *            Object to set in the argument holder.
	 */
	public void setArgumentHolderValue(int index, Object value) throws SQLException;
}
