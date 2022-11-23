#!/bin/bash
portalPackDir="HIRS_AttestationCAPortal/build/distributions"
hirsPackDir="build/distributions"
mkdir -p $hirsPackDir
if [ -n "$(ls -A $hirsPackDir 2>/dev/null)" ]; then
  echo "Cleaning $hirsPackDir"
  rm $hirsPackDir/*
fi
echo "Building HIRS_AttestationCA:war ..."
./gradlew :HIRS_AttestationCA:clean :HIRS_AttestationCA:war
echo "Building HIRS_AttestationCAPortal.war..."
./gradlew :HIRS_AttestationCAPortal:clean :HIRS_AttestationCAPortal:buildRpm
echo "Packaging HIRS_AttestationCA.rpm"
if [ -n "$(ls -A $portalPackDir 2>/dev/null)"  ]; then
  cp $portalPackDir/*  $hirsPackDir/.
else echo "Error: no package found in $packDir"
fi
