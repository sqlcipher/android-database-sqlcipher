
.DEFAULT_GOAL := all

init:
	git submodule update --init
	android update project -p .

all: build-external build-jni build-java

build-external:
	cd external/ && \
	make -f Android.mk build-local-hack && \
	ndk-build && \
  make -f Android.mk copy-libs-hack

build-jni:
	cd jni/ && ndk-build

build-java:
	ant compile && \
	cd bin/classes && \
	jar -cvf sqlcipher.jar .
