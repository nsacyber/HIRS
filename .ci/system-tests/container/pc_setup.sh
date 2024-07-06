#!/bin/bash
#########################################################################################
#   Setup for platform certificates for testing
#   Copies platform certs (Base and Delta) to the tcg directory
#   usage pc_setup.sh <profile> <test>
#########################################################################################

profile=$1
test=$2
ciTestDir="/ci_test"
tcgDir="$ciTestDir/boot/efi/EFI/tcg/cert/platform/"

# Profile selections
profileDir="/hirs/.ci/system-tests/profiles/$profile"
testDir="$profileDir/$test"
pcDir="$testDir/platformcerts"
dmiZip="$profileDir/$profile"_dmi.zip
hwJsonFileName="$profile"_"$test"_hw.json
hwJsonFile="$testDir/$hwJsonFileName"
ciTestHwJsonFile="$ciTestDir/hw.json"

# Use default settings if profile does not have specific changes
if [ ! -f "$hwJsonFile" ]; then
    echo "Test is using a profile with no hardware manifest file. Using default."
    hwJsonFile="$profileDir"/default/laptop_default_hw.json
fi

if [ ! -f "$dmiZip" ]; then
    echo "Test is using a profile with no DMI data. Using default."
    dmiZip="$profileDir"/default/laptop_dmi.zip
fi

# Current TCG folder for platform certs
mkdir -p $tcgDir;  # Create the platform cert folder if its not there
rm -f $tcgDir*;    # Clear out any previous data

echo "Test is using platform cert(s) from $profile : $test"
# Step 1: Copy hw json file, if it exists.
if [ -f "$hwJsonFile" ]; then
    echo "hw file used was $hwJsonFile"
    cp "$hwJsonFile" "$ciTestHwJsonFile"
fi

# Can remove this once unzip is added to the image
dnf install -y unzip &> /dev/null

# Step 2: Unpack the dmi files.
echo "dmi file used was $dmiZip"
unzip -o "$dmiZip" -d "$ciTestDir"

# Step 3: Copy the platform cert to tcg folder 
if [[ ! -d $pcDir ]]; then
    pcDir=$profileDir/default/platformcerts;
fi
pushd $pcDir > /dev/null
# Skip copy of platform cert if .gitigore exists (empty profile)
if [[ ! -f ".gitignore" ]]; then
    for cert in * ; do
          cp -f $cert $tcgDir$cert;
    done
fi

popd > /dev/null
