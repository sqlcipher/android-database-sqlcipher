### Download Source and Binaries

The latest AAR binary package information can be [here](https://www.zetetic.net/sqlcipher/open-source), the source can be found [here](https://github.com/sqlcipher/android-database-sqlcipher).
<p><a title="Latest version from Maven Central" href="https://maven-badges.herokuapp.com/maven-central/net.zetetic/android-database-sqlcipher"><img src="https://maven-badges.herokuapp.com/maven-central/net.zetetic/android-database-sqlcipher/badge.svg"></a></p>

### Compatibility

SQLCipher for Android runs on Android from 5.0 (API 21), for `armeabi-v7a`, `x86`, `x86_64`, and `arm64_v8a` architectures.

### Contributions

We welcome contributions, to contribute to SQLCipher for Android, a [contributor agreement](https://www.zetetic.net/contributions/) needs to be submitted. All submissions should be based on the `master` branch.

### An Illustrative Terminal Listing

A typical SQLite database in unencrypted, and visually parseable even as encoded text. The following example shows the difference between hexdumps of a standard SQLite database and one implementing SQLCipher.

```
~ sjlombardo$ hexdump -C sqlite.db
00000000 53 51 4c 69 74 65 20 66 6f 72 6d 61 74 20 33 00 |SQLite format 3.|
…
000003c0 65 74 32 74 32 03 43 52 45 41 54 45 20 54 41 42 |et2t2.CREATE TAB|
000003d0 4c 45 20 74 32 28 61 2c 62 29 24 01 06 17 11 11 |LE t2(a,b)$…..|
…
000007e0 20 74 68 65 20 73 68 6f 77 15 01 03 01 2f 01 6f | the show…./.o|
000007f0 6e 65 20 66 6f 72 20 74 68 65 20 6d 6f 6e 65 79 |ne for the money|

~ $ sqlite3 sqlcipher.db
sqlite> PRAGMA KEY=’test123′;
sqlite> CREATE TABLE t1(a,b);
sqlite> INSERT INTO t1(a,b) VALUES (‘one for the money’, ‘two for the show’);
sqlite> .quit

~ $ hexdump -C sqlcipher.db
00000000 84 d1 36 18 eb b5 82 90 c4 70 0d ee 43 cb 61 87 |.?6.?..?p.?C?a.|
00000010 91 42 3c cd 55 24 ab c6 c4 1d c6 67 b4 e3 96 bb |.B?..?|
00000bf0 8e 99 ee 28 23 43 ab a4 97 cd 63 42 8a 8e 7c c6 |..?(#C??.?cB..|?|

~ $ sqlite3 sqlcipher.db
sqlite> SELECT * FROM t1;
Error: file is encrypted or is not a database
```
(example courtesy of SQLCipher)

### Application Integration

You have a two main options for using SQLCipher for Android in your app:

- Using it with Room or other consumers of the `androidx.sqlite` API

- Using the native SQLCipher for Android classes

In both cases, you will need to add a dependency on `net.zetetic:android-database-sqlcipher`,
such as having the following line in your module's `build.gradle` `dependencies`
closure:

```gradle
implementation "net.zetetic:android-database-sqlcipher:4.5.3"
implementation "androidx.sqlite:sqlite:2.1.0"
```

(replacing `4.5.3` with the version you want)

<a title="Latest version from Maven Central" href="https://maven-badges.herokuapp.com/maven-central/net.zetetic/android-database-sqlcipher"><img src="https://maven-badges.herokuapp.com/maven-central/net.zetetic/android-database-sqlcipher/badge.svg"></a>

#### Using SQLCipher for Android With Room

SQLCipher for Android has a `SupportFactory` class in the `net.sqlcipher.database` package
that can be used to configure Room to use SQLCipher for Android.

There are three `SupportFactory` constructors:

- `SupportFactory(byte[] passphrase)`
- `SupportFactory(byte[] passphrase, SQLiteDatabaseHook hook)`
- `SupportFactory(byte[] passphrase, SQLiteDatabaseHook hook, boolean clearPassphrase)`

All three take a `byte[]` to use as the passphrase (if you have a `char[]`, use
`SQLiteDatabase.getBytes()` to get a suitable `byte[]` to use).

Two offer a `SQLiteDatabaseHook` parameter that you can use
for executing SQL statements before or after the passphrase is used to key
the database.

The three-parameter constructor also offers `clearPassphrase`, which defaults
to `true` in the other two constructors. If `clearPassphrase` is set to `true`,
this will zero out the bytes of the `byte[]` after we open the database. This
is safest from a security standpoint, but it does mean that the `SupportFactory`
instance is a single-use object. Attempting to reuse the `SupportFactory`
instance later will result in being unable to open the database, because the
passphrase will be wrong. If you think that you might need to reuse the
`SupportFactory` instance, pass `false` for `clearPassphrase`.

Then, pass your `SupportFactory` to `openHelperFactory()` on your `RoomDatabase.Builder`:

```java
final byte[] passphrase = SQLiteDatabase.getBytes(userEnteredPassphrase);
final SupportFactory factory = new SupportFactory(passphrase);
final SomeDatabase room = Room.databaseBuilder(activity, SomeDatabase.class, DB_NAME)
  .openHelperFactory(factory)
  .build();
```

Now, Room will make all of its database requests using SQLCipher for Android instead
of the framework copy of SQLCipher.

Note that `SupportFactory` should work with other consumers of the `androidx.sqlite` API;
Room is merely a prominent example.

#### Using SQLCipher for Android's Native API

If you have existing SQLite code using classes like `SQLiteDatabase` and `SQLiteOpenHelper`,
converting your code to use SQLCipher for Android mostly is a three-step process:

1. Replace all `android.database.sqlite.*` `import` statements with ones that
use `net.sqlcipher.database.*` (e.g., convert `android.database.sqlite.SQLiteDatabase`
to `net.sqlcipher.database.SQLiteDatabase`)

2. Before attempting to open a database, call `SQLiteDatabase.loadLibs()`, passing
in a `Context` (e.g., add this to `onCreate()` of your `Application` subclass, using
the `Application` itself as the `Context`)

3. When opening a database (e.g., `SQLiteDatabase.openOrCreateDatabase()`), pass
in the passphrase as a `char[]` or `byte[]`

The rest of your code may not need any changes.

An article covering both integration of SQLCipher into an Android application as well as building the source can be found [here](https://www.zetetic.net/sqlcipher/sqlcipher-for-android/).

### ProGuard

For applications which utilize ProGuard, a few additional rules must be included when using SQLCipher for Android. These rules instruct ProGuard to omit the renaming of the internal SQLCipher classes which are used via lookup from the JNI layer. It is worth noting that since SQLCipher or Android is based on open source code there is little value in obfuscating the library anyway. The more important use of ProGuard is to protect your application code and business logic.

```
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }
```

### Building

In order to build `android-database-sqlcipher` from source you will need both the Android SDK, Gradle, Android NDK, SQLCipher core source directory, and an OpenSSL source directory. We currently recommend using Android NDK LTS version `23.0.7599858`.

To complete the `make` command, the `ANDROID_NDK_HOME` environment variable must be defined which should point to your NDK root. Once you have cloned the repo, change directory into the root of the repository and run the following commands:

```
SQLCIPHER_ROOT=/some/path/to/sqlcipher-folder \
OPENSSL_ROOT=/some/path/to/openssl-folder \
SQLCIPHER_CFLAGS="-DSQLITE_HAS_CODEC -DSQLITE_TEMP_STORE=2" \
SQLCIPHER_ANDROID_VERSION="4.5.3" \
make build-release
```

You may specify other build flags/features within `SQLCIPHER_CFLAGS`, however, specifying `-DSQLITE_HAS_CODEC` and `-DSQLITE_TEMP_STORE` is necessary in the list of flags.

### License

The Android support libraries are licensed under Apache 2.0, in line with the Android OS code on which they are based. The SQLCipher code itself is licensed under a BSD-style license from Zetetic LLC. Finally, the original SQLite code itself is in the public domain.
