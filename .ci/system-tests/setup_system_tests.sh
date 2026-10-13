#!/bin/bash
#########################################################################################
#    Script to run the System Tests  for HIRS TPM 2.0 Provisoner from GitHub Workflow
#    Used by the workflow and run_system_tests.sh.
#    Usage: setup_system_tests.sh <remote-ref> [expected-commit]
#########################################################################################

set -e
requested_ref="${1:?Usage: $0 <remote-ref> [expected-commit]}"
expected_commit="${2:-}"
source_repository=$(git remote get-url origin)

# Setting variables
aca_container=hirs-aca1
tpm2_container=hirs-provisioner1-tpm2
export HIRS_CI_TPM_SIM="${HIRS_CI_TPM_SIM:-ibmswtpm2}"
case "$HIRS_CI_TPM_SIM" in
  ibmswtpm2|wolftpm) ;;
  *) echo "Unsupported TPM simulator: $HIRS_CI_TPM_SIM" >&2; exit 1 ;;
esac

# Start System Testing Docker Environment
echo "********  Setting up for HIRS System Tests for TPM 2.0 ******** "
echo "[HIRS-CI] Requested TPM simulator: $HIRS_CI_TPM_SIM"
# Recreate containers so previous runs cannot retain a different simulator selection.
docker compose -f ./.ci/docker/docker-compose-system-test.yml up \
  --pull "${HIRS_CI_IMAGE_PULL_POLICY:-always}" --force-recreate -d
container_tpm_sim=$(docker exec "$tpm2_container" printenv HIRS_CI_TPM_SIM) || exit $?
if [ "$container_tpm_sim" != "$HIRS_CI_TPM_SIM" ]; then
  echo "TPM simulator mismatch: requested $HIRS_CI_TPM_SIM, container has $container_tpm_sim" >&2
  exit 1
fi

# Fetch the requested ref explicitly, including refs/pull/*/merge on GitHub.
# Avoid the image's auto_clone_branch helper, which can hide checkout failures.
for container in "$aca_container" "$tpm2_container"; do
  docker exec "$container" bash -ec '
    cd /hirs
    git fetch --no-tags -- "$1" "$2"
    git checkout --detach --force FETCH_HEAD
    actual_commit=$(git rev-parse HEAD)
    echo "[HIRS-CI] Container commit: $actual_commit"
    if [ -n "$3" ] && [ "$actual_commit" != "$3" ]; then
      echo "Expected commit $3, checked out $actual_commit" >&2
      exit 1
    fi
  ' bash "$source_repository" "$requested_ref" "$expected_commit"
done

# Complete ACA configuration before launching its long-running server.
docker exec "$aca_container" bash -ec '
  cd /hirs
  package/linux/aca/aca_setup.sh --unattended
  /tmp/hirs_add_aca_tls_path_to_os.sh
'
docker exec -d "$aca_container" bash -ec '
  cd /hirs
  exec package/linux/aca/aca_bootRun.sh -d > /tmp/hirs-aca-startup.log 2>&1
'

# Switching to current/desired branch in Provisioner Container
docker exec "$tpm2_container" bash -ec '
  cd /hirs/HIRS_Provisioner.NET/hirs
  rm -rf bin/Release
  dotnet deb -r linux-x64 -c Release
  dotnet rpm -r linux-x64 -c Release
'
# Install HIRS Provisioner.Net and setup tpm2 simulator.
# In doing so, tests a single provision between Provisioner.Net and ACA.
echo "Launching provisioner setup"
docker exec "$tpm2_container" bash /hirs/.ci/setup/container/setup_tpm2provisioner_dotnet.sh

# Initiating System Tests
echo "******** Setup Complete. Beginning HIRS System Tests. ******** "
