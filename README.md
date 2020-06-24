<h1><center>Host Integrity at Runtime and Start-up (HIRS) <BR\></center></h1>

[![Build Status](https://travis-ci.org/nsacyber/HIRS.svg?branch=master)](https://travis-ci.org/nsacyber/HIRS)

<h2>Attestation Certificate Authority (ACA) and TPM Provisioning with Trusted Computing-based Supply Chain Validation </h2>

The Host Integrity at Runtime and Start-up Attestation Certificate Authority is a Proof of Concept - Prototype intended to spur interest and adoption of the [Trusted Platform Module (TPM)](https://trustedcomputinggroup.org/work-groups/trusted-platform-module/). It's intended for testing and development purposes only and is not intended for production. The ACA's functionality supports the provisioning of both the TPM 1.2 and [TPM 2.0](https://trustedcomputinggroup.org/wp-content/uploads/2019_TCG_TPM2_BriefOverview_DR02web.pdf) with an [Attestation Identity Credential (AIC)](https://www.trustedcomputinggroup.org/wp-content/uploads/IWG-Credential_Profiles_V1_R1_14.pdf). The ACA can be configured to enforce the Validation of Endorsement and Platform Credentials to illustrate a supply chain validation capability. 

The ACA provides a “provisioner” application to be installed on all devices which will be requesting Attestation Credentials.
The ACA is a web based server which processes Attestation Identity Requests.
![TPM Provisioning](images/TPM_Provisioning.jpg)

Version 1.1 added support for the [Platform Certificate v1.1 Specification](https://trustedcomputinggroup.org/wp-content/uploads/IWG_Platform_Certificate_Profile_v1p1_r15_pubrev.pdf). This allows entities that are part of the supply chain (System integrators and Value Added Resellers) the ability to create Delta Platform Certificate to compliment the Base Platform Certificate created by the Platform Manufacturer. See the [Article on Base and Delta Platform Certificates](https://github.com/nsacyber/HIRS/wiki/Base-and-Delta-Platform-Certificates) for details.

Version 2.0 will add support for the [PC Client Reference Integrity Manifest (RIM) Specification](https://trustedcomputinggroup.org/wp-content/uploads/TCG_PC_Client_RIM_r0p15_15june2020.pdf) to provide firmware validation capability to the HIRS ACA. This requires that the manufacturer of a device provide a digitally signed RIM "Bundle" for each device. The HIRS ACA has a new page for uploading and viewing RIM Bundles and a policy setting for requiring Firmware validation.

To support the TCG RIM concept a new tools folder has been added to the HIRS project which contains a tcg_rim_tool command line application. The tcg_rim_tool can be used to create NISTIR 8060 compatible SWID tags that adhere to the TCG PC Client RIM specification. It also supports the ability to digitally sign the Base RIM file as the HIRS ACA will require a valid signature in order to upload any RIM file. See the [tgc_rim_tool READ.md](https://github.com/nsacyber/HIRS/blob/master/tools/tcg_rim_tool/README.md) for more details.

## Features

* TPM Provisioner
  * Requests an Attestation Identity Credential for the TPM from the ACA.
  * Takes ownership of TPM if not owned
  * Uses REST calls to complete the transaction with the ACA
  * Reads credentials from the TPM's NvRAM as part of the provisioning process.
  * Reads the device's hardware, network, firmware, and OS info for platform validation
* Attestation Certificate Authority
  * Issues Attestation Identity Credentials to validated devices holding a TPM
  * Configures policies for enabling/disabling validation procedures
  * Performs TCG-based Supply Chain Validation of connecting clients
      * Optionally validates Endorsement and Platform Credentials
* Endorsement Credential Certificate Chain Validation
  * Process EK Credentials per [TCG EK Credential Profile For TPM Family 2.0; Level 0
Revision 14](https://www.trustedcomputinggroup.org/wp-content/uploads/Credential_Profile_EK_V2.0_R14_published.pdf)
  * Verifies the endorsement key used by the TPM was placed there by the original equipment manufacturer (OEM)
* Platform Credential Certificate Chain Validation
  * Process Platform Credentials per [TCG Platform Attribute Credential Profile Specification Version 1.1 Revision 15](https://trustedcomputinggroup.org/wp-content/uploads/IWG_Platform_Certificate_Profile_v1p1_r15_pubrev.pdf)
  * Verifies the provenance of the system's hardware components, such as the motherboard and chassis, by comparing measured component information against the manufacturers, models, and serial numbers listed in the Platform Credential
* Attestation CA Dashboard
  * Displays all Validation Reports, Credentials, and Trust Chains
  * Enables ACA policy configuration for validation of Endorsement and Platform Credentials
  * Enables Import/Export of Certificate (Trust) Chains, Endorsement Credentials, and Platform Credentials
  * Optionally allows uploaded credentials to be used in validation for machines that have been reprovisioned by trusted parties since leaving the OEM
* Firmware Integrity Validation
  * Checks that firmware and boot related file hashes match those provided by OEMs.
  * Validates the import of All RIM files imported to the ACA (insure all RIM files were signed by trusted sources)
  * Verifies that the firmware hashes captured by the TPMs Platform Configuration Registers (PCRs) match the firmware hashes obtained from the OEM(s).
  * Verifies TCG/UEFI boot variables (e.g. BIOS setup data) have not been altered (e.g secure boot).
## Requirements

The HIRS Attestation Certificate Authority (ACA) supports installation on CentOS 6 and 7 instances.

The HIRS Provisioner supports both types of TPMs, 1.2 and 2.0. TPM 1.2 support is available on CentOS 6 and 7. Due to the limitations on the libraries available on Centos 6, TPM 2.0 support is only available on Centos 7.

## Installation Instructions

For detailed instructions, see [Installation notes](https://github.com/nsacyber/HIRS/wiki/installation_notes).

Packages used for installation (e.g. HIRS_Provisioner*el7.noarch.rpm) can be found on the [release page](https://github.com/nsacyber/HIRS/releases).

### Installing the ACA

Simply run the command `yum install HIRS_AttestationCA*el6.noarch.rpm` or `yum install HIRS_AttestationCA*el7.noarch.rpm` based on your OS.

### Installing the Provisioner

After enabling your TPM in BIOS/UEFI, determine if your machine has a TPM 1.2 or a TPM 2.0 by using the command: <br>
`dmesg | grep -i tpm_tis`.

To install a TPM 1.2 Provisioner, run:<br>
`yum install tpm_module*.rpm`<br>
`yum install HIRS_Provisioner*el6.noarch.rpm` or `yum install HIRS_Provisioner*el7.noarch.rpm` based on OS<br>
`hirs-provisioner -c`

To install a TPM 2.0 Provisioner, run:<br>
`yum install hirs-provisioner-tpm2*.rpm`

To configure the provisioner, edit the hirs-site.config file in `/etc/hirs/hirs-site.config`. Edit the file to specify the ACA's fully qualified domain name and port.

## Usage

To kick off a provision on the client, run the command `sudo tpm_aca_provision`.

To see the results and interact with the ACA, go to the ACA Portal at `https://ACAPortalAddress:ACAPortalPort/HIRS_AttestationCAPortal/portal/index`.

## Quick Links:

* [TPM 2.0: A brief introduction](https://trustedcomputinggroup.org/wp-content/uploads/2019_TCG_TPM2_BriefOverview_DR02web.pdf)
* [Getting started with The ACA and Platform Credentials](https://github.com/nsacyber/HIRS/wiki/Gettingstarted)
* [HIRS ACA and TPM provisioner Users Guide](https://github.com/nsacyber/HIRS/blob/master/HIRS_AttestationCAPortal/src/main/webapp/docs/HIRS_ACA_UsersGuide_1.0.3.pdf)
* [Installation notes](https://github.com/nsacyber/HIRS/wiki/installation_notes)
* [Project build instructions](https://github.com/nsacyber/HIRS/wiki/Hirs-build-guide)
* [HIRS Attestation Certificate Authority FAQ](https://github.com/nsacyber/HIRS/wiki/FAQ)
* [TPM Provisioner Debug](https://github.com/nsacyber/HIRS/wiki/provisioner_debug)
* [ACA Debug](https://github.com/nsacyber/HIRS/wiki/aca_debug)
* Tools
  * [Platform Certificate Creator](https://github.com/nsacyber/paccor) 
  * [Reference Integrity Manifest tool (tcg_rim_tool)](https://github.com/nsacyber/HIRS/releases)
  * [Event Log tool (tcg_eventlog_tool)](https://github.com/nsacyber/HIRS/releases)
