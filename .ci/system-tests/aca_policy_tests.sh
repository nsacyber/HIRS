#!/bin/bash
#########################################################################################
#    HIRS ACA Policy System Tests
#
#########################################################################################
testResult=false
totalTests=0;
failedTests=0;

# Start ACA Policy Tests
# provision_tpm takes 1 parameter (the expected result): "pass" or "fail"

echo "ACA POLICY TEST 1: Test ACA default policy "
provision_tpm2 "pass"

echo "ACA POLICY TEST 2: Test EK cert Only Validation Policy without a EK Issuer Cert in the trust store"
setPolicyEkOnly 
provision_tpm2 "pass"

echo "ACA POLICY TEST 3: Test EK Only Validation Policy" 
uploadTrustedCerts
provision_tpm2 "pass"

echo "ACA POLICY TEST 4: Test PC Validation Policy with no PC" 
setPolicyEkPc_noAttCheck
provision_tpm2 "fail"

echo "ACA POLICY TEST 5: Test FW and PC Validation Policy with no PC" 
setPolicyEkPcFw
provision_tpm2 "fail"

#  Process Test Results, any single failure will send back a failed result.
if [[ $failedTests != 0 ]]; then
    export TEST_STATUS=1;
    echo "****  $failedTests out of $totalTests ACA Policy Tests Failed! ****"
  else
    echo "****  $totalTests ACA Policy Tests Passed! ****"
fi