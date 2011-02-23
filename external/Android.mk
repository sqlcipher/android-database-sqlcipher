#
# Before building using this do:
#	make -f Android.mk build-local-hack

LOCAL_PATH := $(call my-dir)

# how on earth to you make this damn Android build system run cmd line progs?!?!
build-local-hack: sqlcipher/sqlite3.c ../obj/local/armeabi/libcrypto.so

sqlcipher/sqlite3.c:
	cd sqlcipher && ./configure
	make -C sqlcipher sqlite3.c

../obj/local/armeabi/libcrypto.so:
	cd openssl && ndk-build
	cp openssl/libs/armeabi/libcrypto.so openssl/libs/armeabi/libssl.so \
		../obj/local/armeabi/


#------------------------------------------------------------------------------#
# libsqlite3

# NOTE the following flags,
#   SQLITE_TEMP_STORE=3 causes all TEMP files to go into RAM. and thats the behavior we want
#   SQLITE_ENABLE_FTS3   enables usage of FTS3 - NOT FTS1 or 2.
#   SQLITE_DEFAULT_AUTOVACUUM=1  causes the databases to be subject to auto-vacuum
android_sqlite_cflags :=  -DHAVE_USLEEP=1 -DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=1048576 -DSQLITE_THREADSAFE=1 -DNDEBUG=1 -DSQLITE_ENABLE_MEMORY_MANAGEMENT=1 -DSQLITE_DEFAULT_AUTOVACUUM=1 -DSQLITE_TEMP_STORE=3 -DSQLITE_ENABLE_FTS3 -DSQLITE_ENABLE_FTS3_BACKWARDS

sqlcipher_files := \
	sqlcipher/sqlite3.c

sqlcipher_cflags := -DSQLITE_HAS_CODEC -DHAVE_FDATASYNC=0 -Dfdatasync=fsync
sqlcipher_ldflags:= -Lopenssl/libs/armeabi -lcrypto

include $(CLEAR_VARS)

LOCAL_CFLAGS += $(android_sqlite_cflags) $(sqlcipher_cflags)
LOCAL_C_INCLUDES := openssl/include sqlcipher
LOCAL_LDFLAGS += $(sqlcipher_ldflags)
LOCAL_MODULE    := libsqlcipher
LOCAL_SRC_FILES := $(sqlcipher_files)

include $(BUILD_SHARED_LIBRARY)


#------------------------------------------------------------------------------#
# libsqlite3_android

# these are all files from various external git repos
libsqlite3_android_local_src_files := \
	android-sqlite/android/sqlite3_android.cpp
#	android-sqlite/android/PhonebookIndex.cpp \
	android-sqlite/android/PhoneNumberUtils.cpp \
	android-sqlite/android/PhoneNumberUtilsTest.cpp \
	android-sqlite/android/PhoneticStringUtils.cpp \
	android-sqlite/android/PhoneticStringUtilsTest.cpp \

include $(CLEAR_VARS)

LOCAL_CFLAGS += $(android_sqlite_cflags) $(sqlite_cflags)
LOCAL_C_INCLUDES := sqlcipher icu4c/i18n icu4c/common platform-system-core/include
LOCAL_MODULE    := libsqlite3_android
LOCAL_SRC_FILES := $(libsqlite3_android_local_src_files)

include $(BUILD_SHARED_LIBRARY)

## this might save us linking against the private android shared libraries like
## libnativehelper.so, libutils.so, libcutils.so, libicuuc, libicui18n.so
# LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
