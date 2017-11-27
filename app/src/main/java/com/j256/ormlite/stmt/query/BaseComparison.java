package com.j256.ormlite.stmt.query;

import java.sql.SQLException;
import java.util.List;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.ColumnArg;
import com.j256.ormlite.stmt.SelectArg;

/**
 * Internal base class for all comparison operations.
 * 
 * @author graywatson
 */
abstract class BaseComparison implements Comparison {

	private static final String NUMBER_CHARACTERS = "0123456789.-+";
	protected final String columnName;
	protected final FieldType fieldType;
	private final Object value;

	protected BaseComparison(String columnName, FieldType fieldType, Object value, boolean isComparison)
			throws SQLException {
		if (isComparison && fieldType != null && !fieldType.isComparable()) {
			throw new SQLException("Field '" + columnName + "' is of data type " + fieldType.getDataPersister()
					+ " which can not be compared");
		}
		this.columnName = columnName;
		this.fieldType = fieldType;
		this.value = value;
	}

	@Override
	public abstract void appendOperation(StringBuilder sb);

	@Override
	public void appendSql(DatabaseType databaseType, String tableName, StringBuilder sb, List<ArgumentHolder> argList)
			throws SQLException {
		if (tableName != null) {
			databaseType.appendEscapedEntityName(sb, tableName);
			sb.append('.');
		}
		databaseType.appendEscapedEntityName(sb, columnName);
		sb.append(' ');
		appendOperation(sb);
		// this needs to call appendValue (not appendArgOrValue) because it may be overridden
		appendValue(databaseType, sb, argList);
	}

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public void appendValue(DatabaseType databaseType, StringBuilder sb, List<ArgumentHolder> argList)
			throws SQLException {
		appendArgOrValue(databaseType, fieldType, sb, argList, value);
	}

	/**
	 * Append to the string builder either a {@link ArgumentHolder} argument or a value object.
	 */
	protected void appendArgOrValue(DatabaseType databaseType, FieldType fieldType, StringBuilder sb,
			List<ArgumentHolder> argList, Object argOrValue) throws SQLException {
		boolean appendSpace = true;
		if (argOrValue == null) {
			throw new SQLException("argument for '" + fieldType.getFieldName() + "' is null");
		} else if (argOrValue instanceof ArgumentHolder) {
			sb.append('?');
			ArgumentHolder argHolder = (ArgumentHolder) argOrValue;
			argHolder.setMetaInfo(columnName, fieldType);
			argList.add(argHolder);
		} else if (argOrValue instanceof ColumnArg) {
			ColumnArg columnArg = (ColumnArg) argOrValue;
			String tableName = columnArg.getTableName();
			if (tableName != null) {
				databaseType.appendEscapedEntityName(sb, tableName);
				sb.append('.');
			}
			databaseType.appendEscapedEntityName(sb, columnArg.getColumnName());
		} else if (fieldType.isArgumentHolderRequired()) {
			sb.append('?');
			ArgumentHolder argHolder = new SelectArg();
			argHolder.setMetaInfo(columnName, fieldType);
			// conversion is done when the getValue() is called
			argHolder.setValue(argOrValue);
			argList.add(argHolder);
		} else if (fieldType.isForeign() && fieldType.getType().isAssignableFrom(argOrValue.getClass())) {
			/*
			 * If we have a foreign field and our argument is an instance of the foreign object (i.e. not its id), then
			 * we need to extract the id. We allow super-classes of the field but not sub-classes.
			 */
			FieldType idFieldType = fieldType.getForeignIdField();
			appendArgOrValue(databaseType, idFieldType, sb, argList, idFieldType.extractJavaFieldValue(argOrValue));
			// no need for the space since it was done in the recursion
			appendSpace = false;
		} else if (fieldType.isEscapedValue()) {
			databaseType.appendEscapedWord(sb, fieldType.convertJavaFieldToSqlArgValue(argOrValue).toString());
		} else if (fieldType.isForeign()) {
			/*
			 * I'm not entirely sure this is correct. This is trying to protect against someone trying to pass an object
			 * into a comparison with a foreign field. Typically if they pass the same field type, then ORMLite will
			 * extract the ID of the foreign.
			 */
			String value = fieldType.convertJavaFieldToSqlArgValue(argOrValue).toString();
			if (value.length() > 0) {
				if (NUMBER_CHARACTERS.indexOf(value.charAt(0)) < 0) {
					throw new SQLException("Foreign field " + fieldType
							+ " does not seem to be producing a numerical value '" + value
							+ "'. Maybe you are passing the wrong object to comparison: " + this);
				}
			}
			sb.append(value);
		} else {
			// numbers can't have quotes around them in derby
			sb.append(fieldType.convertJavaFieldToSqlArgValue(argOrValue));
		}
		if (appendSpace) {
			sb.append(' ');
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(columnName).append(' ');
		appendOperation(sb);
		sb.append(' ');
		sb.append(value);
		return sb.toString();
	}
}
