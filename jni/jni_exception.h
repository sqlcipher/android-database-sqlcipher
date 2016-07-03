#include <jni.h>

#ifndef _JNI_EXCEPTION_H
#define _JNI_EXCEPTION_H
void jniThrowException(JNIEnv* env, const char* exceptionClass, const char* sqlite3Message);
#endif
