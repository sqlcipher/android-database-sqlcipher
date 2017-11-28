package com.j256.ormlite.field.types;

import java.sql.SQLException;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Booleans can be stored in the database as the integer column type and the value 1 (really non-0) for true and 0 for
 * false. You must choose this DataType specifically with the {@link DatabaseField#dataType()} specifier.
 * 
 * <pre>
 * &#64;DatabaseField(dataType = DataType.BOOLEAN_INTEGER)
 * </pre>
 * 
 * Thanks much to stew.
 * 
 * @author graywatson
 */
public class BooleanIntegerType extends BooleanType {

	private static final Integer TRUE_VALUE = Integer.valueOf(1);
	private static final Integer FALSE_VALUE = Integer.valueOf(0);

	private static final BooleanIntegerType singleTon = new BooleanIntegerType();

	public static BooleanIntegerType getSingleton() {
		return singleTon;
	}

	public BooleanIntegerType() {
		super(SqlType.INTEGER);
	}

	@Override
	public Object parseDefaultString(FieldType fieldType, String defaultStr) {
		return javaToSqlArg(fieldType, Boolean.parseBoolean(defaultStr));
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object obj) {
		return ((Boolean) obj ? TRUE_VALUE : FALSE_VALUE);
	}

	@Override
	public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
		return results.getInt(columnPos);
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
		return ((Integer) sqlArg == 0 ? Boolean.FALSE : Boolean.TRUE);
	}

	@Override
	public Object resultStringToJava(FieldType fieldType, String stringValue, int columnPos) {
		if (stringValue.length() == 0) {
			return Boolean.FALSE;
		} else {
			return sqlArgToJava(fieldType, Integer.parseInt(stringValue), columnPos);
		}
	}
}
