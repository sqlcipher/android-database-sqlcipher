package com.j256.ormlite.field;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.BaseForeignCollection;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.EagerForeignCollection;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.dao.LazyForeignCollection;
import com.j256.ormlite.dao.ObjectCache;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.types.VoidType;
import com.j256.ormlite.logger.Log.Level;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.stmt.mapped.MappedQueryForFieldEq;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.support.DatabaseConnection;
import com.j256.ormlite.support.DatabaseResults;
import com.j256.ormlite.table.DatabaseTableConfig;
import com.j256.ormlite.table.TableInfo;

/**
 * Per field information configured from the {@link DatabaseField} annotation and the associated {@link Field} in the
 * class. Use the {@link #createFieldType} static method to instantiate the class.
 * 
 * @author graywatson
 */
public class FieldType {

	/** default suffix added to fields that are id fields of foreign objects */
	public static final String FOREIGN_ID_FIELD_SUFFIX = "_id";

	/*
	 * Default values.
	 * 
	 * NOTE: These don't get any values so the compiler assigns them to the default values for the type. Ahhhh. Smart.
	 */
	private static boolean DEFAULT_VALUE_BOOLEAN;
	private static byte DEFAULT_VALUE_BYTE;
	private static char DEFAULT_VALUE_CHAR;
	private static short DEFAULT_VALUE_SHORT;
	private static int DEFAULT_VALUE_INT;
	private static long DEFAULT_VALUE_LONG;
	private static float DEFAULT_VALUE_FLOAT;
	private static double DEFAULT_VALUE_DOUBLE;

	private final ConnectionSource connectionSource;
	private final String tableName;
	private final Field field;
	private final String columnName;
	private final DatabaseFieldConfig fieldConfig;
	private final boolean isId;
	private final boolean isGeneratedId;
	private final String generatedIdSequence;
	private final Method fieldGetMethod;
	private final Method fieldSetMethod;
	private final Class<?> parentClass;

	private DataPersister dataPersister;
	private Object defaultValue;
	private Object dataTypeConfigObj;

	private FieldConverter fieldConverter;
	private FieldType foreignIdField;
	private FieldType foreignRefField;
	private TableInfo<?, ?> foreignTableInfo;
	private FieldType foreignFieldType;
	private BaseDaoImpl<?, ?> foreignDao;
	private MappedQueryForFieldEq<Object, Object> mappedQueryForForeignField;

	/**
	 * ThreadLocal counters to detect initialization loops. Notice that there is _not_ an initValue() method on purpose.
	 * We don't want to create these if we don't have to.
	 */
	private static final ThreadLocal<LevelCounters> threadLevelCounters = new ThreadLocal<LevelCounters>();

	private static final Logger logger = LoggerFactory.getLogger(FieldType.class);

