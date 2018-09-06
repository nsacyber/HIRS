#!/bin/bash

TPM_VER_1_2=$(dmesg | grep -i "1\.2 TPM")
TPM_VERSION_2=$(dmesg | grep -i "2\.0 TPM")
TSS_LIB_FOUND=$(gcc -lsapi 2>&1 | grep 'main')

if [ -z "$TPM_VERSION_2" ]; then
  printf "The system does not show a TPM v2.0 installed on the system.\n";
  if [ ! -z "$TPM_VER_1_2" ]; then
    printf "The system shows a TPM v1.2 installed.\n";
  fi
fi

if [ -z "$TSS_LIB_FOUND" ]; then
  printf "Add the directory containing the TSS libraries to the load path.\n";
fi
