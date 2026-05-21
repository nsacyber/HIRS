---
title: ACA Install
---

# ACA Install

On Linux and Windows devices you can install the HIRS Attestation Certificate Authority (ACA) 
via Docker (the preferred method). Alternatively, on Linux only, you can install the ACA 
via a release package or by building from source.

## Option 1: Install ACA via Docker (Linux or Windows)

Docker is the preferred way to install/run the ACA for both Linux and Windows because it is the simplest 
method for the user. An ACA Docker image is automatically created for each HIRS release. 
To run the ACA from a Docker container:

```shell
docker run --name=aca -p 8443:8443 ghcr.io/nsacyber/hirs/aca:latest
```

## Option 2: Install ACA via package (Linux only)

### Supported Platforms

The ACA currently supports package based installation on

- Redhat OS versions 8 (latest) and 9
- Rocky OS versions 8 (latest) and 9
- Ubuntu 22 LTS with limited support

### Dependencies

=== "RHEL/Rocky 9"
    ``` shell
    sudo dnf install java-25-openjdk java-25-openjdk-devel wget tpm2-tools mariadb-server
    ```
=== "RHEL/Rocky 8"
    ``` shell
    sudo dnf install wget tpm2-tools mariadb-server
    [must install java-25-openjdk java-25-openjdk-devel manually]
    ```
=== "Debian/Ubuntu 22"
    ``` shell
    sudo apt-get install java-25-openjdk java-25-openjdk-devel git tpm2-tools mariadb-server
    ```

### 2a. Install ACA via Release Package

To install the ACA on Linux, download the latest package for your selected OS from the 
[release page :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/releases){:target="_blank"}, 
then run the command

=== "RHEL/Rocky 8 or 9"
    ``` shell
    sudo dnf install HIRS_AttestationCA*.rpm
    ```
=== "Debian/Ubuntu 22"
    ``` shell
    sudo apt-get install HIRS_AttestationCA*.deb
    ```

### 2b. Install ACA via Source

To install the ACA from source, see 
[Building the HIRS ACA from Source :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/wiki/Hirs-build-guide#building-the-hirs-aca-from-source){:target="_blank"}.

## View ACA Portal

Once the ACA is installed, you can view the ACA on the [ACA Web Portal](../webportal/index.md).