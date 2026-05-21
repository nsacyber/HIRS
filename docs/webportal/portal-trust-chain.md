---
title: Trust Chain Management
---

# ACA Portal: Trust Chain Management Page

<style>
.center {
display: block;
margin-left: auto;
margin-right: auto;
width: 50%;
}
</style>

The **Trust Chain Management** page allows users to upload, download, and view certificates
used by the ACA for certificate validation. A certificate chain consists of the root and
intermediate CA certificates required to validate a specific certificate (such as an
Attestation, Endorsement, or Platform certificate). 

<img src= "../../images/portal-trust.png" alt="Portal Trustchain page" style="border: 2px solid grey;">

By default, the ACA generates a certificate chain that is used for verifying all issued 
Attestation Certificates. An Attestation CA certificate may be signed by a CA and replaced 
(the ACA certificate would become a subordinate to the root CA). In either case, the CA 
certificate must be trusted by a TPM Quote appraiser.

Clicking the
<img src="../../images/portal-clipboard.png" alt="clipboard" width="20" height="20" style="vertical-align:middle;">
icon next to the “HIRS Attestation CA Certificate” label on the Trust Chain 
Management page allows a user to view the ACA’s certificates. 

<img src= "../../images/portal-trust-clipboard.png" alt="Portal Trustchain page" class="center" style="border: 2px solid grey;">

This HIRS certificate chain will be required in future processing of TPM Quotes, since TPM Quotes 
are signed by the TPM’s Attestation Key (AK). For example, selecting the clipboard to view 
the HIRS certificate chain would look like:

<img src= "../../images/portal-trust-hirsroot.png" alt="Portal Trustchain page" style="border: 2px solid grey;">

Clicking the
<img src="../../images/portal-download.png" alt="clipboard" width="20" height="20" style="vertical-align:middle;">
icon allows a user to download the ACA's certificates. 

Other CA certificates (from any organization involved with the supply chain) can be uploaded, 
downloaded, deleted, or viewed using the icon selections next to the "Trust Chain CA Certificates" label. 