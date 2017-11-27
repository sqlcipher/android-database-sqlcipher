package com.j256.ormlite.dao;

import java.io.Closeable;
import java.sql.SQLException;
import java.util.Iterator;

import com.j256.ormlite.support.DatabaseResults;

/**
 * Extension to Iterator to provide a close() method. This should be in the JDK.
 * 
 * <p>
 * <b>NOTE:</b> You must call {@link CloseableIterator#close()} method when you are done otherwise the underlying SQL
 * statement and connection may be kept open.
 * </p>
 * 
 * @author graywatson
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {

	/**
	 * Close any underlying SQL statements but swallow any SQLExceptions.
	 */
	public void closeQuietly();

	/**
	 * Return the underlying database results object if any. May return null. This should not be used unless you know
	 * what you are doing. For example, if you shift the cursor in the raw-results, this could cause problems when you
	 * come back here to go the next result.
	 */
	public DatabaseResults getRawResults();

	/**
	 * Move to the next item in the iterator without calling {@link #next()}.
	 */
	public void moveToNext();

	/**
	 * Move to the first result and return it or null if none. This may not work with the default iterator depending on
	 * your database.
	 */
	public T first() throws SQLException;

	/**
	 * Moves to the previous result and return it or null if none. This may not work with the default iterator depending
	 * on your database.
	 */
	public T previous() throws SQLException;

	/**
	 * Return the current result object that we have accessed or null if none.
	 */
	public T current() throws SQLException;

	/**
	 * Returns the {@link #next()} object in the table or null if none.
	 * 
	 * @throws SQLException
	 *             Throws a SQLException on error since {@link #next()} cannot throw because it is part of the
	 *             {@link Iterator} definition. It will <i>not</i> throw if there is no next.
	 */
	public T nextThrow() throws SQLException;

	/**
	 * Move a relative position in the list and return that result or null if none. Moves forward (positive value) or
	 * backwards (negative value) the list of results. moveRelative(1) should be the same as {@link #next()}.
	 * moveRelative(-1) is the same as {@link #previous} result. This may not work with the default iterator depending
	 * on your database.
	 * 
	 * @param offset
	 *            Number of rows to move. Positive moves forward in the results. Negative moves backwards.
	 * @return The object at the new position or null of none.
	 */
	public T moveRelative(int offset) throws SQLException;

	/**
	 * Move to an absolute position in the list and return that result or null if none. This may not be supported
	 * depending on your database or depending on the type of iterator and where you are moving. For example, in
	 * JDBC-land, often the iterator created can only move forward and will throw an exception if you attempt to move to
	 * an absolute position behind the current position.
	 * 
	 * @param position
	 *            Row number in the result list to move to.
	 * @return The object at the new position or null of none.
	 */
	public T moveAbsolute(int position) throws SQLException;
}
