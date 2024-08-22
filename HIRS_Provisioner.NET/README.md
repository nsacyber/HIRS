<h1><center>HIRS Provisioner.NET<BR\></center></h1>

The HIRS Provisioner.NET is an application that can leverage a machine and its TPM to:

   * verify system attributes (as chosen in the ACA policy)
   * request and store an Attestation Identity Certificate and/or a LDevID Certificate
   
The HIRS Provisioner.NET application, along with the HIRS ACA, will perform the following high level tasks
during the provision process. Please refer to appendix B for further details:
• The HIRS Provisioner retrieves the EK Certificate from the TPMs NVRAM.
• The HIRS Provisioner retrieves the Platform Certificate from the EFI partition, if present.
• The HIRS Provisioner retrieves the Reference Integrity Manifest (RIM) from the EFI partition, if present.
• The HIRS Provisioner retrieves the TPM Event Log.
• The HIRS Provisioner retrieves Component data from the device.
• An Attestation Identity Key is generated on the TPM, if one is not already present.
• The HIRS Provisioner forwards the collected data and sends it to the ACA.
• The HIRS ACA (Policy based) validates the Endorsement Credential.
• The HIRS ACA (Policy based) validates the Platform Credential(s).
• The HIRS ACA (Policy based) validates and new RIM(s)
• The performs credential validation according to its policy
• If validation is successful, the ACA issues an Attestation Identity Credential or LocalDevID (Policy based) to the device.

For installation, setep, and usage please refer to the [HIRS_Provisioner.NET Readme](https://github.com/nsacyber/HIRS/blob/master/HIRS_AttestationCAPortal/src/main/webapp/docs/HIRS%20.NET%20Provisioner%20Readme_2.2.pdf)