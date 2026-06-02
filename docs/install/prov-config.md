---
title: Provisioner Configuration
---

# Provisioner Configuration

By default, the HIRS .NET Provisioner is configured to auto-detect a discrete or firmware TPM.
It will also use a bundled version of [PACCOR](../tools/paccor.md) to collect hardware information.

## Config file: appsettings.json

Settings shown on this page are captured in a json file ```appsettings.json```, located in the Provisioner 
installation directory:

=== "Linux"
    /usr/share/hirs
=== "Windows"
    C:\Program Files\(x86)\HIRS_Provisioner.NET

To update values in this json file, use the format:

```shell
<scheme>://<value>
```

## Most common configuration options

### `aca_address_port`

* The user must make sure the Provisioner is configured to talk to an ACA at a valid address. 
Verify or change the value of ```aca_address_port``` to point
to the server running the HIRS ACA. 

* Default is:
    ```shell
    "aca_address_port": "https://127.0.0.1:8443"
    ```

### `efi_prefix`

* If artifacts are expected to be retrievable from the device’s EFI System Partition
  (ESP), set this to the path where the ESP is mounted on the system. Artifacts will be
  captured according to the file structure defined in the TCG FIM or TCG RIM specifications.
  They will be uploaded along with other evidence used during provisioning, and they don’t
  have to be separately uploaded via the portal.
    * Recommended setting on **Windows**  
        The ESP can be mounted to any open drive path.
        1. mountvol P: /S
        2. Set the ```“efi_prefix”: “P:”```
    * Recommended setting on **Linux**
        1. The ESP is loaded to a directory by the Linux kernel by default.
        2. Set the ```“efi_prefix”: “/boot/efi”```

### `certificate_output_directory`

* If set, the Provisioner will output HIRS ACA certificates to the specified directory.
  Otherwise, the Provisioner will output certificates to
    ```shell
    <efi_prefix>/EFI/hirs/
    ```
  where ```efi_prefix``` can be configured as shown above. If neither ```certificate_output_directory```
  nor ```efi_prefix``` is set, any certificates will be output to the current working directory.

### `event_log_file`

* The TCG Event Log must be retrieved when performing RIM validation:

    === "Windows" 
        By default, the HIRS .NET Provisioner will ask the Windows API to provide
        the latest TCG Event Log. Alternatively, you may want to set this key in
        ```appsettings.json``` to a specific path.
    === "Linux"
        If this key is not set in the appsettings file, the HIRS .NET Provisioner
        will attempt to retrieve the TCG Event Log from the standard location if it is made
        available from the Linux kernel.

### `path`

* This value specifies the name of the Provisioner log file and the directory
where it will be placed. The default location is the current installation directory, 
and the default name is:
    ```shell
    hirs.log
    ```

* A common option is to change this to the ACA log directory:
    ```shell
    /var/log/hirs/hirs.log
    ```

    !!! note

        The Provisioner will add a date to the end of the log file. For instance,
        if you have configured your log file path to be 
        <br>
        &emsp;&emsp;/var/log/hirs/hirs.log
        <br>
        the actual log file will look like 
        <br>
        &emsp;&emsp;/var/log/hirs/hirs< date >.log.

## Other configuration options

### `auto_detect_tpm`

* If set to ```TRUE```, the program will search for a TPM. First, it will use an API offered by each OS
to locate a TPM. If that doesn’t find one, it will attempt to connect to a TPM on the
standard TPM socket of ```127.0.0.1:2321```.
* If set to ```FALSE```, the program must be given direction about where to look for the TPM via
command line arguments.

        | Command Line Argument         | Description                             |
                  |-------------------------------|-----------------------------------------|
        | --win                         | Connect to a TPM via the Windows API.   |
        | --nix                         | Connect to a TPM on Linux.              |
        | --tcp –-ip <ip:port>          | Connect to a TPM via socket.            |
        | --tcp --ip <ip:port> --sim    | Connect to a TPM simulator via socket.  |

### `hardware_manifest_collectors`

* This can be set to a comma-separated list of plugins that implement paccor’s
IHardwareManifest interface. These plugins will collect hardware information
according to different Component Class Registries. Plugins must be installed in
a ```plugins``` folder under the installation directory. The current default setting
includes all Component Class Registries
```paccor_scripts,paccor.pcie,paccor.smbios,paccor.storage```. If this setting
is not changed in ```appsettings.json```, hardware information will be collected
using paccor’s shell scripts on Windows or Linux and according to the specifications
for each Component Class Registry.

### `paccor_output_file`

* Alternatively, hardware information can be read from a JSON file. If this option
is set in ```appsettings.json```, the HIRS .NET Provisioner will attempt to read it
as a file. The contents will be sent to the ACA. The JSON format is described in
paccor’s readme file. Example format can be reviewed after running paccor’s ```allcomponents.sh``` script.

* __NOTE:__ If ```hardware_manifest_collectors``` is set, the 
```paccor_output_file``` setting will be ignored.

### `Linux platform descriptor files`

* The following settings will change where the HIRS .NET Provisioner looks for certain
system descriptor files made available by the Linux kernel. These are useful if
the file locations change over time, or if different values are desired for provisioning.
Within the ACA these values are used in database lookups to match records.

        - **linux_bios_vendor_file**  
          The location of the system bios_vendor file.
        - **linux_bios_version_file**  
          The location of the system bios_version file.
        - **linux_bios_date_file**  
          The location of the system bios_date file.
        - **linux_sys_vendor_file**  
          The location of the system sys_vendor file.
        - **linux_product_name_file**  
          The location of the system product_name file.
        - **linux_product_version_file**  
          The location of the system product_version file.
        - **linux_product_serial_file**  
          The location of the system product_serial file.

### `Logging`

* The HIRS .NET Provisioner uses Serilog for logging. A standard 
configuration is defined in the
appsettings.json file. In this standard configuration, the log 
file will appear in the directory that
the HIRS .NET Provisioner is executed from. The log file 
contains detailed information about the
HIRS provisioning process.
Additional configuration options can be found in Serilog 
documentation. See 
[Serilog settings configuration :fontawesome-solid-external-link:](https://github.com/serilog/serilog-settings-configuration){:target="_blank"}.