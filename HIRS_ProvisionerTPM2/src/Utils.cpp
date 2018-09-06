/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <Utils.h>
#include <HirsRuntimeException.h>
#include <Process.h>

#include <sys/stat.h>
#include <sstream>
#include <iomanip>
#include <fstream>
#include <iostream>
#include <algorithm>
#include <cctype>
#include <string>
#include <stdexcept>
#include <unordered_map>
#include <vector>

using std::hex;
using std::ifstream;
using std::ios;
using std::ofstream;
using std::out_of_range;
using std::setfill;
using std::setw;
using std::string;
using std::stringstream;
using std::unordered_map;
using std::vector;

using hirs::exception::HirsRuntimeException;

namespace hirs {

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
        str.erase(std::remove(str.begin(), str.end(), '\n'), str.end());
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

namespace tpm2_tools_utils {

const unordered_map<string, Tpm2ToolsVersion>
        Tpm2ToolsVersionChecker::kVersionMap = {
        {"1.1.0", Tpm2ToolsVersion::VERSION_1_1_0 },
        {"3.0.1", Tpm2ToolsVersion::VERSION_3_0_1 }
};

Tpm2ToolsVersion Tpm2ToolsVersionChecker::findTpm2ToolsVersion() {
    string versionOutput = RUN_PROCESS_OR_THROW("tpm2_rc_decode", "-v");
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput);

    try {
        return kVersionMap.at(version);
    }
    catch (const out_of_range& oor) {
        stringstream ss;
        ss << "Unsupported Tpm2 Tools Version Detected: " << version;
        throw HirsRuntimeException(ss.str(),
                         "Tpm2ToolsVersionChecker::findTpm2ToolsVersion");
    }
}


uint16_t Tpm2ToolsOutputParser::parseNvDataSize(const string &nvHandle,
                                                const string &nvListOutput) {
    stringstream regexPatternStream;
    regexPatternStream << nvHandle
                       << "(?:.*\\n)+?"
                       << "(?i).*size\\S*:\\s*([0-9]+)";

    uint16_t dataSize;
    if (RE2::PartialMatch(nvListOutput, regexPatternStream.str(), &dataSize)) {
        return dataSize;
    } else {
        return 0;
    }
}

string Tpm2ToolsOutputParser::parseNvReadOutput(const string &nvReadOutput) {
    stringstream regexPatternStream;
    regexPatternStream << ".*\\n*The size of data:[0-9]+";

    string byteString = nvReadOutput;
    // Remove tpm2_nvlist header
    int numReplacements = RE2::GlobalReplace(&byteString,
                                             regexPatternStream.str(), "");
    if (numReplacements != 0) {
        // Remove any non-hexadecimal characters
        RE2::GlobalReplace(&byteString, "[^0-9A-Fa-f]+", "");
        return string_utils::hexToBytes(byteString);
    } else {
        return "";
    }
}

bool Tpm2ToolsOutputParser::parsePersistentObjectExists(
        const std::string &handle,
        const std::string &listPersistentOutput) {
    stringstream regexPatternStream;
    regexPatternStream << "(?i)Persistent.*handle.*:\\s*"
                       << handle;
    return RE2::PartialMatch(listPersistentOutput, regexPatternStream.str());
}

string Tpm2ToolsOutputParser::parseTpmErrorCode(const string& toolOutput) {
    stringstream regexPatternStream;
    regexPatternStream << "(?i)Error.*:\\s*(0x[0-9a-fA-F]{3})";

    string errorCode;
    if (RE2::PartialMatch(toolOutput, regexPatternStream.str(), &errorCode)) {
        return errorCode;
    } else {
        return "";
    }
}

string Tpm2ToolsOutputParser::parseTpm2ToolsVersion(const string& toolOutput) {
    stringstream regexPatternStream;
    regexPatternStream << "(?i)version[^0-9]*([0-9]+\\.[0-9]+\\.[0-9]+).*";

    string version;
    if (RE2::PartialMatch(toolOutput, regexPatternStream.str(), &version)) {
        return version;
    } else {
        return "";
    }
}

}  // namespace tpm2_tools_utils

}  // namespace hirs
