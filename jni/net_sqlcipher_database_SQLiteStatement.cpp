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
#include <JNIHelp.h>
#include <android_runtime/AndroidRuntime.h>

#include <sqlite3.h>

#include <utils/Log.h>

#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include "unicode/uchar.h"
#include "unicode/ustring.h"

#include "sqlite3_exception.h"

#define UNICODE_SUBSTITUTION_CHAR 0xFFFD

namespace sqlcipher {


sqlite3_stmt * compile(JNIEnv* env, jobject object,
                       sqlite3 * handle, jstring sqlString);

static jfieldID gHandleField;
static jfieldID gStatementField;

// the android os version
static jint gAndroidApiVersionCode = -1;

#define GET_STATEMENT(env, object) \
        (sqlite3_stmt *)env->GetIntField(object, gStatementField)
#define GET_HANDLE(env, object) \
        (sqlite3 *)env->GetIntField(object, gHandleField)

// return the android os version from the env
jint getAndroidApiVersionCode(JNIEnv * env)
{
    if (gAndroidApiVersionCode < 0) {
        gAndroidApiVersionCode = 0;

        jclass versionClass = env->FindClass("android/os/Build$VERSION");
        if (NULL != versionClass) {
            jfieldID sdkIntFieldID = NULL;
            if (NULL != (sdkIntFieldID = env->GetStaticFieldID(versionClass, "SDK_INT", "I"))) {
                gAndroidApiVersionCode = env->GetStaticIntField(versionClass, sdkIntFieldID);
            }
        }
        LOGD("gAndroidApiVersionCode = %d", gAndroidApiVersionCode);
    }

    return gAndroidApiVersionCode;
}

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
        char const * text = (char const *)sqlite3_column_text(statement, 0);

        // this is a work around for https://code.google.com/p/android/issues/detail?id=81341
        // the bug has been fixed in api 23 (marshmellow)
        if (getAndroidApiVersionCode(env) >= 23) {
            value = env->NewStringUTF(text);
        } else {
            // clean it up by converting to utf16 before creating java string
            UErrorCode errorCode = U_ZERO_ERROR;
            int32_t slen = 0;
            // Find the length of the input in UTF-16 UChars, (by preflighting the conversion)
            u_strFromUTF8(NULL, 0, &slen, text, -1, &errorCode);
            if (U_BUFFER_OVERFLOW_ERROR == errorCode || U_SUCCESS(errorCode)) {
                  if (slen > 0) {
                    UChar * utf16Text = new UChar[slen + 1];
                    int32_t utf16TextLength;
                    errorCode = U_ZERO_ERROR;
                    u_strFromUTF8(utf16Text, slen + 1, &utf16TextLength, text, -1, &errorCode);
                    value = env->NewString(utf16Text, utf16TextLength);
                    delete utf16Text;
                } else {
                    value = env->NewStringUTF("");
                }
            } else {
                // some error, return a signal value
                UChar utf16Text = UNICODE_SUBSTITUTION_CHAR;
                value = env->NewString(&utf16Text, 1);
                LOGE("invalid utf text found in database %d", (int)errorCode);
            }
        }
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

    gHandleField = env->GetFieldID(clazz, "nHandle", "I");
    gStatementField = env->GetFieldID(clazz, "nStatement", "I");

    if (gHandleField == NULL || gStatementField == NULL) {
        LOGE("Error locating fields");
        return -1;
    }

    return android::AndroidRuntime::registerNativeMethods(env,
        "net/sqlcipher/database/SQLiteStatement", sMethods, NELEM(sMethods));
}

} // namespace sqlcipher
