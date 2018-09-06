#!/bin/bash

# script that pulls version information from git for populating the portal dispalyed version,
# RPM file names, and RPM embedded version information

# script should be invoked with 'source' so that the variables are in the scope of the caller

GIT_HASH=`git rev-parse HEAD | head -c6`
VERSION=`cat $SCRIPT_DIR/../VERSION`
GIT_COMMIT_UNIX_TIMESTAMP=`git show -s --format=%ct | xargs echo -n`
RELEASE="$((GIT_COMMIT_UNIX_TIMESTAMP)).$GIT_HASH"
DISPLAY_VERSION="$VERSION.$GIT_COMMIT_UNIX_TIMESTAMP.$GIT_HASH"

echo "Building version:"
echo "VERSION: $VERSION"
echo "GIT_COMMIT_UNIX_TIMESTAMP: $GIT_COMMIT_UNIX_TIMESTAMP"
echo "RELEASE: $RELEASE"
echo "DISPLAY_VERSION: $DISPLAY_VERSION"
