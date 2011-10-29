git submodule init
git submodule update
cd external
make -f Android.mk build-local-hack
ndk-build
cp libs/armeabi/libsqlcipher_android.so ../libs/armeabi
cd ../jni
ndk-build
cp libs/armeabi/libdatabase_sqlcipher.so ../libs/armeabi
cd ..
