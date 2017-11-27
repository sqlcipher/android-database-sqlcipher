package com.j256.ormlite.table;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.j256.ormlite.dao.DaoManager;

/**
 * Annotation that marks a class to be stored in the database. It is only required if you want to mark the class or
 * change its default tableName. You specify this annotation above the classes that you want to persist to the database.
 * For example:
 * 
 * <pre>
 * &#64;DatabaseTable(tableName = "accounts")
 * public class Account {
 *   ...
 * </pre>
 * 
 * <p>
 * <b>NOTE:</b> Classes that are persisted using this package <i>must</i> have a no-argument constructor with at least
 * package visibility so objects can be created when you do a query, etc..
 * </p>
 * 
 * @author graywatson
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface DatabaseTable {

	/**
	 * The name of the column in the database. If not set then the name is taken from the class name lowercased.
	 */
	String tableName() default "";

	/**
	 * The DAO class that corresponds to this class. This is used by the {@link DaoManager} when it constructs a DAO
	 * internally.
	 */
	Class<?> daoClass() default Void.class;
}
