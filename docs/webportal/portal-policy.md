---
title: Policy
---

# ACA Portal: Policy Page

The **Policy** page is used to provide configuration settings for attestation provisioning for the system. 
The default for the ACA is to not check any credentials or attributes for TPM provisioning. 
This initial setting is intended to:

1. Test the proper installation of HIRS, with no validation of supply chain credentials performed.
2. Support TPM provisioning of systems that might not be delivered with supply chain credentials.

<img src= "../../images/portal-policy-default.png" alt="HIRS Policy Page" style="border: 2px solid grey;">

**Endorsement Certificate Validation**: If selected, the ACA will validate the Endorsement Certificate
prior to issuing an Attestation Certificate. The default is ‘Disabled’.

**Platform Certificate Validation**: If selected, the ACA will validate the Platform Certificate
prior to issuing an Attestation Certificate. This option only validates the Certificate itself,
not the attributes within the Platform Certificate. Endorsement Certificate Validation is required
to be enabled prior to enabling this policy option. The default is ‘Disabled’.

**Platform Attribute Certificate Validation**: If selected, the ACA will validate the Platform
Certificate Attributes prior to issuing an Attestation Certificate. This option only validates
the Certificate Attributes, not the Platform Certificate. Platform Certificate Validation is
required to be enabled prior to enabling this policy option. The default is ‘Disabled’.

**Firmware Validation**: If selected, the ACA will validate firmware prior to issuing an
Attestation Credential. The TCG-defined artifacts necessary for this validation are:

- RIM
- Event Log (log file produced by UEFI)
- TPM Quote and PCR list
- Platform Certificate issued by the OEM, System Integrator or Value-Added Reseller
- Endorsement Credential linked to Platform Certificate
- Certificate chain of the organization that produced the Platform Certificate
- Certificate chain of the organization that produced the RIM

**Ignore IMA PCR Entry**: If selected, the ACA will ignore the IMA PCR Entry prior to issuing
an Attestation Certificate. Firmware Validation is required to be enabled prior to enabling
this policy option.

**Ignore TBOOT PCRs Entry**: If selected, the ACA will ignore the TBOOT PCRs Entry prior to
issuing an Attestation Certificate. Firmware Validation is required to be enabled prior to
enabling this policy option.

**Ignore GPT PCRs Entry**: If selected, the ACA will ignore the GPT PCRs Entry prior to issuing
an Attestation Certificate. Firmware Validation is required to be enabled prior to enabling
this policy option.

**Generate Attestation Certificate**: If selected, the ACA will conditionally generate an
Attestation Certificate after a successful TPM provisioning.

**Generate LDevID Certificate**: If selected, the ACA will conditionally generate a Local
Device ID (LDevID) certificate after a successful TPM provisioning.

**Attestation Certificate Validity period**: If selected, the ACA will have an Attestation
Certificate Validity period of the input number of days. ```Generate Attestation Certificate```
is required to be enabled prior to enabling this option. ```Attestation Certificate Validity
period``` being enabled automatically causes ```Attestation Certificate Renewal period``` to become
enabled. If ```Attestation Certificate Renewal period``` is disabled, this will also disable
```Attestation Certificate Validity period```.

**Attestation Certificate Renewal period**: If selected, the ACA will renew the input ```n```
number of days before the Attestation Certificate’s ‘Not After’ validity date which has
a default of 365 days. ```Generate Attestation Certificate``` is required to be enabled prior
to enabling this option. ```Attestation Certificate Validity period``` being enabled automatically
causes ```Attestation Certificate Renewal period``` to become enabled. If ```Attestation Certificate
Validity period``` is disabled, this will also disable ```Attestation Certificate Renewal period```.

!!! note

    The recommended policy setting for Trusted Computing-based supply chain validation will 
    require these policy settings to be set to ```enabled```:

    - Endorsement Certificate Validation: Enabled
    - Platform Certificate Validation: Enabled
    - Platform Attribute Certificate Validation: Enabled
    - Firmware Validation: Enabled

!!! note

    Additional Info: 

    Firmware Validation should only be set to ```enabled``` if the device manufacturer supports RIMs.

    The IMA policy option refers to IMA which is a Linux feature that utilizes PCR10. Selecting
    this option will cause the ACA to skip evaluation of PCR10.

    The TBOOT policy option refers to the TBOOT which is a Linux feature that utilizes PCR17+. 
    Selecting this option will cause the ACA to skip evaluation of PCR17+.

    Selecting the GPT policy option will cause the ACA to skip evaluation of events of type EV_EFI_GPT_EVENT.

    The default for the Attestation Certificate Validity period policy option should be 365 days.

    The default renewal for the ```Attestation Certificate Renewal period``` policy option should be 365 days 
    before the ‘Not After’ validity date. 