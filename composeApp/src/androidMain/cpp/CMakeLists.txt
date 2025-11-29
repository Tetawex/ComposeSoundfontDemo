cmake_minimum_required(VERSION 3.22)
project(fluidsynth_wrapper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add fluidsynth as a subdirectory or find it
set(FLUIDSYNTH_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/fluidsynth/include")
set(FLUIDSYNTH_LIB_DIR "${CMAKE_CURRENT_SOURCE_DIR}/fluidsynth/lib/${CMAKE_ANDROID_ARCH_ABI}")

# Import all dependencies as shared libraries
add_library(libfluidsynth-assetloader SHARED IMPORTED)
set_target_properties(libfluidsynth-assetloader PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libfluidsynth-assetloader.so)

add_library(libfluidsynth SHARED IMPORTED)
set_target_properties(libfluidsynth PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libfluidsynth.so)

add_library(libFLAC SHARED IMPORTED)
set_target_properties(libFLAC PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libFLAC.so)

add_library(liboboe SHARED IMPORTED)
set_target_properties(liboboe PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/liboboe.so)

add_library(libopus SHARED IMPORTED)
set_target_properties(libopus PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libopus.so)

add_library(libogg SHARED IMPORTED)
set_target_properties(libogg PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libogg.so)

add_library(libvorbis SHARED IMPORTED)
set_target_properties(libvorbis PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libvorbis.so)

add_library(libvorbisenc SHARED IMPORTED)
set_target_properties(libvorbisenc PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libvorbisenc.so)

add_library(libvorbisfile SHARED IMPORTED)
set_target_properties(libvorbisfile PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libvorbisfile.so)

add_library(libsndfile SHARED IMPORTED)
set_target_properties(libsndfile PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libsndfile.so)

add_library(libinstpatch SHARED IMPORTED)
set_target_properties(libinstpatch PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libinstpatch-1.0.so)

add_library(libglib SHARED IMPORTED)
set_target_properties(libglib PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libglib-2.0.so)

add_library(libgobject SHARED IMPORTED)
set_target_properties(libgobject PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libgobject-2.0.so)

add_library(libgio SHARED IMPORTED)
set_target_properties(libgio PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libgio-2.0.so)

add_library(libgmodule SHARED IMPORTED)
set_target_properties(libgmodule PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libgmodule-2.0.so)

add_library(libgthread SHARED IMPORTED)
set_target_properties(libgthread PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libgthread-2.0.so)

add_library(libpcre SHARED IMPORTED)
set_target_properties(libpcre PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libpcre.so)

add_library(libpcreposix SHARED IMPORTED)
set_target_properties(libpcreposix PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libpcreposix.so)

# Create the shared library
add_library(fluidsynth_wrapper SHARED
    fluidsynth_wrapper.cpp
)

# Include directories
target_include_directories(fluidsynth_wrapper PRIVATE
    ${FLUIDSYNTH_INCLUDE_DIR}
)

# Link all dependencies
target_link_libraries(fluidsynth_wrapper PRIVATE
    libfluidsynth-assetloader
    libfluidsynth
    libFLAC
    liboboe
    libopus
    libogg
    libvorbis
    libvorbisenc
    libvorbisfile
    libsndfile
    libinstpatch
    libglib
    libgobject
    libgio
    libgmodule
    libgthread
    libpcre
    libpcreposix
    log
)
