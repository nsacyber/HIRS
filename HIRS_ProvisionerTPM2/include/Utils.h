/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#ifndef HIRS_PROVISIONERTPM2_INCLUDE_UTILS_H_
#define HIRS_PROVISIONERTPM2_INCLUDE_UTILS_H_

#include <string>

namespace hirs {

/**
 * Namespace for describing file utility functions.
 */
namespace file_utils {
    /**
     * Returns whether or not the argument directory exists.
     * @return True if the directory exists. False, otherwise.
     */
    bool dirExists(const std::string& filename);

    bool fileExists(const std::string& filename);

    /**
    * Reads the contents of an entire file into a string.
    * @param filename the name and path of the file to read.
    * @return the contents of the specified file as a string.
    */
    std::string fileToString(const std::string& filename);

    std::string fileToString(const std::string& filename,
                             const std::string& defaultVal);

    std::string getFileAsOneLineOrEmptyString(const std::string& filename);

    void writeBinaryFile(const std::string& bytes,
                         const std::string& filename);

    int getFileSize(const std::string& filename);

    void splitFile(const std::string& inFilename,
                   const std::string& outFilename,
                   int startPos,
                   int readSize);

    std::string trimFilenameFromPath(const std::string& path);
}  // namespace file_utils

namespace json_utils {

/**
* Utility class that provides functions to parse information from ACA
* output.
*/
class JSONFieldParser {
 public:
    /**
     * Parses the target field of the provided JSON object as a string.
     *
     * @param jsonObject the JSON-formatted object
     * @param jsonFieldName the name of the field to parse from the JSON object
     * @return the value of the target field in the JSON object
     */
    static std::string parseJsonStringField(const std::string& jsonObject,
                                            const std::string& jsonFieldName);
};

}  // namespace json_utils

namespace string_utils {
    /**
     * Converts a binary string to a hex string.
     *
     * @param bin the binary character array to convert
     * @return hex string representation of bin
     */
    std::string binaryToHex(const std::string& bin);

    /**
     * Checks if a string contains another string.
     *
     * @param str containing string
     * @param substring string to search for
     * @return true, if the string is found / false, otherwise
     */
    bool contains(const std::string& str, const std::string& substring);

    /**
     * Converts an unsigned long (uint32) value to a hex string.
     *
     * @param value the unsigned long to convert
     * @return hex string representation of the value
     */
    std::string longToHex(const uint32_t& value);

    /**
     * Checks provided string contains only hexadecimal characters.
     *
     * @param str the string to check if hexadecimal only
     * @return true, if hex chars only / false, otherwise
     */
    bool isHexString(const std::string& str);

    /**
     * Converts a hex string to a string of bytes.
     * Requires the hex string to have an even number of chars.
     *
     * @param hexString string of hex chars to be converted to bytes
     * @return a byte string or, if error detected, empty string
     */
    std::string hexToBytes(const std::string& hexString);

    /**
     * Takes a given hex string and converts it to a long value.
     *
     * @param hexString the hex string to be converted
     * @return the value of the hex string when converted to a long value
     *  or 0 if an error occurred
     */
    uint32_t hexToLong(const std::string& hexString);

    /**
     * Removes any new line characters in the input string and returns the
     * pruned, input string.
     * @param str string to remove new line characters from.
     * @return str with new line characters removed.
     */
    std::string trimNewLines(std::string str);

    /**
     * Removes any double-quote characters in the input string and returns the
     * pruned, input string.
     * @param str string to remove double-quotes characters from.
     * @return str with double-quote characters removed.
     */
    std::string trimQuotes(std::string str);

    /**
     * Removes any occurrences of the target character in the input string and
     * returns the pruned, input string.
     * @param str string to characters from.
     * @param targetChar char to prune from the string
     * @return str with the characters removed.
     */
    std::string trimChar(std::string str, char targetChar);

    std::string trimWhitespaceFromLeft(std::string str);

    std::string trimWhitespaceFromRight(std::string str);

    std::string trimWhitespaceFromBothEnds(std::string str);
}  // namespace string_utils

}  // namespace hirs

#endif  // HIRS_PROVISIONERTPM2_INCLUDE_UTILS_H_
