!/bin/bash

# Set Java to 1.8.0
sudo update-java-alternatives -s java-1.8.0-openjdk-amd64
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
export PATH=$PATH:$JAVA_HOME
sudo echo "JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64" >> /etc/environment

# Mariadb install
sudo /etc/init.d/mysql stop
sudo apt-get purge mysql* mariadb* -y
sudo apt-get autoremove
sudo apt-get autoclean
sudo rm -rf /etc/mysql /var/lib/mysql /var/log/mysql
curl -LsS https://r.mariadb.com/downloads/mariadb_repo_setup | sudo bash -s -- --mariadb-server-version="mariadb-10.3"
sudo apt-get -y update && apt-get -y install mariadb-server
