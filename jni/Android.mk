LOCAL_PATH := $(call my-dir)
MY_PATH := $(LOCAL_PATH)
include $(CLEAR_VARS)
LOCAL_PATH := $(MY_PATH)
SQLCIPHER_DIR := ../external/sqlcipher
SQLCIPHER_SRC := $(SQLCIPHER_DIR)/sqlite3.c

LOCAL_CFLAGS +=  $(SQLCIPHER_CFLAGS)
LOCAL_LDFLAGS += -L$(LOCAL_PATH)/android-libs/$(TARGET_ARCH_ABI)
LOCAL_STATIC_LIBRARIES += static-libcrypto
LOCAL_MODULE    := libsqlcipher
LOCAL_SRC_FILES := $(SQLCIPHER_SRC)

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := static-libcrypto
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../external/openssl/include
LOCAL_SRC_FILES := $(LOCAL_PATH)/../external/android-libs/$(TARGET_ARCH_ABI)/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)
