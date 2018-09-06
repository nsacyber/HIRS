/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <HirsRuntimeException.h>

#include <sstream>
#include <string>

using hirs::exception::HirsRuntimeException;
using std::endl;
using std::string;
using std::stringstream;

HirsRuntimeException::HirsRuntimeException(const string& msg,
                                           const string& origin)
        : runtime_error(buildMessage(msg, origin)) {}

HirsRuntimeException::~HirsRuntimeException() = default;

string HirsRuntimeException::buildMessage(const string& msg,
                                   const string& origin) {
    stringstream headerStream;
    if (!origin.empty()) {
        headerStream << "<" << origin << ">: ";
    }

    stringstream msgStream;
    msgStream << headerStream.str() << msg << endl;

    return msgStream.str();
}
