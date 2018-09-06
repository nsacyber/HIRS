/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include "Process.h"

#include <iostream>
#include <sstream>
#include <string>

#include "log4cplus/configurator.h"
#include "gtest/gtest.h"

namespace {

class ProcessTest : public :: testing::Test {
 protected:
    ProcessTest() {
        // You can do set-up work for each test here.
        log4cplus::initialize();
        log4cplus::BasicConfigurator::doConfigure();
    }

    virtual ~ProcessTest() {
        // You can do clean-up work that doesn't throw exceptions here.
    }

    virtual void SetUp() {
        // Code here will be called immediately after the constructor (right
        // before each test).
    }

    virtual void TearDown() {
        // Code here will be called immediately after each test (right
        // before the destructor).
    }
};

TEST_F(ProcessTest, ProcessWorks) {
    hirs::utils::Process p("echo \"Hello World\"");
    int retVal = p.run();
    ASSERT_EQ(retVal, 0);
    ASSERT_EQ("Hello World\n", p.getOutputString());
}

TEST_F(ProcessTest, ProcessTwoArgConstructorWorks) {
    hirs::utils::Process p("echo", "\"Hello World\"");
    int retVal = p.run();
    ASSERT_EQ(retVal, 0);
    ASSERT_EQ("Hello World\n", p.getOutputString());
}

TEST_F(ProcessTest, ProcessFailsWithNonZeroReturnValue) {
    hirs::utils::Process p("ls", "isjlfidjsaij");
    int retVal = p.run();
    ASSERT_EQ(retVal, 2);
}

TEST_F(ProcessTest, NonExistentProcessFailsWithNonZeroReturnValue) {
    hirs::utils::Process p("isjlfidjsaij");
    int retVal = p.run();
    ASSERT_EQ(retVal, 127);
}

TEST_F(ProcessTest, NonExistentProcessFailsAndGivesErrorMessage) {
    hirs::utils::Process p("isjlfidjsaij", "ijijdfi");
    std::stringstream expectedError;
    expectedError << "Call to isjlfidjsaij returned 127" << std::endl
                  << "Is isjlfidjsaij in your path?" << std::endl;
    std::string expectedErrorString(expectedError.str());

    std::stringstream errorStream;
    int retVal = p.run(errorStream);
    ASSERT_EQ(retVal, 127);
    std::string receivedErrorString(errorStream.str());
    ASSERT_EQ(receivedErrorString, expectedErrorString);
}

TEST_F(ProcessTest, SuccessfulProcessDoesNotProduceErrorMessage) {
        hirs::utils::Process p("echo", "\"Hello World\"");

    std::stringstream errorStream;
    int retVal = p.run(errorStream);
    ASSERT_EQ(retVal, 0);
    std::string receivedErrorString(errorStream.str());
    ASSERT_TRUE(receivedErrorString.empty());
}

}  // namespace

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
