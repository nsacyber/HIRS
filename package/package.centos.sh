#!/bin/bash

set -e

# Builds the centos 6/7 package for HIRS. This script can be passed a list of arguments that are relative paths to plugin script files.
# The plugin script files are provided the destination directory of where to put the plugin jar file.

# argument $1: Extra package name addendum string
# argument $2 to end: plugin script dirs.

# store the initial directory so this script can concatenate the relative paths specified for the plugin scripts
INITIAL_DIR=`pwd`

# Enter package directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $SCRIPT_DIR/rpm

# Set variables
RPM_BUILD_DIR=`pwd`
# assign build version vars
source $SCRIPT_DIR/build_version_helper.sh

PLUGIN_SOURCE="$RPM_BUILD_DIR/PLUGIN_SOURCE"
PACKAGE_NAME_ADDENDUM="$1"
RPM_EXTRA_CLIENT_DEPENDENCIES="$2"
RPM_EXTRA_SERVER_DEPENDENCIES="$3"

# prepend comma on the extra dependency lists so it can be added to spec file as is, but only if there are
# extra dependencies (not empty)
if [ ! -z "$RPM_EXTRA_CLIENT_DEPENDENCIES" ]; then
    RPM_EXTRA_CLIENT_DEPENDENCIES=", $RPM_EXTRA_CLIENT_DEPENDENCIES"
fi
if [ ! -z "$RPM_EXTRA_SERVER_DEPENDENCIES" ]; then
    RPM_EXTRA_SERVER_DEPENDENCIES=", $RPM_EXTRA_SERVER_DEPENDENCIES"
fi

echo "extra client dependencies:$RPM_EXTRA_CLIENT_DEPENDENCIES"
echo "extra server dependencies:$RPM_EXTRA_SERVER_DEPENDENCIES"

# Clear old builds
rm -rf RPMS SRPMS $PLUGIN_SOURCE

# Create directories
mkdir -p BUILD BUILDROOT RPMS SOURCES SPECS SRPMS $PLUGIN_SOURCE


# build all plugins starting with fourth argument.
echo "Building plugins into $PLUGIN_SOURCE"
for plugin_script in "${@:4}"
do
	# convert argument to absolute path if necessary
    if [ "${plugin_script:0:1}" = "/" ]; then
        plugin_abs_path_script=$plugin_script
    else
        plugin_abs_path_script="$INITIAL_DIR/$plugin_script"
    fi
    echo "Building Plugin: $plugin_abs_path_script"
    $plugin_abs_path_script $PLUGIN_SOURCE
    plugin_return_code=$?

    if [ $plugin_return_code -ne 0 ]; then
        echo "Failed to build plugin $plugin_abs_path_script. Aborting"
        exit -1
    fi
done

# Move specs & sources
cp *.spec SPECS

# Copy sources for TPM Module
tar -c -f SOURCES/tpm_module-$GIT_HASH.tar --exclude='dist' --exclude='build' ../../tpm_module/ --transform s/tpm_module/tpm_module-$GIT_HASH/
tar --append -f SOURCES/tpm_module-$GIT_HASH.tar ../../NOTICE

# Build RPM for TPM Module
rpmbuild --nodeps -ba SPECS/tpm-module.spec --define '_topdir '$RPM_BUILD_DIR  --define 'VERSION '$VERSION --define 'RELEASE '$RELEASE --define 'GIT_HASH '$GIT_HASH || { echo 'Failed to package tpm_module'; exit 1; }
echo '************************************************************************************'
echo 'TPM Module RPM successfully built'
echo '************************************************************************************'

# Copy sources
tar -c -f SOURCES/HIRS-$GIT_HASH.tar ../../settings.gradle ../../build.gradle ../../VERSION ../../gradle.properties ../../gradlew ../../gradle/
tar --append -f SOURCES/HIRS-$GIT_HASH.tar --exclude='build' ../../HIRS_AttestationCA
tar --append -f SOURCES/HIRS-$GIT_HASH.tar --exclude='build' ../../HIRS_AttestationCAPortal
tar --append -f SOURCES/HIRS-$GIT_HASH.tar --exclude='build' ../../HIRS_Provisioner
tar --append -f SOURCES/HIRS-$GIT_HASH.tar --exclude='build' ../../HIRS_Structs
tar --append -f SOURCES/HIRS-$GIT_HASH.tar --exclude='build' ../../HIRS_Utils
tar --append -f SOURCES/HIRS-$GIT_HASH.tar --exclude='build' ../../TPM_Utils
tar --append -f SOURCES/HIRS-$GIT_HASH.tar --exclude='build' ../../tools/tcgRIMTool

