#!/bin/bash
#########################################################################################
#   Setup for platform certificates for testing
#   usage pc_setup.sh -p <profile> -t <test> [-u] [-n]
#   By default, copies platform certs (Base and Delta) to the tcg directory.
#   -u: upload the certs to the ACA directly.
#   -n: disable copy of certs to the tcg directory.
#########################################################################################

# Load env variables
. /hirs/.ci/docker/.env

profile=laptop
test=default
ciTestHwJsonFile=$HIRS_CI_TEST_HW_JSON_FILE

# By default save the artifacts in EFI and do not upload to the ACA
UPLOAD_ARTIFACTS=NO
PUT_ARTIFACTS_IN_ESP=YES

# Process parameters Argument handling 
POSITIONAL_ARGS=()
ORIGINAL_ARGS=("$@")
while [[ $# -gt 0 ]]; do
  case $1 in
    -p|--profile)
      shift # past argument
      profile=$1
      shift # past parameter
      ;;
    -t|--test)
      shift # past argument
      test=$1
      shift # past parameter
      ;;
    -u|--upload)
      UPLOAD_ARTIFACTS=YES
      shift # past argument
      ;;
    -n|--no-efi)
      PUT_ARTIFACTS_IN_ESP=NO
      shift # past argument
      ;;
    -*|--*)
      echo "pc_setup.sh: Unknown option $1"
      shift # past argument
      ;;
    *)
     POSITIONAL_ARGS+=("$1") # save positional arg
     # shift # past argument
     break
      ;;
  esac
done
# Profile selections
profileDir="$HIRS_CI_REPO_ROOT/.ci/system-tests/profiles/$profile"
testDir="$profileDir/$test"
pcDir="$testDir/platformcerts"
hwJsonFileName="$profile"_"$test"_hw.json
hwJsonFile="$testDir/$hwJsonFileName"

# Use default settings if profile does not have specific changes
if [ ! -f "$hwJsonFile" ]; then
    echo "Test is using a profile with no hardware manifest file. Using default."
    hwJsonFile=$HIRS_CI_TEST_DEFAULT_HW_JSON_FILE
fi

# Ensure platform folder under efi is set up and cleared
$HIRS_CI_REPO_ROOT/.ci/system-tests/container/efi_setup.sh -p

echo "Platform certs selected from profile: $profile : $test"
# Step 1: Copy hw json file, if it exists.
if [ -f "$hwJsonFile" ]; then
    echo "hw file used was $hwJsonFile"
    cp "$hwJsonFile" "$ciTestHwJsonFile"
fi

# Can remove this once unzip is added to the image
dnf install -y unzip &> /dev/null

# Step 2: Copy the platform cert to tcg folder and or upload it to the ACA
if [[ ! -d $pcDir ]]; then
    pcDir=$profileDir/default/platformcerts
fi

pushd $pcDir > /dev/null
# Skip copy of platform cert if .gitigore exists (empty profile)
  if [[ ! -f ".gitignore" ]]; then
    for cert in * ; do
      if [ "$PUT_ARTIFACTS_IN_ESP" = YES ]; then
        echo "Saving $cert to $HIRS_CI_EFI_PATH_PLATFORM"
        cp $cert $HIRS_CI_EFI_PATH_PLATFORM
      fi
      if [ "$UPLOAD_ARTIFACTS" = YES ]; then
        echo "Uploading $cert to $SERVER_PCERT_POST"
        curl -k -F "file=@$cert" $SERVER_PCERT_POST
      fi
    done
  fi
popd > /dev/null
