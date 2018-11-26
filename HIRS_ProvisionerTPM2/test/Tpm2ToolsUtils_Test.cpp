/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <fstream>
#include <iostream>
#include <string>
#include <sstream>
#include <vector>
#include "gtest/gtest.h"

#include "Tpm2ToolsUtils.h"

using std::string;
using std::stringstream;

using hirs::tpm2_tools_utils::Tpm2ToolsOutputParser;

namespace {

class Tpm2ToolsUtilsTest : public :: testing::Test {
 protected:
    Tpm2ToolsUtilsTest() {
        // You can do set-up work for each test here.
    }

    virtual ~Tpm2ToolsUtilsTest() {
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

TEST_F(Tpm2ToolsUtilsTest, ParseNvDataSizeSuccess) {
    stringstream nvListOutput;
    nvListOutput << "2 NV indexes defined.\n"
                 << "\n"
                 << "  0. NV Index: 0x1800001\n"
                 << "  {\n"
                 << "\tHash algorithm(nameAlg):11\n"
                 << "\tThe Index attributes(attributes):0x62042c04\n"
                 << "\tThe size of the data area(dataSize):70\n"
                 << "  }\n"
                 << "\n"
                 << "  1. NV Index: 0x1c00002\n"
                 << "  {\n"
                 << "\tHash algorithm(nameAlg):11\n"
                 << "\tThe Index attributes(attributes):0x620f1001\n"
                 << "\tThe size of the data area(dataSize):991\n"
                 << "  }\n";


    uint16_t addressSize = Tpm2ToolsOutputParser::parseNvDataSize(
            "0x1c00002", nvListOutput.str());
    ASSERT_EQ(991, addressSize);
}

TEST_F(Tpm2ToolsUtilsTest, ParseNvDataSizeSuccessTpm2ToolsV3) {
    stringstream nvListOutput;
    nvListOutput << "0x1c00002\n"
                 << "\thash algorithm:\n"
                 << "\t\tfriendly: sha256\n"
                 << "\t\tvalue: 0xB\n"
                 << "\tattributes:\n"
                 << "\t\tfriendly: ownerwrite|policywrite\n"
                 << "\t\tvalue: 0xA000220\n"
                 << "\tsize: 991\n\n"
                 << "0x1c00003\n"
                 << "\thash algorithm:\n"
                 << "\t\tfriendly: sha256\n"
                 << "\t\tvalue: 0xB\n"
                 << "\tattributes:\n"
                 << "\t\tfriendly: ownerwrite|policywrite\n"
                 << "\t\tvalue: 0xA000220\n"
                 << "\tsize: 1722\n\n";


    uint16_t addressSize = Tpm2ToolsOutputParser::parseNvDataSize(
            "0x1c00002", nvListOutput.str());
    ASSERT_EQ(991, addressSize);
}

TEST_F(Tpm2ToolsUtilsTest, ParseNvDataSizeFailure) {
    stringstream nvListOutput;
    nvListOutput << "0 NV indexes defined.\n";

    uint16_t addressSize = Tpm2ToolsOutputParser::parseNvDataSize(
            "0x1c00002", nvListOutput.str());
    ASSERT_EQ(0, addressSize);
}

TEST_F(Tpm2ToolsUtilsTest, ParseNvReadSuccess) {
    stringstream nvReadOutput;
    nvReadOutput << "The size of data:10\n"
                 << " 30 7f 03 6d 30 7f 03 7e 3c 03";

    string nvReadData = Tpm2ToolsOutputParser::parseNvReadOutput(
            nvReadOutput.str());
    string expectedOutput = {48, 127, 3, 109, 48, 127, 3, 126, 60, 3};
    ASSERT_EQ(expectedOutput, nvReadData);
}

TEST_F(Tpm2ToolsUtilsTest, ParseNvReadFailure) {
    stringstream nvReadOutput;
    nvReadOutput << "Failed to read NVRAM area at index 0x1c00001 "
                 << "(29360129).Error:0x28b\n";

    string nvReadData = Tpm2ToolsOutputParser::parseNvReadOutput(
            nvReadOutput.str());
    ASSERT_EQ("", nvReadData);
}

TEST_F(Tpm2ToolsUtilsTest, ParsePersistentObjectExistsSuccess) {
    stringstream listPersistentOutput;
    listPersistentOutput << "1 persistent objects defined.\n"
                         << "\n"
                         << "  0. Persistent handle: 0x81010001\n"
                         << "  {\n"
                         << "\tType: 0x1\n"
                         << "\tHash algorithm(nameAlg): 0xb\n"
                         << "\tAttributes: 0x300b2\n"
                         << "  }\n";

    ASSERT_TRUE(Tpm2ToolsOutputParser::parsePersistentObjectExists(
            "0x81010001", listPersistentOutput.str()));
}

TEST_F(Tpm2ToolsUtilsTest, ParsePersistentObjectExistsSuccessTpm2ToolsV3) {
    stringstream listPersistentOutput;
    listPersistentOutput << "persistent-handle[0]:0x81010001 "
                         << "key-alg:rsa hash-alg:sha256 "
                         << "object-attr:fixedtpm|fixedparent";

    ASSERT_TRUE(Tpm2ToolsOutputParser::parsePersistentObjectExists(
            "0x81010001", listPersistentOutput.str()));
}

TEST_F(Tpm2ToolsUtilsTest, ParsePersistentObjectExistsFailure) {
    stringstream listPersistentOutput;
    listPersistentOutput << "0 persistent objects defined.\n";

    ASSERT_FALSE(Tpm2ToolsOutputParser::parsePersistentObjectExists(
            "0x81010001", listPersistentOutput.str()));
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpmErrorCodeSuccessAnyCharBetweenErrorAndCode) {
    stringstream errorOutput;
    errorOutput << "Create Object Failed ! ErrorCode: 0x922";

    string expectedOutput = "0x922";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpmErrorCodeSuccessHexChars) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:0x28b";

    string expectedOutput = "0x28b";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpmErrorCodeSuccessFirstThreeHex) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:0x28b90210";

