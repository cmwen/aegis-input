cmake_minimum_required(VERSION 3.22.1)
project("aegisinput-engine" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)

# Independent full-Pinyin decoder; dictionary is bundled as an app asset.
add_library(aegisinput-engine SHARED
    rime_jni.cpp
)

find_library(log-lib log)

target_link_options(aegisinput-engine PRIVATE
    "LINKER:-z,max-page-size=16384"
)

target_link_libraries(aegisinput-engine
    ${log-lib}
)
