package com.j256.ormlite.stmt;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.stmt.query.ColumnNameOrRawSql;
import com.j256.ormlite.stmt.query.OrderBy;
import com.j256.ormlite.table.TableInfo;

/**
 * Assists in building sql query (SELECT) statements for a particular table in a particular database.
 * 
 * <p>
 * Here is a <a href="http://www.w3schools.com/Sql/" >good tutorial of SQL commands</a>.
 * </p>
 * 
 * @param <T>
 *            The class that the code will be operating on.
 * @param <ID>
 *            The class of the ID column associated with the class. The T class does not require an ID field. The class
 *            needs an ID parameter however so you can use Void or Object to satisfy the compiler.
 * @author graywatson
 */
public class QueryBuilder<T, ID> extends StatementBuilder<T, ID> {

	private final FieldType idField;
	private FieldType[] resultFieldTypes;

	private boolean distinct;
	private boolean selectIdColumn;
	private List<ColumnNameOrRawSql> selectList;
	private List<OrderBy> orderByList;
	private List<ColumnNameOrRawSql> groupByList;
	private boolean isInnerQuery;
	private String alias;
	private String countOfQuery;
	private String having;
	private Long limit;
	private Long offset;
	private List<JoinInfo> joinList;

	// NOTE: anything added here should be added to the clear() method below

	public QueryBuilder(DatabaseType databaseType, TableInfo<T, ID> tableInfo, Dao<T, ID> dao) {
		super(databaseType, tableInfo, dao, StatementType.SELECT);
		this.idField = tableInfo.getIdField();
		this.selectIdColumn = (idField != null);
	}

	/**
	 * This is used by the internal call structure to note when a query builder is being used as an inner query. This is
	 * necessary because by default, we add in the ID column on every query. When you are returning a data item, its ID
	 * field _must_ be set otherwise you can't do a refresh() or update(). But internal queries must have 1 select
	 * column set so we can't add the ID.
	 */
	void enableInnerQuery() {
		this.isInnerQuery = true;
	}

	/**
	 * Return the number of selected columns in the query.
	 */
	int getSelectColumnCount() {
		if (countOfQuery != null) {
			return 1;
		} else if (selectList == null) {
			return 0;
		} else {
			return selectList.size();
		}
	}

	/**
	 * Return the selected columns in the query or an empty list if none were specified.
	 */
	String getSelectColumnsAsString() {
		if (countOfQuery != null) {
			return "COUNT(" + countOfQuery + ")";
		} else if (selectList == null) {
			return "";
		} else {
			return selectList.toString();
		}
	}

	/**
	 * Build and return a prepared query that can be used by {@link Dao#query(PreparedQuery)} or
	 * {@link Dao#iterator(PreparedQuery)} methods. If you change the where or make other calls you will need to re-call
	 * this method to re-prepare the statement for execution.
	 */
	public PreparedQuery<T> prepare() throws SQLException {
		// we only store things in the cache if there was not selects specified
		boolean cacheStore = (selectList == null);
		return super.prepareStatement(limit, cacheStore);
	}

	/**
	 * Add columns to be returned by the SELECT query. If no columns are selected then all columns are returned by
	 * default. For classes with id columns, the id column is added to the select list automagically. This can be called
	 * multiple times to add more columns to select.
	 * 
	 * <p>
	 * <b>WARNING:</b> If you specify any columns to return, then any foreign-collection fields will be returned as null
	 * <i>unless</i> their {@link ForeignCollectionField#columnName()} is also in the list.
	 * </p>
	 */
	public QueryBuilder<T, ID> selectColumns(String... columns) {
		for (String column : columns) {
			addSelectColumnToList(column);
		}
		return this;
	}

	/**
	 * Same as {@link #selectColumns(String...)} except the columns are specified as an iterable -- probably will be a
	 * {@link Collection}. This can be called multiple times to add more columns to select.
	 */
	public QueryBuilder<T, ID> selectColumns(Iterable<String> columns) {
		for (String column : columns) {
			addSelectColumnToList(column);
		}
		return this;
	}

