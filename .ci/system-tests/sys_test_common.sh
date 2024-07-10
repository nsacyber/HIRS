#!/bin/bash
#########################################################################################
#    Common functions used for HIRS system tests
#
#########################################################################################
. ./.ci/docker/.env && set -a

# Setting variables
aca_container=hirs-aca1
tpm2_container=hirs-provisioner1-tpm2

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
docker exec $aca_container mysql -u root -proot -D hirs_db -e "Update PolicySettings set ecValidationEnabled=0, pcAttributeValidationEnabled=0, pcValidationEnabled=0,
           utcValidationEnabled=0, firmwareValidationEnabled=0, expiredCertificateValidationEnabled=0, ignoreGptEnabled=0, ignoreImaEnabled=0, ignoretBootEnabled=0;"
}

# Policy Settings for tests ...
setPolicyEkOnly() {
docker exec $aca_container mysql -u root -proot -D hirs_db -e "Update PolicySettings set ecValidationEnabled=1, pcAttributeValidationEnabled=0, pcValidationEnabled=0,
           utcValidationEnabled=0, firmwareValidationEnabled=0, expiredCertificateValidationEnabled=0, ignoreGptEnabled=0, ignoreImaEnabled=0, ignoretBootEnabled=0;"
}

setPolicyEkPc_noAttCheck() {
docker exec $aca_container mysql -u root -proot -D hirs_db -e "Update PolicySettings set ecValidationEnabled=1, pcAttributeValidationEnabled=0, pcValidationEnabled=1,
           utcValidationEnabled=0, firmwareValidationEnabled=0, expiredCertificateValidationEnabled=0, ignoreGptEnabled=0, ignoreImaEnabled=0, ignoretBootEnabled=0;"
}

setPolicyEkPc() {
docker exec $aca_container mysql -u root -proot -D hirs_db -e "Update PolicySettings set ecValidationEnabled=1, pcAttributeValidationEnabled=1, pcValidationEnabled=1,
           utcValidationEnabled=0, firmwareValidationEnabled=0, expiredCertificateValidationEnabled=0, ignoreGptEnabled=0, ignoreImaEnabled=0, ignoretBootEnabled=0;"
}

setPolicyEkPcFw() {
docker exec $aca_container mysql -u root -proot -D hirs_db -e "Update PolicySettings set ecValidationEnabled=1, pcAttributeValidationEnabled=1, pcValidationEnabled=1,
           utcValidationEnabled=0, firmwareValidationEnabled=1, expiredCertificateValidationEnabled=0, ignoreGptEnabled=0, ignoreImaEnabled=1, ignoretBootEnabled=0;"
}

# Clear all ACA DB items excluding policy
clearAcaDb() {
docker exec hirs-aca1 mysql -u root -proot -e "use hirs_db; set foreign_key_checks=0; truncate Appraiser;
 truncate Certificate;truncate Certificate_Certificate;truncate CertificatesUsedToValidate;truncate ComponentAttributeResult;
 truncate ComponentInfo;truncate ComponentResult;truncate Device;truncate DeviceInfoReport;truncate PortalInfo;
 truncate ReferenceDigestValue;truncate ReferenceManifest;truncate Report;truncate SupplyChainValidation;
 truncate SupplyChainValidationSummary;truncate SupplyChainValidationSummary_SupplyChainValidation;
 truncate TPM2ProvisionerState;set foreign_key_checks=1;"
}

# Upload Certs to the ACA DB
uploadTrustedCerts() {
  # Create EK Cert from IBMTSS Tools
#  docker exec $tpm2_container sh -c "pushd /ibmtss/utils > /dev/null \
#                                     && ./createekcert -rsa 2048 -cakey cakey.pem -capwd rrrr -v 1> /dev/null \
#                                     && popd > /dev/null"
  # Upload CA Cert from IBMTSS Tools
  docker exec $tpm2_container sh -c "pushd /ibmtss/utils/certificates > /dev/null \
                                     && curl -k -s -F 'file=@cacert.pem' https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload \
                                     && popd > /dev/null"
  # Upload Trusted Certs from HIRS
  pushd .ci/setup/certs > /dev/null
    curl -k -s -F "file=@ca.crt" https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload
    curl -k -s -F "file=@RIMCaCert.pem" https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload
    curl -k -s -F "file=@RimSignCert.pem" https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload
  popd > /dev/null
}

# provision_tpm2 takes one parameter which is the expected result of the provion: "pass" or "fail"
# updates totalTests and failedTests counts
# provision_tpm2 <expected_results>
provisionTpm2() {
   expected_result=$1
   ((totalTests++))
   provisionOutput=$(docker exec $tpm2_container sh -c "/usr/share/hirs/tpm_aca_provision --tcp --ip 127.0.0.1:2321 --sim");
    echo "==========="
    echo "$provisionOutput";
    echo "===========";
  if [[ $provisionOutput == *"failed"* ]]; then
     if [[ $expected_result == "pass" ]]; then
        ((failedTests++))
        echo "!!! Provisioning failed, but was expected to pass"
     else
        echo "Provisioning failed as expected."
     fi
  else   # provisioning succeeded
     if [[ $expected_result == "fail" ]]; then
       ((failedTests++))
       echo "!!! Provisioning passed, but was expected to fail"
     else
        echo "Provisioning passed as expected."
     fi
  fi
}

resetTpmForNewTest() {
  docker exec -i $tpm2_container /bin/bash -c "source /hirs/.ci/setup/container/tpm2_common.sh; startFreshTpmServer -f; startupTpm; installEkCert"
}

# Places platform cert(s) held in the test folder(s) in the provisioners tcg folder
# setPlatCert <profile> <test>
setPlatformCerts() {
  docker exec $tpm2_container sh /hirs/.ci/system-tests/container/pc_setup.sh $1 $2
  #docker exec $tpm2_container bash -c "find / -name oem_platform_v1_Base.cer"
}

# Places RIM files held in the test folder in the provisioners tcg folder
# setRims <profile> <test>
setRims() {
docker exec $tpm2_container sh /hirs/.ci/system-tests/container/rim_setup.sh $1 $2 $3
#docker exec $tpm2_container bash -c "find / -name oem_platform_v1_Base.cer"
}

setAppsettings() {
  OPTIONS=$@
  docker exec -i $tpm2_container /bin/bash -c "source /hirs/.ci/setup/container/tpm2_common.sh; setCiHirsAppsettingsFile $OPTIONS"
}

# Writes to the Action ouput, ACA log, and Provisioner Log
# Used for marking the start of system tests and noting the result
# write_to_logs <log statement>
writeToLogs() {
  line=$1
  echo $line;
  docker exec $aca_container sh -c "cd .. && echo '$line' >> /var/log/hirs/HIRS_AttestationCA_Portal.log"
 # docker exec $tpm2_container sh -c "echo '$line' >> /var/log/hirs/provisioner/HIRS_provisionerTPM2.log"
}
