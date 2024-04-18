#!/bin/bash
#########################################################################################
#    Script to run the System Tests  for HIRS TPM 2.0 Provisoner
#
#########################################################################################
# Uncomment the cd line below if running this script manually (To make Working directory = /HIRS/)
# cd ../..

# Setting variables
aca_container=hirs-aca1
tpm2_container=hirs-provisioner1-tpm2
. ./.ci/docker/.env && set -a

# Start System Testing Docker Environment
echo "********  Setting up for HIRS System Tests for TPM 2.0 ******** "
docker compose -f ./.ci/docker/docker-compose-system-test.yml up -d

# Install HIRS provisioner and setup tpm2 emulator
docker exec $tpm2_container /.ci/setup/container/setup_tpm2provisioner_dotnet.sh
echo "******** HIRS System Tests Complete ******** "

# Collecting ACA and Provisioner.Net logs for workflow artifact
echo "*** Extracting ACA and Provisioner.Net logs ..."
docker exec $aca_container sh -c "cd .. && mkdir -p /HIRS/logs/aca/ && cp -arp /var/log/hirs/* /HIRS/logs/aca/"
docker exec $tpm2_container sh -c "cd .. && mkdir -p /HIRS/logs/provisioner/ && cp -ap hirs*.log /HIRS/logs/provisioner/ && chmod -R 777 /HIRS/logs"

# Clean up services and network
echo "*** Exiting and removing Docker containers and network ..."
docker compose -f ./.ci/docker/docker-compose-system-test.yml down -v

# Return container exit code
if [[ ${TEST_STATUS} == "0" ]]; then
    echo "******** SUCCESS: System Tests for TPM 2.0 passed ********"
    echo "TEST_STATUS=0" >> $GITHUB_ENV
    exit 0;
  else
    echo "******** FAILURE: System Tests for TPM 2.0 failed ********"
    echo "TEST_STATUS=1" >> $GITHUB_ENV
    exit 1
fi