	/**
	 * You should use {@link FieldType#createFieldType} to instantiate one of these field if you have a {@link Field}.
	 */
	public FieldType(ConnectionSource connectionSource, String tableName, Field field, DatabaseFieldConfig fieldConfig,
			Class<?> parentClass) throws SQLException {
		this.connectionSource = connectionSource;
		this.tableName = tableName;
		DatabaseType databaseType = connectionSource.getDatabaseType();
		this.field = field;
		this.parentClass = parentClass;

		// post process our config settings
		fieldConfig.postProcess();

		Class<?> clazz = field.getType();
		DataPersister dataPersister;
		if (fieldConfig.getDataPersister() == null) {
			Class<? extends DataPersister> persisterClass = fieldConfig.getPersisterClass();
			if (persisterClass == null || persisterClass == VoidType.class) {
				dataPersister = DataPersisterManager.lookupForField(field);
			} else {
				Method method;
				try {
					method = persisterClass.getDeclaredMethod("getSingleton");
				} catch (Exception e) {
					throw SqlExceptionUtil
							.create("Could not find getSingleton static method on class " + persisterClass, e);
				}
				Object result;
				try {
					result = method.invoke(null);
				} catch (InvocationTargetException e) {
					throw SqlExceptionUtil.create("Could not run getSingleton method on class " + persisterClass,
							e.getTargetException());
				} catch (Exception e) {
					throw SqlExceptionUtil.create("Could not run getSingleton method on class " + persisterClass, e);
				}
				if (result == null) {
					throw new SQLException(
							"Static getSingleton method should not return null on class " + persisterClass);
				}
				try {
					dataPersister = (DataPersister) result;
				} catch (Exception e) {
					throw SqlExceptionUtil
							.create("Could not cast result of static getSingleton method to DataPersister from class "
									+ persisterClass, e);
				}
			}
		} else {
			dataPersister = fieldConfig.getDataPersister();
			if (!dataPersister.isValidForField(field)) {
				StringBuilder sb = new StringBuilder();
				sb.append("Field class ").append(clazz.getName());
				sb.append(" for field ").append(this);
				sb.append(" is not valid for type ").append(dataPersister);
				Class<?> primaryClass = dataPersister.getPrimaryClass();
				if (primaryClass != null) {
					sb.append(", maybe should be " + primaryClass);
				}
				throw new IllegalArgumentException(sb.toString());
			}
		}
		String foreignColumnName = fieldConfig.getForeignColumnName();
		String defaultFieldName = field.getName();
		if (fieldConfig.isForeign() || fieldConfig.isForeignAutoRefresh() || foreignColumnName != null) {
			if (dataPersister != null && dataPersister.isPrimitive()) {
				throw new IllegalArgumentException(
						"Field " + this + " is a primitive class " + clazz + " but marked as foreign");
			}
			if (foreignColumnName == null) {
				defaultFieldName = defaultFieldName + FOREIGN_ID_FIELD_SUFFIX;
			} else {
				defaultFieldName = defaultFieldName + "_" + foreignColumnName;
			}
			if (ForeignCollection.class.isAssignableFrom(clazz)) {
				throw new SQLException("Field '" + field.getName() + "' in class " + clazz + "' should use the @"
						+ ForeignCollectionField.class.getSimpleName() + " annotation not foreign=true");
			}
		} else if (fieldConfig.isForeignCollection()) {
			if (clazz != Collection.class && !ForeignCollection.class.isAssignableFrom(clazz)) {
				throw new SQLException("Field class for '" + field.getName() + "' must be of class "
						+ ForeignCollection.class.getSimpleName() + " or Collection.");
			}
			Type type = field.getGenericType();
			if (!(type instanceof ParameterizedType)) {
				throw new SQLException("Field class for '" + field.getName() + "' must be a parameterized Collection.");
			}
			Type[] genericArguments = ((ParameterizedType) type).getActualTypeArguments();
			if (genericArguments.length == 0) {
				// i doubt this will ever be reached
				throw new SQLException("Field class for '" + field.getName()
						+ "' must be a parameterized Collection with at least 1 type.");
			}
		} else if (dataPersister == null && (!fieldConfig.isForeignCollection())) {
			if (byte[].class.isAssignableFrom(clazz)) {
				throw new SQLException("ORMLite does not know how to store " + clazz + " for field '" + field.getName()
						+ "'. byte[] fields must specify dataType=DataType.BYTE_ARRAY or SERIALIZABLE");
			} else if (Serializable.class.isAssignableFrom(clazz)) {
				throw new SQLException("ORMLite does not know how to store " + clazz + " for field '" + field.getName()
						+ "'.  Use another class, custom persister, or to serialize it use "
						+ "dataType=DataType.SERIALIZABLE");
			} else {
				throw new IllegalArgumentException("ORMLite does not know how to store " + clazz + " for field "
						+ field.getName() + ". Use another class or a custom persister.");
			}
		}
		if (fieldConfig.getColumnName() == null) {
			this.columnName = defaultFieldName;
		} else {
			this.columnName = fieldConfig.getColumnName();
		}
		this.fieldConfig = fieldConfig;
		if (fieldConfig.isId()) {
			if (fieldConfig.isGeneratedId() || fieldConfig.getGeneratedIdSequence() != null) {
				throw new IllegalArgumentException(
						"Must specify one of id, generatedId, and generatedIdSequence with " + field.getName());
			}
			this.isId = true;
			this.isGeneratedId = false;
			this.generatedIdSequence = null;
		} else if (fieldConfig.isGeneratedId()) {
			if (fieldConfig.getGeneratedIdSequence() != null) {
				throw new IllegalArgumentException(
						"Must specify one of id, generatedId, and generatedIdSequence with " + field.getName());
			}
			this.isId = true;
			this.isGeneratedId = true;
			if (databaseType.isIdSequenceNeeded()) {
				this.generatedIdSequence = databaseType.generateIdSequenceName(tableName, this);
			} else {
				this.generatedIdSequence = null;
			}
		} else if (fieldConfig.getGeneratedIdSequence() != null) {
			this.isId = true;
			this.isGeneratedId = true;
			String seqName = fieldConfig.getGeneratedIdSequence();
			if (databaseType.isEntityNamesMustBeUpCase()) {
				seqName = databaseType.upCaseEntityName(seqName);
			}
			this.generatedIdSequence = seqName;
		} else {
			this.isId = false;
			this.isGeneratedId = false;
			this.generatedIdSequence = null;
		}
		if (this.isId && (fieldConfig.isForeign() || fieldConfig.isForeignAutoRefresh())) {
			throw new IllegalArgumentException("Id field " + field.getName() + " cannot also be a foreign object");
		}
		if (fieldConfig.isUseGetSet()) {
			this.fieldGetMethod = DatabaseFieldConfig.findGetMethod(field, true);
			this.fieldSetMethod = DatabaseFieldConfig.findSetMethod(field, true);
		} else {
			if (!field.isAccessible()) {
				try {
					this.field.setAccessible(true);
				} catch (SecurityException e) {
					throw new IllegalArgumentException("Could not open access to field " + field.getName()
							+ ".  You may have to set useGetSet=true to fix.");
				}
			}
			this.fieldGetMethod = null;
			this.fieldSetMethod = null;
		}
		if (fieldConfig.isAllowGeneratedIdInsert() && !fieldConfig.isGeneratedId()) {
			throw new IllegalArgumentException(
					"Field " + field.getName() + " must be a generated-id if allowGeneratedIdInsert = true");
		}
		if (fieldConfig.isForeignAutoRefresh() && !fieldConfig.isForeign()) {
			throw new IllegalArgumentException(
					"Field " + field.getName() + " must have foreign = true if foreignAutoRefresh = true");
		}
		if (fieldConfig.isForeignAutoCreate() && !fieldConfig.isForeign()) {
			throw new IllegalArgumentException(
					"Field " + field.getName() + " must have foreign = true if foreignAutoCreate = true");
		}
		if (fieldConfig.getForeignColumnName() != null && !fieldConfig.isForeign()) {
			throw new IllegalArgumentException(
					"Field " + field.getName() + " must have foreign = true if foreignColumnName is set");
		}
		if (fieldConfig.isVersion() && (dataPersister == null || !dataPersister.isValidForVersion())) {
			throw new IllegalArgumentException(
					"Field " + field.getName() + " is not a valid type to be a version field");
		}
		assignDataType(databaseType, dataPersister);
	}