	/**
	 * Add raw columns or aggregate functions (COUNT, MAX, ...) to the query. This will turn the query into something
	 * only suitable for the {@link Dao#queryRaw(String, String...)} type of statement. This can be called multiple
	 * times to add more columns to select.
	 */
	public QueryBuilder<T, ID> selectRaw(String... columns) {
		for (String column : columns) {
			addSelectToList(ColumnNameOrRawSql.withRawSql(column));
		}
		return this;
	}

	/**
	 * Add "GROUP BY" clause to the SQL query statement. This can be called multiple times to add additional "GROUP BY"
	 * clauses.
	 * 
	 * <p>
	 * NOTE: Use of this means that the resulting objects may not have a valid ID column value so cannot be deleted or
	 * updated.
	 * </p>
	 */
	public QueryBuilder<T, ID> groupBy(String columnName) {
		FieldType fieldType = verifyColumnName(columnName);
		if (fieldType.isForeignCollection()) {
			throw new IllegalArgumentException("Can't groupBy foreign collection field: " + columnName);
		}
		addGroupBy(ColumnNameOrRawSql.withColumnName(columnName));
		return this;
	}

	/**
	 * Add a raw SQL "GROUP BY" clause to the SQL query statement. This should not include the "GROUP BY".
	 */
	public QueryBuilder<T, ID> groupByRaw(String rawSql) {
		addGroupBy(ColumnNameOrRawSql.withRawSql(rawSql));
		return this;
	}

	/**
	 * Add "ORDER BY" clause to the SQL query statement. This can be called multiple times to add additional "ORDER BY"
	 * clauses. Ones earlier are applied first.
	 */
	public QueryBuilder<T, ID> orderBy(String columnName, boolean ascending) {
		FieldType fieldType = verifyColumnName(columnName);
		if (fieldType.isForeignCollection()) {
			throw new IllegalArgumentException("Can't orderBy foreign collection field: " + columnName);
		}
		addOrderBy(new OrderBy(columnName, ascending));
		return this;
	}

	/**
	 * Add raw SQL "ORDER BY" clause to the SQL query statement.
	 * 
	 * @param rawSql
	 *            The raw SQL order by clause. This should not include the "ORDER BY".
	 */
	public QueryBuilder<T, ID> orderByRaw(String rawSql) {
		addOrderBy(new OrderBy(rawSql, (ArgumentHolder[]) null));
		return this;
	}

	/**
	 * Add raw SQL "ORDER BY" clause to the SQL query statement.
	 * 
	 * @param rawSql
	 *            The raw SQL order by clause. This should not include the "ORDER BY".
	 * @param args
	 *            Optional arguments that correspond to any ? specified in the rawSql. Each of the arguments must have
	 *            the sql-type set.
	 */
	public QueryBuilder<T, ID> orderByRaw(String rawSql, ArgumentHolder... args) {
		addOrderBy(new OrderBy(rawSql, args));
		return this;
	}

	/**
	 * Add "DISTINCT" clause to the SQL query statement.
	 * 
	 * <p>
	 * NOTE: Use of this means that the resulting objects may not have a valid ID column value so cannot be deleted or
	 * updated.
	 * </p>
	 */
	public QueryBuilder<T, ID> distinct() {
		distinct = true;
		selectIdColumn = false;
		return this;
	}

	/**
	 * Limit the output to maxRows maximum number of rows. Set to null for no limit (the default).
	 */
	public QueryBuilder<T, ID> limit(Long maxRows) {
		limit = maxRows;
		return this;
	}

	/**
	 * Start the output at this row number. Set to null for no offset (the default). If you are paging through a table,
	 * you should consider using the {@link Dao#iterator()} method instead which handles paging with a database cursor.
	 * Otherwise, if you are paging you probably want to specify a {@link #orderBy(String, boolean)}.
	 * 
	 * <p>
	 * <b>NOTE:</b> This is not supported for all databases. Also, for some databases, the limit _must_ also be
	 * specified since the offset is an argument of the limit.
	 * </p>
	 */
	public QueryBuilder<T, ID> offset(Long startRow) throws SQLException {
		if (databaseType.isOffsetSqlSupported()) {
			offset = startRow;
			return this;
		} else {
			throw new SQLException("Offset is not supported by this database");
		}
	}

