#!/bin/bash
set -e

# Check packager OS is Centos
OS_NAME=$(awk -F= '/^NAME/{print $2}' /etc/os-release)
if [ "$OS_NAME" != "\"CentOS Linux\"" ] && [ "$OS_NAME" != "\"Amazon Linux\"" ] ; then
    echo "Error: RPMs must be built with CentOS or Amazon Linux"
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

# Build HIRS_ProvisionerTPM2 RPM
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

# Move HIRS_ProvisionerTPM2 RPM
rm -f ../../../package/rpm/RPMS/x86_64/HIRS_Provisioner_TPM_2_0*.rpm
if [ ! -d "../../../package/rpm/RPMS/x86_64" ]; then
    mkdir -p ../../../package/rpm/RPMS/x86_64
fi
mv *.rpm ../../../package/rpm/RPMS/x86_64
