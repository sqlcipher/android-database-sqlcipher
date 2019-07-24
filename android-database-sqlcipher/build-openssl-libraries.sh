#! /usr/bin/env bash

MINIMUM_ANDROID_SDK_VERSION=$1
MINIMUM_ANDROID_64_BIT_SDK_VERSION=$2
OPENSSL=openssl-$3

(cd src/main/external/;
 gunzip -c ${OPENSSL}.tar.gz | tar xf -
)

(cd src/main/external/${OPENSSL};

 if [[ ! ${MINIMUM_ANDROID_SDK_VERSION} ]]; then
     echo "MINIMUM_ANDROID_SDK_VERSION was not provided, include and rerun"
     exit 1
 fi

 if [[ ! ${MINIMUM_ANDROID_64_BIT_SDK_VERSION} ]]; then
     echo "MINIMUM_ANDROID_64_BIT_SDK_VERSION was not provided, include and rerun"
     exit 1
 fi

 if [[ ! ${ANDROID_NDK_ROOT} ]]; then
     echo "ANDROID_NDK_ROOT environment variable not set, set and rerun"
     exit 1
 fi

 NDK_TOOLCHAIN_VERSION=4.9
 ANDROID_LIB_ROOT=../android-libs
 ANDROID_TOOLCHAIN_DIR=/tmp/sqlcipher-android-toolchain
 OPENSSL_CONFIGURE_OPTIONS="-fPIC no-idea no-camellia \
 no-seed no-bf no-cast no-rc2 no-rc4 no-rc5 no-md2 \
 no-md4 no-ecdh no-sock no-ssl3 \
 no-dsa no-dh no-ec no-ecdsa no-tls1 \
 no-rfc3779 no-whirlpool no-srp \
 no-mdc2 no-ecdh no-engine \
 no-srtp"

 HOST_INFO=`uname -a`
 case ${HOST_INFO} in
     Darwin*)
         TOOLCHAIN_SYSTEM=darwin-x86_64
         ;;
     Linux*)
         if [[ "${HOST_INFO}" == *i686* ]]
         then
             TOOLCHAIN_SYSTEM=linux-x86
         else
             TOOLCHAIN_SYSTEM=linux-x86_64
         fi
         ;;
     *)
         echo "Toolchain unknown for host system"
         exit 1
         ;;
 esac

 rm -rf ${ANDROID_LIB_ROOT}
 
 for SQLCIPHER_TARGET_PLATFORM in armeabi armeabi-v7a x86 x86_64 arm64-v8a
 do
     echo "Building libcrypto.a for ${SQLCIPHER_TARGET_PLATFORM}"
     case "${SQLCIPHER_TARGET_PLATFORM}" in
         armeabi)
             TOOLCHAIN_ARCH=arm
             TOOLCHAIN_PREFIX=arm-linux-androideabi
             TOOLCHAIN_FOLDER=arm-linux-androideabi
             CONFIGURE_ARCH=android-arm
             ANDROID_API_VERSION=${MINIMUM_ANDROID_SDK_VERSION}
             OFFSET_BITS=32
             TOOLCHAIN_DIR=${ANDROID_TOOLCHAIN_DIR}-armeabi
             ;;
         armeabi-v7a)
             TOOLCHAIN_ARCH=arm
             TOOLCHAIN_PREFIX=arm-linux-androideabi
             TOOLCHAIN_FOLDER=arm-linux-androideabi
             CONFIGURE_ARCH="android-arm -march=armv7-a"
             ANDROID_API_VERSION=${MINIMUM_ANDROID_SDK_VERSION}
             OFFSET_BITS=32
             TOOLCHAIN_DIR=${ANDROID_TOOLCHAIN_DIR}-armeabi-v7a
             ;;
         x86)
             TOOLCHAIN_ARCH=x86
             TOOLCHAIN_PREFIX=i686-linux-android
             TOOLCHAIN_FOLDER=x86
             CONFIGURE_ARCH=android-x86
             ANDROID_API_VERSION=${MINIMUM_ANDROID_SDK_VERSION}
             OFFSET_BITS=32
             TOOLCHAIN_DIR=${ANDROID_TOOLCHAIN_DIR}-x86
             ;;
         x86_64)
             TOOLCHAIN_ARCH=x86_64
             TOOLCHAIN_PREFIX=x86_64-linux-android
             TOOLCHAIN_FOLDER=x86_64
             CONFIGURE_ARCH=android64-x86_64
             ANDROID_API_VERSION=${MINIMUM_ANDROID_64_BIT_SDK_VERSION}
             OFFSET_BITS=64
             TOOLCHAIN_DIR=${ANDROID_TOOLCHAIN_DIR}-x86_64
             ;;
         arm64-v8a)
             TOOLCHAIN_ARCH=arm64
             TOOLCHAIN_PREFIX=aarch64-linux-android
             TOOLCHAIN_FOLDER=aarch64-linux-android
             CONFIGURE_ARCH=android-arm64
             ANDROID_API_VERSION=${MINIMUM_ANDROID_64_BIT_SDK_VERSION}
             OFFSET_BITS=64
             TOOLCHAIN_DIR=${ANDROID_TOOLCHAIN_DIR}-arm64-v8a
             ;;
         *)
             echo "Unsupported build platform:${SQLCIPHER_TARGET_PLATFORM}"
             exit 1
     esac
     SOURCE_TOOLCHAIN_DIR=${ANDROID_NDK_ROOT}/toolchains/${TOOLCHAIN_FOLDER}-${NDK_TOOLCHAIN_VERSION}/prebuilt/${TOOLCHAIN_SYSTEM}
     rm -rf ${TOOLCHAIN_DIR}
     mkdir -p "${ANDROID_LIB_ROOT}/${SQLCIPHER_TARGET_PLATFORM}"
     python ${ANDROID_NDK_ROOT}/build/tools/make_standalone_toolchain.py \
            --arch ${TOOLCHAIN_ARCH} \
            --api ${ANDROID_API_VERSION} \
            --install-dir ${TOOLCHAIN_DIR} \
            --unified-headers

     if [[ $? -ne 0 ]]; then
         echo "Error executing make_standalone_toolchain.py for ${TOOLCHAIN_ARCH}"
         exit 1
     fi

     export PATH=${TOOLCHAIN_DIR}/bin:${PATH}

     ANDROID_NDK=${ANDROID_NDK_ROOT} \
                PATH=${SOURCE_TOOLCHAIN_DIR}/bin:${PATH} \
                ./Configure ${CONFIGURE_ARCH} \
                -D__ANDROID_API__=${ANDROID_API_VERSION} \
                -D_FILE_OFFSET_BITS=${OFFSET_BITS} \
                ${OPENSSL_CONFIGURE_OPTIONS} \
                --sysroot=${TOOLCHAIN_DIR}/sysroot

     if [[ $? -ne 0 ]]; then
         echo "Error executing:./Configure ${CONFIGURE_ARCH} ${OPENSSL_CONFIGURE_OPTIONS}"
         exit 1
     fi

     make clean
     make build_libs
     
     if [[ $? -ne 0 ]]; then
         echo "Error executing make for platform:${SQLCIPHER_TARGET_PLATFORM}"
         exit 1
     fi
     mv libcrypto.a ${ANDROID_LIB_ROOT}/${SQLCIPHER_TARGET_PLATFORM}
 done
)