	/**
	 * Set whether or not we should only return the count of the results. This query can then be used by
	 * {@link Dao#countOf(PreparedQuery)}.
	 * 
	 * To get the count-of directly, use {@link #countOf()}.
	 */
	public QueryBuilder<T, ID> setCountOf(boolean countOf) {
		return setCountOf("*");
	}

	/**
	 * Set the field that we are counting from the database. For example, you can
	 * {@code qb.setCountOf("DISTINCT(fieldname)")}. This query can then be used by {@link Dao#countOf(PreparedQuery)}.
	 * 
	 * To get the count-of directly, use {@link #countOf(String)}.
	 */
	public QueryBuilder<T, ID> setCountOf(String countOfQuery) {
		this.countOfQuery = countOfQuery;
		return this;
	}

	/**
	 * Add raw SQL "HAVING" clause to the SQL query statement. This should not include the "HAVING" string.
	 */
	public QueryBuilder<T, ID> having(String having) {
		this.having = having;
		return this;
	}

	/**
	 * Join with another query builder. This will add into the SQL something close to " INNER JOIN other-table ...".
	 * Either the object associated with the current QueryBuilder or the argument QueryBuilder must have a foreign field
	 * of the other one. An exception will be thrown otherwise.
	 * 
	 * <p>
	 * <b>NOTE:</b> This will do combine the WHERE statement of the two query builders with a SQL "AND". See
	 * {@link #joinOr(QueryBuilder)}.
	 * </p>
	 */
	public QueryBuilder<T, ID> join(QueryBuilder<?, ?> joinedQueryBuilder) throws SQLException {
		addJoinInfo(JoinType.INNER, null, null, joinedQueryBuilder, JoinWhereOperation.AND);
		return this;
	}

	/**
	 * Like {@link #join(QueryBuilder)} but allows you to specify the join type and the operation used to combine the
	 * WHERE statements.
	 */
	public QueryBuilder<T, ID> join(QueryBuilder<?, ?> joinedQueryBuilder, JoinType type, JoinWhereOperation operation)
			throws SQLException {
		addJoinInfo(type, null, null, joinedQueryBuilder, operation);
		return this;
	}

	/**
	 * Like {@link #join(QueryBuilder)} but this combines the WHERE statements of two query builders with a SQL "OR".
	 */
	public QueryBuilder<T, ID> joinOr(QueryBuilder<?, ?> joinedQueryBuilder) throws SQLException {
		addJoinInfo(JoinType.INNER, null, null, joinedQueryBuilder, JoinWhereOperation.OR);
		return this;
	}

	/**
	 * Similar to {@link #join(QueryBuilder)} but it will use "LEFT JOIN" instead.
	 * 
	 * See: <a href="http://www.w3schools.com/sql/sql_join_left.asp" >LEFT JOIN SQL docs</a>
	 * 
	 * <p>
	 * <b>NOTE:</b> RIGHT and FULL JOIN SQL commands are not supported because we are only returning objects from the
	 * "left" table.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</b> This will do combine the WHERE statement of the two query builders with a SQL "AND". See
	 * {@link #leftJoinOr(QueryBuilder)}.
	 * </p>
	 */
	public QueryBuilder<T, ID> leftJoin(QueryBuilder<?, ?> joinedQueryBuilder) throws SQLException {
		addJoinInfo(JoinType.LEFT, null, null, joinedQueryBuilder, JoinWhereOperation.AND);
		return this;
	}

