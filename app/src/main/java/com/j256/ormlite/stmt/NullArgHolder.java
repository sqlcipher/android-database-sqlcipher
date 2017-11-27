package com.j256.ormlite.stmt;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;

/**
 * An argument to a select SQL statement for null arguments. This overrides the protections around multiple columns
 * since it will always have a null value.
 * 
 * @author graywatson
 */
public class NullArgHolder implements ArgumentHolder {

	public NullArgHolder() {
		// typical that matches all columns/types
	}

	@Override
	public String getColumnName() {
		return "null-holder";
	}

	@Override
	public void setValue(Object value) {
		throw new UnsupportedOperationException("Cannot set null on " + getClass());
	}

	@Override
	public void setMetaInfo(String columnName) {
		// noop
	}

	@Override
	public void setMetaInfo(FieldType fieldType) {
		// noop
	}

	@Override
	public void setMetaInfo(String columnName, FieldType fieldType) {
		// noop
	}

	@Override
	public Object getSqlArgValue() {
		return null;
	}

	@Override
	public SqlType getSqlType() {
		// we use this as our default because it should work with all SQL engines
		return SqlType.STRING;
	}

	@Override
	public FieldType getFieldType() {
		return null;
	}
}
