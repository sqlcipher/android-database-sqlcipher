package com.j256.ormlite.stmt;

import java.sql.SQLException;

import com.j256.ormlite.support.DatabaseResults;

/**
 * Parameterized version similar to Spring's RowMapper which converts a result row into an object.
 * 
 * @param <T>
 *            Type that the mapRow returns.
 * @author graywatson
 */
public interface GenericRowMapper<T> {

	/**
	 * Used to convert a results row to an object.
	 * 
	 * @return The created object with all of the fields set from the results;
	 * @param results
	 *            Results object we are mapping.
	 * @throws SQLException
	 *             If we could not get the SQL results or instantiate the object.
	 */
	public T mapRow(DatabaseResults results) throws SQLException;
}
