.POSIX:
.PHONY: clean distclean build
GRADLE = ./gradlew

clean:
	$(GRADLE) clean

distclean:
	$(GRADLE) distclean

build:
	$(GRADLE) build

build-openssl:
	$(GRADLE) buildOpenSSL