	/**
	 * Because we go recursive in a lot of situations if we construct DAOs inside of the FieldType constructor, we have
	 * to do this 2nd pass initialization so we can better use the DAO caches.
	 * 
	 * @see BaseDaoImpl#initialize()
	 */
	public void configDaoInformation(ConnectionSource connectionSource, Class<?> parentClass) throws SQLException {
		Class<?> fieldClass = field.getType();
		DatabaseType databaseType = connectionSource.getDatabaseType();
		TableInfo<?, ?> foreignTableInfo;
		final FieldType foreignIdField;
		final FieldType foreignRefField;
		final FieldType foreignFieldType;
		final BaseDaoImpl<?, ?> foreignDao;
		final MappedQueryForFieldEq<Object, Object> mappedQueryForForeignField;

		String foreignColumnName = fieldConfig.getForeignColumnName();
		if (fieldConfig.isForeignAutoRefresh() || foreignColumnName != null) {
			DatabaseTableConfig<?> tableConfig = fieldConfig.getForeignTableConfig();
			if (tableConfig == null) {
				// NOTE: the cast is necessary for maven
				foreignDao = (BaseDaoImpl<?, ?>) DaoManager.createDao(connectionSource, fieldClass);
				foreignTableInfo = foreignDao.getTableInfo();
			} else {
				tableConfig.extractFieldTypes(connectionSource);
				// NOTE: the cast is necessary for maven
				foreignDao = (BaseDaoImpl<?, ?>) DaoManager.createDao(connectionSource, tableConfig);
				foreignTableInfo = foreignDao.getTableInfo();
			}
			foreignIdField = foreignTableInfo.getIdField();
			if (foreignIdField == null) {
				throw new IllegalArgumentException("Foreign field " + fieldClass + " does not have id field");
			}
			if (foreignColumnName == null) {
				foreignRefField = foreignIdField;
			} else {
				foreignRefField = foreignTableInfo.getFieldTypeByColumnName(foreignColumnName);
				if (foreignRefField == null) {
					throw new IllegalArgumentException(
							"Foreign field " + fieldClass + " does not have field named '" + foreignColumnName + "'");
				}
			}
			@SuppressWarnings("unchecked")
			MappedQueryForFieldEq<Object, Object> castMappedQueryForForeignField =
					(MappedQueryForFieldEq<Object, Object>) MappedQueryForFieldEq.build(databaseType, foreignTableInfo,
							foreignRefField);
			mappedQueryForForeignField = castMappedQueryForForeignField;
			foreignFieldType = null;
		} else if (fieldConfig.isForeign()) {
			if (this.dataPersister != null && this.dataPersister.isPrimitive()) {
				throw new IllegalArgumentException(
						"Field " + this + " is a primitive class " + fieldClass + " but marked as foreign");
			}
			DatabaseTableConfig<?> tableConfig = fieldConfig.getForeignTableConfig();
			if (tableConfig != null) {
				tableConfig.extractFieldTypes(connectionSource);
				// NOTE: the cast is necessary for maven
				foreignDao = (BaseDaoImpl<?, ?>) DaoManager.createDao(connectionSource, tableConfig);
			} else {
				/*
				 * Initially we were only doing this just for BaseDaoEnabled.class and isForeignAutoCreate(). But we
				 * need it also for foreign fields because the alternative was to use reflection. Chances are if it is
				 * foreign we're going to need the DAO in the future anyway so we might as well create it. This also
				 * allows us to make use of any table configs.
				 */
				// NOTE: the cast is necessary for maven
				foreignDao = (BaseDaoImpl<?, ?>) DaoManager.createDao(connectionSource, fieldClass);
			}
			foreignTableInfo = foreignDao.getTableInfo();
			foreignIdField = foreignTableInfo.getIdField();
			if (foreignIdField == null) {
				throw new IllegalArgumentException("Foreign field " + fieldClass + " does not have id field");
			}
			foreignRefField = foreignIdField;
			if (isForeignAutoCreate() && !foreignIdField.isGeneratedId()) {
				throw new IllegalArgumentException(
						"Field " + field.getName() + ", if foreignAutoCreate = true then class "
								+ fieldClass.getSimpleName() + " must have id field with generatedId = true");
			}
			foreignFieldType = null;
			mappedQueryForForeignField = null;
		} else if (fieldConfig.isForeignCollection()) {
			if (fieldClass != Collection.class && !ForeignCollection.class.isAssignableFrom(fieldClass)) {
				throw new SQLException("Field class for '" + field.getName() + "' must be of class "
						+ ForeignCollection.class.getSimpleName() + " or Collection.");
			}
			Type type = field.getGenericType();
			if (!(type instanceof ParameterizedType)) {
				throw new SQLException("Field class for '" + field.getName() + "' must be a parameterized Collection.");
			}
			Type[] genericArguments = ((ParameterizedType) type).getActualTypeArguments();
			if (genericArguments.length == 0) {
				// i doubt this will ever be reached
				throw new SQLException("Field class for '" + field.getName()
						+ "' must be a parameterized Collection with at least 1 type.");
			}

			// If argument is a type variable we need to get arguments from superclass
			if (genericArguments[0] instanceof TypeVariable) {
				genericArguments = ((ParameterizedType) parentClass.getGenericSuperclass()).getActualTypeArguments();
			}

			if (!(genericArguments[0] instanceof Class)) {
				throw new SQLException("Field class for '" + field.getName()
						+ "' must be a parameterized Collection whose generic argument is an entity class not: "
						+ genericArguments[0]);
			}
			Class<?> collectionClazz = (Class<?>) genericArguments[0];
			DatabaseTableConfig<?> tableConfig = fieldConfig.getForeignTableConfig();
			BaseDaoImpl<Object, Object> foundDao;
			if (tableConfig == null) {
				@SuppressWarnings("unchecked")
				BaseDaoImpl<Object, Object> castDao =
						(BaseDaoImpl<Object, Object>) DaoManager.createDao(connectionSource, collectionClazz);
				foundDao = castDao;
			} else {
				@SuppressWarnings("unchecked")
				BaseDaoImpl<Object, Object> castDao =
						(BaseDaoImpl<Object, Object>) DaoManager.createDao(connectionSource, tableConfig);
				foundDao = castDao;
			}
			foreignDao = foundDao;
			foreignFieldType = findForeignFieldType(collectionClazz, parentClass, (BaseDaoImpl<?, ?>) foundDao);
			foreignIdField = null;
			foreignRefField = null;
			foreignTableInfo = null;
			mappedQueryForForeignField = null;
		} else {
			foreignTableInfo = null;
			foreignIdField = null;
			foreignRefField = null;
			foreignFieldType = null;
			foreignDao = null;
			mappedQueryForForeignField = null;
		}

		this.mappedQueryForForeignField = mappedQueryForForeignField;
		this.foreignTableInfo = foreignTableInfo;
		this.foreignFieldType = foreignFieldType;
		this.foreignDao = foreignDao;
		this.foreignIdField = foreignIdField;
		this.foreignRefField = foreignRefField;

		// we have to do this because if we have a foreign field then our id type might have gone to an _id primitive
		if (this.foreignRefField != null) {
			assignDataType(databaseType, this.foreignRefField.getDataPersister());
		}
	}

