---
title: 4. Upload Artifacts
---

# 4. Gathering artifacts you will need

There are certain artifacts that are required based on the input configuration 
you selected during the HIRS [Configuration](../started/gs3-hirs-config.md) stage.

!!! note

    Based on your input configuration (whether you checked endorsement, hardware, 
    and/or firmware), if you know that you have the relevant artifacts in their 
    proper folders, to include their trust chains, you can skip the particular 
    section on this page for those particular artifacts.
    For instance, if you have configured the ACA to check endorsement, hardware, and
    firmware, and you have all of the following
    
    1. Endorsement Certificate
    2. Platform Certificate
    3. RIM and TCG Event Log
    4. All associated trust chains

    then you can skip this entire section and move on to [Install the Provisioner](gs5-prov-install.md).

## 'Blank' configuration - software install check

In a configuration with no credential validations enabled, there are no 
artifacts that are required.

## Configuration with the Endorsement Certificate

If the Endorsement Certificate configuration on the 
[Policy](../webportal/portal-policy.md) page is enabled, the validation will
require the Endorsement Certificate and its certificate chain.

### Endorsement Certificate

Depending on the [Provisioner Configuration](../install/prov-config.md), the
Provisioner will search for a TPM in specific locations. If the Provisioner 
is able to find the TPM and retrieve the Endorsement 
Certificate, then you will not need to upload an Endorsement Certificate.

However, if the Provisioner cannot retrieve the Endorsement Certificate, you 
will need to obtain it from the TPM manufacturer and upload it to the 
[Endorsement Key Credentials](../webportal/portal-endorsement-certs.md) page. 

### Endorsement Certificate Trust Chain

You will need to download the TPM manufacturer's root and intermediate 
certificates and then upload the (non-zipped) certificates to the
[Trust Chain Management](../webportal/portal-trust-chain.md) page.

The best single source for TPM manufacturer CA (Certificate Authority) certificates is 
kept by Microsoft: 

* [TPM Root Certificates :fontawesome-solid-external-link:](https://docs.microsoft.com/en-us/windows-server/virtualization/guarded-fabric-shielded-vm/guarded-fabric-install-trusted-tpm-root-certificates){:target="_blank"}

Some TPM manufacturers have TPM CA certificates available on the web 
(not a complete list):

* [Infineon CA Certificates :fontawesome-solid-external-link:](https://www.infineon.com/cms/en/product/promopages/optiga_tpm_certificates/#SLB9670xx2.0){:target="_blank"}
* [ST Microelectronics :fontawesome-solid-external-link:](https://www.st.com/resource/en/technical_note/tn1330-st-trusted-platform-module-tpm-endorsement-key-ek-certificates-stmicroelectronics.pdf){:target="_blank"}
(document which lists the URL for the certificates)

### TPM Manufacturer Information

If you do not know the TPM information such as manufacturer, you can determine 
this via command line:

Run in a terminal:

=== "Linux"
```shell
tpm2_getcap properties-fixed
```

Typical output: 

=== "Linux"
```shell
TPM2_PT_MANUFACTURER:
  raw: 0x49465800
  value: "IFX"
```

Where the value can be found in the 
[TCG Vendor ID Registry :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/vendor-id-registry/).

## Configuration with the Platform Certificate

If the Platform Certificate configuration on the
[Policy](../webportal/portal-policy.md) page is enabled, the validation will
require 

* the artifacts from the Endorsement section above
* the Platform Certificate and its certificate chain

### Platform Certificate

The Provisioner will search for a Platform Certificate in the directory specified by the
Provisioner's configuration file ```appsettings.json``` under the scheme
[```efi_prefix```](http://hirs-dlat-ro-03:8000/HIRS/HIRS/install/prov-config/#efi_prefix).
If the Provisioner cannot retrieve the Platform Certificate, you
will need to obtain it from the Platform manufacturer and upload it to the
[Platform Certificates](../webportal/portal-platform-certs.md) page.

If you cannot obtain a manufacturer Platform Certificate, you can create one for 
testing purposes. See the 
[PACCOR Getting Started :fontawesome-solid-external-link:](https://nsacyber.github.io/paccor/getting-started.html){:target="_blank"}
page in the PACCOR project for instructions.

### Platform Certificate Trust Chain

You will need to download the platform manufacturer's root and intermediate
certificates and then upload the (non-zipped) certificates to the
[Trust Chain Management](../webportal/portal-trust-chain.md) page.

## Configuration with the RIM

If the firmware configuration on the [Policy](../webportal/portal-policy.md) 
page is enabled, the validation will require

* the artifacts from the Endorsement section above
* the artifacts from the Platform section above
* the RIM and its certificate chain
* the TCG Event Log as a support RIM

### RIM

The Provisioner will search for a RIM in the directory specified by the
Provisioner's configuration file ```appsettings.json``` under the scheme
[```efi_prefix```](http://hirs-dlat-ro-03:8000/HIRS/HIRS/install/prov-config/#efi_prefix).
If the Provisioner cannot retrieve the RIM, you
will need to obtain it from the RIM manufacturer and upload it to the
[Reference Integrity Manifests](../webportal/portal-rims.md) page.

If you cannot obtain a manufacturer RIM, you can create one for 
testing purposes. See the
[RIM-Tool](https://nsacyber.github.io/RIM-Tool/) 
page in the RIM-Tool project for instructions.

### RIM Trust Chain

You will need to download the RIM manufacturer's root and intermediate
certificates and then upload the (non-zipped) certificates to the
[Trust Chain Management](../webportal/portal-trust-chain.md) page.

### TCG Event Log (Support RIM)

Validation of a platform's firmware requires the TCG Event Log as a support RIM. 

The Provisioner will search for a TCG Event Log in the directory specified by the
Provisioner's configuration file ```appsettings.json``` under the scheme
[```event_log_file```](http://hirs-dlat-ro-03:8000/HIRS/HIRS/install/prov-config/#event_log_file).
If the Provisioner cannot retrieve the TCG Event Log, you
will need to obtain it from the RIM manufacturer and upload it to the
[Reference Integrity Manifests](../webportal/portal-rims.md) page.

If you cannot obtain a manufacturer TCG Event Log, you can create one for testing purposes. See the
[Rim-Tool Getting Started :fontawesome-solid-external-link:](https://nsacyber.github.io/RIM-Tool/getting-started/){:target="_blank"}
page in the RIM-Tool project for instructions.