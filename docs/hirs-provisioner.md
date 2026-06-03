---
title: HIRS Provisioner 
---

<style>
.center {
display: block;
margin-left: auto;
margin-right: auto;
width: 60%;
}
</style>

# HIRS Provisioner

The **HIRS Provisioner** is a command-line application intended to work in conjunction 
with the HIRS Attestation Certificate Authority (ACA) and the Trusted Platform Module (TPM) 
located on the device.  

<img src= "../images/home-prov-logo.jpg" alt="HIRS Acceptance Test Pic" class="center">

This application “provisions” the TPM, meaning it sets up one or more TPM-based 
keys and corresponding certificates based upon interactions with the HIRS ACA. HIRS 
[ACA policy](webportal/portal-policy.md) 
dictates which keys are created on the TPM and what certificates are issued to the device by 
the [HIRS ACA](hirs-aca.md). 

As part of this process, the Provisioner gathers
[device info](background/operation/op4-validation.md/#provisioner-collects-data)
from the client platform and sends 
this to the ACA. Device info includes the platform manufacturer and model, serial number, 
platform security attributes, a reference to the associated TPM, device 
component info, and more.

For setup details, refer to the [Provisioner Installation](install/prov/prov-install.md) section.
For installation requirements, see the [HIRS release page :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/releases){:target="_blank"}
for OS- or distribution-specific packages. 