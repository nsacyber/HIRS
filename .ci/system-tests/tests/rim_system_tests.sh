#!/bin/bash
#########################################################################################
#    HIRS Reference Integrity Manifest System Tests
#
#########################################################################################
source ./.ci/system-tests/sys_test_common.sh
testResult=false
totalTests=0;
failedTests=0;
test="all"
case $1 in
    1) test="1" ;;
    2) test="2" ;;
    3) test="3" ;;
esac

# Start ACA Reference Integrity Manifest Tests
# provisionTpm2 takes 1 parameter (the expected result): "pass" or "fail"
# Note that the aca_policy_tests have already run several RIM system tests

if [ "$test" = "1" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA RIM TEST 1: Test a RIM from an OEM and a Supplemental RIM from a VAR ###"
    clearAcaDb
    resetTpmForNewTest
    uploadTrustedCerts
    setPolicyEkPcFw
    setPlatformCerts -p "laptop" -t "varOsInstall"
    setRims -p "laptop" -t "varOsInstall"
    provisionTpm2 "pass"
fi
if [ "$test" = "2" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA RIM TEST 2: Test a RIM from an OEM with a bad reference measurement and a Supplemental RIM from a VAR ###"
    clearAcaDb
    resetTpmForNewTest
    uploadTrustedCerts
    setPolicyEkPcFw
    setPlatformCerts -p "laptop" -t "badOemInstall"
    setRims -p "laptop" -t "badOemInstall"
    provisionTpm2 "fail"
fi
if [ "$test" = "3" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA RIM TEST 3: Test a RIM from an OEM and a Supplemental RIM from a VAR with a bad reference measurement ###"
    clearAcaDb
    resetTpmForNewTest
    uploadTrustedCerts
    setPolicyEkPcFw
    setPlatformCerts -p "laptop" -t "badVarInstall"
    setRims -p "laptop" -t "badVarInstall"
    provisionTpm2 "pass"
fi

#  Process Test Results, any single failure will send back a failed result.
if [[ $failedTests != 0 ]]; then
    echo "****  $failedTests out of $totalTests ACA RIM Tests Failed! ****"
    exit 1
  else
    echo "****  $totalTests ACA RIM Tests Passed! ****"
fi
