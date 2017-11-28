# Keep all native methods, their classes and any classes in their descriptors
#-keepclasseswithmembers,includedescriptorclasses class net.sqlcipher.** {
#   native <methods>;
#}

# Keep all exception classes
#-keep class net.sqlcipher.**.*Exception

# Keep classes referenced in JNI code
#-keep,includedescriptorclasses class net.sqlcipher.database.SQLiteCustomFunction { *; }
#-keep class net.sqlcipher.database.SQLiteDebug$* { *; }
#-keep net.sqlcipher.database.SQLiteCipherSpec { <fields>; }
#-keep interface net.sqlcipher.support.Log$* { *; }

# Keep methods used as callbacks from JNI code
#-keep class net.sqlcipher.repair.RepairKit { int onProgress(String, int, long); }
#-keep class net.sqlcipher.database.SQLiteConnection { void notifyCheckpoint(String, int); }