Dependencies
============

These are the dependencies currently used by the TPM2 Provisioner that must be supplied by the runtime environment (in this case CentOS 7) and which are not statically linked.

Please look up their respective names in the CentOS repositories.

**NOTE**: Please consult [the developer dependency list](./developer-dependencies-centos.md) and make sure both the regular lib and the devel libs are installed.

| Dependency | Version used | Minimum required  | Repository required   | Project repository                          |
| ---------- | ------------ | ----------------- | --------------------- | ------------------------------------------- |
| log4cplus  | 1.1.2        | 1.1.2             | CentOS 7 epel-release | https://github.com/log4cplus/log4cplus      |
| protobuf   | 2.5.0        | 2.4.1 (estimated) | CentOS 7 base         | https://github.com/google/protobuf          |
| re2        | 20160401     | 20160201          | CentOS 7 epel-release | https://github.com/google/re2               |
| tpm2-tss   | 1.2.0        | 1.0.0             | CentOS 7 base         | https://github.com/intel/tpm2-tss           |
| tpm2-tools | 1.1.0        | 1.1.0             | CentOS 7 base         | https://github.com/tpm2-software/tpm2-tools |
| paccor     | 1.0.6        | none              | N/A                   | https://github.com/nsacyber/paccor          |
| procps-ng  | 3.3.10       | 3.3.0             | CentOS 7 base         | https://gitlab.com/procps-ng/procps         |
