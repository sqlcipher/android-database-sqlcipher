package com.j256.ormlite.db;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import com.j256.ormlite.field.BaseFieldConverter;
import com.j256.ormlite.field.DataPersister;
import com.j256.ormlite.field.FieldConverter;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseResults;
import com.j256.ormlite.table.DatabaseTableConfig;

/**
 * Base class for all of the {@link DatabaseType} classes that provide the per-database type functionality to create
 * tables and build queries.
 *
 * <p>
 * Here's a good page which shows some of the <a href="http://troels.arvin.dk/db/rdbms/" >differences between SQL
 * databases</a>.
 * </p>
 *
 * @author graywatson
 */
public abstract class BaseDatabaseType implements DatabaseType {

	protected static String DEFAULT_SEQUENCE_SUFFIX = "_id_seq";
	protected Driver driver;

	/**
	 * Return the name of the driver class associated with this database type.
	 */
	protected abstract String getDriverClassName();

	@Override
	public void loadDriver() throws SQLException {
		String className = getDriverClassName();
		if (className != null) {
			// this instantiates the driver class which wires in the JDBC glue
			try {
				Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw SqlExceptionUtil.create("Driver class was not found for " + getDatabaseName()
						+ " database.  Missing jar with class " + className + ".", e);
			}
		}
	}

	@Override
	public void setDriver(Driver driver) {
		this.driver = driver;
	}

	@Override
	public void appendColumnArg(String tableName, StringBuilder sb, FieldType fieldType, List<String> additionalArgs,
			List<String> statementsBefore, List<String> statementsAfter, List<String> queriesAfter)
					throws SQLException {
		appendEscapedEntityName(sb, fieldType.getColumnName());
		sb.append(' ');
		DataPersister dataPersister = fieldType.getDataPersister();
		// first try the per-field width
		int fieldWidth = fieldType.getWidth();
		if (fieldWidth == 0) {
			// next try the per-data-type width
			fieldWidth = dataPersister.getDefaultWidth();
		}
		switch (dataPersister.getSqlType()) {

			case STRING:
				appendStringType(sb, fieldType, fieldWidth);
				break;

			case LONG_STRING:
				appendLongStringType(sb, fieldType, fieldWidth);
				break;

			case BOOLEAN:
				appendBooleanType(sb, fieldType, fieldWidth);
				break;

			case DATE:
				appendDateType(sb, fieldType, fieldWidth);
				break;

			case CHAR:
				appendCharType(sb, fieldType, fieldWidth);
				break;

			case BYTE:
				appendByteType(sb, fieldType, fieldWidth);
				break;

			case BYTE_ARRAY:
				appendByteArrayType(sb, fieldType, fieldWidth);
				break;

			case SHORT:
				appendShortType(sb, fieldType, fieldWidth);
				break;

			case INTEGER:
				appendIntegerType(sb, fieldType, fieldWidth);
				break;

			case LONG:
				appendLongType(sb, fieldType, fieldWidth);
				break;

			case FLOAT:
				appendFloatType(sb, fieldType, fieldWidth);
				break;

			case DOUBLE:
				appendDoubleType(sb, fieldType, fieldWidth);
				break;

			case SERIALIZABLE:
				appendSerializableType(sb, fieldType, fieldWidth);
				break;

			case BIG_DECIMAL:
				appendBigDecimalNumericType(sb, fieldType, fieldWidth);
				break;

			case UUID:
				appendUuidNativeType(sb, fieldType, fieldWidth);
				break;

			case OTHER:
				String sqlOtherType = dataPersister.getSqlOtherType();
				if (sqlOtherType != null) {
					sb.append(sqlOtherType);
				}
				break;

			case UNKNOWN:
			default:
				// shouldn't be able to get here unless we have a missing case
				throw new IllegalArgumentException("Unknown SQL-type " + dataPersister.getSqlType());
		}
		sb.append(' ');

		/*
		 * NOTE: the configure id methods must be in this order since isGeneratedIdSequence is also isGeneratedId and
		 * isId. isGeneratedId is also isId.
		 */
		if (fieldType.isGeneratedIdSequence() && !fieldType.isSelfGeneratedId()) {
			configureGeneratedIdSequence(sb, fieldType, statementsBefore, additionalArgs, queriesAfter);
		} else if (fieldType.isGeneratedId() && !fieldType.isSelfGeneratedId()) {
			configureGeneratedId(tableName, sb, fieldType, statementsBefore, statementsAfter, additionalArgs,
					queriesAfter);
		} else if (fieldType.isId()) {
			configureId(sb, fieldType, statementsBefore, additionalArgs, queriesAfter);
		}
		// if we have a generated-id then neither the not-null nor the default make sense and cause syntax errors
		if (!fieldType.isGeneratedId()) {
			Object defaultValue = fieldType.getDefaultValue();
			if (defaultValue != null) {
				sb.append("DEFAULT ");
				appendDefaultValue(sb, fieldType, defaultValue);
				sb.append(' ');
			}
			if (fieldType.isCanBeNull()) {
				appendCanBeNull(sb, fieldType);
			} else {
				sb.append("NOT NULL ");
			}
			if (fieldType.isUnique()) {
				addSingleUnique(sb, fieldType, additionalArgs, statementsAfter);
			}
		}
	}

