/**
 * Copyright (C) 2017-2018, U.S. Government
 */

#ifndef HIRS_PROVISIONERTPM2_INCLUDE_PROPERTIES_H_
#define HIRS_PROVISIONERTPM2_INCLUDE_PROPERTIES_H_

#include <map>
#include <string>
#include "Logger.h"

namespace hirs {
namespace properties {

/**
 * Manages the loading and retrieval of key-value configuration.
 */
class Properties {
 private:
            static const hirs::log::Logger LOGGER;
            std::map<std::string, std::string> properties;

 public:
            Properties();

            explicit Properties(const std::string& filepath);

            void load(const std::string& filepath);

            std::string get(const std::string& key);

            std::string get(const std::string& key,
                            const std::string& defaultValue);

            bool isSet(const std::string& key);

            void set(const std::string& key, const std::string& value);
};
}  // namespace properties
}  // namespace hirs

#endif  // HIRS_PROVISIONERTPM2_INCLUDE_PROPERTIES_H_
