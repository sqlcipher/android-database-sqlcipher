package com.j256.ormlite.support;

import java.lang.reflect.Constructor;
import java.sql.SQLException;

import com.j256.ormlite.misc.SqlExceptionUtil;

/**
 * Database connection proxy factory that uses reflection to create the proxied connection. The class in question should
 * have a constructor that takes a {@link DatabaseConnection} object.
 * 
 * @author graywatson
 */
public class ReflectionDatabaseConnectionProxyFactory implements DatabaseConnectionProxyFactory {

	private final Class<? extends DatabaseConnection> proxyClass;
	private final Constructor<? extends DatabaseConnection> constructor;

	/**
	 * Takes a proxy-class that will be used to instantiate an instance in {@link #createProxy(DatabaseConnection)}.
	 * 
	 * @throws IllegalArgumentException
	 *             if the class does not have a constructor that takes a {@link DatabaseConnection} object.
	 */
	public ReflectionDatabaseConnectionProxyFactory(Class<? extends DatabaseConnection> proxyClass)
			throws IllegalArgumentException {
		this.proxyClass = proxyClass;
		try {
			this.constructor = proxyClass.getConstructor(DatabaseConnection.class);
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not find constructor with DatabaseConnection argument in "
					+ proxyClass);
		}
	}

	@Override
	public DatabaseConnection createProxy(DatabaseConnection realConnection) throws SQLException {
		try {
			return constructor.newInstance(realConnection);
		} catch (Exception e) {
			throw SqlExceptionUtil.create("Could not create a new instance of " + proxyClass, e);
		}
	}
}
