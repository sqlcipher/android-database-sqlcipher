package com.j256.ormlite.field.types;

import java.lang.reflect.Field;
import java.sql.Timestamp;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;

/**
 * Type that persists a {@link java.sql.Date} object.
 * 
 * <p>
 * NOTE: This is <i>not</i> the same as the {@link java.util.Date} class handled with {@link DateType}. If it
 * recommended that you use the other Date class which is more standard to Java programs.
 * </p>
 * 
 * @author graywatson
 */
public class SqlDateType extends DateType {

	private static final SqlDateType singleTon = new SqlDateType();
	private static final DateStringFormatConfig sqlDateFormatConfig = new DateStringFormatConfig("yyyy-MM-dd");

	public static SqlDateType getSingleton() {
		return singleTon;
	}

	private SqlDateType() {
		super(SqlType.DATE, new Class<?>[] { java.sql.Date.class });
	}

	/**
	 * Here for others to subclass.
	 */
	protected SqlDateType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
		Timestamp value = (Timestamp) sqlArg;
		return new java.sql.Date(value.getTime());
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
		java.sql.Date date = (java.sql.Date) javaObject;
		return new Timestamp(date.getTime());
	}

	@Override
	protected DateStringFormatConfig getDefaultDateFormatConfig() {
		return sqlDateFormatConfig;
	}

	@Override
	public boolean isValidForField(Field field) {
		return (field.getType() == java.sql.Date.class);
	}
}
