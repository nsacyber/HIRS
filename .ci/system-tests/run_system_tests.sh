#!/bin/bash
#########################################################################################
#    Script to Locally run the System Tests for HIRS TPM 2.0 Provisoner
#    *** INTENDED FOR LOCAL SYSTEM TESTING, NOT FOR WORKFLOW RUNS ***
#    Run from the HIRS repository root with the desired HIRS branch as an argument
#    (i.e. $./.ci/system-tests/run_system_tests.sh main)
##########################################################################################

# Setting variables
aca_container=hirs-aca1
tpm2_container=hirs-provisioner1-tpm2

# Same setup as the GitHub workflow
./.ci/system-tests/setup_system_tests.sh $1

./.ci/system-tests/tests/aca_policy_tests.sh
./.ci/system-tests/tests/platform_cert_tests.sh
./.ci/system-tests/tests/rim_system_tests.sh

echo "******** HIRS System Tests Complete ******** "

# Collecting ACA and Provisioner.Net logs for workflow artifact
echo "*** Extracting ACA and Provisioner.Net logs ..."
docker exec $aca_container sh -c "mkdir -p /HIRS/logs/aca/ && cp -arp /var/log/hirs/* /HIRS/logs/aca/"
docker exec $tpm2_container sh -c "mkdir -p /HIRS/logs/provisioner/ && cp -ap hirs*.log /HIRS/logs/provisioner/ && chmod -R 777 /HIRS/logs"

# Clean up services and network
echo "*** Exiting and removing Docker containers and network ..."
docker compose -f ./.ci/docker/docker-compose-system-test.yml down -v
