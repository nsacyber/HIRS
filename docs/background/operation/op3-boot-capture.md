---
title: 3. UEFI Captures Boot Info
---

# UEFI Captures Boot Info

## Hardware

UEFI firmware captures device info and stores it in an SMBIOS file in ACPI memory. After boot,
the OS grabs the info from the SMBIOS file and stores it in Class Registries.

## Firmware

As the computer boots, each stage of boot software measures the next stage before it loads.
A measurement is a hash (or digest) of that software. Examples of measured components include: 

- Code
- Configuration files
- Cryptographic keys
- ACM tables

Each measurement is:

1. Extended into a TPM Platform Configuration Register (PCR) slot
    - “Extending” is a one-way cryptographic operation that combines the new measurement
      with the current PCR value. Each PCR value reflects a cumulative chain of all prior measurements.
    - PCRs are registers within the TPM and cannot be directly modified without detection during the 
      verification process.
    - Because the operation is one-way, individual measurements cannot be removed or reversed
      (i.e. by malicious actors).
2. Captured and recorded in the TCG Event Log
    - Each TCG Event Log entry includes the measurement (hash) and a description of what was measured.
    - The TCG Event Log is stored in the UEFI partition on the device’s primary drive.

When the OS loader launches, the device hands the TCG Event Log to the OS. The OS:

- Copies the TCG Event Log to an OS-specific location during boot
- Extends its own measurements into dedicated TPM PCR slots
- Appends those measurements to the Event Log
    - As a result, the original firmware Event Log does not include OS-level measurements.
