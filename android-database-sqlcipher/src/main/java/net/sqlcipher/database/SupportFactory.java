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

import androidx.sqlite.db.SupportSQLiteOpenHelper;

public class SupportFactory implements SupportSQLiteOpenHelper.Factory {
    private final byte[] passphrase;
    private final SQLiteDatabaseHook hook;

    public SupportFactory(byte[] passphrase) {
        this(passphrase, (SQLiteDatabaseHook)null);
    }

    public SupportFactory(byte[] passphrase, SQLiteDatabaseHook hook) {
        this.passphrase = passphrase;
        this.hook = hook;
    }

    @Override
    public SupportSQLiteOpenHelper create(SupportSQLiteOpenHelper.Configuration configuration) {
        return new SupportHelper(configuration, passphrase, hook);
    }
}
