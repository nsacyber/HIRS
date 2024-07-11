#install the rim tool.
docker exec ubuntu_rim_testing /bin/bash -c "cd /hirsTemp/github/tools/tcg_rim_tool && ./gradlew clean Build && ./gradlew buildDeb"
docker exec ubuntu_rim_testing /bin/bash -c "dpkg -i /hirsTemp/github/tools/tcg_rim_tool/build/distributions/tcg-rim-tool*.deb"

#switch to specified branch (main).
docker exec ubuntu_rim_testing /bin/bash -c "cd hirsTemp/github/HIRS && git checkout main"
docker exec ubuntu_rim_testing /bin/bash -c "./hirsTemp/github/HIRS/.ci/tcg-rim-tool/scripts/run_all_tests.sh"

