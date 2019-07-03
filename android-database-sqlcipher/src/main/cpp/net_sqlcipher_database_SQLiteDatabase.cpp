/*
 * Copyright (C) 2006-2007 The Android Open Source Project
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

#undef LOG_TAG
#define LOG_TAG "Database"

#include <jni.h>
#include <sqlite3.h>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/ioctl.h>

#include "log.h"
#include "jni_elements.h"
#include "jni_exception.h"
#include "sqlite3_exception.h"
#include "sqlcipher_loading.h"

#define UTF16_STORAGE 0
#define INVALID_VERSION -1
#define SQLITE_SOFT_HEAP_LIMIT (4 * 1024 * 1024)
#define ANDROID_TABLE "android_metadata"
/* uncomment the next line to force-enable logging of all statements */
// #define DB_LOG_STATEMENTS

namespace sqlcipher {


  enum {
    OPEN_READWRITE          = 0x00000000,
    OPEN_READONLY           = 0x00000001,
    OPEN_READ_MASK          = 0x00000001,
    NO_LOCALIZED_COLLATORS  = 0x00000010,
    CREATE_IF_NECESSARY     = 0x10000000
  };

  static jfieldID offset_db_handle;

  static char *createStr(const char *path) {
    int len = strlen(path);
    char *str = (char *)malloc(len + 1);
    strncpy(str, path, len);
    str[len] = 0;
    return str;
  }

  static void sqlLogger(void *databaseName, int iErrCode, const char *zMsg) {
    // skip printing this message if it is due to certain types of errors
    if (iErrCode == SQLITE_CONSTRAINT) return;
    LOGI("sqlite returned: error code = %d, msg = %s\n", iErrCode, zMsg);
  }

  // register the logging func on sqlite. needs to be done BEFORE any sqlite3 func is called.
  static void registerLoggingFunc(const char *path) {
    static bool loggingFuncSet = false;
    if (loggingFuncSet) {
      return;
    }

    LOGV("Registering sqlite logging func \n");
    int err = sqlite3_config(SQLITE_CONFIG_LOG, &sqlLogger, (void *)createStr(path));
    if (err != SQLITE_OK) {
      LOGE("sqlite_config failed error_code = %d. THIS SHOULD NEVER occur.\n", err);
      return;
    }
    loggingFuncSet = true;
  }

