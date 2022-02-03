#!/bin/bash
#########################################################################################
#    HIRS Reference Integrity Manifest System Tests
#
#########################################################################################
testResult=false
totalTests=0;
failedTests=0;

# Start ACA Reference Integrity Manifest Tests
# provisionTpm2 takes 1 parameter (the expected result): "pass" or "fail"
# Note that the aca_policy_tests have already run several RIM system tests

writeToLogs "### ACA RIM TEST 1: Test a RIM from an OEM and a Supplemental RIM from a VAR ###"
clearAcaDb
uploadTrustedCerts
setPolicyEkPcFw
setPlatformCerts "laptop" "varOsInstall"
provisionTpm2 "pass"