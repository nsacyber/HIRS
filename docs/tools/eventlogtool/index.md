---
title: Event Log Tool
---

# Event Log Tool

The **Event Log Tool** is a command line application that allows a user to inspect the TPM 
Event Log's contents. 

## Event Log File Default Location

!!! info

    The TPM Event Log is a binary file and its default location/name is:

    === "Linux"
        Default location: /sys/kernel/security/tpm0/ <br>
        Default file name: "binary_bios_measurements"
    === "Windows"
        Default location: C:\Windows\Logs\MeasuredBoot\ <br>
        Default file name: Not consistent but an example is "0000000059-0000000000.log"

## Event Log Tool Services

The Event Log Tool supports the 
[PC Client RIM Specification :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/tcg-pc-client-reference-integrity-manifest-specification/){:target="_blank"}, 
which specifies the use of the TPM Event Log as a Support RIM type. 
The TPM Event Log is described in the
<span style="white-space: nowrap;">[TCG PC Client Specific Platform Firmware Profile Specification :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/pc-client-specific-platform-firmware-profile-specification/){:target="_blank"}.</span>
The Event Log Tool can: 

- Parse binary TPM Event Logs and displays event data in a human-readable form
- Extract hexidecimal events from TPM Event Logs for test pattern generation
- Provide PCR (Platform Configuration Register) values from a complete TPM Event Log
- Provide details in the case of events failing comparison

!!! info

    The source code can be found on 
    [GitHub :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/tree/main/tools/tcg_eventlog_tool){:target="_blank"}.

## Background and Additional Info

The TPM Event Log is defined in the 
[TCG PC Client Specific Platform Firmware Profile Specification :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/pc-client-specific-platform-firmware-profile-specification/){:target="_blank"}.
The Event Log file contains all the hashes that get extended into the TPM PCRs during the boot cycle, as 
well as details about each hash and each hash's corresponding event. A Verifier can recreate the resultant PCR 
values by extending the values within this file.

The [HIRS ACA](../../hirs-aca.md) uses the Event Log during its validation process if the 
[firmware option](../../started/gs3-hirs-config.md/#configuration-with-the-firmware-check-enabled) is selected. 
The Event Log is one of the Support RIM file options for PC Client systems. This means that the Base RIM 
(SWID tag) file will have a hash of the Event Log in its payload for verification purposes. 

See [Background on UEFI Capturing Boot Info](../../background/operation/op3-boot-capture.md/#firmware) for
more information on how UEFI captures firmware hashes during boot. 
See [Background on HIRS Verification of Firmware](../../background/operation/op4-validation.md/#verify-firmware) 
for more information how HIRS uses this information for verification services.