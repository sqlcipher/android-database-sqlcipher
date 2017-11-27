LOCAL_PATH := $(call my-dir)
MY_PATH := $(LOCAL_PATH)
include $(CLEAR_VARS)
LOCAL_PATH := $(MY_PATH)
SQLCIPHER_DIR := $(LOCAL_PATH)/external/sqlcipher
SQLCIPHER_SRC := $(SQLCIPHER_DIR)/sqlite3.c

APP_ABI:=armeabi armeabi-v7a arm64-v8a x86 x86_64
APP_PLATFORM := android-25
APP_STL := stlport_static
APP_CFLAGS   += -DSQLITE_HAS_CODEC -DHAVE_PTHREADS -DHAVE_ANDROID_OS=1
APP_CXXFLAGS += -DSQLITE_HAS_CODEC -DHAVE_PTHREADS -DHAVE_ANDROID_OS=1

SQLCIPHER_CFLAGS :=  \
	-DSQLITE_HAS_CODEC \
	-DSQLITE_SOUNDEX \
	-DHAVE_USLEEP=1 \
	-DSQLITE_TEMP_STORE=3 \
	-DSQLITE_THREADSAFE=1 \
	-DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=1048576 \
	-DNDEBUG=1 \
	-DSQLITE_ENABLE_MEMORY_MANAGEMENT=1 \
	-DSQLITE_ENABLE_LOAD_EXTENSION \
	-DSQLITE_ENABLE_COLUMN_METADATA \
	-DSQLITE_ENABLE_UNLOCK_NOTIFY \
	-DSQLITE_ENABLE_RTREE \
	-DSQLITE_ENABLE_STAT3 \
	-DSQLITE_ENABLE_STAT4 \
	-DSQLITE_ENABLE_JSON1 \
	-DSQLITE_ENABLE_FTS3_PARENTHESIS \
	-DSQLITE_ENABLE_FTS4 \
	-DSQLITE_ENABLE_FTS5 \
	-DSQLCIPHER_CRYPTO_OPENSSL

LOCAL_CFLAGS +=  $(SQLCIPHER_CFLAGS) -DLOG_NDEBUG
LOCAL_C_INCLUDES := $(SQLCIPHER_DIR) $(LOCAL_PATH)
LOCAL_LDLIBS := -llog -latomic
LOCAL_LDFLAGS += -L$(LOCAL_PATH)/prebuilt/$(TARGET_ARCH_ABI) -fuse-ld=gold.exe
LOCAL_STATIC_LIBRARIES += static-libcrypto
LOCAL_MODULE    := libsqlcipher
SRC_ROOT_PATH := ${LOCAL_PATH}/src/main/cpp/
LOCAL_SRC_FILES := 	$(SQLCIPHER_SRC) \
    $(SRC_ROOT_PATH)/jni_exception.cpp \
    $(SRC_ROOT_PATH)/net_sqlcipher_database_SQLiteCompiledSql.cpp \
    $(SRC_ROOT_PATH)/net_sqlcipher_database_SQLiteDatabase.cpp \
    $(SRC_ROOT_PATH)/net_sqlcipher_database_SQLiteProgram.cpp \
    $(SRC_ROOT_PATH)/net_sqlcipher_database_SQLiteQuery.cpp \
    $(SRC_ROOT_PATH)/net_sqlcipher_database_SQLiteStatement.cpp \
    $(SRC_ROOT_PATH)/net_sqlcipher_CursorWindow.cpp \
    $(SRC_ROOT_PATH)/CursorWindow.cpp

include $(BUILD_SHARED_LIBRARY)
include $(CLEAR_VARS)
LOCAL_MODULE := static-libcrypto
LOCAL_EXPORT_C_INCLUDES := $(SQLCIPHER_DIR) $(LOCAL_PATH)/prebuilt/$(TARGET_ARCH_ABI)/include/
LOCAL_SRC_FILES := $(LOCAL_PATH)/prebuilt/$(TARGET_ARCH_ABI)/lib/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)