#!/bin/bash

dir=$(pwd)
# Relative paths are different when building locally versus on CI
if [[ "$dir" == *"package"* ]]; then
  SRC_DIR=$dir/../../../../../../HIRS_Utils/src
  DEST_DIR=$dir/../src/main/java/
else
  SRC_DIR=../../HIRS_Utils/src
  DEST_DIR=../src/main/java/
fi

XSD_FILE=$SRC_DIR/main/resources/swid_schema.xsd

xjc -p hirs.utils.xjc $XSD_FILE -d $DEST_DIR -quiet