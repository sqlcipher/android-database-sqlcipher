.DEFAULT_GOAL := all
BIN_DIR := ${CURDIR}/bin
JNI_DIR := ${CURDIR}/jni
LIBS_DIR := ${CURDIR}/libs
EXTERNAL_DIR := ${CURDIR}/external
SQLCIPHER_DIR := ${CURDIR}/external/sqlcipher
LICENSE := ${CURDIR}/SQLCIPHER_LICENSE
SQLCIPHER_CFLAGS :=  \
	-DSQLITE_HAS_CODEC \
	-DSQLITE_SOUNDEX \
	-DHAVE_USLEEP=1 \
	-DSQLITE_TEMP_STORE=3 \
	-DSQLITE_THREADSAFE=1 \
	-DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=1048576 \
	-DNDEBUG=1 \
	-DSQLITE_ENABLE_MEMORY_MANAGEMENT=1 \
	-DSQLITE_ENABLE_LOAD_EXTENSION \
	-DSQLITE_ENABLE_COLUMN_METADATA \
	-DSQLITE_ENABLE_UNLOCK_NOTIFY \
	-DSQLITE_ENABLE_RTREE \
	-DSQLITE_ENABLE_STAT3 \
	-DSQLITE_ENABLE_STAT4 \
	-DSQLITE_ENABLE_JSON1 \
	-DSQLITE_ENABLE_FTS3_PARENTHESIS \
	-DSQLITE_ENABLE_FTS4 \
	-DSQLITE_ENABLE_FTS5 \
	-DSQLCIPHER_CRYPTO_OPENSSL

.PHONY: clean develop-zip release-zip release

init: init-environment build-openssl-libraries

init-environment:
	git submodule update --init
	android update project -p ${CURDIR}

build-openssl-libraries:
	./build-openssl-libraries.sh

build-amalgamation:
	cd ${SQLCIPHER_DIR} && \
	./configure --enable-tempstore=yes \
		--with-crypto-lib=none \
		CFLAGS="${SQLCIPHER_CFLAGS}" && \
	make sqlite3.c

build-java:
	ant release

build-native-32:
	cd ${JNI_DIR} && \
	ndk-build V=1 --environment-overrides NDK_LIBS_OUT=$(JNI_DIR)/libs32 \
		NDK_APPLICATION_MK=$(JNI_DIR)/Application32.mk \
		SQLCIPHER_CFLAGS="${SQLCIPHER_CFLAGS}"

build-native-64:
	cd ${JNI_DIR} && \
	ndk-build V=1 --environment-overrides NDK_LIBS_OUT=$(JNI_DIR)/libs64 \
		NDK_APPLICATION_MK=$(JNI_DIR)/Application64.mk \
		SQLCIPHER_CFLAGS="${SQLCIPHER_CFLAGS}"

build-native: build-native-32 build-native-64

clean-java:
	ant clean
	rm -rf ${LIBS_DIR}

clean-ndk:
	-cd ${JNI_DIR} && \
	ndk-build clean --environment-overrides NDK_LIBS_OUT=$(JNI_DIR)/libs32 \
		NDK_APPLICATION_MK=$(JNI_DIR)/Application32.mk  && \
	ndk-build clean --environment-overrides NDK_LIBS_OUT=$(JNI_DIR)/libs64 \
		NDK_APPLICATION_MK=$(JNI_DIR)/Application64.mk
	-rm -rf ${JNI_DIR}/libs32 ${JNI_DIR}/libs64

clean: clean-ndk clean-java
	-cd ${SQLCIPHER_DIR} && \
	make clean
	rm sqlcipher-for-android-*.zip

distclean: clean
	rm -rf ${EXTERNAL_DIR}/android-libs

copy-libs:
	-cp -R ${JNI_DIR}/libs32/* ${JNI_DIR}/libs64/* ${LIBS_DIR}

release-aar:
	-rm ${LIBS_DIR}/sqlcipher.jar
	-rm ${LIBS_DIR}/sqlcipher-javadoc.jar
	mvn package

develop-zip: LATEST_TAG := $(shell git rev-parse --short HEAD)
develop-zip: SECOND_LATEST_TAG ?= $(shell git tag | sort -r | head -1)
develop-zip: release

release-zip: LATEST_TAG := $(shell git tag | sort -r | head -1)
release-zip: SECOND_LATEST_TAG := $(shell git tag | sort -r | head -2 | tail -1)
release-zip: release

release:
	$(eval RELEASE_DIR := sqlcipher-for-android-${LATEST_TAG})
	$(eval README := ${RELEASE_DIR}/README)
	$(eval CHANGE_LOG_HEADER := "Changes included in the ${LATEST_TAG} release of SQLCipher for Android:")
	-rm -rf ${RELEASE_DIR}
	-rm ${RELEASE_DIR}.zip
	mkdir -p ${RELEASE_DIR}/docs
	cp -R ${LIBS_DIR}/* ${RELEASE_DIR}
	cp -R ${BIN_DIR}/javadoc/* ${RELEASE_DIR}/docs
	cp ${LICENSE} ${RELEASE_DIR}
	printf "%s\n\n" ${CHANGE_LOG_HEADER} > ${README}
	git log --pretty=format:' * %s' ${SECOND_LATEST_TAG}..${LATEST_TAG} >> ${README}
	find ${RELEASE_DIR} | sort -u | zip -@9 ${RELEASE_DIR}.zip
	rm -rf ${RELEASE_DIR}

all: build-amalgamation build-native build-java copy-libs
