#!/bin/bash

# Script to run the System Tests for HIRS

set -e

# Start System Testing Docker Environment
cd .ci/docker

docker-compose up -d

tpm2_container_id="$(docker ps -aqf "name=hirs-aca-provisioner-tpm2")"
echo "TPM2 Container ID: $tpm2_container_id"

tpm2_container_status="$(docker inspect $tpm2_container_id --format='{{.State.Status}}')"
echo "TPM2 Container Status: $tpm2_container_status"

while [ $tpm2_container_status == "running" ] 
do
  sleep 5 

  tpm2_container_status="$(docker inspect $tpm2_container_id --format='{{.State.Status}}')"
done

echo ""
echo "===========hirs-aca-provisioner-tpm2 System Tests Log:==========="
docker logs $tpm2_container_id

echo ""
echo "End of System Tests, cleaning up..."
docker-compose down
