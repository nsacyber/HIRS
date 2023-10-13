#!/bin/bash
#
###############################################################################
# HIRS DB creation
# Environment variables used:
# a. HIRS_MYSQL_ROOT_PWD: Set this variable if mysql root password is already set
# b. HIRS_DB_PWD: Set the pwd if default password to hirs_db user needs to be changed
################################################################################

LOG_FILE=$1
DB_LOG_FILE="/var/log/mariadb/mariadb.log"
PKI_PASS=$2
UNATTENDED=$3
RSA_PATH=rsa_3k_sha384_certs
ECC_PATH=ecc_512_sha384_certs
# Capture location of the script to allow from invocation from any location
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
SPRING_PROP_FILE="/etc/hirs/aca/application.properties"
ACA_PROP_FILE="/etc/hirs/aca/aca.properties"
DB_ADMIN_PWD=""
# Db Configuration fileis, use RHELpaths as default
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

# Make sure required paths exist
mkdir -p /etc/hirs/aca/
mkdir -p /var/log/hirs/

source $SCRIPT_DIR/mysql_util.sh
source $ACA_PROP_FILE 
source /etc/os-release 

# Setup distro specifc paths and variables
if [ $ID = "ubuntu" ]; then 
   DB_SRV_CONF="/etc/mysql/mariadb.conf.d/50-server.cnf"
   DB_CLIENT_CONF="/etc/mysql/mariadb.conf.d/50-client.cnf"
   mkdir -p /var/log/mariadb >> /dev/null
   if [[ $(cat "$DB_SRV_CONF" | grep -c "log-error") < 1 ]]; then
       echo "log_error=/var/log/mariadb/mariadb.log" >> $DB_SRV_CONF
       echo "tls_version = TLSv1.2,TLSv1.3" >> $DB_SRV_CONF
  fi
fi

touch $ACA_PROP_FILE
touch $LOG_FILE
touch $DB_SRV_CONF
touch $DB_LOG_FILE

check_mysql_root_pwd () {
  # Check if DB root password needs to be obtained
 
  if [ -z $HIRS_MYSQL_ROOT_PWD ]; then
	 # Create a 32 character random password
	 echo "Using randomly generated password for the DB admin" | tee -a "$LOG_FILE"
	 DB_ADMIN_PWD=$(head -c 64 /dev/urandom | md5sum | tr -dc 'a-zA-Z0-9')
	 echo "DB Admin will be set to $DB_ADMIN_PWD , please make note for next mysql use."
     # Check UNATTENDED flag set m if not then prompt user for permission ot store mysql root password
	 if [ -z $UNATTENDED ]; then
	   read -p "Do you wish to save this password to the aca.properties file? " confirm
	   if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
	      echo "mysql_admin_password=$DB_ADMIN_PWD" >> $ACA_PROP_FILE
	      echo "Mysql root password saved locally"
	    else
	      echo "Mysql root password not saved locally"
	   fi
	 else
	   echo "mysql_admin_password=$DB_ADMIN_PWD" >> $ACA_PROP_FILE
	   echo "Mysql root password has been saved locally."
	 fi
	 mysqladmin --user=root password "$DB_ADMIN_PWD"
  else
    DB_ADMIN_PWD=$HIRS_MYSQL_ROOT_PWD
    echo "Using system variable supplied password" | tee -a "$LOG_FILE"
  fi
  # Make sure root password is correct
  $(mysql -u root -p$DB_ADMIN_PWD -e 'quit'  &> /dev/null);
  if [ $? -eq 0 ]; then
     echo "Mysql root password verified"  | tee -a "$LOG_FILE"
  else
     echo "MYSQL root password was not the default, not supplied,  or was incorrect"
     echo "      please set the HIRS_MYSQL_ROOT_PWD system variable and retry."
     echo "      ********** ACA Mysql setup aborted ********" ;
     exit 1;
  fi
}

