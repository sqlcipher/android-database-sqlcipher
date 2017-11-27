package com.j256.ormlite.field.types;

import com.j256.ormlite.field.SqlType;

/**
 * Type that persists an enum as its string value produced by call @{link {@link Enum#toString()}. You can also use the
 * {@link EnumIntegerType}. If you want to use the {@link Enum#name()} instead, see the {@link EnumStringType}.
 * 
 * @author graywatson
 */
public class EnumToStringType extends EnumStringType {

	private static final EnumToStringType singleTon = new EnumToStringType();

	public static EnumToStringType getSingleton() {
		return singleTon;
	}

	private EnumToStringType() {
		super(SqlType.STRING, new Class<?>[] { Enum.class });
	}

	/**
	 * Here for others to subclass.
	 */
	protected EnumToStringType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	@Override
	protected String getEnumName(Enum<?> enumVal) {
		return enumVal.toString();
	}
}
