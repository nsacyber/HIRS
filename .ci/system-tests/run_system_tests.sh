#!/bin/bash
#########################################################################################
#    Script to run the System Tests  for HIRS TPM 2.0 Provisoner
#
#########################################################################################
# Working directory = /HIRS/
cd ../..

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

echo "*** End of System Tests, exiting and removing Docker containers and network..."

# Clean up services and network
docker compose -f ./.ci/docker/docker-compose-system-test.yml down -v
