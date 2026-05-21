---
title: Validation Reports
---

# ACA Portal: Validation Reports Page

The **Validation Reports** page indicates the status of previous Attestation Certificate Requests 
from HIRS TPM Provisioners.

The following is an image with one validation report. 

<img src= "../../images/portal-validation-default.png" alt="Portal Platform Cert page" style="border: 2px solid grey;">

The `Credential Validation` columns are populated only if the 
[ACA policy](portal-policy.md) included those items at the time the validation was 
run. In the example above, the endorsement, platform and firmware
checks were not included during this run as there is nothing listed in those columns.
(Note: often this 'default' run with no credential validations
is used to test installation of the software.)
The validation is successful as seen by the green checkmark under `Result`.

The following example shows a validation report for a run that included
endorsement, platform, and firmware validation. This configuration is the 
recommended report policy for supply chain validation.

<img src= "../../images/portal-validation.png" alt="Portal Platform Cert page" style="border: 2px solid grey;">

You can download any of these reports using the
<img src="../../images/portal-download.png" alt="clipboard" width="20" height="20" style="vertical-align:middle;">
icon.
