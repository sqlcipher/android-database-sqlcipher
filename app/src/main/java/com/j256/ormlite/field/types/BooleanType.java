package com.j256.ormlite.field.types;

import com.j256.ormlite.field.SqlType;

/**
 * Type that persists a boolean primitive.
 * 
 * @author graywatson
 */
public class BooleanType extends BooleanObjectType {

	private static final BooleanType singleTon = new BooleanType();

	public static BooleanType getSingleton() {
		return singleTon;
	}

	private BooleanType() {
		super(SqlType.BOOLEAN, new Class<?>[] { boolean.class });
	}

	protected BooleanType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	protected BooleanType(SqlType sqlType) {
		super(sqlType);
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}
}
