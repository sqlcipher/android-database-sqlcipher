APP_PROJECT_PATH := $(shell pwd)
APP_ABI := x86_64 arm64-v8a
APP_PLATFORM := android-$(NDK_APP_PLATFORM)
APP_BUILD_SCRIPT := $(APP_PROJECT_PATH)/Android.mk
APP_STL := stlport_static
APP_CFLAGS := -D_FILE_OFFSET_BITS=64
APP_LDFLAGS += -Wl,--exclude-libs,ALL
