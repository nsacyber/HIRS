To support the [PC Client RIM Specification](https://trustedcomputinggroup.org/wp-content/uploads/TCG_PC_Client_RIM_r0p15_15june2020.pdf) which utilizes the TPM Event Log as a Support RIM type , it was useful to 
have a tool for inspecting the contents of the [TPM event log](https://github.com/nsacyber/HIRS/wiki/TPM-Event-Logs). A Linux command line tool named "elt" (event log tool) has been 
created to parse and print human readable output, provide hedicimal evnts which can be used as test patterns, and to 
compare event logs for providing details on what events miscompared. 

# Building
To build this tool navigate to the tcg_eventlog-tool directory and use the following commmand: 
> ./gradlew clean build

To create an RPM on a linux device use the following command in the dame directory:
> ./gradlew builRPM

To install this tool use the following commmand from the same directory:
> sudo yum localinstall build/distrobutions/tgc_rim_tool.*.rpm

#Usage
The tcg_eventlog_tool can be invoked using the elt command has various command line options to view all, specific events,
or specific PCRs. 
Current options for the tool can be found using the -h option:

> elt -h

With No FILE the default event log path (e.g. /sys/kernel/security/tpm0/binary_bios_measurements o  Linux) is used.
 Note admin privileges are required for accessing the default path in Linux.
All OPTIONS must be separated by a space delimiter, no concatenation of OPTIONS is currently supported.

An example output for the tcg_eventlog_tool filtering on event 1 would be:
> elt -f ~/TpmLog.bin -e 1
