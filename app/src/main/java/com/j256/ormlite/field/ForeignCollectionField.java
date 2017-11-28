package com.j256.ormlite.field;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.dao.LazyForeignCollection;
import com.j256.ormlite.stmt.QueryBuilder;

/**
 * Annotation that identifies a {@link ForeignCollection} field in a class that corresponds to objects in a foreign
 * table that match the foreign-id of the current class.
 * 
 * <pre>
 * &#064;ForeignCollection(id = true)
 * private ForeignCollection&lt;Order&gt; orders;
 * </pre>
 * 
 * @author graywatson
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface ForeignCollectionField {

	/**
	 * @see #maxEagerLevel()
	 */
	public static final int DEFAULT_MAX_EAGER_LEVEL = 1;

	/**
	 * <p>
	 * Set to true if the collection is a an eager collection where all of the results should be retrieved when the
	 * parent object is retrieved. Default is false (lazy) when the results will not be retrieved until you ask for the
	 * iterator from the collection.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</b> If this is false (i.e. we have a lazy collection) then a connection is held open to the database as
	 * you iterate through the collection. This means that you need to make sure it is closed when you finish. See
	 * {@link LazyForeignCollection#iterator()} for more information.
	 * </p>
	 * 
	 * <p>
	 * <b>WARNING:</b> By default, if you have eager collections of objects that themselves have eager collections, the
	 * inner collection will be created as lazy for performance reasons. If you need change this see the
	 * {@link #maxEagerLevel()} setting below.
	 * </p>
	 */
	boolean eager() default false;

	/**
	 * Set this to be the number of times to expand an eager foreign collection's foreign collection. If you query for A
	 * and it has an eager foreign-collection of field B which has an eager foreign-collection of field C ..., then a
	 * lot of database operations are going to happen whenever you query for A. By default this value is 1 meaning that
	 * if you query for A, the collection of B will be eager fetched but each of the B objects will have a lazy
	 * collection instead of an eager collection of C. It should be increased only if you know what you are doing.
	 */
	int maxEagerLevel() default DEFAULT_MAX_EAGER_LEVEL;

	/**
	 * The name of the column. This is only used when you want to match the string passed to
	 * {@link Dao#getEmptyForeignCollection(String)} or when you want to specify it in
	 * {@link QueryBuilder#selectColumns(String...)}.
	 */
	String columnName() default "";

	/**
	 * The name of the column in the object that we should order by.
	 */
	String orderColumnName() default "";

	/**
	 * If an order column has been defined with {@link #orderColumnName()}, this sets the order as ascending (true, the
	 * default) or descending (false).
	 */
	boolean orderAscending() default true;

	/**
	 * Name of the _field_ (not the column name) in the class that the collection is holding that corresponds to the
	 * entity which holds the collection. This is needed if there are two foreign fields in the class in the collection
	 * (such as a tree structure) and you want to identify which column identifies the "owner" of the foreign class.
	 * This should be a field with the same type as the one which has the collection.
	 * 
	 * <p>
	 * <b>WARNING:</b> Due to some internal complexities, this it field/member name in the class and _not_ the
	 * column-name.
	 * </p>
	 */
	String foreignFieldName() default "";

	/*
	 * NOTE to developers: if you add fields here you have to add them to the DatabaseFieldConfigLoader,
	 * DatabaseFieldConfigLoaderTest, and DatabaseTableConfigUtil.
	 */
}
