#!/bin/bash

# Calls the tcgRIMTool and passes in parameters
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
baseDir=${scriptDir%/*}
jar="tcgRIMTool-1.0.jar";
java -jar $baseDir/$jar "$@"

