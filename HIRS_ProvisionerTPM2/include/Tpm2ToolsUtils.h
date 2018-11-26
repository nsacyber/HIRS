/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#ifndef HIRS_PROVISIONERTPM2_INCLUDE_TPM2TOOLSUTILS_H_
#define HIRS_PROVISIONERTPM2_INCLUDE_TPM2TOOLSUTILS_H_

#include <string>
#include <unordered_map>

namespace hirs {

namespace tpm2_tools_utils {

/**
 * Enum class that provides list of supported tpm2_tools versions
 */
enum class Tpm2ToolsVersion {
    VERSION_1_1_0,
    VERSION_2_1_0,
    VERSION_3
};

/**
 * Utility class that determines the version of tpm2_tools present on the
 * system.
 */
class Tpm2ToolsVersionChecker {
 private:
    static const std::unordered_map<std::string, Tpm2ToolsVersion> kVersionMap;
    static const std::unordered_map<std::string,
            Tpm2ToolsVersion> kMaxSupportedVersionMap;

    static std::string getDistribution();

 public:
    /**
     * Makes a simple tpm2-tools call to determine the version available on the
     * current system.
     *
     * @return enum specifying the version of tpm2-tools available locally
     */
    static Tpm2ToolsVersion findTpm2ToolsVersion();
};


/**
 * Utility class that provides functions to parse information from tpm2_tools
 * output.
 */
class Tpm2ToolsOutputParser {
 public:
    /**
     * Parses the provided Non-volatile (NV) Index List for the data size of
     * the index at the prescribed NV handle. Expects output from tpm2_nvlist.
     *
     * @param nvHandle memory address to search the nvListOutput for
     * @param nvListOutput the NV Index List to search for the data size
     * @return the size of the desired NV index or 0 if the index isn't found
     */
    static uint16_t parseNvDataSize(const std::string& nvHandle,
                                    const std::string& nvListOutput);

    /**
     * Pulls the data out of the output from a tpm2_nvread command.
     *
     * @param nvReadOutput the output from a call to tpm2_nvread
     * @return the data serialized as bytes, or an empty string if
     * nvReadOutput improperly formatted
     */
    static std::string parseNvReadOutput(const std::string& nvReadOutput);

    /**
     * Parses the provided Persistent Object List for the provided handle.
     *
     * @param handle memory address to search the nvListPersistentOutput for
     * @param listPersistentOutput the Persistent Object list to search
     * @return true, if handle is found / false, otherwise
     */
    static bool parsePersistentObjectExists(const std::string& handle,
                                     const std::string& listPersistentOutput);

    /**
     * Parses the provided tpm2-tool output for a TPM Error Code.
     *
     * @param toolOutput the output from a call to any of the tpm2-tools
     * @return a TPM error code if found, or an empty string, otherwise
     */
    static std::string parseTpmErrorCode(const std::string& toolOutput);

    /**
     * Parses the provided tpm2-tool output for a tpm2_tools version.
     *
     * @param toolOutput the output from a call to any of the tpm2-tools
     * @return a tpm2_tools version if found, or an empty string, otherwise
     */
    static std::string parseTpm2ToolsVersion(const std::string& toolOutput);

    /**
     * Parses the provided tpm2-tool version for the major version.
     *
     * @param toolVersion the output from a call to parseTpm2ToolsVersion
     * @return tpm2_tools major version if found, or an empty string, otherwise
     */
    static std::string parseTpm2ToolsMajorVersion(
            const std::string& toolVersion);
};

}  // namespace tpm2_tools_utils

}  // namespace hirs

#endif  // HIRS_PROVISIONERTPM2_INCLUDE_TPM2TOOLSUTILS_H_
