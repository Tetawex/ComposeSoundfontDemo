cmake_minimum_required(VERSION 3.22)
project(fluidsynth_wrapper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add fluidsynth as a subdirectory or find it
set(FLUIDSYNTH_INCLUDE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/fluidsynth/include")

# Create the shared library
add_library(fluidsynth_wrapper SHARED
    fluidsynth_wrapper.cpp
)

# Include directories
target_include_directories(fluidsynth_wrapper PRIVATE
    ${FLUIDSYNTH_INCLUDE_DIR}
)

# Link fluidsynth library
target_link_directories(fluidsynth_wrapper PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}/fluidsynth/lib/${ANDROID_ABI}"
)

target_link_libraries(fluidsynth_wrapper PRIVATE
    fluidsynth
)
