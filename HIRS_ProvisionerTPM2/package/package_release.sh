#!/bin/bash

# NOTE: This script uses many of the same commands as the build_version_helper.sh found under the top level
# 'package' directory

# script that pulls version information from git for populating the TPM2 Provisioner release information
# with regards to packaging

# script is to be invoked in CMake for loading into build process

GIT_HASH=`git rev-parse HEAD | head -c6`
GIT_COMMIT_UNIX_TIMESTAMP=`git show -s --format=%ct | xargs echo -n`
RELEASE="$((GIT_COMMIT_UNIX_TIMESTAMP)).$GIT_HASH"

echo "$RELEASE"
