package com.j256.ormlite.stmt;

import com.j256.ormlite.field.SqlType;

/**
 * An argument to a select SQL statement. After the query is constructed, the caller can set the value on this argument
 * and run the query. Then the argument can be set again and the query re-executed. This is equivalent in SQL to a ?
 * argument.
 * 
 * <p>
 * NOTE: If the argument has not been set by the time the query is executed, an exception will be thrown.
 * </p>
 * 
 * <p>
 * NOTE: For protections sake, the object cannot be reused with different column names.
 * </p>
 * 
 * @author graywatson
 */
public class SelectArg extends BaseArgumentHolder {

	private boolean hasBeenSet = false;
	private Object value = null;

	/**
	 * Constructor for when the value will be set later with {@link #setValue(Object)}.
	 */
	public SelectArg() {
		super();
		// value set later
	}

	/**
	 * This constructor is only necessary if you are using the {@link Where#raw(String, ArgumentHolder...)} and similar
	 * methods.
	 * 
	 * @param columnName
	 *            Name of the column this argument corresponds to.
	 * @param value
	 *            Value for the select-arg if know at time of construction. Otherwise call {@link #setValue(Object)}
	 *            later.
	 */
	public SelectArg(String columnName, Object value) {
		super(columnName);
		setValue(value);
	}

	/**
	 * This constructor is only necessary if you are using the {@link Where#raw(String, ArgumentHolder...)} and similar
	 * methods.
	 * 
	 * @param sqlType
	 *            Type of the column that this argument corresponds to. Only necessary if you are using the
	 *            {@link Where#raw(String, ArgumentHolder...)} and similar methods.
	 * @param value
	 *            Value for the select-arg if know at time of construction. Otherwise call {@link #setValue(Object)}
	 *            later.
	 */
	public SelectArg(SqlType sqlType, Object value) {
		super(sqlType);
		setValue(value);
	}

	/**
	 * This constructor is only necessary if you are using the {@link Where#raw(String, ArgumentHolder...)} and similar
	 * methods.
	 * 
	 * @param sqlType
	 *            Type of the column that this argument corresponds to. Only necessary if you are using the
	 *            {@link Where#raw(String, ArgumentHolder...)} and similar methods.
	 */
	public SelectArg(SqlType sqlType) {
		super(sqlType);
	}

	/**
	 * Constructor for when the value is known at time of construction. You can instead use the {@link #SelectArg()}
	 * empty constructor and set the value later with {@link #setValue(Object)}.
	 * 
	 * <p>
	 * <b>WARNING,</b> This constructor sets the _value_ not the column-name. To set the column-name only, use the
	 * {@link #SelectArg(String, Object)} and pass a null as the value.
	 * </p>
	 */
	public SelectArg(Object value) {
		setValue(value);
	}

	@Override
	protected Object getValue() {
		return value;
	}

	@Override
	public void setValue(Object value) {
		this.hasBeenSet = true;
		this.value = value;
	}

	@Override
	protected boolean isValueSet() {
		return hasBeenSet;
	}
}
