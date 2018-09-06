# CMake file for managing TPM 2.0 Library Test Suite
include(CTest)

# Setup for GoogleTest
configure_file(../lib/GoogleTest.CMakeLists.txt.in ${CMAKE_BINARY_DIR}/lib/googletest-download/CMakeLists.txt)
execute_process(COMMAND ${CMAKE_COMMAND} -G "${CMAKE_GENERATOR}" .
        RESULT_VARIABLE result
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/lib/googletest-download)
if(result)
    message(FATAL "CMake step for GoogleTest failed: ${result}")
endif()
execute_process(COMMAND ${CMAKE_COMMAND} --build .
        RESULT_VARIABLE result
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/lib/googletest-download )
if(result)
    message(FATAL "Build step for GoogleTest failed: ${result}")
endif()

# Prevent overriding the parent project's compiler/linker
# settings on Windows
if(MSVC)
    set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
endif()

# Add googletest directly to our build. This defines
# the gtest and gtest_main targets.
# Note the EXCLUDE_FROM_ALL here will prevent the test code from
# being linked into the executable.
add_subdirectory(${CMAKE_BINARY_DIR}/lib/googletest-src
        ${CMAKE_BINARY_DIR}/lib/googletest-build
        EXCLUDE_FROM_ALL)

macro(register_test TEST_TARGET_NAME)
    add_executable(${TEST_TARGET_NAME} ${TEST_TARGET_NAME}_Test.cpp)
    target_link_libraries(${TEST_TARGET_NAME}
            TPM2_PROVISIONER_LIBRARY
            gtest gmock)
    add_test(NAME ${TEST_TARGET_NAME}_Tests COMMAND $<TARGET_FILE:${TEST_TARGET_NAME}>)
    add_custom_command(
            TARGET ${TEST_TARGET_NAME}
            COMMENT "Run ${TEST_TARGET_NAME} Tests"
            POST_BUILD
            COMMAND ${TEST_TARGET_NAME}
    )
endmacro()

register_test(Process)
register_test(RestfulClientProvisioner)
register_test(Properties)
register_test(Utils)
