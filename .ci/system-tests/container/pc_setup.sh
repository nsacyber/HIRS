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

echo "Test is using platform cert(s) from $profile : $test"

# Step 1: Copy allcomponents script to the paccor/scripts folder if there is one.
# Use the default if test does not have a test specific file.

allCompScript=/HIRS/.ci/system-tests/profiles/$profile/$test/$compscript
if [ ! -f "$allCompFile" ]; then
  allCompScript=/HIRS/.ci/system-tests/profiles/"$profile"/default/"$profile"_default_allcomponents.sh
fi
cp -f $allCompScript /opt/paccor/scripts/allcomponents.sh;

# Step 2: Copy allcomponents json file to the paccor/scripts folder if there is one
# Use the default if test does not have a test specific file.

allCompJson=/HIRS/.ci/system-tests/profiles/$profile/$test/$hwlist /opt/paccor/scripts/$hwlist ;
if [ ! -f "$allCompJson" ]; then
      allCompJson=/HIRS/.ci/system-tests/profiles/"$profile"/default/"$profile"_default_hw.json
fi
cp  -f $allCompJson /opt/paccor/scripts/$hwlist ;

# Step 3: Copy the platform cert to tcg folder on boot drive
pushd /HIRS/.ci/system-tests/profiles/$profile/$test/platformcerts/ > /dev/null
#skip copy of platform cert if .gitigore exists (empty profile)
if [[ ! -f ".gitignore" ]]; then
    for cert in * ; do
          cp -f $cert /boot/tcg/cert/platform/$cert;
    done
fi

popd > /dev/null

# Step 4: Make some data available for debugging
bash /opt/paccor/scripts/allcomponents.sh > /var/log/hirs/provisioner/allcomponents.output.log