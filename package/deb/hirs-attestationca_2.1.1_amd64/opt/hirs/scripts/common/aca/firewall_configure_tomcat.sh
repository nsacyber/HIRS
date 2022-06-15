#!/bin/bash
echo "Configuring Firewall"

# Allow Tomcat to use port 3306 to communicate with MySQL
ufw enable
ufw allow 3306
ufw allow 8443
ufw status
