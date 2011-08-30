LOCAL_PATH:= $(call my-dir)

EXTERNAL_PATH := ../external

ifeq ($(TARGET_ARCH), arm)
	LOCAL_CFLAGS += -DPACKED="__attribute__ ((packed))"
else
	LOCAL_CFLAGS += -DPACKED=""
endif

TARGET_PLATFORM := android-8

ifeq ($(WITH_JIT),true)
	LOCAL_CFLAGS += -DWITH_JIT
endif

ifneq ($(USE_CUSTOM_RUNTIME_HEAP_MAX),)
  LOCAL_CFLAGS += -DCUSTOM_RUNTIME_HEAP_MAX=$(USE_CUSTOM_RUNTIME_HEAP_MAX)
endif

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	CursorWindow.cpp \
	info_guardianproject_database_sqlcipher_SQLiteDatabase.cpp \
	info_guardianproject_database_sqlcipher_SQLiteProgram.cpp \
	info_guardianproject_database_sqlcipher_SQLiteQuery.cpp \
	info_guardianproject_database_sqlcipher_SQLiteStatement.cpp \
	info_guardianproject_database_sqlcipher_SQLiteCompiledSql.cpp
	#android_database_CursorWindow.cpp \
	#info_guardianproject_database_CursorWindow.cpp \
#	info_guardianproject_database_sqlcipher_SQLiteDebug.cpp

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

LOCAL_SHARED_LIBRARIES := \
	libcrypto \
	libssl \
	libsqlcipher \
	libsqlite3_android

LOCAL_CFLAGS += -U__APPLE__
LOCAL_LDFLAGS += -L../external/android-2.2/ -L../external/libs/armeabi/
LOCAL_LDFLAGS += -L/home/n8fr8/android/mydroid/out/target/product/generic/obj/SHARED_LIBRARIES/libutils_intermediates/LINKED/ -L/home/n8fr8/android/mydroid/out/target/product/generic/obj/SHARED_LIBRARIES/libbinder_intermediates/LINKED/ -L/home/n8fr8/android/mydroid/out/target/product/generic/obj/SHARED_LIBRARIES/libandroid_runtime_intermediates/LINKED/

# libs from the NDK
LOCAL_LDLIBS += -ldl -llog
# libnativehelper and libandroid_runtime are included with Android but not the NDK
LOCAL_LDLIBS += -lnativehelper -landroid_runtime -lutils -lbinder
# these are build in the ../external section
LOCAL_LDLIBS += -lsqlcipher -lsqlcipher_android

ifeq ($(WITH_MALLOC_LEAK_CHECK),true)
	LOCAL_CFLAGS += -DMALLOC_LEAK_CHECK
endif

LOCAL_MODULE:= libdatabase_sqlcipher

include $(BUILD_SHARED_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))
