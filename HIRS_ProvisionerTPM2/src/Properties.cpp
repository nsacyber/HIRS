/**
 * Copyright (C) 2017-2018, U.S. Government
 */

#include "Properties.h"
#include "Utils.h"
#include <HirsRuntimeException.h>
#include <sstream>
#include <utility>
#include <iostream>
#include <string>
#include <fstream>
#include <Logger.h>

using hirs::exception::HirsRuntimeException;
using hirs::log::Logger;
using std::string;
using hirs::file_utils::fileExists;
using hirs::string_utils::trimWhitespaceFromLeft;
using hirs::string_utils::trimWhitespaceFromRight;
using hirs::string_utils::trimWhitespaceFromBothEnds;

const Logger hirs::properties::Properties::LOGGER = Logger::getDefaultLogger();

/**
 * This is a class that holds key-value configuration properties using a map.
 * Properties can be dynamically set and retrieved using the available methods,
 * or the class can load properties from a file with the constructor or 'load'
 * method.  Properties files used with this class should be of the following form:
 *
 * # comment lines begin with a '#'
 * some.key=some.value
 * some.other.key=some.other.value # inline comments are also allowed
 *
 * For each line, a key is the content to the left of the line's first equals sign,
 * and a key's associated value is the content to the right of the first equals sign.
 * Subsequent equals signs, if any, will be part of the value.
 * Left-bounding and right-bounding whitespace will be trimmed from both keys and values, but any interior whitespace will be preserved.
 * Keys and values are both case-sensitive.
 * There may be only one pair of key and value per line.
 * Keys that have a blank/empty string for a value will not be set.
 * The current implementation does not provide for wrapping keys or values onto proceeding lines.
 */
namespace hirs {
namespace properties {

        /**
         * Construct an empty Properties mapping with no keys or values.
         */
        Properties::Properties() {}

        /**
         * Construct a Properties mapping from a file path, pointing to a readable properties file
         * file whose format conforms to the description given above.
         *
         * @param filepath path to a properties file
         */
        Properties::Properties(const string& filepath) {
            load(filepath);
        }

        /**
         * Load keys and values from a readable properties file whose format conforms to the description given above.
         * If a key contained in the file is already set in this instance, its value will be updated
         * to reflect its value in the file.  Keys contained in this instance but not present in the file
         * will remain set.
         *
         * @param filepath path to a properties file
         */
        void Properties::load(const string& filepath) {
            if (!fileExists(filepath)) {
                throw HirsRuntimeException(
                        "Can't load properties from file: " + filepath,
                        "Properties.cpp::properties::load");
            }

            std::ifstream f(filepath);
            string line;
            while (std::getline(f, line)) {
                // trim whitespace from left
                line = trimWhitespaceFromLeft(line);

                // remove leading and inline comments
                std::size_t hash_index = line.find('#');
                if (hash_index != string::npos) {
                    line.erase(hash_index, string::npos);
                }

                // add new key, val pair, separated by first occurrence of '='
                std::size_t eq_index = line.find('=');
                if (eq_index != string::npos) {
                    string key = trimWhitespaceFromBothEnds(
                            line.substr(0, eq_index));
                    string value = trimWhitespaceFromBothEnds(
                            line.substr(eq_index+1));

                    // ensure that both key and value are not blank
                    if (key.length() > 0 && value.length() > 0) {
                        set(key, value);
                    }
                }
            }

            f.close();
        }

        /**
         * Retrieve the value associated with the given key.  If no such key exists in these properties,
         * this method will throw a HirsRuntimeException.
         *
         * @param key
         * @return the key's associated value
         */
        string Properties::get(const string& key) {
            if (!isSet(key)) {
                throw HirsRuntimeException(
                        "No such key: " + key,
                        "Properties.cpp::properties::get");
            } else {
                return properties.at(key);
            }
        }

        /**
         * Retrieve the value associated with the given key.  If no such key exists in these properties,
         * this method will return the given default value.
         *
         * @param key
         * @return the key's associated value, or the given default if no value is set
         */
        string Properties::get(const string& key, const string& defaultValue) {
            if (!isSet(key)) {
                LOGGER.warn("No such key " + key +
                            " found in properties; returning default: " +
                            defaultValue);
                return defaultValue;
            } else {
                return properties.at(key);
            }
        }

        /**
         * Returns true if there is a value set for the given key.
         *
         * @param key the key to check
         * @return true if the key has an associated value, false if no value has been set for the given key
         */
        bool Properties::isSet(const string& key) {
            return properties.count(key) > 0;
        }

        /**
         * Set the given key to the given value.  Leading and trailing whitespace, if any, are trimmed from both the key and value.
         * Attempting to set an empty key or value will result in this method throwing a
         * HirsRuntimeException.
         *
         * @param key the property key to set
         * @param value the property value to associate with the given key
         */
        void Properties::set(const string& key, const string& value) {
            string trimmedKey = trimWhitespaceFromBothEnds(key);
            string trimmedValue = trimWhitespaceFromBothEnds(value);

            if (trimmedKey.length() == 0 || trimmedValue.length() == 0) {
                throw HirsRuntimeException(
                        "Cannot insert blank key (" + trimmedKey
                        + ") or value (" + value + ")",
                        "Properties.cpp::properties::set");
            }

            properties[trimmedKey] = trimmedValue;
        }
    }  // namespace properties
}  // namespace hirs
