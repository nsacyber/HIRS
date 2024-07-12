#!/bin/bash
#########################################################################################
#   Setup a local directory to act as the ESP for testing
#   This just creates the directory structure.
#   usage efi_setup.sh [-c] [-p] [-r]
#   -c: clear all artifact directories
#   -p: clear only the platform directory
#   -r: clear only the rim directories
#########################################################################################

# Load env variables
. /hirs/.ci/docker/.env

# Process parameters Argument handling 
POSITIONAL_ARGS=()
ORIGINAL_ARGS=("$@")
while [[ $# -gt 0 ]]; do
  case $1 in
    -c|--clear-all)
      CLEAR_ALL=YES
      shift # past argument
      ;;
    -p|--clear-platform)
      CLEAR_PLATFORM=YES
      shift # past argument
      ;;
    -r|--clear-rim)
      CLEAR_RIM=YES
      shift # past argument
      ;;
    *)
     POSITIONAL_ARGS+=("$1") # save positional arg
     # shift # past argument
     break
      ;;
  esac
  
# Ensure file structure is there
mkdir -p $HIRS_CI_EFI_PATH_PLATFORM
mkdir -p $HIRS_CI_EFI_PATH_RIM
mkdir -p $HIRS_CI_EFI_PATH_SWIDTAG

# Clear out any previous artifacts

if [ "$CLEAR_ALL" = YES ] || [ "$CLEAR_PLATFORM" = YES ] ; then
  rm -f $HIRS_CI_EFI_PATH_PLATFORM/*
fi
if [ "$CLEAR_ALL" = YES ] || [ "$CLEAR_RIM" = YES ] ; then
  rm -f $HIRS_CI_EFI_PATH_RIM/*
  rm -f $HIRS_CI_EFI_PATH_SWIDTAG/*
fi

