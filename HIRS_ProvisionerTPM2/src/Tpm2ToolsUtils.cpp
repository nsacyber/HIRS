/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <Tpm2ToolsUtils.h>
#include <HirsRuntimeException.h>
#include <Process.h>
#include <Utils.h>

#include <re2/re2.h>

#include <fstream>
#include <iomanip>
#include <sstream>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include <vector>

using std::ifstream;
using std::out_of_range;
using std::string;
using std::stringstream;
using std::unordered_map;
using std::vector;

using hirs::exception::HirsRuntimeException;

namespace hirs {

namespace tpm2_tools_utils {

const unordered_map<string, Tpm2ToolsVersion>
        Tpm2ToolsVersionChecker::kVersionMap = {
        {"1.1.0", Tpm2ToolsVersion::VERSION_1_1_0 },
        {"2.1.0", Tpm2ToolsVersion::VERSION_2_1_0 },
        {"3", Tpm2ToolsVersion::VERSION_3 }
};

const unordered_map<string, Tpm2ToolsVersion>
        Tpm2ToolsVersionChecker::kMaxSupportedVersionMap = {
        {"Ubuntu 17.10", Tpm2ToolsVersion::VERSION_1_1_0 },
        {"Ubuntu 18.04", Tpm2ToolsVersion::VERSION_2_1_0 },
        {"Ubuntu 18.10", Tpm2ToolsVersion::VERSION_2_1_0 },
        {"CentOS Linux 7", Tpm2ToolsVersion::VERSION_3 }
};

Tpm2ToolsVersion Tpm2ToolsVersionChecker::findTpm2ToolsVersion() {
    string versionOutput = RUN_PROCESS_OR_THROW("tpm2_nvlist", "-v");
    string version = Tpm2ToolsOutputParser::parseTpm2ToolsVersion(
            versionOutput);
    string majorVersion = Tpm2ToolsOutputParser::parseTpm2ToolsMajorVersion(
            version);

    if (!version.empty()) {
        try {
            // Look to see if tpm2-tools major version is supported
            return kVersionMap.at(majorVersion);
        }
        catch (const out_of_range& oor) {
            // If major version not supported, then look for specific version
            try {
                return kVersionMap.at(version);
            }
            catch (const out_of_range& oor) {
                // If no version found, version is unsupported, throw exception
                stringstream ss;
                ss << "Unsupported Tpm2 Tools Version Detected: " << version;
                throw HirsRuntimeException(ss.str(),
                            "Tpm2ToolsVersionChecker::findTpm2ToolsVersion");
            }
        }
    } else {
        // If version check returns empty, instead of throwing exception,
        // then tpm2-tools is installed but version lookup is faulty.
        // Get current runtime environment distribution.
        string currentDistribution = getDistribution();
        try {
            // Look to see if current distribution has a supported version
            // and use that as best guess at version number
            return kMaxSupportedVersionMap.at(currentDistribution);
        } catch (const out_of_range& oor) {
            stringstream ss;
            ss << "Unsupported Distribution Detected: " << currentDistribution;
            throw HirsRuntimeException(ss.str(),
                        "Tpm2ToolsVersionChecker::findTpm2ToolsVersion");
        }
    }
}

string Tpm2ToolsVersionChecker::getDistribution() {
    stringstream completeDistro;
    string distribution;
    string distributionRelease;
    ifstream releaseFile;
    string line;
    releaseFile.open("/etc/os-release");
    if (releaseFile.is_open()) {
        while (getline(releaseFile, line)) {
            stringstream ss(line);
            string item;
            vector<string> tokens;
            while (getline(ss, item, '=')) {
                tokens.push_back(item);
            }
            if (!tokens.empty() && tokens.at(0) == "NAME") {
                distribution = string_utils::trimQuotes(tokens.at(1));
            } else if (!tokens.empty() && tokens.at(0) == "VERSION_ID") {
                distributionRelease = string_utils::trimQuotes(tokens.at(1));
            }
        }
        completeDistro << distribution << " " << distributionRelease;
        releaseFile.close();
    }
    return completeDistro.str();
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

string Tpm2ToolsOutputParser::parseTpm2ToolsMajorVersion(
        const string& toolVersion) {
    stringstream regexPatternStream;
    regexPatternStream << "^([0-9]+)\\.[0-9]+\\.[0-9]+$";

    string majorVersion;
    if (RE2::PartialMatch(toolVersion, regexPatternStream.str(),
                          &majorVersion)) {
        return majorVersion;
    } else {
        return "";
    }
}

}  // namespace tpm2_tools_utils
}  // namespace hirs
