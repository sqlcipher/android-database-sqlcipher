.POSIX:
.PHONY: init clean distclean build-openssl build publish-local-snapshot \
	publish-local-release publish-remote-snapshot public-remote-release check
GRADLE = ./gradlew

init:
	git submodule update --init

clean:
	$(GRADLE) clean

distclean:
	$(GRADLE) distclean

build-openssl:
	$(GRADLE) buildOpenSSL

check:
	$(GRADLE) check

build-debug: check
	$(GRADLE) android-database-sqlcipher:bundleDebugAar \
	-PdebugBuild=true

build-release: check
	$(GRADLE) android-database-sqlcipher:bundleReleaseAar \
	-PdebugBuild=false

publish-local-snapshot:
	@ $(collect-signing-info) \
	$(GRADLE) \
	-PpublishSnapshot=true \
	-PpublishLocal=true \
	-PsigningKeyId="$$gpgKeyId" \
	-PsigningKeyRingFile="$$gpgKeyRingFile" \
	-PsigningKeyPassword="$$gpgPassword" \
	uploadArchives

publish-local-release:
	@ $(collect-signing-info) \
	$(GRADLE) \
	-PpublishSnapshot=false \
	-PpublishLocal=true \
	-PsigningKeyId="$$gpgKeyId" \
	-PsigningKeyRingFile="$$gpgKeyRingFile" \
	-PsigningKeyPassword="$$gpgPassword" \
	uploadArchives

publish-remote-snapshot:
	@ $(collect-signing-info) \
	$(collect-nexus-info) \
	$(GRADLE) \
	-PpublishSnapshot=true \
	-PpublishLocal=false \
	-PsigningKeyId="$$gpgKeyId" \
	-PsigningKeyRingFile="$$gpgKeyRingFile" \
	-PsigningKeyPassword="$$gpgPassword" \
	-PnexusUsername="$$nexusUsername" \
	-PnexusPassword="$$nexusPassword" \
	uploadArchives

publish-remote-release:
	@ $(collect-signing-info) \
	$(collect-nexus-info) \
	$(GRADLE) \
	-PpublishSnapshot=false \
	-PpublishLocal=false \
	-PdebugBuild=false \
	-PsigningKeyId="$$gpgKeyId" \
	-PsigningKeyRingFile="$$gpgKeyRingFile" \
	-PsigningKeyPassword="$$gpgPassword" \
	-PnexusUsername="$$nexusUsername" \
	-PnexusPassword="$$nexusPassword" \
	uploadArchives

collect-nexus-info := \
	read -p "Enter Nexus username:" nexusUsername; \
	stty -echo; read -p "Enter Nexus password:" nexusPassword; stty echo;

collect-signing-info := \
	read -p "Enter GPG signing key id:" gpgKeyId; \
	read -p "Enter full path to GPG keyring file \
	(possibly ${HOME}/.gnupg/secring.gpg)" gpgKeyRingFile; \
	stty -echo; read -p "Enter GPG password:" gpgPassword; stty echo;
