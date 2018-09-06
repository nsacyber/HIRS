#!/bin/bash

# Define script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Set variables
GIT_HASH=`git rev-parse HEAD | head -c6`
# assign build version vars
source $SCRIPT_DIR/build_version_helper.sh
DEBIAN_FULL_VERSION=$VERSION.$RELEASE
OS=`lsb_release -c | awk '{print $2}'`
PROVISIONER_FULL_PACKAGE_NAME="hirs-provisioner"

# Enter package directory
mkdir -p $SCRIPT_DIR/deb
cd $SCRIPT_DIR/deb

# Clear old builds
rm -rf DEB_SOURCES
rm -f DEBS/hirs*.deb
rm -f DEBS/tpm-module*.deb

# Create directories
mkdir -p DEBS

# Copy Sources for HIRS_Provisioner
cd $SCRIPT_DIR/..

# build HIRS items.
./gradlew -PdisplayVersion=$DISPLAY_VERSION :HIRS_Provisioner:installDist

# Setup Provisioner directories
mkdir -p $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/debian
mkdir -p $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/bin
mkdir -p $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/lib
mkdir -p $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/scripts
mkdir -p $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/setup

# Copy Provisioner files
cp -r HIRS_Provisioner/build/install/HIRS_Provisioner/* $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner
rm -rf $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/bin/*.bat
cp -r HIRS_Provisioner/debian/* $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/debian
cp -r HIRS_Provisioner/man/* $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/debian
cp -r HIRS_Provisioner/scripts/* $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/scripts
cp -r HIRS_Provisioner/src/main/resources/*.properties $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/scripts/install

cp -r HIRS_Provisioner/setup/* $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/setup
cp HIRS_Provisioner/hirs-provisioner-config.sh $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/scripts
cp HIRS_Provisioner/scripts/install/*.sh $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/scripts
cp HIRS_Provisioner/setup/hirs-provisioner.properties $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/setup
cp HIRS_Provisioner/build/resources/main/defaults.properties $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner/setup/provisioner.properties
cp HIRS_Utils/src/main/resources/logging.properties $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/install-provisioner
cp NOTICE $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/debian/copyright

sed -i "s/VER/$DEBIAN_FULL_VERSION/" $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/debian/changelog
sed -i "s/RELEASE/$OS/" $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner/debian/changelog

# Build Debian package for HIRS Provisioner
echo "Building $PROVISIONER_FULL_PACKAGE_NAME"
cd $SCRIPT_DIR/deb/DEB_SOURCES/$PROVISIONER_FULL_PACKAGE_NAME/
debuild -i -us -uc -b
ret=$?

if [[ $ret -ne 0 ]]; then
    echo "Failed to build HIRS Provisioner deb package"
    exit 1
fi

mv $SCRIPT_DIR/deb/DEB_SOURCES/hirs-provisioner*.deb $SCRIPT_DIR/deb/DEBS/

echo "HIRS deb building complete"

# TPM Module
cd $SCRIPT_DIR/../tpm_module
# Setup build directories
mkdir -p $SCRIPT_DIR/deb/DEB_SOURCES/tpm-module/
mkdir -p $SCRIPT_DIR/deb/DEB_SOURCES/tpm-module/debian
mkdir -p $SCRIPT_DIR/deb/DEB_SOURCES/tpm-module/src

# Compile the TPM Module
../gradlew build

# Copy build files
cp tpm_module $SCRIPT_DIR/deb/DEB_SOURCES/tpm-module/src/
cp -r debian/* $SCRIPT_DIR/deb/DEB_SOURCES/tpm-module/debian
cp -r man/* $SCRIPT_DIR/deb/DEB_SOURCES/tpm-module/debian
cp ../NOTICE $SCRIPT_DIR/deb/DEB_SOURCES/tpm-module/debian/copyright

# Build Debian package for TPM Module
cd $SCRIPT_DIR/deb/DEB_SOURCES/tpm-module/
debuild -i -us -uc -b
ret=$?

if [[ $ret -ne 0 ]]; then
    echo "Failed to build tpm-module deb package"
    exit 1
fi

mkdir -p $SCRIPT_DIR/deb/DEBS/
mv $SCRIPT_DIR/deb/DEB_SOURCES/tpm-module*.deb $SCRIPT_DIR/deb/DEBS/

echo "TPM deb building complete"
