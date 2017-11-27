package com.j256.ormlite.field;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.j256.ormlite.field.types.EnumStringType;

/**
 * Class used to manage the various data types used by the system. The bulk of the data types come from the
 * {@link DataType} enumerated fields although you can also register your own here using the
 * {@link #registerDataPersisters(DataPersister...)} method.
 * 
 * @author graywatson
 */
public class DataPersisterManager {

	private static final DataPersister DEFAULT_ENUM_PERSISTER = EnumStringType.getSingleton();

	private static final Map<String, DataPersister> builtInMap;
	private static List<DataPersister> registeredPersisters = null;

	static {
		// add our built-in persisters
		builtInMap = new HashMap<String, DataPersister>();
		for (DataType dataType : DataType.values()) {
			DataPersister persister = dataType.getDataPersister();
			if (persister != null) {
				for (Class<?> clazz : persister.getAssociatedClasses()) {
					builtInMap.put(clazz.getName(), persister);
				}
				String[] associatedClassNames = persister.getAssociatedClassNames();
				if (associatedClassNames != null) {
					for (String className : persister.getAssociatedClassNames()) {
						builtInMap.put(className, persister);
					}
				}
			}
		}
	}

	private DataPersisterManager() {
		// only for static methods
	}

	/**
	 * Register a data type with the manager.
	 */
	public static void registerDataPersisters(DataPersister... dataPersisters) {
		// we build the map and replace it to lower the chance of concurrency issues
		List<DataPersister> newList = new ArrayList<DataPersister>();
		if (registeredPersisters != null) {
			newList.addAll(registeredPersisters);
		}
		for (DataPersister persister : dataPersisters) {
			newList.add(persister);
		}
		registeredPersisters = newList;
	}

	/**
	 * Remove any previously persisters that were registered with {@link #registerDataPersisters(DataPersister...)}.
	 */
	public static void clear() {
		registeredPersisters = null;
	}

	/**
	 * Lookup the data-type associated with the class.
	 * 
	 * @return The associated data-type interface or null if none found.
	 */
	public static DataPersister lookupForField(Field field) {

		// see if the any of the registered persisters are valid first
		if (registeredPersisters != null) {
			for (DataPersister persister : registeredPersisters) {
				if (persister.isValidForField(field)) {
					return persister;
				}
				// check the classes instead
				for (Class<?> clazz : persister.getAssociatedClasses()) {
					if (field.getType() == clazz) {
						return persister;
					}
				}
			}
		}

		// look it up in our built-in map by class
		DataPersister dataPersister = builtInMap.get(field.getType().getName());
		if (dataPersister != null) {
			return dataPersister;
		}

		/*
		 * Special case for enum types. We can't put this in the registered persisters because we want people to be able
		 * to override it.
		 */
		if (field.getType().isEnum()) {
			return DEFAULT_ENUM_PERSISTER;
		} else {
			/*
			 * Serializable classes return null here because we don't want them to be automatically configured for
			 * forwards compatibility with future field types that happen to be Serializable.
			 */
			return null;
		}
	}
}
