---
title: Provisioner Build from Source
---

# Build Provisioner From Source

The HIRS .NET Provisioner requires the
[.NET SDK :fontawesome-solid-external-link:](https://learn.microsoft.com/en-us/dotnet/core/install/){:target="_blank"}
version 10 or later. Please follow the instructions from Microsoft for installing the .NET SDK on your system.

!!! note

    After installing the .NET SDK, you must restart your terminal for the 
    environment variables to get updated.

Most of the build commands work the same way whether they are run from Windows or Linux.
Location of where these commands are run matters. The directory the command should be run
from is included in each section below.

## Steps to Build

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

    === "RHEL/Rocky"
        ```shell
        dotnet tool install --global dotnet-rpm --allow-roll-forward
        ```
    === "Ubuntu"
        ```shell
        dotnet tool install --global dotnet-deb --allow-roll-forward
        ```
    === "Windows ZIP"
        ```shell
        dotnet tool install --global dotnet-zip --allow-roll-forward
        ```

5. `cd` to the directory
   ```shell
   cd <HIRS repository>/HIRS_Provisioner.NET/hirs 
   ```
6. Create installation package(s)

    === "RHEL/Rocky"
        ```shell
        dotnet rpm install
        dotnet rpm -r linux-x64 -c Release  
        ```
    === "Ubuntu"
        ```shell
        dotnet deb install
        dotnet deb -r linux-x64 -c Release   
        ```
    === "Linux ZIP"
        ```shell
        dotnet zip install
        dotnet zip -r linux-x64 -c Release   
        ```
    === "Windows"
        ```shell
        dotnet msbuild HIRS_Provisioner.NET.csproj /t:Msi /P:TargetFramework=net10.0 /p:RuntimeIdentifier=win-x64 /p:Configuration=Release 
        ```
    === "Windows ZIP"
        ```shell
        dotnet zip install
        dotnet zip -r win-x64 -c Release  
        ```

!!! note

    After building, you can find the generated file(s) in the relative path (from above):
    <br> 
    /bin/Release/net10.0/< platform >/HIRS_Provisioner.NET.*

## Installation Directory

The Provisioner is installed to the following directory:

=== "Windows"
    C:\Program Files\(x86)\HIRS_Provisioner.NET
=== "Linux"
    /usr/share/hirs

