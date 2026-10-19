---
title: Provisioner Build from Source
---

# Build Provisioner From Source

## Install .NET SDK

The HIRS .NET Provisioner requires the
[.NET SDK :fontawesome-solid-external-link:](https://learn.microsoft.com/en-us/dotnet/core/install/){:target="_blank"}
version 10 or later. Please follow the instructions from Microsoft for installing the .NET SDK on your system.

!!! tip

    If you are on a RHEL or Rocky, you can use:<br>
    ```shell
    sudo dnf install dotnet-sdk-10.0
    ```

!!! tip

    After installing the .NET SDK, you must restart your terminal for the 
    environment variables to get updated.

## Build the Provisioner Packages

Most of the build commands for HIRS .NET Provisioner work the same way whether
they are run from Windows or Linux. Location of where these commands are run matters.
The directory the command should be run from is included in each section below.

1. Checkout HIRS from Github
   ```shell
   git clone https://github.com/nsacyber/HIRS.git 
   ```
2. `restore` to retrieve all dependencies required for building or testing source code
   ```shell
   cd <HIRS repository>/HIRS_Provisioner.NET/
   dotnet restore 
   ```
3. Build and Run Unit Tests
   ```shell
   dotnet test 
   ```
4. Packaging uses the HIRS repository's Gradle wrapper from the repository root.

    === "Linux"
        ```shell
        ./gradlew -p HIRS_Provisioner.NET release
        ```
    === "Windows"
        ```powershell
        .\gradlew.bat -p HIRS_Provisioner.NET release
        ```

    Both commands produce an RPM and DEB for `linux-x64`. The Windows command will additionally produce an MSI for `win-x64`.

!!! info

    After building, you'll find package files under the provisioner project folder.
    ```shell
    cd HIRS_Provisioner.NET/build/distributions/
    ```

## Install the Provisioner Package

To install the Provisioner package, see the
[Provisioner install](prov-install.md/#package-installation) section.

## Installation Directory

The Provisioner is installed to the following directory:

=== "Linux"
    /usr/share/hirs
=== "Windows"
    C:\Program Files\(x86)\HIRS_Provisioner.NET
