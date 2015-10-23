
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
RELEASE_DIR := sqlcipher-for-android-${LATEST_TAG}
CHANGE_LOG_HEADER := "Changes included in the ${LATEST_TAG} release of SQLCipher for Android:"
README := ${RELEASE_DIR}/README

# Use faketime to freeze time to make for reproducible builds.
# faketime needs to have a very specific timestamp format in order to freeze
# time.  The time needs to be frozen so that the timestamps don't depend on
# the speed of the machine that the build process is running on.  See `man
# faketime` for more info on the "advanced timestamp format".  Also, force
# time to UTC so its always the same on all machines.
ifeq ($(shell if which faketime > /dev/null; then echo faketime; fi),faketime)
  export TZ=UTC
  TIMESTAMP := $(shell faketime -f "`git log -n1 --format=format:%ai`" \
                   date -u '+%Y-%m-%d %H:%M:%S')
  TOUCH := touch -t $(shell faketime -f "`git log -n1 --format=format:%ai`" \
                        date -u '+%Y%m%d%H%M.%S')
# frozen time
  FAKETIME := faketime -f "$(TIMESTAMP)"
# time moving at 5% of normal speed
  FAKETIME_5 := faketime -f "@$(TIMESTAMP) x0.05"
endif

init:
	git submodule update --init
	android update project -p .
	cd ${OPENSSL_DIR} && git clean -dfx && \
	git checkout -f && ./Configure dist

all: build-external build-jni build-java copy-libs

build-external:
	cd ${EXTERNAL_DIR} && \
	$(FAKETIME) make -f Android.mk build-local-hack && \
	$(FAKETIME) ndk-build NDK_LIBS_OUT=$(EXTERNAL_DIR)/libs && \
	$(FAKETIME) make -f Android.mk copy-libs-hack

build-jni:
	cd ${JNI_DIR} && \
	$(FAKETIME) ndk-build NDK_LIBS_OUT=$(JNI_DIR)/libs

build-java:
	$(FAKETIME_5) ant release

release: release-zip release-aar

release-aar:
	-rm libs/sqlcipher.jar
	-rm libs/sqlcipher-javadoc.jar
	mvn package

release-zip:
	-rm -rf ${RELEASE_DIR}
	-rm ${RELEASE_DIR}.zip
	mkdir ${RELEASE_DIR}
	cp -R ${LIBRARY_ROOT} ${RELEASE_DIR}
	cp -R ${ASSETS_DIR} ${RELEASE_DIR}
	cp ${LICENSE} ${RELEASE_DIR}
	printf "%s\n\n" ${CHANGE_LOG_HEADER} > ${README}
	git log --pretty=format:' * %s' ${SECOND_LATEST_TAG}..${LATEST_TAG} >> ${README}
# fix the timestamp on the files to include in the zipball
	find ${RELEASE_DIR} | xargs $(TOUCH)
	ls -lR ${RELEASE_DIR}
	find ${RELEASE_DIR} | sort -u | $(FAKETIME) zip -@9 ${RELEASE_DIR}.zip
	rm -rf ${RELEASE_DIR}

clean:
	-rm SQLCipher\ for\ Android\*.zip
	-ant clean
	-cd ${EXTERNAL_DIR} && ndk-build clean NDK_LIBS_OUT=$(EXTERNAL_DIR)/libs
	-cd ${SQLCIPHER_DIR} && make clean
	-cd ${JNI_DIR} && ndk-build clean NDK_LIBS_OUT=$(JNI_DIR)/libs
	-rm ${LIBRARY_ROOT}/armeabi/libsqlcipher_android.so
	-rm ${LIBRARY_ROOT}/armeabi/libdatabase_sqlcipher.so
	-rm ${LIBRARY_ROOT}/armeabi/libstlport_shared.so
	-rm ${LIBRARY_ROOT}/sqlcipher.jar
	-rm ${LIBRARY_ROOT}/x86/libsqlcipher_android.so
	-rm ${LIBRARY_ROOT}/x86/libdatabase_sqlcipher.so
	-rm ${LIBRARY_ROOT}/x86/libstlport_shared.so
	-rm ${LIBRARY_ROOT}/armeabi-v7a/libsqlcipher_android.so
	-rm ${LIBRARY_ROOT}/armeabi-v7a/libdatabase_sqlcipher.so
	-rm ${LIBRARY_ROOT}/armeabi-v7a/libstlport_shared.so

copy-libs:
	mkdir -p ${LIBRARY_ROOT}/armeabi
	cp ${EXTERNAL_DIR}/libs/armeabi/libsqlcipher_android.so \
		 ${LIBRARY_ROOT}/armeabi  && \
	cp ${JNI_DIR}/libs/armeabi/libdatabase_sqlcipher.so \
		${LIBRARY_ROOT}/armeabi && \
	cp ${EXTERNAL_DIR}/libs/armeabi/libstlport_shared.so \
		 ${LIBRARY_ROOT}/armeabi
	mkdir -p ${LIBRARY_ROOT}/x86
	cp ${EXTERNAL_DIR}/libs/x86/libsqlcipher_android.so \
		 ${LIBRARY_ROOT}/x86  && \
	cp ${JNI_DIR}/libs/x86/libdatabase_sqlcipher.so \
		${LIBRARY_ROOT}/x86 && \
	cp ${EXTERNAL_DIR}/libs/x86/libstlport_shared.so \
		 ${LIBRARY_ROOT}/x86
	mkdir -p ${LIBRARY_ROOT}/armeabi-v7a
	cp ${EXTERNAL_DIR}/libs/armeabi-v7a/libsqlcipher_android.so \
		 ${LIBRARY_ROOT}/armeabi-v7a  && \
	cp ${JNI_DIR}/libs/armeabi-v7a/libdatabase_sqlcipher.so \
		${LIBRARY_ROOT}/armeabi-v7a && \
	cp ${EXTERNAL_DIR}/libs/armeabi-v7a/libstlport_shared.so \
		 ${LIBRARY_ROOT}/armeabi-v7a

copy-libs-dist:
	cp ${LIBRARY_ROOT}/*.jar dist/SQLCipherForAndroid-SDK/libs/ && \
	cp ${LIBRARY_ROOT}/armeabi/*.so dist/SQLCipherForAndroid-SDK/libs/armeabi/

build-openssl-libraries:
	./build-openssl-libraries.sh
