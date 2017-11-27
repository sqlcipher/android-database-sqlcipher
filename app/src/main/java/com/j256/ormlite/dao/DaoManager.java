package com.j256.ormlite.dao;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTable;
import com.j256.ormlite.table.DatabaseTableConfig;

/**
 * Class which caches created DAOs. Sometimes internal DAOs are used to support such features as auto-refreshing of
 * foreign fields or collections of sub-objects. Since instantiation of the DAO is a bit expensive, this class is used
 * in an attempt to only create a DAO once for each class.
 * 
 * <p>
 * <b>NOTE:</b> To use this cache, you should make sure you've added a {@link DatabaseTable#daoClass()} value to the
 * annotation to the top of your class.
 * </p>
 * 
 * @author graywatson
 */
public class DaoManager {

	private static Map<Class<?>, DatabaseTableConfig<?>> configMap = null;
	private static Map<ClassConnectionSource, Dao<?, ?>> classMap = null;
	private static Map<TableConfigConnectionSource, Dao<?, ?>> tableConfigMap = null;

	private static Logger logger = LoggerFactory.getLogger(DaoManager.class);

	/**
	 * Helper method to create a DAO object without having to define a class. This checks to see if the DAO has already
	 * been created. If not then it is a call through to {@link BaseDaoImpl#createDao(ConnectionSource, Class)}.
	 */
	public synchronized static <D extends Dao<T, ?>, T> D createDao(ConnectionSource connectionSource, Class<T> clazz)
			throws SQLException {
		if (connectionSource == null) {
			throw new IllegalArgumentException("connectionSource argument cannot be null");
		}
		ClassConnectionSource key = new ClassConnectionSource(connectionSource, clazz);
		Dao<?, ?> dao = lookupDao(key);
		if (dao != null) {
			@SuppressWarnings("unchecked")
			D castDao = (D) dao;
			return castDao;
		}

		// see if we can build it from source
		dao = createDaoFromConfig(connectionSource, clazz);
		if (dao != null) {
			@SuppressWarnings("unchecked")
			D castDao = (D) dao;
			return castDao;
		}

		DatabaseTable databaseTable = clazz.getAnnotation(DatabaseTable.class);
		if (databaseTable == null || databaseTable.daoClass() == Void.class
				|| databaseTable.daoClass() == BaseDaoImpl.class) {
			// see if the database type has some special table config extract method (Android)
			DatabaseType databaseType = connectionSource.getDatabaseType();
			DatabaseTableConfig<T> config = databaseType.extractDatabaseTableConfig(connectionSource, clazz);
			Dao<T, ?> daoTmp;
			if (config == null) {
				daoTmp = BaseDaoImpl.createDao(connectionSource, clazz);
			} else {
				daoTmp = BaseDaoImpl.createDao(connectionSource, config);
			}
			dao = daoTmp;
			logger.debug("created dao for class {} with reflection", clazz);
		} else {
			Class<?> daoClass = databaseTable.daoClass();
			Object[] arguments = new Object[] { connectionSource, clazz };
			// look first for the constructor with a class parameter in case it is a generic dao
			Constructor<?> daoConstructor = findConstructor(daoClass, arguments);
			if (daoConstructor == null) {
				// then look for the constructor with just the ConnectionSource
				arguments = new Object[] { connectionSource };
				daoConstructor = findConstructor(daoClass, arguments);
				if (daoConstructor == null) {
					throw new SQLException(
							"Could not find public constructor with ConnectionSource and optional Class parameters "
									+ daoClass + ".  Missing static on class?");
				}
			}
			try {
				dao = (Dao<?, ?>) daoConstructor.newInstance(arguments);
				logger.debug("created dao for class {} from constructor", clazz);
			} catch (Exception e) {
				throw SqlExceptionUtil.create("Could not call the constructor in class " + daoClass, e);
			}
		}

		registerDao(connectionSource, dao);
		@SuppressWarnings("unchecked")
		D castDao = (D) dao;
		return castDao;
	}

