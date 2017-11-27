package com.j256.ormlite.stmt;

import java.sql.SQLException;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;

/**
 * Base class for other select argument classes.
 * 
 * @author graywatson
 */
public abstract class BaseArgumentHolder implements ArgumentHolder {

	private String columnName = null;
	private FieldType fieldType = null;
	private SqlType sqlType = null;

	public BaseArgumentHolder() {
		// no args
	}

	public BaseArgumentHolder(String columName) {
		this.columnName = columName;
	}

	public BaseArgumentHolder(SqlType sqlType) {
		this.sqlType = sqlType;
	}

	/**
	 * Return the stored value.
	 */
	protected abstract Object getValue();

	@Override
	public abstract void setValue(Object value);

	/**
	 * Return true if the value is set.
	 */
	protected abstract boolean isValueSet();

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public void setMetaInfo(String columnName) {
		if (this.columnName == null) {
			// not set yet
		} else if (this.columnName.equals(columnName)) {
			// set to the same value as before
		} else {
			throw new IllegalArgumentException("Column name cannot be set twice from " + this.columnName + " to "
					+ columnName + ".  Using a SelectArg twice in query with different columns?");
		}
		this.columnName = columnName;
	}

	@Override
	public void setMetaInfo(FieldType fieldType) {
		if (this.fieldType == null) {
			// not set yet
		} else if (this.fieldType == fieldType) {
			// set to the same value as before
		} else {
			throw new IllegalArgumentException("FieldType name cannot be set twice from " + this.fieldType + " to "
					+ fieldType + ".  Using a SelectArg twice in query with different columns?");
		}
		this.fieldType = fieldType;
	}

	@Override
	public void setMetaInfo(String columnName, FieldType fieldType) {
		setMetaInfo(columnName);
		setMetaInfo(fieldType);
	}

	@Override
	public Object getSqlArgValue() throws SQLException {
		if (!isValueSet()) {
			throw new SQLException("Column value has not been set for " + columnName);
		}
		Object value = getValue();
		if (value == null) {
			return null;
		} else if (fieldType == null) {
			return value;
		} else if (fieldType.isForeign() && fieldType.getType() == value.getClass()) {
			FieldType refFieldType = fieldType.getForeignRefField();
			return refFieldType.extractJavaFieldValue(value);
		} else {
			return fieldType.convertJavaFieldToSqlArgValue(value);
		}
	}

	@Override
	public FieldType getFieldType() {
		return fieldType;
	}

	@Override
	public SqlType getSqlType() {
		return sqlType;
	}

	@Override
	public String toString() {
		if (!isValueSet()) {
			return "[unset]";
		}
		Object val;
		try {
			val = getSqlArgValue();
			if (val == null) {
				return "[null]";
			} else {
				return val.toString();
			}
		} catch (SQLException e) {
			return "[could not get value: " + e + "]";
		}
	}
}
