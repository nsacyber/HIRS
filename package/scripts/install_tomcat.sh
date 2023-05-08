#!/bin/bash
tom_version="10.1.1"
tom_maj=$(echo "$tom_version" | cut -d '.' -f 1)
CATALINA_HOME=/opt/tomcat/
CATALINA_BASE=/opt/tomcat/

# Check if tomcat already installed 
if [ -d "/opt/tomcat" ]; then
   echo "tomcat already installed"
else 
   echo "installing $tom_version"
   pushd /tmp
   useradd -r -d /opt/tomcat/ -s /bin/false -c "Tomcat User" tomcat
   dnf install wget -y
   wget https://dlcdn.apache.org/tomcat/tomcat-$tom_maj/v$tom_version/bin/apache-tomcat-$tom_version.tar.gz
   mkdir /opt/tomcat
   tar -xzf apache-tomcat-$tom_version.tar.gz -C /opt/tomcat --strip-components=1
   rm apache-tomcat-$tom_version.tar.gz
   chown -R tomcat: /opt/tomcat
   sudo sh -c 'chmod +x /opt/tomcat/bin/*.sh'
   popd
fi
if [ -f /.dockerenv ]; then
  echo "in a container..."
  sh /opt/tomcat/bin/catalina.sh start
else 
  cp /opt/hirs/scripts/aca/tomcat.service   /etc/systemd/system/.
  systemctl daemon-reload
  systemctl start tomcat
  systemctl enable tomcat
fi
if [ $(pgrep -c FirewallD) == "1" ]; then
   firewall-cmd --add-port=8080/tcp --permanent
   firewall-cmd --add-port=8443/tcp --permanent
fi
