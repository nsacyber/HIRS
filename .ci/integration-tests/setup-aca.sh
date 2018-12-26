#!/bin/bash

# Script to setup the ACA Docker Image for Integration Tests

set -e

# Prevent rebuild of packages if they already exist
cd /HIRS
if [ ! -d package/rpm/RPMS ]; then
    ./package/package.centos.sh
fi
yum install -y package/rpm/RPMS/noarch/HIRS_AttestationCA*.el7.noarch.rpm

echo "ACA Loaded!"

tail -f /dev/null
