cmake_minimum_required(VERSION 3.4.1)
SET(SRC_ROOT_PATH ${PROJECT_SOURCE_DIR}/src/main/cpp/)
include_directories(
    ${SRC_ROOT_PATH}/
)
link_directories(
    ./libs
)

SET(SRC_LIST
${SRC_ROOT_PATH}/jni_exception.cpp
${SRC_ROOT_PATH}/CursorWindow.cpp
${SRC_ROOT_PATH}/net_sqlcipher_CursorWindow.cpp
${SRC_ROOT_PATH}/net_sqlcipher_database_SQLiteCompiledSql.cpp
${SRC_ROOT_PATH}/net_sqlcipher_database_SQLiteDatabase.cpp
${SRC_ROOT_PATH}/net_sqlcipher_database_SQLiteProgram.cpp
${SRC_ROOT_PATH}/net_sqlcipher_database_SQLiteQuery.cpp
${SRC_ROOT_PATH}/net_sqlcipher_database_SQLiteStatement.cpp
)
list(APPEND SRC_LIST ${OTHER_SRC})
add_library(libSqlcipher SHARED ${SRC_LIST} )
find_library(log-lib log )
target_link_libraries( libSqlcipher ${log-lib} )