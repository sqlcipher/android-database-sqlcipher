package com.j256.ormlite.dao;

import java.sql.SQLException;

import com.j256.ormlite.stmt.QueryBuilder;

/**
 * Parameterized row mapper that takes output from the {@link GenericRawResults} and returns a T. Is used in the
 * {@link Dao#queryRaw(String, RawRowMapper, String...)} method.
 * 
 * <p>
 * <b> NOTE: </b> If you need to map Objects instead then consider using the {@link RawRowObjectMapper} with the
 * {@link Dao#queryRaw(String, com.j256.ormlite.field.DataType[], RawRowObjectMapper, String...)} method which allows
 * you to iterate over the raw results as Object[].
 * </p>
 * 
 * @param <T>
 *            Type that the mapRow returns.
 * @author graywatson
 */
public interface RawRowMapper<T> {

	/**
	 * Used to convert a raw results row to an object.
	 * 
	 * <p>
	 * <b>NOTE:</b> If you are using the {@link QueryBuilder#prepareStatementString()} to build your query, it may have
	 * added the id column to the selected column list if the Dao object has an id you did not include it in the columns
	 * you selected. So the results might have one more column than you are expecting.
	 * </p>
	 * 
	 * @return The created object with all of the fields set from the results. Return null if there is no object
	 *         generated from these results.
	 * @param columnNames
	 *            Array of names of columns.
	 * @param resultColumns
	 *            Array of result columns.
	 * @throws SQLException
	 *             If there is any critical error with the data and you want to stop the paging.
	 */
	public T mapRow(String[] columnNames, String[] resultColumns) throws SQLException;
}
