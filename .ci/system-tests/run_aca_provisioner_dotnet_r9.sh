# Setting variables
(cd ../.. && . ./.ci/docker/.env && set -a)
aca_container_name="aca_r9"
provisioner_image_name="r9ts"
provisioner_container_name="provisioner_test"
port_map_8443="8444"
network_name=system_test_net

# Create network for ACA container and Provisioner container to communicate inside of
docker network create --subnet=172.19.0.0/16 $network_name

# Check for ACA container, build if does not exist
# Note that ACA port 8443 is mapped to 8444 for testing purposes, to avoid crowding other users on the device using 8443
if [ ! "$(docker ps -a -q -f name="$aca_container_name")" ]; then
  docker run --name="$aca_container_name" -p $port_map_8443:8443 --network=$network_name --ip="${HIRS_ACA_PORTAL_IP}" -dit ghcr.io/nsacyber/hirs/aca:5445278
fi

# Check for Provisioner Image
if [ ! "$(docker ps -a -q -f name="$provisioner_container_name")" ]; then
  (cd ../.. && docker build -f ./.ci/docker/Dockerfile.tpm2provisioner_dotnet -t $provisioner_image_name .)
fi

# Check for Provisioner container, build if does not exist
if [ ! "$(docker ps -a -q -f name="$provisioner_container_name")" ]; then
  docker run --name="$provisioner_container_name" --network=$network_name --ip="${HIRS_ACA_PROVISIONER_IP}" -it $provisioner_image_name
fi

# Upon exiting Provisioner container, stop and remove ACA and Provisioner containers
docker stop $aca_container_name
docker rm $aca_container_name
docker stop $provisioner_container_name
docker rm $provisioner_container_name
docker image rm $provisioner_image_name
docker network rm $network_name
echo "ACA container, Provisioner container, Provisioner image, and Docker network have been removed."