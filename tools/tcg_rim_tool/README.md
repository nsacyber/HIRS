**Note**: The tcg_rim_tool is being phased out in favor of the [RIM-Tool](https://github.com/nsacyber/RIM-Tool) launched in February 2026. Please refer to the RIM-Tool for future plans. 

To support the [TCG RIM concept](https://trustedcomputinggroup.org/resource/tcg-reference-integrity-manifest-rim-information-model/) a new command line application called the The tcg_rim_tool has been created. 
The tcg_rim_tool can be used to create NISTIR 8060 compatible SWID tags that adhere to the [TCG PC Client RIM specification](https://trustedcomputinggroup.org/resource/tcg-pc-client-reference-integrity-manifest-specification/).
It also supports the ability to digitally sign the Base RIM file as the HIRS ACA will require a valid signature in order to upload any RIM file.

# Building
## Linux
To build this tool navigate to the tcg_eventlog-tool directory and use the following command: 
> ./gradlew clean build

## Windows 
Several options exist for building on Windows 11:

1. Windows command shell (CMD.exe):
   *  Navigate to the tcg_eventlog_tool folder and run the widows gradle wrapper:
   >  gradlew.bat clean build
2. Windows powershell with Windows Subsystem for Linux enabled. 
   *  Navigate to the tcg_eventlog_tool folder and run the Linux gradle wrapper:
   > ./gradlew clean build

In both cases the tcg_rim_tool-X.X.jar file should have been placed in the build\libs\tools\ (Windows) or build/libs/tools/ (Linux) folder.

# Packaging
Packages for this tool can be found on the [HIRS release page](https://github.com/nsacyber/HIRS/release

Currently only a packaging for Linux is supported.

To create an RPM package on a Redhat or Rocky linux device use the following command in the same directory:
> ./gradlew buildRpm

or for a Debian or Ubuntu Linux distro:
> ./gradlew buildDeb 

the package can be found under the build/distributions/ folder

# Installing
Currently only a install packages for Linux are supported. 

To install this tool on a Redhat or Rocky Linux distro use the following command from the same directory:
> sudo  dnf install build/distributions/tcg_eventlog_tool*.rpm

or for a Debian or Ubuntu Linux distro:
> sudo  apt-get install build/distributions/tcg_eventlog_tool*.deb

# Usage

The tcg_rim_tool rpm will create a rim commandline shortcut. This can be invoked from a command line:
> rim -h

The tcg_eventlog_tool also can be invoked using java from the tcg_eventlog_tool directory:

> java -jar build/libs/tools/tcg_rim_tool-1.0.jar -h

Current options for the tool can be found using the -h option.
