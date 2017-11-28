package com.j256.ormlite.stmt.query;

import java.sql.SQLException;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.stmt.ArgumentHolder;
import com.j256.ormlite.stmt.NullArgHolder;
import com.j256.ormlite.stmt.StatementBuilder;

/**
 * Internal class handling the SQL SET part used by UPDATE statements. Used by
 * {@link StatementBuilder#updateColumnValue(String, Object)}.
 * 
 * <p>
 * It's not a comparison per se but does have a columnName = value form so it works.
 * </p>
 * 
 * @author graywatson
 */
public class SetValue extends BaseComparison {

	/**
	 * Special value in case we are trying to set a field to null. We can't just use the null value because it looks
	 * like the argument has not been set in the base class.
	 */
	private static final ArgumentHolder nullValue = new NullArgHolder();

	public SetValue(String columnName, FieldType fieldType, Object value) throws SQLException {
		super(columnName, fieldType, (value == null ? nullValue : value), false);
	}

	@Override
	public void appendOperation(StringBuilder sb) {
		sb.append("= ");
	}
}
