package com.j256.ormlite.logger;

import java.lang.reflect.Constructor;

import com.j256.ormlite.logger.Log.Level;

/**
 * Factory that creates {@link Logger} instances. It uses reflection to see what loggers are installed on the system and
 * tries to find the most appropriate one.
 * 
 * <p>
 * To set the logger to a particular type, set the system property ("com.j256.ormlite.logger.type") contained in
 * {@link #LOG_TYPE_SYSTEM_PROPERTY} ("com.j256.ormlite.logger.type") to be one of the values in
 * {@link LoggerFactory.LogType} enum.
 * </p>
 */
public class LoggerFactory {

	public static final String LOG_TYPE_SYSTEM_PROPERTY = "com.j256.ormlite.logger.type";
	private static LogType logType;

	/**
	 * For static calls only.
	 */
	private LoggerFactory() {
	}

	/**
	 * Return a logger associated with a particular class.
	 */
	public static Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}

	/**
	 * Return a logger associated with a particular class name.
	 */
	public static Logger getLogger(String className) {
		if (logType == null) {
			logType = findLogType();
		}
		return new Logger(logType.createLog(className));
	}

	/**
	 * Return the single class name from a class-name string.
	 */
	public static String getSimpleClassName(String className) {
		// get the last part of the class name
		String[] parts = className.split("\\.");
		if (parts.length <= 1) {
			return className;
		} else {
			return parts[parts.length - 1];
		}
	}

	/**
	 * Return the most appropriate log type. This should _never_ return null.
	 */
	private static LogType findLogType() {

		// see if the log-type was specified as a system property
		String logTypeString = System.getProperty(LOG_TYPE_SYSTEM_PROPERTY);
		if (logTypeString != null) {
			try {
				return LogType.valueOf(logTypeString);
			} catch (IllegalArgumentException e) {
				Log log = new LocalLog(LoggerFactory.class.getName());
				log.log(Level.WARNING, "Could not find valid log-type from system property '" + LOG_TYPE_SYSTEM_PROPERTY
						+ "', value '" + logTypeString + "'");
			}
		}

		for (LogType logType : LogType.values()) {
			if (logType.isAvailable()) {
				return logType;
			}
		}
		// fall back is always LOCAL, never reached
		return LogType.LOCAL;
	}

	/**
	 * Type of internal logs supported. This is package permissions for testing.
	 */
	public enum LogType {
		SLF4J("org.slf4j.LoggerFactory", "com.j256.ormlite.logger.Slf4jLoggingLog"),
		/**
		 * WARNING: Android log must be _before_ commons logging since Android provides commons logging but logging
		 * messages are ignored that are sent there. Grrrrr.
		 */
		ANDROID("android.util.Log", "com.j256.ormlite.android.AndroidLog"),
		COMMONS_LOGGING("org.apache.commons.logging.LogFactory", "com.j256.ormlite.logger.CommonsLoggingLog"),
		LOG4J2("org.apache.logging.log4j.LogManager", "com.j256.ormlite.logger.Log4j2Log"),
		LOG4J("org.apache.log4j.Logger", "com.j256.ormlite.logger.Log4jLog"),
		// this should always be at the end as the fall-back, so it's always available
		LOCAL(LocalLog.class.getName(), LocalLog.class.getName()) {
			@Override
			public Log createLog(String classLabel) {
				return new LocalLog(classLabel);
			}

			@Override
			public boolean isAvailable() {
				// always available
				return true;
			}
		},
		// we put this down here because it's always available but we rarely want to use it
		JAVA_UTIL("java.util.logging.Logger", "com.j256.ormlite.logger.JavaUtilLog"),
		// end
		;

		private final String detectClassName;
		private final String logClassName;

		private LogType(String detectClassName, String logClassName) {
			this.detectClassName = detectClassName;
			this.logClassName = logClassName;
		}

		/**
		 * Create and return a Log class for this type.
		 */
		public Log createLog(String classLabel) {
			try {
				return createLogFromClassName(classLabel);
			} catch (Exception e) {
				// oh well, fall back to the local log
				Log log = new LocalLog(classLabel);
				log.log(Level.WARNING, "Unable to call constructor with single String argument for class "
						+ logClassName + ", so had to use local log: " + e.getMessage());
				return log;
			}
		}

		/**
		 * Return true if the log class is available.
		 */
		public boolean isAvailable() {
			if (!isAvailableTestClass()) {
				return false;
			}
			try {
				// try to actually use the logger which resolves problems with the Android stub
				Log log = createLogFromClassName(getClass().getName());
				log.isLevelEnabled(Level.INFO);
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		/**
		 * Try to create the log from the class name which may throw.
		 */
		private Log createLogFromClassName(String classLabel) throws Exception {
			Class<?> clazz = Class.forName(logClassName);
			@SuppressWarnings("unchecked")
			Constructor<Log> constructor = (Constructor<Log>) clazz.getConstructor(String.class);
			return constructor.newInstance(classLabel);
		}

		/**
		 * Is this class available meaning that we should use this logger. This is package permissions for testing
		 * purposes.
		 */
		boolean isAvailableTestClass() {
			try {
				Class.forName(detectClassName);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}
}
