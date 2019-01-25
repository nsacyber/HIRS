#!/bin/bash

# Script to run the System Tests for HIRS

set -e

echo ""
echo "System Tests Starting..."
echo ""

# Start System Testing Docker Environment
cd .ci/docker

docker-compose up -d

tpm_container_id="$(docker ps -aqf "name=hirs-aca-provisioner")"
echo "TPM Container ID: $tpm_container_id"
tpm2_container_id="$(docker ps -aqf "name=hirs-aca-provisioner-tpm2")"
echo "TPM2 Container ID: $tpm2_container_id"

tpm_container_status="$(docker inspect $tpm_container_id --format='{{.State.Status}}')"
echo "TPM Container Status: $tpm_container_status"
tpm2_container_status="$(docker inspect $tpm2_container_id --format='{{.State.Status}}')"
echo "TPM2 Container Status: $tpm2_container_status"

while [[ $tpm_container_status == "running" || $tpm2_container_status == "running" ]]
do
  sleep 10

  # Add status message, so Travis will not time out. 
  # It may timeout if it hasn't received output for more than 10 minutes.
  echo "Still running tests, please wait..."

  tpm_container_status="$(docker inspect $tpm_container_id --format='{{.State.Status}}')"
  tpm2_container_status="$(docker inspect $tpm2_container_id --format='{{.State.Status}}')"
done

# Store TPM2 container exit code
tpm2_container_exit_code="$(docker inspect $tpm2_container_id --format='{{.State.ExitCode}}')"
echo "TPM2 Container Exit Code: $tpm2_container_exit_code"

# Display TPM2 container log
echo ""
echo "===========hirs-aca-provisioner System Tests Log:==========="
docker logs $tpm_container_id

echo ""
echo "===========hirs-aca-provisioner-tpm2 System Tests Log:==========="
docker logs $tpm2_container_id

echo ""
echo "End of System Tests, cleaning up..."
echo ""
# Clean up services and network
docker-compose down

# Return TPM2 container exit code
if [[ $tpm2_container_exit_code == 0 ]]
then
    echo "SUCCESS: System tests passed"
    exit 0
fi

echo "ERROR: System tests failed"
exit 1
