FROM ubuntu:22.04

RUN apt-get update -y && apt-get upgrade -y && apt-get clean -y

# Install packages for building HIRS ACA
RUN apt-get -y install openjdk-17-jdk  mariadb-server
RUN apt-get -y install git curl nano cron


# Checkout HIRS main branch and run gradlew to install gradlew dependencies, then delete HIRS
# Use '--depth=1' so as to not download the history of all commits
RUN git clone -b v3_issue-726 --depth=1 https://github.com/nsacyber/HIRS.git /hirsTemp/github
RUN git config --global http.sslVerify false

CMD /bin/bash
