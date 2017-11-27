package com.j256.ormlite.table;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.misc.IOUtils;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.stmt.StatementBuilder.StatementType;
import com.j256.ormlite.support.CompiledStatement;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.support.DatabaseResults;

/**
 * Couple utility methods for the creating, dropping, and maintenance of tables.
 * 
 * @author graywatson
 */
public class TableUtils {

	private static Logger logger = LoggerFactory.getLogger(TableUtils.class);
	private static final FieldType[] noFieldTypes = new FieldType[0];

	/**
	 * For static methods only.
	 */
	private TableUtils() {
	}

	/**
	 * Issue the database statements to create the table associated with a class.
	 * 
	 * @param connectionSource
	 *            Associated connection source.
	 * @param dataClass
	 *            The class for which a table will be created.
	 * @return The number of statements executed to do so.
	 */
	public static <T> int createTable(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
		Dao<T, ?> dao = DaoManager.createDao(connectionSource, dataClass);
		return doCreateTable(dao, false);
	}

	/**
	 * Issue the database statements to create the table associated with a table configuration.
	 * 
	 * @param dao
	 *            Associated dao.
	 * @return The number of statements executed to do so.
	 */
	public static int createTable(Dao<?, ?> dao) throws SQLException {
		return doCreateTable(dao, false);
	}

	/**
	 * Create a table if it does not already exist. This is not supported by all databases.
	 */
	public static <T> int createTableIfNotExists(ConnectionSource connectionSource, Class<T> dataClass)
			throws SQLException {
		Dao<T, ?> dao = DaoManager.createDao(connectionSource, dataClass);
		return doCreateTable(dao, true);
	}

	/**
	 * Issue the database statements to create the table associated with a table configuration.
	 * 
	 * @param connectionSource
	 *            connectionSource Associated connection source.
	 * @param tableConfig
	 *            Hand or spring wired table configuration. If null then the class must have {@link DatabaseField}
	 *            annotations.
	 * @return The number of statements executed to do so.
	 */
	public static <T> int createTable(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig)
			throws SQLException {
		Dao<T, ?> dao = DaoManager.createDao(connectionSource, tableConfig);
		return doCreateTable(dao, false);
	}

	/**
	 * Create a table if it does not already exist. This is not supported by all databases.
	 */
	public static <T> int createTableIfNotExists(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig)
			throws SQLException {
		Dao<T, ?> dao = DaoManager.createDao(connectionSource, tableConfig);
		return doCreateTable(dao, true);
	}

	/**
	 * Return an ordered collection of SQL statements that need to be run to create a table. To do the work of creating,
	 * you should call {@link #createTable}.
	 * 
	 * @param connectionSource
	 *            Our connect source which is used to get the database type, not to apply the creates.
	 * @param dataClass
	 *            The class for which a table will be created.
	 * @return The collection of table create statements.
	 */
	public static <T, ID> List<String> getCreateTableStatements(ConnectionSource connectionSource, Class<T> dataClass)
			throws SQLException {
		Dao<T, ID> dao = DaoManager.createDao(connectionSource, dataClass);
		if (dao instanceof BaseDaoImpl<?, ?>) {
			return addCreateTableStatements(connectionSource, ((BaseDaoImpl<?, ?>) dao).getTableInfo(), false);
		} else {
			TableInfo<T, ID> tableInfo = new TableInfo<T, ID>(connectionSource, null, dataClass);
			return addCreateTableStatements(connectionSource, tableInfo, false);
		}
	}

	/**
	 * Return an ordered collection of SQL statements that need to be run to create a table. To do the work of creating,
	 * you should call {@link #createTable}.
	 * 
	 * @param connectionSource
	 *            Our connect source which is used to get the database type, not to apply the creates.
	 * @param tableConfig
	 *            Hand or spring wired table configuration. If null then the class must have {@link DatabaseField}
	 *            annotations.
	 * @return The collection of table create statements.
	 */
	public static <T, ID> List<String> getCreateTableStatements(ConnectionSource connectionSource,
			DatabaseTableConfig<T> tableConfig) throws SQLException {
		Dao<T, ID> dao = DaoManager.createDao(connectionSource, tableConfig);
		if (dao instanceof BaseDaoImpl<?, ?>) {
			return addCreateTableStatements(connectionSource, ((BaseDaoImpl<?, ?>) dao).getTableInfo(), false);
		} else {
			tableConfig.extractFieldTypes(connectionSource);
			TableInfo<T, ID> tableInfo = new TableInfo<T, ID>(connectionSource.getDatabaseType(), null, tableConfig);
			return addCreateTableStatements(connectionSource, tableInfo, false);
		}
	}

