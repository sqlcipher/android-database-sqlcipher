package com.j256.ormlite.field.types;

import java.math.BigInteger;
import java.sql.SQLException;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Type that persists a {@link BigInteger} object.
 * 
 * @author graywatson
 */
public class BigIntegerType extends BaseDataType {

	public static int DEFAULT_WIDTH = 255;

	private static final BigIntegerType singleTon = new BigIntegerType();

	public static BigIntegerType getSingleton() {
		return singleTon;
	}

	protected BigIntegerType() {
		super(SqlType.STRING, new Class<?>[] { BigInteger.class });
	}

	/**
	 * Here for others to subclass.
	 */
	protected BigIntegerType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	@Override
	public Object parseDefaultString(FieldType fieldType, String defaultStr) throws SQLException {
		try {
			return new BigInteger(defaultStr).toString();
		} catch (IllegalArgumentException e) {
			throw SqlExceptionUtil.create(
					"Problems with field " + fieldType + " parsing default BigInteger string '" + defaultStr + "'", e);
		}
	}

	@Override
	public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
		return results.getString(columnPos);
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
		try {
			return new BigInteger((String) sqlArg);
		} catch (IllegalArgumentException e) {
			throw SqlExceptionUtil
					.create("Problems with column " + columnPos + " parsing BigInteger string '" + sqlArg + "'", e);
		}
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object obj) {
		BigInteger bigInteger = (BigInteger) obj;
		return bigInteger.toString();
	}

	@Override
	public Object moveToNextValue(Object currentValue) {
		if (currentValue == null) {
			return BigInteger.ONE;
		} else {
			return ((BigInteger) currentValue).add(BigInteger.ONE);
		}
	}

	@Override
	public Object convertIdNumber(Number number) {
		return BigInteger.valueOf(number.longValue());
	}

	@Override
	public boolean isValidGeneratedType() {
		return true;
	}

	@Override
	public boolean isValidForVersion() {
		return true;
	}
}
