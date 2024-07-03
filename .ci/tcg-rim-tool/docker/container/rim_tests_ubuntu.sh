#install the rim tool.
docker exec ubuntu_rim_testing /bin/bash -c "cd /hirsTemp/github/tools/tcg_rim_tool && ./gradlew clean Build && ./gradlew buildDeb"
docker exec ubuntu_rim_testing /bin/bash -c "dpkg -i /hirsTemp/github/tools/tcg_rim_tool/build/distributions/tcg-rim-tool*.deb"

# clone gitlab and run the tests...
docker exec ubuntu_rim_testing /bin/bash -c "mkdir hirsTemp/gitlab"
docker exec -it ubuntu_rim_testing /bin/bash -c "cd hirsTemp/gitlab && git clone https://gitlab.evoforge.org/HIRS/HIRS.git"
#switch to specified branch (issue_107).
docker exec ubuntu_rim_testing /bin/bash -c "cd hirsTemp/gitlab/HIRS && git checkout main"
docker exec ubuntu_rim_testing /bin/bash -c "./hirsTemp/gitlab/HIRS/test/rim/scripts/run_all_tests.sh"

