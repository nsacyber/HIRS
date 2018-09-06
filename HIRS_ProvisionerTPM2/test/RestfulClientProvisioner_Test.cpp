/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include "gtest/gtest.h"
#include "RestfulClientProvisioner.h"

namespace {

class RestfulClientProvisionerTest : public :: testing::Test {
 protected:
    RestfulClientProvisionerTest() {
        // You can do set-up work for each test here.
        kRestfulClientProvisioner =
                new RestfulClientProvisioner("localhost", 8443);
    }

    virtual ~RestfulClientProvisionerTest() {
        // You can do clean-up work that doesn't throw exceptions here.
        delete kRestfulClientProvisioner;
        kRestfulClientProvisioner = NULL;
    }

    virtual void SetUp() {
        // Code here will be called immediately after the constructor (right
        // before each test).
    }

    virtual void TearDown() {
        // Code here will be called immediately after each test (right
        // before the destructor).
    }

    // Objects declared here can be used by all tests in the test case for
    // RestfulClientProvisioner.
    static RestfulClientProvisioner* kRestfulClientProvisioner;

    static const char kAcaTestAddress[];
};

RestfulClientProvisioner* RestfulClientProvisionerTest::
        kRestfulClientProvisioner = NULL;
const char RestfulClientProvisionerTest::kAcaTestAddress[] = "localhost";

TEST_F(RestfulClientProvisionerTest, GetAcaAddress) {
    ASSERT_STREQ(kAcaTestAddress,
                 kRestfulClientProvisioner->getAcaAddress().c_str());
}

}  // namespace

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
