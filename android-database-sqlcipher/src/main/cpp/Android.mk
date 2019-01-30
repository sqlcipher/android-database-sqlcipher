LOCAL_PATH := $(call my-dir)
MY_PATH := $(LOCAL_PATH)
include $(CLEAR_VARS)
LOCAL_PATH := $(MY_PATH)

SQLCIPHER_SRC := $(SQLCIPHER_DIR)/sqlite3.c
LOCAL_CFLAGS +=  $(SQLCIPHER_CFLAGS) $(SQLCIPHER_OTHER_CFLAGS)
LOCAL_C_INCLUDES := $(SQLCIPHER_DIR) $(LOCAL_PATH)
LOCAL_LDLIBS := -llog -latomic
LOCAL_LDFLAGS += -L$(ANDROID_NATIVE_ROOT_DIR)/$(TARGET_ARCH_ABI) -fuse-ld=bfd
LOCAL_STATIC_LIBRARIES += static-libcrypto
LOCAL_MODULE    := libsqlcipher
LOCAL_SRC_FILES := $(SQLCIPHER_SRC) \
	jni_exception.cpp \
	net_sqlcipher_database_SQLiteCompiledSql.cpp \
	net_sqlcipher_database_SQLiteDatabase.cpp \
	net_sqlcipher_database_SQLiteProgram.cpp \
	net_sqlcipher_database_SQLiteQuery.cpp \
	net_sqlcipher_database_SQLiteStatement.cpp \
	net_sqlcipher_CursorWindow.cpp \
	CursorWindow.cpp

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := static-libcrypto
LOCAL_EXPORT_C_INCLUDES := $(OPENSSL_DIR)/include
LOCAL_SRC_FILES := $(ANDROID_NATIVE_ROOT_DIR)/$(TARGET_ARCH_ABI)/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)
