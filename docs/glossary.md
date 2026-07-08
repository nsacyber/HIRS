# Glossary

Terms used repeatedly across HIRS, ACA, Provisioner and the TCG specification documents.

## A

**Artifact (Digital)**
:   An information-bearing object that is, or is encoded to be used with, a digital computer system.

**Attester**
:   A computing entity whose trustworthiness can be evaluated. The Attester implements attestation 
functions (e.g., collects claims, protects claims, and conveys Evidence to a Verifier). 
The Attester is sometimes referred to as device or client.

**Attestation Certificate (AC)**
:   An X.509 certificate issued by the HIRS Verifier to provide device identity and validation of the device hardware and software load.

**Attestation Certificate Authority (ACA)**
:   A specialized Certificate Authority (CA) which supports the creation and issuance of an Attestation Key (AK) and/or a Device Identifier (DevID) Certificate for HIRS.

## E

**Endorsement**
:   A type of verifiable Artifact that makes claims about an Attester,
that are authenticatable, that are supplied by an Endorser.

**Endorsement Key (EK) Credential**
:   An X.509 v3 certificate that contains the public EK. 
Also commonly referred to as EK Certificate.

**Endorser**
:   An entity that describes trustworthiness properties of an Attester that typically do not
appear in Evidence. An Endorser Role refers to functionality that creates, provisions,
or conveys trustworthiness properties, i.e., Endorsements, to Verifiers.

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