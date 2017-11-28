package com.j256.ormlite.field.types;

import java.sql.SQLException;
import java.util.UUID;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Type that persists a {@link UUID} object using a database String. This is not to be confused with the native UUID
 * types supported by some databases.
 * 
 * @author graywatson
 */
public class UuidType extends BaseDataType {

	public static int DEFAULT_WIDTH = 48;

	private static final UuidType singleTon = new UuidType();

	public static UuidType getSingleton() {
		return singleTon;
	}

	private UuidType() {
		super(SqlType.STRING, new Class<?>[] { UUID.class });
	}

	protected UuidType(SqlType sqlType) {
		super(sqlType);
	}

	/**
	 * Here for others to subclass.
	 */
	protected UuidType(SqlType sqlType, Class<?>[] classes) {
		super(sqlType, classes);
	}

	@Override
	public Object parseDefaultString(FieldType fieldType, String defaultStr) {
		return defaultStr;
	}

	@Override
	public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
		return results.getString(columnPos);
	}

	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
		String uuidStr = (String) sqlArg;
		try {
			return java.util.UUID.fromString(uuidStr);
		} catch (IllegalArgumentException e) {
			throw SqlExceptionUtil
					.create("Problems with column " + columnPos + " parsing UUID-string '" + uuidStr + "'", e);
		}
	}

	@Override
	public Object javaToSqlArg(FieldType fieldType, Object obj) {
		UUID uuid = (UUID) obj;
		return uuid.toString();
	}

	@Override
	public boolean isValidGeneratedType() {
		return true;
	}

	@Override
	public boolean isSelfGeneratedId() {
		return true;
	}

	@Override
	public Object generateId() {
		return java.util.UUID.randomUUID();
	}

	@Override
	public int getDefaultWidth() {
		return DEFAULT_WIDTH;
	}
}
