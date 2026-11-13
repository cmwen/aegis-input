cmake_minimum_required(VERSION 3.22.1)
project(aegisinput-engine LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
add_compile_options(-Wno-error=deprecated-declarations)
set(AEGIS_VENDOR "${CMAKE_CURRENT_SOURCE_DIR}/vendor")
list(PREPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/cmake")

# Compile upstream components as static dependencies of our single JNI library.
foreach(flag BUILD_SHARED_LIBS BUILD_TESTING BUILD_TEST BUILD_DATA BUILD_SAMPLE
        BUILD_SEPARATE_LIBS ENABLE_EXTERNAL_PLUGINS ENABLE_TIMESTAMP WITH_GFLAGS
        WITH_GTEST YAML_CPP_BUILD_CONTRIB YAML_CPP_BUILD_TESTS YAML_CPP_BUILD_TOOLS
        YAML_CPP_INSTALL LEVELDB_BUILD_TESTS LEVELDB_BUILD_BENCHMARKS LEVELDB_INSTALL
        OPENCC_ENABLE_INSTALL ENABLE_GTEST ENABLE_BENCHMARK BUILD_DOCUMENTATION
        BUILD_OPENCC_JIEBA_PLUGIN)
    set(${flag} OFF CACHE BOOL "Aegis native build" FORCE)
endforeach()
set(BUILD_STATIC ON CACHE BOOL "Statically link dependencies" FORCE)
set(USE_SYSTEM_MARISA ON CACHE BOOL "Use the marisa target below" FORCE)
set(LIBMARISA marisa CACHE STRING "Marisa target" FORCE)
set(WITH_UNWIND none CACHE STRING "Android does not provide libunwind" FORCE)
set(BOOST_INCLUDE_LIBRARIES algorithm crc dll interprocess range regex scope_exit signals2 utility uuid)
add_subdirectory("${AEGIS_VENDOR}/boost" native/boost EXCLUDE_FROM_ALL)
foreach(dependency glog yaml-cpp leveldb marisa-trie opencc)
    add_subdirectory("${AEGIS_VENDOR}/librime/deps/${dependency}" "native/${dependency}" EXCLUDE_FROM_ALL)
endforeach()
# Rime uses opencc/foo.hpp while upstream's source headers are flat.
file(GLOB opencc_headers "${AEGIS_VENDOR}/librime/deps/opencc/src/*.hpp")
file(COPY ${opencc_headers} DESTINATION "${CMAKE_BINARY_DIR}/opencc-include/opencc")
set(X11Keysym "${AEGIS_VENDOR}/librime/include")
add_subdirectory("${AEGIS_VENDOR}/librime" native/rime EXCLUDE_FROM_ALL)

if(ANDROID)
    add_library(aegisinput-engine SHARED rime_jni.cpp)
    target_include_directories(aegisinput-engine PRIVATE "${AEGIS_VENDOR}/librime/src" "${AEGIS_VENDOR}/librime/include")
    target_link_libraries(aegisinput-engine PRIVATE rime-static log android)
    target_link_options(aegisinput-engine PRIVATE "LINKER:-z,max-page-size=16384")
else()
    add_executable(aegis-rime-test "${CMAKE_CURRENT_SOURCE_DIR}/../../test/cpp/rime_engine_test.cpp")
    target_include_directories(aegis-rime-test PRIVATE "${AEGIS_VENDOR}/librime/src")
    target_link_libraries(aegis-rime-test PRIVATE rime-static)
endif()
