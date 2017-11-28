package com.j256.ormlite.stmt.query;

/**
 * Internal class handling the SQL 'GROUP BY' operation and other lists of column-names or raw SQL.
 * 
 * @author graywatson
 */
public class ColumnNameOrRawSql {

	private final String columnName;
	private final String rawSql;

	public static ColumnNameOrRawSql withColumnName(String columnName) {
		return new ColumnNameOrRawSql(columnName, null);
	}

	public static ColumnNameOrRawSql withRawSql(String rawSql) {
		return new ColumnNameOrRawSql(null, rawSql);
	}

	private ColumnNameOrRawSql(String columnName, String rawSql) {
		this.columnName = columnName;
		this.rawSql = rawSql;
	}

	public String getColumnName() {
		return columnName;
	}

	public String getRawSql() {
		return rawSql;
	}

	@Override
	public String toString() {
		if (rawSql == null) {
			return columnName;
		} else {
			return rawSql;
		}
	}
}
