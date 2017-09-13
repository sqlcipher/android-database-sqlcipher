/* //device/libs/include/android_runtime/sqlite3_exception.h
**
** Copyright 2007, The Android Open Source Project
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


#include <jni.h>
/* #include <JNIHelp.h> */
/* #include <android_runtime/AndroidRuntime.h> */

#include <sqlite3.h>

namespace sqlcipher {

int register_android_database_SQLiteDatabase(JNIEnv *env);

int register_android_database_SQLiteCompiledSql(JNIEnv * env);

int register_android_database_SQLiteQuery(JNIEnv * env);

int register_android_database_SQLiteProgram(JNIEnv * env);

int register_android_database_SQLiteStatement(JNIEnv * env);

int register_android_database_SQLiteDebug(JNIEnv *env);

int register_android_database_CursorWindow(JNIEnv *env);

}

