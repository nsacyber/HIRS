#!/bin/bash
#########################################################################################
#    Script to set up the System Tests for the HIRS TPM 2.0 Provisioner
#    Used by the GitHub workflow, which then runs each test, and by run_system_tests.sh for local runs.
#    Run from the HIRS repository root with the HIRS branch to test as an argument
#    (i.e. $./.ci/system-tests/setup_system_tests.sh main)
#########################################################################################

# Setting variables
aca_container=hirs-aca1
tpm2_container=hirs-provisioner1-tpm2

# Start System Testing Docker Environment
echo "********  Setting up for HIRS System Tests for TPM 2.0 ******** "
docker compose -f ./.ci/docker/docker-compose-system-test.yml up --pull "always" -d

# Setting up and Starting ACA + Switching to current/desired branch in ACA Container
docker exec $aca_container sh -c "/tmp/auto_clone_branch $1 > /dev/null 2>&1 \
                                  && echo 'ACA Container Current Branch: ' && git rev-parse --abbrev-ref HEAD \
                                  && echo 'ACA Container Current Commit: ' && git rev-parse --short HEAD \
                                  && /hirs/package/linux/aca/aca_setup.sh --unattended 1> /dev/null \
                                  && /tmp/hirs_add_aca_tls_path_to_os.sh 1> /dev/null \
                                  && /hirs/package/linux/aca/aca_bootRun.sh -d 1> /dev/null" &

# Switching to current/desired branch in Provisioner Container
docker exec $tpm2_container sh -c "/tmp/auto_clone_branch $1 > /dev/null 2>&1 \
                                   && echo 'Provisioner Container Current Branch: ' && git rev-parse --abbrev-ref HEAD \
                                   && echo 'Provisioner Container Current Commit: ' && git rev-parse --short HEAD \
                                   && ./gradlew -p HIRS_Provisioner.NET clean release"

# Install HIRS Provisioner.Net and setup tpm2 simulator.
# In doing so, tests a single provision between Provisioner.Net and ACA.
echo "Launching provisioner setup"
docker exec -i $tpm2_container /bin/bash -c "/hirs/.ci/setup/container/setup_tpm2provisioner_dotnet.sh"

# Initiating System Tests
echo "******** Setup Complete. Beginning HIRS System Tests. ******** "
