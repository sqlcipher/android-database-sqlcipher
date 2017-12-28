APP_PROJECT_PATH := $(shell pwd)
APP_ABI := armeabi armeabi-v7a x86
APP_PLATFORM := android-21
APP_BUILD_SCRIPT := $(APP_PROJECT_PATH)/Android.mk
APP_STL := stlport_static
APP_CFLAGS := -D_FILE_OFFSET_BITS=32
