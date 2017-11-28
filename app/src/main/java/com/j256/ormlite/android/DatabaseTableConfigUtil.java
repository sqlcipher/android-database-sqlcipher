package com.j256.ormlite.android;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.DataPersister;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.DatabaseTableConfig;

/**
 * Class which uses reflection to make the job of processing the {@link DatabaseField} annotation more efficient. In
 * current (as of 11/2011) versions of Android, Annotations are ghastly slow. This uses reflection on the Android
 * classes to work around this issue. Gross and a hack but a significant (~20x) performance improvement.
 * 
 * <p>
 * Thanks much go to Josh Guilfoyle for the idea and the code framework to make this happen.
 * </p>
 * 
 * @author joshguilfoyle, graywatson
 */
public class DatabaseTableConfigUtil {

	/**
	 * Set this system property to any value to disable the annotations hack which seems to cause problems on certain
	 * operating systems.
	 */
	public static final String DISABLE_ANNOTATION_HACK_SYSTEM_PROPERTY = "ormlite.annotation.hack.disable";

	private static Class<?> annotationFactoryClazz;
	private static Field elementsField;
	private static Class<?> annotationMemberClazz;
	private static Field nameField;
	private static Field valueField;
	private static int workedC = 0;

	private static final int[] configFieldNums;

