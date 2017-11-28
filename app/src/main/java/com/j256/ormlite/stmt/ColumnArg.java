package com.j256.ormlite.stmt;

/**
 * An argument to a select SQL statement that represents a column in a table. This allows you using the
 * {@link QueryBuilder} to be able to compare two database fields or using {@link QueryBuilder#join(QueryBuilder)} to be
 * able to compare fields in different tables.
 * 
 * <p>
 * <b>NOTE:</b> This does not verify that the two fields in question _can_ be compared via SQL. If you try to compare
 * (for example) a string to a number, a SQL exception will most likely be generated.
 * </p>
 * 
 * @author graywatson
 */
public class ColumnArg {

	private final String tableName;
	private final String columnName;

	/**
	 * For queries where only one table is being addressed. This will output an escaped column-name only into the query.
	 */
	public ColumnArg(String columnName) {
		this.tableName = null;
		this.columnName = columnName;
	}

	/**
	 * For queries where multiple tables are being addressed. This will output an escaped table-name, then a period,
	 * then escaped column-name only into the query.
	 */
	public ColumnArg(String tableName, String columnName) {
		this.tableName = tableName;
		this.columnName = columnName;
	}

	public String getTableName() {
		return tableName;
	}

	public String getColumnName() {
		return columnName;
	}
}
