# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(libs "${CMAKE_SOURCE_DIR}/src/main/jniLibs")
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)

add_library(opencv_imgproc STATIC IMPORTED )
set_target_properties(opencv_imgproc PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_imgproc.a")

add_library(opencv_core STATIC IMPORTED )
set_target_properties(opencv_core PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_core.a")


if(EXISTS ${libs}/${ANDROID_ABI}/libtbb.a)
add_library(tbb STATIC IMPORTED)
endif()


if(EXISTS ${libs}/${ANDROID_ABI}/libtbb.a)
set_target_properties(tbb PROPERTIES IMPORTED_LOCATION ${libs}/${ANDROID_ABI}/libtbb.a)
endif()


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

aux_source_directory(src/main/cpp/ DIR_LIB_SOURCE)

add_library( # Sets the name of the library.
             smart_camera

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/smart_camera.cpp
             src/main/cpp/opencv_utils.cpp
             src/main/cpp/android_utils.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log)

find_library(jnigraphics-lib
             jnigraphics)


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

if(EXISTS ${libs}/${ANDROID_ABI}/libtbb.a)
target_link_libraries( # Specifies the target library.
                         smart_camera
                         opencv_imgproc
                         opencv_core
                         tbb

                         # Links the target library to the log library
                         # included in the NDK.
                         ${log-lib}
                         ${jnigraphics-lib})
else()
target_link_libraries( # Specifies the target library.
                         smart_camera
                         opencv_imgproc
                         opencv_core

                         # Links the target library to the log library
                         # included in the NDK.
                         ${log-lib}
                         ${jnigraphics-lib})
endif()