package com.j256.ormlite.stmt;

import com.j256.ormlite.field.SqlType;

/**
 * Like {@link SelectArg} but using a {@link ThreadLocal} internally to improve reentrance so that multiple threads can
 * use the same compiled statement.
 * 
 * @author graywatson
 */
public class ThreadLocalSelectArg extends BaseArgumentHolder {

	private ThreadLocal<ValueWrapper> threadValue = new ThreadLocal<ValueWrapper>();

	public ThreadLocalSelectArg() {
		super();
		// value set later
	}

	public ThreadLocalSelectArg(String columnName, Object value) {
		super(columnName);
		setValue(value);
	}

	public ThreadLocalSelectArg(SqlType sqlType, Object value) {
		super(sqlType);
		setValue(value);
	}

	public ThreadLocalSelectArg(Object value) {
		setValue(value);
	}

	@Override
	protected Object getValue() {
		ValueWrapper wrapper = threadValue.get();
		if (wrapper == null) {
			return null;
		} else {
			return wrapper.value;
		}
	}

	@Override
	public void setValue(Object value) {
		threadValue.set(new ValueWrapper(value));
	}

	@Override
	protected boolean isValueSet() {
		return threadValue.get() != null;
	}

	/**
	 * Value wrapper so we can differentiate between a null value and no value. Since we need to do this on a per thread
	 * basis, this can't be a volatile field or something.
	 */
	private static class ValueWrapper {
		Object value;
		public ValueWrapper(Object value) {
			this.value = value;
		}
	}
}
