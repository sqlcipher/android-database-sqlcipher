# If set to 24 then app is crashing on Android 6 with "java.lang.UnsatisfiedLinkError: dlopen failed: cannot locate symbol "__aeabi_memclr4" referenced by "/data/app/com.spotme.android-1/lib/arm/libsqlcipher.so"""
#See https://github.com/android-ndk/ndk/issues/126
APP_PLATFORM=android-23

APP_PROJECT_PATH := $(shell pwd)
APP_ABI := armeabi armeabi-v7a x86
APP_BUILD_SCRIPT := $(APP_PROJECT_PATH)/Android.mk
APP_STL := stlport_static