	/**
	 * Helper method to lookup a DAO if it has already been associated with the class. Otherwise this returns null.
	 */
	public synchronized static <D extends Dao<T, ?>, T> D lookupDao(ConnectionSource connectionSource, Class<T> clazz) {
		if (connectionSource == null) {
			throw new IllegalArgumentException("connectionSource argument cannot be null");
		}
		ClassConnectionSource key = new ClassConnectionSource(connectionSource, clazz);
		Dao<?, ?> dao = lookupDao(key);
		@SuppressWarnings("unchecked")
		D castDao = (D) dao;
		return castDao;
	}

	/**
	 * Helper method to create a DAO object without having to define a class. This checks to see if the DAO has already
	 * been created. If not then it is a call through to
	 * {@link BaseDaoImpl#createDao(ConnectionSource, DatabaseTableConfig)}.
	 */
	public synchronized static <D extends Dao<T, ?>, T> D createDao(ConnectionSource connectionSource,
			DatabaseTableConfig<T> tableConfig) throws SQLException {
		if (connectionSource == null) {
			throw new IllegalArgumentException("connectionSource argument cannot be null");
		}
		return doCreateDao(connectionSource, tableConfig);
	}

	/**
	 * Helper method to lookup a DAO if it has already been associated with the table-config. Otherwise this returns
	 * null.
	 */
	public synchronized static <D extends Dao<T, ?>, T> D lookupDao(ConnectionSource connectionSource,
			DatabaseTableConfig<T> tableConfig) {
		if (connectionSource == null) {
			throw new IllegalArgumentException("connectionSource argument cannot be null");
		}
		TableConfigConnectionSource key = new TableConfigConnectionSource(connectionSource, tableConfig);
		Dao<?, ?> dao = lookupDao(key);
		if (dao == null) {
			return null;
		} else {
			@SuppressWarnings("unchecked")
			D castDao = (D) dao;
			return castDao;
		}
	}

	/**
	 * Register the DAO with the cache. This will allow folks to build a DAO externally and then register so it can be
	 * used internally as necessary.
	 * 
	 * <p>
	 * <b>NOTE:</b> By default this registers the DAO to be associated with the class that it uses. If you need to
	 * register multiple dao's that use different {@link DatabaseTableConfig}s then you should use
	 * {@link #registerDaoWithTableConfig(ConnectionSource, Dao)}.
	 * </p>
	 * 
	 * <p>
	 * <b>NOTE:</b> You should maybe use the {@link DatabaseTable#daoClass()} and have the DaoManager construct the DAO
	 * if possible.
	 * </p>
	 */
	public static synchronized void registerDao(ConnectionSource connectionSource, Dao<?, ?> dao) {
		if (connectionSource == null) {
			throw new IllegalArgumentException("connectionSource argument cannot be null");
		}
		addDaoToClassMap(new ClassConnectionSource(connectionSource, dao.getDataClass()), dao);
	}

	/**
	 * Remove a DAO from the cache. This is necessary if we've registered it already but it throws an exception during
	 * configuration.
	 */
	public static synchronized void unregisterDao(ConnectionSource connectionSource, Dao<?, ?> dao) {
		if (connectionSource == null) {
			throw new IllegalArgumentException("connectionSource argument cannot be null");
		}
		removeDaoToClassMap(new ClassConnectionSource(connectionSource, dao.getDataClass()), dao);
	}

	/**
	 * Same as {@link #registerDao(ConnectionSource, Dao)} but this allows you to register it just with its
	 * {@link DatabaseTableConfig}. This allows multiple versions of the DAO to be configured if necessary.
	 */
	public static synchronized void registerDaoWithTableConfig(ConnectionSource connectionSource, Dao<?, ?> dao) {
		if (connectionSource == null) {
			throw new IllegalArgumentException("connectionSource argument cannot be null");
		}
		if (dao instanceof BaseDaoImpl) {
			DatabaseTableConfig<?> tableConfig = ((BaseDaoImpl<?, ?>) dao).getTableConfig();
			if (tableConfig != null) {
				addDaoToTableMap(new TableConfigConnectionSource(connectionSource, tableConfig), dao);
				return;
			}
		}
		addDaoToClassMap(new ClassConnectionSource(connectionSource, dao.getDataClass()), dao);
	}

