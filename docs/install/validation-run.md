---
title: Validation Run
---

# Validation Run

Running the Provisioner is the same thing as running the Validation.
If you are new to HIRS and want a step-by-step tutorial, it is best to start with
the [Getting Started](../started/index.md) page.

!!! note

    Before running the Provisioner, make sure to check the ACA Web Portal:

    1. Verify the ACA policy is set for your use case
    2. Verify all necessary Trusted Certificates (including the chains) are uploaded

## Run

=== "Linux"
    Run in a terminal:
    ```shell
    sudo tpm_aca_provision
    ```
=== "Windows"
    Run in an Admin PowerShell terminal:
    ```shell
    tpm_aca_provision.exe
    ```

!!! note

    If the TPM auto detection is turned off, additional command line options are necessary. See the 
    [Configuration Options](prov-config.md) section for details on this.

## Troubleshooting

- Nothing prints to the terminal when run:  
  Check that ```appsettings.json``` is a valid JSON file. Search “JSON validator” on a preferred
  internet search engine, then copy-and-paste the ```appsettings.json``` file into the validator. This 
  will help find any structural errors in this file. 


