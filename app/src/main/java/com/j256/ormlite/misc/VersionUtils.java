package com.j256.ormlite.misc;

import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;

/**
 * A class which helps us verify that we are running symmetric versions.
 * 
 * @author graywatson
 */
public class VersionUtils {

	private static final String CORE_VERSION = "VERSION__5.1-SNAPSHOT__";

	private static Logger logger;
	private static boolean thrownOnErrors = false;
	private static String coreVersion = CORE_VERSION;

	private VersionUtils() {
		// only for static methods
	}

	/**
	 * Verifies that the ormlite-core and -jdbc version files hold the same string.
	 */
	public static final void checkCoreVersusJdbcVersions(String jdbcVersion) {
		logVersionWarnings("core", coreVersion, "jdbc", jdbcVersion);
	}

	/**
	 * Verifies that the ormlite-core and -android version files hold the same string.
	 */
	public static final void checkCoreVersusAndroidVersions(String androidVersion) {
		logVersionWarnings("core", coreVersion, "android", androidVersion);
	}

	public static String getCoreVersion() {
		return coreVersion;
	}

	/**
	 * For testing purposes.
	 */
	static void setThrownOnErrors(boolean thrownOnErrors) {
		VersionUtils.thrownOnErrors = thrownOnErrors;
	}

	/**
	 * Log error information
	 */
	private static void logVersionWarnings(String label1, String version1, String label2, String version2) {
		if (version1 == null) {
			if (version2 != null) {
				warning(null, "Unknown version", " for {}, version for {} is '{}'", new Object[] { label1, label2,
						version2 });
			}
		} else {
			if (version2 == null) {
				warning(null, "Unknown version", " for {}, version for {} is '{}'", new Object[] { label2, label1,
						version1 });
			} else if (!version1.equals(version2)) {
				warning(null, "Mismatched versions", ": {} is '{}', while {} is '{}'", new Object[] { label1, version1,
						label2, version2 });
			}
		}
	}

	private static void warning(Throwable th, String msg, String format, Object[] args) {
		getLogger().warn(th, msg + format, args);
		if (VersionUtils.thrownOnErrors) {
			throw new IllegalStateException("See error log for details:" + msg);
		}
	}

	/**
	 * Get the logger for the class. We do this so we don't have to create it all of the time.
	 */
	private static Logger getLogger() {
		if (logger == null) {
			logger = LoggerFactory.getLogger(VersionUtils.class);
		}
		return logger;
	}
}
