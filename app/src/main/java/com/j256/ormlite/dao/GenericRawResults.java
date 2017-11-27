package com.j256.ormlite.dao;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.j256.ormlite.field.DataType;

/**
 * Results returned by a call to {@link Dao#queryRaw(String, String...)} which returns results as a String[],
 * {@link Dao#queryRaw(String, RawRowMapper, String...)} which returns results mapped by the caller to an Object, and
 * {@link Dao#queryRaw(String, DataType[], String...)} which returns each results as a Object[].
 * 
 * <p>
 * You must access the results one of three ways using this object. You can call the {@link #getResults()} method which
 * will extract all results into a list which is returned, you can get the first result only with
 * {@link #getFirstResult()}, or you can call the {@link #iterator()} method either directly or with the for... Java
 * statement. The iterator allows you to page through the results and is more appropriate for queries which will return
 * a large number of results.
 * </p>
 * 
 * <p>
 * <b>NOTE:</b> If you access the {@link #iterator()} method, you must call {@link CloseableIterator#close()} method
 * when you are done otherwise the underlying SQL statement and connection may be kept open.
 * </p>
 * 
 * @author graywatson
 */
public interface GenericRawResults<T> extends CloseableWrappedIterable<T> {

	/**
	 * Return the number of columns in each result row.
	 */
	public int getNumberColumns();

	/**
	 * Return the array of column names for each result row.
	 */
	public String[] getColumnNames();

	/**
	 * Return a list of all of the results. For large queries, this should not be used since the {@link #iterator()}
	 * method will allow your to process the results page-by-page.
	 */
	public List<T> getResults() throws SQLException;

	/**
	 * Return the first result only. This should be used if you are only expecting one result. It cannot be used in
	 * conjunction with {@link #getResults()}.
	 * 
	 * @return null if there are no results.
	 */
	public T getFirstResult() throws SQLException;

	/**
	 * Close any open database connections associated with the GenericRawResults. This is only necessary if the
	 * {@link Dao#iterator()} or another iterator method was called.
	 */
	@Override
	public void close() throws IOException;
}
