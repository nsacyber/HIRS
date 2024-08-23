# HIRS TPM 2.0 Provisioner

Notice: The HIRS TPM 2.0 Provisioner is being deprecated. 
Please refer to the [HIRS_Provisioner.Net](https://github.com/nsacyber/HIRS/tree/main/HIRS_Provisioner.NET) for currently supported HIRS provisioner. 

### Overview

This document describes the HIRS TPM 2.0 Provisioner, a program that can leverage a machine and its TPM to:
- verify system attributes (as chosen in the ACA policy)
- request and store an Attestation Identity Credential

See the top-level project documentation for more details.

### Requirements

**Development and runtime of this project is currently only supported on CentOS 7.**

This project is built with the CMake cross-platform build suite.  Consult the developer dependencies in [docs/](docs/) for a list of all third-party software that should be installed before attempting to the compile the project.  Additional dependencies will be downloaded and built by CMake, so an active Internet connection is required to properly build the project.

Python 2 is required for style checking. If you do not have Python 2 installed, either install it or set the `STYLE_CHECK` option to `OFF` as part of your CMake command or in the root `CMakeLists.txt` file.

This project uses cppcheck to provide static code analysis. If you do not wish to run this analysis, set the `STATIC_ANALYSIS` option to `OFF` as part of your CMake Command or in the root `CMakeLists.txt` file.

### Building

Before you begin, please ensure you have the prerequisite dependencies installed on your system (listed in docs/developer-dependencies-centos.md).

Two procedures for building the HIRS TPM 2.0 Provisioner with CMake are described below.

#### Building with the CLion IDE

1. Import the root directory as a project into the [CLion](https://www.jetbrains.com/clion/) IDE.
2. Click `Tools` > `CMake` > `Reset Cache and Reload Project`
3. Click `Run` > `Build`

#### Building on the CLI

1. Navigate to the root of the project directory.
2. Make a build folder.
3. Navigate into the build folder.
4. Run the following command to generate the appropriate make files:
```
cmake ../
```
5. Run the following command to build the executable in the `bin` directory of the build folder:
```
make
```

By default, the build will gather additional third-party dependencies, run the unit test suite, run static analsysis with cppcheck, and will generate code documentation (which is placed in the `./docs` directory.)

#### Troubleshooting build issues
- CMake will fetch additional third-party dependencies during the build.  The build will fail if these cannot be retrieved, so please ensure you have an active Internet connection before building.
- If it is found that CMake is building in an unusual/undesired directory, it's likely that CMake is using a cached target directory in lieu of an implicit target. At this point, look around the local project for a CMakeCache.txt file and delete it to force a cache refresh.

### RPM Packaging

The only currently supported target runtime environment is CentOS7.

The CMakeLists is configured to package the project into an RPM using CPack.  To build the RPM, navigate to the target build directory and run the following command:
```
cpack
```

This will create the CentOS 7 RPM.

**NOTE:** Packaging of the software for a given distribution should be done in the same environment as the target environment. Due to host-system specific, compilation-time targeting of certain system libraries and APIs, cross-platform compilation is not advised and could lead to package installation errors.

### Installing

Ensure that the third-party runtime dependencies are present on the target machine(s).  These can be found in [/docs/developer-depedencies-centos.md](/docs/developer-depedencies-centos.md).  If installing these via yum or another package manager that performs dependency resolution, the EPEL repository will need to be configured, as several of the Provisioner's dependencies are not in the base CentOS 7 repository.  The Provisioner RPM can be transferred and installed on client machines via the usual mechanisms (rpm/yum/etc.)
