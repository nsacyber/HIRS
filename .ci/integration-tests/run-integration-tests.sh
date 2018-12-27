#!/bin/bash

# Script to run the Integration Tests for HIRS

set -e

# Start Integration Testing Docker Environment
docker-compose -f .ci/docker/docker-compose.yml up -d

# Check to see if Environment Stand-Up is Complete
# TODO: Refine to handle multiple container IDs
container_id_regex='([a-f0-9]{12})\s+hirs\/hirs-ci:tpm2provisioner'
while : ; do
    docker_containers=$(docker container ls)
    if [[ $docker_containers =~ $container_id_regex ]]; then
        container_id=${BASH_REMATCH[1]}
        break
    fi
done

tpm2_provisioner_started_regex='TPM2 Provisioner Loaded!'
while : ; do
    docker_logs=$(docker logs $container_id)
    if [[ $docker_logs =~ $tpm2_provisioner_started_regex ]]; then
        break
    fi
done

echo "Environment Stand-Up Complete!"
