LOCAL_PATH := $(call my-dir)
MY_PATH := $(LOCAL_PATH)
include $(CLEAR_VARS)
LOCAL_PATH := $(MY_PATH)

LOCAL_CFLAGS +=  $(SQLCIPHER_CFLAGS) $(SQLCIPHER_OTHER_CFLAGS)
LOCAL_C_INCLUDES += $(LOCAL_PATH) $(OPENSSL_DIR)
LOCAL_LDLIBS := -llog
LOCAL_LDFLAGS += -L$(ANDROID_NATIVE_ROOT_DIR)/$(TARGET_ARCH_ABI)
ifeq ($(CRYPTO_LIB),wolfssl)
LOCAL_SHARED_LIBRARIES += libwolfssl
else
LOCAL_STATIC_LIBRARIES += static-libcrypto
endif
LOCAL_MODULE    := libsqlcipher
LOCAL_SRC_FILES := sqlite3.c \
	jni_exception.cpp \
	net_sqlcipher_database_SQLiteCompiledSql.cpp \
	net_sqlcipher_database_SQLiteDatabase.cpp \
	net_sqlcipher_database_SQLiteProgram.cpp \
	net_sqlcipher_database_SQLiteQuery.cpp \
	net_sqlcipher_database_SQLiteStatement.cpp \
	net_sqlcipher_CursorWindow.cpp \
	CursorWindow.cpp

include $(BUILD_SHARED_LIBRARY)

ifeq ($(CRYPTO_LIB),wolfssl)

include $(CLEAR_VARS)

# BUILD_SQLCIPHER: Used to indicate which product wolfSSL is being built for
# WOLFSSL_USER_SETTINGS: Use build configurations from a user_settings.h file
LOCAL_MODULE:= libwolfssl
LOCAL_MODULE_TAGS := optional
LOCAL_VENDOR_MODULE := true
LOCAL_ARM_MODE := arm
LOCAL_MULTILIB := both
LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)
LOCAL_CFLAGS := -DWOLFSSL_USER_SETTINGS -DBUILD_SQLCIPHER -Os -fomit-frame-pointer
LOCAL_EXPORT_CFLAGS := -DWOLFSSL_USER_SETTINGS -DBUILD_SQLCIPHER
LOCAL_C_INCLUDES += \
	$(OPENSSL_DIR)/wolfssl \
	$(OPENSSL_DIR) \

LOCAL_SRC_FILES:= \
	$(OPENSSL_DIR)/src/crl.c \
	$(OPENSSL_DIR)/src/internal.c \
	$(OPENSSL_DIR)/src/keys.c \
	$(OPENSSL_DIR)/src/ocsp.c \
	$(OPENSSL_DIR)/src/sniffer.c \
	$(OPENSSL_DIR)/src/ssl.c \
	$(OPENSSL_DIR)/src/tls.c \
	$(OPENSSL_DIR)/src/tls13.c \
	$(OPENSSL_DIR)/src/wolfio.c

