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
        ../android-libs/x86/libcrypto.a

    git clean -dfx && git checkout -f
    ./Configure dist

    ANDROID_PLATFORM_VERSION=android-14
    ANDROID_TOOLCHAIN_DIR=/tmp/sqlcipher-android-toolchain
    OPENSSL_EXCLUSION_LIST=no-krb5 no-gost no-idea no-camellia \
        no-seed no-bf no-cast no-rc2 no-rc4 no-rc5 no-md2 \
        no-md4 no-ripemd no-rsa no-ecdh no-sock no-ssl2 no-ssl3 \
        no-dsa no-dh no-ec no-ecdsa no-tls1 no-x509 no-pkcs7 \
        no-pbe no-pkcs no-tlsext no-pem no-rfc3779 no-whirlpool \
        no-ocsp no-x509v3 no-ui no-srp no-ssltrace no-tlsext \
        no-mdc2 no-ecdh no-engine no-tls2 no-srtp

    ${ANDROID_NDK_ROOT}/build/tools/make-standalone-toolchain.sh \
        --platform=${ANDROID_PLATFORM_VERSION} \
        --install-dir=${ANDROID_TOOLCHAIN_DIR} \
        --system=${TOOLCHAIN_SYSTEM} \
        --arch=arm

    export PATH=${ANDROID_TOOLCHAIN_DIR}/bin:$PATH

    RANLIB=arm-linux-androideabi-ranlib \
        AR=arm-linux-androideabi-ar \
        CC=arm-linux-androideabi-gcc \
        ./Configure android ${OPENSSL_EXCLUSION_LIST}

    make build_crypto

    mv libcrypto.a ../android-libs/armeabi/

    rm -rf ${ANDROID_TOOLCHAIN_DIR}

    ${ANDROID_NDK_ROOT}/build/tools/make-standalone-toolchain.sh \
        --platform=${ANDROID_PLATFORM_VERSION} \
        --install-dir=${ANDROID_TOOLCHAIN_DIR} \
        --system=${TOOLCHAIN_SYSTEM} \
        --arch=x86

    export PATH=${ANDROID_TOOLCHAIN_DIR}/bin:$PATH

    RANLIB=i686-linux-android-ranlib \
        AR=i686-linux-android-ar \
        CC=i686-linux-android-gcc \
        ./Configure android-x86 ${OPENSSL_EXCLUSION_LIST}

    make build_crypto

    mv libcrypto.a ../android-libs/x86/
)