	/**
	 * Like {@link #leftJoin(QueryBuilder)} but this combines the WHERE statements of two query builders with a SQL
	 * "OR".
	 */
	public QueryBuilder<T, ID> leftJoinOr(QueryBuilder<?, ?> joinedQueryBuilder) throws SQLException {
		addJoinInfo(JoinType.LEFT, null, null, joinedQueryBuilder, JoinWhereOperation.OR);
		return this;
	}

	/**
	 * Similar to {@link #join(QueryBuilder)} but this allows you to link two tables that share a field of the same
	 * type. So even if there is _not_ a foreign-object relationship between the tables, you can JOIN them. This will
	 * add into the SQL something close to " INNER JOIN other-table ...".
	 */
	public QueryBuilder<T, ID> join(String localColumnName, String joinedColumnName,
			QueryBuilder<?, ?> joinedQueryBuilder) throws SQLException {
		addJoinInfo(JoinType.INNER, localColumnName, joinedColumnName, joinedQueryBuilder, JoinWhereOperation.AND);
		return this;
	}

	/**
	 * Similar to {@link #join(QueryBuilder, JoinType, JoinWhereOperation)} but this allows you to link two tables that
	 * share a field of the same type.
	 */
	public QueryBuilder<T, ID> join(String localColumnName, String joinedColumnName,
			QueryBuilder<?, ?> joinedQueryBuilder, JoinType type, JoinWhereOperation operation) throws SQLException {
		addJoinInfo(type, localColumnName, joinedColumnName, joinedQueryBuilder, operation);
		return this;
	}

	/**
	 * A short cut to {@link Dao#query(PreparedQuery)}.
	 */
	public List<T> query() throws SQLException {
		return dao.query(prepare());
	}

	/**
	 * A short cut to {@link Dao#queryRaw(String, String...)}.
	 */
	public GenericRawResults<String[]> queryRaw() throws SQLException {
		return dao.queryRaw(prepareStatementString());
	}

	/**
	 * A short cut to {@link Dao#queryForFirst(PreparedQuery)}.
	 */
	public T queryForFirst() throws SQLException {
		return dao.queryForFirst(prepare());
	}

	/**
	 * A short cut to {@link Dao#queryRaw(String, String...)} and {@link GenericRawResults#getFirstResult()}.
	 */
	public String[] queryRawFirst() throws SQLException {
		return dao.queryRaw(prepareStatementString()).getFirstResult();
	}

	/**
	 * A short cut to {@link Dao#iterator(PreparedQuery)}.
	 */
	public CloseableIterator<T> iterator() throws SQLException {
		return dao.iterator(prepare());
	}

	/**
	 * Returns the count of the number of rows in the table. This uses {@link #setCountOf(boolean)} to true and then
	 * calls {@link Dao#countOf(PreparedQuery)}. It restores the previous count-of value before returning.
	 */
	public long countOf() throws SQLException {
		String countOfQuerySave = this.countOfQuery;
		try {
			setCountOf(true);
			return dao.countOf(prepare());
		} finally {
			setCountOf(countOfQuerySave);
		}
	}

	/**
	 * Returns the count of the number of rows that match a field so you can do
	 * {@code qb.countOf("DISTINCT(fieldname)")}. This uses {@link #setCountOf(String)} and then calls
	 * {@link Dao#countOf(PreparedQuery)}. It restores the previous count-of value before returning.
	 */
	public long countOf(String countOfQuery) throws SQLException {
		String countOfQuerySave = this.countOfQuery;
		try {
			setCountOf(countOfQuery);
			return dao.countOf(prepare());
		} finally {
			setCountOf(countOfQuerySave);
		}
	}

	/**
	 * Add an alias for this table. In FROM clause, the table will use AS to define the alias, and qualified fields will
	 * be qualified using the alias instead of table name.
	 */
	public QueryBuilder<T, ID> setAlias(String alias) {
		this.alias = alias;
		return this;
	}