set_mysql_server_tls () {
  # Check DB server setup. If HIRS ssl params dont exist then we need to add them.
  if [[ $(cat "$DB_SRV_CONF" | grep -c "HIRS") < 1 ]]; then
    # Add TLS files to my.cnf
    echo "Updating $DB_SRV_CONF with ssl parameters..." | tee -a "$LOG_FILE"
    echo "ssl_ca=$SSL_DB_SRV_CHAIN" >> "$DB_SRV_CONF"
    echo "ssl_cert=$SSL_DB_SRV_CERT" >> "$DB_SRV_CONF"
    echo "ssl_key=$SSL_DB_SRV_KEY" >> "$DB_SRV_CONF"
    # Make sure mysql can access them
    chown mysql:mysql $SSL_DB_SRV_CHAIN $SSL_DB_SRV_CERT $SSL_DB_SRV_KEY
    # Make selinux contexts for config files, if selinux is enabled
    if [ $ID = "rhel" ]; then 
        selinuxenabled
        if [ $? -eq 0 ]; then
           semanage fcontext -a -t mysqld_etc_t $DB_SRV_CONF  > /dev/null #adds the context type to file
           restorecon -v -F $DB_SRV_CONF      > /dev/null                 # changes the file's context type
       fi
    fi
  else
       echo "mysql.cnf contians existing entry for ssl, skipping..." | tee -a "$LOG_FILE"
  fi
}

set_mysql_client_tls () {
# Update ACA property file with client cert info, if not there already
if [[ $(cat "$DB_CLIENT_CONF" | grep -c "HIRS") < 1 ]]; then 
  echo "Updating $DB_CLIENT_CONF with ssl parameters..." | tee -a "$LOG_FILE"
  echo "ssl_ca=$SSL_DB_CLIENT_CHAIN" >> $DB_CLIENT_CONF
  echo "ssl_cert=$SSL_DB_CLIENT_CERT" >> $DB_CLIENT_CONF
  echo "ssl_key=$SSL_DB_CLIENT_KEY" >> $DB_CLIENT_CONF
  chown mysql:mysql $SSL_DB_CLIENT_CHAIN $SSL_DB_CLIENT_CERT $SSL_DB_CLIENT_KEY 
  # Make selinux contexts for config files, if selinux is enabled
   if [ $ID = "rhel" ]; then 
      selinuxenabled
      if [ $? -eq 0 ]; then
          semanage fcontext -a -t mysqld_etc_t $DB_CLIENT_CONFf > /dev/null  #adds the context type to file
         restorecon -F $DB_CLIENT_CONF                         > /dev/null #changes the file's context type
      fi
  fi                           
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
     echo "hibernate.connection.username=hirs_db" >> $SPRING_PROP_FILE
     echo "hibernate.connection.password=$HIRS_DB_PWD" >> $SPRING_PROP_FILE
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
#     mysql -u root --password=$DB_ADMIN_PWD -e "ALTER USER 'hirs_db'@'localhost' IDENTIFIED BY '"$HIRS_DB_PWD"'; FLUSH PRIVILEGES;";
     mysql -u root --password=$DB_ADMIN_PWD -e "SET PASSWORD FOR 'hirs_db'@'localhost' = PASSWORD('"$HIRS_DB_PWD"'); FLUSH PRIVILEGES;";
  fi
}

# Create a JDBC connector used by hibernate and place in Springs application.properties
create_hibernate_url () {
 ALG=$1
 db_username=$2

  if [ $ALG = "RSA" ]; then 
    CERT_PATH="/etc/hirs/certificates/HIRS/$RSA_PATH"
    CERT_CHAIN="$CERT_PATH/HIRS_rsa_3k_sha384_Cert_Chain.pem"
    CLIENT_DB_P12=$CERT_PATH/HIRS_db_client_rsa_3k_sha384.p12
    ALIAS="hirs_aca_tls_rsa_3k_sha384"
  else
    CERT_PATH="/etc/hirs/certificates/HIRS/$ECC_PATH"
    CERT_CHAIN="$CERT_PATH/HIRS_ecc_512_sha384_Cert_Chain.pem"
    CLIENT_DB_P12=$CERT_PATH/HIRS_db_client_ecc_512_sha384.p12
    ALIAS="hirs_aca_tls_ecc_512_sha384"
  fi

CONNECTOR_URL="hibernate.connection.url=jdbc:mariadb://localhost:3306/hirs_db?autoReconnect=true&\
user=$db_username&\
password=$HIRS_DB_PWD&\
sslMode=VERIFY_CA&\
serverSslCert=$CERT_CHAIN&\
keyStoreType=PKCS12&\
keyStorePassword=$PKI_PASS&\
keyStore="$CLIENT_DB_P12" "

echo $CONNECTOR_URL >> $SPRING_PROP_FILE

}
# HIRS ACA Mysqld processing ...
check_mariadb_install
check_for_container -p
set_mysql_server_tls
set_mysql_client_tls
start_mysqlsd
check_mysql
check_mysql_root_pwd
set_hirs_db_pwd
create_hirs_db_with_tls
create_hibernate_url "RSA" "hirs_db"
mysqld_reboot
