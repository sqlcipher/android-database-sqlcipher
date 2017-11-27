package com.j256.ormlite.support;

import java.sql.SQLException;

import com.j256.ormlite.logger.Logger;

/**
 * Connection source base class which provides the save/clear mechanism using a thread local.
 * 
 * @author graywatson
 */
public abstract class BaseConnectionSource implements ConnectionSource {

	private ThreadLocal<NestedConnection> specialConnection = new ThreadLocal<NestedConnection>();

	@Override
	public DatabaseConnection getSpecialConnection(String tableName) {
		NestedConnection currentSaved = specialConnection.get();
		if (currentSaved == null) {
			return null;
		} else {
			return currentSaved.connection;
		}
	}

	/**
	 * Returns the connection that has been saved or null if none.
	 */
	protected DatabaseConnection getSavedConnection() {
		NestedConnection nested = specialConnection.get();
		if (nested == null) {
			return null;
		} else {
			return nested.connection;
		}
	}

	/**
	 * Return true if the connection being released is the one that has been saved.
	 */
	protected boolean isSavedConnection(DatabaseConnection connection) {
		NestedConnection currentSaved = specialConnection.get();
		if (currentSaved == null) {
			return false;
		} else if (currentSaved.connection == connection) {
			// ignore the release when we have a saved connection
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Save this connection as our special connection to be returned by the {@link #getSavedConnection()} method.
	 * 
	 * @return True if the connection was saved or false if it was already saved.
	 */
	protected boolean saveSpecial(DatabaseConnection connection) throws SQLException {
		// check for a connection already saved
		NestedConnection currentSaved = specialConnection.get();
		if (currentSaved == null) {
			specialConnection.set(new NestedConnection(connection));
			return true;
		} else {
			if (currentSaved.connection != connection) {
				throw new SQLException("trying to save connection " + connection
						+ " but already have saved connection " + currentSaved.connection);
			}
			// we must have a save call within another save
			currentSaved.increment();
			return false;
		}
	}

	/**
	 * Clear the connection that was previously saved.
	 * 
	 * @return True if the connection argument had been saved.
	 */
	protected boolean clearSpecial(DatabaseConnection connection, Logger logger) {
		NestedConnection currentSaved = specialConnection.get();
		boolean cleared = false;
		if (connection == null) {
			// ignored
		} else if (currentSaved == null) {
			logger.error("no connection has been saved when clear() called");
		} else if (currentSaved.connection == connection) {
			if (currentSaved.decrementAndGet() == 0) {
				// we only clear the connection if nested counter is 0
				specialConnection.set(null);
			}
			cleared = true;
		} else {
			logger.error("connection saved {} is not the one being cleared {}", currentSaved.connection, connection);
		}
		// release should then be called after clear
		return cleared;
	}

	/**
	 * Return true if the two connections seem to one one connection under the covers.
	 */
	protected boolean isSingleConnection(DatabaseConnection conn1, DatabaseConnection conn2) throws SQLException {
		// initialize the connections auto-commit flags
		conn1.setAutoCommit(true);
		conn2.setAutoCommit(true);
		try {
			// change conn1's auto-commit to be false
			conn1.setAutoCommit(false);
			if (conn2.isAutoCommit()) {
				// if the 2nd connection's auto-commit is still true then we have multiple connections
				return false;
			} else {
				// if the 2nd connection's auto-commit is also false then we have a single connection
				return true;
			}
		} finally {
			// restore its auto-commit
			conn1.setAutoCommit(true);
		}
	}

	private static class NestedConnection {
		public final DatabaseConnection connection;
		private int nestedC;

		public NestedConnection(DatabaseConnection connection) {
			this.connection = connection;
			this.nestedC = 1;
		}

		public void increment() {
			nestedC++;
		}

		public int decrementAndGet() {
			nestedC--;
			return nestedC;
		}
	}
}
