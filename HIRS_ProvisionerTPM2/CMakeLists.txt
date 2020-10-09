# Root CMake file in charge of managing build/testing of TPM 2.0 Provisioner Library and Executable

# General CMake Configuration
cmake_minimum_required(VERSION 2.8.12)

# Initialize Project
project(HIRS_ProvisionerTPM2)

# Set Project Information Variables
set(PROJECT_NAME hirs-provisioner-tpm2)
# Retrieve Complete Version
file(STRINGS ../VERSION COMPLETE_VERSION LIMIT_COUNT 1)
# Break Version into Components
string(REGEX MATCHALL "[0-9]+" VERSION_COMPONENTS ${COMPLETE_VERSION})
# Set MAJOR_VERSION
list(GET VERSION_COMPONENTS 0 MAJOR_VERSION)
# Set MINOR_VERSION
list(GET VERSION_COMPONENTS 1 MINOR_VERSION)
# Set PATCH_VERSION
list(GET VERSION_COMPONENTS 2 PATCH_VERSION)
# Sets PACKAGE_RELEASE_NUMBER & PACKAGE_RELEASE_RETURN_CODE
execute_process(COMMAND sh "package/package_release.sh"
        WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
        RESULT_VARIABLE PACKAGE_RELEASE_RETURN_ERROR
        OUTPUT_VARIABLE PACKAGE_RELEASE_NUMBER
        ERROR_STRIP_TRAILING_WHITESPACE
        OUTPUT_STRIP_TRAILING_WHITESPACE)

list(LENGTH VERSION_COMPONENTS VERSION_COMPONENTS_LENGTH)
# Check if version information pulled successfully, error otherwise
if(NOT ${VERSION_COMPONENTS_LENGTH} EQUAL 3)
    message(FATAL_ERROR "Failed to pull version information from VERSION file, aborting.")
elseif(${PACKAGE_RELEASE_RETURN_ERROR})
    message(FATAL_ERROR "Failed to pull package release information from git, aborting.")
endif()

# Embed version and package release into header file
configure_file ("${CMAKE_SOURCE_DIR}/include/Version.h.in"
        "${CMAKE_SOURCE_DIR}/include/Version.h")

# Attempt to Determine Build Environment
if (UNIX AND NOT APPLE)
    file(READ /etc/os-release OS_INFO)
    string(REGEX MATCH "NAME=\"[A-Za-z ]+\"" DISTRIBUTION_NAME ${OS_INFO})
    string(REGEX MATCH "VERSION_ID=\"[0-9. ]+\"" DISTRIBUTION_VERSION ${OS_INFO})
    string(REPLACE "NAME=" "" DISTRIBUTION ${DISTRIBUTION_NAME})
    string(REPLACE "VERSION_ID=" "" DISTRIBUTION_VERSION ${DISTRIBUTION_VERSION})
    string(REPLACE "\"" "" DISTRIBUTION ${DISTRIBUTION})
    string(REPLACE "\"" "" DISTRIBUTION_VERSION ${DISTRIBUTION_VERSION})
endif()

# Set C++ Standard 11 based on version information
if (${CMAKE_MAJOR_VERSION}.${CMAKE_MINOR_VERSION} GREATER 3.0)
    set(CMAKE_CXX_STANDARD 11)
    set(CMAKE_CXX_STANDARD_REQUIRED ON)
else ()
    set(CMAKE_CXX_FLAGS "-std=gnu++11")
endif ()

# Set User configurable options
option(BUILD_TESTS "Set to OFF to turn off testing" ON)
option(GENERATE_DOCS "Set to OFF to turn off documentation generation" ON)
option(STATIC_ANALYSIS "Set to OFF to turn off Static Analysis" ON)
option(STYLE_CHECK "Set to OFF to turn off code style checking" ON)

# Set Project Path Variables
set(EXECUTABLE_OUTPUT_PATH bin)
set(PROJECT_CONFIG_DIR ${CMAKE_SOURCE_DIR}/config)
set(PROJECT_CONFIG_FILES ${PROJECT_CONFIG_DIR}/log4cplus_config.ini)

# Set directories to look for header files
include_directories(${CMAKE_SOURCE_DIR}/include)
# Protobuf generated files are placed in the binary directory. The structure of
# the binary directory matches that of the source directory. Specifically,
# protobuf places the generated files in the same subfolder of the binary
# directory where the spec file was located in the source directory. In this
# case, that is the src folder. We get this file location automatically in the
# src/CMakeLists.txt file, but the variable holding its location is not defined
# in the scope of this file, so we need to add that directory to the include
# path manually.
include_directories(${CMAKE_BINARY_DIR}/src)