	public Field getField() {
		return field;
	}

	public String getTableName() {
		return tableName;
	}

	public String getFieldName() {
		return field.getName();
	}

	/**
	 * Return the class of the field associated with this field type.
	 */
	public Class<?> getType() {
		return field.getType();
	}

	/**
	 * Return the generic type of the field associated with this field type.
	 */
	public Type getGenericType() {
		return field.getGenericType();
	}

	public String getColumnName() {
		return columnName;
	}

	public DataPersister getDataPersister() {
		return dataPersister;
	}

	public Object getDataTypeConfigObj() {
		return dataTypeConfigObj;
	}

	public SqlType getSqlType() {
		return fieldConverter.getSqlType();
	}

	/**
	 * Return the default value as parsed from the {@link DatabaseFieldConfig#getDefaultValue()} or null if no default
	 * value.
	 */
	public Object getDefaultValue() {
		return defaultValue;
	}

	public int getWidth() {
		return fieldConfig.getWidth();
	}

	public boolean isCanBeNull() {
		return fieldConfig.isCanBeNull();
	}

	/**
	 * Return whether the field is an id field. It is an id if {@link DatabaseField#id},
	 * {@link DatabaseField#generatedId}, OR {@link DatabaseField#generatedIdSequence} are enabled.
	 */
	public boolean isId() {
		return isId;
	}

	/**
	 * Return whether the field is a generated-id field. This is true if {@link DatabaseField#generatedId} OR
	 * {@link DatabaseField#generatedIdSequence} are enabled.
	 */
	public boolean isGeneratedId() {
		return isGeneratedId;
	}

	/**
	 * Return whether the field is a generated-id-sequence field. This is true if
	 * {@link DatabaseField#generatedIdSequence} is specified OR if {@link DatabaseField#generatedId} is enabled and the
	 * {@link DatabaseType#isIdSequenceNeeded} is enabled. If the latter is true then the sequence name will be
	 * auto-generated.
	 */
	public boolean isGeneratedIdSequence() {
		return generatedIdSequence != null;
	}

	/**
	 * Return the generated-id-sequence associated with the field or null if {@link #isGeneratedIdSequence} is false.
	 */
	public String getGeneratedIdSequence() {
		return generatedIdSequence;
	}

	public boolean isForeign() {
		return fieldConfig.isForeign();
	}

	/**
	 * Assign to the data object the val corresponding to the fieldType.
	 */
	public void assignField(Object data, Object val, boolean parentObject, ObjectCache objectCache)
			throws SQLException {
		if (logger.isLevelEnabled(Level.TRACE)) {
			logger.trace("assiging from data {}, val {}: {}", (data == null ? "null" : data.getClass()),
					(val == null ? "null" : val.getClass()), val);
		}
		// if this is a foreign object then val is the foreign object's id val
		if (foreignRefField != null && val != null) {
			// get the current field value which is the foreign-id
			Object foreignRef = extractJavaFieldValue(data);
			/*
			 * See if we don't need to create a new foreign object. If we are refreshing and the id field has not
			 * changed then there is no need to create a new foreign object and maybe lose previously refreshed field
			 * information.
			 */
			if (foreignRef != null && foreignRef.equals(val)) {
				return;
			}
			// awhitlock: raised as OrmLite issue: bug #122
			Object cachedVal;
			ObjectCache foreignCache = foreignDao.getObjectCache();
			if (foreignCache == null) {
				cachedVal = null;
			} else {
				cachedVal = foreignCache.get(getType(), val);
			}
			if (cachedVal != null) {
				val = cachedVal;
			} else if (!parentObject) {
				// the value we are to assign to our field is now the foreign object itself
				val = createForeignObject(val, objectCache);
			}
		}

		if (fieldSetMethod == null) {
			try {
				field.set(data, val);
			} catch (IllegalArgumentException e) {
				throw SqlExceptionUtil.create(
						"Could not assign object '" + val + "' of type " + val.getClass() + " to field " + this, e);
			} catch (IllegalAccessException e) {
				throw SqlExceptionUtil.create(
						"Could not assign object '" + val + "' of type " + val.getClass() + "' to field " + this, e);
			}
		} else {
			try {
				fieldSetMethod.invoke(data, val);
			} catch (Exception e) {
				throw SqlExceptionUtil
						.create("Could not call " + fieldSetMethod + " on object with '" + val + "' for " + this, e);
			}
		}
	}

