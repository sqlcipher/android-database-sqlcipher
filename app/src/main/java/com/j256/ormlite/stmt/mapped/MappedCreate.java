package com.j256.ormlite.stmt.mapped;

import java.sql.SQLException;

import com.j256.ormlite.dao.ObjectCache;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.logger.Log.Level;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.support.GeneratedKeyHolder;
import com.j256.ormlite.table.TableInfo;

/**
 * A mapped statement for creating a new instance of an object.
 * 
 * @author graywatson
 */
public class MappedCreate<T, ID> extends BaseMappedStatement<T, ID> {

	private final String queryNextSequenceStmt;
	private String dataClassName;
	private int versionFieldTypeIndex;

	private MappedCreate(TableInfo<T, ID> tableInfo, String statement, FieldType[] argFieldTypes,
			String queryNextSequenceStmt, int versionFieldTypeIndex) {
		super(tableInfo, statement, argFieldTypes);
		this.dataClassName = tableInfo.getDataClass().getSimpleName();
		this.queryNextSequenceStmt = queryNextSequenceStmt;
		this.versionFieldTypeIndex = versionFieldTypeIndex;
	}

	/**
	 * Create an object in the database.
	 */
	public int insert(DatabaseType databaseType, DatabaseConnection databaseConnection, T data, ObjectCache objectCache)
			throws SQLException {
		KeyHolder keyHolder = null;
		if (idField != null) {
			boolean assignId;
			if (idField.isAllowGeneratedIdInsert() && !idField.isObjectsFieldValueDefault(data)) {
				assignId = false;
			} else {
				assignId = true;
			}
			if (idField.isSelfGeneratedId() && idField.isGeneratedId()) {
				if (assignId) {
					idField.assignField(data, idField.generateId(), false, objectCache);
				}
			} else if (idField.isGeneratedIdSequence() && databaseType.isSelectSequenceBeforeInsert()) {
				if (assignId) {
					assignSequenceId(databaseConnection, data, objectCache);
				}
			} else if (idField.isGeneratedId()) {
				if (assignId) {
					// get the id back from the database
					keyHolder = new KeyHolder();
				}
			} else {
				// the id should have been set by the caller already
			}
		}

		try {
			// implement {@link DatabaseField#foreignAutoCreate()}, need to do this _before_ getFieldObjects() below
			if (tableInfo.isForeignAutoCreate()) {
				for (FieldType fieldType : tableInfo.getFieldTypes()) {
					if (!fieldType.isForeignAutoCreate()) {
						continue;
					}
					// get the field value
					Object foreignObj = fieldType.extractRawJavaFieldValue(data);
					if (foreignObj != null && fieldType.getForeignIdField().isObjectsFieldValueDefault(foreignObj)) {
						fieldType.createWithForeignDao(foreignObj);
					}
				}
			}

			Object[] args = getFieldObjects(data);
			Object versionDefaultValue = null;
			// implement {@link DatabaseField#version()}
			if (versionFieldTypeIndex >= 0 && args[versionFieldTypeIndex] == null) {
				// if the version is null then we need to initialize it before create
				FieldType versionFieldType = argFieldTypes[versionFieldTypeIndex];
				versionDefaultValue = versionFieldType.moveToNextValue(null);
				args[versionFieldTypeIndex] = versionFieldType.convertJavaFieldToSqlArgValue(versionDefaultValue);
			}

			int rowC;
			try {
				rowC = databaseConnection.insert(statement, args, argFieldTypes, keyHolder);
			} catch (SQLException e) {
				logger.debug("insert data with statement '{}' and {} args, threw exception: {}", statement,
						args.length, e);
				if (args.length > 0) {
					// need to do the (Object) cast to force args to be a single object
					logger.trace("insert arguments: {}", (Object) args);
				}
				throw e;
			}
			logger.debug("insert data with statement '{}' and {} args, changed {} rows", statement, args.length, rowC);
			if (args.length > 0) {
				// need to do the (Object) cast to force args to be a single object
				logger.trace("insert arguments: {}", (Object) args);
			}
			if (rowC > 0) {
				if (versionDefaultValue != null) {
					argFieldTypes[versionFieldTypeIndex].assignField(data, versionDefaultValue, false, null);
				}
				if (keyHolder != null) {
					// assign the key returned by the database to the object's id field after it was inserted
					Number key = keyHolder.getKey();
					if (key == null) {
						// may never happen but let's be careful out there
						throw new SQLException(
								"generated-id key was not set by the update call, maybe a schema mismatch between entity and database table?");
					}
					if (key.longValue() == 0L) {
						// sanity check because the generated-key returned is 0 by default, may never happen
						throw new SQLException(
								"generated-id key must not be 0 value, maybe a schema mismatch between entity and database table?");
					}
					assignIdValue(data, key, "keyholder", objectCache);
				}
				/*
				 * If we have a cache and if all of the foreign-collection fields have been assigned then add to cache.
				 * However, if one of the foreign collections has not be assigned then don't add it to the cache.
				 */
				if (objectCache != null && foreignCollectionsAreAssigned(tableInfo.getForeignCollections(), data)) {
					Object id = idField.extractJavaFieldValue(data);
					objectCache.put(clazz, id, data);
				}
			}

			return rowC;
		} catch (SQLException e) {
			throw SqlExceptionUtil.create("Unable to run insert stmt on object " + data + ": " + statement, e);
		}
	}

