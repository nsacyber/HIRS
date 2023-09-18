#!/bin/bash

# Script to generate protobuf Java code. Called by gradle to compile the
# protobuf spec file to Java source. Generates the file
# hirs/attestationca/configuration/provisionerTpm2/ProvisionerTpm2.java.

dir=$(pwd)
# Relative paths are different when building locally versus on CI
if [[ "$dir" == *"package"* ]]; then
  SRC_DIR=$dir/../../../../../../HIRS_ProvisionerTPM2/src
  DEST_DIR=$dir/../src/main/java
else
  SRC_DIR=../../HIRS_ProvisionerTPM2/src
  DEST_DIR=../src/main/java
fi
protoc -I=$SRC_DIR --java_out=$DEST_DIR $SRC_DIR/ProvisionerTpm2.proto
