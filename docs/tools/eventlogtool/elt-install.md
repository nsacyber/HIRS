---
title: Installation
---

# Event Log Tool Installation

## Linux Build

   ```shell
   ./gradlew clean build
   ```

## Windows Build

Two options exist for building on Windows 11:

* Windows command shell (CMD.exe):
    * Navigate to the tcg_eventlog_tool folder and run the Windows gradle wrapper:
    ```shell
    gradlew.bat clean build
    ```
* Windows powershell with Windows Subsystem for Linux enabled:
    * Navigate to the tcg_eventlog_tool folder and run the Linux gradle wrapper:
    ```shell
    ./gradlew clean build
    ```

## Jar File Location

!!! info

    The tcg_eventlog_tool-X.X.jar file should populate in:
    === "Linux"
        build/libs/tools/
    === "Windows"
        build\libs\

## Packaging

### Pre-built Packages

Packages for this tool can be found on the
[HIRS release page :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/releases){:target="_blank"}. 
Download the RPM files which apply to the latest release. Currently installation packages for HIRS V3 
are only available for RHEL and Rocky version 8 and 9, and Ubuntu 22 and 24.

### Build Package

To create an RPM on a Redhat or Rocky Linux device use the following command in the same directory:

=== "RHEL/Rocky 8 or 9"
    ``` shell
    ./gradlew buildRpm
    ```
=== "Debian/Ubuntu 22"
    ``` shell
    ./gradlew buildDeb
    ```

The package can be found under the build/distributions/ folder.

## Install a Package

Currently only an install package for Linux is supported.
Once installed, the tcg_eventlog_tool can be run from any directory in Linux.

To install this tool on a Redhat or Rocky Linux distro use the following command from the same directory:

=== "RHEL/Rocky 8 or 9"
    ``` shell
    sudo dnf install build/distributions/tcg_eventlog_tool*.rpm
    ```
=== "Debian/Ubuntu 22"
    ``` shell
    sudo apt-get install build/distributions/tcg_eventlog_tool*.deb
    ```

!!! note

    Package naming convention: tcg_eventlog_tool-X.X.X-Y.Z.el8-1.x86_64.rpm <br>
    &emsp;where X.X.X is the latest version of the tcg_eventlog_tool package, <br>
    &emsp;Y is the date <br>
    &emsp;and Z is the git commit hash associated with that version tag <br>