	public static <T, ID> MappedCreate<T, ID> build(DatabaseType databaseType, TableInfo<T, ID> tableInfo) {
		StringBuilder sb = new StringBuilder(128);
		appendTableName(databaseType, sb, "INSERT INTO ", tableInfo.getTableName());
		int argFieldC = 0;
		int versionFieldTypeIndex = -1;
		// first we count up how many arguments we are going to have
		for (FieldType fieldType : tableInfo.getFieldTypes()) {
			if (isFieldCreatable(databaseType, fieldType)) {
				if (fieldType.isVersion()) {
					versionFieldTypeIndex = argFieldC;
				}
				argFieldC++;
			}
		}
		FieldType[] argFieldTypes = new FieldType[argFieldC];
		if (argFieldC == 0) {
			databaseType.appendInsertNoColumns(sb);
		} else {
			argFieldC = 0;
			boolean first = true;
			sb.append('(');
			for (FieldType fieldType : tableInfo.getFieldTypes()) {
				if (!isFieldCreatable(databaseType, fieldType)) {
					continue;
				}
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				appendFieldColumnName(databaseType, sb, fieldType, null);
				argFieldTypes[argFieldC++] = fieldType;
			}
			sb.append(") VALUES (");
			first = true;
			for (FieldType fieldType : tableInfo.getFieldTypes()) {
				if (!isFieldCreatable(databaseType, fieldType)) {
					continue;
				}
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				sb.append('?');
			}
			sb.append(')');
		}
		FieldType idField = tableInfo.getIdField();
		String queryNext = buildQueryNextSequence(databaseType, idField);
		return new MappedCreate<T, ID>(tableInfo, sb.toString(), argFieldTypes, queryNext, versionFieldTypeIndex);
	}

	private boolean foreignCollectionsAreAssigned(FieldType[] foreignCollections, Object data) throws SQLException {
		for (FieldType fieldType : foreignCollections) {
			if (fieldType.extractJavaFieldValue(data) == null) {
				return false;
			}
		}
		return true;
	}

	private static boolean isFieldCreatable(DatabaseType databaseType, FieldType fieldType) {
		// we don't insert anything if it is a collection
		if (fieldType.isForeignCollection()) {
			// skip foreign collections
			return false;
		} else if (fieldType.isReadOnly()) {
			// ignore read-only fields
			return false;
		} else if (databaseType.isIdSequenceNeeded() && databaseType.isSelectSequenceBeforeInsert()) {
			// we need to query for the next value from the sequence and the idField is inserted afterwards
			return true;
		} else if (fieldType.isGeneratedId() && !fieldType.isSelfGeneratedId() && !fieldType.isAllowGeneratedIdInsert()) {
			// skip generated-id fields because they will be auto-inserted
			return false;
		} else {
			return true;
		}
	}

	private static String buildQueryNextSequence(DatabaseType databaseType, FieldType idField) {
		if (idField == null) {
			return null;
		}
		String seqName = idField.getGeneratedIdSequence();
		if (seqName == null) {
			return null;
		} else {
			StringBuilder sb = new StringBuilder(64);
			databaseType.appendSelectNextValFromSequence(sb, seqName);
			return sb.toString();
		}
	}

	private void assignSequenceId(DatabaseConnection databaseConnection, T data, ObjectCache objectCache)
			throws SQLException {
		// call the query-next-sequence stmt to increment the sequence
		long seqVal = databaseConnection.queryForLong(queryNextSequenceStmt);
		logger.debug("queried for sequence {} using stmt: {}", seqVal, queryNextSequenceStmt);
		if (seqVal == 0) {
			// sanity check that it is working
			throw new SQLException("Should not have returned 0 for stmt: " + queryNextSequenceStmt);
		}
		assignIdValue(data, seqVal, "sequence", objectCache);
	}

	private void assignIdValue(T data, Number val, String label, ObjectCache objectCache) throws SQLException {
		// better to do this in one place with consistent logging
		idField.assignIdValue(data, val, objectCache);
		if (logger.isLevelEnabled(Level.DEBUG)) {
			logger.debug("assigned id '{}' from {} to '{}' in {} object",
					new Object[] { val, label, idField.getFieldName(), dataClassName });
		}
	}

	private static class KeyHolder implements GeneratedKeyHolder {
		Number key;

		public Number getKey() {
			return key;
		}

		@Override
		public void addKey(Number key) throws SQLException {
			if (this.key == null) {
				this.key = key;
			} else {
				throw new SQLException("generated key has already been set to " + this.key + ", now set to " + key);
			}
		}
	}
}
