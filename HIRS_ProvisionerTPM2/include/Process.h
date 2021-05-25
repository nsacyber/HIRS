/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#ifndef HIRS_PROVISIONERTPM2_INCLUDE_PROCESS_H_
#define HIRS_PROVISIONERTPM2_INCLUDE_PROCESS_H_

#include "Logger.h"

#include <string>
#include <iostream>

namespace hirs {
namespace utils {

/**
 * The Process class represents a Linux process, its return value,
 * and the standard output stream.
 */
class Process {
 private:
    static const hirs::log::Logger LOGGER;

    static const char* const kPgrepCommand;
    static const int kMaxStatFileProcessNameLength;

    std::string executable;

    std::string arguments;

    std::string output;

 public:
    explicit Process(const std::string& executable,
        const std::string& arguments = "");

    int run();

    int run(std::ostream& osForErrorLogging);

    std::string getOutputString() const;

    static std::string run(const std::string& executable,
                           const std::string& arguments,
                           const std::string& sourceFileName,
                           int sourceLineNumber);

    static std::string runData(const std::string& executable,
                           const std::string& arguments,
                           const std::string& sourceFileName,
                           int sourceLineNumber);

    static bool isRunning(const std::string& executable);
};

}  // namespace utils
}  // namespace hirs

#endif  // HIRS_PROVISIONERTPM2_INCLUDE_PROCESS_H_
