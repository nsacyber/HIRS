#!/bin/bash

# Calls the the_tcg_event_tool and passes in parameters 
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")") 
baseDir=${scriptDir%/*}
libDir=$baseDir"/lib/"
jar="tcg_eventlog_tool-1.0.jar";
java -jar $libDir$jar "$@"
