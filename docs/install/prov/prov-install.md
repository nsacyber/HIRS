---
title: Provisioner Package Install
---

# Provisioner Install

## Supported Platforms

The HIRS_Provisioner.NET currently supports package based installation on:


=== "RHEL/Rocky"
    Redhat OS versions 8 (latest) and 9<br>
    Rocky OS versions 8 (latest) and 9
=== "Ubuntu"
    Ubuntu 22 LTS with limited support
=== "Windows"
    Windows 10 (latest)<br>
    Windows 11 

## Dependencies

The HIRS .NET Provisioner is self-contained. No dependencies should be needed to run the program.

## Package Installation

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

## Troubleshooting

### GPG Error

When installing the Provisioner RPM on RHEL Linux, if you see the following:
```text
Error: GPG check FAILED
```
Try installing with the gpg check disabled:
```shell
sudo dnf install --nogpgcheck HIRS_Provisioner.NET.*.rpm
```

