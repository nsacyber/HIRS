#!/bin/bash
#########################################################################################
#   Setup for platform certificates for testing
#   Copies platform certs (Base and Delta) to the tcg directory
#   usage pc_setup.sh <profile> <test>
#########################################################################################

profile=$1
test=$2
tcgDir="/boot/efi/EFI/tcg/cert/platform/"
compscript="$profile"_"$test"_allcomponents.sh
hwlist="$profile"_"$test"_hw.json
testDir="/hirs/.ci/system-tests/profiles/$profile/$test"
pcDir=$testDir/platformcerts
profileDir="/hirs/.ci/system-tests/profiles/$profile"

# Current TCG folder for platform certs, likely to change with release of the next FIM specification
tcgDir=/boot/efi/EFI/tcg/cert/platform/
mkdir -p $tcgDir;  # Create the platform cert folder if its not there
rm -f $tcgDir*;    # Clear out any previous data

echo "Test is using platform cert(s) from $profile : $test"
# Step 1: Copy allcomponents script to the paccor/scripts folder if there is one.
# Use the default if test does not have a test specific file.

allCompScript=/hirs/.ci/system-tests/profiles/$profile/$test/$compscript
if [ ! -f "$allCompScript" ]; then
  allCompScript=/hirs/.ci/system-tests/profiles/"$profile"/default/"$profile"_default_allcomponents.sh
fi
mkdir -p /opt/paccor/scripts/
cp -f $allCompScript /opt/paccor/scripts/allcomponents.sh;

# Step 2: Copy allcomponents json file to the paccor/scripts folder if there is one
# Use the default if test does not have a test specific file.

allCompJson=/hirs/.ci/system-tests/profiles/$profile/$test/$hwlist;
if [ ! -f "$allCompJson" ]; then
      allCompJson=/hirs/.ci/system-tests/profiles/"$profile"/default/"$profile"_default_hw.json
      cp $allCompJson /opt/paccor/scripts/"$profile"_default_hw.json ;
else
  cp "$allCompJson" /opt/paccor/scripts/$hwlist ;
fi

# Step 3: Copy the platform cert to tcg folder on boot drive
#      a: See if test specific swidtag folder exists, if not use the defualt folder
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
# Step 4: Make some data available for debugging
bash /opt/paccor/scripts/allcomponents.sh > allcomponents.output.log