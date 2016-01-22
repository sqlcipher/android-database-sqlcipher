#! /usr/bin/env sh
(cd external/openssl;

    if [ ! ${ANDROID_NDK_ROOT} ]; then
        echo "ANDROID_NDK_ROOT environment variable not set, set and rerun"
        exit 1
    fi

    HOST_INFO=`uname -a`
    case ${HOST_INFO} in
        Darwin*)
            TOOLCHAIN_SYSTEM=darwin-x86
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

    rm ../android-libs/armeabi/libcrypto.a \
        ../android-libs/x86/libcrypto.a \
        ../android-libs/arm64-v8a/libcrypto.a \
        ../android-libs/x86_64/libcrypto.a


    git clean -dfx && git checkout -f
    ./Configure dist

    ANDROID_PLATFORM_VERSION=android-19
    ANDROID_TOOLCHAIN_DIR=/tmp/sqlcipher-android-toolchain
    OPENSSL_CONFIGURE_OPTIONS="-no-krb5 no-idea no-camellia
        no-seed no-bf no-cast no-rc2 no-rc4 no-rc5 no-md2 
        no-md4 no-ripemd no-rsa no-ecdh no-sock no-ssl2 no-ssl3 
        no-dsa no-dh no-ec no-ecdsa no-tls1 no-pbe no-pkcs
        no-tlsext no-pem no-rfc3779 no-whirlpool no-ui no-srp
        no-ssltrace no-tlsext no-mdc2 no-ecdh no-engine
        no-tls2 no-srtp -fPIC"

    # arm build
    ${ANDROID_NDK_ROOT}/build/tools/make-standalone-toolchain.sh \
        --platform=${ANDROID_PLATFORM_VERSION} \
        --install-dir=${ANDROID_TOOLCHAIN_DIR} \
        --system=${TOOLCHAIN_SYSTEM} \
        --arch=arm

    export PATH=${ANDROID_TOOLCHAIN_DIR}/bin:$PATH

    RANLIB=arm-linux-androideabi-ranlib \
        AR=arm-linux-androideabi-ar \
        CC=arm-linux-androideabi-gcc \
        ./Configure android ${OPENSSL_CONFIGURE_OPTIONS}

    make clean
    make build_crypto
    mv libcrypto.a ../android-libs/armeabi/

    rm -rf ${ANDROID_TOOLCHAIN_DIR}

    #armv7 build
    ${ANDROID_NDK_ROOT}/build/tools/make-standalone-toolchain.sh \
        --platform=${ANDROID_PLATFORM_VERSION} \
        --install-dir=${ANDROID_TOOLCHAIN_DIR} \
        --system=${TOOLCHAIN_SYSTEM} \
        --arch=arm

    export PATH=${ANDROID_TOOLCHAIN_DIR}/bin:$PATH

    RANLIB=arm-linux-androideabi-ranlib \
        AR=arm-linux-androideabi-ar \
        CC=arm-linux-androideabi-gcc \
        ./Configure android-armv7 ${OPENSSL_CONFIGURE_OPTIONS}

    make clean
    make build_crypto
    mv libcrypto.a ../android-libs/armeabi-v7a/

    rm -rf ${ANDROID_TOOLCHAIN_DIR}    

    # x86 build
    ${ANDROID_NDK_ROOT}/build/tools/make-standalone-toolchain.sh \
        --platform=${ANDROID_PLATFORM_VERSION} \
        --install-dir=${ANDROID_TOOLCHAIN_DIR} \
        --system=${TOOLCHAIN_SYSTEM} \
        --arch=x86

    export PATH=${ANDROID_TOOLCHAIN_DIR}/bin:$PATH

    RANLIB=i686-linux-android-ranlib \
        AR=i686-linux-android-ar \
        CC=i686-linux-android-gcc \
        ./Configure android-x86 ${OPENSSL_CONFIGURE_OPTIONS}

    make clean
    make build_crypto
    mv libcrypto.a ../android-libs/x86/
    
    rm -rf ${ANDROID_TOOLCHAIN_DIR}
    
    make clean
    git clean -dfx && git checkout -f

    # Patch openssl to support building for arm64-v8a && x86_64
    # Note, we only patch the Configure script
    patch -p1 < ../../openssl_android_64_bit_support.patch

    # arm64-v8a build
    ANDROID_PLATFORM_VERSION=android-21
    ${ANDROID_NDK_ROOT}/build/tools/make-standalone-toolchain.sh \
        --platform=${ANDROID_PLATFORM_VERSION} \
        --install-dir=${ANDROID_TOOLCHAIN_DIR} \
        --system=${TOOLCHAIN_SYSTEM} \
        --arch=arm64

    export PATH=${ANDROID_TOOLCHAIN_DIR}/bin:$PATH

    RANLIB=aarch64-linux-android-ranlib \
        AR=aarch64-linux-android-ar \
        CC=aarch64-linux-android-gcc \
        ./Configure android-aarch64 ${OPENSSL_EXCLUSION_LIST}

    make build_crypto

    mv libcrypto.a ../android-libs/arm64-v8a/
    
    rm -rf ${ANDROID_TOOLCHAIN_DIR}    

    make clean
    git clean -dfx && git checkout -f
    
    patch -p1 < ../../openssl_android_64_bit_support.patch

    # x86_64 build
    ANDROID_PLATFORM_VERSION=android-21
    ${ANDROID_NDK_ROOT}/build/tools/make-standalone-toolchain.sh \
        --platform=${ANDROID_PLATFORM_VERSION} \
        --install-dir=${ANDROID_TOOLCHAIN_DIR} \
        --system=${TOOLCHAIN_SYSTEM} \
        --arch=x86_64

    export PATH=${ANDROID_TOOLCHAIN_DIR}/bin:$PATH

    RANLIB=x86_64-linux-android-ranlib \
        AR=x86_64-linux-android-ar \
        CC=x86_64-linux-android-gcc \
        ./Configure android-x86_64 ${OPENSSL_EXCLUSION_LIST}

    make build_crypto

    mv libcrypto.a ../android-libs/x86_64/
    
    rm -rf ${ANDROID_TOOLCHAIN_DIR}    
)
