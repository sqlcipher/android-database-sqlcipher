package com.j256.ormlite.logger;

/**
 * Interface so we can front various log code which may or may not be in the classpath.
 * 
 * @author graywatson
 */
public interface Log {

	/**
	 * Returns true if the log mode is in trace or higher.
	 */
	public boolean isLevelEnabled(Level level);

	/**
	 * Log a trace message.
	 */
	public void log(Level level, String message);

	/**
	 * Log a trace message with a throwable.
	 */
	public void log(Level level, String message, Throwable t);

	/**
	 * Level of log messages being sent.
	 */
	public enum Level {
		/** for tracing messages that are very verbose */
		TRACE(1),
		/** messages suitable for debugging purposes */
		DEBUG(2),
		/** information messages */
		INFO(3),
		/** warning messages */
		WARNING(4),
		/** error messages */
		ERROR(5),
		/** severe fatal messages */
		FATAL(6),
		// end
		;

		private int level;

		private Level(int level) {
			this.level = level;
		}

		/**
		 * Return whether or not a level argument is enabled for this level value. So,
		 * {@code Level.INFO.isEnabled(Level.WARN)} returns true but {@code Level.INFO.isEnabled(Level.DEBUG)} returns
		 * false.
		 */
		public boolean isEnabled(Level otherLevel) {
			return level <= otherLevel.level;
		}
	}
}
