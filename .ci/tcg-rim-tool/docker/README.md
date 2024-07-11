The script ```start_container.sh``` in this directory will run RIM tool tests found in
```HIRS/.ci/tcg-rim-tool/scripts/``` in an Ubuntu docker container.

To build the docker image and run the script:
```
docker build . -t <image_name>  
./start_container.sh <image_name>
```  
