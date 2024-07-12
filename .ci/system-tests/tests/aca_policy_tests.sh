#!/bin/bash
#########################################################################################
#    HIRS ACA Policy System Tests
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
    4) test="4" ;;
    5) test="5" ;;
    6) test="6" ;;
    7) test="7" ;;
    8) test="8" ;;
    9) test="9" ;;
    10) test="10" ;;
esac

# Start ACA Policy Tests
# provisionTpm2 takes 1 parameter (the expected result): "pass" or "fail"

if [ "$test" = "1" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA POLICY TEST 1: Test ACA default policy  ###"
    writeToLogs "Now using default appsettings"
    clearAcaDb
    resetTpmForNewTest
    setAppsettings
    setPolicyNone
    setPlatformCerts -p "laptop" -t "empty"
    provisionTpm2 "pass"
fi
if [ "$test" = "2" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA POLICY TEST 2: Test EK cert Only Validation Policy without a EK Issuer Cert in the trust store ###"
    setPolicyEkOnly
    provisionTpm2 "fail"
fi
if [ "$test" = "3" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA POLICY TEST 3: Test EK Only Validation Policy ###"
    uploadTrustedCerts
    provisionTpm2 "pass"
fi
if [ "$test" = "4" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA POLICY TEST 4: Test PC Validation Policy with no PC ###"
    setPolicyEkPc_noAttCheck
    provisionTpm2 "fail"
fi
if [ "$test" = "5" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA POLICY TEST 5: Test FW and PC Validation Policy with no PC ###"
    setPolicyEkPcFw
    provisionTpm2 "fail"
fi
if [ "$test" = "6" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA POLICY TEST 6: Test PC Validation Policy with valid PC with no Attribute Check ###"
    clearAcaDb
    resetTpmForNewTest
    setPolicyEkPc_noAttCheck
    uploadTrustedCerts
    setPlatformCerts -p "laptop" -t "default"
    provisionTpm2 "pass"
fi
if [ "$test" = "7" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA POLICY TEST 7: Test PC Validation Policy with valid PC with Attribute Check ###"
    writeToLogs "Now using appsettings with hardware information"
    clearAcaDb
    resetTpmForNewTest
    setPolicyEkPc
    uploadTrustedCerts
    setPlatformCerts -p "laptop" -t "default"
    setAppsettings --paccor-output-file /ci_test/hw.json --event-log-file /ci_test/binary_bios_measurements --linux-dmi
    provisionTpm2 "pass"
fi
if [ "$test" = "8" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA POLICY TEST 8: Test PC with RIM Validation Policy with valid PC and RIM ###"
    clearAcaDb
    resetTpmForNewTest
    setPolicyEkPcFw
    uploadTrustedCerts
    setPlatformCerts -p "laptop" -t "default"
    setRims -p "laptop" -t "default"
    provisionTpm2 "pass"
fi
if [ "$test" = "9" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA POLICY TEST 9: Test valid PC and RIM with PC only uploaded ###"
    clearAcaDb
    resetTpmForNewTest
    setPolicyEkPcFw
    uploadTrustedCerts
    setPlatformCerts -p "laptop" -t "default" -u -n
    setRims -p "laptop" -t "default"
    provisionTpm2 "pass"
fi
if [ "$test" = "10" ] || [ "$test" = "all" ]; then
    writeToLogs "### ACA POLICY TEST 10: Test valid PC and RIM with RIM only uploaded ###"
    clearAcaDb
    resetTpmForNewTest
    setPolicyEkPcFw
    uploadTrustedCerts
    setPlatformCerts -p "laptop" -t "default"
    setRims -p "laptop" -t "default" -u -n
    provisionTpm2 "pass"
fi

#  Process Test Results, any single failure will send back a failed result.
if [[ $failedTests != 0 ]]; then
    export TEST_STATUS=1;
    echo "****  $failedTests out of $totalTests ACA Policy Tests Failed! ****"
  else
    echo "****  $totalTests ACA Policy Tests Passed! ****"
fi
