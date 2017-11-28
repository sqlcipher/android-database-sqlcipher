package com.j256.ormlite.field.types;

import java.util.UUID;

import com.j256.ormlite.field.SqlType;

/**
 * Type that persists a {@link UUID} object but as a UUID type which is supported by a couple of database-types.
 * 
 * @author graywatson
 */
public class NativeUuidType extends UuidType {

	private static final NativeUuidType singleTon = new NativeUuidType();

	public static NativeUuidType getSingleton() {
		return singleTon;
	}

	private NativeUuidType() {
		super(SqlType.UUID);
	}

	/**
	 * Here for others to subclass.
	 */
	protected NativeUuidType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}
}
