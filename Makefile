.DEFAULT_GOAL := all

init: init-environment build-openssl-libraries

init-environment:
	git submodule update --init

build-openssl-libraries:
	./build-openssl-libraries.sh

all:
