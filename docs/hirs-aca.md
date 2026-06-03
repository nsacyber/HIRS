---
title: HIRS ACA 
---

<style>
.center {
display: block;
margin-left: auto;
margin-right: auto;
width: 50%;
}
</style>

# HIRS Attestation Certificate Authority (ACA)

The **HIRS Attestation Certificate Authority (ACA)** is a Verifier that 
supports the creation and issuance of an Attestation Certificate and/or Local Device 
Identifier (LDevID) Certificate according to TCG specifications. 

<img src= "../images/home-aca-ca-logo.jpg" alt="HIRS Acceptance Test Pic" class="center">

[ACA Policy](webportal/portal-policy.md) 
controls what features (hardware or firmware) on a device are tested. The recommended policy
setting for Trusted Computing is to validate the TPM, the platform hardware and the firmware.

The ACA compares expected values (Assertions) against actual values (Evidence): 

- **Expected values** are delivered to the ACA as endorsed artifacts, such as Platform Certificates.
Each artifact is digitally signed. In order to trust the artifact/certificate, 
the ACA must validate the signature via the signature’s certificate chain. Each vendor 
must supply this certificate chain, which consists of a set of intermediate and root CA 
certificates. The ACA stores all of these certificates in its database. Some vendors choose to 
post the chain to a website while others choose to send the chain directly to the customer.

- **Actual values** are delivered to the ACA by the HIRS [Provisioner](hirs-provisioner.md). For more information
on what Evidence the Provisioner collects on a device, see the section 
[Background - Provisioner Collects Data](background/operation/op4-validation.md/#provisioner-collects-data).

If all checks pass, the ACA issues an Attestation Certificate and/or LDevID 
to the HIRS Provisioner.