	/**
	 * Issue the database statements to drop the table associated with a class.
	 * 
	 * <p>
	 * <b>WARNING:</b> This is [obviously] very destructive and is unrecoverable.
	 * </p>
	 * 
	 * @param connectionSource
	 *            Associated connection source.
	 * @param dataClass
	 *            The class for which a table will be dropped.
	 * @param ignoreErrors
	 *            If set to true then try each statement regardless of {@link SQLException} thrown previously.
	 * @return The number of statements executed to do so.
	 */
	public static <T, ID> int dropTable(ConnectionSource connectionSource, Class<T> dataClass, boolean ignoreErrors)
			throws SQLException {
		Dao<T, ID> dao = DaoManager.createDao(connectionSource, dataClass);
		return dropTable(dao, ignoreErrors);
	}

	/**
	 * Issue the database statements to drop the table associated with a dao.
	 * 
	 * @param dao
	 *            Associated dao.
	 * @return The number of statements executed to do so.
	 */
	public static <T, ID> int dropTable(Dao<T, ID> dao, boolean ignoreErrors) throws SQLException {
		ConnectionSource connectionSource = dao.getConnectionSource();
		Class<T> dataClass = dao.getDataClass();
		DatabaseType databaseType = connectionSource.getDatabaseType();
		if (dao instanceof BaseDaoImpl<?, ?>) {
			return doDropTable(databaseType, connectionSource, ((BaseDaoImpl<?, ?>) dao).getTableInfo(), ignoreErrors);
		} else {
			TableInfo<T, ID> tableInfo = new TableInfo<T, ID>(connectionSource, null, dataClass);
			return doDropTable(databaseType, connectionSource, tableInfo, ignoreErrors);
		}
	}

	/**
	 * Issue the database statements to drop the table associated with a table configuration.
	 * 
	 * <p>
	 * <b>WARNING:</b> This is [obviously] very destructive and is unrecoverable.
	 * </p>
	 * 
	 * @param connectionSource
	 *            Associated connection source.
	 * @param tableConfig
	 *            Hand or spring wired table configuration. If null then the class must have {@link DatabaseField}
	 *            annotations.
	 * @param ignoreErrors
	 *            If set to true then try each statement regardless of {@link SQLException} thrown previously.
	 * @return The number of statements executed to do so.
	 */
	public static <T, ID> int dropTable(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig,
			boolean ignoreErrors) throws SQLException {
		DatabaseType databaseType = connectionSource.getDatabaseType();
		Dao<T, ID> dao = DaoManager.createDao(connectionSource, tableConfig);
		if (dao instanceof BaseDaoImpl<?, ?>) {
			return doDropTable(databaseType, connectionSource, ((BaseDaoImpl<?, ?>) dao).getTableInfo(), ignoreErrors);
		} else {
			tableConfig.extractFieldTypes(connectionSource);
			TableInfo<T, ID> tableInfo = new TableInfo<T, ID>(databaseType, null, tableConfig);
			return doDropTable(databaseType, connectionSource, tableInfo, ignoreErrors);
		}
	}

	/**
	 * Clear all data out of the table. For certain database types and with large sized tables, which may take a long
	 * time. In some configurations, it may be faster to drop and re-create the table.
	 * 
	 * <p>
	 * <b>WARNING:</b> This is [obviously] very destructive and is unrecoverable.
	 * </p>
	 */
	public static <T> int clearTable(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
		String tableName = DatabaseTableConfig.extractTableName(dataClass);
		DatabaseType databaseType = connectionSource.getDatabaseType();
		if (databaseType.isEntityNamesMustBeUpCase()) {
			tableName = databaseType.upCaseEntityName(tableName);
		}
		return clearTable(connectionSource, tableName);
	}

