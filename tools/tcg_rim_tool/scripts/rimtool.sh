#!/bin/bash

# Calls the the_tcg_rim_tool and passes in parameters
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
baseDir=${scriptDir%/*}
jar="tcg_rim_tool-2.0.0.jar";
java -jar $baseDir/$jar "$@"

