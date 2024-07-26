#!/bin/bash
#########################################################################################
#   Setup for PC Client Reference Integrity Manifest (RIM) tests
#   usage rim_setup.sh -p <profile> -t <test> [-u] [-n]
#########################################################################################

# Load env variables
. /hirs/.ci/docker/.env

profile=laptop
test=default
ciTestEventLog=$HIRS_CI_TEST_EVENT_LOG_FILE

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
      echo "rim_setup.sh: Unknown option $1"
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
defaultDir="$profileDir/default"
testDir="$profileDir/$test"
eventLog="$testDir"/"$profile"_"$test"_binary_bios_measurements
swidDir="$testDir/swidtags"
rimDir="$testDir/rims"
pcrScript="$testDir/"$profile"_"$test"_setpcrs.sh"

echo "Test is using RIM files from $profile : $test"

# Ensure rim folders under efi are set up and cleared
$HIRS_CI_REPO_ROOT/.ci/system-tests/container/efi_setup.sh -r

# Step 1: Copy binary_bios_measurement file
if [ ! -e "$eventLog" ]; then
  eventLog=$HIRS_CI_TEST_DEFAULT_EVENT_LOG
fi
echo "eventLog used was  $eventLog"
cp "$eventLog" "$ciTestEventLog"

# Step 2: Copy Base RIM files to the TCG folder
#      a: See if test specific swidtag folder exists, if not use the default folder
if [[ ! -d $swidDir ]]; then
    swidDir=$defaultDir/swidtags;
fi
pushd $swidDir > /dev/null
  if [[ ! -f ".gitignore" ]]; then
    for swidtag in * ; do
      if [ "$PUT_ARTIFACTS_IN_ESP" = YES ]; then
        echo "Saving $swidtag to $HIRS_CI_EFI_PATH_SWIDTAG"
        cp $swidtag $HIRS_CI_EFI_PATH_SWIDTAG
      fi
      if [ "$UPLOAD_ARTIFACTS" = YES ]; then
        echo "Uploading $swidtag to $SERVER_RIM_POST"
        curl -k -F "file=@$swidtag" $SERVER_RIM_POST
      fi
    done
  fi
popd > /dev/null
# Step 3: Copy Support RIM files to the TCG folder in the same manner
if [[ ! -d $rimDir ]]; then 
    rimDir=$defaultDir/rims;
fi
pushd $rimDir > /dev/null
  if [[ ! -f ".gitignore" ]]; then
    for rim in * ; do
      if [ "$PUT_ARTIFACTS_IN_ESP" = YES ]; then
        echo "Saving $rim to $HIRS_CI_EFI_PATH_RIM"
        cp $rim $HIRS_CI_EFI_PATH_RIM
      fi
      if [ "$UPLOAD_ARTIFACTS" = YES ]; then
        echo "Uploading $rim to $SERVER_RIM_POST"
        curl -k -F "file=@$rim" $SERVER_RIM_POST
      fi
    done
  fi
popd > /dev/null

#Step 4, run the setpcr script to make the TPM emulator hold values that correspond the binary_bios_measurement file
if [[ ! -f $pcrScript ]]; then
    pcrScript=$HIRS_CI_TEST_DEFAULT_SETPCRS_SH
fi
sh $pcrScript;

