LOCAL_PATH := $(call my-dir)/openssl

# Enable to be able to use ALOG* with #include "cutils/log.h"
#log_c_includes += system/core/include
#log_shared_libraries := liblog

# These makefiles are here instead of being Android.mk files in the
# respective crypto, ssl, and apps directories so
# that import_openssl.sh import won't remove them.
include $(LOCAL_PATH)/build-config-64.mk
include $(LOCAL_PATH)/build-config-32.mk
#include $(LOCAL_PATH)/openssl-aosp/Crypto.mk
#######################################
# target static library
include $(CLEAR_VARS)
LOCAL_SHARED_LIBRARIES := $(log_shared_libraries)
LOCAL_C_INCLUDES := $(log_c_includes) $(LOCAL_PATH)/include $(LOCAL_PATH)/crypto $(LOCAL_PATH)/crypto/asn1 $(LOCAL_PATH)/crypto/evp $(LOCAL_PATH)/crypto/modes

# The static library should be used in only unbundled apps
# and we don't have clang in unbundled build yet.
LOCAL_SDK_VERSION := 9

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libcrypto_static
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/android-config.mk 
include $(LOCAL_PATH)/Crypto-config-target.mk
include $(LOCAL_PATH)/android-config.mk
# Replace cflags with static-specific cflags so we dont build in libdl deps
LOCAL_CFLAGS_32 := $(openssl_cflags_static_32)
LOCAL_CFLAGS_64 := $(openssl_cflags_static_64)
LOCAL_CFLAGS := -DNO_WINDOWS_BRAINDEATH
LOCAL_SRC_FILES := $(common_src_files)
ifeq ($(TARGET_ARCH_ABI), armeabi)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_32) 
LOCAL_CFLAGS += $(LOCAL_CFLAGS_arm)
LOCAL_SRC_FILES :=  $(filter-out $(arm_exclude_files),$(LOCAL_SRC_FILES)) $(arm_src_files)
else ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_32) 
LOCAL_CFLAGS += $(LOCAL_CFLAGS_arm)
LOCAL_SRC_FILES :=  $(filter-out $(arm_exclude_files),$(LOCAL_SRC_FILES)) $(arm_src_files)
else ifeq  ($(TARGET_ARCH_ABI), x86)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_32)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_x86)
LOCAL_SRC_FILES :=  $(filter-out $(x86_exclude_files),$(LOCAL_SRC_FILES)) $(x86_src_files)
else ifeq  ($(TARGET_ARCH_ABI), arm64-v8a)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_64)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_arm64)
LOCAL_SRC_FILES :=  $(filter-out $(arm64_exclude_files),$(LOCAL_SRC_FILES)) $(arm64_src_files)
else ifeq  ($(TARGET_ARCH_ABI), x86_64)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_64)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_x86_64)
LOCAL_SRC_FILES :=  $(filter-out $(x86_64_exclude_files),$(LOCAL_SRC_FILES)) $(x86_64_src_files)
endif

LOCAL_CFLAGS += -DOPENSSL_THREADS -D_REENTRANT -DDSO_DLFCN -DHAVE_DLFCN_H -O3 -fomit-frame-pointer -Wall -DSHA1_ASM -DSHA256_ASM -DSHA512_ASM -DMD5_ASM -DRMD160_ASM -DAES_ASM -DVPAES_ASM -DWHIRLPOOL_ASM -DGHASH_ASM -DOPENSSL_NO_EC_NISTP_64_GCC_128 -DOPENSSL_NO_GMP -DOPENSSL_NO_JPAKE -DOPENSSL_NO_MD2 -DOPENSSL_NO_RC5 -DOPENSSL_NO_RFC3779 -DOPENSSL_NO_SCTP -DOPENSSL_NO_STORE

include $(BUILD_STATIC_LIBRARY)

#######################################
# target shared library
include $(CLEAR_VARS)
LOCAL_SHARED_LIBRARIES := $(log_shared_libraries)
LOCAL_C_INCLUDES := $(log_c_includes) $(LOCAL_PATH)/include $(LOCAL_PATH)/crypto $(LOCAL_PATH)/crypto/asn1 $(LOCAL_PATH)/crypto/evp $(LOCAL_PATH)/crypto/modes