# copy includes directory into release TAR
tar --append -f SOURCES/HIRS-$GIT_HASH.tar ../extras/
tar --append -f SOURCES/HIRS-$GIT_HASH.tar ../scripts/
tar --append -f SOURCES/HIRS-$GIT_HASH.tar ../../NOTICE

# Build HIRS CentOS6 RPMs. Provides PLUGIN_SOURCE variable to gradle task.
if [ -z "$ONLY_BUILD_EL7_RPMS" ]; then
    echo "Building CentOS6 RPMs..."
    rpmbuild --nodeps -ba SPECS/HIRS.spec --define 'build6 1' --define 'dist .el6' --define '_topdir '$RPM_BUILD_DIR  --define 'VERSION '$VERSION --define 'RELEASE '$RELEASE --define 'GIT_HASH '$GIT_HASH --define 'DISPLAY_VERSION '$DISPLAY_VERSION --define 'PLUGIN_SOURCE '$PLUGIN_SOURCE --define 'PACKAGE_NAME_ADDENDUM '$PACKAGE_NAME_ADDENDUM --define 'RPM_EXTRA_CLIENT_DEPENDENCIES '"$RPM_EXTRA_CLIENT_DEPENDENCIES" --define 'RPM_EXTRA_SERVER_DEPENDENCIES '"$RPM_EXTRA_SERVER_DEPENDENCIES"|| { echo 'Failed to package HIRS'; exit 1; }
    echo '************************************************************************************'
    echo 'HIRS CentOS6 RPMs successfully built'
    echo '************************************************************************************'
else
    echo "Skipping building CentOS6 RPMs because of ONLY_BUILD_EL7_RPMS environment variable"
fi


# Cleanup before CENTOS 7 build
rm -rf BUILD BUILDROOT

## Build HIRS CentOS7 RPMs. Provides PLUGIN_SOURCE variable to gradle task
if [ -z "$ONLY_BUILD_EL6_RPMS" ]; then
    rpmbuild --nodeps -ba SPECS/HIRS.spec --define 'build7 1' --define 'dist .el7' --define '_topdir '$RPM_BUILD_DIR  --define 'VERSION '$VERSION --define 'RELEASE '$RELEASE --define 'GIT_HASH '$GIT_HASH --define 'DISPLAY_VERSION '$DISPLAY_VERSION --define 'PLUGIN_SOURCE '$PLUGIN_SOURCE --define 'PACKAGE_NAME_ADDENDUM '$PACKAGE_NAME_ADDENDUM --define 'RPM_EXTRA_CLIENT_DEPENDENCIES '"$RPM_EXTRA_CLIENT_DEPENDENCIES" --define 'RPM_EXTRA_SERVER_DEPENDENCIES '"$RPM_EXTRA_SERVER_DEPENDENCIES"|| { echo 'Failed to package HIRS'; exit 1; }
    echo '************************************************************************************'
    echo 'HIRS CentOS7 RPMs successfully built'
    echo '************************************************************************************'
else
    echo "Skipping building CentOS7 RPMs because of ONLY_BUILD_EL6_RPMS environment variable"
fi

# Cleanup
rm -rf BUILD BUILDROOT SOURCES SPECS $PLUGIN_SOURCE

# Build RPM for HIRS_ProvisionerTPM2
$SCRIPT_DIR/../HIRS_ProvisionerTPM2/package/package.tpm2.centos7.sh
if [ -f RPMS/x86_64/HIRS_Provisioner_TPM_2_0*.rpm ]; then
    echo '************************************************************************************'
    echo 'HIRS_ProvisionerTPM2 RPM successfully built'
    echo '************************************************************************************'
else
    echo 'Error: HIRS_ProvisionerTPM2 failed to package'
    exit 1
fi
