---
title: Validation Outputs
---

# Validation Outputs

Upon validating a device, HIRS can issue certain artifacts which include:

| Artifact Output                                 | Creator                                          | Usage                                                                                                                                 |
|-------------------------------------------------|--------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| [**Attestation Certificate**](../outputs/ac.md) | :medal: IT Departments                           | Validates software load for attestation and provides device identity                                                                  |
| [**LDevID Certificate**](../outputs/ldevid.md)  | :medal: IT Departments                           | Validates software load for other application and provides device identity |


An [**Attestation Certificate**](ac.md) can be used for attestation only, whereas an 
[**LDevID**](ldevid.md) can be used in other applications. An LDevID can act as an ID 
to hand to another process to prove the identification of a device, and to prove 
that device has been properly validated.

