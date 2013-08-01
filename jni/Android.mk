LOCAL_PATH:= $(call my-dir)

EXTERNAL_PATH := $(LOCAL_PATH)/../external

LOCAL_CFLAGS += -DPACKED="__attribute__ ((packed))"

#TARGET_PLATFORM := android-8

ifeq ($(WITH_JIT),true)
	LOCAL_CFLAGS += -DWITH_JIT
endif

ifneq ($(USE_CUSTOM_RUNTIME_HEAP_MAX),)
  LOCAL_CFLAGS += -DCUSTOM_RUNTIME_HEAP_MAX=$(USE_CUSTOM_RUNTIME_HEAP_MAX)
endif

include $(CLEAR_VARS)

# expose the sqlcipher C API
LOCAL_CFLAGS += -DSQLITE_HAS_CODEC

LOCAL_SRC_FILES:= \
	net_sqlcipher_database_SQLiteCompiledSql.cpp \
	net_sqlcipher_database_SQLiteDatabase.cpp \
	net_sqlcipher_database_SQLiteProgram.cpp \
	net_sqlcipher_database_SQLiteQuery.cpp \
	net_sqlcipher_database_SQLiteStatement.cpp \
	net_sqlcipher_CursorWindow.cpp \
	CursorWindow.cpp
#	net_sqlcipher_database_sqlcipher_SQLiteDebug.cpp

LOCAL_C_INCLUDES += \
	$(JNI_H_INCLUDE) \
	$(EXTERNAL_PATH)/sqlcipher \
	$(EXTERNAL_PATH)/openssl/include \
	$(EXTERNAL_PATH)/platform-frameworks-base/core/jni \
	$(EXTERNAL_PATH)/android-sqlite/android \
	$(EXTERNAL_PATH)/dalvik/libnativehelper/include \
	$(EXTERNAL_PATH)/dalvik/libnativehelper/include/nativehelper \
	$(EXTERNAL_PATH)/platform-system-core/include \
	$(LOCAL_PATH)/include \
	$(EXTERNAL_PATH)/platform-frameworks-base/include \
	$(EXTERNAL_PATH)/icu4c/common \

#LOCAL_SHARED_LIBRARIES := libsqlcipher_android
LOCAL_STATIC_LIBRARIES := libsqlcipher_android libicuuc

LOCAL_CFLAGS += -U__APPLE__
LOCAL_LDFLAGS += -L$(EXTERNAL_PATH)/android-libs/$(TARGET_ARCH_ABI) -L$(EXTERNAL_PATH)/libs/$(TARGET_ARCH_ABI)/

# libs from the NDK
LOCAL_LDLIBS += -ldl -llog
# libnativehelper and libandroid_runtime are included with Android but not the NDK
LOCAL_LDLIBS += -lnativehelper -landroid_runtime -lutils -lbinder -lcrypto

# these are build in the ../external section

ifeq ($(WITH_MALLOC_LEAK_CHECK),true)
	LOCAL_CFLAGS += -DMALLOC_LEAK_CHECK
endif

LOCAL_MODULE:= libdatabase_sqlcipher

include $(BUILD_SHARED_LIBRARY)

$(call import-module,external)

