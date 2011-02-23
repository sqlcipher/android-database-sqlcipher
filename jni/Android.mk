LOCAL_PATH:= $(call my-dir)

EXTERNAL_PATH := ../external
LOCAL_CFLAGS += -U__APPLE__

ifeq ($(TARGET_ARCH), arm)
	LOCAL_CFLAGS += -DPACKED="__attribute__ ((packed))"
else
	LOCAL_CFLAGS += -DPACKED=""
endif

ifeq ($(WITH_JIT),true)
	LOCAL_CFLAGS += -DWITH_JIT
endif

ifneq ($(USE_CUSTOM_RUNTIME_HEAP_MAX),)
  LOCAL_CFLAGS += -DCUSTOM_RUNTIME_HEAP_MAX=$(USE_CUSTOM_RUNTIME_HEAP_MAX)
endif

APP_STL := stlport_static

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	info_guardianproject_database_sqlcipher_SQLiteCompiledSql.cpp \
	info_guardianproject_database_sqlcipher_SQLiteDatabase.cpp \
	info_guardianproject_database_sqlcipher_SQLiteProgram.cpp \
	info_guardianproject_database_sqlcipher_SQLiteQuery.cpp \
	info_guardianproject_database_sqlcipher_SQLiteStatement.cpp \

LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
	$(LOCAL_PATH)/include \
	$(EXTERNAL_PATH)/sqlcipher \
	$(EXTERNAL_PATH)/dalvik/libnativehelper/include \
	$(EXTERNAL_PATH)/dalvik/libnativehelper/include/nativehelper \
	$(EXTERNAL_PATH)/openssl/include \
	$(EXTERNAL_PATH)/platform-frameworks-base/include \
	$(EXTERNAL_PATH)/platform-frameworks-base/core/jni \
	$(EXTERNAL_PATH)/platform-system-core/include \
	$(EXTERNAL_PATH)/android-sqlite/android


LOCAL_STATIC_LIBRARIES := \
	libsqlcipher \
	libsqlite3_android

LOCAL_SHARED_LIBRARIES := \
	libcrypto \
	libssl

ifneq ($(TARGET_SIMULATOR),true)
LOCAL_SHARED_LIBRARIES += \
	libdl
endif

LOCAL_LDLIBS += -ldl -llog

ifeq ($(TARGET_SIMULATOR),true)
ifeq ($(TARGET_OS)-$(TARGET_ARCH),linux-x86)
LOCAL_LDLIBS += -lrt
endif
endif

ifeq ($(WITH_MALLOC_LEAK_CHECK),true)
	LOCAL_CFLAGS += -DMALLOC_LEAK_CHECK
endif

LOCAL_MODULE:= libdatabase_sqlcipher

include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))
include $(EXTERNAL_PATH)/Android.mk
