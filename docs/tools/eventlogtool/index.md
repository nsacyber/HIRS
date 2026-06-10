---
title: Event Log Tool
---

# Event Log Tool

The **Event Log Tool** is a command line application that allows a user to inspect the TPM 
Event Log's contents. This tool supports the 
[PC Client RIM Specification :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/tcg-pc-client-reference-integrity-manifest-specification/){:target="_blank"}, 
which specifies the use of the TPM Event Log as a Support RIM type. This tool: 

- Parses binary TPM Event Logs and displays event data in a human-readable form
- Extracts Events from TPM Event Logs for test pattern generation
- Provides PCR values from a complete TPM Event Log

!!! info

    The source code can be found on 
    [GitHub :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/tree/main/tools/tcg_eventlog_tool){:target="_blank"}.