	@Override
	public void reset() {
		super.reset();
		distinct = false;
		selectIdColumn = (idField != null);
		if (selectList != null) {
			selectList.clear();
			selectList = null;
		}
		if (orderByList != null) {
			orderByList.clear();
			orderByList = null;
		}
		if (groupByList != null) {
			groupByList.clear();
			groupByList = null;
		}
		isInnerQuery = false;
		countOfQuery = null;
		having = null;
		limit = null;
		offset = null;
		if (joinList != null) {
			// help gc
			joinList.clear();
			joinList = null;
		}
		addTableName = false;
		alias = null;
	}

	@Override
	protected void appendStatementStart(StringBuilder sb, List<ArgumentHolder> argList) {
		if (joinList == null) {
			setAddTableName(false);
		} else {
			setAddTableName(true);
		}
		sb.append("SELECT ");
		if (databaseType.isLimitAfterSelect()) {
			appendLimit(sb);
		}
		if (distinct) {
			sb.append("DISTINCT ");
		}
		if (countOfQuery == null) {
			// type set in appendSelects depending on raw or not
			appendSelects(sb);
		} else {
			type = StatementType.SELECT_LONG;
			sb.append("COUNT(").append(countOfQuery).append(") ");
		}
		sb.append("FROM ");
		databaseType.appendEscapedEntityName(sb, tableName);
		if (alias != null) {
			appendAlias(sb);
		}
		sb.append(' ');
		if (joinList != null) {
			appendJoinSql(sb);
		}
	}

	@Override
	protected FieldType[] getResultFieldTypes() {
		return resultFieldTypes;
	}

	@Override
	protected boolean appendWhereStatement(StringBuilder sb, List<ArgumentHolder> argList, WhereOperation operation)
			throws SQLException {
		boolean first = (operation == WhereOperation.FIRST);
		if (this.where != null) {
			first = super.appendWhereStatement(sb, argList, operation);
		}
		if (joinList != null) {
			for (JoinInfo joinInfo : joinList) {
				if (first) {
					operation = WhereOperation.FIRST;
				} else {
					operation = joinInfo.operation.whereOperation;
				}
				first = joinInfo.queryBuilder.appendWhereStatement(sb, argList, operation);
			}
		}
		return first;
	}

	@Override
	protected void appendStatementEnd(StringBuilder sb, List<ArgumentHolder> argList) throws SQLException {
		// 'group by' comes before 'order by'
		maybeAppendGroupBys(sb, true);
		maybeAppendHaving(sb);
		maybeAppendOrderBys(sb, argList, true);
		if (!databaseType.isLimitAfterSelect()) {
			appendLimit(sb);
		}
		appendOffset(sb);
		// clear the add-table name flag so we can reuse the builder
		setAddTableName(false);
	}

	@Override
	protected boolean shouldPrependTableNameToColumns() {
		return joinList != null;
	}

	protected void appendTableQualifier(StringBuilder sb) {
		databaseType.appendEscapedEntityName(sb, getTableName());
	}

	@Override
	protected String getTableName() {
		return (alias == null ? tableName : alias);
	}

	private void addOrderBy(OrderBy orderBy) {
		if (orderByList == null) {
			orderByList = new ArrayList<OrderBy>();
		}
		orderByList.add(orderBy);
	}

	private void addGroupBy(ColumnNameOrRawSql groupBy) {
		if (groupByList == null) {
			groupByList = new ArrayList<ColumnNameOrRawSql>();
		}
		groupByList.add(groupBy);
		selectIdColumn = false;
	}

	private void setAddTableName(boolean addTableName) {
		this.addTableName = addTableName;
		if (joinList != null) {
			for (JoinInfo joinInfo : joinList) {
				joinInfo.queryBuilder.setAddTableName(addTableName);
			}
		}
	}

	/**
	 * Add join info to the query. This can be called multiple times to join with more than one table.
	 */
	private void addJoinInfo(JoinType type, String localColumnName, String joinedColumnName,
			QueryBuilder<?, ?> joinedQueryBuilder, JoinWhereOperation operation) throws SQLException {
		JoinInfo joinInfo = new JoinInfo(type, joinedQueryBuilder, operation);
		if (localColumnName == null) {
			matchJoinedFields(joinInfo, joinedQueryBuilder);
		} else {
			matchJoinedFieldsByName(joinInfo, localColumnName, joinedColumnName, joinedQueryBuilder);
		}
		if (joinList == null) {
			joinList = new ArrayList<JoinInfo>();
		}
		joinList.add(joinInfo);
	}

