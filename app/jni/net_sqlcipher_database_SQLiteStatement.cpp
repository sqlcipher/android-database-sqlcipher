/* //device/libs/android_runtime/android_database_SQLiteCursor.cpp
**
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#undef LOG_TAG
#define LOG_TAG "Cursor"

#include <jni.h>
#include <sqlite3.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include "log.h"
#include "jni_elements.h"
#include "sqlite3_exception.h"

namespace sqlcipher {


sqlite3_stmt * compile(JNIEnv* env, jobject object,
                       sqlite3 * handle, jstring sqlString);

static jfieldID gHandleField;
static jfieldID gStatementField;


#define GET_STATEMENT(env, object) \
        (sqlite3_stmt *)env->GetLongField(object, gStatementField)
#define GET_HANDLE(env, object) \
        (sqlite3 *)env->GetLongField(object, gHandleField)


static void native_execute(JNIEnv* env, jobject object)
{
    int err;
    sqlite3 * handle = GET_HANDLE(env, object);
    sqlite3_stmt * statement = GET_STATEMENT(env, object);

    // Execute the statement
    err = sqlite3_step(statement);

    // Throw an exception if an error occured
    if (err != SQLITE_DONE) {
        throw_sqlite3_exception_errcode(env, err, sqlite3_errmsg(handle));
    }

    // Reset the statment so it's ready to use again
    sqlite3_reset(statement);
}

static jlong native_1x1_long(JNIEnv* env, jobject object)
{
    int err;
    sqlite3 * handle = GET_HANDLE(env, object);
    sqlite3_stmt * statement = GET_STATEMENT(env, object);
    jlong value = -1;

    // Execute the statement
    err = sqlite3_step(statement);

    // Handle the result
    if (err == SQLITE_ROW) {
        // No errors, read the data and return it
        value = sqlite3_column_int64(statement, 0);
    } else {
        throw_sqlite3_exception_errcode(env, err, sqlite3_errmsg(handle));
    }

    // Reset the statment so it's ready to use again
    sqlite3_reset(statement);

    return value;
}

static jstring native_1x1_string(JNIEnv* env, jobject object)
{
    int err;
    sqlite3 * handle = GET_HANDLE(env, object);
    sqlite3_stmt * statement = GET_STATEMENT(env, object);
    jstring value = NULL;

    // Execute the statement
    err = sqlite3_step(statement);

    // Handle the result
    if (err == SQLITE_ROW) {
        // No errors, read the data and return it
        //char const * text = (char const *)sqlite3_column_text(statement, 0);

       const jchar *str = 0;
       jint strlength = 0;
       str = (const jchar*) sqlite3_column_text16(statement, 0);
       strlength = sqlite3_column_bytes16(statement, 0) / sizeof(jchar);
       value = str ? env->NewString(str, strlength) : NULL;
    } else {
        throw_sqlite3_exception_errcode(env, err, sqlite3_errmsg(handle));
    }

    // Reset the statment so it's ready to use again
    sqlite3_reset(statement);

    return value;
}


static JNINativeMethod sMethods[] =
{
     /* name, signature, funcPtr */
    {"native_execute", "()V", (void *)native_execute},
    {"native_1x1_long", "()J", (void *)native_1x1_long},
    {"native_1x1_string", "()Ljava/lang/String;", (void *)native_1x1_string},
};


int register_android_database_SQLiteStatement(JNIEnv * env)
{
    jclass clazz;

    clazz = env->FindClass("net/sqlcipher/database/SQLiteStatement");
    if (clazz == NULL) {
        LOGE("Can't find net/sqlcipher/database/SQLiteStatement");
        return -1;
    }

    gHandleField = env->GetFieldID(clazz, "nHandle", "J");
    gStatementField = env->GetFieldID(clazz, "nStatement", "J");

    if (gHandleField == NULL || gStatementField == NULL) {
        LOGE("Error locating fields");
        return -1;
    }
    return env->RegisterNatives(clazz, sMethods, NELEM(sMethods));
}


} // namespace sqlcipher
