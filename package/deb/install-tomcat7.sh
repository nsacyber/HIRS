#!/bin/bash
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
wget https://archive.apache.org/dist/tomcat/tomcat-7/v7.0.76/bin/apache-tomcat-7.0.76.tar.gz
groupadd tomcat
mkdir /usr/share/tomcat
useradd -s /bin/false -g tomcat -d /usr/share/tomcat tomcat
tar xzvf ./apache-tomcat-7.0.76.tar.gz  -C /usr/share/tomcat --strip-components=1
chgrp -R tomcat /usr/share/tomcat
cd /usr/share/tomcat
chmod -R g+r conf
chmod g+x conf
chown -R tomcat webapps/ work/ temp/ logs/

echo "[Unit]
Description=Apache Tomcat Web Application Container
After=network.target

[Service]
Type=forking
Environment=JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64/jre
Environment=CATALINA_PID=/usr/share/tomcat/temp/tomcat.pid
Environment=CATALINA_Home=/usr/share/tomcat
Environment=CATALINA_BASE=/usr/share/tomcat
Environment=’CATALINA_OPTS=-Xms512M -Xmx1024M -server -XX:+UseParallelGC’
Environment=’JAVA_OPTS.awt.headless=true -Djava.security.egd=file:/dev/v/urandom’

ExecStart=/usr/share/tomcat/bin/startup.sh
ExecStop=/usr/share/tomcat/bin/shutdown.sh

User=tomcat
Group=tomcat
UMask=0007
RestartSec=10
Restart=always

[Install]
WantedBy=multi-user.target" >> /etc/systemd/system/tomcat.service

cd  /usr/share/tomcat
chown -R tomcat webapps temp logs work conf
chmod -R 777 webapps temp logs work conf
ufw allow 8080

# Start tomcat in actions after everything is setup