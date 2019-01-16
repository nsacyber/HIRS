#!/bin/bash

# Script to setup the TPM 1.2 Emulator in the System Test Environment

set -e

if [ ! -d /usr/src/linux-headers-$(uname -r) ]; then
    echo "Installing Necessary Linux Headers"
    sudo apt-get -y install linux-headers-$(uname -r)
fi

if [ ! -e /lib/modules/$(uname -r)/build ]; then
    echo "Linking Linux headers to /lib/modules"
    sudo ln -s /usr/src/linux-headers-$(uname -r) /lib/modules/$(uname -r)/build
fi

if [ ! -d /lib/modules/$(uname -r)/extra ]; then
    sudo mkdir -p /lib/modules/$(uname -r)/extra
fi

echo "Making TPM 1.2 Emulator Working Directory"
mkdir tpm_emulator
pushd tpm_emulator

echo "Downloading TPM 1.2 Emulator"
wget https://github.com/PeterHuewe/tpm-emulator/archive/v0.7.5.tar.gz

echo "Opening and Building TPM 1.2 Emulator"
tar -zxvf v0.7.5.tar.gz
cd tpm-emulator-0.7.5
./build.sh

echo "Installing TPM 1.2 Emulator"
cd build
sudo make install

echo "Installing tpmd_dev Kernel Module"
sudo cp tpmd_dev/linux/tpmd_dev.ko /lib/modules/$(uname -r)/extra
sudo depmod

popd

echo "Enabling TPM 1.2 Emulator"
sudo modprobe tpmd_dev
sudo /usr/local/bin/tpmd