	/**
	 * Assign an ID value to this field.
	 */
	public Object assignIdValue(Object data, Number val, ObjectCache objectCache) throws SQLException {
		Object idVal = dataPersister.convertIdNumber(val);
		if (idVal == null) {
			throw new SQLException("Invalid class " + dataPersister + " for sequence-id " + this);
		} else {
			assignField(data, idVal, false, objectCache);
			return idVal;
		}
	}

	/**
	 * Return the value from the field in the object that is defined by this FieldType.
	 */
	public <FV> FV extractRawJavaFieldValue(Object object) throws SQLException {
		Object val;
		if (fieldGetMethod == null) {
			try {
				// field object may not be a T yet
				val = field.get(object);
			} catch (Exception e) {
				throw SqlExceptionUtil.create("Could not get field value for " + this, e);
			}
		} else {
			try {
				val = fieldGetMethod.invoke(object);
			} catch (Exception e) {
				throw SqlExceptionUtil.create("Could not call " + fieldGetMethod + " for " + this, e);
			}
		}

		@SuppressWarnings("unchecked")
		FV converted = (FV) val;
		return converted;
	}

	/**
	 * Return the value from the field in the object that is defined by this FieldType. If the field is a foreign object
	 * then the ID of the field is returned instead.
	 */
	public Object extractJavaFieldValue(Object object) throws SQLException {

		Object val = extractRawJavaFieldValue(object);

		// if this is a foreign object then we want its reference field
		if (foreignRefField != null && val != null) {
			val = foreignRefField.extractRawJavaFieldValue(val);
		}

		return val;
	}

	/**
	 * Extract a field from an object and convert to something suitable to be passed to SQL as an argument.
	 */
	public Object extractJavaFieldToSqlArgValue(Object object) throws SQLException {
		return convertJavaFieldToSqlArgValue(extractJavaFieldValue(object));
	}

	/**
	 * Convert a field value to something suitable to be stored in the database.
	 */
	public Object convertJavaFieldToSqlArgValue(Object fieldVal) throws SQLException {
		/*
		 * Limitation here. Some people may want to override the null with their own value in the converter but we
		 * currently don't allow that. Specifying a default value I guess is a better mechanism.
		 */
		if (fieldVal == null) {
			return null;
		} else {
			return fieldConverter.javaToSqlArg(this, fieldVal);
		}
	}

	/**
	 * Convert a string value into the appropriate Java field value.
	 */
	public Object convertStringToJavaField(String value, int columnPos) throws SQLException {
		if (value == null) {
			return null;
		} else {
			return fieldConverter.resultStringToJava(this, value, columnPos);
		}
	}

	/**
	 * Move the SQL value to the next one for version processing.
	 */
	public Object moveToNextValue(Object val) throws SQLException {
		if (dataPersister == null) {
			return null;
		} else {
			return dataPersister.moveToNextValue(val);
		}
	}

	/**
	 * Return the id of the associated foreign object or null if none.
	 */
	public FieldType getForeignIdField() {
		return foreignIdField;
	}

	/**
	 * Return the field associated with the foreign object or null if none.
	 */
	public FieldType getForeignRefField() {
		return foreignRefField;
	}

	/**
	 * Call through to {@link DataPersister#isEscapedValue()}
	 */
	public boolean isEscapedValue() {
		return dataPersister.isEscapedValue();
	}

	public Enum<?> getUnknownEnumVal() {
		return fieldConfig.getUnknownEnumValue();
	}

	/**
	 * Return the format of the field.
	 */
	public String getFormat() {
		return fieldConfig.getFormat();
	}

	public boolean isUnique() {
		return fieldConfig.isUnique();
	}

	public boolean isUniqueCombo() {
		return fieldConfig.isUniqueCombo();
	}

	public String getIndexName() {
		return fieldConfig.getIndexName(tableName);
	}

	public String getUniqueIndexName() {
		return fieldConfig.getUniqueIndexName(tableName);
	}

	/**
	 * Call through to {@link DataPersister#isEscapedDefaultValue()}
	 */
	public boolean isEscapedDefaultValue() {
		return dataPersister.isEscapedDefaultValue();
	}

	/**
	 * Call through to {@link DataPersister#isComparable()}
	 */
	public boolean isComparable() throws SQLException {
		if (fieldConfig.isForeignCollection()) {
			return false;
		}
		/*
		 * We've seen dataPersister being null here in some strange cases. Why? It may because someone is searching on
		 * an improper field. Or maybe a table-config does not match the Java object?
		 */
		if (dataPersister == null) {
			throw new SQLException("Internal error.  Data-persister is not configured for field.  "
					+ "Please post _full_ exception with associated data objects to mailing list: " + this);
		} else {
			return dataPersister.isComparable();
		}
	}