	/**
	 * Match up our joined fields so we can throw a nice exception immediately if you can't join with this type.
	 */
	private void matchJoinedFieldsByName(JoinInfo joinInfo, String localColumnName, String joinedColumnName,
			QueryBuilder<?, ?> joinedQueryBuilder) throws SQLException {
		joinInfo.localField = tableInfo.getFieldTypeByColumnName(localColumnName);
		if (joinInfo.localField == null) {
			throw new SQLException("Could not find field in " + tableInfo.getDataClass() + " that has column-name '"
					+ localColumnName + "'");
		}
		joinInfo.remoteField = joinedQueryBuilder.tableInfo.getFieldTypeByColumnName(joinedColumnName);
		if (joinInfo.remoteField == null) {
			throw new SQLException("Could not find field in " + joinedQueryBuilder.tableInfo.getDataClass()
					+ " that has column-name '" + joinedColumnName + "'");
		}
	}

	/**
	 * Match up our joined fields so we can throw a nice exception immediately if you can't join with this type.
	 */
	private void matchJoinedFields(JoinInfo joinInfo, QueryBuilder<?, ?> joinedQueryBuilder) throws SQLException {
		for (FieldType fieldType : tableInfo.getFieldTypes()) {
			// if this is a foreign field and its foreign field is the same as the other's id
			FieldType foreignRefField = fieldType.getForeignRefField();
			if (fieldType.isForeign() && foreignRefField.equals(joinedQueryBuilder.tableInfo.getIdField())) {
				joinInfo.localField = fieldType;
				joinInfo.remoteField = foreignRefField;
				return;
			}
		}
		// if this other field is a foreign field and its foreign-id field is our id
		for (FieldType fieldType : joinedQueryBuilder.tableInfo.getFieldTypes()) {
			if (fieldType.isForeign() && fieldType.getForeignIdField().equals(idField)) {
				joinInfo.localField = idField;
				joinInfo.remoteField = fieldType;
				return;
			}
		}

		throw new SQLException("Could not find a foreign " + tableInfo.getDataClass() + " field in "
				+ joinedQueryBuilder.tableInfo.getDataClass() + " or vice versa");
	}

	private void addSelectColumnToList(String columnName) {
		verifyColumnName(columnName);
		addSelectToList(ColumnNameOrRawSql.withColumnName(columnName));
	}

	private void addSelectToList(ColumnNameOrRawSql select) {
		if (selectList == null) {
			selectList = new ArrayList<ColumnNameOrRawSql>();
		}
		selectList.add(select);
	}

	private void appendJoinSql(StringBuilder sb) {
		for (JoinInfo joinInfo : joinList) {
			sb.append(joinInfo.type.sql).append(" JOIN ");
			databaseType.appendEscapedEntityName(sb, joinInfo.queryBuilder.tableName);
			if (joinInfo.queryBuilder.alias != null) {
				joinInfo.queryBuilder.appendAlias(sb);
			}
			sb.append(" ON ");
			appendTableQualifier(sb);
			sb.append('.');
			databaseType.appendEscapedEntityName(sb, joinInfo.localField.getColumnName());
			sb.append(" = ");
			joinInfo.queryBuilder.appendTableQualifier(sb);
			sb.append('.');
			databaseType.appendEscapedEntityName(sb, joinInfo.remoteField.getColumnName());
			sb.append(' ');
			// keep on going down if multiple JOIN layers
			if (joinInfo.queryBuilder.joinList != null) {
				joinInfo.queryBuilder.appendJoinSql(sb);
			}
		}
	}