# Attempt to find local 3rd party libraries and set their absolute paths
# Sets LOG_LIB
find_library(LOG_LIB NAMES log4cplus)
list(APPEND REQUIRED_LIBS ${LOG_LIB})

# Sets RE_LIB
find_library(RE_LIB NAMES re2)
list(APPEND REQUIRED_LIBS ${RE_LIB})

# Setup for TPM2_TSS_LIBRARIES
find_library(TPM2_SAPI_LIB NAMES sapi tss2)
find_library(TPM2_TCTI_DEVICE_LIB NAMES tcti-device tss2)
find_library(TPM2_TCTI_SOCKET_LIB NAMES tcti-socket tss2)
find_library(TPM2_TCTI_TABRMD_LIB NAMES tcti-tabrmd tss2)
set(TPM2_TSS_LIBRARIES ${TPM2_SAPI_LIB} ${TPM2_TCTI_DEVICE_LIB}
        ${TPM2_TCTI_SOCKET_LIB} ${TPM2_TCTI_TABRMD_LIB})
list(APPEND REQUIRED_LIBS ${TPM2_TSS_LIBRARIES})

# Set variable to determine TSS SAPI import
set(TSS_LIBRARY "<sapi/tpm20.h>")
string(COMPARE EQUAL ${TPM2_SAPI_LIB} ${TPM2_TCTI_DEVICE_LIB} LEGACY_TSS2_LIB_PRESENT)
if(LEGACY_TSS2_LIB_PRESENT)
    set(TSS_LIBRARY "<tss2/tpm20.h>")
endif()

# Embed correct TSS import into header file
configure_file ("${CMAKE_SOURCE_DIR}/include/Tss.h.in"
        "${CMAKE_SOURCE_DIR}/include/Tss.h")

# Download necessary 3rd party libraries
# Setup for CPR
configure_file(lib/CPR.CMakeLists.txt.in ${CMAKE_BINARY_DIR}/lib/cpr-download/CMakeLists.txt)
set(USE_SYSTEM_CURL ON CACHE BOOL "Do not allow CPR to use its own version of curl." FORCE)
set(BUILD_CPR_TESTS OFF CACHE BOOL "Do not waste time running CPR unit tests" FORCE)
execute_process(COMMAND ${CMAKE_COMMAND} -G "${CMAKE_GENERATOR}" .
        RESULT_VARIABLE result
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/lib/cpr-download)
if(result)
    message(FATAL_ERROR "CMake step for CPR failed: ${result}")
endif()
execute_process(COMMAND ${CMAKE_COMMAND} --build .
        RESULT_VARIABLE result
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/lib/cpr-download )
if(result)
    message(FATAL_ERROR "Build step for CPR failed: ${result}")
endif()

# Add cpr directly to our build and define the cpr target.
add_subdirectory(${CMAKE_BINARY_DIR}/lib/cpr-src
        ${CMAKE_BINARY_DIR}/lib/cpr-build)
list(APPEND REQUIRED_LIBS ${CPR_LIBRARIES})

# Imports the FindProtobuf module, used to locate protobuf package and
# do source code generation
include(FindProtobuf)
# Finds protobuf binaries
find_package(Protobuf REQUIRED)
list(APPEND REQUIRED_LIBS ${PROTOBUF_LIBRARY})

# Define the TPM 2.0 Provisioner Library
add_subdirectory(src)

# Create project executable
add_executable(${PROJECT_NAME} src/TPM2_Provisioner.cpp ${PROJECT_CONFIG_FILES})

# In TPM 2.0 land, there is currently not a way to fetch the TPM version info
add_executable(tpm_version src/tpm_version.cpp)
target_link_libraries(tpm_version ${TPM2_SAPI_LIB} ${TPM2_TCTI_TABRMD_LIB})

# Link necessary libraries
target_link_libraries(${PROJECT_NAME} TPM2_PROVISIONER_LIBRARY)

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)
if(THREADS_HAVE_PTHREAD_ARG)
    target_compile_options(${PROJECT_NAME} PUBLIC "-pthread")
endif()
if(CMAKE_THREAD_LIBS_INIT)
    target_link_libraries(${PROJECT_NAME} "${CMAKE_THREAD_LIBS_INIT}")
endif()

# Set commands for installation of project on target system (i.e. "make install")
install(TARGETS ${PROJECT_NAME} tpm_version
        DESTINATION "bin")
install(FILES config/log4cplus_config.ini DESTINATION /etc/hirs/TPM2_Provisioner)
install(FILES scripts/tpm_aca_provision DESTINATION /usr/local/bin PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ GROUP_EXECUTE WORLD_READ WORLD_EXECUTE)
install(DIRECTORY DESTINATION ${/var/log/hirs/provisioner}
        DIRECTORY_PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ
                              GROUP_EXECUTE WORLD_READ WORLD_EXECUTE)

