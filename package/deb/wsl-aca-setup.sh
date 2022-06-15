#!/bin/bash

echo "dash dash/sh boolean false" | debconf-set-selections
DEBIAN_FRONTEND=noninteractive dpkg-reconfigure dash
export RUNLEVEL=1
apt-get -y update 

# Move repo to wsl mount for permission edits
cp -r ../HIRS /tmp
cd /tmp/HIRS

# ACA Build Dependencies
DEBIAN_FRONTEND=noninteractive apt-get -y install openjdk-8-jdk protobuf-compiler build-essential devscripts lintian debhelper

# Mariadb install
curl -LsS https://r.mariadb.com/downloads/mariadb_repo_setup | bash
apt-get -y update && apt-get -y install mariadb-server
if [ -f "/etc/init.d/mysql" ]; then
    /etc/init.d/mysql start 
elif [ -f "/etc/init.d/mariadb" ]; then
    /etc/init.d/mariadb start 
else
    systemctl start mariadb
fi

cd ./package/deb

# Set permissions for debian files
chmod -R 755 ./hirs-attestationca_2.1.1_amd64

# Install tomcat 7
sh ./install-tomcat7.sh

# Fix random generation issue
# Comment out RANDFILE
sed -i '13 s/^/#/' /etc/ssl/openssl.cnf

# Build WAR Files, complete deb package and build.
sh ./package-aca-ubuntu.sh

# Install .deb
apt-get install ./hirs-attestationca_2.1.1_amd64.deb

# Install tpm-tools
apt install -y tpm2-tools

tpm2_startup --clear &

ek_cert=/mnt/d/a/HIRS/HIRS/.ci/setup/certs/ek_cert.der

echo "Writing Endorsement Key to Simulator"
if tpm2_nvlist | grep -q 0x1c00002; then      echo "Released NVRAM for EK.";      tpm2_nvrelease -x 0x1c00002 -a 0x40000001;    fi
size=$(cat $ek_cert | wc -c)
tpm2_nvdefine -x 0x1c00002 -a 0x40000001 -t 0x2000A -s $size
tpm2_nvwrite -x 0x1c00002 -a 0x40000001 -f $ek_cert
if tpm2_nvlist | grep -q 0x1c90000; then      echo "Released NVRAM for PC.";      tpm2_nvrelease -x 0x1c90000 -a 0x40000001;    fi

echo "Finished Writing Endorsement Key to Simulator"
echo "Starting Tomcat"
/usr/share/tomcat/bin/startup.sh

# Keeping shell Process Open. Letting it close
# closes tomcat
while [ 1 ]
do
    sleep 1
done