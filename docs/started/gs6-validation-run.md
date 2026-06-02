---
title: 6. Run the Validation
---

# 6. Run the Validation

Running the Validation is the same thing as running the Provisioner software. 
Upon running the Provisioner, the Provisioner will gather the required material, send 
this to the ACA, and the ACA will perform validation services based on the configuration 
set in the ACA Web Portal. Finally, the validation result will be available on 
the ACA Web Portal. 

## Connect ACA and Provisioner to same network

If you are running the ACA on a different device than the Provisioner, connect 
the two devices with the Ethernet cable. Ensure the two devices are on the 
same network.

## Run Provisioner software

For details on running the Provisioner,
refer to the [Provisioner Run](../install/validation-run.md) page. 

As the Provisioner runs, it will print information in the shell 
that displays the various stages of the process, which helps to identify 
if and where failures occur. The Provisioner will also print whether the 
validation failed or succeeded.

## Provisioner logs

If there are any issues during the Provisioner run, the 
[Provisioner Logs](../install/logs/prov-logs.md) can be helpful. Note that for the default configuration,
the log file will appear in the directory that the HIRS .NET Provisioner 
is executed from, so you may want to view this prior to exiting the 
Provisioner environment. 
 