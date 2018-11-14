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
using hirs::json_utils::JSONFieldParser;
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

TEST_F(UtilsTest, ParseJsonFieldSuccess) {
    stringstream jsonObject;
        jsonObject << R"({"error":"identityClaim cannot be null or empty"})";

    string errorMessage = JSONFieldParser::parseJsonStringField(
            jsonObject.str(), "error");
    string expectedOutput = "identityClaim cannot be null or empty";
    ASSERT_EQ(expectedOutput, errorMessage);
}

TEST_F(UtilsTest, ParseJsonFieldSuccessCaseInsensitive) {
    stringstream jsonObject;
    jsonObject << R"({"ERROR":"identityClaim cannot be null or empty"})";

    string errorMessage = JSONFieldParser::parseJsonStringField(
            jsonObject.str(), "error");
    string expectedOutput = "identityClaim cannot be null or empty";
    ASSERT_EQ(expectedOutput, errorMessage);
}

TEST_F(UtilsTest, ParseJsonFieldSuccessWhiteSpaces) {
    stringstream jsonObject;
    jsonObject << R"({"error"  :  "identityClaim cannot be null or empty"})";

    string errorMessage = JSONFieldParser::parseJsonStringField(
            jsonObject.str(), "error");
    string expectedOutput = "identityClaim cannot be null or empty";
    ASSERT_EQ(expectedOutput, errorMessage);
}

TEST_F(UtilsTest, ParseJsonFieldSuccessMultiJsonFields) {
    stringstream jsonObject;
    jsonObject << R"({"error"  :  "identityClaim cannot be null or empty",)"
                 << "\n" << R"("endpoint":"url.com"})";

    string errorMessage = JSONFieldParser::parseJsonStringField(
            jsonObject.str(), "error");
    string expectedOutput = "identityClaim cannot be null or empty";
    ASSERT_EQ(expectedOutput, errorMessage);
}

TEST_F(UtilsTest, ParseJsonFieldInvalidJson) {
    stringstream jsonObject;
    jsonObject << R"({error:"identityClaim cannot be null or empty"})";

    string errorMessage = JSONFieldParser::parseJsonStringField(
            jsonObject.str(), "error");
    string expectedOutput = "";
    ASSERT_EQ(expectedOutput, errorMessage);
}

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

}  // namespace

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
