To support the [PC Client RIM Specification](https://trustedcomputinggroup.org/resource/tcg-pc-client-reference-integrity-manifest-specification/) which utilizes the TPM Event Log as a Support RIM type , it was useful to have a tool for inspecting the contents of the [TPM event log](https://github.com/nsacyber/HIRS/wiki/TPM-Event-Logs). A Linux command line tool named "elt" (event log tool) has been created to parse and print human readable output, provide hexidecimal events which can be used as test patterns, and to compare event logs for providing details on what events mis-compared. 

Note that a TCG Event Log will only be populated on a given device if the device:
1. Utilizes TCG compliant UEFI Firmware.
2. Has a TPM 2.0.
3. Has a TPM aware OS (Most flavors of Linux and Windows).

The default locations for the TCG Event Log are:
* Windows:  C:\Windows\Logs\MeasuredBoot\
* Linux:    /sys/kernel/security/tpm0/    with a default name of "binary_bios_measurements"

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

In both cases the tcg_eventlog_tool-X.X.jar file should have been placed in the build\libs\tools\ (Windows) or build/libs/tools/ (Linux) folder.

# Packaging
Packages for this tool can be found on the [HIRS release page](https://github.com/nsacyber/HIRS/releases)

Currently only a packaging for Linux is supported.

To create an RPM on a Redhat or Rocky linux device use the following command in the same directory:
> ./gradlew buildRpm

or for a Debian or Ubuntu Linux device:
> ./gradlew buildDeb

the package can be found under the build/distributions/ folder

# Installing
Currently only a install package for Linux is supported. 

To install this tool on a Redhat or Rocky Linux distro use the following command from the same directory:
> sudo  dnf install build/distributions/tcg_eventlog_tool*.rpm

or for a Debian or Ubuntu Linux distro:
> sudo  apt-get install build/distributions/tcg_eventlog_tool*.deb

# Usage
## Linux

The tcg_eventlog_tool installation package provides an elt command. The elt command has various command line options to view all event , specific events,
or to display expected PCRs. 

Current options for the tool can be found using the -h option:

> elt -h

With No FILE the default event log path (e.g. /sys/kernel/security/tpm0/binary_bios_measurements on Linux) is used.
Note admin privileges are required for accessing the default path in Linux.

All OPTIONS must be separated by a space delimiter, no concatenation of OPTIONS is currently supported.

An example output for the tcg_eventlog_tool filtering on event 1 would be:
> elt -f ~/TpmLog.bin -e 1

## Windows
Currently there is not a install package for the tcg_eventlog_tool for windows. it can be invoked usinng java:

To run the tcg_eventlog_tool from the a command shell:

navigate to the tcg_eventlog_tool folder
invoke using java -jar option to the tcg_eventlog_tool jar file with options:

> java -jar build\libs\tools\tcg_eventlog_tool-1.0.jar -h

another example:

> java -jar build\libs\tools\tcg_eventlog_tool-1.0.jar -f C:\Windows\Logs\MeasuredBoot\0000000059-0000000000.log -e
