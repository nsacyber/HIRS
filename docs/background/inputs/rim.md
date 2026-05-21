---
title: Reference Integrity Manifest 
---

<style>
.center {
display: block;
margin-left: auto;
margin-right: auto;
width: 30%;
}
</style>

# Reference Integrity Manifest

A **Reference Integrity Manifest** (RIM) is defined in the 
[TCG PC Client Reference Integrity Manifest Specification :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/tcg-pc-client-reference-integrity-manifest-specification/){:target="_blank"}
, and is an OEM-produced artifact that represents the expected
values used by the ACA to validate device firmware. Firmware validation complements 
platform validation for supply chain acceptance testing by providing an automated way 
to verify the firmware and boot software before an Attestation Certificate is issued.

<img src= "../../../images/bg-rim-scroll.jpg" alt="RIM Scroll Pic" class="center" style="float: right; width: 40%; margin-left: 20px;">

The Reference Integrity Information Model 
defines structures that a Verifier uses to validate expected values (Assertions) against actual 
values (Evidence). 

For PC Clients, there are two types of RIM files, collectively called the **RIM Bundle**:

- **Base RIM**:
    - Complies with the ISO 19770-2 Software Identity (SWID) standard.
    - Provides a verifiable identity of the RIM creator and integrity information for associated Support RIMs.
    - Can be stored on the device in the boot partition or made available via a Uniform Resource Identifier (URI).
- **Support RIM**:
    - Provides additional information required for verification.
    - For PC Clients, the TCG Event Log generated during the boot process is a required Support RIM.
    - The Event Log (defined by [TCG PC Client Platform Firmware Profile :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/pc-client-specific-platform-firmware-profile-specification/){:target="_blank"}) records all events that extend the TPM's Platform Configuration Registers (PCRs).
    - The OEM captures this Event Log at the end of production and inserts a hash of it in the Base RIM before signing.

For PC Components, **Component RIMs** may come in different forms, such as the TCG 
Component RIM and the IETF CoRIM. Unlike the PC Client RIM, a PC Component RIM uses Concise 
Binary Object Representation (CBOR) encoding and CBOR Object Signing and Encryption (COSE) signatures.

!!! info

    Additional information about the RIM can be found on the 
    [RIM-Tool :fontawesome-solid-external-link:](https://nsacyber.github.io/RIM-Tool/){:target="_blank"}
    documentation webpage.
