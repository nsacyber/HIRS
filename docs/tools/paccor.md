---
title: PACCOR
---

# PACCOR

The **Platform Attribute Certificate Creator** (PACCOR) is an open source tool used for creating 
and testing Platform Certificates according to the 
[TCG Platform Certificate Profile :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/tcg-platform-certificate-profile/){:target="_blank"}. 

!!! note

    **At this time, if you are using paccor to create platform certificates
    for HIRS ACA validation runs, you will need to use 
    [paccor v1.5.1](https://github.com/nsacyber/paccor/releases/tag/v1.5.1). The
    latest version of paccor will not work with the current version of the ACA.**

A Platform Certificate is an X.509 Attribute Certificate which encapsulates details 
about components on a host and the security standards met by the platform manufacturer. This tool: 

- Creates Platform Certificates according to the Platform Certificate Profile
- Assists in gathering the data in a device to produce a signed attribute certificate
- Validates signatures on TCG [Platform Certificate](../background/inputs/platform-cert.md)

!!! info

    The [PACCOR help page :fontawesome-solid-external-link:](https://nsacyber.github.io/paccor/index.html){:target="_blank"}
    has tutorials for using the tool.

!!! info

    The PACCOR source code can be found on [GitHub :fontawesome-solid-external-link:](https://github.com/nsacyber/paccor){:target="_blank"}.

