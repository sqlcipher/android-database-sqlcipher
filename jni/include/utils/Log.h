/* 
 * The internal Android code uses LOGV, LOGD, LOGI, LOGW, LOGW, etc for
 * logging debug stuff. These are not exposed externally, so we need to define
 * them ourselves.  This is even how its done in the Android NDK sample apps.
 * hans@eds.org
 */
#ifndef LOG_HACK_H
#define LOG_HACK_H

# include <android/log.h> 
# define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__) 
# define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  ,LOG_TAG,__VA_ARGS__) 
# define LOGI(...) __android_log_print(ANDROID_LOG_INFO   ,LOG_TAG,__VA_ARGS__) 
# define LOGW(...) __android_log_print(ANDROID_LOG_WARN   ,LOG_TAG,__VA_ARGS__) 
# define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  ,LOG_TAG,__VA_ARGS__) 

#endif /* LOG_HACK_H */
