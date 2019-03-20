APP_PROJECT_PATH := $(shell pwd)
APP_ABI := armeabi armeabi-v7a x86
APP_PLATFORM := android-$(NDK_APP_PLATFORM)
APP_BUILD_SCRIPT := $(APP_PROJECT_PATH)/Android.mk
APP_STL := stlport_static
APP_CFLAGS := -D_FILE_OFFSET_BITS=32
APP_LDFLAGS += -Wl,--exclude-libs,ALL
