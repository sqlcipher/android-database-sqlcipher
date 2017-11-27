package com.j256.ormlite.stmt;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.stmt.mapped.MappedPreparedStmt;
import com.j256.ormlite.table.TableInfo;

/**
 * Assists in building of SQL statements for a particular table in a particular database.
 * 
 * @param <T>
 *            The class that the code will be operating on.
 * @param <ID>
 *            The class of the ID column associated with the class. The T class does not require an ID field. The class
 *            needs an ID parameter however so you can use Void or Object to satisfy the compiler.
 * @author graywatson
 */
public abstract class StatementBuilder<T, ID> {

	private static Logger logger = LoggerFactory.getLogger(StatementBuilder.class);

	protected final TableInfo<T, ID> tableInfo;
	protected final String tableName;
	protected final DatabaseType databaseType;
	protected final Dao<T, ID> dao;
	protected StatementType type;
	protected boolean addTableName;

	protected Where<T, ID> where = null;
	// NOTE: anything added here should be added to the clear() method below

	public StatementBuilder(DatabaseType databaseType, TableInfo<T, ID> tableInfo, Dao<T, ID> dao, StatementType type) {
		this.databaseType = databaseType;
		this.tableInfo = tableInfo;
		this.tableName = tableInfo.getTableName();
		this.dao = dao;
		this.type = type;
		if (!type.isOkForStatementBuilder()) {
			throw new IllegalStateException("Building a statement from a " + type + " statement is not allowed");
		}
	}

	/**
	 * Creates and returns a new {@link Where} object for this QueryBulder that can be used to add WHERE clauses to the
	 * SQL statement. Only one {@link Where} object can be associated with a QueryBuilder at a time and calling this
	 * method again creates a new {@link Where} object and resets the where information for this QueryBuilder.
	 */
	public Where<T, ID> where() {
		where = new Where<T, ID>(tableInfo, this, databaseType);
		return where;
	}

	/**
	 * Set the {@link Where} object on the query. This allows someone to use the same Where object on multiple queries.
	 */
	public void setWhere(Where<T, ID> where) {
		this.where = where;
	}

	/**
	 * Prepare our statement for the subclasses.
	 * 
	 * @param limit
	 *            Limit for queries. Can be null if none.
	 */
	protected MappedPreparedStmt<T, ID> prepareStatement(Long limit, boolean cacheStore) throws SQLException {
		List<ArgumentHolder> argList = new ArrayList<ArgumentHolder>();
		String statement = buildStatementString(argList);
		ArgumentHolder[] selectArgs = argList.toArray(new ArgumentHolder[argList.size()]);
		FieldType[] resultFieldTypes = getResultFieldTypes();
		FieldType[] argFieldTypes = new FieldType[argList.size()];
		for (int selectC = 0; selectC < selectArgs.length; selectC++) {
			argFieldTypes[selectC] = selectArgs[selectC].getFieldType();
		}
		if (!type.isOkForStatementBuilder()) {
			throw new IllegalStateException("Building a statement from a " + type + " statement is not allowed");
		}
		return new MappedPreparedStmt<T, ID>(tableInfo, statement, argFieldTypes, resultFieldTypes, selectArgs,
				(databaseType.isLimitSqlSupported() ? null : limit), type, cacheStore);
	}

	/**
	 * Build and return a string version of the query. If you change the where or make other calls you will need to
	 * re-call this method to re-prepare the query for execution.
	 */
	public String prepareStatementString() throws SQLException {
		List<ArgumentHolder> argList = new ArrayList<ArgumentHolder>();
		return buildStatementString(argList);
	}

	/**
	 * Build and return all of the information about the prepared statement. See {@link StatementInfo} for more details.
	 */
	public StatementInfo prepareStatementInfo() throws SQLException {
		List<ArgumentHolder> argList = new ArrayList<ArgumentHolder>();
		String statement = buildStatementString(argList);
		return new StatementInfo(statement, argList);
	}

	/**
	 * Clear out all of the statement settings so we can reuse the builder.
	 */
	public void reset() {
		where = null;
	}

	protected String buildStatementString(List<ArgumentHolder> argList) throws SQLException {
		StringBuilder sb = new StringBuilder(128);
		appendStatementString(sb, argList);
		String statement = sb.toString();
		logger.debug("built statement {}", statement);
		return statement;
	}

	/**
	 * Internal method to build a query while tracking various arguments. Users should use the
	 * {@link #prepareStatementString()} method instead.
	 * 
	 * <p>
	 * This needs to be protected because of (WARNING: DO NOT MAKE A JAVADOC LINK) InternalQueryBuilder (WARNING: DO NOT
	 * MAKE A JAVADOC LINK).
	 * </p>
	 */
	protected void appendStatementString(StringBuilder sb, List<ArgumentHolder> argList) throws SQLException {
		appendStatementStart(sb, argList);
		appendWhereStatement(sb, argList, WhereOperation.FIRST);
		appendStatementEnd(sb, argList);
	}

