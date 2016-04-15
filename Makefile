.DEFAULT_GOAL := all
JNI_DIR := ${CURDIR}/jni
EXTERNAL_DIR := ${CURDIR}/external
SQLCIPHER_DIR := ${CURDIR}/external/sqlcipher
SQLCIPHER_CFLAGS :=  -DHAVE_USLEEP=1 -DSQLITE_HAS_CODEC \
	-DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=1048576 -DSQLITE_THREADSAFE=1 -DNDEBUG=1 \
	-DSQLITE_ENABLE_MEMORY_MANAGEMENT=1 -DSQLITE_TEMP_STORE=3 \
	-DSQLITE_ENABLE_FTS3_BACKWARDS -DSQLITE_ENABLE_LOAD_EXTENSION \
	-DSQLITE_ENABLE_MEMORY_MANAGEMENT -DSQLITE_ENABLE_COLUMN_METADATA \
	-DSQLITE_ENABLE_FTS4 -DSQLITE_ENABLE_UNLOCK_NOTIFY -DSQLITE_ENABLE_RTREE \
	-DSQLITE_SOUNDEX -DSQLITE_ENABLE_STAT3 -DSQLITE_ENABLE_FTS4_UNICODE61 \
	-DSQLITE_THREADSAFE -DSQLITE_ENABLE_JSON1 -DSQLITE_ENABLE_FTS3_PARENTHESIS \
	-DSQLITE_ENABLE_STAT4 -DSQLITE_ENABLE_FTS5

init: init-environment build-openssl-libraries

init-environment:
	git submodule update --init
	android update project -p ${CURDIR}

build-openssl-libraries:
	./build-openssl-libraries.sh

build-amalgamation:
	cd ${SQLCIPHER_DIR} && \
	./configure --enable-tempstore=yes \
		CFLAGS="${SQLCIPHER_CFLAGS}" && \
	make sqlite3.c

build-native:
	cd ${JNI_DIR} && \
	ndk-build V=1 --environment-overrides NDK_LIBS_OUT=$(JNI_DIR)/libs \
		SQLCIPHER_CFLAGS="${SQLCIPHER_CFLAGS}"

clean:
	cd ${SQLCIPHER_DIR} && \
	make clean
	cd ${JNI_DIR} && \
	ndk-build clean

distclean: clean
	rm -rf ${EXTERNAL_DIR}/android-libs

all: build-amalgamation build-native
