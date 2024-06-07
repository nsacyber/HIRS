#!/bin/bash
#########################################################################################
#    Script to run the System Tests  for HIRS TPM 2.0 Provisoner from GitHub Workflow
#    *** INTENDED FOR WORKFLOW RUNS, NOT FOR LOCAL SYSTEM TESTING ***
#    For local system testing, use run_system_tests.sh instead
#########################################################################################

# Setting variables
aca_container=hirs-aca1
tpm2_container=hirs-provisioner1-tpm2

# Start System Testing Docker Environment
echo "********  Setting up for HIRS System Tests for TPM 2.0 ******** "
docker compose -f ./.ci/docker/docker-compose-system-test.yml up --pull "always" -d

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
echo "Launching provisioner setup"
docker exec $tpm2_container sh /hirs/.ci/setup/container/setup_tpm2provisioner_dotnet.sh

# Initiating System Tests
echo "******** Setup Complete. Beginning HIRS System Tests. ******** "