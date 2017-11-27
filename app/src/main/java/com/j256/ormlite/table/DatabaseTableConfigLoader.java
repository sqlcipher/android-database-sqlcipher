package com.j256.ormlite.table;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.field.DatabaseFieldConfig;
import com.j256.ormlite.field.DatabaseFieldConfigLoader;
import com.j256.ormlite.misc.SqlExceptionUtil;

/**
 * Database table configuration loader which reads and writes {@link DatabaseTableConfig} from a text file/stream.
 * 
 * @author graywatson
 */
public class DatabaseTableConfigLoader {

	private static final String CONFIG_FILE_START_MARKER = "# --table-start--";
	private static final String CONFIG_FILE_END_MARKER = "# --table-end--";
	private static final String CONFIG_FILE_FIELDS_START = "# --table-fields-start--";
	private static final String CONFIG_FILE_FIELDS_END = "# --table-fields-end--";

	/**
	 * Load in a number of database configuration entries from a buffered reader.
	 */
	public static List<DatabaseTableConfig<?>> loadDatabaseConfigFromReader(BufferedReader reader) throws SQLException {
		List<DatabaseTableConfig<?>> list = new ArrayList<DatabaseTableConfig<?>>();
		while (true) {
			DatabaseTableConfig<?> config = DatabaseTableConfigLoader.fromReader(reader);
			if (config == null) {
				break;
			}
			list.add(config);
		}
		return list;
	}

	/**
	 * Load a table configuration in from a text-file reader.
	 * 
	 * @return A config if any of the fields were set otherwise null if we reach EOF.
	 */
	public static <T> DatabaseTableConfig<T> fromReader(BufferedReader reader) throws SQLException {
		DatabaseTableConfig<T> config = new DatabaseTableConfig<T>();
		boolean anything = false;
		while (true) {
			String line;
			try {
				line = reader.readLine();
			} catch (IOException e) {
				throw SqlExceptionUtil.create("Could not read DatabaseTableConfig from stream", e);
			}
			if (line == null) {
				break;
			}
			// we do this so we can support multiple class configs per file
			if (line.equals(CONFIG_FILE_END_MARKER)) {
				break;
			}
			// we do this so we can support multiple class configs per file
			if (line.equals(CONFIG_FILE_FIELDS_START)) {
				readFields(reader, config);
				continue;
			}
			// skip empty lines or comments
			if (line.length() == 0 || line.startsWith("#") || line.equals(CONFIG_FILE_START_MARKER)) {
				continue;
			}
			String[] parts = line.split("=", -2);
			if (parts.length != 2) {
				throw new SQLException("DatabaseTableConfig reading from stream cannot parse line: " + line);
			}
			readTableField(config, parts[0], parts[1]);
			anything = true;
		}
		// if we got any config lines then we return the config
		if (anything) {
			return config;
		} else {
			// otherwise we return null for none
			return null;
		}
	}

	/**
	 * Write the table configuration to a buffered writer.
	 */
	public static <T> void write(BufferedWriter writer, DatabaseTableConfig<T> config) throws SQLException {
		try {
			writeConfig(writer, config);
		} catch (IOException e) {
			throw SqlExceptionUtil.create("Could not write config to writer", e);
		}
	}

	// field names in the config file
	private static final String FIELD_NAME_DATA_CLASS = "dataClass";
	private static final String FIELD_NAME_TABLE_NAME = "tableName";

	/**
	 * Write the config to the writer.
	 */
	private static <T> void writeConfig(BufferedWriter writer, DatabaseTableConfig<T> config) throws IOException,
			SQLException {
		writer.append(CONFIG_FILE_START_MARKER);
		writer.newLine();
		if (config.getDataClass() != null) {
			writer.append(FIELD_NAME_DATA_CLASS).append('=').append(config.getDataClass().getName());
			writer.newLine();
		}
		if (config.getTableName() != null) {
			writer.append(FIELD_NAME_TABLE_NAME).append('=').append(config.getTableName());
			writer.newLine();
		}
		writer.append(CONFIG_FILE_FIELDS_START);
		writer.newLine();
		if (config.getFieldConfigs() != null) {
			for (DatabaseFieldConfig field : config.getFieldConfigs()) {
				DatabaseFieldConfigLoader.write(writer, field, config.getTableName());
			}
		}
		writer.append(CONFIG_FILE_FIELDS_END);
		writer.newLine();
		writer.append(CONFIG_FILE_END_MARKER);
		writer.newLine();
	}

	/**
	 * Read a field into our table configuration for field=value line.
	 */
	private static <T> void readTableField(DatabaseTableConfig<T> config, String field, String value) {
		if (field.equals(FIELD_NAME_DATA_CLASS)) {
			try {
				@SuppressWarnings("unchecked")
				Class<T> clazz = (Class<T>) Class.forName(value);
				config.setDataClass(clazz);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Unknown class specified for dataClass: " + value);
			}
		} else if (field.equals(FIELD_NAME_TABLE_NAME)) {
			config.setTableName(value);
		}
	}

	/**
	 * Read all of the fields information from the configuration file.
	 */
	private static <T> void readFields(BufferedReader reader, DatabaseTableConfig<T> config) throws SQLException {
		List<DatabaseFieldConfig> fields = new ArrayList<DatabaseFieldConfig>();
		while (true) {
			String line;
			try {
				line = reader.readLine();
			} catch (IOException e) {
				throw SqlExceptionUtil.create("Could not read next field from config file", e);
			}
			if (line == null || line.equals(CONFIG_FILE_FIELDS_END)) {
				break;
			}
			DatabaseFieldConfig fieldConfig = DatabaseFieldConfigLoader.fromReader(reader);
			if (fieldConfig == null) {
				break;
			}
			fields.add(fieldConfig);
		}
		config.setFieldConfigs(fields);
	}
}
