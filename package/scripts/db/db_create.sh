#!/bin/bash
#
###############################################################################
# HIRS DB creation
# Environment variables used:
# a. HIRS_MYSQL_ROOT_PWD: Set this variable if mysql root password is already set
# b. HIRS_DB_PWD: Set the pwd if default password to hirs_db user needs to be changed
################################################################################

LOG_FILE=$1
# LOG_FILE="/var/log/hirs/hirs_aca_install_$(date +%Y-%m-%d).log"
# Capture location of the script to allow from invocation from any location
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
SPRING_PROP_FILE="/etc/hirs/aca/application.properties"
ACA_PROP_FILE="/etc/hirs/aca/aca.properties"
DB_ADMIN_PWD=""
#DB_USER="hirs_db"
# Db Configuration files
DB_SRV_CONF="/etc/my.cnf.d/mariadb-server.cnf"
DB_CLIENT_CONF="/etc/my.cnf.d/client.cnf"
# Default Server Side Certificates
SSL_DB_SRV_CHAIN="/etc/hirs/certificates/HIRS/rsa_3k_sha384_certs/HIRS_rsa_3k_sha384_Cert_Chain.pem";
SSL_DB_SRV_CERT="/etc/hirs/certificates/HIRS/rsa_3k_sha384_certs/HIRS_db_srv_rsa_3k_sha384.pem"; 
SSL_DB_SRV_KEY="/etc/hirs/certificates/HIRS/rsa_3k_sha384_certs/HIRS_db_srv_rsa_3k_sha384.key";
# Default Client Side Certificates
SSL_DB_CLIENT_CHAIN="/etc/hirs/certificates/HIRS/rsa_3k_sha384_certs/HIRS_rsa_3k_sha384_Cert_Chain.pem";
SSL_DB_CLIENT_CERT="/etc/hirs/certificates/HIRS/rsa_3k_sha384_certs/HIRS_db_client_rsa_3k_sha384.pem"; 
SSL_DB_CLIENT_KEY="/etc/hirs/certificates/HIRS/rsa_3k_sha384_certs/HIRS_db_client_rsa_3k_sha384.key";

touch $ACA_PROP_FILE
touch $LOG_FILE
touch $DB_SRV_CONF

# Make sure required paths exist
mkdir -p /etc/hirs/aca/
mkdir -p /var/log/hirs/

source $SCRIPT_DIR/start_mysqld.sh
source $ACA_PROP_FILE 

check_mysql_root_pwd () {
  # Check if DB root password needs to be obtained
  echo "HIRS_MYSQL_ROOT_PWD is $HIRS_MYSQL_ROOT_PWD"
  if [ -z $HIRS_MYSQL_ROOT_PWD ]; then
	 # Create a 32 character random password
	 echo "Using randomly generated password for the DB admin" | tee -a "$LOG_FILE"
	 DB_ADMIN_PWD=$(head -c 64 /dev/urandom | md5sum | tr -dc 'a-zA-Z0-9')
	 echo "DB Admin will be set to $DB_ADMIN_PWD , please make note for next mysql use."
	 read -p "Do you wish to save this password to the aca.properties file? " confirm
	 if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
	    echo "mysql_admin_password=$DB_ADMIN_PWD" >> $ACA_PROP_FILE
	    echo "Password saved."
	   else
	    echo "Password not saved."
	 fi
	 mysqladmin --user=root password "$DB_ADMIN_PWD"
  else
    DB_ADMIN_PWD=$HIRS_MYSQL_ROOT_PWD
    echo "Using system variable supplied password" | tee -a "$LOG_FILE"
  fi
  # Make sure root password is correct
  $(mysql -u root -p$DB_ADMIN_PWD -e 'quit'  &> /dev/null);
  if [ $? -eq 0 ]; then
     echo "root password verified"  | tee -a "$LOG_FILE"
  else
     echo "MYSQL root password was not the default, not supplied,  or was incorrect"
     echo "      please set the HIRS_MYSQL_ROOT_PWD system variable and retry."
     echo "      ********** ACA Mysql setup aborted ********" ;
     exit 1;
  fi
}

