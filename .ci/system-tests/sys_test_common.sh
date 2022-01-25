#!/bin/bash
#########################################################################################
#    Common functions used for HIRS system tests
#
#########################################################################################

# Check container status and abort if container is not running
checkContainerStatus() {
  container_name=$1
  container_id="$(docker ps -aqf "name=$container_name")"
  container_status="$(docker inspect $container_id --format='{{.State.Status}}')"
  echo "Container id is $container_id and the status is $container_status"

  if [ "$container_status" != "running" ]; then
     container_exit_code="$(docker inspect $container_id --format='{{.State.ExitCode}}')"
     echo "Container Exit Code: $container_exit_code"
     docker info
     exit 1;
fi
}

# clear all policy settings
setPolicyNone() {
docker exec $aca_container mysql -u root -D hirs_db -e "Update SupplyChainPolicy set enableEcValidation=0, enablePcAttributeValidation=0, enablePcValidation=0,
           enableUtcValidation=0, enableFirmwareValidation=0, enableExpiredCertificateValidation=0, enableIgnoreGpt=0, enableIgnoreIma=0, enableIgnoretBoot=0;" 
}

# Policy Settings for tests ...
setPolicyEkOnly() {
docker exec $aca_container mysql -u root -D hirs_db -e "Update SupplyChainPolicy set enableEcValidation=1, enablePcAttributeValidation=0, enablePcValidation=0,
           enableUtcValidation=0, enableFirmwareValidation=0, enableExpiredCertificateValidation=0, enableIgnoreGpt=0, enableIgnoreIma=0, enableIgnoretBoot=0;"
}

setPolicyEkPc_noAttCheck() {
docker exec $aca_container mysql -u root -D hirs_db -e "Update SupplyChainPolicy set enableEcValidation=1, enablePcAttributeValidation=0, enablePcValidation=1,
           enableUtcValidation=0, enableFirmwareValidation=0, enableExpiredCertificateValidation=0, enableIgnoreGpt=0, enableIgnoreIma=0, enableIgnoretBoot=0;"
}

setPolicyEkPc() {
docker exec $aca_container mysql -u root -D hirs_db -e "Update SupplyChainPolicy set enableEcValidation=1, enablePcAttributeValidation=1, enablePcValidation=1,
           enableUtcValidation=0, enableFirmwareValidation=0, enableExpiredCertificateValidation=0, enableIgnoreGpt=0, enableIgnoreIma=0, enableIgnoretBoot=0;"
}

setPolicyEkPcFw() {
docker exec $aca_container mysql -u root -D hirs_db -e "Update SupplyChainPolicy set enableEcValidation=1, enablePcAttributeValidation=1, enablePcValidation=1,
           enableUtcValidation=0, enableFirmwareValidation=1, enableExpiredCertificateValidation=0, enableIgnoreGpt=0, enableIgnoreIma=0, enableIgnoretBoot=0;"
}

# Clear all ACA DB items including policy
clearAcaDb() {
docker exec $aca_container mysql -u root -e "use hirs_db; set foreign_key_checks=0; truncate Alert;truncate AlertBaselineIds;truncate
 AppraisalResult;truncate Certificate;truncate Certificate_Certificate;truncate CertificatesUsedToValidate;truncate
 ComponentInfo;truncate Device;truncate DeviceInfoReport;truncate IMADeviceState;truncate IMAMeasurementRecord;truncate
 ImaBlacklistRecord;truncate ImaIgnoreSetRecord;truncate IntegrityReport;truncate IntegrityReports_Reports_Join;truncate
 RepoPackage_IMABaselineRecord;truncate Report;truncate ReportMapper;truncate ReportRequestState;truncate ReportSummary;truncate
 State;truncate SupplyChainValidation;truncate SupplyChainValidationSummary;truncate ReferenceManifest;truncate
 ReferenceDigestRecord; truncate ReferenceDigestValue; truncate
 SupplyChainValidationSummary_SupplyChainValidation;truncate TPM2ProvisionerState;truncate TPMBaselineRecords;truncate
 TPMDeviceState;truncate TPMReport;truncate TPMReport_pcrValueList; set foreign_key_checks=1;" 
}

# Upload Certs to the ACA DB
uploadTrustedCerts() {
pushd ../setup/certs > /dev/null

  curl -k -s -F "file=@ca.crt" https://${HIRS_ACA_PORTAL_IP}:8443/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload
  curl -k -s -F "file=@RIMCaCert.pem" https://${HIRS_ACA_PORTAL_IP}:8443/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload
  curl -k -s -F "file=@RimSignCert.pem" https://${HIRS_ACA_PORTAL_IP}:8443/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload

popd > /dev/null
}

# provision_tpm2 takes one parameter which is the expected result of the provion: "pass" or "fail"
# updates totalTests and failedTests counts
# provision_tpm2 <expected_results>
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

# Places platform cert(s) held in the test folder(s) in the provisioners tcg folder
# setPlatCert <profile> <test>
setPlatformCerts() {
  docker exec $tpm2_container sh /HIRS/.ci/system-tests/container/pc_setup.sh $1 $2 
  #docker exec $tpm2_container bash -c "find / -name oem_platform_v1_Base.cer"
}

# Places RIM files held in the test folder in the provisioners tcg folder
# setRims <profile> <test>
setRims() {
docker exec $tpm2_container sh /HIRS/.ci/system-tests/scripts/rim_setup.sh $1 $2 
#docker exec $tpm2_container bash -c "find / -name oem_platform_v1_Base.cer"
}

# Writes to the Action ouput, ACA log, and Provisioner Log
# Used for marking the start of system tests and noting the result
# write_to_logs <log statement>
write_to_logs() {
  line=$1
  echo $line;
  docker exec $aca_container sh -c "echo '$line' >> /var/log/tomcat/HIRS_AttestationCA.log"
 # docker exec $tpm2_container sh -c "echo '$line' >> /var/log/hirs/provisioner/HIRS_provisionerTPM2.log"
}
