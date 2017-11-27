package com.j256.ormlite.field.types;

import java.sql.SQLException;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Type that persists a Long object.
 * 
 * @author graywatson
 */
public class LongObjectType extends BaseDataType {

	private static final LongObjectType singleTon = new LongObjectType();

	public static LongObjectType getSingleton() {
		return singleTon;
	}

	private LongObjectType() {
		super(SqlType.LONG, new Class<?>[] { Long.class });
	}

	protected LongObjectType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	@Override
	public Object parseDefaultString(FieldType fieldType, String defaultStr) {
		return Long.parseLong(defaultStr);
	}

	@Override
	public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
		return (Long) results.getLong(columnPos);
	}

	@Override
	public Object convertIdNumber(Number number) {
		return (Long) number.longValue();
	}

	@Override
	public boolean isEscapedValue() {
		return false;
	}

	@Override
	public boolean isValidGeneratedType() {
		return true;
	}

	@Override
	public boolean isValidForVersion() {
		return true;
	}

	@Override
	public Object moveToNextValue(Object currentValue) {
		if (currentValue == null) {
			return (Long) 1L;
		} else {
			return ((Long) currentValue) + 1L;
		}
	}
}
