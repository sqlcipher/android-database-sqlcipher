package com.j256.ormlite.support;

import java.sql.SQLException;

/**
 * The holder of a generated key so we can return the value of generated keys from update methods.
 * 
 * @author graywatson
 */
public interface GeneratedKeyHolder {

	/**
	 * Add the key number on the key holder. May be called multiple times.
	 */
	public void addKey(Number key) throws SQLException;
}
