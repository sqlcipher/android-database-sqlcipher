package com.j256.ormlite.table;

import java.lang.reflect.Constructor;
import java.sql.SQLException;

import com.j256.ormlite.dao.Dao;

/**
 * Interface that allows you to inject a factory class that creates objects of this class. You set it on the DAO using:
 * {@link Dao#setObjectFactory(ObjectFactory)}.
 * 
 * @author graywatson
 */
public interface ObjectFactory<T> {

	/**
	 * Construct and return an object of a certain class.
	 * 
	 * @throws SQLException
	 *             if there was a problem creating the object.
	 */
	public T createObject(Constructor<T> construcor, Class<T> dataClass) throws SQLException;
}
