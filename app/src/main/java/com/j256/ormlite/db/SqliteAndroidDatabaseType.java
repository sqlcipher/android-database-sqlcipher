package com.j256.ormlite.db;

import java.sql.SQLException;

import com.j256.ormlite.android.DatabaseTableConfigUtil;
import com.j256.ormlite.field.DataPersister;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.types.DateStringType;
import com.j256.ormlite.field.types.SqlDateStringType;
import com.j256.ormlite.field.types.SqlDateType;
import com.j256.ormlite.field.types.TimeStampStringType;
import com.j256.ormlite.field.types.TimeStampType;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

/**
 * Sqlite database type information for the Android OS that makes native calls to the Android OS database APIs.
 * 
 * <b>NOTE:</b> This database-type maps any persisted dates into specific string formats. If you are using a custom date
 * persister, you need to store the value as a string and not as a SQL date type.
 * 
 * @author graywatson
 */
public class SqliteAndroidDatabaseType extends BaseSqliteDatabaseType {

	@Override
	public void loadDriver() {
		// noop
	}

	@Override
	public boolean isDatabaseUrlThisType(String url, String dbTypePart) {
		// not used by the android code
		return true;
	}

	@Override
	protected String getDriverClassName() {
		// no driver to load in android-land
		return null;
	}

	@Override
	public String getDatabaseName() {
		return "Android SQLite";
	}

	@Override
	protected void appendDateType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		// default is to store the date as a string
		appendStringType(sb, fieldType, fieldWidth);
	}

	@Override
	public void appendEscapedEntityName(StringBuilder sb, String name) {
		/*
		 * Android Sqlite doesn't seem to be able to handle the 'foo'.'bar' form of the column name which is for
		 * database.table I think so we just default to be single quoted like before.
		 */
		sb.append('`').append(name).append('`');
	}

	@Override
	protected void appendBooleanType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		// we have to convert booleans to numbers
		appendShortType(sb, fieldType, fieldWidth);
	}

	@Override
	public DataPersister getDataPersister(DataPersister defaultPersister, FieldType fieldType) {
		if (defaultPersister == null) {
			return super.getDataPersister(defaultPersister, fieldType);
		}
		// we are only overriding certain types
		switch (defaultPersister.getSqlType()) {
			case DATE:
				/*
				 * We need to map the dates into their string equivalents because of mapping issues with Sqlite's
				 * default date string formats.
				 */
				if (defaultPersister instanceof TimeStampType) {
					return TimeStampStringType.getSingleton();
				} else if (defaultPersister instanceof SqlDateType) {
					return SqlDateStringType.getSingleton();
				} else {
					return DateStringType.getSingleton();
				}
			default:
				return super.getDataPersister(defaultPersister, fieldType);
		}
	}

	@Override
	public boolean isNestedSavePointsSupported() {
		return false;
	}

	@Override
	public boolean isBatchUseTransaction() {
		return true;
	}

	@Override
	public <T> DatabaseTableConfig<T> extractDatabaseTableConfig(ConnectionSource connectionSource, Class<T> clazz)
			throws SQLException {
		return DatabaseTableConfigUtil.fromClass(connectionSource, clazz);
	}
}
