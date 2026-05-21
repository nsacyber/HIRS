---
title: ACA Logs
---

# ACA Logs

A log of the HIRS ACA install can be found here:

```shell
/var/log/hirs/hirs_aca_install_<date-of-install>.log
```

A current day-of log of the HIRS ACA operation can be found here:

```shell
/var/log/hirs/HIRS_AttestationCA_Portal.log
```

ACA operation logs from previous days can be found here:

```shell
/var/log/hirs/HIRS_AttestationCA_Portal-<date>.log
```

!!! note

    Reminder that if your ACA is inside a docker container, the logs exist in the 
    ACA container, not in the device that the container is running on.