# If we're building an unbundled build, don't try to use clang since it's not
# in the NDK yet. This can be removed when a clang version that is fast enough
# in the NDK.
ifeq (,$(TARGET_BUILD_APPS))
LOCAL_CLANG := true
ifeq ($(HOST_OS), darwin)
LOCAL_ASFLAGS += -no-integrated-as
LOCAL_CFLAGS += -no-integrated-as
endif
else
LOCAL_SDK_VERSION := 9
endif
LOCAL_LDFLAGS += -ldl

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libcrypto
LOCAL_STATIC_LIBRARIES := libcrypto_static

include $(BUILD_SHARED_LIBRARY)


#######################################
# target static library
include $(CLEAR_VARS)
LOCAL_SHARED_LIBRARIES := $(log_shared_libraries)
LOCAL_C_INCLUDES := $(log_c_includes)

# The static library should be used in only unbundled apps
# and we don't have clang in unbundled build yet.
LOCAL_SDK_VERSION := 9

LOCAL_SRC_FILES += $(target_src_files)
LOCAL_CFLAGS += $(target_c_flags)
LOCAL_C_INCLUDES += $(target_c_includes)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libssl_static
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/android-config.mk $(LOCAL_PATH)/Ssl.mk
include $(LOCAL_PATH)/Ssl-config-target.mk
include $(LOCAL_PATH)/android-config.mk

LOCAL_SRC_FILES := $(common_src_files)
ifeq ($(TARGET_ARCH_ABI), armeabi)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_32) 
LOCAL_CFLAGS += $(LOCAL_CFLAGS_arm)
LOCAL_SRC_FILES +=  $(arm_src_files)
else ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_32) 
LOCAL_CFLAGS += $(LOCAL_CFLAGS_arm)
LOCAL_SRC_FILES +=  $(arm_src_files)
else ifeq  ($(TARGET_ARCH_ABI), x86)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_32)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_x86)
LOCAL_SRC_FILES +=  $(x86_src_files)
else ifeq ($(TARGET_ARCH_ABI), arm64-v8a)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_64) 
LOCAL_CFLAGS += $(LOCAL_CFLAGS_arm)
LOCAL_SRC_FILES +=  $(arm64_src_files)
else ifeq  ($(TARGET_ARCH_ABI), x86_64)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_64)
LOCAL_CFLAGS += $(LOCAL_CFLAGS_x86_64)
LOCAL_SRC_FILES +=  $(x86_64_src_files)
endif

LOCAL_C_INCLUDES := $(LOCAL_C_INCLUDES) $(LOCAL_PATH)/include $(LOCAL_PATH)/crypto 
LOCAL_CFLAGS += -DOPENSSL_THREADS -D_REENTRANT -DDSO_DLFCN -DHAVE_DLFCN_H -O3 -fomit-frame-pointer -Wall -DSHA1_ASM -DSHA256_ASM -DSHA512_ASM -DMD5_ASM -DRMD160_ASM -DAES_ASM -DVPAES_ASM -DWHIRLPOOL_ASM -DGHASH_ASM -DOPENSSL_NO_EC_NISTP_64_GCC_128 -DOPENSSL_NO_GMP -DOPENSSL_NO_JPAKE -DOPENSSL_NO_MD2 -DOPENSSL_NO_RC5 -DOPENSSL_NO_RFC3779 -DOPENSSL_NO_SCTP -DOPENSSL_NO_STORE

include $(BUILD_STATIC_LIBRARY)

#######################################
# target shared library
include $(CLEAR_VARS)
LOCAL_SHARED_LIBRARIES := $(log_shared_libraries)
LOCAL_C_INCLUDES := $(log_c_includes)

# If we're building an unbundled build, don't try to use clang since it's not
# in the NDK yet. This can be removed when a clang version that is fast enough
# in the NDK.
ifeq (,$(TARGET_BUILD_APPS))
LOCAL_CLANG := true
else
LOCAL_SDK_VERSION := 9
endif

LOCAL_SHARED_LIBRARIES += libcrypto
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libssl
LOCAL_STATIC_LIBRARIES := libssl_static

include $(BUILD_SHARED_LIBRARY)



