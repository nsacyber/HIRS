# Glossary

Terms used repeatedly across HIRS, ACA, Provisioner and the TCG specification documents.

## A

**Attestation Certificate (AC)**
:   An X.509 certificate issued by the HIRS Verifier to provide device identity and validation of the device hardware and software load.

**Attestation Certificate Authority (ACA)**
:   A specialized Certificate Authority (CA) which supports the creation and issuance of an Attestation Key (AK) and/or a Device Identifier (DevID) Certificate for HIRS.

## E

**Endorsement Key (EK) Credential**
:   An X.509 certificate, signed by a TPM manufacturer's CA, that 
binds a TPM to a manufacturer and provides proof of authenticity for a TPM's 
Endorsement Key. Also commonly referred to as EK Certificate.

## H

**Host Integrity at Runtime and Startup (HIRS)**
:   A Verifier with a collection of measurement
and attestation capabilities that provide integrity analysis of a running platform.

## O

**Original Equipment Manufacturer (OEM)**
:   An organization that produces a computer or
component, and may or may not use component parts bought from other organizations.

## P

**Platform Certificate**
:   An X.509 certificate, signed by a platform manufacturer's CA, that
binds a device (server, desktop or laptop) to a manufacturer, model, and serial number.
Also commonly referred to as Platform Credential.

## R

**Reference Integrity Manifest (RIM)**
:   An OEM-produced artifact that provides a set of
Expected Values that can be used by a Verifier to compare with Evidence (or measured
values) for firmware validation.

## T

**TCG**
:   Trusted Computing Group. The standards body that publishes the platform certificate, component registry, TPM, and related specifications that paccor implements.

**Verifier**
:   A system that analyzes evidence from a platform or platform component to determine
its state.