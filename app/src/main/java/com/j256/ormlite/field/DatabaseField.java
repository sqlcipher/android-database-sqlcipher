package com.j256.ormlite.field;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.types.VoidType;

/**
 * <p>
 * Annotation that identifies a field in a class that corresponds to a column in the database and will be persisted.
 * Fields that are not to be persisted such as transient or other temporary fields probably should be ignored. For
 * example:
 * </p>
 * 
 * <pre>
 * &#064;DatabaseField(id = true)
 * private String name;
 * 
 * &#064;DatabaseField(columnName = &quot;passwd&quot;, canBeNull = false)
 * private String password;
 * </pre>
 * 
 * <p>
 * <b> WARNING:</b> If you add any extra fields here, you will need to add them to {@link DatabaseFieldConfig},
 * {@link DatabaseFieldConfigLoader}, DatabaseFieldConfigLoaderTest, and DatabaseTableConfigUtil as well.
 * </p>
 * 
 * @author graywatson
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface DatabaseField {

	/** this special string is used as a .equals check to see if no default was specified */
	public static final String DEFAULT_STRING = "__ormlite__ no default value string was specified";

	/**
	 * Default for the maxForeignAutoRefreshLevel.
	 * 
	 * @see #maxForeignAutoRefreshLevel()
	 */
	public static final int DEFAULT_MAX_FOREIGN_AUTO_REFRESH_LEVEL = 2;

	/**
	 * The name of the column in the database. If not set then the name is taken from the field name.
	 */
	String columnName() default "";

	/**
	 * The DataType associated with the field. If not set then the Java class of the field is used to match with the
	 * appropriate DataType. This should only be set if you are overriding the default database type or if the field
	 * cannot be automatically determined (ex: byte[]).
	 */
	DataType dataType() default DataType.UNKNOWN;

	/**
	 * The default value of the field for creating the table. Default is none.
	 * 
	 * <p>
	 * <b>NOTE:</b> If the field has a null value then this value will be inserted in its place when you call you call
	 * {@link Dao#create(Object)}. This does not apply to primitive fields so you should just assign them in the class
	 * instead.
	 * </p>
	 */
	String defaultValue() default DEFAULT_STRING;

	/**
	 * Width of array fields (often for strings). Default is 0 which means to take the data-type and database-specific
	 * default. For strings that means 255 characters although some databases do not support this.
	 */
	int width() default 0;

	/**
	 * Whether the field can be assigned to null or have no value. Default is true.
	 */
	boolean canBeNull() default true;

	/**
	 * Whether the field is the id field or not. Default is false. Only one field can have this set in a class. If you
	 * don't have it set then you won't be able to use the query, update, and delete by ID methods. Only one of this,
	 * {@link #generatedId}, and {@link #generatedIdSequence} can be specified.
	 */
	boolean id() default false;

	/**
	 * Whether the field is an auto-generated id field. Default is false. With databases for which
	 * {@link DatabaseType#isIdSequenceNeeded} is true then this will cause the name of the sequence to be
	 * auto-generated. To specify the name of the sequence use {@link #generatedIdSequence}. Only one of this,
	 * {@link #id}, and {@link #generatedIdSequence} can be specified.
	 */
	boolean generatedId() default false;

	/**
	 * The name of the sequence number to be used to generate this value. Default is none. This is only necessary for
	 * database for which {@link DatabaseType#isIdSequenceNeeded} is true and you already have a defined sequence that
	 * you want to use. If you use {@link #generatedId} instead then the code will auto-generate a sequence name. Only
	 * one of this, {@link #id}, and {@link #generatedId} can be specified.
	 */
	String generatedIdSequence() default "";

	/**
	 * Field is a non-primitive object that corresponds to another class that is also stored in the database. It must
	 * have an id field (either {@link #id}, {@link #generatedId}, or {@link #generatedIdSequence} which will be stored
	 * in this table. When an object is returned from a query call, any foreign objects will just have the id field set
	 * in it. To get all of the other fields you will have to do a refresh on the object using its own Dao.
	 */
	boolean foreign() default false;

	/**
	 * <p>
	 * Package should use get...() and set...() to access the field value instead of the default direct field access via
	 * reflection. This may be necessary if the object you are storing has protections around it.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</b> The name of the get method <i>must</i> match getXxx() where Xxx is the name of the field with the
	 * first letter capitalized. The get <i>must</i> return a class which matches the field's. The set method
	 * <i>must</i> match setXxx(), have a single argument whose class matches the field's, and return void. For example:
	 * </p>
	 * 
	 * <pre>
	 * &#064;DatabaseField
	 * private Integer orderCount;
	 * 
	 * public Integer getOrderCount() {
	 * 	return orderCount;
	 * }
	 * 
	 * public void setOrderCount(Integer orderCount) {
	 * 	this.orderCount = orderCount;
	 * }
	 * </pre>
	 */
	boolean useGetSet() default false;

	/**
	 * If the field is an Enum and the database has a value that is not one of the names in the enum then this name will
	 * be used instead. It must match one of the enum names. This is mainly useful when you are worried about backwards
	 * compatibility with older database rows or future compatibility if you have to roll back to older data definition.
	 */
	String unknownEnumName() default "";

	/**
	 * If this is set to true (default false) then it will throw a SQLException if a null value is attempted to be
	 * de-persisted into a primitive. This must only be used on a primitive field. If this is false then if the database
	 * field is null, the value of the primitive will be set to 0.
	 */
	boolean throwIfNull() default false;

	/**
	 * Set this to be false (default true) to not store this field in the database. This is useful if you want to have
	 * the annotation on all of your fields but turn off the writing of some of them to the database.
	 */
	boolean persisted() default true;

	/**
	 * Optional format information that can be used by various field types. For example, if the Date is to be persisted
	 * as a string, this can set what format string to use for the date.
	 */
	String format() default "";

	/**
	 * Set this to be true (default false) to have the database insure that the column is unique to all rows in the
	 * table. Use this when you wan a field to be unique even if it is not the identify field. For example, if you have
	 * the firstName and lastName fields, both with unique=true and you have "Bob", "Smith" in the database, you cannot
	 * insert either "Bob", "Jones" or "Kevin", "Smith".
	 */
	boolean unique() default false;

	/**
	 * Set this to be true (default false) to have the database insure that _all_ of the columns marked with this as
	 * true will together be unique. For example, if you have the firstName and lastName fields, both with unique=true
	 * and you have "Bob", "Smith" in the database, you cannot insert another "Bob", "Smith" but you can insert "Bob",
	 * "Jones" and "Kevin", "Smith".
	 */
	boolean uniqueCombo() default false;

	/**
	 * Set this to be true (default false) to have the database add an index for this field. This will create an index
	 * with the name columnName + "_idx". To specify a specific name of the index or to index multiple fields, use
	 * {@link #indexName()}.
	 */
	boolean index() default false;

	/**
	 * Set this to be true (default false) to have the database add a unique index for this field. This is the same as
	 * the {@link #index()} field but this ensures that all of the values in the index are unique..
	 */
	boolean uniqueIndex() default false;

	/**
	 * Set this to be a string (default none) to have the database add an index for this field with this name. You do
	 * not need to specify the {@link #index()} boolean as well. To index multiple fields together in one index, each of
	 * the fields should have the same indexName value.
	 */
	String indexName() default "";

	/**
	 * Set this to be a string (default none) to have the database add a unique index for this field with this name.
	 * This is the same as the {@link #indexName()} field but this ensures that all of the values in the index are
	 * unique.
	 */
	String uniqueIndexName() default "";

	/**
	 * Set this to be true (default false) to have a foreign field automagically refreshed when an object is queried.
	 * This will _not_ automagically create the foreign object but when the object is queried, a separate database call
	 * will be made to load of the fields of the foreign object via an internal DAO. The default is to just have the ID
	 * field in the object retrieved and for the caller to call refresh on the correct DAO.
	 */
	boolean foreignAutoRefresh() default false;

	/**
	 * Set this to be the number of times to refresh a foreign object's foreign object. If you query for A and it has an
	 * foreign field B which has an foreign field C ..., then querying for A could get expensive. Setting this value to
	 * 1 will mean that when you query for A, B will be auto-refreshed, but C will just have its id field set. This also
	 * works if A has an auto-refresh field B which has an auto-refresh field A.
	 * 
	 * <p>
	 * <b>NOTE:</b> Increasing this value will result in more database transactions whenever you query for A, so use
	 * carefully.
	 * </p>
	 */
	int maxForeignAutoRefreshLevel() default DEFAULT_MAX_FOREIGN_AUTO_REFRESH_LEVEL;

	/**
	 * Allows you to set a custom persister class to handle this field. This class must have a getSingleton() static
	 * method defined which will return the singleton persister.
	 * 
	 * @see DataPersister
	 */
	Class<? extends DataPersister> persisterClass() default VoidType.class;

	/**
	 * If this is set to true then inserting an object with the ID field already set (i.e. not null, 0) will not
	 * override it with a generated-id. If the field is null or 0 then the id will be generated. This is useful when you
	 * have a table where items sometimes have IDs and sometimes need them generated. This only works if the database
	 * supports this behavior and if {@link #generatedId()} is also true for the field.
	 */
	boolean allowGeneratedIdInsert() default false;

	/**
	 * Specify the SQL necessary to create this field in the database. This can be used if you need to tune the schema
	 * to enable some per-database feature or to override the default SQL generated. See {@link #fullColumnDefinition()}
	 * .
	 */
	String columnDefinition() default "";

	/**
	 * <p>
	 * Set this to be true (default false) to have the foreign field will be automagically created using its internal
	 * DAO if the ID field is not set (null or 0). So when you call dao.create() on the parent object, any field with
	 * this set to true will possibly be created via an internal DAO. By default you have to create the object using its
	 * DAO directly. This only works if {@link #generatedId()} is also set to true.
	 * </p>
	 * 
	 * <pre>
	 * Order order1 = new Order();
	 * // account1 has not been created in the db yet and it's id == null
	 * order1.account = account1;
	 * // this will create order1 _and_ pass order1.account to the internal account dao.create().
	 * orderDao.create(order1);
	 * </pre>
	 */
	boolean foreignAutoCreate() default false;

	/**
	 * Set this to be true (default false) to have this field to be a version field similar to
	 * javax.persistence.Version. When an update is done on a row the following happens:
	 * 
	 * <ul>
	 * <li>The update statement is augmented with a "WHERE version = current-value"</li>
	 * <li>The new value being updated is the current-value + 1 or the current Date</li>
	 * <li>If the row has been updated by another entity then the update will not change the row and 0 rows changed will
	 * be returned.</li>
	 * <li>If a row was changed then the object is changed so the version field gets the new value</li>
	 * </ul>
	 * 
	 * The field should be a short, integer, long, Date, Date-string, or Date-long type.
	 */
	boolean version() default false;

	/**
	 * Name of the foreign object's field that is tied to this table. This does not need to be specified if you are
	 * using the ID of the foreign object which is recommended. For example, if you have an Order object with a foreign
	 * Account then you may want to key off of the Account name instead of the Account ID.
	 * 
	 * <p>
	 * <b>NOTE:</b> Setting this implies {@link #foreignAutoRefresh()} is also set to true because there is no way to
	 * refresh the object since the id field is not stored in the database. So when this is set, the field will be
	 * automatically refreshed in another database query.
	 * </p>
	 */
	String foreignColumnName() default "";

	/**
	 * Set this to be true (default false) if this field is a read-only field. This field will be returned by queries
	 * however it will be ignored during insert/create statements.
	 */
	boolean readOnly() default false;

	/**
	 * Specify the SQL necessary to create this field in the database including the column name, which should be
	 * properly escaped and in proper case depending on your database type. This can be used if you need to fully
	 * describe the schema to enable some per-database feature or to override the default SQL generated. The
	 * {@link #columnDefinition()} should be used instead of this unless your database type needs to wrap the field name
	 * somehow when defining the field.
	 */
	String fullColumnDefinition() default "";

	/*
	 * NOTE to developers: if you add fields here you have to add them to the DatabaseFieldConfig,
	 * DatabaseFieldConfigLoader, DatabaseFieldConfigLoaderTest, and DatabaseTableConfigUtil.
	 */
}