install(DIRECTORY DESTINATION ${/etc/hirs/provisioner}
        DIRECTORY_PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ
                              GROUP_EXECUTE WORLD_READ WORLD_EXECUTE)
install(FILES ../HIRS_Provisioner/src/main/resources/defaults.properties DESTINATION /etc/hirs/provisioner RENAME provisioner.properties)
install(FILES ../HIRS_Provisioner/hirs-provisioner-config.sh DESTINATION /etc/hirs/provisioner
        PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ
                    GROUP_EXECUTE WORLD_READ WORLD_EXECUTE)
install(FILES ../HIRS_Provisioner/scripts/install/hirs-provisioner.sh DESTINATION /etc/hirs/provisioner
        PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE GROUP_READ
                    GROUP_EXECUTE WORLD_READ WORLD_EXECUTE)

execute_process(COMMAND cp ../HIRS_Utils/src/main/resources/logging.properties ./config/
        RESULT_VARIABLE result
        WORKING_DIRECTORY ${CMAKE_SOURCE_DIR})
if(result)
    message(FATAL_ERROR "cp logging.properties from HIRS_Utils failed.")
endif()
execute_process(COMMAND cp ../HIRS_Provisioner/scripts/install/tpm_aca_provision ./scripts/
        RESULT_VARIABLE result
        WORKING_DIRECTORY ${CMAKE_SOURCE_DIR})
if(result)
    message(FATAL_ERROR "cp tpm_aca_provision from HIRS_Provisioner failed.")
endif()
install(FILES config/logging.properties DESTINATION /etc/hirs/)

# check if Doxygen is installed
if(GENERATE_DOCS)
    find_package(Doxygen)
    if (DOXYGEN_FOUND)
        # set input config file
        set(DOXYGEN_CONFIG ${PROJECT_CONFIG_DIR}/doxygen.config)

        add_custom_target( doc_doxygen ALL
                COMMAND ${DOXYGEN_EXECUTABLE} ${DOXYGEN_CONFIG}
                WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
                COMMENT "Generating API documentation with Doxygen"
                VERBATIM )
    else (DOXYGEN_FOUND)
        message("Doxygen needs to be installed to generate the doxygen documentation")
    endif (DOXYGEN_FOUND)
endif(GENERATE_DOCS)

