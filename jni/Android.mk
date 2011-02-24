LOCAL_PATH:= $(call my-dir)

EXTERNAL_PATH := ../external

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

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	info_guardianproject_database_sqlcipher_SQLiteCompiledSql.cpp \
	info_guardianproject_database_sqlcipher_SQLiteDatabase.cpp \
	info_guardianproject_database_sqlcipher_SQLiteProgram.cpp \
	info_guardianproject_database_sqlcipher_SQLiteQuery.cpp \
	info_guardianproject_database_sqlcipher_SQLiteStatement.cpp
#	info_guardianproject_database_sqlcipher_SQLiteDebug.cpp

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

LOCAL_SHARED_LIBRARIES := \
	libcrypto \
	libssl \
	libsqlcipher \
	libsqlite3_android

LOCAL_CFLAGS += -U__APPLE__
LOCAL_LDFLAGS += -L../obj/local/armeabi/
LOCAL_LDLIBS += -ldl -llog -lsqlcipher

ifeq ($(WITH_MALLOC_LEAK_CHECK),true)
	LOCAL_CFLAGS += -DMALLOC_LEAK_CHECK
endif

LOCAL_MODULE:= libdatabase_sqlcipher

include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))
