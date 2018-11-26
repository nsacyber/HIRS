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
                           std::string sourceFileName,
                           int sourceLineNumber,
                           const std::string& arguments = "");

    static bool isRunning(const std::string& executable);
};

}  // namespace utils
}  // namespace hirs


#define RUN_PROCESS_OR_THROW(executable, arguments)\
    hirs::utils::Process::run(executable, __FILE__, __LINE__, arguments)

#endif  // HIRS_PROVISIONERTPM2_INCLUDE_PROCESS_H_