	/**
	 * Clear all data out of the table. For certain database types and with large sized tables, which may take a long
	 * time. In some configurations, it may be faster to drop and re-create the table.
	 * 
	 * <p>
	 * <b>WARNING:</b> This is [obviously] very destructive and is unrecoverable.
	 * </p>
	 */
	public static <T> int clearTable(ConnectionSource connectionSource, DatabaseTableConfig<T> tableConfig)
			throws SQLException {
		return clearTable(connectionSource, tableConfig.getTableName());
	}

	private static <T> int clearTable(ConnectionSource connectionSource, String tableName) throws SQLException {
		DatabaseType databaseType = connectionSource.getDatabaseType();
		StringBuilder sb = new StringBuilder(48);
		if (databaseType.isTruncateSupported()) {
			sb.append("TRUNCATE TABLE ");
		} else {
			sb.append("DELETE FROM ");
		}
		databaseType.appendEscapedEntityName(sb, tableName);
		String statement = sb.toString();
		logger.info("clearing table '{}' with '{}", tableName, statement);
		CompiledStatement compiledStmt = null;
		DatabaseConnection connection = connectionSource.getReadWriteConnection(tableName);
		try {
			compiledStmt = connection.compileStatement(statement, StatementType.EXECUTE, noFieldTypes,
					DatabaseConnection.DEFAULT_RESULT_FLAGS, false);
			return compiledStmt.runExecute();
		} finally {
			IOUtils.closeThrowSqlException(compiledStmt, "compiled statement");
			connectionSource.releaseConnection(connection);
		}
	}

	private static <T, ID> int doDropTable(DatabaseType databaseType, ConnectionSource connectionSource,
			TableInfo<T, ID> tableInfo, boolean ignoreErrors) throws SQLException {
		logger.info("dropping table '{}'", tableInfo.getTableName());
		List<String> statements = new ArrayList<String>();
		addDropIndexStatements(databaseType, tableInfo, statements);
		addDropTableStatements(databaseType, tableInfo, statements);
		DatabaseConnection connection = connectionSource.getReadWriteConnection(tableInfo.getTableName());
		try {
			return doStatements(connection, "drop", statements, ignoreErrors,
					databaseType.isCreateTableReturnsNegative(), false);
		} finally {
			connectionSource.releaseConnection(connection);
		}
	}

	private static <T, ID> void addDropIndexStatements(DatabaseType databaseType, TableInfo<T, ID> tableInfo,
			List<String> statements) {
		// run through and look for index annotations
		Set<String> indexSet = new HashSet<String>();
		for (FieldType fieldType : tableInfo.getFieldTypes()) {
			String indexName = fieldType.getIndexName();
			if (indexName != null) {
				indexSet.add(indexName);
			}
			String uniqueIndexName = fieldType.getUniqueIndexName();
			if (uniqueIndexName != null) {
				indexSet.add(uniqueIndexName);
			}
		}

		StringBuilder sb = new StringBuilder(48);
		for (String indexName : indexSet) {
			logger.info("dropping index '{}' for table '{}", indexName, tableInfo.getTableName());
			sb.append("DROP INDEX ");
			databaseType.appendEscapedEntityName(sb, indexName);
			statements.add(sb.toString());
			sb.setLength(0);
		}
	}

