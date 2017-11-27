package com.j256.ormlite.field.types;

import java.sql.SQLException;
import java.util.Date;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Persists the {@link java.util.Date} Java class as integer seconds (not milliseconds) since epoch.
 *
 * <p>
 * NOTE: This is <i>not</i> the same as the {@link java.sql.Date} class.
 * </p>
 *
 * @author Noor Dawod, noor@fineswap.com
 * @since September 9, 2016
 */
public class DateIntegerType extends BaseDateType {

	private static final DateIntegerType singleTon = new DateIntegerType();

	public static DateIntegerType getSingleton() {
		return singleTon;
	}

	private DateIntegerType() {
		super(SqlType.INTEGER);
	}

	/**
	 * Here for others to subclass.
	 */
	protected DateIntegerType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	@Override
	public Object parseDefaultString(FieldType fieldType, String defaultStr) throws SQLException {
		try {
			return Integer.parseInt(defaultStr);
		} catch (NumberFormatException e) {
			throw SqlExceptionUtil.create(
					"Problems with field " + fieldType + " parsing default date-integer value: " + defaultStr, e);
		}
	}

	@Override
	public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
		return results.getInt(columnPos);
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
		return new Date(((Integer) sqlArg) * 1000L);
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object obj) {
		Date date = (Date) obj;
		return (int) (date.getTime() / 1000);
	}

	@Override
	public boolean isEscapedValue() {
		return false;
	}

	@Override
	public Class<?> getPrimaryClass() {
		return Date.class;
	}
}
