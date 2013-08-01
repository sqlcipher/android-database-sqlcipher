APP_PROJECT_PATH := $(shell pwd)
APP_ABI := armeabi x86
#APP_PLATFORM := android-8
NDK_MODULE_PATH=$(APP_PROJECT_PATH)
# fixes this error when building external/android-sqlite/android/sqlite3_android.cpp
#   icu4c/common/unicode/std_string.h:39:18: error: string: No such file or directory
APP_STL := stlport_shared

