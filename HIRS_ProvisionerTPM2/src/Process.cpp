/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <Process.h>
#include <Logger.h>
#include <HirsRuntimeException.h>
#include <Utils.h>

#include <arpa/inet.h>
#include <dirent.h>

#include <cstdio>
#include <cerrno>
#include <cstring>

#include <sstream>
#include <iostream>
#include <string>
#include <utility>

using hirs::exception::HirsRuntimeException;
using hirs::log::Logger;
using hirs::utils::Process;
using hirs::file_utils::dirExists;
using hirs::file_utils::fileExists;
using hirs::file_utils::fileToString;
using std::cerr;
using std::endl;
using std::ostream;
using std::stringstream;
using std::string;
using std::to_string;

const Logger Process::LOGGER = Logger::getDefaultLogger();

const char* const Process::kProcessDirectory = "/proc";
const char* const Process::kStatFilename = "stat";
const char* const Process::kCmdlineFilename = "cmdline";

/**
 * Constructor.
 * @param executable the executable to be run
 * @param arguments the arguments including options to be passed to the
 * executable (defaults to empty string)
 */
Process::Process(const string& executable, const string& arguments)
    : executable(executable), arguments(arguments) {
}

/**
 * Run the command and return the return value of the process.
 * @return the return value of the Linux process (between 0-255)
 */
int Process::run() {
    fflush(nullptr);  // Flush output streams of preexisting buffered output.
    stringstream commandStringStream;
    commandStringStream << executable;
    if (!arguments.empty()) {
        commandStringStream << " " << arguments;
    }

    string command = commandStringStream.str();
    LOGGER.info("Executing command: " + command);
    FILE* f = popen(command.c_str(), "re");
    if (f == nullptr) {
        stringstream errStream;
        errStream << "Unable to open output stream from command \""
                  << command << "\":" << strerror(errno) << endl;
        LOGGER.error(errStream.str());
        return -1;
    }
    int c;
    stringstream ss;
    while ((c = getc(f)) != EOF) {
        ss << static_cast<char>(c);
    }

    output = ss.str();

    // Linux return values are 0-255 even though pclose() returns a 32-bit int
    uint16_t retValAsBigEndian = static_cast<uint16_t>(pclose(f));
    return ntohs(retValAsBigEndian);
}

/**
 * Run the command and return the return value of the process.
 * @param osForErrorLogging ostream to collect error message on failure
 * @return the return value of the Linux process (between 0-255)
 */
int Process::run(ostream& osForErrorLogging) {
    int processReturnValue = run();
    if (processReturnValue != 0) {
        osForErrorLogging << "Call to " << executable
                          << " returned " << processReturnValue << endl;
    }

    if (processReturnValue == 127) {
        osForErrorLogging << "Is " << executable << " in your path?" << endl;
    }
    return processReturnValue;
}

/**
 * Return a string containing the standard output stream of the Linux process.
 * @return a string containing the standard output stream of the Linux process
 */
string Process::getOutputString() const {
    return output;
}

/**
 * Static function for calling a process that must succeed or throw
 * a HirsRuntimeException. This function is meant to be used with the
 * RUN_PROCESS_OR_THROW macro in order to capture the source file name
 * and source file line number for use in the exception message.
 *
 * @param executable the executable to be run
 * @param sourceFileName source file from which this method was called
 * @param sourceLineNumber line number of source file from which this method
 *                         was called
 * @param arguments the arguments including options to be passed to the
 * executable (defaults to empty string)
 */
string Process::run(const string& executable,
                    string sourceFileName,
                    int sourceLineNumber,
                    const string& arguments) {
    stringstream errorStream;
    Process p(executable, arguments);
    if (p.run(errorStream) != 0) {
        errorStream << "\n\n"
                    << "Process Output: "
                    << p.getOutputString();
        throw HirsRuntimeException(errorStream.str(),
                    sourceFileName + ": " + to_string(sourceLineNumber));
    }

    // Remove trailing newline if one exists
    string str = p.getOutputString();
    if (!str.empty() && str[str.length() - 1] == '\n') {
        str.erase(str.length() - 1);
    }
    return str;
}

/**
 * Static function to check if a specified process is currently running in the
 * local environment.
 *
 * @param executable the executable to check is running
 * @param checkCmdline a flag to check the complete cmdline commands
 *  rather than just search for the process name, default false
 * @return true, if executable is found to be running / false, otherwise
 */
bool Process::isRunning(const string& executable, bool checkCmdline) {
    // If the executable is the empty string, return false
    if (executable.empty()) {
        return false;
    }

    DIR* processDir;
    if (!dirExists(kProcessDirectory)) {
        stringstream errorStream;
        errorStream << "The " << kProcessDirectory
                    << " directory could not be found. "
                    << "Please ensure this is running "
                    << "on a supported Unix environment.";
        throw HirsRuntimeException(errorStream.str(), "Process::isRunning");
    } else {
        processDir = opendir(kProcessDirectory);
    }

    struct dirent* entry;
    char* endptr;
    while ((entry = readdir(processDir)) != nullptr) {
        // Attempt to parse current directory entry into a PID
        int64_t pid = strtol(entry->d_name, &endptr, 10);
        // Current entry can be skipped since it wasn't purely numeric
        if (*endptr != '\0') {
            continue;
        }

        // Check current process for desired executable
        stringstream filename;
        filename << kProcessDirectory << "/"
                 << to_string(pid) << "/";
        if (checkCmdline) {
            filename << kCmdlineFilename;
        } else {
            filename << kStatFilename;
        }
        string cmd;
        try {
            cmd = fileToString(filename.str());
        } catch (HirsRuntimeException& hirsRuntimeException) {
            // Process terminated between parsing the PID
            // and reading its stat file. Can skip as a result.
            continue;
        }
        // Desired executable was found, so it's running, return true
        if (cmd.find(executable) != string::npos) {
            closedir(processDir);
            return true;
        }
    }
    // The target executable is not a running process, return false
    closedir(processDir);
    return false;
}
