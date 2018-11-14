/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <Utils.h>
#include <HirsRuntimeException.h>

#include <re2/re2.h>

#include <sys/stat.h>
#include <sstream>
#include <iomanip>
#include <fstream>
#include <iostream>
#include <algorithm>
#include <cctype>
#include <string>
#include <vector>

using std::hex;
using std::ifstream;
using std::ios;
using std::ofstream;
using std::remove;
using std::setfill;
using std::setw;
using std::string;
using std::stringstream;
using std::vector;

using hirs::exception::HirsRuntimeException;

namespace hirs {

namespace json_utils {

string JSONFieldParser::parseJsonStringField(const std::string &jsonObject,
                                            const std::string &jsonFieldName) {
    stringstream regexPatternStream;
    regexPatternStream << "(?i)\\\""
                       << jsonFieldName
                       << "\\\"\\s*:\\s*\\\"(.*)\\\"";

    string value;
    if (RE2::PartialMatch(jsonObject, regexPatternStream.str(), &value)) {
        return value;
    } else {
        return "";
    }
}

}  // namespace json_utils

namespace file_utils {

    /**
     * Returns whether or not the argument directory exists.
     * @return True if the directory exists. False, otherwise.
     */
    bool dirExists(const string& path) {
        struct stat pathinfo;

        if (stat(path.c_str(), &pathinfo) != 0) {
            return false;
        } else {
            return (pathinfo.st_mode & S_IFDIR) != 0;
        }
    }

    /**
     * Returns whether or not the argument file name exists and is a
     * regular file.
     * @param filename file to check for existence
     * @return True if the file exists. False, otherwise.
     */
    bool fileExists(const string& filename) {
        struct stat buffer;

        if (stat(filename.c_str(), &buffer) != 0) {
            return false;
        } else {
            return S_ISREG(buffer.st_mode);
        }
    }

    string fileToString(const string& filename) {
        stringstream ss;
        ifstream t(filename);
        if (!t.good()) {
            stringstream ss;
            ss << "Unable to open file: " << filename;
            throw HirsRuntimeException(ss.str(),
                                       "Utils.cpp::file_utils::fileToString");
        }
        ss << t.rdbuf();
        t.close();
        return ss.str();
    }

    /**
    * Reads the contents of an entire file into a string. If
    * the file can't be opened, the default value is returned.
    * @param filename the name and path of the file to read.
    * @param defaultVal the value to return if the file can't be
    * opened.
    * @return the contents of the specified file as a string.
    */
    string fileToString(const string& filename, const string& defaultVal) {
        stringstream ss;
        ifstream t(filename);
        if (!t.good()) {
            return defaultVal;
        }
        ss << t.rdbuf();
        t.close();
        return ss.str();
    }

    string getFileAsOneLineOrEmptyString(const string& filename) {
        return string_utils::trimNewLines(fileToString(filename, ""));
    }

    /**
     * Takes a byte string and writes the contents to a file of the given name.
     * @param bytes string bytes to write
     * @param filename file name to be written to
     */
    void writeBinaryFile(const string& bytes, const string& filename) {
        ofstream file(filename, ios::out | ios::binary);
        if (file.is_open()) {
            file.write(bytes.c_str(), bytes.size());
            file.close();
        } else {
            throw std::invalid_argument("Cannot write to specified file");
        }
    }

    int getFileSize(const string& filename) {
        std::ifstream in(filename, std::ifstream::ate | std::ifstream::binary);
        return in.tellg();
    }

    // copy portion of file to temp file
    void splitFile(const string& inFilename, const string& outFilename,
                   int startPos, int readSize) {
        ifstream inStr(inFilename, ifstream::binary);
        ofstream outStr(outFilename, ofstream::binary);

        inStr.seekg(startPos);
        char* fileBlock = new char[readSize];

        inStr.read(fileBlock, readSize);
        outStr.write(fileBlock, readSize);

        inStr.close();
        outStr.close();

        delete[] fileBlock;
    }
}  // namespace file_utils

namespace string_utils {

    string binaryToHex(const string& bin) {
        stringstream output;
        for (int i = 0; i < 20; i++) {
            stringstream str;
            str << hex << setfill('0') << setw(2) << static_cast<int>(bin[i]);
            string dig = str.str();
            if (dig.length() > 2) {
                dig = dig.substr(dig.length() - 2, 2);
            }
            output << dig;
        }
        return output.str();
    }

    bool contains(const string& str, const string& substring) {
        return str.find(substring) != string::npos;
    }

    string longToHex(const uint32_t& value) {
        stringstream output;
        output << "0x" << hex << value;
        return output.str();
    }

    bool isHexString(const string& str) {
        bool isHexStringFlag = !str.empty();
        if (isHexStringFlag) {
            auto startIndex = str.begin();
            if (str.substr(0, 2) == "0x") {
                startIndex += 2;
            }
            isHexStringFlag = std::all_of(startIndex, str.end(), isxdigit);
        }
        return isHexStringFlag;
    }

    string hexToBytes(const string& hexString) {
        // if the string has an odd number of chars, return an empty string
        if (!isHexString(hexString) || hexString.size() % 2 != 0) {
            return {};
        }

        vector<uint8_t> bytes;
        for (uint32_t i = 0; i < hexString.length(); i += 2) {
            string byteString = hexString.substr(i, 2);
            uint8_t byte = static_cast<uint8_t>(strtol(byteString.c_str(),
                                                       nullptr, 16));
            bytes.push_back(byte);
        }

        return string(bytes.begin(), bytes.end());
    }

    uint32_t hexToLong(const string& hexString) {
        uint32_t value;
        stringstream conversionStream;
        conversionStream << hexString;
        conversionStream >> hex >> value;
        return value;
    }

    string trimNewLines(string str) {
        return trimChar(str, '\n');
    }

    string trimQuotes(string str) {
        return trimChar(str, '\"');
    }

    string trimChar(string str, char targetChar) {
        str.erase(remove(str.begin(), str.end(), targetChar), str.end());
        return str;
    }

    string trimWhitespaceFromLeft(string str) {
        RE2 pattern("^\\s+");
        while (RE2::PartialMatch(str, pattern)) {
            str = str.erase(0, 1);
        }
        return str;
    }

    string trimWhitespaceFromRight(string str) {
        RE2 pattern("\\s+$");
        while (RE2::PartialMatch(str, pattern)) {
            str = str.erase(str.length()-1, 1);
        }
        return str;
    }

    string trimWhitespaceFromBothEnds(string str) {
        return trimWhitespaceFromRight(trimWhitespaceFromLeft(str));
    }

}  // namespace string_utils

}  // namespace hirs
