#include <cstddef>
#include "jni_exception.h"

void jniThrowException(JNIEnv* env, const char* exceptionClass, const char* sqlite3Message) {
  jclass exClass;
  exClass = env->FindClass(exceptionClass);
  env->ThrowNew(exClass, sqlite3Message);
}
