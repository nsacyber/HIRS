Developer Dependencies
======================

These are the dependencies currently used by the TPM2 Provisioner project that must be supplied by the development environment (in this case CentOS 7) in order to properly build and package the project.

Please look up their respective names in the CentOS repositories.

If no available repository for the development environment contains the dependencies at an acceptable version level, it is expected that the packages be retrieved and built from their respective source repositories.

| Dependency        | Version used | Minimum required   | Repository            | Project repository                          |
| ----------------- | ------------ | ------------------ | --------------------- | --------------------------------------      |
| cppcheck          | 1.80         | 1.72               | CentOS 7 epel-release | http://cppcheck.sourceforge.net/            |
| doxygen           | 1.8.13       | 1.8.0 (estimated)  | CentOS 7 base         | https://github.com/doxygen/doxygen          |
| graphviz          | 2.30.1       | 2.28.0 (estimated) | CentOS 7 base         | https://gitlab.com/graphviz/graphviz        |
| gcc-c++           | 4.8.5        | 4.8.5              | CentOS 7 base         | https://gcc.gnu.org/                        |
| libcurl-devel     | 7.29.0       | 7.0.0 (estimated)  | CentOS 7 base         | https://github.com/curl/curl                |
| libssh2-devel     | 1.4.3        | 1.4.3 (estimated)  | CentOS 7 base         | https://github.com/libssh2/libssh2          |
| log4cplus-devel   | 1.1.3        | 1.1.2              | CentOS 7 epel-release | https://github.com/log4cplus/log4cplus      |
| openssl-devel     | 1.0.2k       | 1.0.2g (estimated) | CentOS 7 base         | https://github.com/openssl/openssl          |
| protobuf-compiler | 2.5.0        | 2.4.1 (estimated)  | CentOS 7 base         | https://github.com/google/protobuf          |
| protobuf-devel    | 2.5.0        | 2.4.1 (estimated)  | CentOS 7 base         | https://github.com/google/protobuf          |
| re2-devel         | 20160401     | 20160201           | CentOS 7 epel-release | https://github.com/google/re2               |
| tpm2-tss-devel    | 1.2.0        | 1.0.0              | CentOS 7 base         | https://github.com/intel/tpm2-tss           |
| tpm2-abrmd-devel  | 1.1.0        | 1.1.0              | CentOS 7 base         | https://github.com/tpm2-software/tpm2-abrmd |
| cmake             | 2.8.12.2     | 2.6.0 (estimated)  | CentOS 7 base         | https://cmake.org/                          |
| cpack             | 2.8.12.2     | 2.6.0 (estimated)  | CentOS 7 base         | https://cmake.org/                          |
| git               | 1.8.3.1      | 1.6.0 (estimated)  | CentOS 7 base         | https://github.com/git/git                  |
