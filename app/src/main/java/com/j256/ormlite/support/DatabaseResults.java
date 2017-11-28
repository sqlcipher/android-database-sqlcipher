package com.j256.ormlite.support;

import java.io.Closeable;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.j256.ormlite.dao.ObjectCache;

/**
 * A reduction of the SQL ResultSet so we can implement it outside of JDBC.
 * 
 * <p>
 * <b>NOTE:</b> In all cases, the columnIndex parameters are 0 based -- <i>not</i> 1 based like JDBC.
 * </p>
 * 
 * @author graywatson
 */
public interface DatabaseResults extends Closeable {

	/**
	 * Returns the number of columns in these results.
	 */
	public int getColumnCount() throws SQLException;

	/**
	 * Returns an array of column names.
	 */
	public String[] getColumnNames() throws SQLException;

	/**
	 * Moves to the first result. This may not work with the default iterator depending on your database.
	 * 
	 * @return true if there are more results to be processed.
	 */
	public boolean first() throws SQLException;

	/**
	 * Moves to the previous result. This may not work with the default iterator depending on your database.
	 * 
	 * @return true if there are more results to be processed.
	 */
	public boolean previous() throws SQLException;

	/**
	 * Moves to the next result.
	 * 
	 * @return true if there are more results to be processed.
	 */
	public boolean next() throws SQLException;

	/**
	 * Moves to the last result. This may not work with the default iterator depending on your database.
	 * 
	 * @return true if there are more results to be processed.
	 */
	public boolean last() throws SQLException;

	/**
	 * Moves forward (positive value) or backwards (negative value) the list of results. moveRelative(1) should be the
	 * same as {@link #next()}. moveRelative(-1) is the same as {@link #previous} result. This may not work with the
	 * default iterator depending on your database.
	 * 
	 * @param offset
	 *            Number of rows to move. Positive moves forward in the results. Negative moves backwards.
	 * @return true if there are more results to be processed.
	 */
	public boolean moveRelative(int offset) throws SQLException;

	/**
	 * Moves to an absolute position in the list of results. This may not work with the default iterator depending on
	 * your database.
	 * 
	 * @param position
	 *            Row number in the result list to move to.
	 * @return true if there are more results to be processed.
	 */
	public boolean moveAbsolute(int position) throws SQLException;

	/**
	 * Returns the column index associated with the column name.
	 * 
	 * @throws SQLException
	 *             if the column was not found in the results.
	 */
	public int findColumn(String columnName) throws SQLException;

	/**
	 * Returns the string from the results at the column index.
	 */
	public String getString(int columnIndex) throws SQLException;

	/**
	 * Returns the boolean value from the results at the column index.
	 */
	public boolean getBoolean(int columnIndex) throws SQLException;

	/**
	 * Returns the char value from the results at the column index.
	 */
	public char getChar(int columnIndex) throws SQLException;

	/**
	 * Returns the byte value from the results at the column index.
	 */
	public byte getByte(int columnIndex) throws SQLException;

	/**
	 * Returns the byte array value from the results at the column index.
	 */
	public byte[] getBytes(int columnIndex) throws SQLException;

	/**
	 * Returns the short value from the results at the column index.
	 */
	public short getShort(int columnIndex) throws SQLException;

	/**
	 * Returns the integer value from the results at the column index.
	 */
	public int getInt(int columnIndex) throws SQLException;

	/**
	 * Returns the long value from the results at the column index.
	 */
	public long getLong(int columnIndex) throws SQLException;

	/**
	 * Returns the float value from the results at the column index.
	 */
	public float getFloat(int columnIndex) throws SQLException;

	/**
	 * Returns the double value from the results at the column index.
	 */
	public double getDouble(int columnIndex) throws SQLException;

	/**
	 * Returns the SQL timestamp value from the results at the column index.
	 */
	public Timestamp getTimestamp(int columnIndex) throws SQLException;

	/**
	 * Returns an input stream for a blob value from the results at the column index.
	 */
	public InputStream getBlobStream(int columnIndex) throws SQLException;

	/**
	 * Returns the SQL big decimal value from the results at the column index.
	 */
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException;

	/**
	 * Returns the SQL object value from the results at the column index.
	 */
	public Object getObject(int columnIndex) throws SQLException;

	/**
	 * Returns true if the last object returned with the column index is null.
	 */
	public boolean wasNull(int columnIndex) throws SQLException;

	/**
	 * Returns the object cache for looking up objects associated with these results or null if none.
	 */
	public ObjectCache getObjectCacheForRetrieve();

	/**
	 * Returns the object cache for storing objects generated by these results or null if none.
	 */
	public ObjectCache getObjectCacheForStore();

	/**
	 * Closes any underlying database connections but swallows any SQLExceptions.
	 */
	public void closeQuietly();
}
