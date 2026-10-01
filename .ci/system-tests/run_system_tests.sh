#!/bin/bash
#########################################################################################
# Run from the HIRS repository root with the remote branch/ref to test:
#   HIRS_CI_TPM_SIM=wolftpm .ci/system-tests/run_system_tests.sh <remote-ref>
# To use a rebuilt provisioner image, set HIRS_CI_PROVISIONER_IMAGE to its tag
# and HIRS_CI_IMAGE_PULL_POLICY=never.
#########################################################################################

set -e
: "${1:?Usage: $0 <remote-ref> [expected-commit]}"
export HIRS_CI_TPM_SIM="${HIRS_CI_TPM_SIM:-ibmswtpm2}"
case "$HIRS_CI_TPM_SIM" in
  ibmswtpm2|wolftpm) ;;
  *) echo "Unsupported TPM simulator: $HIRS_CI_TPM_SIM" >&2; exit 1 ;;
esac

# Preserve failures, including setup failures, while still collecting diagnostics.
# shellcheck disable=SC2317 # Invoked by the EXIT trap.
cleanup() {
  local status=$?
  trap - EXIT
  bash .ci/system-tests/copy_system_test_logs.sh || true
  echo "*** Exiting and removing Docker containers and network ..."
  docker compose -f .ci/docker/docker-compose-system-test.yml down -v || {
    if [ "$status" -eq 0 ]; then status=1; fi
  }
  exit "$status"
}
trap 'cleanup' EXIT

bash .ci/system-tests/setup_system_tests.sh "$@"

# Run all suites, but report failure if any suite fails.
status=0
./.ci/system-tests/tests/aca_policy_tests.sh || status=1
./.ci/system-tests/tests/platform_cert_tests.sh || status=1
./.ci/system-tests/tests/rim_system_tests.sh || status=1
echo "******** HIRS System Tests Complete (status: $status) ******** "
exit "$status"
