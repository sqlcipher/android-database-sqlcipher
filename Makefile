
.DEFAULT_GOAL := all
LIBRARY_ROOT := libs
JNI_DIR := ${CURDIR}/jni
EXTERNAL_DIR := ${CURDIR}/external
SQLCIPHER_DIR := ${EXTERNAL_DIR}/sqlcipher
LICENSE := SQLCIPHER_LICENSE
ASSETS_DIR := assets
OPENSSL_DIR := ${EXTERNAL_DIR}/openssl
LATEST_TAG := $(shell git tag | sort -r | head -1)
SECOND_LATEST_TAG := $(shell git tag | sort -r | head -2 | tail -1)
RELEASE_DIR := "SQLCipher for Android ${LATEST_TAG}"
CHANGE_LOG_HEADER := "Changes included in the ${LATEST_TAG} release of SQLCipher for Android:"
README := ${RELEASE_DIR}/README

init:
	git submodule update --init
	android update project -p .
	cd ${OPENSSL_DIR} && git clean -dfx && \
	git checkout -f && ./Configure dist

all: build-external build-jni build-java copy-libs

build-external:
	cd ${EXTERNAL_DIR} && \
	make -f Android.mk build-local-hack && \
	ndk-build && \
	make -f Android.mk copy-libs-hack

build-jni:
	cd ${JNI_DIR} && \
	ndk-build

build-java:
	ant release && \
	cd ${CURDIR}/bin/classes && \
	jar -cvf sqlcipher.jar .

release:
	-rm -rf ${RELEASE_DIR}
	-rm ${RELEASE_DIR}.zip
	mkdir ${RELEASE_DIR}
	cp -R ${LIBRARY_ROOT} ${RELEASE_DIR}
	cp -R ${ASSETS_DIR} ${RELEASE_DIR}
	cp ${LICENSE} ${RELEASE_DIR}
	printf "%s\n\n" ${CHANGE_LOG_HEADER} > ${README}
	git log --pretty=format:' * %s' ${SECOND_LATEST_TAG}..${LATEST_TAG} >> ${README}
	zip -r ${RELEASE_DIR}.zip ${RELEASE_DIR}
	rm -rf ${RELEASE_DIR}

clean:
	-rm SQLCipher\ for\ Android\*.zip
	-ant clean
	-cd ${EXTERNAL_DIR} && ndk-build clean
	-cd ${SQLCIPHER_DIR} && make clean
	-cd ${JNI_DIR} && ndk-build clean
	-rm ${LIBRARY_ROOT}/armeabi/libsqlcipher_android.so
	-rm ${LIBRARY_ROOT}/armeabi/libdatabase_sqlcipher.so
	-rm ${LIBRARY_ROOT}/armeabi/libstlport_shared.so
	-rm ${LIBRARY_ROOT}/sqlcipher.jar
	-rm ${LIBRARY_ROOT}/x86/libsqlcipher_android.so
	-rm ${LIBRARY_ROOT}/x86/libdatabase_sqlcipher.so
	-rm ${LIBRARY_ROOT}/x86/libstlport_shared.so

copy-libs:
	mkdir -p ${LIBRARY_ROOT}/armeabi
	cp ${EXTERNAL_DIR}/libs/armeabi/libsqlcipher_android.so \
		 ${LIBRARY_ROOT}/armeabi  && \
	cp ${JNI_DIR}/libs/armeabi/libdatabase_sqlcipher.so \
		${LIBRARY_ROOT}/armeabi && \
	cp ${CURDIR}/bin/classes/sqlcipher.jar ${LIBRARY_ROOT} && \
	cp ${EXTERNAL_DIR}/libs/armeabi/libstlport_shared.so \
		 ${LIBRARY_ROOT}/armeabi
	mkdir -p ${LIBRARY_ROOT}/x86
	cp ${EXTERNAL_DIR}/libs/x86/libsqlcipher_android.so \
		 ${LIBRARY_ROOT}/x86  && \
	cp ${JNI_DIR}/libs/x86/libdatabase_sqlcipher.so \
		${LIBRARY_ROOT}/x86 && \
	cp ${EXTERNAL_DIR}/libs/x86/libstlport_shared.so \
		 ${LIBRARY_ROOT}/x86

copy-libs-dist:
	cp ${LIBRARY_ROOT}/*.jar dist/SQLCipherForAndroid-SDK/libs/ && \
	cp ${LIBRARY_ROOT}/armeabi/*.so dist/SQLCipherForAndroid-SDK/libs/armeabi/

build-openssl-libraries:
	./build-openssl-libraries.sh
