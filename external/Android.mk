#
# Before building using this do:
#	make -f Android.mk build-local-hack
#   ndk-build
#   ndk-build
#	make -f Android.mk copy-libs-hack

LOCAL_PATH := $(call my-dir)
LOCAL_PRELINK_MODULE := false

# how on earth to you make this damn Android build system run cmd line progs?!?!
build-local-hack: sqlcipher/sqlite3.c ../obj/local/armeabi/libcrypto.so

sqlcipher/sqlite3.c:
	cd sqlcipher && ./configure --enable-tempstore=yes CFLAGS="-DSQL_HAS_CODEC" LDFLAGS="-lcrypto"
	make -C sqlcipher sqlite3.c

# TODO include this Android.mk to integrate this into the build
../obj/local/armeabi/libcrypto.so:
	cd openssl && ndk-build -j4
	install -p openssl/libs/armeabi/libcrypto.so openssl/libs/armeabi/libssl.so \
		 ../obj/local/armeabi/

copy-libs-hack: build-local-hack
	install -p -m644 openssl/libs/armeabi/*.so ../obj/local/armeabi/
	install -p -m644 libs/armeabi/*.so ../obj/local/armeabi/
##	install -p -m644 android-2.2/*.so ../obj/local/armeabi/

project_ldflags:= -Llibs/armeabi/ -Landroid-2.1/

#------------------------------------------------------------------------------#
# libsqlite3

# NOTE the following flags,
#   SQLITE_TEMP_STORE=3 causes all TEMP files to go into RAM. and thats the behavior we want
#   SQLITE_ENABLE_FTS3   enables usage of FTS3 - NOT FTS1 or 2.
#   SQLITE_DEFAULT_AUTOVACUUM=1  causes the databases to be subject to auto-vacuum
android_sqlite_cflags :=  -DHAVE_USLEEP=1 -DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=1048576 -DSQLITE_THREADSAFE=1 -DNDEBUG=1 -DSQLITE_ENABLE_MEMORY_MANAGEMENT=1 -DSQLITE_DEFAULT_AUTOVACUUM=1 -DSQLITE_TEMP_STORE=3 -DSQLITE_ENABLE_FTS3 -DSQLITE_ENABLE_FTS3_BACKWARDS -DSQLITE_ENABLE_LOAD_EXTENSION

sqlcipher_files := \
	sqlcipher/sqlite3.c

sqlcipher_cflags := -DSQLITE_HAS_CODEC -DHAVE_FDATASYNC=0 -Dfdatasync=fsync

include $(CLEAR_VARS)

LOCAL_CFLAGS += $(android_sqlite_cflags) $(sqlcipher_cflags)
LOCAL_C_INCLUDES := openssl/include sqlcipher
LOCAL_LDFLAGS += $(project_ldflags)
LOCAL_LDLIBS += -lcrypto
LOCAL_MODULE    := libsqlcipher
LOCAL_SRC_FILES := $(sqlcipher_files)

include $(BUILD_STATIC_LIBRARY)

#------------------------------------------------------------------------------#
# libsqlcipher_android (our version of Android's libsqlite_android)

# these are all files from various external git repos
libsqlite3_android_local_src_files := \
	android-sqlite/android/sqlite3_android.cpp \
	android-sqlite/android/PhonebookIndex.cpp \
	android-sqlite/android/PhoneNumberUtils.cpp \
	android-sqlite/android/OldPhoneNumberUtils.cpp \
	android-sqlite/android/PhoneticStringUtils.cpp \
	platform-frameworks-base/libs/utils/String8.cpp
#	android-sqlite/android/PhoneNumberUtilsTest.cpp \
#	android-sqlite/android/PhoneticStringUtilsTest.cpp \

include $(CLEAR_VARS)

## this might save us linking against the private android shared libraries like
## libnativehelper.so, libutils.so, libcutils.so, libicuuc, libicui18n.so
LOCAL_ALLOW_UNDEFINED_SYMBOLS := false

# TODO this needs to depend on libsqlcipher being built, how to do that?
#LOCAL_REQUIRED_MODULES += libsqlcipher libicui18n libicuuc 
LOCAL_STATIC_LIBRARIES := libsqlcipher libicui18n libicuuc

LOCAL_CFLAGS += $(android_sqlite_cflags) $(sqlite_cflags) -DOS_PATH_SEPARATOR="'/'"

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/sqlcipher \
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/icu4c/i18n \
	$(LOCAL_PATH)/icu4c/common \
	$(LOCAL_PATH)/platform-system-core/include \
	$(LOCAL_PATH)/platform-frameworks-base/include

LOCAL_LDFLAGS += -L$(LOCAL_PATH)/android-2.1/ -L$(LOCAL_PATH)/libs/armeabi/
LOCAL_LDLIBS := -llog -lutils -lcutils -lcrypto
LOCAL_MODULE := libsqlcipher_android
LOCAL_MODULE_FILENAME := libsqlcipher_android
LOCAL_SRC_FILES := $(libsqlite3_android_local_src_files)

include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/icu4c/Android.mk
