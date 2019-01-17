#!/bin/bash

# Script to download and install the TrouSerS TSS in the System Test Environment

set -e

sudo apt-get -y install libssl-dev openssl build-essential pkg-config libtool automake autoconf
sudo apt-get -y install libgtk2.0-dev

mkdir trousers
pushd trousers
wget https://sourceforge.net/projects/trousers/files/trousers/0.3.14/trousers-0.3.14.tar.gz
tar -xzvf trousers-0.3.14.tar.gz

./bootstrap.sh
export PKG_CONFIG_PATH=/usr/lib64/pkgconfig
CFLAGS="-L/usr/lib64 -L/opt/gnome/lib64" LDFLAGS="-L/usr/lib64 \
           -L/opt/gnome/lib64" ./configure --libdir="/usr/local/lib64"
make
sudo make install

popd
