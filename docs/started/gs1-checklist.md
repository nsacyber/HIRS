---
title: 1. Checklist
---

# Checklist for Getting Started

Ensure you have the following in order to install and run HIRS:

<style>
  .md-typeset table {
    max-width: 500px !important;
  }
  .md-typeset table:not([class]) th:nth-child(1),
  .md-typeset table:not([class]) td:nth-child(1) {
    padding-right: 0 !important;
    text-align: center; 
  }
  .md-typeset table:not([class]) th:nth-child(2),
  .md-typeset table:not([class]) td:nth-child(2) {
    padding-left: 0 !important;
  }
  .md-typeset table:not([class]) th:nth-child(2) {
    transform: translateX(110%) !important;
    display: inline-block !important; /* Required for transform to work on header text */
    white-space: nowrap !important;   /* Prevents text from breaking into two lines when shifted */
  }
</style>
|                                 | Checklist                                                                                                                  |
|---------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| :computer:                      | Physical platform (laptop, desktop or server) to run the Provisioner on (eg. the device you are validating)                |
| :material-desktop-tower:        | Platform (laptop, desktop or server) to install the ACA server if your ACA and Provisioner are running on separate devices |
| :material-ethernet-cable:       | Ethernet cable to connect the ACA and Provisioner if your ACA and Provisioner are running on separate devices              |
| :fontawesome-regular-file-code: | Access to [Provisioner code or install package](gs5-prov-install.md)                                                       |
| :material-file-code:            | Access to [ACA code or install package](gs2-aca-install.md)                                                                |
| :scroll:                        | [Artifacts](gs4-artifacts.md) required depending on your configuration                                                     |
| :material-certificate:          | Complete certificate chain for each artifact                                                                               |

!!! note

    The ACA can be installed on either the device you are provisioning, or on a 
    separate device. Typically in a real-world scenario the ACA server would be 
    installed on a separate device, but for testing purposes it can be run on the 
    same device as the Provisioner.

!!! note

    You can provision multiple platforms at the same time with a single ACA server. They 
    must all be on the same network.




