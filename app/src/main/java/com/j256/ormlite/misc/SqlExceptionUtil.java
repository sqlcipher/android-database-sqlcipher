package com.j256.ormlite.misc;

import java.sql.SQLException;

/**
 * Utility class to help with SQLException throwing.
 * 
 * @author graywatson
 */
public class SqlExceptionUtil {

	/**
	 * Should be used in a static context only.
	 */
	private SqlExceptionUtil() {
	}

	/**
	 * Convenience method to allow a cause. Grrrr.
	 */
	public static SQLException create(String message, Throwable cause) {
		SQLException sqlException;
		if (cause instanceof SQLException) {
			// if the cause is another SQLException, pass alot of the SQL state
			sqlException = new SQLException(message, ((SQLException) cause).getSQLState());
		} else {
			sqlException = new SQLException(message);
		}
		sqlException.initCause(cause);
		return sqlException;
	}
}
