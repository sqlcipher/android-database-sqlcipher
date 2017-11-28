package com.j256.ormlite.field.types;

import java.math.BigDecimal;
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
public class BigDecimalStringType extends BaseDataType {

	public static int DEFAULT_WIDTH = 255;

	private static final BigDecimalStringType singleTon = new BigDecimalStringType();

	public static BigDecimalStringType getSingleton() {
		return singleTon;
	}

	private BigDecimalStringType() {
		super(SqlType.STRING, new Class<?>[] { BigDecimal.class });
	}

	/**
	 * Here for others to subclass.
	 */
	protected BigDecimalStringType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	@Override
	public Object parseDefaultString(FieldType fieldType, String defaultStr) throws SQLException {
		try {
			return new BigDecimal(defaultStr).toString();
		} catch (IllegalArgumentException e) {
			throw SqlExceptionUtil.create("Problems with field " + fieldType + " parsing default BigDecimal string '"
					+ defaultStr + "'", e);
		}
	}

	@Override
	public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
		return results.getString(columnPos);
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
		try {
			return new BigDecimal((String) sqlArg);
		} catch (IllegalArgumentException e) {
			throw SqlExceptionUtil.create("Problems with column " + columnPos + " parsing BigDecimal string '" + sqlArg
					+ "'", e);
		}
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object obj) {
		BigDecimal bigInteger = (BigDecimal) obj;
		return bigInteger.toString();
	}

	@Override
	public int getDefaultWidth() {
		return DEFAULT_WIDTH;
	}

	@Override
	public boolean isAppropriateId() {
		return false;
	}
}
