/*
 * Copyright (C) 2019 Mark L. Murphy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sqlcipher.database;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

public class SupportHelper implements SupportSQLiteOpenHelper {
    private SQLiteOpenHelper standardHelper;
    private byte[] passphrase;

    SupportHelper(final SupportSQLiteOpenHelper.Configuration configuration,
                  byte[] passphrase, final SQLiteDatabaseHook hook) {
        SQLiteDatabase.loadLibs(configuration.context);
        this.passphrase = passphrase;

        standardHelper =
            new SQLiteOpenHelper(configuration.context, configuration.name,
                null, configuration.callback.version, hook) {
                @Override
                public void onCreate(SQLiteDatabase db) {
                    configuration.callback.onCreate(db);
                }

                @Override
                public void onUpgrade(SQLiteDatabase db, int oldVersion,
                                      int newVersion) {
                    configuration.callback.onUpgrade(db, oldVersion,
                        newVersion);
                }

                @Override
                public void onDowngrade(SQLiteDatabase db, int oldVersion,
                                        int newVersion) {
                    configuration.callback.onDowngrade(db, oldVersion,
                        newVersion);
                }

                @Override
                public void onOpen(SQLiteDatabase db) {
                    configuration.callback.onOpen(db);
                }

                @Override
                public void onConfigure(SQLiteDatabase db) {
                    configuration.callback.onConfigure(db);
                }
            };
    }

    @Override
    public String getDatabaseName() {
        return standardHelper.getDatabaseName();
    }

    @Override
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        standardHelper.setWriteAheadLoggingEnabled(enabled);
    }

    @Override
    public SupportSQLiteDatabase getWritableDatabase() {
        SQLiteDatabase result = standardHelper.getWritableDatabase(passphrase);
        if(passphrase != null) {
            for (int i = 0; i < passphrase.length; i++) {
              passphrase[i] = (byte)0;
            }
        }
        return result;
    }

    @Override
    public SupportSQLiteDatabase getReadableDatabase() {
        return getWritableDatabase();
    }

    @Override
    public void close() {
        standardHelper.close();
    }
}