set_mysql_server_tls () {
  # Check DB server setup. If ssl params dont exist then we need to add them.
  if [[ $(cat "$DB_SRV_CONF" | grep -c "ssl") < 1 ]]; then
    # Add TLS files to my.cnf
    echo "Updating $DB_SRV_CONF with ssl parameters..." | tee -a "$LOG_FILE"
    echo "ssl_ca=$SSL_DB_SRV_CHAIN" >> "$DB_SRV_CONF"
    echo "ssl_cert=$SSL_DB_SRV_CERT" >> "$DB_SRV_CONF"
    echo "ssl_key=$SSL_DB_SRV_KEY" >> "$DB_SRV_CONF"
    # Make sure mysql can access them
    chown mysql:mysql $SSL_DB_SRV_CHAIN $SSL_DB_SRV_CERT $SSL_DB_SRV_KEY
     # Make selinux contexts for configu file
    semanage fcontext -a -t mysqld_etc_t $DB_SRV_CONF  > /dev/null #adds the context type to file
    restorecon -v -F $DB_SRV_CONF                           # changes the file's context type
  else
       echo "mysql.cnf contians existing entry for ssl, skipping..." | tee -a "$LOG_FILE"
  fi
}

set_mysql_client_tls () {
# Update ACA property file with client cert info, if not there already
if [[ $(cat "$DB_CLIENT_CONF" | grep -c "ssl") < 1 ]]; then 
  echo "Updating $DB_CLIENT_CONF with ssl parameters..." | tee -a "$LOG_FILE"
  echo "ssl_ca=$SSL_DB_CLIENT_CHAIN" >> $DB_CLIENT_CONF
  echo "ssl_cert=$SSL_DB_CLIENT_CERT" >> $DB_CLIENT_CONF
  echo "ssl_key=$SSL_DB_CLIENT_KEY" >> $DB_CLIENT_CONF
  chown mysql:mysql $SSL_DB_CLIENT_CHAIN $SSL_DB_CLIENT_CERT $SSL_DB_CLIENT_KEY 
  # Make selinux contexts for configu file
  semanage fcontext -a -t mysqld_etc_t $DB_CLIENT_CONFf > /dev/null  #adds the context type to file
  restorecon -F $DB_CLIENT_CONF                           #changes the file's context type
fi
}

# Process HIRS DB USER
set_hirs_db_pwd () {

   RESULT="$(mysql -u root --password=$DB_ADMIN_PWD -e "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = 'hirs_db')")"
   if [ "$RESULT" = 1 ]; then
      echo "hirs-db user exists"
      HIRS_DB_PWD=$hirs_db_password
   else   
   # Check if Mysql HIRS DB  password set by system variable or set to random number
     if [ -z $HIRS_DB_PWD ]; then
       HIRS_DB_PWD=$(head -c 64 /dev/urandom | md5sum | tr -dc 'a-zA-Z0-9')
     fi

     echo "hirs_db_username=hirs_db" >> $ACA_PROP_FILE
     echo "hirs_db_password=$HIRS_DB_PWD" >> $ACA_PROP_FILE
  fi

}

# Create a hirs_db with client side TLS enabled
create_hirs_db_with_tls () {
  # Check if hirs_db not created and create it if it wasn't
  mysqlshow --user=root --password="$DB_ADMIN_PWD" | grep "hirs_db" > /dev/null 2>&1
  if [ $? -eq 0 ]; then
     echo "hirs_db exists, skipping hirs_db create"
  else
     mysql -u root --password=$DB_ADMIN_PWD < $MYSQL_DIR/db_create.sql
     mysql -u root --password=$DB_ADMIN_PWD < $MYSQL_DIR/secure_mysql.sql
     mysql -u root --password=$DB_ADMIN_PWD -e "ALTER USER 'hirs_db'@'localhost' IDENTIFIED BY '"$HIRS_DB_PWD"'; FLUSH PRIVILEGES;";
  fi
}

# HIRS ACA Mysqld processing ...
check_mariadb_install
check_for_container
set_mysql_server_tls
set_mysql_client_tls
start_mysqlsd
check_mysql_root_pwd
set_hirs_db_pwd
create_hirs_db_with_tls
# reboot mysql server
mysql -u root --password=$DB_ADMIN_PWD -e "SHUTDOWN"
sleep 2
check_for_container
start_mysqlsd
