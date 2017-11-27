package com.j256.ormlite.field.types;

import java.lang.reflect.Field;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;

/**
 * Type that persists a {@link java.sql.Timestamp} object.
 * 
 * @author graywatson
 */
public class TimeStampType extends DateType {

	private static final TimeStampType singleTon = new TimeStampType();

	public static TimeStampType getSingleton() {
		return singleTon;
	}

	private TimeStampType() {
		super(SqlType.DATE, new Class<?>[] { java.sql.Timestamp.class });
	}

	/**
	 * Here for others to subclass.
	 */
	protected TimeStampType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
		// noop pass-thru
		return sqlArg;
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
		// noop pass-thru
		return javaObject;
	}

	@Override
	public boolean isValidForField(Field field) {
		return (field.getType() == java.sql.Timestamp.class);
	}

	@Override
	public Object moveToNextValue(Object currentValue) {
		long newVal = System.currentTimeMillis();
		if (currentValue == null) {
			return new java.sql.Timestamp(newVal);
		} else if (newVal == ((java.sql.Timestamp) currentValue).getTime()) {
			return new java.sql.Timestamp(newVal + 1L);
		} else {
			return new java.sql.Timestamp(newVal);
		}
	}
}
