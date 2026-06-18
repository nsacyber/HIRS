---
title: Platform Certificate
---

<style>
.center {
display: block;
margin-left: auto;
margin-right: auto;
width: 35%;
}
</style>

# Platform Certificate

The **Platform Certificate** as defined in the
[TCG Platform Certificate Profile :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/tcg-platform-certificate-profile/){:target="_blank"}
is "a signed statement describing characteristics of a platform that can affect the platform’s
trustworthiness."

<img src= "../../../images/bg-cert-plat.jpg" alt="Platform Cert Pic" style="float: right; width: 40%; margin-left: 20px;">

The Platform Certificate is created by the platform manufacturer, and 
serves as the definitive link between the identity of a specific platform and its internal 
security components (one or more Roots of Trust such as the TPM). The signature of the 
issuer (manufacturer) cryptographically binds the manufacturer, model, serial number, and a 
set of hardware components to that specific platform. The platform can be a server, 
desktop or laptop.

## Component List

Inside the Platform Certificate is a Component List. This list typically includes: 

- <b>Component Class</b>: (e.g., CPU, TPM, NIC, Storage)
- <b>Manufacturer & Model</b>: The specific identity of the part
- <b>Serial Numbers</b>: Unique identifiers for each critical chip
- <b>Network Adapter MAC addresses</b>: Unique identifiers for each integrated network controller
- <b>Measurements/Hashes</b>: Cryptographic snapshots of firmware or hardware configurations

## HBOM

Recently, the Hardware Bill of Materials (HBOM) has received significant attention due to
the growing adoption of Zero Trust security principles. In a Zero Trust architecture, organizations
must verify and understand the components that make up their systems; if the silicon and hardware
components inside servers, vehicles, or medical devices are unknown, they cannot be fully trusted.
An HBOM provides this visibility by documenting all physical components within a device, along with
the supply chain origins of those parts.

While the HBOM offers visibility into all components, the Platform Certificate provides 
verification—it proves that a device has not been tampered with. The Platform Certificate
focuses specifically on security-critical components, taking a subset of the HBOM and
signing only the parts that are sensitive to system integrity.

## Delta Platform Certificates

A Delta Platform Certificate is defined in the TCG Platform Certificate Profile. It is
issued by an entity such as a system integrator or Value Added Reseller (VAR) when
authorized changes are made to a platform that are not reflected in the original Base
Platform Certificate. Examples include upgrading RAM, adding a GPU, or replacing a
faulty network card.

The Delta Platform Certificate is cryptographically linked to 
the Base Platform Certificate and describes only the changes (the "deltas").
Together, the Base and Delta Certificates form a Platform Certificate Chain,
representing the full, validated configuration of a device.

Key Fields in a Delta Certificate:

- <b>Base Certificate Reference</b>: A pointer (usually a hash or serial number) to the 
  original Base Platform Certificate
- <b>Component Status</b>: For every item in the new component list, it specifies a status:
    - <b>Added</b>: A new physical component was installed
    - <b>Removed</b>: An existing component was taken out
    - <b>Modified</b>: An existing component was upgraded or reconfigured (e.g., firmware update)
- <b>Issuer</b>: Usually the VAR, not the original manufacturer

## Platform Certificate & Attestation

Attestation CAs, like HIRS, use the Platform Certificate Chain to verify a system's measured
security attributes and physical components match the "golden standard"
information found in the Platform Certificate. 
This ensures the hardware has not been tampered with before 
an Attestation Certificate is issued.


