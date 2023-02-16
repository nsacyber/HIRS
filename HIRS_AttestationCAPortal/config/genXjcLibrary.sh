#!/bin/bash

dir=$(pwd)
# Relative paths are different when building locally versus on CI
#if [[ "$dir" == *"package"* ]]; then
#  SRC_DIR=$dir/../../../../../../src
#  DEST_DIR=$dir/../src/main/java/
#else
  SRC_DIR=/hirs/HIRS/src/
  DEST_DIR=/hirs/HIRS/src/main/java #/hirs/attestationca/portal
#fi

XSD_FILE=$SRC_DIR/main/resources/swid_schema.xsd

if [ ! -d "$DEST_DIR/hirs/attestationca/portal/utils/xjc" ]; then
  xjc -p hirs.attestationca.portal.utils.xjc $XSD_FILE -d $DEST_DIR -quiet
fi