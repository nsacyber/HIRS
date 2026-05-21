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

The **HIRS Attestation Certificate Authority (ACA)** is a specialized Certificate Authority (CA) that 
supports the creation and issuance of an Attestation Key (AK) and/or Local Device Identifier (LDevID) 
Certificate according to TCG specifications. 

<img src= "../images/home-aca-ca-logo.jpg" alt="HIRS Acceptance Test Pic" class="center">

The ACA’s specialized nature stems from the types of keys it certifies, the formats of requests and
responses exchanged with the ACA, and the identity creation process details that are crucial for
maintaining the "chain of trust" which underpins the secure use of a TPM.

The ACA compares expected values (Assertions) against actual values (Evidence): 

- **Expected values** are delivered to the ACA as artifacts, such as Platform Certificates.
Each artifact has a signature used for validation. In order to trust the artifact/certificate, 
the ACA must validate the signature via the signature’s certificate chain. Each vendor 
must supply this certificate chain, which consists of a set of intermediate and root CA 
certificates. The ACA stores all of these certificates in its database. Some vendors choose to 
post the chain to a website while others choose to send the chain directly to the customer.

- **Actual values** are delivered to the ACA by the HIRS [Provisioner](hirs-provisioner.md). 

If all checks pass, the ACA issues an Attestation Certificate and/or LDevID 
to the HIRS Provisioner as during client provisioning.

See the [ACA Installation](install/aca-install.md) section for details on setup.

<!-- Hidden:
The ACA is a core component of the TPM PKI architecture. Its role is certifying attestation
keys, used by TPMs to sign Quotes. 
-->

!!! note

    The HIRS ACA uses a different request/response format and verification scheme than are traditionally 
    used for PKI. In the future, the ACA may operate as a subordinate to a commercial CA, which can
    provide certificate revocation support.

