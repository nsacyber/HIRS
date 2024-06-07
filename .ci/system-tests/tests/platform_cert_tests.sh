#!/bin/bash
#########################################################################################
#    HIRS Platform Certificate System Tests
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

# Start ACA Platform Certificate Tests
# provisionTpm2 takes 1 parameter (the expected result): "pass" or "fail"
# Note that the aca_policy_tests have already run several Platform Certificate system tests

if [ "$test" = "1" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA PLATFORM CERTIFICATE TEST 1: Test a delta Platform Certificate that adds a new memory component ###"
    clearAcaDb
    uploadTrustedCerts
    setPolicyEkPc
    setPlatformCerts "laptop" "deltaPlatMem"
    provisionTpm2 "pass"
fi
if [ "$test" = "2" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA PLATFORM CERTIFICATE TEST 2: Test a Platform Certificate that is missing a memory component ###"
    clearAcaDb
    uploadTrustedCerts
    setPlatformCerts "laptop" "platCertLight"
    provisionTpm2 "pass"
fi
if [ "$test" = "3" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA PLATFORM CERTIFICATE TEST 3: Test a Delta Platform Certificate that has a wrong a memory component ###"
    clearAcaDb
    uploadTrustedCerts
    setPlatformCerts "laptop" "badDeltaMem"
    provisionTpm2 "fail"
fi

#  Process Test Results, any single failure will send back a failed result.
if [[ $failedTests != 0 ]]; then
    export TEST_STATUS=1;
    echo "****  $failedTests out of $totalTests Platform Certificate Tests Failed! ****"
  else
    echo "****  $totalTests Platform Certificate Tests Passed! ****"
fi