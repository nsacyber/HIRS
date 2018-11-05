#!/bin/bash
set -e

# Check packager OS is Ubuntu
OS_NAME=$(awk -F= '/^NAME/{print $2}' /etc/os-release)
if [ "$OS_NAME" != "\"Ubuntu\"" ]; then
    echo "Error: DEBs must be built with Ubuntu"
    exit 1
fi

# Find package directory
cd $( dirname "${BASH_SOURCE[0]}" )

# Ensure clean build environment
shopt -s extglob
# Delete everything but downloaded dependencies
rm -rf BUILD/!(lib)
shopt -u extglob

# Make BUILD directory if it doesn't already exist
if [ ! -d "BUILD" ]; then
    mkdir BUILD
fi

# Navigate to build directory
cd BUILD

# Build HIRS_ProvisionerTPM2 DEB
cmake ../..

# If the current directory is empty, there may be an existing CmakeCache.txt
# file that prevents cmake from building in the current directory
if ! [ "$(ls -A)" ]; then
    echo "CMake failed to generate files in the target directory. Is there "
    echo "an existing CMakeCache.txt file in the CMake source directory? "
    echo "If so, delete it."
    exit 1
fi
make
cpack

# Move HIRS_ProvisionerTPM2 DEB
rm -f ../../../package/deb/DEBS/HIRSProvisionerTPM2.0*.deb
if [ ! -d "../../../package/deb/DEBS" ]; then
    mkdir -p ../../../package/deb/DEBS
fi
mv *.deb ../../../package/deb/DEBS