  int native_status(JNIEnv* env, jobject object, jint operation, jboolean reset)
  {
    int value;
    int highWater;
    sqlite3 * handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);
    int status = sqlite3_status(operation, &value, &highWater, reset);
    if(status != SQLITE_OK){
      throw_sqlite3_exception(env, handle);
    }
    return value;
  }

  void native_key(JNIEnv* env, jobject object, jbyteArray jKey) {
    int rc = 0;
    int index = 0;
    jsize size = 0;
    jbyte *key = 0;
    sqlite3 *handle = NULL;
    handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);
    if(handle == NULL){
      LOGE("env->GetLongField returned NULL when retrieving sqlite3 *\n");
    }
    key = env->GetByteArrayElements(jKey, NULL);
    size = env->GetArrayLength(jKey);
    if(key == NULL || size == 0) goto done;
    rc = sqlite3_key(handle, key, size);
    if(rc != SQLITE_OK) {
      throw_sqlite3_exception(env, handle);
    }
  done:
    if(key) env->ReleaseByteArrayElements(jKey, key, JNI_ABORT);
  }

  void native_rekey(JNIEnv* env, jobject object, jbyteArray jKey) {
    int rc = 0;
    jsize size = 0;
    jbyte *key = 0;
    sqlite3 *handle = NULL;
    handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);
    key = env->GetByteArrayElements(jKey, NULL);
    size = env->GetArrayLength(jKey);
    if(key == NULL || size == 0) goto done;
    rc = sqlite3_rekey(handle, key, size);
    if(rc != SQLITE_OK) {
      throw_sqlite3_exception(env, handle);
    }
  done:
    if(key) env->ReleaseByteArrayElements(jKey, key, JNI_ABORT);
  }

  void native_key_mutf8(JNIEnv* env, jobject object, jcharArray jKey) {
    int rc;
    int idx;
    jint releaseElements = 0;
    jboolean arrayIsCopy;
    sqlite3 *handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);
    jsize sz = env->GetArrayLength(jKey);
    jchar* jKeyChar = env->GetCharArrayElements(jKey, &arrayIsCopy);
    jstring key = env->NewString(jKeyChar, sz);
    const char* password = env->GetStringUTFChars(key, JNI_FALSE);
    int password_sz = env->GetStringUTFLength(key);
    if(password_sz > 0){
      rc = sqlite3_key(handle, password, password_sz);
      if(rc != SQLITE_OK){
        throw_sqlite3_exception(env, handle);
      }
    }
    env->ReleaseCharArrayElements(jKey, jKeyChar, JNI_ABORT);
    env->ReleaseStringUTFChars(key, password);
  }

  void native_rawExecSQL(JNIEnv* env, jobject object, jstring sql)
  {
    sqlite3 * handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);
    char const * sqlCommand = env->GetStringUTFChars(sql, NULL);
    int status = sqlite3_exec(handle, sqlCommand, NULL, NULL, NULL);
    env->ReleaseStringUTFChars(sql, sqlCommand);
    if(status != SQLITE_OK){
      throw_sqlite3_exception(env, handle);
    }
  }

  /* public native void setICURoot(String path); */
  // void setICURoot(JNIEnv* env, jobject object, jstring ICURoot)
  // {
  //   char const * ICURootPath = env->GetStringUTFChars(ICURoot, NULL);
  //   setenv("SQLCIPHER_ICU_PREFIX", ICURootPath, 1);
  //   env->ReleaseStringUTFChars(ICURoot, ICURootPath);
  // }


  /* public native void dbopen(String path, int flags, String locale); */
  void dbopen(JNIEnv* env, jobject object, jstring pathString, jint flags)
  {
    int err;
    sqlite3 * handle = NULL;
    sqlite3_stmt * statement = NULL;
    char const * path8 = env->GetStringUTFChars(pathString, NULL);
    int sqliteFlags;

    // register the logging func on sqlite. needs to be done BEFORE any sqlite3 func is called.
    registerLoggingFunc(path8);

    // convert our flags into the sqlite flags
    if (flags & CREATE_IF_NECESSARY) {
      sqliteFlags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE;
    } else if (flags & OPEN_READONLY) {
      sqliteFlags = SQLITE_OPEN_READONLY;
    } else {
      sqliteFlags = SQLITE_OPEN_READWRITE;
    }

    err = sqlite3_open_v2(path8, &handle, sqliteFlags, NULL);
    if (err != SQLITE_OK) {
      LOGE("sqlite3_open_v2(\"%s\", &handle, %d, NULL) failed\n", path8, sqliteFlags);
      throw_sqlite3_exception_errcode(env, err, "Could not open database");
      goto done;
    }

    // Check that the database is really read/write when that is what we asked for.
    if ((sqliteFlags & SQLITE_OPEN_READWRITE) && sqlite3_db_readonly(handle, NULL)) {
      throw_sqlite3_exception(env, handle, "Could not open the database in read/write mode.");
      goto done;
    }

    // The soft heap limit prevents the page cache allocations from growing
    // beyond the given limit, no matter what the max page cache sizes are
    // set to. The limit does not, as of 3.5.0, affect any other allocations.
    sqlite3_soft_heap_limit(SQLITE_SOFT_HEAP_LIMIT);

    // Set the default busy handler to retry for 1000ms and then return SQLITE_BUSY
    err = sqlite3_busy_timeout(handle, 1000 /* ms */);
    if (err != SQLITE_OK) {
      LOGE("sqlite3_busy_timeout(handle, 1000) failed for \"%s\"\n", path8);
      throw_sqlite3_exception(env, handle, "Could not set busy timeout");
      goto done;
    }

#ifdef DB_INTEGRITY_CHECK
    static const char* integritySql = "pragma integrity_check(1);";
    err = sqlite3_prepare_v2(handle, integritySql, -1, &statement, NULL);
    if (err != SQLITE_OK) {
      LOGE("sqlite_prepare_v2(handle, \"%s\") failed for \"%s\"\n", integritySql, path8);
      throw_sqlite3_exception(env, handle, "sqlite_prepare_v2(handle, \"pragma integrity_check(1);\") failed");
      goto done;
    }

    // first is OK or error message
    err = sqlite3_step(statement);
    if (err != SQLITE_ROW) {
      LOGE("integrity check failed for \"%s\"\n", integritySql, path8);
      throw_sqlite3_exception(env, handle);
      goto done;
    } else {
      const char *text = (const char*)sqlite3_column_text(statement, 0);
      if (strcmp(text, "ok") != 0) {
        LOGE("integrity check failed for \"%s\": %s\n", integritySql, path8, text);
        jniThrowException(env, "net/sqlcipher/database/SQLiteDatabaseCorruptException", text);
        goto done;
      }
    }
#endif

    sqlite3_enable_load_extension(handle, 1);

    LOGV("Opened '%s' - %p\n", path8, handle);
    env->SetLongField(object, offset_db_handle, (intptr_t)handle);
    handle = NULL;  // The caller owns the handle now.

  done:
    // Release allocated resources
    if (path8 != NULL) env->ReleaseStringUTFChars(pathString, path8);
    if (statement != NULL) sqlite3_finalize(statement);
    if (handle != NULL) sqlite3_close(handle);
  }

  static char *getDatabaseName(JNIEnv* env, sqlite3 * handle, jstring databaseName) {
    char const *path = env->GetStringUTFChars(databaseName, NULL);
    if (path == NULL) {
      LOGE("Failure in getDatabaseName(). VM ran out of memory?\n");
      return NULL; // VM would have thrown OutOfMemoryError
    }
    char *dbNameStr = createStr(path);
    env->ReleaseStringUTFChars(databaseName, path);
    return dbNameStr;
  }

  static void sqlTrace(void *databaseName, const char *sql) {
    LOGI("sql_statement|%s|%s\n", (char *)databaseName, sql);
  }

  /* public native void enableSqlTracing(); */
  static void enableSqlTracing(JNIEnv* env, jobject object, jstring databaseName)
  {
    sqlite3 * handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);
    sqlite3_trace(handle, &sqlTrace, (void *)getDatabaseName(env, handle, databaseName));
  }

  static void sqlProfile(void *databaseName, const char *sql, sqlite3_uint64 tm) {
    double d = tm/1000000.0;
    LOGI("elapsedTime4Sql|%s|%.3f ms|%s\n", (char *)databaseName, d, sql);
  }

  /* public native void enableSqlProfiling(); */
  static void enableSqlProfiling(JNIEnv* env, jobject object, jstring databaseName)
  {
    sqlite3 * handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);
    sqlite3_profile(handle, &sqlProfile, (void *)getDatabaseName(env, handle, databaseName));
  }


  /* public native void close(); */
  static void dbclose(JNIEnv* env, jobject object)
  {
    sqlite3 * handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);

    if (handle != NULL) {
      // release the memory associated with the traceFuncArg in enableSqlTracing function
      void *traceFuncArg = sqlite3_trace(handle, &sqlTrace, NULL);
      if (traceFuncArg != NULL) {
        free(traceFuncArg);
      }
      // release the memory associated with the traceFuncArg in enableSqlProfiling function
      traceFuncArg = sqlite3_profile(handle, &sqlProfile, NULL);
      if (traceFuncArg != NULL) {
        free(traceFuncArg);
      }
      LOGV("Closing database: handle=%p\n", handle);
      int result = sqlite3_close(handle);
      if (result == SQLITE_OK) {
        LOGV("Closed %p\n", handle);
        env->SetLongField(object, offset_db_handle, 0);
      } else {
        // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
        LOGE("sqlite3_close(%p) failed: %d\n", handle, result);
        throw_sqlite3_exception(env, handle, "sqlite3_close() failed");
      }
    }
  }

  /* public native void native_execSQL(String sql); */
  static void native_execSQL(JNIEnv* env, jobject object, jstring sqlString)
  {
    int err;
    int stepErr;
    sqlite3_stmt * statement = NULL;
    sqlite3 * handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);
    jchar const * sql = env->GetStringChars(sqlString, NULL);
    jsize sqlLen = env->GetStringLength(sqlString);

    if (sql == NULL || sqlLen == 0) {
      jniThrowException(env, "java/lang/IllegalArgumentException", "You must supply an SQL string");
      return;
    }

    err = sqlite3_prepare16_v2(handle, sql, sqlLen * 2, &statement, NULL);

    env->ReleaseStringChars(sqlString, sql);

    if (err != SQLITE_OK) {
      char const * sql8 = env->GetStringUTFChars(sqlString, NULL);
      LOGE("Failure %d (%s) on %p when preparing '%s'.\n", err, sqlite3_errmsg(handle), handle, sql8);
      throw_sqlite3_exception(env, handle, sql8);
      env->ReleaseStringUTFChars(sqlString, sql8);
      return;
    }

    stepErr = sqlite3_step(statement);
    err = sqlite3_finalize(statement);

    if (stepErr != SQLITE_DONE) {
      if (stepErr == SQLITE_ROW) {
        throw_sqlite3_exception(env, "Queries cannot be performed using execSQL(), use query() instead.");
      } else {
        char const * sql8 = env->GetStringUTFChars(sqlString, NULL);
        LOGE("Failure %d (%s) on %p when executing '%s'\n", err, sqlite3_errmsg(handle), handle, sql8);
        throw_sqlite3_exception(env, handle, sql8);
        env->ReleaseStringUTFChars(sqlString, sql8);

      }
    } else
