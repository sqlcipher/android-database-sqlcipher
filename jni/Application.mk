APP_PROJECT_PATH := $(shell pwd)
APP_ABI := armeabi x86
APP_BUILD_SCRIPT := $(APP_PROJECT_PATH)/Android.mk
APP_STL := gabi++_shared
APP_CPPFLAGS += -frtti