	/**
	 * Call through to {@link DataPersister#isArgumentHolderRequired()}
	 */
	public boolean isArgumentHolderRequired() {
		return dataPersister.isArgumentHolderRequired();
	}

	/**
	 * Call through to {@link DatabaseFieldConfig#isForeignCollection()}
	 */
	public boolean isForeignCollection() {
		return fieldConfig.isForeignCollection();
	}

	/**
	 * Build and return a foreign collection based on the field settings that matches the id argument. This can return
	 * null in certain circumstances.
	 * 
	 * @param parent
	 *            The parent object that we will set on each item in the collection.
	 * @param id
	 *            The id of the foreign object we will look for. This can be null if we are creating an empty
	 *            collection.
	 */
	public <FT, FID> BaseForeignCollection<FT, FID> buildForeignCollection(Object parent, FID id) throws SQLException {
		// this can happen if we have a foreign-auto-refresh scenario
		if (foreignFieldType == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Dao<FT, FID> castDao = (Dao<FT, FID>) foreignDao;
		if (!fieldConfig.isForeignCollectionEager()) {
			// we know this won't go recursive so no need for the counters
			return new LazyForeignCollection<FT, FID>(castDao, parent, id, foreignFieldType,
					fieldConfig.getForeignCollectionOrderColumnName(), fieldConfig.isForeignCollectionOrderAscending());
		}

		// try not to create level counter objects unless we have to
		LevelCounters levelCounters = threadLevelCounters.get();
		if (levelCounters == null) {
			if (fieldConfig.getForeignCollectionMaxEagerLevel() == 0) {
				// then return a lazy collection instead
				return new LazyForeignCollection<FT, FID>(castDao, parent, id, foreignFieldType,
						fieldConfig.getForeignCollectionOrderColumnName(),
						fieldConfig.isForeignCollectionOrderAscending());
			}
			levelCounters = new LevelCounters();
			threadLevelCounters.set(levelCounters);
		}

		if (levelCounters.foreignCollectionLevel == 0) {
			levelCounters.foreignCollectionLevelMax = fieldConfig.getForeignCollectionMaxEagerLevel();
		}
		// are we over our level limit?
		if (levelCounters.foreignCollectionLevel >= levelCounters.foreignCollectionLevelMax) {
			// then return a lazy collection instead
			return new LazyForeignCollection<FT, FID>(castDao, parent, id, foreignFieldType,
					fieldConfig.getForeignCollectionOrderColumnName(), fieldConfig.isForeignCollectionOrderAscending());
		}
		levelCounters.foreignCollectionLevel++;
		try {
			return new EagerForeignCollection<FT, FID>(castDao, parent, id, foreignFieldType,
					fieldConfig.getForeignCollectionOrderColumnName(), fieldConfig.isForeignCollectionOrderAscending());
		} finally {
			levelCounters.foreignCollectionLevel--;
		}
	}

	/**
	 * Get the result object from the results. A call through to {@link FieldConverter#resultToJava}.
	 */
	public <T> T resultToJava(DatabaseResults results, Map<String, Integer> columnPositions) throws SQLException {
		Integer dbColumnPos = columnPositions.get(columnName);
		if (dbColumnPos == null) {
			dbColumnPos = results.findColumn(columnName);
			columnPositions.put(columnName, dbColumnPos);
		}

		/*
		 * Subtle problem here. If the field is a foreign-field and/or a primitive and the value was null then we get 0
		 * from results.getInt() which mirrors the ResultSet. We have to specifically test to see if we have a null
		 * result with results.wasNull(...) afterwards so we return a null value to not create the sub-object or turn a
		 * Integer field into 0 instead of null.
		 * 
		 * But this presents a limitation. Sometimes people want to convert a result field value that is stored in the
		 * database as null into a non-null value when it comes out of the database. There is no way to do that because
		 * of the results.wasNull(...) checks after the fact then override the non-null value from the converter.
		 */
		@SuppressWarnings("unchecked")
		T converted = (T) fieldConverter.resultToJava(this, results, dbColumnPos);
		if (fieldConfig.isForeign()) {
			/*
			 * If your foreign field is a primitive and the value was null then this would return 0 from
			 * results.getInt(). We have to specifically test to see if we have a foreign field so if it is null we
			 * return a null value to not create the sub-object.
			 */
			if (results.wasNull(dbColumnPos)) {
				return null;
			}
		} else if (dataPersister.isPrimitive()) {
			if (fieldConfig.isThrowIfNull() && results.wasNull(dbColumnPos)) {
				throw new SQLException(
						"Results value for primitive field '" + field.getName() + "' was an invalid null value");
			}
		} else if (!fieldConverter.isStreamType() && results.wasNull(dbColumnPos)) {
			// we can't check if we have a null if this is a stream type
			return null;
		}
		return converted;
	}

	/**
	 * Call through to {@link DataPersister#isSelfGeneratedId()}
	 */
	public boolean isSelfGeneratedId() {
		return dataPersister.isSelfGeneratedId();
	}

	/**
	 * Call through to {@link DatabaseFieldConfig#isAllowGeneratedIdInsert()}
	 */
	public boolean isAllowGeneratedIdInsert() {
		return fieldConfig.isAllowGeneratedIdInsert();
	}

	/**
	 * Call through to {@link DatabaseFieldConfig#getColumnDefinition()}
	 */
	public String getColumnDefinition() {
		return fieldConfig.getColumnDefinition();
	}

	/**
	 * Call through to {@link DatabaseFieldConfig#isForeignAutoCreate()}
	 */
	public boolean isForeignAutoCreate() {
		return fieldConfig.isForeignAutoCreate();
	}

	/**
	 * Call through to {@link DatabaseFieldConfig#isVersion()}
	 */
	public boolean isVersion() {
		return fieldConfig.isVersion();
	}

	/**
	 * Call through to {@link DataPersister#generateId()}
	 */
	public Object generateId() {
		return dataPersister.generateId();
	}

	/**
	 * Call through to {@link DatabaseFieldConfig#isReadOnly()}
	 */
	public boolean isReadOnly() {
		return fieldConfig.isReadOnly();
	}

	/**
	 * Return the value of field in the data argument if it is not the default value for the class. If it is the default
	 * then null is returned.
	 */
	public <FV> FV getFieldValueIfNotDefault(Object object) throws SQLException {
		@SuppressWarnings("unchecked")
		FV fieldValue = (FV) extractJavaFieldValue(object);
		if (isFieldValueDefault(fieldValue)) {
			return null;
		} else {
			return fieldValue;
		}
	}

	/**
	 * Return whether or not the data object has a default value passed for this field of this type.
	 */
	public boolean isObjectsFieldValueDefault(Object object) throws SQLException {
		Object fieldValue = extractJavaFieldValue(object);
		return isFieldValueDefault(fieldValue);
	}

	/**
	 * Return whether or not the field value passed in is the default value for the type of the field. Null will return
	 * true.
	 */
	public Object getJavaDefaultValueDefault() {
		if (field.getType() == boolean.class) {
			return DEFAULT_VALUE_BOOLEAN;
		} else if (field.getType() == byte.class || field.getType() == Byte.class) {
			return DEFAULT_VALUE_BYTE;
		} else if (field.getType() == char.class || field.getType() == Character.class) {
			return DEFAULT_VALUE_CHAR;
		} else if (field.getType() == short.class || field.getType() == Short.class) {
			return DEFAULT_VALUE_SHORT;
		} else if (field.getType() == int.class || field.getType() == Integer.class) {
			return DEFAULT_VALUE_INT;
		} else if (field.getType() == long.class || field.getType() == Long.class) {
			return DEFAULT_VALUE_LONG;
		} else if (field.getType() == float.class || field.getType() == Float.class) {
			return DEFAULT_VALUE_FLOAT;
		} else if (field.getType() == double.class || field.getType() == Double.class) {
			return DEFAULT_VALUE_DOUBLE;
		} else {
			return null;
		}
	}

	/**
	 * Pass the foreign data argument to the foreign {@link Dao#create(Object)} method.
	 */
	public <T> int createWithForeignDao(T foreignData) throws SQLException {
		@SuppressWarnings("unchecked")
		Dao<T, ?> castDao = (Dao<T, ?>) foreignDao;
		return castDao.create(foreignData);
	}

	/**
	 * Return An instantiated {@link FieldType} or null if the field does not have a {@link DatabaseField} annotation.
	 */
	public static FieldType createFieldType(ConnectionSource connectionSource, String tableName, Field field,
			Class<?> parentClass) throws SQLException {
		DatabaseType databaseType = connectionSource.getDatabaseType();
		DatabaseFieldConfig fieldConfig = DatabaseFieldConfig.fromField(databaseType, tableName, field);
		if (fieldConfig == null) {
			return null;
		} else {
			return new FieldType(connectionSource, tableName, field, fieldConfig, parentClass);
		}
	}

	@Override
	public boolean equals(Object arg) {
		if (arg == null || arg.getClass() != this.getClass()) {
			return false;
		}
		FieldType other = (FieldType) arg;
		return field.equals(other.field)
				&& (parentClass == null ? other.parentClass == null : parentClass.equals(other.parentClass));
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":name=" + field.getName() + ",class="
				+ field.getDeclaringClass().getSimpleName();
	}

	private Object createForeignObject(Object val, ObjectCache objectCache) throws SQLException {

		// try to stop the level counters objects from being created
		LevelCounters levelCounters = threadLevelCounters.get();
		if (levelCounters == null) {
			// only create a shell if we are not in auto-refresh mode
			if (!fieldConfig.isForeignAutoRefresh()) {
				return createForeignShell(val, objectCache);
			}
			levelCounters = new LevelCounters();
			threadLevelCounters.set(levelCounters);
		}

		// we record the current auto-refresh level which will be used along the way
		if (levelCounters.autoRefreshLevel == 0) {
			// if we aren't in an auto-refresh loop then don't _start_ an new loop if auto-refresh is not configured
			if (!fieldConfig.isForeignAutoRefresh()) {
				return createForeignShell(val, objectCache);
			}
			levelCounters.autoRefreshLevelMax = fieldConfig.getMaxForeignAutoRefreshLevel();
		}
		// if we have recursed the proper number of times, return a shell with just the id set
		if (levelCounters.autoRefreshLevel >= levelCounters.autoRefreshLevelMax) {
			return createForeignShell(val, objectCache);
		}

		/*
		 * We may not have a mapped query for id because we aren't auto-refreshing ourselves. But a parent class may be
		 * auto-refreshing us with a level > 1 so we may need to build our query-for-id optimization on the fly here.
		 */
		if (mappedQueryForForeignField == null) {
			@SuppressWarnings("unchecked")
			MappedQueryForFieldEq<Object, Object> castMappedQueryForId =
					(MappedQueryForFieldEq<Object, Object>) MappedQueryForFieldEq.build(
							connectionSource.getDatabaseType(), ((BaseDaoImpl<?, ?>) foreignDao).getTableInfo(),
							foreignIdField);
			mappedQueryForForeignField = castMappedQueryForId;
		}
		levelCounters.autoRefreshLevel++;
		try {
			DatabaseConnection databaseConnection = connectionSource.getReadOnlyConnection(tableName);
			try {
				// recurse and get the sub-object
				return mappedQueryForForeignField.execute(databaseConnection, val, objectCache);
			} finally {
				connectionSource.releaseConnection(databaseConnection);
			}
		} finally {
			levelCounters.autoRefreshLevel--;
			if (levelCounters.autoRefreshLevel <= 0) {
				threadLevelCounters.remove();
			}
		}
	}

	/**
	 * Create a shell object and assign its id field.
	 */
	private Object createForeignShell(Object val, ObjectCache objectCache) throws SQLException {
		Object foreignObject = foreignTableInfo.createObject();
		foreignIdField.assignField(foreignObject, val, false, objectCache);
		return foreignObject;
	}

	/**
	 * Return whether or not the field value passed in is the default value for the type of the field. Null will return
	 * true.
	 */
	private boolean isFieldValueDefault(Object fieldValue) {
		if (fieldValue == null) {
			return true;
		} else {
			return fieldValue.equals(getJavaDefaultValueDefault());
		}
	}

	/**
	 * If we have a class Foo with a collection of Bar's then we go through Bar's DAO looking for a Foo field. We need
	 * this field to build the query that is able to find all Bar's that have foo_id that matches our id.
	 */
	private FieldType findForeignFieldType(Class<?> clazz, Class<?> foreignClass, BaseDaoImpl<?, ?> foreignDao)
			throws SQLException {
		String foreignColumnName = fieldConfig.getForeignCollectionForeignFieldName();
		for (FieldType fieldType : foreignDao.getTableInfo().getFieldTypes()) {
			if (fieldType.getType() == foreignClass
					&& (foreignColumnName == null || fieldType.getField().getName().equals(foreignColumnName))) {
				if (!fieldType.fieldConfig.isForeign() && !fieldType.fieldConfig.isForeignAutoRefresh()) {
					// this may never be reached
					throw new SQLException("Foreign collection object " + clazz + " for field '" + field.getName()
							+ "' contains a field of class " + foreignClass + " but it's not foreign");
				}
				return fieldType;
			}
		}
		// build our complex error message
		StringBuilder sb = new StringBuilder();
		sb.append("Foreign collection class ").append(clazz.getName());
		sb.append(" for field '").append(field.getName()).append("' column-name does not contain a foreign field");
		if (foreignColumnName != null) {
			sb.append(" named '").append(foreignColumnName).append('\'');
		}
		sb.append(" of class ").append(foreignClass.getName());
		throw new SQLException(sb.toString());
	}

	/**
	 * Configure our data persister and any dependent fields. We have to do this here because both the constructor and
	 * {@link #configDaoInformation} method can set the data-type.
	 */
	private void assignDataType(DatabaseType databaseType, DataPersister dataPersister) throws SQLException {
		dataPersister = databaseType.getDataPersister(dataPersister, this);
		this.dataPersister = dataPersister;
		if (dataPersister == null) {
			if (!fieldConfig.isForeign() && !fieldConfig.isForeignCollection()) {
				// may never happen but let's be careful out there
				throw new SQLException("Data persister for field " + this
						+ " is null but the field is not a foreign or foreignCollection");
			}
			return;
		}
		this.fieldConverter = databaseType.getFieldConverter(dataPersister, this);
		if (this.isGeneratedId && !dataPersister.isValidGeneratedType()) {
			StringBuilder sb = new StringBuilder();
			sb.append("Generated-id field '").append(field.getName());
			sb.append("' in ").append(field.getDeclaringClass().getSimpleName());
			sb.append(" can't be type ").append(dataPersister.getSqlType());
			sb.append(".  Must be one of: ");
			for (DataType dataType : DataType.values()) {
				DataPersister persister = dataType.getDataPersister();
				if (persister != null && persister.isValidGeneratedType()) {
					sb.append(dataType).append(' ');
				}
			}
			throw new IllegalArgumentException(sb.toString());
		}
		if (fieldConfig.isThrowIfNull() && !dataPersister.isPrimitive()) {
			throw new SQLException("Field " + field.getName() + " must be a primitive if set with throwIfNull");
		}
		if (this.isId && !dataPersister.isAppropriateId()) {
			throw new SQLException("Field '" + field.getName() + "' is of data type " + dataPersister
					+ " which cannot be the ID field");
		}
		this.dataTypeConfigObj = dataPersister.makeConfigObject(this);
		String defaultStr = fieldConfig.getDefaultValue();
		if (defaultStr == null) {
			this.defaultValue = null;
		} else if (this.isGeneratedId) {
			throw new SQLException("Field '" + field.getName() + "' cannot be a generatedId and have a default value '"
					+ defaultStr + "'");
		} else {
			this.defaultValue = this.fieldConverter.parseDefaultString(this, defaultStr);
		}
	}

	private static class LevelCounters {

		LevelCounters() {
			// NOTE: package perms to removed synthetic accessor
		}

		// current auto-refresh recursion level
		int autoRefreshLevel;
		// maximum auto-refresh recursion level
		int autoRefreshLevelMax;

		// current foreign-collection recursion level
		int foreignCollectionLevel;
		// maximum foreign-collection recursion level
		int foreignCollectionLevelMax;
	}
}
