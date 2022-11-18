#!/bin/bash
tom_version="10.1.1"

# Check if tomcat already installed 
if [ -d "/opt/tomcat" ]; then
   echo "tomcat already installed"
else 
   echo "installing $tom_version"
   pushd /tmp
   useradd -r -d /opt/tomcat/ -s /bin/false -c "Tomcat User" tomcat
   dnf install wget -y
   wget https://dlcdn.apache.org/tomcat/tomcat-10/v$tom_version/bin/apache-tomcat-10.1.1.tar.gz
   mkdir /opt/tomcat
   tar -xzf apache-tomcat-$tom_version.tar.gz -C /opt/tomcat --strip-components=1
   rm apache-tomcat-$tom_version.tar.gz
   chown -R tomcat: /opt/tomcat
   sudo sh -c 'chmod +x /opt/tomcat/bin/*.sh'
   popd
fi
if [ -f /.dockerenv ]; then
  echo "in a container..."
  ./opt/tomcat/tomcat/bin/catalina.sh
else 
  cp /opt/hirs/scripts/aca/tomcat.service   /etc/systemd/system/.
  systemctl daemon-reload
  systemctl start tomcat
  systemctl enable tomcat
fi
firewall-cmd --add-port=8080/tcp --permanent
firewall-cmd --add-port=8443/tcp --permanent
