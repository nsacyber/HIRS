#!/bin/bash
#########################################################################################
#   Setup for PC Client Reference Integrity Manifest (RIM) tests
#
#########################################################################################

profile=$1
test=$2
tcgDir="/boot/tcg"
testDir="/HIRS/.ci/system-tests/profiles/$profile/$test"

mkdir -p $tcgDir/manifest/rim/;  # Create the platform cert folder if its not there
rm -f $tcgDir/manifest/rim/*;   # clear out any previous data

mkdir -p $tcgDir/manifest/swidtag/;  # Create the platform cert folder if its not there
rm -f $tcgDir/manifest/swidtag/*;   # clear out any previous data

echo "Test is using RIM files from $profile : $test"

# update tcg_boot.properties to use test specific binary_bios_measurement file
eventLog="$testDir"/"$profile"_"$test"_binary_bios_measurements
propFile="/etc/hirs/tcg_boot.properties";
#echo "propFile = $propFile"

# tcg_boot_properties is being erased, so recreate for now ......
#echo "tcg.rim.dir=/boot/tcg/manifest/rim/" > $propFile;
#echo "tcg.swidtag.dir=/boot/tcg/manifest/swidtag/" >> $propFile;
#echo "tcg.cert.dir=/boot/tcg/cert/platform/" >> $propFile;
#echo "tcg.event.file=/sys/kernel/security/tpm0/binary_bios_measurements" >> $propFile;

#echo "eventLog = $eventLog"
#echo "Contents of /etc/hirs is $(ls -al /etc/hirs)";
#echo "Contents of $propFile before sed is $(cat $propFile)";

sed -i "s:tcg.event.file=.*:tcg.event.file=$eventLog:g" "$propFile"

#echo "Contents of $propFile after sed is $(cat $propFile)";
#echo "======================"

#echo "Contents of/boot/tcg/cert/platform/ is $(ls /boot/tcg/cert/platform/) : "

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
#echo "Setting PCR register 0 - 23 for test $profile : $test"
sh $testDir/"$profile"_"$test"_setpcrs.sh
#tpm2_pcrlist -g sha256 

# Done with rim_setup 