# Based on user-defined flag, optionally code style check the TPM 2.0 Library
if(STYLE_CHECK)
    # Download and integrate CppLint for Style Checking
    configure_file(lib/CppLint.CMakeLists.txt.in ${CMAKE_BINARY_DIR}/lib/cpplint/CMakeLists.txt)
    execute_process(COMMAND ${CMAKE_COMMAND} -G "${CMAKE_GENERATOR}" .
            RESULT_VARIABLE result
            WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/lib/cpplint)
    if(result)
        message(FATAL_ERROR "CMake step for CppLint failed: ${result}")
    endif()
    execute_process(COMMAND ${CMAKE_COMMAND} --build .
            RESULT_VARIABLE result
            WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/lib/cpplint)
    if(result)
        message(FATAL_ERROR "Build step for CppLint failed: ${result}")
    endif()
    configure_file(${CMAKE_BINARY_DIR}/lib/cpplint-download/cpplint/cpplint.py ${CMAKE_SOURCE_DIR}/lint/cpplint.py)
    add_custom_command(
            TARGET ${PROJECT_NAME}
            COMMENT "Run Style Check"
            PRE_BUILD
            WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/lint
            COMMAND python cpplint.py --root=${CMAKE_SOURCE_DIR}/../ --filter=-build/c++11,-legal/copyright ${CMAKE_SOURCE_DIR}/src/*.cpp ${CMAKE_SOURCE_DIR}/include/*.hpp ${CMAKE_SOURCE_DIR}/src/*.c ${CMAKE_SOURCE_DIR}/include/*.h ${CMAKE_SOURCE_DIR}/test/*.cpp
    )
endif(STYLE_CHECK)

if(STATIC_ANALYSIS)
    add_custom_command(
            TARGET ${PROJECT_NAME}
            COMMENT "Run Cppcheck Static Analysis"
            PRE_BUILD
            WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
            COMMAND cppcheck
            --enable=warning,performance,portability,style
            --std=c++11
            --library=posix.cfg
            --error-exitcode=1
            --verbose
            --suppress=readdirCalled
            -I include/
            src/
    )
endif(STATIC_ANALYSIS)

# Set variables for CPack Package generation tool
set(CPACK_PACKAGE_NAME HIRS_Provisioner_TPM_2_0)
set(CPACK_PACKAGE_VENDOR "U.S. Government")
set(CPACK_PACKAGE_CONTACT "U.S. Government")
set(CPACK_PACKAGE_VERSION_MAJOR ${MAJOR_VERSION})
set(CPACK_PACKAGE_VERSION_MINOR ${MINOR_VERSION})
set(CPACK_PACKAGE_VERSION_PATCH ${PATCH_VERSION})
set(CPACK_PACKAGE_RELEASE ${PACKAGE_RELEASE_NUMBER})
set(CPACK_PACKAGE_VERSION "${CPACK_PACKAGE_VERSION_MAJOR}.${CPACK_PACKAGE_VERSION_MINOR}.${CPACK_PACKAGE_VERSION_PATCH}")
set(CPACK_PACKAGING_INSTALL_PREFIX ${CMAKE_INSTALL_PREFIX})

# Setup Development Distribution CPack
if (${DISTRIBUTION} STREQUAL "Ubuntu")
    # Set variables specific to CPack DEB package generator
    set(CPACK_GENERATOR "DEB")
    set(CPACK_DEBIAN_PACKAGE_NAME "HIRSProvisionerTPM2.0")
    set(CPACK_DEBIAN_PACKAGE_SECTION "admin")
    set(CPACK_DEBIAN_PACKAGE_DEPENDS "liblog4cplus-1.1-9(>=1.1.2), libcurl4-openssl-dev(>=7.0.0), paccor, procps(>=3.3.0)")
    # Set variables specific to Ubuntu release version
    if (${DISTRIBUTION_VERSION} STREQUAL "16.04")
        set(CPACK_DEBIAN_PACKAGE_DEPENDS "${CPACK_DEBIAN_PACKAGE_DEPENDS}, libre2-1v5(>=20160201), libprotobuf9v5(>=2.4.1)")
    else()
        set(CPACK_DEBIAN_PACKAGE_DEPENDS "${CPACK_DEBIAN_PACKAGE_DEPENDS}, libprotobuf10(>=2.4.1)")
        if (${DISTRIBUTION_VERSION} STREQUAL "17.10")
            set(CPACK_DEBIAN_PACKAGE_DEPENDS "${CPACK_DEBIAN_PACKAGE_DEPENDS}, libre2-3(>=20160201)")
        elseif(${DISTRIBUTION_VERSION} STREQUAL "18.04" OR ${DISTRIBUTION_VERSION} STREQUAL "18.10")
            set(CPACK_DEBIAN_PACKAGE_DEPENDS "${CPACK_DEBIAN_PACKAGE_DEPENDS}, libre2-4(>=20160201)")
        endif()
    endif()
    set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE amd64)
    set(CPACK_DEBIAN_PACKAGE_CONTROL_EXTRA ${CMAKE_SOURCE_DIR}/package/postinst)
    set(CPACK_PACKAGE_FILE_NAME "${CPACK_DEBIAN_PACKAGE_NAME}_${CPACK_PACKAGE_VERSION}-${CPACK_PACKAGE_RELEASE}_${CPACK_DEBIAN_PACKAGE_ARCHITECTURE}")
elseif (${DISTRIBUTION} STREQUAL "CentOS Linux")
    # Set variables specific to CPack RPM package generator
    set(CPACK_GENERATOR "RPM")
    set(CPACK_RPM_PACKAGE_NAME "HIRS_Provisioner_TPM_2_0")
    set(CPACK_RPM_PACKAGE_RELEASE_DIST "el7")
    set(CPACK_RPM_PACKAGE_LICENSE "Apache License, Version 2.0")
    set(CPACK_RPM_PACKAGE_GROUP "System Environment/Base")
    set(CPACK_RPM_PACKAGE_REQUIRES "log4cplus >= 1.1.2, tpm2-tss >= 1.0, tpm2-tools >= 1.1.0, protobuf >= 2.4.1, re2 >= 20160401, libcurl >= 7.0.0, paccor, procps-ng >= 3.3.0")
    set(CPACK_RPM_POST_INSTALL_SCRIPT_FILE ${CMAKE_SOURCE_DIR}/package/rpm-post-install.sh)
    set(CPACK_RPM_EXCLUDE_FROM_AUTO_FILELIST_ADDITION /usr/local /usr/local/bin /usr/local/include /usr/local/lib)
    set(CPACK_PACKAGE_FILE_NAME "${CPACK_RPM_PACKAGE_NAME}-${CPACK_PACKAGE_VERSION}-${CPACK_PACKAGE_RELEASE}.${CPACK_RPM_PACKAGE_RELEASE_DIST}.${CMAKE_SYSTEM_PROCESSOR}")
endif()

# Set command to allow for running of CPack tool in build directory
include(CPack)

# Based on user-defined flag, optionally build tests for TPM 2.0 Library
if (BUILD_TESTS)
    enable_testing()
    add_subdirectory(test)
endif(BUILD_TESTS)
