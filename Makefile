
.DEFAULT_GOAL := all
LIBRARY_ROOT := libs
JNI_DIR := ${CURDIR}/jni
EXTERNAL_DIR := ${CURDIR}/external
SQLCIPHER_DIR := ${EXTERNAL_DIR}/sqlcipher

init:
	git submodule update --init
	android update project -p .

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

clean:
	ant clean
	cd ${EXTERNAL_DIR} && ndk-build clean
	-cd ${SQLCIPHER_DIR} && make clean
	cd ${JNI_DIR} && ndk-build clean
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
