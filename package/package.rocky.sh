#!/bin/bash

echo "Cleaning build/distributions"
rm build/distributions/*
echo "Building HIRS_AttestationCA:war ..."
./gradlew :HIRS_AttestationCA:clean :HIRS_AttestationCA:war
echo "Building HIRS_AttestationCAPortal.war..."
./gradlew :HIRS_AttestationCAPortal:clean :HIRS_AttestationCAPortal:buildRpm
echo "Packaging HIRS_AttestationCA.rpm"
cp HIRS_AttestationCAPortal/build/distributions/*  build/distributions/