LOCAL_SRC_FILES+= \
	$(OPENSSL_DIR)/wolfcrypt/src/arc4.c \
	$(OPENSSL_DIR)/wolfcrypt/src/asm.c \
	$(OPENSSL_DIR)/wolfcrypt/src/asn.c \
	$(OPENSSL_DIR)/wolfcrypt/src/blake2b.c \
	$(OPENSSL_DIR)/wolfcrypt/src/blake2s.c \
	$(OPENSSL_DIR)/wolfcrypt/src/camellia.c \
	$(OPENSSL_DIR)/wolfcrypt/src/chacha.c \
	$(OPENSSL_DIR)/wolfcrypt/src/chacha20_poly1305.c \
	$(OPENSSL_DIR)/wolfcrypt/src/coding.c \
	$(OPENSSL_DIR)/wolfcrypt/src/compress.c \
	$(OPENSSL_DIR)/wolfcrypt/src/cpuid.c \
	$(OPENSSL_DIR)/wolfcrypt/src/cryptocb.c \
	$(OPENSSL_DIR)/wolfcrypt/src/curve25519.c \
	$(OPENSSL_DIR)/wolfcrypt/src/dsa.c \
	$(OPENSSL_DIR)/wolfcrypt/src/ecc_fp.c \
	$(OPENSSL_DIR)/wolfcrypt/src/ed25519.c \
	$(OPENSSL_DIR)/wolfcrypt/src/error.c \
	$(OPENSSL_DIR)/wolfcrypt/src/fe_low_mem.c \
	$(OPENSSL_DIR)/wolfcrypt/src/fe_operations.c \
	$(OPENSSL_DIR)/wolfcrypt/src/ge_low_mem.c \
	$(OPENSSL_DIR)/wolfcrypt/src/ge_operations.c \
	$(OPENSSL_DIR)/wolfcrypt/src/hash.c \
	$(OPENSSL_DIR)/wolfcrypt/src/hc128.c \
	$(OPENSSL_DIR)/wolfcrypt/src/idea.c \
	$(OPENSSL_DIR)/wolfcrypt/src/integer.c \
	$(OPENSSL_DIR)/wolfcrypt/src/logging.c \
	$(OPENSSL_DIR)/wolfcrypt/src/md2.c \
	$(OPENSSL_DIR)/wolfcrypt/src/md4.c \
	$(OPENSSL_DIR)/wolfcrypt/src/md5.c \
	$(OPENSSL_DIR)/wolfcrypt/src/memory.c \
	$(OPENSSL_DIR)/wolfcrypt/src/pkcs12.c \
	$(OPENSSL_DIR)/wolfcrypt/src/pkcs7.c \
	$(OPENSSL_DIR)/wolfcrypt/src/poly1305.c \
	$(OPENSSL_DIR)/wolfcrypt/src/pwdbased.c \
	$(OPENSSL_DIR)/wolfcrypt/src/rabbit.c \
	$(OPENSSL_DIR)/wolfcrypt/src/ripemd.c \
	$(OPENSSL_DIR)/wolfcrypt/src/selftest.c \
	$(OPENSSL_DIR)/wolfcrypt/src/signature.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sp_arm32.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sp_arm64.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sp_armthumb.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sp_c32.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sp_c64.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sp_cortexm.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sp_int.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sp_x86_64.c \
	$(OPENSSL_DIR)/wolfcrypt/src/srp.c \
	$(OPENSSL_DIR)/wolfcrypt/src/tfm.c \
	$(OPENSSL_DIR)/wolfcrypt/src/wc_encrypt.c \
	$(OPENSSL_DIR)/wolfcrypt/src/wc_pkcs11.c \
	$(OPENSSL_DIR)/wolfcrypt/src/wc_port.c \
	$(OPENSSL_DIR)/wolfcrypt/src/wolfevent.c \
	$(OPENSSL_DIR)/wolfcrypt/src/wolfmath.c

# FIPS Boundary Files
LOCAL_SRC_FILES+= \
	$(OPENSSL_DIR)/wolfcrypt/src/wolfcrypt_first.c \
	$(OPENSSL_DIR)/wolfcrypt/src/aes.c \
	$(OPENSSL_DIR)/wolfcrypt/src/cmac.c \
	$(OPENSSL_DIR)/wolfcrypt/src/des3.c \
	$(OPENSSL_DIR)/wolfcrypt/src/dh.c \
	$(OPENSSL_DIR)/wolfcrypt/src/ecc.c \
	$(OPENSSL_DIR)/wolfcrypt/src/hmac.c \
	$(OPENSSL_DIR)/wolfcrypt/src/kdf.c \
	$(OPENSSL_DIR)/wolfcrypt/src/random.c \
	$(OPENSSL_DIR)/wolfcrypt/src/rsa.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sha.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sha256.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sha3.c \
	$(OPENSSL_DIR)/wolfcrypt/src/sha512.c \
	$(OPENSSL_DIR)/wolfcrypt/src/fips.c \
	$(OPENSSL_DIR)/wolfcrypt/src/fips_test.c \
	$(OPENSSL_DIR)/wolfcrypt/src/wolfcrypt_last.c

include $(BUILD_SHARED_LIBRARY)

else

include $(CLEAR_VARS)
LOCAL_MODULE := static-libcrypto
LOCAL_EXPORT_C_INCLUDES := $(OPENSSL_DIR)/include
LOCAL_SRC_FILES := $(ANDROID_NATIVE_ROOT_DIR)/$(TARGET_ARCH_ABI)/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)

endif
