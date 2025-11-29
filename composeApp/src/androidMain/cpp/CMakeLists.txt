cmake_minimum_required(VERSION 3.22)
project(fluidsynth_wrapper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Custom lib folder
set(FLUIDSYNTH_DIR ${CMAKE_CURRENT_SOURCE_DIR}/fluidsynth)
set(FLUIDSYNTH_LIB_DIR ${FLUIDSYNTH_DIR}/lib/${CMAKE_ANDROID_ARCH_ABI})

# Add the fluidsynth library
add_library(libfluidsynth SHARED IMPORTED)
set(SHARED_LIBRARY_SO ${FLUIDSYNTH_LIB_DIR}/libfluidsynth.so)
set_target_properties(libfluidsynth PROPERTIES IMPORTED_LOCATION ${SHARED_LIBRARY_SO})

# Add the dependencies included in Android NDK (OpenMP)
find_package(OpenMP REQUIRED)

# Add the dependencies included in the release
# Repeat the process with all the dependencies included (libFLAC, liboboe, libopus...)
add_library(libfluidsynth-assetloader SHARED IMPORTED)
set_target_properties(libfluidsynth-assetloader PROPERTIES IMPORTED_LOCATION ${FLUIDSYNTH_LIB_DIR}/libfluidsynth-assetloader.so)

# Put the pieces together under a single library
add_library(fluidsynth INTERFACE)
target_link_libraries(fluidsynth INTERFACE
        libFLAC
        libfluidsynth-assetloader
        libgio-2.0
        libglib-2.0
        libgmodule-2.0
        libgobject-2.0
        libgthread-2.0
        libinstpatch-1.0
        liboboe
        libogg
        libopus
        libpcre
        libpcreposix
        libsndfile
        libvorbis
        libvorbisenc
        libvorbisfile
        libfluidsynth
        OpenMP::OpenMP_CXX
)

# Create shared library
add_library(fluidsynth_wrapper SHARED
    fluidsynth_wrapper.cpp
)

# Link FluidSynth library
target_link_libraries(fluidsynth_wrapper
    fluidsynth
    log
)

# Include directories for JNI
#target_include_directories(fluidsynth_wrapper PRIVATE
#    ${ANDROID_NDK}/toolchains/llvm/prebuilt/${ANDROID_PLATFORM}/sysroot/usr/include
#)
