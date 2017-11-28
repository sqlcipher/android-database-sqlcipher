package com.j256.ormlite.field.types;

import com.j256.ormlite.field.SqlType;

/**
 * Type that persists a byte primitive.
 * 
 * @author graywatson
 */
public class ByteType extends ByteObjectType {

	private static final ByteType singleTon = new ByteType();

	public static ByteType getSingleton() {
		return singleTon;
	}

	private ByteType() {
		super(SqlType.BYTE, new Class<?>[] { byte.class });
	}

	/**
	 * Here for others to subclass.
	 */
	protected ByteType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}
}
