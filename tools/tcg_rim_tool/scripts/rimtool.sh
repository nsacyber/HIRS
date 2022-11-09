#!/bin/bash

# Calls the the_tcg_rim_tool and passes in parameters
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
baseDir=${scriptDir%/*}
jar="tcg_rim_tool.jar";
java -cp $baseDir/lib/$jar:$baseDir/lib/* hirs.swid.Main "$@"

