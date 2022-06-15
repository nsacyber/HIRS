#!/bin/bash

# Run script from location where script is located

# Define script directory

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Define repo directory
REPO_DIR="$( cd ../../; pwd )"

cd $REPO_DIR

sudo ./gradlew :HIRS_AttestationCA:clean :HIRS_AttestationCA:build :HIRS_AttestationCAPortal:clean :HIRS_AttestationCAPortal:build

mkdir $REPO_DIR/package/deb/hirs-attestationca_2.1.1_amd64/usr/share/tomcat
mkdir $REPO_DIR/package/deb/hirs-attestationca_2.1.1_amd64/usr/share/tomcat/webapps
cp  $REPO_DIR/HIRS_AttestationCA/build/libs/HIRS_AttestationCA.war $REPO_DIR/package/deb/hirs-attestationca_2.1.1_amd64/usr/share/tomcat/webapps/
cp  $REPO_DIR/HIRS_AttestationCAPortal/build/libs/HIRS_AttestationCAPortal.war $SCRIPT_DIR/hirs-attestationca_2.1.1_amd64/usr/share/tomcat/webapps/

echo "Done copying war files"

cd $SCRIPT_DIR

mkdir hirs-attestationca_2.1.1_amd64/etc/hirs/aca/certificates
mkdir hirs-attestationca_2.1.1_amd64/etc/hirs/aca/client-files

sudo dpkg-deb --build --root-owner-group hirs-attestationca_2.1.1_amd64
