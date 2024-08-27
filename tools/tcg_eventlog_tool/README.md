The tcg_eventlog_tool is a command line application that allows a user to inspect the Trusted Platform Module (TPM) Event Log's contents. This command tool supports the [PC Client RIM Specification](https://trustedcomputinggroup.org/resource/tcg-pc-client-reference-integrity-manifest-specification/), which specifies the use of the TPM Event Log as a Support RIM type. This tool can be used to parse and print human readable output, provide hexadecimal events which can be used as test patterns, and provide details in the case of events failing comparison. 

Note that a TPM Event Log will only be populated on a given device if the device:
1. Utilizes TCG compliant UEFI Firmware
2. Has a TPM 2.0
3. Has a TPM aware OS (true for most flavors of Linux and Windows)

The default locations for the TPM Event Log are:
* Windows:  C:\Windows\Logs\MeasuredBoot\
* Linux:    /sys/kernel/security/tpm0/    with a default name of "binary_bios_measurements"

# Building

## Linux
To build this tool, navigate to the tcg_eventlog_tool directory and use the following command: 
> ./gradlew clean build

## Windows 
Several options exist for building on Windows 11:

1. Windows command shell (CMD.exe):
   *  Navigate to the tcg_eventlog_tool folder and run the Windows gradle wrapper:
   >  gradlew.bat clean build
2. Windows powershell with Windows Subsystem for Linux enabled:
   *  Navigate to the tcg_eventlog_tool folder and run the Linux gradle wrapper:
   > ./gradlew clean build

The tcg_eventlog_tool-X.X.jar file should populate in the build\libs\ (Windows) or build/libs/tools/ (Linux) folder.

# Packaging
Packages for this tool can be found on the [HIRS release page](https://github.com/nsacyber/HIRS/releases). Download the RPM files which apply to the latest release.  Currently installation packages for HIRS V3 are only available for Rocky and RHEL version 8 and 9, and Ubuntu 22 and 24. 

To create an RPM on a Redhat or Rocky linux device use the following command in the same directory:
> ./gradlew buildRpm

or for a Debian or Ubuntu Linux device:
> ./gradlew buildDeb

The package can be found under the build/distributions/ folder.

# Installing
Currently only an install package for Linux is supported. 

To install this tool on a Redhat or Rocky Linux distro use the following command from the same directory:
> sudo  dnf install build/distributions/tcg_eventlog_tool*.rpm

or for a Debian or Ubuntu Linux distro:
> sudo  apt-get install build/distributions/tcg_eventlog_tool*.deb

Notes:
* Package naming convention: tcg_eventlog_tool-X.X.X-Y.Z.el8-1.x86_64.rpm
  * Where X.X.X is the latest version of the tcg_eventlog_tool package, Y is the date and Z is the git commit hash associated with that version tag
* Once installed, the tcg_eventlog_tool can be run from any directory in Linux

# Usage

Additional details on using the tcg_eventlog_tool can be found in the TCG Event Log Tool user Guide. A quick summary is listed below.

## Linux

The tcg_eventlog_tool installation package provides an elt command. The elt command has various command line options to view all events, specific events, or to display expected PCRs. 

Current options for the tool can be found using the -h option:

> elt -h

With No FILE the default event log path (e.g. /sys/kernel/security/tpm0/binary_bios_measurements on Linux) is used.
Note admin privileges are required for accessing the default path in Linux.

All OPTIONS must be separated by a space delimiter, no concatenation of OPTIONS is currently supported.

An example output for the tcg_eventlog_tool filtering on event 1 would be:
> elt -f ~/TpmLog.bin -e 1

## Windows
Currently there is not an install package for the tcg_eventlog_tool for Windows. It can be invoked using java:

To run the tcg_eventlog_tool from a command shell:

navigate to the tcg_eventlog_tool folder
invoke using java -jar option to the tcg_eventlog_tool jar file with options:

> java -jar build\libs\tcg_eventlog_tool-1.0.jar -h

another example:

> java -jar build\libs\tools\tcg_eventlog_tool-1.0.jar -f C:\Windows\Logs\MeasuredBoot\0000000059-0000000000.log -e
