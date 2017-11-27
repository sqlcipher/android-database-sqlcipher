package com.j256.ormlite.android;

import android.util.Log;

import com.j256.ormlite.logger.LoggerFactory;

/**
 * Implementation of our logger which delegates to the internal Android logger.
 * 
 * <p>
 * To see log messages you will do something like:
 * </p>
 * 
 * <pre>
 * adb shell setprop log.tag.OrmLiteBaseActivity VERBOSE
 * </pre>
 * 
 * <p>
 * <b>NOTE:</b> Unfortunately, Android variables are limited in size so this class takes that last 23 (sic) characters
 * of the class name if it is larger than 23 characters. For example, if the class is AndroidDatabaseConnection you
 * would do:
 * </p>
 * 
 * <pre>
 * adb shell setprop log.tag.droidDatabaseConnection VERBOSE
 * </pre>
 * 
 * <p>
 * To see all ORMLite log messages use:
 * </p>
 * 
 * <pre>
 * adb shell setprop log.tag.ORMLite DEBUG
 * </pre>
 * 
 * @author graywatson
 */
public class AndroidLog implements com.j256.ormlite.logger.Log {

	private final static String ALL_LOGS_NAME = "ORMLite";
	private final static int REFRESH_LEVEL_CACHE_EVERY = 200;

	private final static int MAX_TAG_LENGTH = 23;
	private String className;

	// we do this because supposedly Log.isLoggable() always does IO
	private volatile int levelCacheC = 0;
	private final boolean levelCache[];

	public AndroidLog(String className) {
		// get the last part of the class name
		this.className = LoggerFactory.getSimpleClassName(className);
		// make sure that our tag length is not too long
		int length = this.className.length();
		if (length > MAX_TAG_LENGTH) {
			this.className = this.className.substring(length - MAX_TAG_LENGTH, length);
		}
		// find the maximum level value
		int maxLevel = 0;
		for (com.j256.ormlite.logger.Log.Level level : com.j256.ormlite.logger.Log.Level.values()) {
			int androidLevel = levelToAndroidLevel(level);
			if (androidLevel > maxLevel) {
				maxLevel = androidLevel;
			}
		}
		levelCache = new boolean[maxLevel + 1];
		refreshLevelCache();
	}

	@Override
	public boolean isLevelEnabled(Level level) {
		// we don't care if this is not synchronized, it will be updated sooner or later and multiple updates are fine.
		if (++levelCacheC >= REFRESH_LEVEL_CACHE_EVERY) {
			refreshLevelCache();
			levelCacheC = 0;
		}
		int androidLevel = levelToAndroidLevel(level);
		if (androidLevel < levelCache.length) {
			return levelCache[androidLevel];
		} else {
			return isLevelEnabledInternal(androidLevel);
		}
	}

	@Override
	public void log(Level level, String msg) {
		switch (level) {
			case TRACE :
				Log.v(className, msg);
				break;
			case DEBUG :
				Log.d(className, msg);
				break;
			case INFO :
				Log.i(className, msg);
				break;
			case WARNING :
				Log.w(className, msg);
				break;
			case ERROR :
				Log.e(className, msg);
				break;
			case FATAL :
				Log.e(className, msg);
				break;
			default :
				Log.i(className, msg);
				break;
		}
	}

	@Override
	public void log(Level level, String msg, Throwable t) {
		switch (level) {
			case TRACE :
				Log.v(className, msg, t);
				break;
			case DEBUG :
				Log.d(className, msg, t);
				break;
			case INFO :
				Log.i(className, msg, t);
				break;
			case WARNING :
				Log.w(className, msg, t);
				break;
			case ERROR :
				Log.e(className, msg, t);
				break;
			case FATAL :
				Log.e(className, msg, t);
				break;
			default :
				Log.i(className, msg, t);
				break;
		}
	}

	private void refreshLevelCache() {
		for (com.j256.ormlite.logger.Log.Level level : com.j256.ormlite.logger.Log.Level.values()) {
			int androidLevel = levelToAndroidLevel(level);
			if (androidLevel < levelCache.length) {
				levelCache[androidLevel] = isLevelEnabledInternal(androidLevel);
			}
		}
	}

	private boolean isLevelEnabledInternal(int androidLevel) {
		// this is supposedly expensive with an IO operation for each call so we cache them into levelCache[]
		return Log.isLoggable(className, androidLevel) || Log.isLoggable(ALL_LOGS_NAME, androidLevel);
	}

	private int levelToAndroidLevel(com.j256.ormlite.logger.Log.Level level) {
		switch (level) {
			case TRACE :
				return Log.VERBOSE;
			case DEBUG :
				return Log.DEBUG;
			case INFO :
				return Log.INFO;
			case WARNING :
				return Log.WARN;
			case ERROR :
				return Log.ERROR;
			case FATAL :
				return Log.ERROR;
			default :
				return Log.INFO;
		}
	}
}
