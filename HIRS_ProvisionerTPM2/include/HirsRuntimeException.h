/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#ifndef HIRS_PROVISIONERTPM2_INCLUDE_HIRSRUNTIMEEXCEPTION_H_
#define HIRS_PROVISIONERTPM2_INCLUDE_HIRSRUNTIMEEXCEPTION_H_

#include <stdexcept>
#include <string>

namespace hirs {
namespace exception {

/**
 * Represents a runtime exception thrown by HIRS code.
 */
class HirsRuntimeException : public std::runtime_error {
 private:
    static std::string buildMessage(const std::string& msg,
                                    const std::string& origin = "");

 public:
    HirsRuntimeException(const std::string& origin,
                  const std::string& msg);

    virtual ~HirsRuntimeException();
};

}  // namespace exception
}  // namespace hirs

#endif  // HIRS_PROVISIONERTPM2_INCLUDE_HIRSRUNTIMEEXCEPTION_H_
