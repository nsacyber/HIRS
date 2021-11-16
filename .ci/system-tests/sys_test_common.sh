#!/bin/bash
#########################################################################################
#    Common functions used for HIRS system tests
#
#########################################################################################

CheckContainerStatus() {
  container_id=$1
  container_status="$(docker inspect $container_id --format='{{.State.Status}}')"
  echo "Container Status: $container_status"

  if [ "$container_status" != "running" ]; then
     container_exit_code="$(docker inspect $container_id --format='{{.State.ExitCode}}')"
     echo "Container Exit Code: $container_exit_code"
     docker info
     exit 1;
fi
}

setPolicyNone() {
docker exec $aca_container mysql -u root -D hirs_db -e"Update SupplyChainPolicy set enableEcValidation=0, enablePcAttributeValidation=0, enablePcValidation=0, enableUtcValidation=0, enableFirmwareValidation=0, enableExpiredCertificateValidation=0;" 
}

setPolicyEkOnly() {
docker exec $aca_container mysql -u root -D hirs_db -e"Update SupplyChainPolicy set enableEcValidation=1, enablePcAttributeValidation=0, enablePcValidation=0, enableUtcValidation=0, enableFirmwareValidation=0, enableExpiredCertificateValidation=0;" 
}

setPolicyEkPc_noAttCheck() {
docker exec $aca_container mysql -u root -D hirs_db -e"Update SupplyChainPolicy set enableEcValidation=1, enablePcAttributeValidation=0, enablePcValidation=1, enableUtcValidation=0, enableFirmwareValidation=0, enableExpiredCertificateValidation=0;" 
}

setPolicyEkPc() {
docker exec $aca_container mysql -u root -D hirs_db -e"Update SupplyChainPolicy set enableEcValidation=1, enablePcAttributeValidation=1, enablePcValidation=1, enableUtcValidation=0, enableFirmwareValidation=0, enableExpiredCertificateValidation=0;" 
}

setPolicyEkPcFw() {
docker exec $aca_container mysql -u root -D hirs_db -e"Update SupplyChainPolicy set enableEcValidation=1, enablePcAttributeValidation=1, enablePcValidation=1, enableUtcValidation=0, enableFirmwareValidation=1, enableExpiredCertificateValidation=0;" 
}

uploadTrustedCerts() {
  curl -k -s -F "file=@$issuerCert" https://${HIRS_ACA_PORTAL_IP}:8443/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload
}

# provision_tpm2 takes one parameter which is the expected result of the provion: "pass" or "fail"
# updates totalTests and failedTests counts
provision_tpm2() {
   expected_result=$1
   ((totalTests++))
   provisionOutput=$(docker exec $tpm2_container tpm_aca_provision);
    echo "==========="
    echo "$provisionOutput";
    echo "===========";
  if [[ $provisionOutput == *"failed"* ]]; then
     if [[ $expected_result == "pass" ]]; then
        ((failedTests++))
        echo "!!! Provisiong failed, but was expected to pass"
     else
        echo "Provisiong failed as expected."
     fi
  else   # provisioning succeeded
     if [[ $expected_result == "fail" ]]; then
       ((failedTests++))
       echo "!!! Provisiong passed, but was expected to fail"
     else
        echo "Provisiong passed as expected."
     fi
  fi
}

clearAcaDb() {
docker exec $aca_container mysql -u root -e "use hirs_db; set foreign_key_checks=0; truncate Alert;truncate AlertBaselineIds;truncate
 AppraisalResult;truncate Certificate;truncate Certificate_Certificate;truncate CertificatesUsedToValidate;truncate
 ComponentInfo;truncate Device;truncate DeviceInfoReport;truncate IMADeviceState;truncate IMAMeasurementRecord;truncate
 ImaBlacklistRecord;truncate ImaIgnoreSetRecord;truncate IntegrityReport;truncate IntegrityReports_Reports_Join;truncate
 RepoPackage_IMABaselineRecord;truncate Report;truncate ReportMapper;truncate ReportRequestState;truncate ReportSummary;truncate
 State;truncate SupplyChainValidation;truncate SupplyChainValidationSummary;truncate ReferenceManifest;truncate
 SupplyChainValidationSummary_SupplyChainValidation;truncate TPM2ProvisionerState;truncate TPMBaselineRecords;truncate
 TPMDeviceState;truncate TPMReport;truncate TPMReport_pcrValueList; set foreign_key_checks=1;" 
}