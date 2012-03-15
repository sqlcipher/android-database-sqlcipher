
.DEFAULT_GOAL := all
LIBRARY_ROOT := libs

init:
	git submodule update --init
	android update project -p .

all: build-external build-jni build-java copy-libs

build-external:
	cd external/ && \
	make -f Android.mk build-local-hack && \
	ndk-build && \
	make -f Android.mk copy-libs-hack

build-jni:
	cd jni/ && \
	ndk-build

build-java:
	ant release && \
	cd bin/classes && \
	jar -cvf sqlcipher.jar .

clean:
	ant clean
	cd external/ && ndk-build clean
	cd external/sqlcipher && make clean
	cd jni/ && ndk-build clean 
	-rm ${LIBRARY_ROOT}/armeabi/libsqlcipher_android.so
	-rm ${LIBRARY_ROOT}/armeabi/libdatabase_sqlcipher.so
	-rm ${LIBRARY_ROOT}/sqlcipher.jar

copy-libs:
	mkdir -p ${LIBRARY_ROOT}/armeabi
	cp external/libs/armeabi/libsqlcipher_android.so \
		 ${LIBRARY_ROOT}/armeabi  && \
	cp jni/libs/armeabi/libdatabase_sqlcipher.so \
		${LIBRARY_ROOT}/armeabi && \
	cp bin/classes/sqlcipher.jar ${LIBRARY_ROOT} && \
	cp ${ANDROID_NDK_ROOT}/sources/cxx-stl/stlport/libs/armeabi/libstlport_shared.so \
		 ${LIBRARY_ROOT}/armeabi

copy-libs-dist:
	cp ${LIBRARY_ROOT}/*.jar dist/SQLCipherForAndroid-SDK/libs/ && \
	cp ${LIBRARY_ROOT}/armeabi/*.so dist/SQLCipherForAndroid-SDK/libs/armeabi/
