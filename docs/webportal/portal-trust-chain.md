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

The user can upload/download trusted certificates (intermediate and root)
from all organizations involved with the supply chain via
buttons at the top left of the page. To download a specific certificate, the user can click on the "
<img src="../../images/portal-download.png" alt="clipboard" width="20" height="20" style="vertical-align:middle;">
icon to the far right of that certificate listed.

The user can also view or download the ACA trust chain certificates via buttons at the top left of 
the page. By default, the ACA generates a certificate chain that is used for verifying all issued 
Attestation Certificates. An Attestation CA certificate may be signed by a CA and replaced 
(the ACA certificate would become a subordinate to the root CA). In either case, the CA 
certificate must be trusted by a TPM Quote appraiser.

For example, selecting the "View ACA Certificates" button at the top left will allow the user to 
select one of the certificates in the chain (root, intermediate, leaf) and view the details: 

<img src= "../../images/portal-trust-hirsroot.png" alt="Portal Trustchain page" style="border: 2px solid grey;">

This ACA trust chain certificates will be required in future processing of TPM Quotes, since TPM Quotes
are signed by the TPM’s Attestation Key. 