cmake_minimum_required(VERSION 3.22)
project(fluidsynth_wrapper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find FluidSynth package (prebuilt)
find_package(fluidsynth REQUIRED CONFIG)

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
target_include_directories(fluidsynth_wrapper PRIVATE
    ${ANDROID_NDK}/toolchains/llvm/prebuilt/${ANDROID_PLATFORM}/sysroot/usr/include
)
