#!/bin/bash
#
###############################################################################
# HIRS DB creation
# Environment variables used:
# a. HIRS_MYSQL_ROOT_PWD: Set this variable if mysql root password is already set
# b. HIRS_DB_PWD: Set the pwd if default password to hirs_db user needs to be changed
################################################################################

param (
    [string]$LOG_FILE = $null,
	[switch]$unattended = $false
)

$APP_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path "$APP_HOME" .. aca aca_common.ps1)

# Load other scripts
. $ACA_COMMON_SCRIPT
. $global:HIRS_REL_WIN_DB_MYSQL_UTIL

# Read aca.properties
read_aca_properties $global:HIRS_DATA_ACA_PROPERTIES_FILE

# Read spring application.properties
read_spring_properties $global:HIRS_DATA_SPRING_PROP_FILE

# Parameter check
if ($LOG_FILE) {
	touch $LOG_FILE
	$global:LOG_FILE=$LOG_FILE
} else {
	set_up_log
}

touch $global:HIRS_DATA_ACA_PROPERTIES_FILE
touch $global:DB_CONF

# Make sure required paths exist
mkdir -F -p $global:HIRS_CONF_DIR 2>&1 > $null
mkdir -F -p $global:HIRS_DATA_LOG_DIR 2>&1 > $null

Function check_mysql_root_pwd () {
    # Check if DB root password needs to be obtainedS
    $DB_ADMIN_PWD=""
    if (!$Env:HIRS_MYSQL_ROOT_PWD) {
        # Create a 32 character random password
	    echo "Using randomly generated password for the DB admin" | WriteAndLog
	    $DB_ADMIN_PWD=(create_random)
	    Write-Host "NOT LOGGED: DB Admin will be set to $DB_ADMIN_PWD, please make note for next mysql use."
        # Check unattended flag set m if not then prompt user for permission ot store mysql root password
	    if (!$unattended) {
			$confirm=Read-Host 'Do you wish to save this password to the aca.properties file?'
			if (($confirm -eq "y") -or ($confirm -eq "yes")) { # case-insensitive
			    add_new_aca_property "$global:HIRS_DATA_ACA_PROPERTIES_FILE" "mysql_admin_password=$DB_ADMIN_PWD"
	            echo "Mysql root password saved locally" | WriteAndLog
			} else {
	            echo "Mysql root password not saved locally" | WriteAndLog
			}
		} else { # unattended install
	        add_new_aca_property "$global:HIRS_DATA_ACA_PROPERTIES_FILE" "mysql_admin_password=$DB_ADMIN_PWD"
	        echo "Mysql root password has been saved locally." | WriteAndLog
		}
	    mysqladmin --user=root password "$DB_ADMIN_PWD"
	} else {
        $DB_ADMIN_PWD=$Env:HIRS_MYSQL_ROOT_PWD
        echo "Using system variable supplied password" | WriteAndLog
	}
    # Make sure root password is correct

    mysql -u root -p"$DB_ADMIN_PWD" -e 'quit' 2>&1 | WriteAndLog 
	if ($LastExitCode -eq 0) {
        echo "Mysql root password verified"  | WriteAndLog
    } else {
        echo "MYSQL root password was not the default, not supplied,  or was incorrect" | WriteAndLog
        echo "      please set the HIRS_MYSQL_ROOT_PWD system variable and retry." | WriteAndLog
        echo "      ********** ACA Mysql setup aborted ********" | WriteAndLog
        exit 1
	}
	return $DB_ADMIN_PWD
}

Function set_mysql_tls () {
    # Check DB server setup. If ssl params dont exist then we need to add them.
	if (!(Get-Content $global:DB_CONF | grep "ssl")) {
		# Add TLS files to my.ini- Assumes [client] section at the end, and no [server] section
		echo "Updating $global:DB_CONF with ssl parameters..." | WriteAndLog
        echo "ssl_ca=$SSL_DB_CLIENT_CHAIN" >> $global:DB_CONF
		echo "ssl_cert=$SSL_DB_CLIENT_CERT" >> $global:DB_CONF
        echo "ssl_key=$SSL_DB_CLIENT_KEY" >> $global:DB_CONF
		echo "[server]" >> $global:DB_CONF
		echo "ssl_ca=$global:SSL_DB_SRV_CHAIN" >> $global:DB_CONF
		echo "ssl_cert=$global:SSL_DB_SRV_CERT" >> $global:DB_CONF
		echo "ssl_key=$global:SSL_DB_SRV_KEY" >> $global:DB_CONF
		ChangeFileBackslashToForwardSlash $global:DB_CONF
	} else {
        echo "$global:DB_CONF contains existing entry for ssl, skipping..." | WriteAndLog
	}
}

