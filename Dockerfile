FROM centos:7.5.1804

MAINTAINER apl.dev3@jhuapl.edu

# Install packages for building HIRS
RUN yum -y update && yum clean all
RUN yum install -y java-1.8.0-openjdk-devel epel-release cmake make git gcc-c++ doxygen graphviz python libssh2-devel openssl protobuf-compiler protobuf-devel tpm2-tss-devel trousers-devel
RUN yum install -y cppcheck log4cplus-devel re2-devel

# Set Environment Variables
ENV JAVA_HOME /usr/lib/jvm/java

# Download HIRS Project
RUN git clone https://github.com/nsacyber/HIRS.git /root/HIRS
