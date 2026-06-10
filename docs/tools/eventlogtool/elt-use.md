---
title: User Guide (Linux)
---

# Event Log Tool User Guide (Linux)

The Event Log Tool rpm install will create a command line shortcut. This can be invoked from a command 
line by using:

   ```shell
   elt –h
   ```

Invoking this command will bring up a help page, which lists out the Event Log Tool’s many uses
and functions.

## Parameters

### -f: --file

* Use a specific Event Log file. The following parameter MUST be a path and file name.
* The local Event Log file will be used if this option is not present.
* Note: Access to the local Event Log may require admin privileges.

### -e: --event

* Display event descriptions (including event content) in human-readable form.
* The following optional parameter is a single event number used to filter the output. 
* All events will be displayed if the optional parameter is not provided.

### -ec: --contenthex

* Displays event content in eventhex format when -event is used.

### -ex: --eventhex

* Displays event in hex format when -event is used.

### -d: --diff

* Compares two TPM Event Logs and outputs a list of events of the second log that differed.

### -o: --output

* Output to a file. The following parameter MUST be a relative path and file name.

### -p: --pcr

* Output expected PCR value calculated from the TCG Log (for PCR Replay).
* The following parameter MAY be a PCR number used to specify a single PCR.
* No following parameters will display all PCRs.

### -v: --version

* Parser version.

### -x: --hex

* Displays an event in hex format. Use with -ec to get content.
* Use -e -ec and -ex options to filter output.
* If not present, all output will be human-readable form.

## Event Log Structure

!!! note

    The default location for the TPM Event Log is:

    === "Linux"
        /sys/kernel/security/tpm0/ with a default name of "binary_bios_measurements"
    === "Windows"
        C:\Windows\Logs\MeasuredBoot\ 

The format of the Event Log file is as follows:

* **pcrIndex**: The PCR Register number, typically shown in documentation as PCR[0], where 0 is the pcrIndex.

* **eventType**: An enumerated type found in Table 27 of the 
[PFP :fontawesome-solid-external-link:](https://trustedcomputinggroup.org/resource/pc-client-specific-platform-firmware-profile-specification/){:target="_blank"}
(Version 1.06 Revision 52). 
The PFP uses upper case labels to reference the events (e.g. event type 0x00000007 is labeled EV_S_CRTM_CONTENTS).

* **digests**: This is a hash value (SHA1, SHA256 or SHA384 depending upon the log type). This may be a hash of firmware, a file, or the event itself. The coverage of the digest is dictated by Table 27 of the PFP.

* **eventSize**: The size (in bytes) of the event data.

* **event**: The event data as described by Table 27 of PFP.

!!! note

    In HIRS, the Event# is not part of the TPM Event Log but is useful to display for 
    identification purposes.

## Displaying Events

### Displaying All Events

### Displaying Only One Event

## Outputting Event Log Information to a File

## Displaying Information in Hex Format

### Displaying An Event in Hex Format

### Displaying An Event in Hex Format With Additional Context

### Displaying Event Content in Hex Format With Additional Context

## Displaying Expected PCR Values

## Comparing Event Log Files

## 

