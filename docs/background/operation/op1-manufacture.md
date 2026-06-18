---
title: 1. Manufacturer Creates Device
---

# Manufacturer Creates Device

## Initial Manufacturing of Device

The OEM (Original Equipment Manufacturer) creates several artifacts during the manufacturing 
of the device. Primarily these are for:

### 1. TPM “Root-of-Trust”

- TPM manufacturer creates an [Endorsement Key (EK) Credential](../inputs/endorsement-cert.md) during TPM manufacturing.
- EK Credential is stored in a shielded location on the TPM.
- Private portion of the EK is never exposed.
- Public portion of the EK is included in the EK Credential.
- TPM 2.0 can have more than one EK.

### 2. Platform/Hardware
- OEM creates a [Platform Certificate](../inputs/platform-cert.md) during manufacturing of platform.
- The Platform Certificate can have its own private key or it can reference the EK Credential.
- The Platform Certificate can be placed on the boot partition of the device for retrieval by a 
  Verifier or the OEM can provide the Platform Certificate out of band.

### 3. Firmware
- **RIM Bundle**
    - The OEM typically creates a [PC Client RIM Bundle](../inputs/rim.md) as part of the code production process.
    - The OEM can place the RIM Bundle on the boot partition of the device for retrieval by a Verifier or the 
      Verifier may retrieve the RIM Bundle out of band.
    - The Verifier can use the RIM Bundle as a benchmark for comparison of the device’s firmware at point of delivery.
    - The RIM Bundle contains at least one Base RIM and one Support RIM.
         - **Base RIM**
             - Base RIM is a signed XML file that meets the ISO 19770-2 Specification (SWID tag).
             - An OEM-specific Certificate Chain is required to verify the Base RIM.
             - The Base RIM contain hashes of the Support RIM files in the RIM Bundle.
         - **Support RIM**
             - The most common Support RIM is the TCG Event Log. The OEM captures the TCG Event Log at the 
               end of manufacturing and inserts a hash of the log into the Base RIM before the Base RIM is signed.
             - The digests (hashes) captured in the TCG Event Log should cover firmware 
               executed during start up, select configuration items, and Secure Boot keys. It 
               may include other digests covering modules provided by the operating system.
     
- Patch RIM
    - The OEM creates a Patch RIM Bundle for post-manufacturing updates to firmware.

### 4. Future: Component RIMs

- In the future, the Base RIM may be bundled with [Component RIMS :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/tcg-component-rim-binding-for-swid-and-coswid/){:target="_blank"}, which capture the expected 
  hardware/firmware state of individual components such as storage devices and network interface cards (NICs).

## Post-Manufacturing (Legitimate) Changes 

In some cases, a Value-Added Reseller (VAR) may make legitimate changes to a platform
before delivery to a customer.
Post-manufacturing changes must be included as digitally signed documents to maintain trust.
If not present, HIRS will flag them as malicious.

- Hardware changes:
    - Require a Delta Platform Certificate

- Firmware changes:
    - Require a Supplemental RIM (for VAR modifications)
    - Require a Patch RIM (for manufacturer-issued updates)
