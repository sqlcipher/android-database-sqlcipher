APP_PROJECT_PATH := $(shell pwd)
APP_ABI := armeabi armeabi-v7a x86 x86_64 arm64-v8a
APP_PLATFORM := android-$(NDK_APP_PLATFORM)
APP_BUILD_SCRIPT := $(APP_PROJECT_PATH)/Android.mk
APP_STL := stlport_static
ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI),armeabi armeabi-v7a x86))
	APP_CFLAGS := -D_FILE_OFFSET_BITS=32
else
	APP_CFLAGS := -D_FILE_OFFSET_BITS=64
endif
APP_LDFLAGS += -Wl,--exclude-libs,ALL
