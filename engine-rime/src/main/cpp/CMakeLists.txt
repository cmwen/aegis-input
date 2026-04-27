cmake_minimum_required(VERSION 3.22.1)
project("aegisinput-engine" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)

# Librime headers and prebuilt library
# Users must provide librime via local build or prebuilt AAR
# set(RIME_ROOT "${CMAKE_SOURCE_DIR}/../../../../librime" CACHE PATH "Path to librime")

add_library(aegisinput-engine SHARED
    rime_jni.cpp
)

find_library(log-lib log)

target_link_options(aegisinput-engine PRIVATE
    "LINKER:-z,max-page-size=16384"
)

target_link_libraries(aegisinput-engine
    ${log-lib}
    # ${RIME_ROOT}/lib/librime.so  # Uncomment when librime is available
)

# target_include_directories(aegisinput-engine PRIVATE ${RIME_ROOT}/include)
