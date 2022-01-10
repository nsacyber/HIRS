#!/bin/bash
#########################################################################################
#   Setup for platform certificate tests
#
#########################################################################################

profile=$1
test=$2
compscript="$profile"_"$test"_allcomponents.sh
hwlist="$profile"_"$test"_hw.json
mkdir -p /boot/tcg/cert/platform/;  # Create the platform cert folder if its not there
rm -f /boot/tcg/cert/platform/*;   # clear out any previous data

# set the folder to read the platform cert from
#echo "tcg.cert.dir=/boot/tcg/cert/platform/" >  /etc/hirs/tcg_boot.properties

#echo "Test is using platform cert(s) from $profile : $test"
# Step 1: Copy allcomponents script to the paccor/scripts folder
cp -f /HIRS/.ci/system-tests/profiles/$profile/$test/$compscript /opt/paccor/scripts/allcomponents.sh;

# Step 2: Copy allcomponents json file to the paccor/scripts folder
cp  -f /HIRS/.ci/system-tests/profiles/$profile/$test/$hwlist /opt/paccor/scripts/$hwlist ;

# Step 3: Copy the platform cert to tcg folder on boot drive
pushd /HIRS/.ci/system-tests/profiles/$profile/$test/platformcerts/ > /dev/null

for cert in * ; do
          cp -f $cert /boot/tcg/cert/platform/$cert;
    done

#    echo "contents of /boot/tcg/cert/platform/ is $(ls /boot/tcg/cert/platform/)"
#    echo "contents of hirs config is $(ls -al /etc/hirs)"
#    echo "contents of tcg config is $(cat /etc/hirs/tcg_boot.properties)"
popd > /dev/null

# Step 4: Make some data available for debugging
bash /opt/paccor/scripts/allcomponents.sh > /var/log/hirs/provisioner/allcomponents.output.log