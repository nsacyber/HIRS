#!/bin/bash

# Script to run the System Tests for HIRS TPM 1.2 Provisioner

set -e

echo ""
echo "System Tests Starting..."
echo ""

# Start System Testing Docker Environment
cd .ci/docker

docker-compose up -d

tpm_container_id="$(docker ps -aqf "name=hirs-aca-provisioner")"
echo "TPM Container ID: $tpm_container_id"

tpm_container_status="$(docker inspect $tpm_container_id --format='{{.State.Status}}')"
echo "TPM Container Status: $tpm_container_status"

while [[ $tpm_container_status == "running" ]]
do
  sleep 10

  # Add status message, so Travis will not time out. 
  # It may timeout if it hasn't received output for more than 10 minutes.
  echo "Still running tests, please wait..."

  tpm_container_status="$(docker inspect $tpm_container_id --format='{{.State.Status}}')"
done

# Store container exit codes
tpm_container_exit_code="$(docker inspect $tpm_container_id --format='{{.State.ExitCode}}')"
echo "TPM Container Exit Code: $tpm_container_exit_code"

# Display container logs
echo ""
echo "===========hirs-aca-provisioner System Tests Log:==========="
docker logs $tpm_container_id

echo ""
echo "End of TPM 1.2 System Tests, cleaning up..."
echo ""
# Clean up services and network
docker-compose down

# Return container exit codes
if [[ $tpm_container_exit_code == 0 ]]
then
    echo "SUCCESS: TPM 1.2 System tests passed"
    exit 0
fi

echo "ERROR: System tests failed"
exit 1
