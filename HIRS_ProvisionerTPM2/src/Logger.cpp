/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <Logger.h>

#include <log4cplus/loggingmacros.h>
#include <string>
#include <mutex>
#include <Properties.h>
#include <Utils.h>

using std::exception;
using std::string;
using std::stringstream;
using hirs::file_utils::fileExists;
using hirs::log::Logger;
using hirs::properties::Properties;

const char* const Logger::kDefaultProvisionerLoggerName = "tpm2_provisioner";
const char* const Logger::PROP_FILE_LOC = "/etc/hirs/logging.properties";

static std::once_flag configureRootLoggerOnce;

Logger::Logger(const string loggerName)
    : kLogger(log4cplus::Logger::getInstance(LOG4CPLUS_TEXT(loggerName))) {

    // set root logger threshold once (on first logger instantiation)
    std::call_once(configureRootLoggerOnce, [] {
        setThresholdFromLoggingProperties(log4cplus::Logger::getRoot());
    });

    // set logger threshold for actual logger
    setThresholdFromLoggingProperties(kLogger);
}

void Logger::setThresholdFromLoggingProperties(log4cplus::Logger logger) {
    // if logging.properties exists, attempt to set the
    // appropriate level for the given logger
    if (fileExists(PROP_FILE_LOC)) {
        Properties props(PROP_FILE_LOC);

        string loggerName = logger.getName();
        string logLevelKey = loggerName + ".level";
        if (props.isSet(logLevelKey)) {
            string level = props.get(logLevelKey);
            int l4cpLevel = log4cplus::getLogLevelManager().fromString(level);
            if (l4cpLevel != log4cplus::NOT_SET_LOG_LEVEL) {
                LOG4CPLUS_INFO(
                        logger,
                        LOG4CPLUS_TEXT(
                                "Configuring logger " + loggerName
                                + " with level " + level));
                logger.setLogLevel(l4cpLevel);
            } else {
                LOG4CPLUS_INFO(
                        logger,
                        LOG4CPLUS_TEXT(
                            "Unable to configure logger " + loggerName
                            + " with level " + level
                            + "; no such level found."));
            }
        }
    }
}

void Logger::log(const LogLevel& logLevel, const string& msg,
                 const exception* ex = nullptr) const {
    stringstream logMsg;
    if (ex != nullptr) {
        logMsg << ex->what() << ": ";
    }

    switch (logLevel) {
        case LogLevel::DEBUG: {
            LOG4CPLUS_DEBUG(kLogger, LOG4CPLUS_TEXT(logMsg.str())
                    << LOG4CPLUS_TEXT(msg));
            break;
        }
        case LogLevel::ERROR: {
            LOG4CPLUS_ERROR(kLogger, LOG4CPLUS_TEXT(logMsg.str())
                    << LOG4CPLUS_TEXT(msg));
            break;
        }
        case LogLevel::FATAL: {
            LOG4CPLUS_FATAL(kLogger, LOG4CPLUS_TEXT(logMsg.str())
                    << LOG4CPLUS_TEXT(msg));
            break;
        }
        case LogLevel::INFO: {
            LOG4CPLUS_INFO(kLogger, LOG4CPLUS_TEXT(logMsg.str())
                    << LOG4CPLUS_TEXT(msg));
            break;
        }
        case LogLevel::TRACE: {
            LOG4CPLUS_TRACE(kLogger, LOG4CPLUS_TEXT(logMsg.str())
                    << LOG4CPLUS_TEXT(msg));
            break;
        }
        case LogLevel::WARN: {
            LOG4CPLUS_WARN(kLogger, LOG4CPLUS_TEXT(logMsg.str())
                    << LOG4CPLUS_TEXT(msg));
            break;
        }
    }
}

Logger Logger::getDefaultLogger() {
    return getLogger(kDefaultProvisionerLoggerName);
}

Logger Logger::getLogger(const string& loggerName) {
    return Logger(loggerName);
}

void Logger::debug(const string& msg) const {
    log(LogLevel::DEBUG, msg);
}

void Logger::debug(const string& msg, const exception* ex) const {
    log(LogLevel::DEBUG, msg, ex);
}

void Logger::error(const string& msg) const {
    log(LogLevel::ERROR, msg);
}

void Logger::error(const string& msg, const exception* ex) const {
    log(LogLevel::ERROR, msg, ex);
}

void Logger::fatal(const string& msg) const {
    log(LogLevel::FATAL, msg);
}

void Logger::fatal(const string& msg, const exception* ex) const {
    log(LogLevel::FATAL, msg, ex);
}

void Logger::info(const string& msg) const {
    log(LogLevel::INFO, msg);
}

void Logger::info(const string& msg, const exception* ex) const {
    log(LogLevel::INFO, msg, ex);
}

void Logger::trace(const string& msg) const {
    log(LogLevel::TRACE, msg);
}

void Logger::trace(const string& msg, const exception* ex) const {
    log(LogLevel::TRACE, msg, ex);
}

void Logger::warn(const string& msg) const {
    log(LogLevel::WARN, msg);
}

void Logger::warn(const string& msg, const exception* ex) const {
    log(LogLevel::WARN, msg, ex);
}