	private void appendSelects(StringBuilder sb) {
		// the default
		type = StatementType.SELECT;

		// if no columns were specified then * is the default
		if (selectList == null) {
			if (addTableName) {
				appendTableQualifier(sb);
				sb.append('.');
			}
			sb.append("* ");
			resultFieldTypes = tableInfo.getFieldTypes();
			return;
		}

		boolean first = true;
		boolean hasId;
		if (isInnerQuery) {
			hasId = true;
		} else {
			hasId = false;
		}
		List<FieldType> fieldTypeList = new ArrayList<FieldType>(selectList.size() + 1);
		for (ColumnNameOrRawSql select : selectList) {
			if (select.getRawSql() != null) {
				// if any are raw-sql then that's our type
				type = StatementType.SELECT_RAW;
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(select.getRawSql());
				continue;
			}
			FieldType fieldType = tableInfo.getFieldTypeByColumnName(select.getColumnName());
			/*
			 * If this is a foreign-collection then we add it to our field-list but _not_ to the select list because
			 * foreign collections don't have a column in the database.
			 */
			if (fieldType.isForeignCollection()) {
				fieldTypeList.add(fieldType);
				continue;
			}
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			appendFieldColumnName(sb, fieldType, fieldTypeList);
			if (fieldType == idField) {
				hasId = true;
			}
		}

		if (type != StatementType.SELECT_RAW) {
			// we have to add the idField even if it isn't in the columnNameSet
			if (!hasId && selectIdColumn) {
				if (!first) {
					sb.append(',');
				}
				appendFieldColumnName(sb, idField, fieldTypeList);
			}

			resultFieldTypes = fieldTypeList.toArray(new FieldType[fieldTypeList.size()]);
		}
		sb.append(' ');
	}

	private void appendFieldColumnName(StringBuilder sb, FieldType fieldType, List<FieldType> fieldTypeList) {
		appendColumnName(sb, fieldType.getColumnName());
		if (fieldTypeList != null) {
			fieldTypeList.add(fieldType);
		}
	}

	private void appendLimit(StringBuilder sb) {
		if (limit != null && databaseType.isLimitSqlSupported()) {
			databaseType.appendLimitValue(sb, limit, offset);
		}
	}

	private void appendOffset(StringBuilder sb) throws SQLException {
		if (offset == null) {
			return;
		}
		if (databaseType.isOffsetLimitArgument()) {
			if (limit == null) {
				throw new SQLException("If the offset is specified, limit must also be specified with this database");
			}
		} else {
			databaseType.appendOffsetValue(sb, offset);
		}
	}

	private boolean maybeAppendGroupBys(StringBuilder sb, boolean first) {
		if (groupByList != null && !groupByList.isEmpty()) {
			appendGroupBys(sb, first);
			first = false;
		}
		// NOTE: this doesn't seem to work with some database types but we will check for sub-queries anyway
		if (joinList != null) {
			for (JoinInfo joinInfo : joinList) {
				first = joinInfo.queryBuilder.maybeAppendGroupBys(sb, first);
			}
		}
		return first;
	}

