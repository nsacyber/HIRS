#!/bin/bash

TCG_BOOT_FILE="/etc/hirs/tcg_boot.properties"
MAINFEST_DIRECTORY="/boot/tcg/manifest"
LOG_FILE_LOCATION="$MAINFEST_DIRECTORY/rim/"
TAG_FILE_LOCATION="$MAINFEST_DIRECTORY/swidtag/"

if [ ! -f "$TCG_BOOT_FILE" ]; then
  touch "$TCG_BOOT_FILE"
fi

if [ -d "$LOG_FILE_LOCATION" ]; then
  RIM_FILE=$(find "$LOG_FILE_LOCATION" -name '*.rimel' -or -name '*.bin' -or -name '*.rimpcr' -or -name '*.log')
  echo "tcg.rim.file=$RIM_FILE" >> "$TCG_BOOT_FILE"
fi

if [ -d "$TAG_FILE_LOCATION" ]; then
  SWID_FILE=$(find "$TAG_FILE_LOCATION" -name '*.swidtag')
  echo "tcg.swidtag.file=$SWID_FILE" >> "$TCG_BOOT_FILE"
fi

chmod -w "$TCG_BOOT_FILE"