	/**
	 * Clear out all of internal caches.
	 */
	public static synchronized void clearCache() {
		if (configMap != null) {
			configMap.clear();
			configMap = null;
		}
		clearDaoCache();
	}

	/**
	 * Clear out our DAO caches.
	 */
	public static synchronized void clearDaoCache() {
		if (classMap != null) {
			classMap.clear();
			classMap = null;
		}
		if (tableConfigMap != null) {
			tableConfigMap.clear();
			tableConfigMap = null;
		}
	}

	/**
	 * This adds database table configurations to the internal cache which can be used to speed up DAO construction.
	 * This is especially true of Android and other mobile platforms.
	 */
	public static synchronized void addCachedDatabaseConfigs(Collection<DatabaseTableConfig<?>> configs) {
		Map<Class<?>, DatabaseTableConfig<?>> newMap;
		if (configMap == null) {
			newMap = new HashMap<Class<?>, DatabaseTableConfig<?>>();
		} else {
			newMap = new HashMap<Class<?>, DatabaseTableConfig<?>>(configMap);
		}
		for (DatabaseTableConfig<?> config : configs) {
			newMap.put(config.getDataClass(), config);
			logger.info("Loaded configuration for {}", config.getDataClass());
		}
		configMap = newMap;
	}

	private static void addDaoToClassMap(ClassConnectionSource key, Dao<?, ?> dao) {
		if (classMap == null) {
			classMap = new HashMap<ClassConnectionSource, Dao<?, ?>>();
		}
		classMap.put(key, dao);
	}

	private static void removeDaoToClassMap(ClassConnectionSource key, Dao<?, ?> dao) {
		if (classMap != null) {
			classMap.remove(key);
		}
	}

	private static void addDaoToTableMap(TableConfigConnectionSource key, Dao<?, ?> dao) {
		if (tableConfigMap == null) {
			tableConfigMap = new HashMap<TableConfigConnectionSource, Dao<?, ?>>();
		}
		tableConfigMap.put(key, dao);
	}

	private static <T> Dao<?, ?> lookupDao(ClassConnectionSource key) {
		if (classMap == null) {
			classMap = new HashMap<ClassConnectionSource, Dao<?, ?>>();
		}
		Dao<?, ?> dao = classMap.get(key);
		if (dao == null) {
			return null;
		} else {
			return dao;
		}
	}

	private static <T> Dao<?, ?> lookupDao(TableConfigConnectionSource key) {
		if (tableConfigMap == null) {
			tableConfigMap = new HashMap<TableConfigConnectionSource, Dao<?, ?>>();
		}
		Dao<?, ?> dao = tableConfigMap.get(key);
		if (dao == null) {
			return null;
		} else {
			return dao;
		}
	}

	private static Constructor<?> findConstructor(Class<?> daoClass, Object[] params) {
		for (Constructor<?> constructor : daoClass.getConstructors()) {
			Class<?>[] paramsTypes = constructor.getParameterTypes();
			if (paramsTypes.length == params.length) {
				boolean match = true;
				for (int i = 0; i < paramsTypes.length; i++) {
					if (!paramsTypes[i].isAssignableFrom(params[i].getClass())) {
						match = false;
						break;
					}
				}
				if (match) {
					return constructor;
				}
			}
		}
		return null;
	}

	/**
	 * Creates the DAO if we have config information cached and caches the DAO.
	 */
	private static <D, T> D createDaoFromConfig(ConnectionSource connectionSource, Class<T> clazz) throws SQLException {
		// no loaded configs
		if (configMap == null) {
			return null;
		}

		@SuppressWarnings("unchecked")
		DatabaseTableConfig<T> config = (DatabaseTableConfig<T>) configMap.get(clazz);
		// if we don't config information cached return null
		if (config == null) {
			return null;
		}

		// else create a DAO using configuration
		Dao<T, ?> configedDao = doCreateDao(connectionSource, config);
		@SuppressWarnings("unchecked")
		D castDao = (D) configedDao;
		return castDao;
	}

