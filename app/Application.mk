APP_ABI:=armeabi-v7a arm64-v8a x86 x86_64
APP_PLATFORM := android-25
APP_STL := stlport_static
APP_CFLAGS   += -DHAVE_PTHREADS -DHAVE_ANDROID_OS=1
APP_CXXFLAGS += -DHAVE_PTHREADS -DHAVE_ANDROID_OS=1

 