	private void appendGroupBys(StringBuilder sb, boolean first) {
		if (first) {
			sb.append("GROUP BY ");
		}
		for (ColumnNameOrRawSql groupBy : groupByList) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}
			if (groupBy.getRawSql() == null) {
				appendColumnName(sb, groupBy.getColumnName());
			} else {
				sb.append(groupBy.getRawSql());
			}
		}
		sb.append(' ');
	}

	private boolean maybeAppendOrderBys(StringBuilder sb, List<ArgumentHolder> argList, boolean first) {
		if (orderByList != null && !orderByList.isEmpty()) {
			appendOrderBys(sb, first, argList);
			first = false;
		}
		// NOTE: this doesn't seem to work with some database types but we will check for sub-queries anyway
		if (joinList != null) {
			for (JoinInfo joinInfo : joinList) {
				first = joinInfo.queryBuilder.maybeAppendOrderBys(sb, argList, first);
			}
		}
		return first;
	}

	private void appendOrderBys(StringBuilder sb, boolean first, List<ArgumentHolder> argList) {
		if (first) {
			sb.append("ORDER BY ");
		}
		for (OrderBy orderBy : orderByList) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}
			if (orderBy.getRawSql() == null) {
				appendColumnName(sb, orderBy.getColumnName());
				if (orderBy.isAscending()) {
					// here for documentation purposes, ASC is the default
					// sb.append(" ASC");
				} else {
					sb.append(" DESC");
				}
			} else {
				sb.append(orderBy.getRawSql());
				if (orderBy.getOrderByArgs() != null) {
					for (ArgumentHolder arg : orderBy.getOrderByArgs()) {
						argList.add(arg);
					}
				}
			}
		}
		sb.append(' ');
	}

	private void appendColumnName(StringBuilder sb, String columnName) {
		if (addTableName) {
			appendTableQualifier(sb);
			sb.append('.');
		}
		databaseType.appendEscapedEntityName(sb, columnName);
	}

	private void maybeAppendHaving(StringBuilder sb) {
		if (having != null) {
			sb.append("HAVING ").append(having).append(' ');
		}
	}

	private void appendAlias(StringBuilder sb) {
		sb.append(" AS ");
		databaseType.appendEscapedEntityName(sb, alias);
	}

	/**
	 * Encapsulates our join information.
	 */
	private static class JoinInfo {
		final JoinType type;
		final QueryBuilder<?, ?> queryBuilder;
		FieldType localField;
		FieldType remoteField;
		JoinWhereOperation operation;

		public JoinInfo(JoinType type, QueryBuilder<?, ?> queryBuilder, JoinWhereOperation operation) {
			this.type = type;
			this.queryBuilder = queryBuilder;
			this.operation = operation;
		}
	}

	/**
	 * Internal class used to expose methods to internal classes but through a wrapper instead of a builder.
	 */
	public static class InternalQueryBuilderWrapper {

		private final QueryBuilder<?, ?> queryBuilder;

		InternalQueryBuilderWrapper(QueryBuilder<?, ?> queryBuilder) {
			this.queryBuilder = queryBuilder;
		}

		public void appendStatementString(StringBuilder sb, List<ArgumentHolder> argList) throws SQLException {
			queryBuilder.appendStatementString(sb, argList);
		}

		public FieldType[] getResultFieldTypes() {
			return queryBuilder.getResultFieldTypes();
		}
	}

	/**
	 * Type of the JOIN that we are adding.
	 * 
	 * <p>
	 * <b>NOTE:</b> RIGHT and FULL JOIN SQL commands are not supported because we are only returning objects from the
	 * "left" table.
	 */
	public enum JoinType {
		/**
		 * The most common type of join. "An SQL INNER JOIN return all rows from multiple tables where the join
		 * condition is met."
		 * 
		 * <p>
		 * See <a href="http://www.w3schools.com/sql/sql_join.asp" >SQL JOIN</a>
		 * </p>
		 */
		INNER("INNER"),
		/**
		 * "The LEFT JOIN keyword returns all rows from the left table (table1), with the matching rows in the right
		 * table (table2). The result is NULL in the right side when there is no match."
		 * 
		 * <p>
		 * See: <a href="http://www.w3schools.com/sql/sql_join_left.asp" >LEFT JOIN SQL docs</a>
		 * </p>
		 */
		LEFT("LEFT"),
		// end
		;

		final String sql;

		private JoinType(String sql) {
			this.sql = sql;
		}
	}

	/**
	 * When we are combining WHERE statements from the two joined query-builders, this determines the operator to use to
	 * do so.
	 */
	public enum JoinWhereOperation {
		/** combine the two WHERE parts of the JOINed queries with an AND */
		AND(WhereOperation.AND),
		/** combine the two WHERE parts of the JOINed queries with an OR */
		OR(WhereOperation.OR),
		// end
		;

		final WhereOperation whereOperation;

		private JoinWhereOperation(WhereOperation whereOperation) {
			this.whereOperation = whereOperation;
		}
	}
}
