# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(libs "${CMAKE_SOURCE_DIR}/src/main/jniLibs")
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)


add_library(libopencv_java3 SHARED IMPORTED )
set_target_properties(libopencv_java3 PROPERTIES
    IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_java3.so")


add_library(libopencv_saliency STATIC IMPORTED )
set_target_properties(libopencv_saliency PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_saliency.a")

add_library(libopencv_photo STATIC IMPORTED )
set_target_properties(libopencv_photo PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_photo.a")

add_library(libopencv_bgsegm STATIC IMPORTED )
set_target_properties(libopencv_bgsegm PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_bgsegm.a")

add_library(libopencv_videoio STATIC IMPORTED )
set_target_properties(libopencv_videoio PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_videoio.a")

add_library(libopencv_surface_matching STATIC IMPORTED )
set_target_properties(libopencv_surface_matching PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_surface_matching.a")

add_library(libopencv_datasets STATIC IMPORTED )
set_target_properties(libopencv_datasets PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_datasets.a")

add_library(libopencv_dnn_objdetect STATIC IMPORTED )
set_target_properties(libopencv_dnn_objdetect PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_dnn_objdetect.a")

add_library(libopencv_calib3d STATIC IMPORTED )
set_target_properties(libopencv_calib3d PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_calib3d.a")

add_library(libopencv_xphoto STATIC IMPORTED )
set_target_properties(libopencv_xphoto PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_xphoto.a")

add_library(libopencv_features2d STATIC IMPORTED )
set_target_properties(libopencv_features2d PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_features2d.a")

add_library(libopencv_img_hash STATIC IMPORTED )
set_target_properties(libopencv_img_hash PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_img_hash.a")

add_library(libopencv_dnn STATIC IMPORTED )
set_target_properties(libopencv_dnn PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_dnn.a")

add_library(libopencv_core STATIC IMPORTED )
set_target_properties(libopencv_core PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_core.a")

add_library(libopencv_rgbd STATIC IMPORTED )
set_target_properties(libopencv_rgbd PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_rgbd.a")

add_library(libopencv_plot STATIC IMPORTED )
set_target_properties(libopencv_plot PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_plot.a")

add_library(libopencv_imgcodecs STATIC IMPORTED )
set_target_properties(libopencv_imgcodecs PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_imgcodecs.a")

add_library(libopencv_highgui STATIC IMPORTED )
set_target_properties(libopencv_highgui PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_highgui.a")

add_library(libopencv_fuzzy STATIC IMPORTED )
set_target_properties(libopencv_fuzzy PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_fuzzy.a")

add_library(libopencv_structured_light STATIC IMPORTED )
set_target_properties(libopencv_structured_light PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_structured_light.a")

add_library(libopencv_video STATIC IMPORTED )
set_target_properties(libopencv_video PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_video.a")

add_library(libopencv_tracking STATIC IMPORTED )
set_target_properties(libopencv_tracking PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_tracking.a")

add_library(libopencv_superres STATIC IMPORTED )
set_target_properties(libopencv_superres PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_superres.a")

add_library(libopencv_reg STATIC IMPORTED )
set_target_properties(libopencv_reg PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_reg.a")

add_library(libopencv_xobjdetect STATIC IMPORTED )
set_target_properties(libopencv_xobjdetect PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_xobjdetect.a")

add_library(libopencv_text STATIC IMPORTED )
set_target_properties(libopencv_text PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_text.a")

add_library(libopencv_ml STATIC IMPORTED )
set_target_properties(libopencv_ml PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_ml.a")

add_library(libopencv_ccalib STATIC IMPORTED )
set_target_properties(libopencv_ccalib PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_ccalib.a")

add_library(libopencv_objdetect STATIC IMPORTED )
set_target_properties(libopencv_objdetect PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_objdetect.a")

add_library(libopencv_stereo STATIC IMPORTED )
set_target_properties(libopencv_stereo PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_stereo.a")

add_library(libopencv_videostab STATIC IMPORTED )
set_target_properties(libopencv_videostab PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_videostab.a")

add_library(libopencv_aruco STATIC IMPORTED )
set_target_properties(libopencv_aruco PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_aruco.a")

add_library(libopencv_face STATIC IMPORTED )
set_target_properties(libopencv_face PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_face.a")

add_library(libopencv_stitching STATIC IMPORTED )
set_target_properties(libopencv_stitching PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_stitching.a")

add_library(libopencv_phase_unwrapping STATIC IMPORTED )
set_target_properties(libopencv_phase_unwrapping PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_phase_unwrapping.a")

add_library(libopencv_xfeatures2d STATIC IMPORTED )
set_target_properties(libopencv_xfeatures2d PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_xfeatures2d.a")

add_library(libopencv_ximgproc STATIC IMPORTED )
set_target_properties(libopencv_ximgproc PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_ximgproc.a")

add_library(libopencv_shape STATIC IMPORTED )
set_target_properties(libopencv_shape PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_shape.a")

add_library(libopencv_line_descriptor STATIC IMPORTED )
set_target_properties(libopencv_line_descriptor PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_line_descriptor.a")

add_library(libopencv_dpm STATIC IMPORTED )
set_target_properties(libopencv_dpm PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_dpm.a")

add_library(libopencv_flann STATIC IMPORTED )
set_target_properties(libopencv_flann PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_flann.a")

add_library(libopencv_hfs STATIC IMPORTED )
set_target_properties(libopencv_hfs PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_hfs.a")

add_library(libopencv_bioinspired STATIC IMPORTED )
set_target_properties(libopencv_bioinspired PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_bioinspired.a")

add_library(libopencv_imgproc STATIC IMPORTED )
set_target_properties(libopencv_imgproc PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_imgproc.a")

add_library(libopencv_optflow STATIC IMPORTED )
set_target_properties(libopencv_optflow PROPERTIES IMPORTED_LOCATION "${libs}/${ANDROID_ABI}/libopencv_optflow.a")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11 -fexceptions -frtti")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

aux_source_directory(src/main/cpp/ DIR_LIB_SOURCE)

add_library( # Sets the name of the library.
             photoprocessing

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/enhance.cpp
             src/main/cpp/nanojpeg.cpp
             src/main/cpp/mem_utils.cpp
             src/main/cpp/bitmap.cpp
             src/main/cpp/bicubic_resize.cpp
             src/main/cpp/filter.cpp
             src/main/cpp/transform.cpp
             src/main/cpp/colour_space.cpp
             src/main/cpp/matrix.cpp
             src/main/cpp/blur.cpp
             src/main/cpp/filters.cpp
             src/main/cpp/photo_processing.cpp )

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

target_link_libraries( # Specifies the target library.
                         photoprocessing

                        libopencv_java3
 #                       libopencv_java
                        libopencv_saliency
                        libopencv_photo
                        libopencv_bgsegm
                        libopencv_videoio
                        libopencv_surface_matching
                        libopencv_datasets
                        libopencv_dnn_objdetect
                        libopencv_calib3d
                        libopencv_xphoto
                        libopencv_features2d
                        libopencv_img_hash
                        libopencv_dnn
                        libopencv_core
                        libopencv_rgbd
                        libopencv_plot
                        libopencv_imgcodecs
                        libopencv_highgui
                        libopencv_fuzzy
                        libopencv_structured_light
                        libopencv_video
                        libopencv_tracking
                        libopencv_superres
                        libopencv_reg
                        libopencv_xobjdetect
                        libopencv_text
                        libopencv_ml
                        libopencv_ccalib
                        libopencv_objdetect
                        libopencv_stereo
                        libopencv_videostab
                        libopencv_aruco
                        libopencv_face
                        libopencv_stitching
                        libopencv_phase_unwrapping
                        libopencv_xfeatures2d
                        libopencv_ximgproc
                        libopencv_shape
                        libopencv_line_descriptor
                        libopencv_dpm
                        libopencv_flann
                        libopencv_hfs
                        libopencv_bioinspired
                        libopencv_imgproc
                        libopencv_optflow

                         # Links the target library to the log library
                         # included in the NDK.
                         ${log-lib}
                         ${jnigraphics-lib})