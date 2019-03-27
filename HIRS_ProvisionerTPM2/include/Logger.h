/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#ifndef HIRS_PROVISIONERTPM2_INCLUDE_LOGGER_H_
#define HIRS_PROVISIONERTPM2_INCLUDE_LOGGER_H_

#include <log4cplus/logger.h>
#include <string>
#include <exception>

namespace hirs {
namespace log {

/**
 * The Logger class provides a wrapper for log4cplus that allows for
 * getting standardized Loggers in the TPM 2 Provisioner Library.
 */
class Logger {
 private:
    static const char* const kDefaultProvisionerLoggerName;
    static const char* const kPropFileLocation;

    const log4cplus::Logger kLogger;

    explicit Logger(std::string loggerName);

    enum class LogLevel {
        DEBUG,
        ERROR,
        FATAL,
        INFO,
        TRACE,
        WARN
    };

    void log(const LogLevel& logLevel, const std::string& msg,
             const std::exception* ex) const;

    static void setThresholdFromLoggingProperties(log4cplus::Logger logger);

 public:
    static Logger getDefaultLogger();
    static Logger getLogger(const std::string& loggerName);

    void debug(const std::string& msg) const;
    void debug(const std::string& msg, const std::exception* ex) const;

    void error(const std::string& msg) const;
    void error(const std::string& msg, const std::exception* ex) const;

    void fatal(const std::string& msg) const;
    void fatal(const std::string& msg, const std::exception* ex) const;

    void info(const std::string& msg) const;
    void info(const std::string& msg, const std::exception* ex) const;

    void trace(const std::string& msg) const;
    void trace(const std::string& msg, const std::exception* ex) const;

    void warn(const std::string& msg) const;
    void warn(const std::string& msg, const std::exception* ex) const;
};

}  // namespace log
}  // namespace hirs

#endif  // HIRS_PROVISIONERTPM2_INCLUDE_LOGGER_H_
