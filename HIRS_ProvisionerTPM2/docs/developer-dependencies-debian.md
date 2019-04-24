Developer Dependencies
======================

These are the dependencies currently used by the TPM2 Provisioner project that must be supplied by the development environment (in this case Debian-based) in order to properly build and package the project.

Please look up their respective names in the appropriate repositories.

If no available repository for the development environment contains the dependencies at an acceptable version level, it is expected that the packages be retrieved and built from their respective source repositories.

| Dependency           | Version used | Minimum required   | Repository            | Project repository                            |
| -------------------- | ------------ | ------------------ | --------------------- | --------------------------------------------- |
| cppcheck             | 1.82         | 1.72               | Ubuntu 18.04 base     | http://cppcheck.sourceforge.net/              |
| doxygen              | 1.8.13       | 1.8.0 (estimated)  | Ubuntu 18.04 base     | https://github.com/doxygen/doxygen            |
| graphviz             | 2.40.1       | 2.28.0 (estimated) | Ubuntu 18.04 base     | https://gitlab.com/graphviz/graphviz          |
| libcurl4-openssl-dev | 7.47.0       | 7.0.0 (estimated)  | Ubuntu 18.04 base     | https://github.com/curl/curl                  |
| liblog4cplus-dev     | 1.1.2        | 1.1.2              | Ubuntu 18.04 base     | https://github.com/log4cplus/log4cplus        |
| libssl-dev           | 1.1.0g       | 1.0.2g (estimated) | Ubuntu 18.04 base     | https://github.com/openssl/openssl            |
| protobuf-compiler    | 3.0.0        | 2.4.1 (estimated)  | Ubuntu 18.04 base     | https://github.com/google/protobuf            |
| libprotobuf-dev      | 3.0.0        | 2.4.1 (estimated)  | Ubuntu 18.04 base     | https://github.com/google/protobuf            |
| libre2-dev           | 20180201     | 20160201           | Ubuntu 18.04 base     | https://github.com/google/re2                 |
| libsapi-dev          | 1.0.0        | 1.0.0              | Ubuntu 18.04 base     | https://github.com/intel/tpm2-tss             |
| tpm2-tss             | 1.3.0        | 1.3.0              | Source Code           | https://github.com/tpm2-software/tpm2-tss     |
| tpm2-abrmd           | 1.3.1        | 1.3.1              | Source Code           | https://github.com/tpm2-software/tpm2-abrmd   |
| cmake                | 3.10.2       | 2.6.0 (estimated)  | Ubuntu 18.04 base     | https://cmake.org/                            |
| git                  | 2.17.1       | 1.6.0 (estimated)  | Ubuntu 18.04 base     | https://github.com/git/git                    |