	private static <T, ID> void addCreateIndexStatements(DatabaseType databaseType, TableInfo<T, ID> tableInfo,
			List<String> statements, boolean ifNotExists, boolean unique) {
		// run through and look for index annotations
		Map<String, List<String>> indexMap = new HashMap<String, List<String>>();
		for (FieldType fieldType : tableInfo.getFieldTypes()) {
			String indexName;
			if (unique) {
				indexName = fieldType.getUniqueIndexName();
			} else {
				indexName = fieldType.getIndexName();
			}
			if (indexName == null) {
				continue;
			}

			List<String> columnList = indexMap.get(indexName);
			if (columnList == null) {
				columnList = new ArrayList<String>();
				indexMap.put(indexName, columnList);
			}
			columnList.add(fieldType.getColumnName());
		}

		StringBuilder sb = new StringBuilder(128);
		for (Map.Entry<String, List<String>> indexEntry : indexMap.entrySet()) {
			logger.info("creating index '{}' for table '{}", indexEntry.getKey(), tableInfo.getTableName());
			sb.append("CREATE ");
			if (unique) {
				sb.append("UNIQUE ");
			}
			sb.append("INDEX ");
			if (ifNotExists && databaseType.isCreateIndexIfNotExistsSupported()) {
				sb.append("IF NOT EXISTS ");
			}
			databaseType.appendEscapedEntityName(sb, indexEntry.getKey());
			sb.append(" ON ");
			databaseType.appendEscapedEntityName(sb, tableInfo.getTableName());
			sb.append(" ( ");
			boolean first = true;
			for (String columnName : indexEntry.getValue()) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				databaseType.appendEscapedEntityName(sb, columnName);
			}
			sb.append(" )");
			statements.add(sb.toString());
			sb.setLength(0);
		}
	}

	/**
	 * Generate and return the list of statements to drop a database table.
	 */
	private static <T, ID> void addDropTableStatements(DatabaseType databaseType, TableInfo<T, ID> tableInfo,
			List<String> statements) {
		List<String> statementsBefore = new ArrayList<String>();
		List<String> statementsAfter = new ArrayList<String>();
		for (FieldType fieldType : tableInfo.getFieldTypes()) {
			databaseType.dropColumnArg(fieldType, statementsBefore, statementsAfter);
		}
		StringBuilder sb = new StringBuilder(64);
		sb.append("DROP TABLE ");
		databaseType.appendEscapedEntityName(sb, tableInfo.getTableName());
		sb.append(' ');
		statements.addAll(statementsBefore);
		statements.add(sb.toString());
		statements.addAll(statementsAfter);
	}

	private static <T, ID> int doCreateTable(Dao<T, ID> dao, boolean ifNotExists) throws SQLException {
		if (dao instanceof BaseDaoImpl<?, ?>) {
			return doCreateTable(dao.getConnectionSource(), ((BaseDaoImpl<?, ?>) dao).getTableInfo(), ifNotExists);
		} else {
			TableInfo<T, ID> tableInfo = new TableInfo<T, ID>(dao.getConnectionSource(), null, dao.getDataClass());
			return doCreateTable(dao.getConnectionSource(), tableInfo, ifNotExists);
		}
	}

	private static <T, ID> int doCreateTable(ConnectionSource connectionSource, TableInfo<T, ID> tableInfo,
			boolean ifNotExists) throws SQLException {
		DatabaseType databaseType = connectionSource.getDatabaseType();
		logger.info("creating table '{}'", tableInfo.getTableName());
		List<String> statements = new ArrayList<String>();
		List<String> queriesAfter = new ArrayList<String>();
		addCreateTableStatements(databaseType, tableInfo, statements, queriesAfter, ifNotExists);
		DatabaseConnection connection = connectionSource.getReadWriteConnection(tableInfo.getTableName());
		try {
			int stmtC = doStatements(connection, "create", statements, false,
					databaseType.isCreateTableReturnsNegative(), databaseType.isCreateTableReturnsZero());
			stmtC += doCreateTestQueries(connection, databaseType, queriesAfter);
			return stmtC;
		} finally {
			connectionSource.releaseConnection(connection);
		}
	}

	private static int doStatements(DatabaseConnection connection, String label, Collection<String> statements,
			boolean ignoreErrors, boolean returnsNegative, boolean expectingZero) throws SQLException {
		int stmtC = 0;
		for (String statement : statements) {
			int rowC = 0;
			CompiledStatement compiledStmt = null;
			try {
				compiledStmt = connection.compileStatement(statement, StatementType.EXECUTE, noFieldTypes,
						DatabaseConnection.DEFAULT_RESULT_FLAGS, false);
				rowC = compiledStmt.runExecute();
				logger.info("executed {} table statement changed {} rows: {}", label, rowC, statement);
			} catch (SQLException e) {
				if (ignoreErrors) {
					logger.info("ignoring {} error '{}' for statement: {}", label, e, statement);
				} else {
					throw SqlExceptionUtil.create("SQL statement failed: " + statement, e);
				}
			} finally {
				IOUtils.closeThrowSqlException(compiledStmt, "compiled statement");
			}
			// sanity check
			if (rowC < 0) {
				if (!returnsNegative) {
					throw new SQLException(
							"SQL statement " + statement + " updated " + rowC + " rows, we were expecting >= 0");
				}
			} else if (rowC > 0 && expectingZero) {
				throw new SQLException("SQL statement updated " + rowC + " rows, we were expecting == 0: " + statement);
			}
			stmtC++;
		}
		return stmtC;
	}

	private static int doCreateTestQueries(DatabaseConnection connection, DatabaseType databaseType,
			List<String> queriesAfter) throws SQLException {
		int stmtC = 0;
		// now execute any test queries which test the newly created table
		for (String query : queriesAfter) {
			CompiledStatement compiledStmt = null;
			try {
				compiledStmt = connection.compileStatement(query, StatementType.SELECT, noFieldTypes,
						DatabaseConnection.DEFAULT_RESULT_FLAGS, false);
				// we don't care about an object cache here
				DatabaseResults results = compiledStmt.runQuery(null);
				int rowC = 0;
				// count the results
				for (boolean isThereMore = results.first(); isThereMore; isThereMore = results.next()) {
					rowC++;
				}
				logger.info("executing create table after-query got {} results: {}", rowC, query);
			} catch (SQLException e) {
				// we do this to make sure that the statement is in the exception
				throw SqlExceptionUtil.create("executing create table after-query failed: " + query, e);
			} finally {
				// result set is closed by the statement being closed
				IOUtils.closeThrowSqlException(compiledStmt, "compiled statement");
			}
			stmtC++;
		}
		return stmtC;
	}

	private static <T, ID> List<String> addCreateTableStatements(ConnectionSource connectionSource,
			TableInfo<T, ID> tableInfo, boolean ifNotExists) throws SQLException {
		List<String> statements = new ArrayList<String>();
		List<String> queriesAfter = new ArrayList<String>();
		addCreateTableStatements(connectionSource.getDatabaseType(), tableInfo, statements, queriesAfter, ifNotExists);
		return statements;
	}

	/**
	 * Generate and return the list of statements to create a database table and any associated features.
	 */
	private static <T, ID> void addCreateTableStatements(DatabaseType databaseType, TableInfo<T, ID> tableInfo,
			List<String> statements, List<String> queriesAfter, boolean ifNotExists) throws SQLException {
		StringBuilder sb = new StringBuilder(256);
		sb.append("CREATE TABLE ");
		if (ifNotExists && databaseType.isCreateIfNotExistsSupported()) {
			sb.append("IF NOT EXISTS ");
		}
		databaseType.appendEscapedEntityName(sb, tableInfo.getTableName());
		sb.append(" (");
		List<String> additionalArgs = new ArrayList<String>();
		List<String> statementsBefore = new ArrayList<String>();
		List<String> statementsAfter = new ArrayList<String>();
		// our statement will be set here later
		boolean first = true;
		for (FieldType fieldType : tableInfo.getFieldTypes()) {
			// skip foreign collections
			if (fieldType.isForeignCollection()) {
				continue;
			} else if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			String columnDefinition = fieldType.getColumnDefinition();
			if (columnDefinition == null) {
				// we have to call back to the database type for the specific create syntax
				databaseType.appendColumnArg(tableInfo.getTableName(), sb, fieldType, additionalArgs, statementsBefore,
						statementsAfter, queriesAfter);
			} else {
				// hand defined field
				databaseType.appendEscapedEntityName(sb, fieldType.getColumnName());
				sb.append(' ').append(columnDefinition).append(' ');
			}
		}
		// add any sql that sets any primary key fields
		databaseType.addPrimaryKeySql(tableInfo.getFieldTypes(), additionalArgs, statementsBefore, statementsAfter,
				queriesAfter);
		// add any sql that sets any unique fields
		databaseType.addUniqueComboSql(tableInfo.getFieldTypes(), additionalArgs, statementsBefore, statementsAfter,
				queriesAfter);
		for (String arg : additionalArgs) {
			// we will have spat out one argument already so we don't have to do the first dance
			sb.append(", ").append(arg);
		}
		sb.append(") ");
		databaseType.appendCreateTableSuffix(sb);
		statements.addAll(statementsBefore);
		statements.add(sb.toString());
		statements.addAll(statementsAfter);
		addCreateIndexStatements(databaseType, tableInfo, statements, ifNotExists, false);
		addCreateIndexStatements(databaseType, tableInfo, statements, ifNotExists, true);
	}
}
