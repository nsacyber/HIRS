#!/bin/bash
#########################################################################################
#    Script to run the System Tests  for HIRS TPM 2.0 Provisoner
#    Notes for running manually/locally (not from GitHub Actions)
#    1. Uncomment the "cd ../.." line below to make working directory = /HIRS/
#    2. Run with the desired HIRS branch as an argument (i.e. $./run_system_tests.sh main)
#########################################################################################
#cd ../..

# Setting variables
aca_container=hirs-aca1
tpm2_container=hirs-provisioner1-tpm2

# Start System Testing Docker Environment
echo "********  Setting up for HIRS System Tests for TPM 2.0 ******** "
docker compose -f ./.ci/docker/docker-compose-system-test.yml up -d

# Setting up and Starting ACA + Switching to current/desired branch in ACA Container
docker exec $aca_container sh -c "/tmp/auto_clone_branch $1 > /dev/null 2>&1 \
                                  && echo 'ACA Container Current Branch: ' && git branch \
                                  && /hirs/package/linux/aca/aca_setup.sh --unattended 1> /dev/null \
                                  && /tmp/hirs_add_aca_tls_path_to_os.sh 1> /dev/null \
                                  && /hirs/package/linux/aca/aca_bootRun.sh 1> /dev/null" &

# Switching to current/desired branch in Provisioner Container
docker exec $tpm2_container sh -c "/tmp/auto_clone_branch $1 > /dev/null 2>&1 \
                                   && echo 'Provisioner Container Current Branch: ' && git branch"

# Install HIRS Provisioner.Net and setup tpm2 simulator.
# In doing so, tests a single provision between Provisioner.Net and ACA.
docker exec $tpm2_container sh /hirs/.ci/setup/container/setup_tpm2provisioner_dotnet.sh

# Initiating System Tests
echo "******** Setup Complete. Beginning HIRS System Tests. ******** "
./.ci/system-tests/tests/aca_policy_tests.sh
#./.ci/system-tests/tests/platform_cert_tests.sh
#./.ci/system-tests/tests/rim_system_tests.sh

echo "******** HIRS System Tests Complete ******** "

# Collecting ACA and Provisioner.Net logs for workflow artifact
echo "*** Extracting ACA and Provisioner.Net logs ..."
docker exec $aca_container sh -c "mkdir -p /HIRS/logs/aca/ && cp -arp /var/log/hirs/* /HIRS/logs/aca/"
docker exec $tpm2_container sh -c "mkdir -p /HIRS/logs/provisioner/ && cp -ap hirs*.log /HIRS/logs/provisioner/ && chmod -R 777 /HIRS/logs"

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