# Process HIRS DB USER
Function set_hirs_db_pwd () {
    param (
        [string]$DB_ADMIN_PWD = $null
    )
	if (!$DB_ADMIN_PWD) {
		echo "set_hirs_db_pwd was called without supplying a required variable" | WriteAndLog
	}
	
    $HIRS_PASS=""
	if ($Env:HIRS_DB_PWD) {
		$HIRS_PASS=$Env:HIRS_DB_PWD
		echo "Using hirs_db password found in the environment variable HIRS_DB_PWD" | WriteAndLog
	} elseif ($global:ACA_PROPERTIES.'hirs_db_password') {
		$HIRS_PASS=$global:ACA_PROPERTIES.'hirs_db_password'
		echo "Using hirs_db password found in the ACA properties file $global:HIRS_DATA_ACA_PROPERTIES_FILE" | WriteAndLog
	} else {
		echo "Using randomly generated password for the DB key password" | WriteAndLog
		$HIRS_PASS=(create_random)
		add_new_aca_property "$global:HIRS_DATA_ACA_PROPERTIES_FILE" "hirs_db_username=hirs_db"
		add_new_aca_property "$global:HIRS_DATA_ACA_PROPERTIES_FILE" "hirs_db_password=$HIRS_PASS"
		echo "Stored hirs_db password in the ACA properties file $global:HIRS_DATA_ACA_PROPERTIES_FILE" | WriteAndLog
	}

    $RESULT=(mysql -u root -p"$DB_ADMIN_PWD" -e "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = 'hirs_db')")
    if ($RESULT -eq 1) {
        echo "hirs-db user exists" | WriteAndLog
    }
	
	return $HIRS_PASS
}

# Create a hirs_db with client side TLS enabled
Function create_hirs_db_with_tls () {
    param (
        [string]$DB_ADMIN_PWD = $null,
		[string]$HIRS_PASS = $null
    )
	if (!$DB_ADMIN_PWD) {
		echo "create_hirs_db_with_tls: DB_ADMIN_PWD not provided and is required" | WriteAndLog
	}
	if (!$HIRS_PASS) {
		echo "create_hirs_db_with_tls: HIRS_PASS not provided and is required" | WriteAndLog
	}
	
    # Check if hirs_db not created and create it if it wasn't
    mysqlshow -u root -p"$DB_ADMIN_PWD" | grep "hirs_db" 2>&1 > $null
	if ($LastExitCode -eq 0) {
        echo "hirs_db exists, skipping hirs_db create" | WriteAndLog
	} else {
		echo "Creating hirs_db database" | WriteAndLog
        mysql -u root -p"$DB_ADMIN_PWD" -e "source $global:HIRS_REL_SCRIPTS_DB_CREATE_SQL"
        mysql -u root -p"$DB_ADMIN_PWD" -e "source $global:HIRS_REL_SCRIPTS_DB_SECURE_MYSQL_SQL"
        mysql -u root -p"$DB_ADMIN_PWD" -e "ALTER USER 'hirs_db'@'localhost' IDENTIFIED BY '$HIRS_PASS'; FLUSH PRIVILEGES;"
	}
}

Function create_hibernate_url () {
    param (
        [string]$ALG = $null
    )
    
    if ($ALG -eq "RSA") {
       $CERT_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_rsa_3k_sha384_Cert_Chain.pem')
       $CLIENT_DB_P12=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_client_rsa_3k_sha384.p12')
       $ALIAS="hirs_aca_tls_rsa_3k_sha384"
    } elseif ($ALG -eq "ECC") {
       $CERT_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_ecc_512_sha384_Cert_Chain.pem')
       $CLIENT_DB_P12=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_db_client_ecc_512_sha384.p12')
       $ALIAS="hirs_aca_tls_ecc_512_sha384"
    }
    
    $CONNECTOR_URL="hibernate.connection.url=jdbc:mariadb://localhost:3306/hirs_db?autoReconnect=true&user="+$global:ACA_PROPERTIES.'hirs_db_username'+"&password="+$global:ACA_PROPERTIES.'hirs_db_password'+"&sslMode=VERIFY_CA&serverSslCert=$CERT_CHAIN&keyStoreType=PKCS12&keyStorePassword="+$global:ACA_PROPERTIES.'hirs_pki_password'+"&keyStore=$CLIENT_DB_P12" | ChangeBackslashToForwardSlash
    
    # Save connector information to the application properties file.
    add_new_spring_property "$global:HIRS_DATA_SPRING_PROP_FILE" "$CONNECTOR_URL"
}

# HIRS ACA Mysqld processing ...
check_mariadb_install -p
check_for_container -p
set_mysql_tls
start_mysqlsd -p
$DB_ADMIN_PWD=check_mysql_root_pwd
$HIRS_PASS=set_hirs_db_pwd -DB_ADMIN_PWD:"$DB_ADMIN_PWD"
create_hirs_db_with_tls -DB_ADMIN_PWD:"$DB_ADMIN_PWD" -HIRS_PASS:"$HIRS_PASS"
create_hibernate_url -ALG:"RSA"
mysqld_reboot -p
