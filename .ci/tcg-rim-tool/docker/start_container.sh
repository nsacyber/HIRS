# run the container by using the image name as an argument. ie.. (./start_container rim_tool)
docker run -d -it --name=ubuntu_rim_testing $1

# run script in docker container
./container/rim_tests_ubuntu.sh

#stop and remove the container
docker stop ubuntu_rim_testing
docker rm ubuntu_rim_testing