    string expectedOutput = "0x28b";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpmErrorCodeSuccessMultiline) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:\n\n0x28b";

    string expectedOutput = "0x28b";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpmErrorCodeSuccessCapitalHex) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:\n\n0x28B";

    string expectedOutput = "0x28B";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpmErrorCodeFailNonHex) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:0x28g";

    string expectedOutput = "";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpmErrorCodeFailNonHexFormatted) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:28b";

    string expectedOutput = "";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpmErrorCodeFailNotErrorCode) {
    stringstream errorOutput;
    errorOutput << "Easter Egg to be found at memory address: 0x042";

    string expectedOutput = "";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpm2ToolsVersionSuccess) {
    stringstream versionOutput;
    versionOutput << R"(tool="tpm2_rc_decode" version="3.0.1")"
                  << R"(tctis="tabrmd,socket,device,")";

    string expectedOutput = "3.0.1";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpm2ToolsVersionSuccessCaseInsensitive) {
    stringstream versionOutput;
    versionOutput << R"(tool="tpm2_rc_decode" VeRSion="3.0.1")"
                  << R"(tctis="tabrmd,socket,device,")";

    string expectedOutput = "3.0.1";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpm2ToolsVersionSuccessWhitespace) {
    stringstream versionOutput;
    versionOutput << "tpm2_rc_decode, version 1.1.0";

    string expectedOutput = "1.1.0";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpm2ToolsVersionSuccessMultiNumeralVersion) {
    stringstream versionOutput;
    versionOutput << "tpm2_rc_decode, version 10.29.970";

    string expectedOutput = "10.29.970";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpm2ToolsVersionSuccessAnyCharsBeforeVersion) {
    stringstream versionOutput;
    versionOutput << "tpm2_rc_decode, version!@#$%^&*()+=-_|1.2.9";

    string expectedOutput = "1.2.9";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpm2ToolsVersionFailNonSemanticVersion) {
    stringstream versionOutput;
    versionOutput << "tpm2_rc_decode, version 1.2";

    string expectedOutput = "";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpm2ToolsMajorVersionSuccess) {
    stringstream versionOutput;
    versionOutput << "3.0.1";

    string expectedOutput = "3";
    string majorVersion = Tpm2ToolsOutputParser::parseTpm2ToolsMajorVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, majorVersion);
}

TEST_F(Tpm2ToolsUtilsTest,
       ParseTpm2ToolsMajorVersionSuccessMultiNumeralVersion) {
    stringstream versionOutput;
    versionOutput << "10.29.970";

    string expectedOutput = "10";
    string majorVersion = Tpm2ToolsOutputParser::parseTpm2ToolsMajorVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, majorVersion);
}

TEST_F(Tpm2ToolsUtilsTest, ParseTpm2ToolsMajorVersionFailNonSemanticVersion) {
    stringstream versionOutput;
    versionOutput << "3.0";

    string expectedOutput = "";
    string majorVersion = Tpm2ToolsOutputParser::parseTpm2ToolsMajorVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, majorVersion);
}

TEST_F(Tpm2ToolsUtilsTest,
       ParseTpm2ToolsMajorVersionFailLongNonSemanticVersion) {
    stringstream versionOutput;
    versionOutput << "3.0.1.27";

    string expectedOutput = "";
    string majorVersion = Tpm2ToolsOutputParser::parseTpm2ToolsMajorVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, majorVersion);
}

}  // namespace

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
