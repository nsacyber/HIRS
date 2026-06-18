---
title: User Guide (Windows)
---

# Event Log Tool User Guide (Windows)

Currently there is no install package for the Event Log Tool for Windows. It can be invoked using java
from a command shell:

First navigate to the tcg_eventlog_tool folder.

Example 1: 

``` shell
java -jar build\libs\tcg_eventlog_tool-1.0.jar -h
```

Example 2:

``` shell
java -jar build\libs\tcg_eventlog_tool-1.0.jar -f C:\Windows\Logs\MeasuredBoot\0000000059-0000000000.log -e
```