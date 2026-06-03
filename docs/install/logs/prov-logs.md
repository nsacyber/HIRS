---
title: Provisioner Logs
---

# Provisioner Logs

The HIRS .NET Provisioner uses
[Serilog :fontawesome-solid-external-link:](https://github.com/serilog/serilog-settings-configuration){:target="_blank"}
for logging. The log file contains detailed information about the HIRS provisioning process.

A standard configuration is defined in the 
[appsettings.json file](../prov/prov-config.md/#path). 
In this standard configuration, the log file will appear in the directory that 
the HIRS .NET Provisioner is executed from. The log name is formatted:

```shell
hirs<date>.log
```
