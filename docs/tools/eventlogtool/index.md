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

This tool supports the 
[PC Client RIM Specification :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/tcg-pc-client-reference-integrity-manifest-specification/){:target="_blank"}, 
which specifies the use of the TPM Event Log as a Support RIM type. This tool: 

- Parses binary TPM Event Logs and displays event data in a human-readable form
- Extracts Events from TPM Event Logs for test pattern generation
- Provides PCR values from a complete TPM Event Log

!!! info

    The source code can be found on 
    [GitHub :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/tree/main/tools/tcg_eventlog_tool){:target="_blank"}.

