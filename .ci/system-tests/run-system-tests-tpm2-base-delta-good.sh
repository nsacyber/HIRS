#!/bin/bash

# Script to run the System Tests Base/Delta(Good) for HIRS TPM 2.0 Provisioner

set -e

echo ""
echo "System Tests Base/Delta(Good) TPM 2.0 Starting..."
echo ""

# Start System Testing Docker Environment
cd .ci/docker

docker-compose -f docker-compose-tpm2-base-delta-good.yml up -d

tpm2_container_id="$(docker ps -aqf "name=hirs-aca-provisioner-tpm2")"
echo "TPM2 Container ID: $tpm2_container_id"

tpm2_container_status="$(docker inspect $tpm2_container_id --format='{{.State.Status}}')"
echo "TPM2 Container Status: $tpm2_container_status"

while [[ $tpm2_container_status == "running" ]]
do
  sleep 20

  # Add status message, so Travis will not time out.
  # It may timeout if it hasn't received output for more than 10 minutes.
  echo "Still running tests, please wait..."

  tpm2_container_status="$(docker inspect $tpm2_container_id --format='{{.State.Status}}')"
done

# Store container exit code
tpm2_container_exit_code="$(docker inspect $tpm2_container_id --format='{{.State.ExitCode}}')"
echo "TPM2 Container Exit Code: $tpm2_container_exit_code"

# Display container log
echo ""
echo "===========hirs-aca-provisioner-tpm2 System Tests Log:==========="
docker logs $tpm2_container_id

echo ""
echo "End of System Tests Base/Delta(Good) TPM 2.0, cleaning up..."
echo ""
# Clean up services and network
docker-compose down

# Clean up dangling containers
echo "Cleaning up dangling containers..."
echo ""
docker ps -a
echo ""
docker container prune --force
echo ""

# Return container exit code
if [[ $tpm2_container_exit_code == 0 ]]
then
    echo "SUCCESS: System Tests Base/Delta(Good) TPM 2.0 passed"
    exit 0
fi

echo "ERROR: System Tests Base/Delta(Good) TPM 2.0 failed"
exit 1
