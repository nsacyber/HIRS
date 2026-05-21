---
title: Endorsement Key Credential
---

<style>
.center {
display: block;
margin-left: auto;
margin-right: auto;
width: 35%;
}
</style>

# Endorsement Key Credential

The **Endorsement Key Credential** (EK Credential) as defined in the 
[TCG EK Credential Profile :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/http-trustedcomputinggroup-org-wp-content-uploads-tcg-ek-credential-profile-v-2-5-r2_published-pdf/){:target="_blank"}
is "an X.509 v3 certificate that contains the public EK, as well as
various assertions regarding the security qualities and provenance of the TPM."

<img src= "../../../images/bg-cert-end.jpg" alt="Endorsement Cert Pic" style="float: right; width: 40%; margin-left: 20px;">

The EK Credential is created by the TPM manufacturer, and the signature
of the issuer (TPM manufacturer) cryptographically binds the public
key material and the subject of the certificate to a specific TPM.

The Endorsement Key (EK) is defined in the TCG EK Credential Profile as "an asymmetric 
key pair consisting of a public and private key stored
in a Shielded Location on the TPM." The private key of the EK is burned into the TPM 
and is non-exportable. The public key of the EK can be read from the TPM and 
is included in the EK Credential.

!!! note

    The EK Credential is sometimes referred to as the Endorsement Credential, the 
    Endorsement Key Certificate (EK Certificate), or the Endorsement Certificate.

## Endorsement Key Credential & Attestation

Attestation CAs (like HIRS) use the EK Credential to verify that a new 
signing key (the Attestation Key) is being generated on the same TPM as the 
Endorsement Key. This ensures the Attestation Key is physically bound to that specific 
hardware before an Attestation Certificate is issued.