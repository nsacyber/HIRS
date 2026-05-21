---
title: Provisioner Install
---

# Provisioner Install

## Supported Platforms

The HIRS_Provisioner.NET currently supports package based installation on:

- Redhat OS versions 8 (latest) and 9
- Rocky OS versions 8 (latest) and 9
- Ubuntu 22 LTS with limited support
- Windows 10 (latest)
- Windows 11 

## Dependencies

The HIRS .NET Provisioner is self-contained. No dependencies should be needed to run the program.

## Option 1: Package Installation

=== "RHEL/Rocky"
    Download the latest RPM from the [release page :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/releases){:target="_blank"},
    then run the following in a terminal:
    ``` shell
    sudo dnf install HIRS_Provisioner.NET.*.rpm
    ```
=== "Ubuntu"
    Download the latest DEB from the [release page :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/releases){:target="_blank"},
    then run the following in a terminal:
    ``` shell
    sudo apt-get install ./HIRS_Provisioner.NET.*.deb
    ```
=== "Windows"
    Download the latest MSI from the [release page :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/releases){:target="_blank"},
    then run the following in PowerShell:
    ``` shell
    msiexec /package HIRS_Provisioner.NET.*.msi /quiet
    ```

### Troubleshooting

- GPG Error:<br><br>
   If you see the following, for example on Linux Redhat:
   ```text
   Downloading Packages:
   Public key for HIRS_Provisioner.NET.3.0.6.linux-x64.rpm is not installed
   Error: GPG check FAILED
   ```
   Try installing with the gpg check disabled. For instance for Linux Redhat:
   ```shell
   sudo dnf install --nogpgcheck HIRS_Provisioner.NET.*.rpm
   ```


## Option 2: Build from Source

The HIRS .NET Provisioner requires the 
[.NET SDK :fontawesome-solid-external-link:](https://learn.microsoft.com/en-us/dotnet/core/install/){:target="_blank"}
version 6 or later. Please follow the instructions from Microsoft for installing the .NET SDK on your system.

!!! note

    You can also look at the HIRS GitHub Actions workflows for hints on setting up your build environment.

Most of the build commands work the same way whether they are run from Windows or Linux. 
Location of where these commands are run matters. The directory the command should be run 
from is included in each section below. 

Steps to Build

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
4. Install a packaging library.
   Depending on your choice below, you might need to install a packaging library. This step is not 
   needed to build an MSI installer.

    === "RPM"
        ```shell
        dotnet tool install --global dotnet-rpm
        ```
    === "DEB"
        ```shell
        dotnet tool install --global dotnet-deb
        ```
    === "ZIP"
        ```shell
        dotnet tool install --global dotnet-zip
        ```

5. `cd` to the directory
   ```shell
   cd <HIRS repository>/HIRS_Provisioner.NET/hirs 
   ```
6. Create installation package(s)

    === "MSI"
        ```shell
        dotnet msbuild HIRS_Provisioner.NET.csproj /t:Msi /P:TargetFramework=net8.0 /p:RuntimeIdentifier=win-x64 /p:Configuration=Release 
        ```
    === "RPM"
        ```shell
        dotnet rpm -r linux-x64 -c Release  
        ```
    === "DEB"
        ```shell
        dotnet deb -r linux-x64 -c Release   
        ```
    === "ZIP (Windows bin)"
        ```shell
        dotnet zip -r win-x64 -c Release  
        ```
    === "ZIP (Linux bin)"
        ```shell
        dotnet zip -r linux-x64 -c Release   
        ```

!!! note
    After building, you can find the generated file(s) in the relative path (from above): 
    /bin/Release/net8.0/<platform>/HIRS_Provisioner.NET.*

