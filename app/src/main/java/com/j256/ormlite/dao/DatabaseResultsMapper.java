package com.j256.ormlite.dao;

import java.sql.SQLException;

import com.j256.ormlite.support.DatabaseResults;

/**
 * Result apper that utilized the raw {@link DatabaseResults} object. See
 * {@link Dao#queryRaw(String, DatabaseResultsMapper, String...)}.
 * 
 * @author nonameplum
 */
public interface DatabaseResultsMapper<T> {

	/**
	 * Map the row with the raw DatabaseResults object. This method should not cause the result set to advance or
	 * otherwise change position. If you are using under JDBC then you can cast the DatabaseResults object to be
	 * {@code JdbcDatabaseResults} and get the raw ResultSet using {@code jdbcDatabaseResults.getResultSet()}.
	 * 
	 * @param databaseResults
	 *            The results entry that is currently being iterated. You must not advance or call any of the other move
	 *            operations on this parameter.
	 * @return The value to return for this row to be included in the raw results iterator.
	 * @throws SQLException
	 *             If there is any critical error with the data and you want to stop the paging.
	 */
	public T mapRow(DatabaseResults databaseResults) throws SQLException;
}