	private static <D extends Dao<T, ?>, T> D doCreateDao(ConnectionSource connectionSource,
			DatabaseTableConfig<T> tableConfig) throws SQLException {
		TableConfigConnectionSource tableKey = new TableConfigConnectionSource(connectionSource, tableConfig);
		// look up in the table map
		Dao<?, ?> dao = lookupDao(tableKey);
		if (dao != null) {
			@SuppressWarnings("unchecked")
			D castDao = (D) dao;
			return castDao;
		}

		// now look it up in the class map
		Class<T> dataClass = tableConfig.getDataClass();
		ClassConnectionSource classKey = new ClassConnectionSource(connectionSource, dataClass);
		dao = lookupDao(classKey);
		if (dao != null) {
			// if it is not in the table map but is in the class map, add it
			addDaoToTableMap(tableKey, dao);
			@SuppressWarnings("unchecked")
			D castDao = (D) dao;
			return castDao;
		}

		// build the DAO using the table information
		DatabaseTable databaseTable = tableConfig.getDataClass().getAnnotation(DatabaseTable.class);
		if (databaseTable == null || databaseTable.daoClass() == Void.class
				|| databaseTable.daoClass() == BaseDaoImpl.class) {
			Dao<T, ?> daoTmp = BaseDaoImpl.createDao(connectionSource, tableConfig);
			dao = daoTmp;
		} else {
			Class<?> daoClass = databaseTable.daoClass();
			Object[] arguments = new Object[] { connectionSource, tableConfig };
			Constructor<?> constructor = findConstructor(daoClass, arguments);
			if (constructor == null) {
				throw new SQLException(
						"Could not find public constructor with ConnectionSource, DatabaseTableConfig parameters in class "
								+ daoClass);
			}
			try {
				dao = (Dao<?, ?>) constructor.newInstance(arguments);
			} catch (Exception e) {
				throw SqlExceptionUtil.create("Could not call the constructor in class " + daoClass, e);
			}
		}

		addDaoToTableMap(tableKey, dao);
		logger.debug("created dao for class {} from table config", dataClass);

		// if it is not in the class config either then add it
		if (lookupDao(classKey) == null) {
			addDaoToClassMap(classKey, dao);
		}

		@SuppressWarnings("unchecked")
		D castDao = (D) dao;
		return castDao;
	}

	/**
	 * Key for our class DAO map.
	 */
	private static class ClassConnectionSource {
		ConnectionSource connectionSource;
		Class<?> clazz;
		public ClassConnectionSource(ConnectionSource connectionSource, Class<?> clazz) {
			this.connectionSource = connectionSource;
			this.clazz = clazz;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = prime + clazz.hashCode();
			result = prime * result + connectionSource.hashCode();
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			ClassConnectionSource other = (ClassConnectionSource) obj;
			if (!clazz.equals(other.clazz)) {
				return false;
			} else if (!connectionSource.equals(other.connectionSource)) {
				return false;
			} else {
				return true;
			}
		}
	}

	/**
	 * Key for our table-config DAO map.
	 */
	private static class TableConfigConnectionSource {
		ConnectionSource connectionSource;
		DatabaseTableConfig<?> tableConfig;
		public TableConfigConnectionSource(ConnectionSource connectionSource, DatabaseTableConfig<?> tableConfig) {
			this.connectionSource = connectionSource;
			this.tableConfig = tableConfig;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = prime + tableConfig.hashCode();
			result = prime * result + connectionSource.hashCode();
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			TableConfigConnectionSource other = (TableConfigConnectionSource) obj;
			if (!tableConfig.equals(other.tableConfig)) {
				return false;
			} else if (!connectionSource.equals(other.connectionSource)) {
				return false;
			} else {
				return true;
			}
		}
	}
}
