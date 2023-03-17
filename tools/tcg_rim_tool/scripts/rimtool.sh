#!/bin/bash

# Calls the the_tcg_rim_tool and passes in parameters
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
baseDir=${scriptDir%/*}
libDir=$baseDir"/lib/"
jar="tcg_rim_tool-*.jar";
java -jar $libDir$jar "$@"