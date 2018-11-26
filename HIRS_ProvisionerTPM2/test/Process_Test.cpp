/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include "Process.h"

#include <iostream>
#include <sstream>
#include <string>

#include "log4cplus/configurator.h"
#include "gtest/gtest.h"

using hirs::utils::Process;
using std::string;
using std::stringstream;

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
    Process p("echo \"Hello World\"");
    int retVal = p.run();
    ASSERT_EQ(retVal, 0);
    ASSERT_EQ("Hello World\n", p.getOutputString());
}

TEST_F(ProcessTest, ProcessTwoArgConstructorWorks) {
    Process p("echo", "\"Hello World\"");
    int retVal = p.run();
    ASSERT_EQ(retVal, 0);
    ASSERT_EQ("Hello World\n", p.getOutputString());
}

TEST_F(ProcessTest, ProcessFailsWithNonZeroReturnValue) {
    Process p("ls", "isjlfidjsaij");
    int retVal = p.run();
    ASSERT_EQ(retVal, 2);
}

TEST_F(ProcessTest, NonExistentProcessFailsWithNonZeroReturnValue) {
    Process p("isjlfidjsaij");
    int retVal = p.run();
    ASSERT_EQ(retVal, 127);
}

TEST_F(ProcessTest, NonExistentProcessFailsAndGivesErrorMessage) {
    Process p("isjlfidjsaij", "ijijdfi");
    stringstream expectedError;
    expectedError << "Call to isjlfidjsaij returned 127" << std::endl
                  << "Is isjlfidjsaij in your path?" << std::endl;
    string expectedErrorString(expectedError.str());

    stringstream errorStream;
    int retVal = p.run(errorStream);
    ASSERT_EQ(retVal, 127);
    string receivedErrorString(errorStream.str());
    ASSERT_EQ(receivedErrorString, expectedErrorString);
}

TEST_F(ProcessTest, SuccessfulProcessDoesNotProduceErrorMessage) {
    Process p("echo", "\"Hello World\"");

    stringstream errorStream;
    int retVal = p.run(errorStream);
    ASSERT_EQ(retVal, 0);
    string receivedErrorString(errorStream.str());
    ASSERT_TRUE(receivedErrorString.empty());
}

TEST_F(ProcessTest, ProcessIsRunningSuccessful) {
    ASSERT_TRUE(Process::isRunning("Process"));
}

TEST_F(ProcessTest, ProcessIsRunningSuccessfulPathBased) {
    ASSERT_TRUE(Process::isRunning("/opt/Process"));
}

TEST_F(ProcessTest, ProcessIsRunningFalse) {
    ASSERT_FALSE(Process::isRunning("foobar"));
}

TEST_F(ProcessTest, ProcessIsRunningEmptyStringReturnsFalse) {
    ASSERT_FALSE(Process::isRunning(""));
}

TEST_F(ProcessTest, ProcessIsRunningPreventCommandHijack) {
    ASSERT_FALSE(Process::isRunning("foobar; echo blarg"));
}

}  // namespace

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