	static {
		/*
		 * If we are dealing with older versions of the OS and if we've not disabled the annotation hack...
		 */
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH
				&& System.getProperty(DISABLE_ANNOTATION_HACK_SYSTEM_PROPERTY) == null) {
			configFieldNums = lookupClasses();
		} else {
			configFieldNums = null;
		}
	}

	/**
	 * Build our list table config from a class using some annotation fu around.
	 */
	public static <T> DatabaseTableConfig<T> fromClass(ConnectionSource connectionSource, Class<T> clazz)
			throws SQLException {
		DatabaseType databaseType = connectionSource.getDatabaseType();
		String tableName = DatabaseTableConfig.extractTableName(clazz);
		List<DatabaseFieldConfig> fieldConfigs = new ArrayList<DatabaseFieldConfig>();
		for (Class<?> classWalk = clazz; classWalk != null; classWalk = classWalk.getSuperclass()) {
			for (Field field : classWalk.getDeclaredFields()) {
				DatabaseFieldConfig config = configFromField(databaseType, tableName, field);
				if (config != null && config.isPersisted()) {
					fieldConfigs.add(config);
				}
			}
		}
		if (fieldConfigs.size() == 0) {
			return null;
		} else {
			return new DatabaseTableConfig<T>(clazz, tableName, fieldConfigs);
		}
	}

	/**
	 * Return the number of fields configured using our reflection hack. This is for testing.
	 */
	public static int getWorkedC() {
		return workedC;
	}

	/**
	 * This does all of the class reflection fu to find our classes, find the order of field names, and construct our
	 * array of ConfigField entries the correspond to the AnnotationMember array.
	 */
	private static int[] lookupClasses() {
		Class<?> annotationMemberArrayClazz;
		try {
			annotationFactoryClazz = Class.forName("org.apache.harmony.lang.annotation.AnnotationFactory");
			annotationMemberClazz = Class.forName("org.apache.harmony.lang.annotation.AnnotationMember");
			annotationMemberArrayClazz = Class.forName("[Lorg.apache.harmony.lang.annotation.AnnotationMember;");
		} catch (ClassNotFoundException e) {
			return null;
		}

		Field fieldField;
		try {
			elementsField = annotationFactoryClazz.getDeclaredField("elements");
			elementsField.setAccessible(true);

			nameField = annotationMemberClazz.getDeclaredField("name");
			nameField.setAccessible(true);
			valueField = annotationMemberClazz.getDeclaredField("value");
			valueField.setAccessible(true);

			fieldField = DatabaseFieldSample.class.getDeclaredField("field");
		} catch (SecurityException e) {
			return null;
		} catch (NoSuchFieldException e) {
			return null;
		}

		DatabaseField databaseField = fieldField.getAnnotation(DatabaseField.class);
		InvocationHandler proxy = Proxy.getInvocationHandler(databaseField);
		if (proxy.getClass() != annotationFactoryClazz) {
			return null;
		}

		try {
			// this should be an array of AnnotationMember objects
			Object elements = elementsField.get(proxy);
			if (elements == null || elements.getClass() != annotationMemberArrayClazz) {
				return null;
			}

			Object[] elementArray = (Object[]) elements;
			int[] configNums = new int[elementArray.length];

			// build our array of field-numbers that match the AnnotationMember array
			for (int i = 0; i < elementArray.length; i++) {
				String name = (String) nameField.get(elementArray[i]);
				configNums[i] = configFieldNameToNum(name);
			}
			return configNums;
		} catch (IllegalAccessException e) {
			return null;
		}
	}

	/*
	 * NOTE: we are doing this instead of an enum (which otherwise would be much better) because we don't want to take
	 * the class-size hit that comes with each enum being its own class.
	 */
	private static final int COLUMN_NAME = 1;
	private static final int DATA_TYPE = 2;
	private static final int DEFAULT_VALUE = 3;
	private static final int WIDTH = 4;
	private static final int CAN_BE_NULL = 5;
	private static final int ID = 6;
	private static final int GENERATED_ID = 7;
	private static final int GENERATED_ID_SEQUENCE = 8;
	private static final int FOREIGN = 9;
	private static final int USE_GET_SET = 10;
	private static final int UNKNOWN_ENUM_NAME = 11;
	private static final int THROW_IF_NULL = 12;
	private static final int PERSISTED = 13;
	private static final int FORMAT = 14;
	private static final int UNIQUE = 15;
	private static final int UNIQUE_COMBO = 16;
	private static final int INDEX = 17;
	private static final int UNIQUE_INDEX = 18;
	private static final int INDEX_NAME = 19;
	private static final int UNIQUE_INDEX_NAME = 20;
	private static final int FOREIGN_AUTO_REFRESH = 21;
	private static final int MAX_FOREIGN_AUTO_REFRESH_LEVEL = 22;
	private static final int PERSISTER_CLASS = 23;
	private static final int ALLOW_GENERATED_ID_INSERT = 24;
	private static final int COLUMN_DEFINITON = 25;
	private static final int FOREIGN_AUTO_CREATE = 26;
	private static final int VERSION = 27;
	private static final int FOREIGN_COLUMN_NAME = 28;
	private static final int READ_ONLY = 29;

	/**
	 * Convert the name of the @DatabaseField fields into a number for easy processing later.
	 */
	private static int configFieldNameToNum(String configName) {
		if (configName.equals("columnName")) {
			return COLUMN_NAME;
		} else if (configName.equals("dataType")) {
			return DATA_TYPE;
		} else if (configName.equals("defaultValue")) {
			return DEFAULT_VALUE;
		} else if (configName.equals("width")) {
			return WIDTH;
		} else if (configName.equals("canBeNull")) {
			return CAN_BE_NULL;
		} else if (configName.equals("id")) {
			return ID;
		} else if (configName.equals("generatedId")) {
			return GENERATED_ID;
		} else if (configName.equals("generatedIdSequence")) {
			return GENERATED_ID_SEQUENCE;
		} else if (configName.equals("foreign")) {
			return FOREIGN;
		} else if (configName.equals("useGetSet")) {
			return USE_GET_SET;
		} else if (configName.equals("unknownEnumName")) {
			return UNKNOWN_ENUM_NAME;
		} else if (configName.equals("throwIfNull")) {
			return THROW_IF_NULL;
		} else if (configName.equals("persisted")) {
			return PERSISTED;
		} else if (configName.equals("format")) {
			return FORMAT;
		} else if (configName.equals("unique")) {
			return UNIQUE;
		} else if (configName.equals("uniqueCombo")) {
			return UNIQUE_COMBO;
		} else if (configName.equals("index")) {
			return INDEX;
		} else if (configName.equals("uniqueIndex")) {
			return UNIQUE_INDEX;
		} else if (configName.equals("indexName")) {
			return INDEX_NAME;
		} else if (configName.equals("uniqueIndexName")) {
			return UNIQUE_INDEX_NAME;
		} else if (configName.equals("foreignAutoRefresh")) {
			return FOREIGN_AUTO_REFRESH;
		} else if (configName.equals("maxForeignAutoRefreshLevel")) {
			return MAX_FOREIGN_AUTO_REFRESH_LEVEL;
		} else if (configName.equals("persisterClass")) {
			return PERSISTER_CLASS;
		} else if (configName.equals("allowGeneratedIdInsert")) {
			return ALLOW_GENERATED_ID_INSERT;
		} else if (configName.equals("columnDefinition")) {
			return COLUMN_DEFINITON;
		} else if (configName.equals("foreignAutoCreate")) {
			return FOREIGN_AUTO_CREATE;
		} else if (configName.equals("version")) {
			return VERSION;
		} else if (configName.equals("foreignColumnName")) {
			return FOREIGN_COLUMN_NAME;
		} else if (configName.equals("readOnly")) {
			return READ_ONLY;
		} else {
			throw new IllegalStateException("Could not find support for DatabaseField " + configName);
		}
	}

	/**
	 * Extract our configuration information from the field by looking for a {@link DatabaseField} annotation.
	 */
	private static DatabaseFieldConfig configFromField(DatabaseType databaseType, String tableName, Field field)
			throws SQLException {

		if (configFieldNums == null) {
			return DatabaseFieldConfig.fromField(databaseType, tableName, field);
		}

		/*
		 * This, unfortunately, we can't get around. This creates a AnnotationFactory, an array of AnnotationMember
		 * fields, and possibly another array of AnnotationMember values. This creates a lot of GC'd objects.
		 */
		DatabaseField databaseField = field.getAnnotation(DatabaseField.class);

		DatabaseFieldConfig config = null;
		try {
			if (databaseField != null) {
				config = buildConfig(databaseField, tableName, field);
			}
		} catch (Exception e) {
			// ignored so we will configure normally below
		}

		if (config == null) {
			/*
			 * We configure this the old way because we might be using javax annotations, have a ForeignCollectionField,
			 * or may still be using the deprecated annotations. At this point we know that there isn't a @DatabaseField
			 * or we can't do our reflection hacks for some reason.
			 */
			return DatabaseFieldConfig.fromField(databaseType, tableName, field);
		} else {
			workedC++;
			return config;
		}
	}

	/**
	 * Instead of calling the annotation methods directly, we peer inside the proxy and investigate the array of
	 * AnnotationMember objects stored by the AnnotationFactory.
	 */
	private static DatabaseFieldConfig buildConfig(DatabaseField databaseField, String tableName, Field field)
			throws Exception {
		InvocationHandler proxy = Proxy.getInvocationHandler(databaseField);
		if (proxy.getClass() != annotationFactoryClazz) {
			return null;
		}
		// this should be an array of AnnotationMember objects
		Object elementsObject = elementsField.get(proxy);
		if (elementsObject == null) {
			return null;
		}
		DatabaseFieldConfig config = new DatabaseFieldConfig(field.getName());
		Object[] objs = (Object[]) elementsObject;
		for (int i = 0; i < configFieldNums.length; i++) {
			Object value = valueField.get(objs[i]);
			if (value != null) {
				assignConfigField(configFieldNums[i], config, field, value);
			}
		}
		return config;
	}

	/**
	 * Converts from field/value from the {@link DatabaseField} annotation to {@link DatabaseFieldConfig} values. This
	 * is very specific to this annotation.
	 */
	private static void assignConfigField(int configNum, DatabaseFieldConfig config, Field field, Object value) {
		switch (configNum) {
			case COLUMN_NAME :
				config.setColumnName(valueIfNotBlank((String) value));
				break;
			case DATA_TYPE :
				config.setDataType((DataType) value);
				break;
			case DEFAULT_VALUE :
				String defaultValue = (String) value;
				if (!(defaultValue == null || defaultValue.equals(DatabaseField.DEFAULT_STRING))) {
					config.setDefaultValue(defaultValue);
				}
				break;
			case WIDTH :
				config.setWidth((Integer) value);
				break;
			case CAN_BE_NULL :
				config.setCanBeNull((Boolean) value);
				break;
			case ID :
				config.setId((Boolean) value);
				break;
			case GENERATED_ID :
				config.setGeneratedId((Boolean) value);
				break;
			case GENERATED_ID_SEQUENCE :
				config.setGeneratedIdSequence(valueIfNotBlank((String) value));
				break;
			case FOREIGN :
				config.setForeign((Boolean) value);
				break;
			case USE_GET_SET :
				config.setUseGetSet((Boolean) value);
				break;
			case UNKNOWN_ENUM_NAME :
				config.setUnknownEnumValue(DatabaseFieldConfig.findMatchingEnumVal(field, (String) value));
				break;
			case THROW_IF_NULL :
				config.setThrowIfNull((Boolean) value);
				break;
			case PERSISTED :
				config.setPersisted((Boolean) value);
				break;
			case FORMAT :
				config.setFormat(valueIfNotBlank((String) value));
				break;
			case UNIQUE :
				config.setUnique((Boolean) value);
				break;
			case UNIQUE_COMBO :
				config.setUniqueCombo((Boolean) value);
				break;
			case INDEX :
				config.setIndex((Boolean) value);
				break;
			case UNIQUE_INDEX :
				config.setUniqueIndex((Boolean) value);
				break;
			case INDEX_NAME :
				config.setIndexName(valueIfNotBlank((String) value));
				break;
			case UNIQUE_INDEX_NAME :
				config.setUniqueIndexName(valueIfNotBlank((String) value));
				break;
			case FOREIGN_AUTO_REFRESH :
				config.setForeignAutoRefresh((Boolean) value);
				break;
			case MAX_FOREIGN_AUTO_REFRESH_LEVEL :
				config.setMaxForeignAutoRefreshLevel((Integer) value);
				break;
			case PERSISTER_CLASS :
				@SuppressWarnings("unchecked")
				Class<? extends DataPersister> clazz = (Class<? extends DataPersister>) value;
				config.setPersisterClass(clazz);
				break;
			case ALLOW_GENERATED_ID_INSERT :
				config.setAllowGeneratedIdInsert((Boolean) value);
				break;
			case COLUMN_DEFINITON :
				config.setColumnDefinition(valueIfNotBlank((String) value));
				break;
			case FOREIGN_AUTO_CREATE :
				config.setForeignAutoCreate((Boolean) value);
				break;
			case VERSION :
				config.setVersion((Boolean) value);
				break;
			case FOREIGN_COLUMN_NAME :
				config.setForeignColumnName(valueIfNotBlank((String) value));
				break;
			case READ_ONLY :
				config.setReadOnly((Boolean) value);
				break;
			default :
				throw new IllegalStateException("Could not find support for DatabaseField number " + configNum);
		}
	}

	private static String valueIfNotBlank(String value) {
		if (value == null || value.length() == 0) {
			return null;
		} else {
			return value;
		}
	}

	/**
	 * Class used to investigate the @DatabaseField annotation.
	 */
	private static class DatabaseFieldSample {
		@DatabaseField
		String field;
	}
}
