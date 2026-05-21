---
title: 3. Configure HIRS
---

# Configure the HIRS validation variables

HIRS is configured directly through the [ACA Web Portal](../webportal/index.md). This 
section steps through a few sample configurations. 

!!! note

    Configuration of the ACA can be done before or after installing the 
    Provisioner, but it must be done prior to running the Provisioner.

## Get familiar with the Validation Reports page

If you are new to HIRS, it would be helpful to become familiar with the 
[Validation Reports](../webportal/portal-validation-reports.md) page. If no validations 
have been run yet, there will be nothing listed in the results table.  Note that any  
validations that have been run will persist in the database even if the ACA is uninstalled 
and reinstalled. 

## Set the level of logging desired

On the [Help](../webportal/portal-help.md) page, you can change the log level depending on how much detail you would 
like recorded in the logs. The default level is `Info`.

## 'Blank' configuration - software install check

After a fresh installation of the ACA and/or Provisioner, it can be useful to
configure the ACA with all credential validations disabled and 
all outputs disabled to ensure that nothing went wrong with the ACA or 
Provisioner installation. Note that this will not actually check anything 
in terms of validation; it is simply an install-sanity check.

On the [Policy](../webportal/portal-policy.md) page, ensure the following credential validations are 
configured as follows:

<table style="margin-left: 40px;">
  <tr><td>Endorsement Credential Validation: Disabled<br>
  Platform Credential Validation: Disabled<br>
  Firmware Validation: Disabled
</td></tr>
</table>

Ensure the following outputs are configured as follows:

<table style="margin-left: 40px;">
  <tr><td>Generate Attestation Certificate: Disabled<br>
  Generate LDevID Certificate: Disabled
</td></tr>
</table>

In this case you can skip the [artifacts](../started/gs4-artifacts.md) stage.
After the [Provisioner install](../started/gs5-prov-install.md) stage and 
[Provisioner run](../started/gs6-prov-run.md) stage, the 
[validation result](gs7-results.md) should be 
successful if the software was installed properly.

## Select the input configuration

To configure the inputs that the ACA will validate, select one of the 
following options and follow the instructions.

### Configuration with only the Endorsement Certificate

The Endorsement Certificate is used as an assertion of identity and authenticity 
of the TPM. The TPM is the sole entity with the private key that matches the 
public key of its Endorsement Certificate. There are various reasons you may 
want to test a validation of the Endorsement Certificate only:

* Most modern computers come with a TPM and Endorsement Certificate, and usually 
you can obtain the certificate chain, so this can be an easy initial test.
* If you do not have a Platform Certificate or its trusted chain, 
the Endorsement/TPM check will be the highest level validation you can perform.

On the [Policy](../webportal/portal-policy.md) page, ensure the following:

<table style="margin-left: 40px;">
  <tr><td>Endorsement Credential Validation: Enabled<br>
  Platform Credential Validation: Disabled<br>
  Firmware Validation: Disabled
</td></tr>
</table>

### Configuration with the Endorsement and Platform Certificates

To validate the hardware but not include checks of the firmware, 
you will need to enable the Platform check. The Platform
Certificate is used as the `Assertion` for the measured `Evidence` 
of the hardware. This check also requires that you have Endorsement 
checked, as the hardware validation requires a validated TPM. 

On the [Policy](../webportal/portal-policy.md) page, ensure the following:

<table style="margin-left: 40px;">
  <tr><td>Endorsement Credential Validation: Enabled<br>
Platform Credential Validation: Enabled<br>
Firmware Validation: Disabled
</td></tr>
</table>

!!! info

    For more information on the specific options under Platform Certificate Validation, 
    see the [Portal Policy Guide](../../webportal/portal-policy).

### Configuration with the Endorsement and Platform Certificates and the RIM

This configuration is the recommended report policy for supply chain validation 
as it checks the validity of the TPM, the hardware, and the firmware. The RIM is 
used as the `Assertion` for the measured `Evidence` of the firmware.

On the [Policy](../webportal/portal-policy.md) page, ensure the following:

<table style="margin-left: 40px;">
  <tr><td>Endorsement Credential Validation: Enabled<br>
Platform Credential Validation: Enabled<br>
Firmware Validation: Enabled
</td></tr>
</table>

!!! info

    For more information on the specifc options under Firmware Validation, 
    see the [Portal Policy Guide](../../webportal/portal-policy).

## Select the output configuration

To configure the outputs that the ACA will create, the following are options. 
One or both can be enabled.

### Configuration with Attestation Certificate

On the [Policy](../webportal/portal-policy.md) page, ensure the following outputs are configured as follows:

<table style="margin-left: 40px;">
  <tr><td>Generate Attestation Certificate: Enabled
</td></tr>
</table>

### Configuration with LDevID

On the [Policy](../webportal/portal-policy.md) page, ensure the following outputs are configured as follows:

<table style="margin-left: 40px;">
  <tr><td>Generate LDevID Certificate: Enabled<br>
</td></tr>
</table>
