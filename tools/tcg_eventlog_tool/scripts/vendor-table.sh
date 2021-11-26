#!/bin/bash

HIRS_DIR="/opt/hirs/default-properties"
VENDOR_FILE="vendor-table.json"

# check that the directory exists
if [ ! -d "$HIRS_DIR" ]; then
  mkdir -p $HIRS_DIR
fi

# in case there was a problem, still check and if all is well
# move the file with no-clobber
if [ -d "$HIRS_DIR" ]; then
  # if the file doesn't exist
  if [ ! -f "$HIRS_DIR/$VENDOR_FILE" ]; then
    mv -n "/tmp/$VENDOR_FILE" $HIRS_DIR
    chmod 0644 "$HIRS_DIR/$VENDOR_FILE"
  else
    # if it does, then just remove the tmp folder version
    rm "/tmp/$VENDOR_FILE"
  fi
fi