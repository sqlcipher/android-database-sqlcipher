package net.sqlcipher.database;

/**
 * An interface to perform pre and post key operations against a database.
 */
public interface SQLiteDatabaseHook {
    /**
     * Called immediately before opening the database.
     */
    void preKey(SQLiteDatabase database);
    /**
     * Called immediately after opening the database.
     */
    void postKey(SQLiteDatabase database);
}
