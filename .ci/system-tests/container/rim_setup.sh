#!/bin/bash
#########################################################################################
#   Setup for PC Client Reference Integrity Manifest (RIM) tests
#
#########################################################################################

profile=$1
test=$2
tcgDir="/boot/tcg"
testDir="/HIRS/.ci/system-tests/profiles/$profile/$test"
propFile="/etc/hirs/tcg_boot.properties";
eventLog="$testDir"/"$profile"_"$test"_binary_bios_measurements

mkdir -p $tcgDir/manifest/rim/;  # Create the platform cert folder if its not there
rm -f $tcgDir/manifest/rim/*;   # clear out any previous data

mkdir -p $tcgDir/manifest/swidtag/;  # Create the platform cert folder if its not there
rm -f $tcgDir/manifest/swidtag/*;   # clear out any previous data

echo "Test is using RIM files from $profile : $test"

# update tcg_boot.properties to use test specific binary_bios_measurement file
sed -i "s:tcg.event.file=.*:tcg.event.file=$eventLog:g" "$propFile"

#echo "Contents of $propFile after sed is $(cat $propFile)";

# Step 2: Copy Base RIM files to the TCG folder
pushd $testDir/swidtags/ > /dev/null

  if [[ ! -f ".gitignore" ]]; then
    for swidtag in * ; do
          cp -f $swidtag $tcgDir/manifest/swidtag/$swidtag;
    done
  fi
popd > /dev/null
# Step 3: Copy Support RIM files to the TCG folder
pushd $testDir/rims/ > /dev/null

  if [[ ! -f ".gitignore" ]]; then
    for rim in * ; do
          cp -f $rim $tcgDir/manifest/rim/$rim;
    done
  fi
popd > /dev/null

#  echo "Contents of tcg swidtag folder $tcgDir/manifest/swidtag/ : $(ls $tcgDir/manifest/swidtag/)"
#  echo "Contents of tcg rim folder tcgDir/manifest/rim/: $(ls $tcgDir/manifest/rim/)"

#Step 4, run the setpcr script to make the TPM emulator hold values that correspond the binary_bios_measurement file
sh $testDir/"$profile"_"$test"_setpcrs.sh
#tpm2_pcrlist -g sha256 

# Done with rim_setup 