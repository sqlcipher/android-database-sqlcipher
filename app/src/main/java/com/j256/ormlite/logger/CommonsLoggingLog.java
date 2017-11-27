package com.j256.ormlite.logger;

/**
 * Class which implements our Log interface by delegating to the Apache commons logging classes.
 * 
 * @author graywatson
 */
public class CommonsLoggingLog implements Log {

	private final org.apache.commons.logging.Log log;

	public CommonsLoggingLog(String className) {
		this.log = org.apache.commons.logging.LogFactory.getLog(className);
	}

	@Override
	public boolean isLevelEnabled(Level level) {
		switch (level) {
			case TRACE :
				return log.isTraceEnabled();
			case DEBUG :
				return log.isDebugEnabled();
			case INFO :
				return log.isInfoEnabled();
			case WARNING :
				return log.isWarnEnabled();
			case ERROR :
				return log.isErrorEnabled();
			case FATAL :
				return log.isFatalEnabled();
			default :
				return log.isInfoEnabled();
		}
	}

	@Override
	public void log(Level level, String msg) {
		switch (level) {
			case TRACE :
				log.trace(msg);
				break;
			case DEBUG :
				log.debug(msg);
				break;
			case INFO :
				log.info(msg);
				break;
			case WARNING :
				log.warn(msg);
				break;
			case ERROR :
				log.error(msg);
				break;
			case FATAL :
				log.fatal(msg);
				break;
			default :
				log.info(msg);
				break;
		}
	}

	@Override
	public void log(Level level, String msg, Throwable t) {
		switch (level) {
			case TRACE :
				log.trace(msg, t);
				break;
			case DEBUG :
				log.debug(msg, t);
				break;
			case INFO :
				log.info(msg, t);
				break;
			case WARNING :
				log.warn(msg, t);
				break;
			case ERROR :
				log.error(msg, t);
				break;
			case FATAL :
				log.fatal(msg, t);
				break;
			default :
				log.info(msg, t);
				break;
		}
	}
}
