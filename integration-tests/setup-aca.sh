# Script to setup the ACA Docker Image for Integration Tests
cd /HIRS
./package/package.centos.sh
yum localinstall -y package/rpm/RPMS/noarch/HIRS_AttestationCA*.el7.noarch.rpm
tail -f /dev/null