#ifndef DB_LOG_STATEMENTS
      //    IF_LOGV()
#endif
      {
        char const * sql8 = env->GetStringUTFChars(sqlString, NULL);
        LOGV("Success on %p when executing '%s'\n", handle, sql8);
        env->ReleaseStringUTFChars(sqlString, sql8);
      }
  }

  /* native long lastInsertRow(); */
  static jlong lastInsertRow(JNIEnv* env, jobject object)
  {
    sqlite3 * handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);

    return sqlite3_last_insert_rowid(handle);
  }

  /* native int lastChangeCount(); */
  static jint lastChangeCount(JNIEnv* env, jobject object)
  {
    sqlite3 * handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);

    return sqlite3_changes(handle);
  }

  /* native int native_getDbLookaside(); */
  static jint native_getDbLookaside(JNIEnv* env, jobject object)
  {
    sqlite3 * handle = (sqlite3 *)env->GetLongField(object, offset_db_handle);
    int pCur = -1;
    int unused;
    sqlite3_db_status(handle, SQLITE_DBSTATUS_LOOKASIDE_USED, &pCur, &unused, 0);
    return pCur;
  }

  /* set locale in the android_metadata table, install localized collators, and rebuild indexes */
  // static void native_setLocale(JNIEnv* env, jobject object, jstring localeString, jint flags)
  // {
  //     if ((flags & NO_LOCALIZED_COLLATORS)) return;

  //     int err;
  //     char const* locale8 = env->GetStringUTFChars(localeString, NULL);
  //     sqlite3 * handle = (sqlite3 *)env->GetIntField(object, offset_db_handle);
  //     sqlite3_stmt* stmt = NULL;
  //     char** meta = NULL;
  //     int rowCount, colCount;
  //     char* dbLocale = NULL;

  //     // create the table, if necessary and possible
  //     if (!(flags & OPEN_READONLY)) {
  //         static const char *createSql ="CREATE TABLE IF NOT EXISTS " ANDROID_TABLE " (locale TEXT)";
  //         err = sqlite3_exec(handle, createSql, NULL, NULL, NULL);
  //         if (err != SQLITE_OK) {
  //             LOGE("CREATE TABLE " ANDROID_TABLE " failed\n");
  //             throw_sqlite3_exception(env, handle, "create locale table failed");
  //             goto done;
  //         }
  //     }

  //     // try to read from the table
  //     static const char *selectSql = "SELECT locale FROM " ANDROID_TABLE " LIMIT 1";
  //     err = sqlite3_get_table(handle, selectSql, &meta, &rowCount, &colCount, NULL);
  //     if (err != SQLITE_OK) {
  //         LOGE("SELECT locale FROM " ANDROID_TABLE " failed\n");
  //         throw_sqlite3_exception(env, handle, "select locale failed");
  //         goto done;
  //     }

  //     dbLocale = (rowCount >= 1) ? meta[colCount] : NULL;

  //     if (dbLocale != NULL && !strcmp(dbLocale, locale8)) {
  //         // database locale is the same as the desired locale; set up the collators and go
  //         err = register_localized_collators(handle, locale8, UTF16_STORAGE);
  //         if (err != SQLITE_OK) throw_sqlite3_exception(env, handle);
  //         goto done;   // no database changes needed
  //     }

  //     if ((flags & OPEN_READONLY)) {
  //         // read-only database, so we're going to have to put up with whatever we got
  //         // For registering new index. Not for modifing the read-only database.
  //         err = register_localized_collators(handle, locale8, UTF16_STORAGE);
  //         if (err != SQLITE_OK) throw_sqlite3_exception(env, handle, "register localized collators failed");
  //         goto done;
  //     }

  //     // need to update android_metadata and indexes atomically, so use a transaction...
  //     err = sqlite3_exec(handle, "BEGIN TRANSACTION", NULL, NULL, NULL);
  //     if (err != SQLITE_OK) {
  //         LOGE("BEGIN TRANSACTION failed setting locale\n");
  //         throw_sqlite3_exception(env, handle, "BEGIN TRANSACTION failed setting locale");
  //         goto done;
  //     }

  //     err = register_localized_collators(handle, locale8, UTF16_STORAGE);
  //     if (err != SQLITE_OK) {
  //         LOGE("register_localized_collators() failed setting locale\n");
  //         throw_sqlite3_exception(env, handle, "register_localized_collators() failed setting locale");
  //         goto rollback;
  //     }

  //     err = sqlite3_exec(handle, "DELETE FROM " ANDROID_TABLE, NULL, NULL, NULL);
  //     if (err != SQLITE_OK) {
  //         LOGE("DELETE failed setting locale\n");
  //         throw_sqlite3_exception(env, handle, "DELETE failed setting locale");
  //         goto rollback;
  //     }

  //     static const char *sql = "INSERT INTO " ANDROID_TABLE " (locale) VALUES(?);";
  //     err = sqlite3_prepare_v2(handle, sql, -1, &stmt, NULL);
  //     if (err != SQLITE_OK) {
  //         LOGE("sqlite3_prepare_v2(\"%s\") failed\n", sql);
  //         throw_sqlite3_exception(env, handle, "sqlite3_prepare_v2() failed setting locale");
  //         goto rollback;
  //     }

  //     err = sqlite3_bind_text(stmt, 1, locale8, -1, SQLITE_TRANSIENT);
  //     if (err != SQLITE_OK) {
  //         LOGE("sqlite3_bind_text() failed setting locale\n");
  //         throw_sqlite3_exception(env, handle, "sqlite3_bind_text() failed setting locale");
  //         goto rollback;
  //     }

  //     err = sqlite3_step(stmt);
  //     if (err != SQLITE_OK && err != SQLITE_DONE) {
  //         LOGE("sqlite3_step(\"%s\") failed setting locale\n", sql);
  //         throw_sqlite3_exception(env, handle, "sqlite3_step() failed setting locale");
  //         goto rollback;
  //     }

  //     err = sqlite3_exec(handle, "REINDEX LOCALIZED", NULL, NULL, NULL);
  //     if (err != SQLITE_OK) {
  //         LOGE("REINDEX LOCALIZED failed\n");
  //         throw_sqlite3_exception(env, handle, "REINDEX LOCALIZED failed");
  //         goto rollback;
  //     }

  //     // all done, yay!
  //     err = sqlite3_exec(handle, "COMMIT TRANSACTION", NULL, NULL, NULL);
  //     if (err != SQLITE_OK) {
  //         LOGE("COMMIT TRANSACTION failed setting locale\n");
  //         throw_sqlite3_exception(env, handle, "COMMIT TRANSACTION failed setting locale");
  //         goto done;
  //     }

  // rollback:
  //     if (err != SQLITE_OK) {
  //         sqlite3_exec(handle, "ROLLBACK TRANSACTION", NULL, NULL, NULL);
  //     }

  // done:
  //     if (locale8 != NULL) env->ReleaseStringUTFChars(localeString, locale8);
  //     if (stmt != NULL) sqlite3_finalize(stmt);
  //     if (meta != NULL) sqlite3_free_table(meta);
  // }

  static jint native_releaseMemory(JNIEnv *env, jobject clazz)
  {
    // Attempt to release as much memory from the
    return sqlite3_release_memory(SQLITE_SOFT_HEAP_LIMIT);
  }

  static JNINativeMethod sMethods[] =
    {
      /* name, signature, funcPtr */
      {"dbopen", "(Ljava/lang/String;I)V", (void *)dbopen},
      {"dbclose", "()V", (void *)dbclose},
      {"enableSqlTracing", "(Ljava/lang/String;)V", (void *)enableSqlTracing},
      {"enableSqlProfiling", "(Ljava/lang/String;)V", (void *)enableSqlProfiling},
      {"native_execSQL", "(Ljava/lang/String;)V", (void *)native_execSQL},
      {"lastInsertRow", "()J", (void *)lastInsertRow},
      {"lastChangeCount", "()I", (void *)lastChangeCount},
      {"native_getDbLookaside", "()I", (void *)native_getDbLookaside},
      {"releaseMemory", "()I", (void *)native_releaseMemory},
      {"native_rawExecSQL", "(Ljava/lang/String;)V", (void *)native_rawExecSQL},
      {"native_status", "(IZ)I", (void *)native_status},
      {"key_mutf8", "([C)V", (void *)native_key_mutf8},
      {"key", "([B)V", (void *)native_key},
      {"rekey", "([B)V", (void *)native_rekey},
    };

  int register_android_database_SQLiteDatabase(JNIEnv *env)
  {
    jclass clazz;

    clazz = env->FindClass("net/sqlcipher/database/SQLiteDatabase");
    if (clazz == NULL) {
      LOGE("Can't find net/sqlcipher/database/SQLiteDatabase\n");
      return -1;
    }

    offset_db_handle = env->GetFieldID(clazz, "mNativeHandle", "J");
    if (offset_db_handle == NULL) {
      LOGE("Can't find SQLiteDatabase.mNativeHandle\n");
      return -1;
    }
    return env->RegisterNatives(clazz, sMethods, NELEM(sMethods));
  }

  //this code is not executed
  extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved)
  {
    JNIEnv *env;
    //gJavaVM = vm;
    LOGI("JNI_OnLoad called");
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_2) != JNI_OK) {
      LOGE("Failed to get the environment using GetEnv()");
      return -1;
    }

    LOGI("JNI_OnLoad register methods ");

    register_android_database_SQLiteDatabase(env);
    register_android_database_SQLiteCompiledSql(env);
    register_android_database_SQLiteQuery(env);
    register_android_database_SQLiteProgram(env);
    register_android_database_SQLiteStatement(env);
    register_android_database_CursorWindow(env);

    //register_android_database_SQLiteDebug(env);

    return JNI_VERSION_1_2;

  }

  /* throw a SQLiteException with a message appropriate for the error in handle */
  void throw_sqlite3_exception(JNIEnv* env, sqlite3* handle) {
    throw_sqlite3_exception(env, handle, NULL);
  }

  /* throw a SQLiteException with the given message */
  void throw_sqlite3_exception(JNIEnv* env, const char* message) {
    throw_sqlite3_exception(env, NULL, message);
  }

  /* throw a SQLiteException with a message appropriate for the error in handle
     concatenated with the given message
  */
  void throw_sqlite3_exception(JNIEnv* env, sqlite3* handle, const char* message) {
    if (handle && sqlite3_errcode(handle) != SQLITE_OK) {
      throw_sqlite3_exception(env, sqlite3_errcode(handle),
                              sqlite3_errmsg(handle), message);
    } else {
      // we use SQLITE_OK so that a generic SQLiteException is thrown;
      // any code not specified in the switch statement below would do.
      throw_sqlite3_exception(env, SQLITE_OK, "unknown error", message);
    }
  }

  /* throw a SQLiteException for a given error code */
  void throw_sqlite3_exception_errcode(JNIEnv* env, int errcode, const char* message) {
    if (errcode == SQLITE_DONE) {
      throw_sqlite3_exception(env, errcode, NULL, message);
    } else {
      char temp[21];
      sprintf(temp, "error code %d", errcode);
      throw_sqlite3_exception(env, errcode, temp, message);
    }
  }

  /* throw a SQLiteException for a given error code, sqlite3message, and
     user message
  */
  void throw_sqlite3_exception(JNIEnv* env, int errcode,
                               const char* sqlite3Message, const char* message) {
    const char* exceptionClass;
    switch (errcode) {
    case SQLITE_IOERR:
      exceptionClass = "net/sqlcipher/database/SQLiteDiskIOException";
      break;
    case SQLITE_CORRUPT:
      exceptionClass = "net/sqlcipher/database/SQLiteDatabaseCorruptException";
      break;
    case SQLITE_CONSTRAINT:
      exceptionClass = "net/sqlcipher/database/SQLiteConstraintException";
      break;
    case SQLITE_ABORT:
      exceptionClass = "net/sqlcipher/database/SQLiteAbortException";
      break;
    case SQLITE_DONE:
      exceptionClass = "net/sqlcipher/database/SQLiteDoneException";
      break;
    case SQLITE_FULL:
      exceptionClass = "net/sqlcipher/database/SQLiteFullException";
      break;
    case SQLITE_MISUSE:
      exceptionClass = "net/sqlcipher/database/SQLiteMisuseException";
      break;
    default:
      exceptionClass = "net/sqlcipher/database/SQLiteException";
      break;
    }

    if (sqlite3Message != NULL && message != NULL) {
      char* fullMessage = (char *)malloc(strlen(sqlite3Message) + strlen(message) + 3);
      if (fullMessage != NULL) {
        strcpy(fullMessage, sqlite3Message);
        strcat(fullMessage, ": ");
        strcat(fullMessage, message);
        jniThrowException(env, exceptionClass, fullMessage);
        free(fullMessage);
      } else {
        jniThrowException(env, exceptionClass, sqlite3Message);
      }
    } else if (sqlite3Message != NULL) {
      jniThrowException(env, exceptionClass, sqlite3Message);
    } else {
      jniThrowException(env, exceptionClass, message);
    }
  }


} // namespace sqlcipher
