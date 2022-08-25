#!/bin/bash

# Script to setup the ACA Docker Image for Integration Tests

set -e

# Prevent rebuild of packages if they already exist
cd /HIRS
echo "Building and packaging the ACA"
if [ ! -d package/rpm/RPMS ]; then
     mkdir -p /HIRS/logs/aca/
     sh package/package.centos.sh &> /HIRS/logs/aca/aca_build.log
fi
echo "Building and packaging the ACA completed"
echo "Installing the ACA"
yum install -y package/rpm/RPMS/noarch/HIRS_AttestationCA*.el7.noarch.rpm &> /HIRS/logs/aca/aca_install.log
filename=package/rpm/RPMS/noarch/HIRS_AttestationCA*.el7.noarch.rpm
echo "================================================================================"
echo "Installing:"
echo "       HIRS_AttestationCA"
echo "                   $filename" 
echo ""
echo "Transaction Summary"
echo "================================================================================"
echo "Install  1 Package"
echo ""
echo "********************* End of ACA installation *********************"m