	/**
	 * Append the start of our statement string to the StringBuilder.
	 */
	protected abstract void appendStatementStart(StringBuilder sb, List<ArgumentHolder> argList) throws SQLException;

	/**
	 * Append the WHERE part of the statement to the StringBuilder.
	 */
	protected boolean appendWhereStatement(StringBuilder sb, List<ArgumentHolder> argList, WhereOperation operation)
			throws SQLException {
		if (where == null) {
			return operation == WhereOperation.FIRST;
		}
		operation.appendBefore(sb);
		where.appendSql((addTableName ? getTableName() : null), sb, argList);
		operation.appendAfter(sb);
		return false;
	}

	/**
	 * Append the end of our statement string to the StringBuilder.
	 */
	protected abstract void appendStatementEnd(StringBuilder sb, List<ArgumentHolder> argList) throws SQLException;

	/**
	 * Return true if we need to prepend table-name to columns.
	 */
	protected boolean shouldPrependTableNameToColumns() {
		return false;
	}

	/**
	 * Get the result array from our statement after the {@link #appendStatementStart(StringBuilder, List)} was called.
	 * This will be null except for the QueryBuilder.
	 */
	protected FieldType[] getResultFieldTypes() {
		return null;
	}

	/**
	 * Verify the columnName is valid and return its FieldType.
	 * 
	 * @throws IllegalArgumentException
	 *             if the column name is not valid.
	 */
	protected FieldType verifyColumnName(String columnName) {
		return tableInfo.getFieldTypeByColumnName(columnName);
	}

	protected String getTableName() {
		return tableName;
	}

	/**
	 * Return the type of the statement.
	 */
	StatementType getType() {
		return type;
	}

	/**
	 * Types of statements that we are building.
	 */
	public static enum StatementType {
		/** SQL statement in the form of SELECT ... */
		SELECT(true, true, false, false),
		/** SQL statement in the form of SELECT COUNT(*)... or something */
		SELECT_LONG(true, true, false, false),
		/** SQL statement in the form of SELECT... with aggregate functions or something */
		SELECT_RAW(true, true, false, false),
		/** SQL statement in the form of UPDATE ... */
		UPDATE(true, false, true, false),
		/** SQL statement in the form of DELETE ... */
		DELETE(true, false, true, false),
		/**
		 * SQL statement in the form of CREATE TABLE, ALTER TABLE, or something returning the number of rows affected
		 */
		EXECUTE(false, false, false, true),
		// end
		;

		private final boolean okForStatementBuilder;
		private final boolean okForQuery;
		private final boolean okForUpdate;
		private final boolean okForExecute;

		private StatementType(boolean okForStatementBuilder, boolean okForQuery, boolean okForUpdate,
				boolean okForExecute) {
			this.okForStatementBuilder = okForStatementBuilder;
			this.okForQuery = okForQuery;
			this.okForUpdate = okForUpdate;
			this.okForExecute = okForExecute;
		}

		public boolean isOkForStatementBuilder() {
			return okForStatementBuilder;
		}

		public boolean isOkForQuery() {
			return okForQuery;
		}

		public boolean isOkForUpdate() {
			return okForUpdate;
		}

		public boolean isOkForExecute() {
			return okForExecute;
		}
	}

	/**
	 * Class which wraps information about a statement including the arguments and the generated SQL statement string.
	 */
	public static class StatementInfo {

		private final String statement;
		private final List<ArgumentHolder> argList;

		StatementInfo(String statement, List<ArgumentHolder> argList) {
			this.argList = argList;
			this.statement = statement;
		}

		public String getStatement() {
			return statement;
		}

		public List<ArgumentHolder> getArgList() {
			return argList;
		}
	}

	/**
	 * Enum which defines which type of where operation we are appending.
	 */
	protected enum WhereOperation {
		FIRST("WHERE ", null),
		AND("AND (", ") "),
		OR("OR (", ") "),
		// end
		;

		private final String before;
		private final String after;

		private WhereOperation(String before, String after) {
			this.before = before;
			this.after = after;
		}

		/**
		 * Append the necessary operators before the where statement.
		 */
		public void appendBefore(StringBuilder sb) {
			if (before != null) {
				sb.append(before);
			}
		}

		/**
		 * Append the necessary operators after the where statement.
		 */
		public void appendAfter(StringBuilder sb) {
			if (after != null) {
				sb.append(after);
			}
		}
	}
}
