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

In order to display all events within a specified log file, you will need an Event Log file.
Typically the filetype would be a .bin. See [] for an example test patterns.

Once you have this file, you can input it into this command for results:

``` shell
elt -f TpmLog.bin -e
```

** pic

### Displaying Only One Event

If you would like to display only one event from an Event Log, you can use:

   ``` shell
   elt -f TpmLog.bin -e 1
   ```

For this example, Event #1 was used.

** pic

## Outputting Event Log Information to a File

If you would like to output information from the tcg_eventlog_tool to an external file for 
use later, you can use the -o option as below:

   ``` shell
   elt -f TpmLog.bin -p 0 -o example.txt
   ```

In this case, the query information about the TpmLog.bin file was saved to a new text 
file named example.txt.

Using cat example.txt shows that the information queried above from elt –f 
TpmLog.bin -p 0 was saved to the example.txt file that was created:

** pic

## Displaying Information in Hex Format

### Displaying An Event in Hex Format

If you would like to display an event from the tcg_eventlog_tool in a hex format, you 
can use the –x option like this:

   ``` shell
   elt -f TpmLog.bin -e 1 –x
   ```

In this example, Event #1 is transcribed into hex format:

** pic

### Displaying An Event in Hex Format With Additional Context

If you would like to display an event in hex format with additional context but no 
content information, you can use the -ex option like this:

   ``` shell
   elt -f TpmLog.bin -e 1 -ex
   ```

In this example, Event #1 is transcribed into hex format:

** pic

### Displaying Event Content in Hex Format With Additional Context

If you would like to display an event with content information in hex format with 
additional context, you can use the -ec option like this:

   ``` shell
   elt -f TpmLog.bin -e 1 -ec
   ```

In this example, Event #1 and its content have been transcribed into hex format:

** pic

## Displaying Expected PCR Values

If you would like to view all expected PCR Values of an Event Log, you can use 
the -p option as below:

   ``` shell
   elt -f TpmLog.bin -p
   ```

** pic

## Comparing Event Log Files

If you would like to compare Event Log files to see where certain events may have 
failed comparison, you can use this command:

   ``` shell
   elt -d TpmLog.bin TPMLog_Altered.bin -p
   ```

The two files being compared in this example are TpmLog.bin and TPMLog_Altered.bin.

** pic

As you can see above, the Event Logs had 2 event mismatches. Since a mismatch has 
occurred, this could mean that the digest values within the Event Log are not 
verifiable and may have been tampered with. 

