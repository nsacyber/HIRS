# Script to setup the ACA Docker Image for Integration Tests

set -e

# TODO: Make packaging of project modular (i.e. can package ACA without also packaging other projects)
# Otherwise we'll run into conflicts when different Docker containers try to package at the same time
cd /HIRS
./package/package.centos.sh
yum install -y package/rpm/RPMS/noarch/HIRS_AttestationCA*.el7.noarch.rpm
tail -f /dev/null
