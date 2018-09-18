/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <fstream>
#include <iostream>
#include <string>
#include <sstream>
#include <vector>
#include "gtest/gtest.h"

#include "Utils.h"

using hirs::file_utils::dirExists;
using hirs::file_utils::fileExists;
using hirs::string_utils::binaryToHex;
using hirs::string_utils::contains;
using hirs::string_utils::longToHex;
using hirs::string_utils::isHexString;
using hirs::string_utils::hexToBytes;
using hirs::string_utils::hexToLong;
using hirs::string_utils::trimNewLines;
using hirs::string_utils::trimQuotes;
using hirs::string_utils::trimChar;
using hirs::string_utils::trimWhitespaceFromLeft;
using hirs::string_utils::trimWhitespaceFromRight;
using hirs::string_utils::trimWhitespaceFromBothEnds;
using hirs::tpm2_tools_utils::Tpm2ToolsOutputParser;
using std::ofstream;
using std::string;
using std::stringstream;

namespace {

class UtilsTest : public :: testing::Test {
 protected:
    UtilsTest() {
        // You can do set-up work for each test here.
    }

    virtual ~UtilsTest() {
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

    // Objects declared here can be used by all tests in the test case for
    // Utils.
    static const char kFileName[];
};

const char UtilsTest::kFileName[] = "bitsAndBytes";

TEST_F(UtilsTest, DirectoryExists) {
    mkdir(kFileName, 0755);
    ASSERT_TRUE(dirExists(kFileName));
    rmdir(kFileName);
}

TEST_F(UtilsTest, DirectoryDoesNotExist) {
    ASSERT_FALSE(dirExists(kFileName));
}

TEST_F(UtilsTest, FileExists) {
    ofstream outputFile;
    outputFile.open(kFileName);
    outputFile.close();
    ASSERT_TRUE(fileExists(kFileName));
    remove(kFileName);
}

TEST_F(UtilsTest, FileDoesNotExist) {
    ASSERT_FALSE(fileExists(kFileName));
}

TEST_F(UtilsTest, FileSize) {
    string test = "Hello World";
    hirs::file_utils::writeBinaryFile(test, "testfile");
    int size = hirs::file_utils::getFileSize("testfile");
    ASSERT_EQ(size, 11);
}

TEST_F(UtilsTest, SplitFile) {
    string test = "Hello World";
    hirs::file_utils::writeBinaryFile(test, "testfile");
    hirs::file_utils::splitFile("testfile", "testfilep1", 0, 5);
    string s = hirs::file_utils::fileToString("testfilep1");
    ASSERT_EQ(s, "Hello");

    hirs::file_utils::splitFile("testfile", "testfilep2", 5, 5);
    s = hirs::file_utils::fileToString("testfilep2");
    ASSERT_EQ(s, " Worl");

    hirs::file_utils::splitFile("testfile", "testfilep3", 10, 1);
    s = hirs::file_utils::fileToString("testfilep3");
    ASSERT_EQ(s, "d");
}

TEST_F(UtilsTest, BinToHex) {
    const char* testBin = "j\223\255x\216=\330c\aaj\262@\343i\246?\204T5";
    ASSERT_EQ(binaryToHex(testBin),
              "6a93ad788e3dd86307616ab240e369a63f845435");
}

TEST_F(UtilsTest, Contains) {
    string teststr = "The more you know";
    string substr = "more you";
    ASSERT_TRUE(contains(teststr, substr));
}

TEST_F(UtilsTest, ContainsSelf) {
    string teststr = "The more you know";
    string substr = "The more you know";
    ASSERT_TRUE(contains(teststr, substr));
}

TEST_F(UtilsTest, DoesNotContain) {
    string teststr = "The more you know";
    string substr = "moor";
    ASSERT_FALSE(contains(teststr, substr));
}

TEST_F(UtilsTest, DoesNotContainMoreThanSelf) {
    string teststr = "The more you know";
    string substr = "The more you know.";
    ASSERT_FALSE(contains(teststr, substr));
}

TEST_F(UtilsTest, LongToHex) {
    const uint32_t testValue = 464367618;
    ASSERT_EQ(longToHex(testValue), "0x1badb002");
}

TEST_F(UtilsTest, LongToHexZero) {
    const uint32_t testValue = 0;
    ASSERT_EQ(longToHex(testValue), "0x0");
}

TEST_F(UtilsTest, LongToHexUnderflow) {
    const uint32_t testValue = -1;
    ASSERT_EQ(longToHex(testValue), "0xffffffff");
}

TEST_F(UtilsTest, LongToHexOverflow) {
    uint32_t testValue = 0xffffffff + 1;
    ASSERT_EQ(longToHex(testValue), "0x0");
}

TEST_F(UtilsTest, IsHexStringEmpty) {
    string testStr = "";
    ASSERT_FALSE(isHexString(testStr));
}

TEST_F(UtilsTest, IsHexStringTrue) {
    string testStr = "8BADF00D";
    ASSERT_TRUE(isHexString(testStr));
}

TEST_F(UtilsTest, IsHexStringPrefixTrue) {
    string testStr = "0x8BADF00D";
    ASSERT_TRUE(isHexString(testStr));
}

TEST_F(UtilsTest, IsHexStringFalse) {
    string testStr = "G00DF00D";
    ASSERT_FALSE(isHexString(testStr));
}

TEST_F(UtilsTest, IsHexStringFalseWithSpaces) {
    string testStr = "8BAD F00D";
    ASSERT_FALSE(isHexString(testStr));
}

TEST_F(UtilsTest, HexToBytesEmptyString) {
    string testStr = "";
    ASSERT_TRUE(hexToBytes(testStr).empty());
}

TEST_F(UtilsTest, HexToBytesNotHex) {
    string testStr = "A study in mopishness";
    ASSERT_TRUE(hexToBytes(testStr).empty());
}

TEST_F(UtilsTest, HexToBytesNotEven) {
    string testStr = "8BADF00";
    ASSERT_TRUE(hexToBytes(testStr).empty());
}

TEST_F(UtilsTest, HexToBytesSuccess) {
    // ASCII bytes for "TWO$"
    string testBytes = {84, 87, 79, 36};
    // Hex encoding of "TWO$"
    string testStr = "54574F24";
    ASSERT_EQ(testBytes, hexToBytes(testStr));
}

TEST_F(UtilsTest, HexToLong) {
    string testStr = "BADF00D";
    ASSERT_EQ(hexToLong(testStr), 195948557);
}

TEST_F(UtilsTest, HexWithPrefixToLong) {
    string testStr = "0xBADF00D";
    ASSERT_EQ(hexToLong(testStr), 195948557);
}

TEST_F(UtilsTest, HexToLongNotHex) {
    string testStr = "G00DF00D";
    ASSERT_EQ(hexToLong(testStr), 0);
}

TEST_F(UtilsTest, TrimNewLines) {
    string test = "abc\ndef\nghi\n";
    ASSERT_EQ(trimNewLines(test),
              "abcdefghi");
}

TEST_F(UtilsTest, TrimQuotes) {
    string test = "abc\"def\"ghi\"";
    ASSERT_EQ(trimQuotes(test),
              "abcdefghi");
}

TEST_F(UtilsTest, TrimChar) {
    string test = "abc@def@ghi@";
    ASSERT_EQ(trimChar(test, '@'),
              "abcdefghi");
}

TEST_F(UtilsTest, trimWhitespaceFromLeft) {
    ASSERT_EQ(trimWhitespaceFromLeft(" asdf"), "asdf");
    ASSERT_EQ(trimWhitespaceFromLeft("   as df"), "as df");
    ASSERT_EQ(trimWhitespaceFromLeft("\tas df"), "as df");
    ASSERT_EQ(trimWhitespaceFromLeft("\t\ras\rdf"), "as\rdf");
    ASSERT_EQ(trimWhitespaceFromLeft("asdf "), "asdf ");
    ASSERT_EQ(trimWhitespaceFromLeft("asdf"), "asdf");
    ASSERT_EQ(trimWhitespaceFromLeft(" "), "");
    ASSERT_EQ(trimWhitespaceFromLeft(""), "");
}

TEST_F(UtilsTest, trimWhitespaceFromRight) {
    ASSERT_EQ(trimWhitespaceFromRight("asdf "), "asdf");
    ASSERT_EQ(trimWhitespaceFromRight("as df    "), "as df");
    ASSERT_EQ(trimWhitespaceFromRight("as df\t"), "as df");
    ASSERT_EQ(trimWhitespaceFromRight("as\rdf\t\r"), "as\rdf");
    ASSERT_EQ(trimWhitespaceFromRight(" asdf"), " asdf");
    ASSERT_EQ(trimWhitespaceFromRight("asdf"), "asdf");
    ASSERT_EQ(trimWhitespaceFromRight(" "), "");
    ASSERT_EQ(trimWhitespaceFromRight(""), "");
}

TEST_F(UtilsTest, trimWhitespaceFromBoth) {
    ASSERT_EQ(trimWhitespaceFromBothEnds(" asdf "), "asdf");
    ASSERT_EQ(trimWhitespaceFromBothEnds("    as df    "), "as df");
    ASSERT_EQ(trimWhitespaceFromBothEnds("\tas df\t"), "as df");
    ASSERT_EQ(trimWhitespaceFromBothEnds("\t\ras\rdf\t\r"), "as\rdf");
    ASSERT_EQ(trimWhitespaceFromBothEnds("asdf"), "asdf");
    ASSERT_EQ(trimWhitespaceFromBothEnds(" "), "");
    ASSERT_EQ(trimWhitespaceFromBothEnds(""), "");

    ASSERT_EQ(trimWhitespaceFromBothEnds("asdf "), "asdf");
    ASSERT_EQ(trimWhitespaceFromBothEnds("as df    "), "as df");
    ASSERT_EQ(trimWhitespaceFromBothEnds("as df\t"), "as df");
    ASSERT_EQ(trimWhitespaceFromBothEnds("as\rdf\t\r"), "as\rdf");

    ASSERT_EQ(trimWhitespaceFromBothEnds(" asdf"), "asdf");
    ASSERT_EQ(trimWhitespaceFromBothEnds("   as df"), "as df");
    ASSERT_EQ(trimWhitespaceFromBothEnds("\tas df"), "as df");
    ASSERT_EQ(trimWhitespaceFromBothEnds("\t\ras\rdf"), "as\rdf");
}

TEST_F(UtilsTest, ParseNvDataSizeSuccess) {
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

TEST_F(UtilsTest, ParseNvDataSizeSuccessTpm2ToolsV3) {
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

TEST_F(UtilsTest, ParseNvDataSizeFailure) {
    stringstream nvListOutput;
    nvListOutput << "0 NV indexes defined.\n";

    uint16_t addressSize = Tpm2ToolsOutputParser::parseNvDataSize(
            "0x1c00002", nvListOutput.str());
    ASSERT_EQ(0, addressSize);
}

TEST_F(UtilsTest, ParseNvReadSuccess) {
    stringstream nvReadOutput;
    nvReadOutput << "The size of data:10\n"
                 << " 30 7f 03 6d 30 7f 03 7e 3c 03";

    string nvReadData = Tpm2ToolsOutputParser::parseNvReadOutput(
            nvReadOutput.str());
    string expectedOutput = {48, 127, 3, 109, 48, 127, 3, 126, 60, 3};
    ASSERT_EQ(expectedOutput, nvReadData);
}

TEST_F(UtilsTest, ParseNvReadFailure) {
    stringstream nvReadOutput;
    nvReadOutput << "Failed to read NVRAM area at index 0x1c00001 "
                 << "(29360129).Error:0x28b\n";

    string nvReadData = Tpm2ToolsOutputParser::parseNvReadOutput(
            nvReadOutput.str());
    ASSERT_EQ("", nvReadData);
}

TEST_F(UtilsTest, ParsePersistentObjectExistsSuccess) {
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

TEST_F(UtilsTest, ParsePersistentObjectExistsSuccessTpm2ToolsV3) {
    stringstream listPersistentOutput;
    listPersistentOutput << "persistent-handle[0]:0x81010001 "
                         << "key-alg:rsa hash-alg:sha256 "
                         << "object-attr:fixedtpm|fixedparent";

    ASSERT_TRUE(Tpm2ToolsOutputParser::parsePersistentObjectExists(
            "0x81010001", listPersistentOutput.str()));
}

TEST_F(UtilsTest, ParsePersistentObjectExistsFailure) {
    stringstream listPersistentOutput;
    listPersistentOutput << "0 persistent objects defined.\n";

    ASSERT_FALSE(Tpm2ToolsOutputParser::parsePersistentObjectExists(
            "0x81010001", listPersistentOutput.str()));
}

TEST_F(UtilsTest, ParseTpmErrorCodeSuccessAnyCharBetweenErrorAndCode) {
    stringstream errorOutput;
    errorOutput << "Create Object Failed ! ErrorCode: 0x922";

    string expectedOutput = "0x922";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(UtilsTest, ParseTpmErrorCodeSuccessHexChars) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:0x28b";

    string expectedOutput = "0x28b";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(UtilsTest, ParseTpmErrorCodeSuccessFirstThreeHex) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:0x28b90210";

    string expectedOutput = "0x28b";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(UtilsTest, ParseTpmErrorCodeSuccessMultiline) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:\n\n0x28b";

    string expectedOutput = "0x28b";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(UtilsTest, ParseTpmErrorCodeSuccessCapitalHex) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:\n\n0x28B";

    string expectedOutput = "0x28B";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(UtilsTest, ParseTpmErrorCodeFailNonHex) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:0x28g";

    string expectedOutput = "";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(UtilsTest, ParseTpmErrorCodeFailNonHexFormatted) {
    stringstream errorOutput;
    errorOutput << "Failed to read NVRAM area at index 0x1c00003"
                << " (29360131).Error:28b";

    string expectedOutput = "";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(UtilsTest, ParseTpmErrorCodeFailNotErrorCode) {
    stringstream errorOutput;
    errorOutput << "Easter Egg to be found at memory address: 0x042";

    string expectedOutput = "";
    string errorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(
            errorOutput.str());
    ASSERT_EQ(expectedOutput, errorCode);
}

TEST_F(UtilsTest, ParseTpm2ToolsVersionSuccess) {
    stringstream versionOutput;
    versionOutput << R"(tool="tpm2_rc_decode" version="3.0.1")"
                  << R"(tctis="tabrmd,socket,device,")";

    string expectedOutput = "3.0.1";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(UtilsTest, ParseTpm2ToolsVersionSuccessCaseInsensitive) {
    stringstream versionOutput;
    versionOutput << R"(tool="tpm2_rc_decode" VeRSion="3.0.1")"
                  << R"(tctis="tabrmd,socket,device,")";

    string expectedOutput = "3.0.1";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(UtilsTest, ParseTpm2ToolsVersionSuccessWhitespace) {
    stringstream versionOutput;
    versionOutput << "tpm2_rc_decode, version 1.1.0";

    string expectedOutput = "1.1.0";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(UtilsTest, ParseTpm2ToolsVersionSuccessMultiNumeralVersion) {
    stringstream versionOutput;
    versionOutput << "tpm2_rc_decode, version 10.29.970";

    string expectedOutput = "10.29.970";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(UtilsTest, ParseTpm2ToolsVersionSuccessAnyCharsBeforeVersion) {
    stringstream versionOutput;
    versionOutput << "tpm2_rc_decode, version!@#$%^&*()+=-_|1.2.9";

    string expectedOutput = "1.2.9";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

TEST_F(UtilsTest, ParseTpm2ToolsVersionFailNonSemanticVersion) {
    stringstream versionOutput;
    versionOutput << "tpm2_rc_decode, version 1.2";

    string expectedOutput = "";
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput.str());
    ASSERT_EQ(expectedOutput, version);
}

}  // namespace

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
