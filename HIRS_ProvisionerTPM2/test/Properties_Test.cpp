/**
 * Copyright (C) 2017-2018, U.S. Government
 */

#include "Properties.h"

#include <fstream>
#include <string>

#include "gtest/gtest.h"
#include "log4cplus/configurator.h"

#include "HirsRuntimeException.h"
#include "Utils.h"

using hirs::properties::Properties;

namespace {
class PropertiesTest : public ::testing::Test {
 protected:
        PropertiesTest() {
            // You can do set-up work for each test here.
            log4cplus::initialize();
            log4cplus::BasicConfigurator::doConfigure();
        }

        virtual ~PropertiesTest() {
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

const char kFileName[] = "/tmp/test_provisioner.properties";
const char kFileContents[] =
        "# a comment here\nk1=v1\nk2=v2 #this is a value\nk3= v 3 \nk4=\n";

TEST_F(PropertiesTest, EmptyConstructor) {
    Properties props;
    EXPECT_THROW(props.get("k1"), hirs::exception::HirsRuntimeException);
}

TEST_F(PropertiesTest, SetAndGet) {
    Properties props;
    props.set("k1", "v1");
    ASSERT_EQ(props.get("k1"), "v1");
}

TEST_F(PropertiesTest, SetTwiceAndGet) {
    Properties props;
    props.set("k1", "v1");
    ASSERT_EQ(props.get("k1"), "v1");
    props.set("k1", "v2");
    ASSERT_EQ(props.get("k1"), "v2");
}

TEST_F(PropertiesTest, GetUsingDefault) {
    Properties props;
    ASSERT_EQ(props.get("k1", "v1"), "v1");
    ASSERT_EQ(props.get("k1", ""), "");
    EXPECT_THROW(props.get("k1"), hirs::exception::HirsRuntimeException);
}

TEST_F(PropertiesTest, LoadAndGet) {
    if (hirs::file_utils::fileExists(kFileName)) {
        remove(kFileName);
    }
    std::ofstream propFile(kFileName);
    propFile << kFileContents;
    propFile.close();

    Properties props;
    props.load(kFileName);
    ASSERT_EQ(props.get("k1"), "v1");
    ASSERT_EQ(props.get("k2"), "v2");
    ASSERT_EQ(props.get("k3"), "v 3");
    ASSERT_FALSE(props.isSet("k4"));

    EXPECT_THROW(props.get("k4"), hirs::exception::HirsRuntimeException);
    remove(kFileName);
}

TEST_F(PropertiesTest, IsSet) {
    Properties props;
    props.set("k1", "v1");
    ASSERT_TRUE(props.isSet("k1"));
    ASSERT_FALSE(props.isSet("k2"));
    ASSERT_FALSE(props.isSet(""));
}

TEST_F(PropertiesTest, SetBlankKey) {
    Properties props;
    EXPECT_THROW(props.set(" ", "v1"), hirs::exception::HirsRuntimeException);
}

TEST_F(PropertiesTest, SetBlankValue) {
    Properties props;
    EXPECT_THROW(props.set("k1", " "), hirs::exception::HirsRuntimeException);
}

TEST_F(PropertiesTest, SetBlankKeyAndValue) {
    Properties props;
    EXPECT_THROW(props.set(" ", " "), hirs::exception::HirsRuntimeException);
}

}  // namespace

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}

