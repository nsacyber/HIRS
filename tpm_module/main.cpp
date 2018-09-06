/**
 * When compiling in Linux define LINUX. When compiling in Windows define
 * WINDOWS. This software is designed to be compiled on either platform without
 * modification. Simply set the preprocessor definitions and the code will take
 * care of the rest.
 */

/**
 * This software was originally implemented as a static library. However,
 * requirements changed and it resumed existing as a command line-driven
 * executable. The libhis header files are designed such that you could easily
 * break them out into their own library. They do not require inclusion of TCG
 * headers or the use of TCG data structures. All input is handled with standard
 * C++ types.
 */

#include "libhis_cli.hpp"

#ifdef LINUX
int main(int argc, char **argv)
#endif
#ifdef WINDOWS
unsigned long main(int argc, char **argv)
#endif
{
    //provide all arguments to our controller class
    libhis_cli test(argc, argv);

    //return the integer result from our controller class's execution function
    return test.cli();
}
