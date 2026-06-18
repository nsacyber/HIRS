---
title: Endorsement Key Certificates
---

# ACA Portal: Endorsement Key Certificates Page

The **Endorsement Certificates** page is used to upload, download, delete, and view Endorsement Certificates.

<img src= "../../images/portal-endorse.png" alt="Portal Endorsement Cert page" style="border: 2px solid grey;">

The EK Credential must contain:

* TPM public key
* TPM manufacturer, TPM model, and TPM version
* TPM security assertions (optional)

The EK Credential is required for TPM provisioning and supply chain confirmation. The ACA 
requires that the EK Credential's certificate chain is uploaded via the Trust Chain 
page of the ACA prior to performing any validation of EK Credential.

The user can view the uploaded EK Certificate by clicking the
<img src="../../images/portal-clipboard.png" alt="clipboard" width="20" height="20" style="vertical-align:middle;">
icon under the `Options` tab.

<img src= "../../images/portal-endorse-cert.png" alt="Portal Endorsement Cert page" style="border: 2px solid grey;">

!!! note

    When an Endorsement Certificate's signature is validated via the Trust Chain, a
    <img src="../../images/portal-green-check.png" alt="clipboard" width="20" height="20" style="vertical-align:middle;">
    icon will appear in the Issuer section. If any certificate is missing the chain, a
    <img src="../../images/portal-red-error.png" alt="clipboard" width="20" height="20" style="vertical-align:middle;">
    icon will appear instead.

For more information on Endorsement Certificates, see the 
[Endorsement Key Credential](../background/inputs/endorsement-cert.md) page. 