	/**
	 * Output the SQL type for a Java String.
	 */
	protected void appendStringType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		if (isVarcharFieldWidthSupported()) {
			sb.append("VARCHAR(").append(fieldWidth).append(')');
		} else {
			sb.append("VARCHAR");
		}
	}

	/**
	 * Output the SQL type for a Java UUID. This is used to support specific sub-class database types which support the
	 * UUID type.
	 */
	protected void appendUuidNativeType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		throw new UnsupportedOperationException("UUID is not supported by this database type");
	}

	/**
	 * Output the SQL type for a Java Long String.
	 */
	protected void appendLongStringType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("TEXT");
	}

	/**
	 * Output the SQL type for a Java Date.
	 */
	protected void appendDateType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("TIMESTAMP");
	}

	/**
	 * Output the SQL type for a Java boolean.
	 */
	protected void appendBooleanType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("BOOLEAN");
	}

	/**
	 * Output the SQL type for a Java char.
	 */
	protected void appendCharType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("CHAR");
	}

	/**
	 * Output the SQL type for a Java byte.
	 */
	protected void appendByteType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("TINYINT");
	}

	/**
	 * Output the SQL type for a Java short.
	 */
	protected void appendShortType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("SMALLINT");
	}

	/**
	 * Output the SQL type for a Java integer.
	 */
	private void appendIntegerType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("INTEGER");
	}

	/**
	 * Output the SQL type for a Java long.
	 */
	protected void appendLongType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("BIGINT");
	}

	/**
	 * Output the SQL type for a Java float.
	 */
	private void appendFloatType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("FLOAT");
	}

	/**
	 * Output the SQL type for a Java double.
	 */
	private void appendDoubleType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("DOUBLE PRECISION");
	}

	/**
	 * Output the SQL type for either a serialized Java object or a byte[].
	 */
	protected void appendByteArrayType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("BLOB");
	}

	/**
	 * Output the SQL type for a serialized Java object.
	 */
	protected void appendSerializableType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("BLOB");
	}

	/**
	 * Output the SQL type for a BigDecimal object.
	 */
	protected void appendBigDecimalNumericType(StringBuilder sb, FieldType fieldType, int fieldWidth) {
		sb.append("NUMERIC");
	}

	/**
	 * Output the SQL type for the default value for the type.
	 */
	private void appendDefaultValue(StringBuilder sb, FieldType fieldType, Object defaultValue) {
		if (fieldType.isEscapedDefaultValue()) {
			appendEscapedWord(sb, defaultValue.toString());
		} else {
			sb.append(defaultValue);
		}
	}

	/**
	 * Output the SQL necessary to configure a generated-id column. This may add to the before statements list or
	 * additional arguments later.
	 *
	 * NOTE: Only one of configureGeneratedIdSequence, configureGeneratedId, or configureId will be called.
	 */
	protected void configureGeneratedIdSequence(StringBuilder sb, FieldType fieldType, List<String> statementsBefore,
			List<String> additionalArgs, List<String> queriesAfter) throws SQLException {
		throw new SQLException(
				"GeneratedIdSequence is not supported by database " + getDatabaseName() + " for field " + fieldType);
	}

	/**
	 * Output the SQL necessary to configure a generated-id column. This may add to the before statements list or
	 * additional arguments later.
	 *
	 * NOTE: Only one of configureGeneratedIdSequence, configureGeneratedId, or configureId will be called.
	 */
	protected void configureGeneratedId(String tableName, StringBuilder sb, FieldType fieldType,
			List<String> statementsBefore, List<String> statementsAfter, List<String> additionalArgs,
			List<String> queriesAfter) {
		throw new IllegalStateException(
				"GeneratedId is not supported by database " + getDatabaseName() + " for field " + fieldType);
	}

	/**
	 * Output the SQL necessary to configure an id column. This may add to the before statements list or additional
	 * arguments later.
	 *
	 * NOTE: Only one of configureGeneratedIdSequence, configureGeneratedId, or configureId will be called.
	 */
	protected void configureId(StringBuilder sb, FieldType fieldType, List<String> statementsBefore,
			List<String> additionalArgs, List<String> queriesAfter) {
		// default is noop since we do it at the end in appendPrimaryKeys()
	}

	@Override
	public void addPrimaryKeySql(FieldType[] fieldTypes, List<String> additionalArgs, List<String> statementsBefore,
			List<String> statementsAfter, List<String> queriesAfter) {
		StringBuilder sb = null;
		for (FieldType fieldType : fieldTypes) {
			if (fieldType.isGeneratedId() && !generatedIdSqlAtEnd() && !fieldType.isSelfGeneratedId()) {
				// don't add anything
			} else if (fieldType.isId()) {
				if (sb == null) {
					sb = new StringBuilder(48);
					sb.append("PRIMARY KEY (");
				} else {
					sb.append(',');
				}
				appendEscapedEntityName(sb, fieldType.getColumnName());
			}
		}
		if (sb != null) {
			sb.append(") ");
			additionalArgs.add(sb.toString());
		}
	}

	/**
	 * Return true if we should add generated-id SQL in the {@link #addPrimaryKeySql} method at the end. If false then
	 * it needs to be done by hand inline.
	 */
	protected boolean generatedIdSqlAtEnd() {
		return true;
	}

	@Override
	public void addUniqueComboSql(FieldType[] fieldTypes, List<String> additionalArgs, List<String> statementsBefore,
			List<String> statementsAfter, List<String> queriesAfter) {
		StringBuilder sb = null;
		for (FieldType fieldType : fieldTypes) {
			if (fieldType.isUniqueCombo()) {
				if (sb == null) {
					sb = new StringBuilder(48);
					sb.append("UNIQUE (");
				} else {
					sb.append(',');
				}
				appendEscapedEntityName(sb, fieldType.getColumnName());
			}
		}
		if (sb != null) {
			sb.append(") ");
			additionalArgs.add(sb.toString());
		}
	}

	@Override
	public void dropColumnArg(FieldType fieldType, List<String> statementsBefore, List<String> statementsAfter) {
		// by default this is a noop
	}

	@Override
	public void appendEscapedWord(StringBuilder sb, String word) {
		sb.append('\'').append(word).append('\'');
	}

	@Override
	public void appendEscapedEntityName(StringBuilder sb, String name) {
		sb.append('`');
		int dotPos = name.indexOf('.');
		if (dotPos > 0) {
			sb.append(name.substring(0, dotPos));
			sb.append("`.`");
			sb.append(name.substring(1 + dotPos));
		} else {
			sb.append(name);
		}
		sb.append('`');
	}

	@Override
	public String generateIdSequenceName(String tableName, FieldType idFieldType) {
		String name = tableName + DEFAULT_SEQUENCE_SUFFIX;
		if (isEntityNamesMustBeUpCase()) {
			return upCaseEntityName(name);
		} else {
			return name;
		}
	}

	@Override
	public String getCommentLinePrefix() {
		return "-- ";
	}

	@Override
	public DataPersister getDataPersister(DataPersister defaultPersister, FieldType fieldType) {
		// default is noop
		return defaultPersister;
	}

	@Override
	public FieldConverter getFieldConverter(DataPersister dataPersister, FieldType fieldType) {
		// default is to use the dataPersister itself
		return dataPersister;
	}

	@Override
	public boolean isIdSequenceNeeded() {
		return false;
	}

	@Override
	public boolean isVarcharFieldWidthSupported() {
		return true;
	}

	@Override
	public boolean isLimitSqlSupported() {
		return true;
	}

	@Override
	public boolean isOffsetSqlSupported() {
		return true;
	}

	@Override
	public boolean isOffsetLimitArgument() {
		return false;
	}

	@Override
	public boolean isLimitAfterSelect() {
		return false;
	}

	@Override
	public void appendLimitValue(StringBuilder sb, long limit, Long offset) {
		sb.append("LIMIT ").append(limit).append(' ');
	}

	@Override
	public void appendOffsetValue(StringBuilder sb, long offset) {
		sb.append("OFFSET ").append(offset).append(' ');
	}

	@Override
	public void appendSelectNextValFromSequence(StringBuilder sb, String sequenceName) {
		// noop by default.
	}

	@Override
	public void appendCreateTableSuffix(StringBuilder sb) {
		// noop by default.
	}

	@Override
	public boolean isCreateTableReturnsZero() {
		return true;
	}

	@Override
	public boolean isCreateTableReturnsNegative() {
		return false;
	}

	@Override
	public boolean isEntityNamesMustBeUpCase() {
		return false;
	}

	@Override
	public String upCaseEntityName(String entityName) {
		/*
		 * We are forcing the ENGLISH locale because of locale capitalization issues. In a couple of languages, the
		 * capital version of many letters is a letter that is incompatible with many SQL libraries. For example, in
		 * Turkish (Locale.forLanguageTag("tr-TR")), "i".toUpperCase() returns "İ" which is a dotted uppercase i.
		 */
		return entityName.toUpperCase(Locale.ENGLISH);
	}

	@Override
	public boolean isNestedSavePointsSupported() {
		return true;
	}

	@Override
	public String getPingStatement() {
		return "SELECT 1";
	}

	@Override
	public boolean isBatchUseTransaction() {
		return false;
	}

	@Override
	public boolean isTruncateSupported() {
		return false;
	}

	@Override
	public boolean isCreateIfNotExistsSupported() {
		return false;
	}

	@Override
	public boolean isCreateIndexIfNotExistsSupported() {
		return isCreateIfNotExistsSupported();
	}

	@Override
	public boolean isSelectSequenceBeforeInsert() {
		return false;
	}

	@Override
	public boolean isAllowGeneratedIdInsertSupported() {
		return true;
	}

	/**
	 * @throws SQLException
	 *             for sub classes.
	 */
	@Override
	public <T> DatabaseTableConfig<T> extractDatabaseTableConfig(ConnectionSource connectionSource, Class<T> clazz)
			throws SQLException {
		// default is no default extractor
		return null;
	}

	@Override
	public void appendInsertNoColumns(StringBuilder sb) {
		sb.append("() VALUES ()");
	}

	/**
	 * If the field can be nullable, do we need to add some sort of NULL SQL for the create table. By default it is a
	 * noop. This is necessary because MySQL has a auto default value for the TIMESTAMP type that required a default
	 * value otherwise it would stick in the current date automagically.
	 */
	private void appendCanBeNull(StringBuilder sb, FieldType fieldType) {
		// default is a noop
	}

	/**
	 * Add SQL to handle a unique=true field. THis is not for uniqueCombo=true.
	 */
	private void addSingleUnique(StringBuilder sb, FieldType fieldType, List<String> additionalArgs,
			List<String> statementsAfter) {
		StringBuilder alterSb = new StringBuilder();
		alterSb.append(" UNIQUE (");
		appendEscapedEntityName(alterSb, fieldType.getColumnName());
		alterSb.append(')');
		additionalArgs.add(alterSb.toString());
	}

	/**
	 * Conversion to/from the Boolean Java field as a number because some databases like the true/false.
	 */
	protected static class BooleanNumberFieldConverter extends BaseFieldConverter {
		@Override
		public SqlType getSqlType() {
			return SqlType.BOOLEAN;
		}

		@Override
		public Object parseDefaultString(FieldType fieldType, String defaultStr) {
			boolean bool = Boolean.parseBoolean(defaultStr);
			return (bool ? Byte.valueOf((byte) 1) : Byte.valueOf((byte) 0));
		}

		@Override
		public Object javaToSqlArg(FieldType fieldType, Object obj) {
			Boolean bool = (Boolean) obj;
			return (bool ? Byte.valueOf((byte) 1) : Byte.valueOf((byte) 0));
		}

		@Override
		public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
			return results.getByte(columnPos);
		}

		@Override
		public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
			byte arg = (Byte) sqlArg;
			return (arg == 1 ? (Boolean) true : (Boolean) false);
		}

		@Override
		public Object resultStringToJava(FieldType fieldType, String stringValue, int columnPos) {
			return sqlArgToJava(fieldType, Byte.parseByte(stringValue), columnPos);
		}
	}
}
