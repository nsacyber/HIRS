#!/bin/bash
#
###############################################################################
# HIRS DB creation
# Environment variables used:
# a. HIRS_MYSQL_ROOT_PWD: Set this variable if mysql root password is already set
# b. HIRS_DB_PWD: Set the pwd if default password to hirs_db user needs to be changed
################################################################################

param (
	[Parameter(Mandatory=$true)]
    [string]$LOG_FILE,
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
if (-not (Test-Path -Path $LOG_FILE)) {
	New-Item -ItemType File -Path $LOG_FILE 
	$global:LOG_FILE=$LOG_FILE
} else {
	set_up_log
}

if (-not (Test-Path -Path $global:HIRS_DATA_ACA_PROPERTIES_FILE)) {
    New-Item -ItemType File -Path $global:HIRS_DATA_ACA_PROPERTIES_FILE
} else {
    Write-Output "File already exists: $global:HIRS_DATA_ACA_PROPERTIES_FILE"
}

if (-not (Test-Path -Path $global:DB_CONF)) {
    New-Item -ItemType File -Path $global:DB_CONF
} else {
    Write-Output "File already exists: $global:DB_CONF"
}


# Make sure required paths exist
New-Item -ItemType Directory -Path $global:HIRS_CONF_DIR -Force | Out-Null
New-Item -ItemType Directory -Path $global:HIRS_DATA_LOG_DIR -Force | Out-Null

# Check if MariaDB is installed
check_mariadb_install -p

# Check if the the ACA is running inside a Docker container
check_for_container -p

Function check_mysql_root_pwd () {
    $DB_ADMIN_PWD=""

    # Check if DB root password needs to be retrieved from the system environment variable or existing property file
    if (!$Env:HIRS_MYSQL_ROOT_PWD) {
        Write-Output "Using randomly generated password for the DB admin" | WriteAndLog
		
		# Attempt to find the mysql password from the aca property file
		$DB_ADMIN_PWD=find_property_value -file:"$global:HIRS_DATA_ACA_PROPERTIES_FILE" -key "mysql_admin_password"

		# if the value associated with the mysql_admin_password key is empty
		if(!$DB_ADMIN_PWD) {
			Write-Output "No DB Admin password has been found in the properties file [$global:HIRS_DATA_ACA_PROPERTIES_FILE]. The script will now create and set the password for the root user." | WriteAndLog

			# Create a 32 character random password
			$DB_ADMIN_PWD=(create_random)
			
			Write-Host "NOT LOGGED: DB Admin password will be set to [$DB_ADMIN_PWD]. Please make note of it for future uses of MYSQL."

			# Check if unattended flag is set if not then prompt user for permission to store mysql root password
			if (!$unattended) {
				$confirm=Read-Host 'Do you wish to save this password to the aca.properties file?'
				if (($confirm -eq "y") -or ($confirm -eq "yes")) { # case-insensitive
					add_new_aca_property -file:"$global:HIRS_DATA_ACA_PROPERTIES_FILE" -newKeyAndValue:"mysql_admin_password=$DB_ADMIN_PWD"
					Write-Output "A new MYSQL password for the root user has been saved locally." | WriteAndLog
				} else {
					Write-Output "MYSQL password for the root user has not been saved locally" | WriteAndLog
				}
			} else { # unattended install
				add_new_aca_property -file:"$global:HIRS_DATA_ACA_PROPERTIES_FILE" -newKeyAndValue:"mysql_admin_password=$DB_ADMIN_PWD"
				Write-Output "A new MYSQL password for the root user has been saved locally." | WriteAndLog	
			}
			mysqladmin --user=root password "$DB_ADMIN_PWD"
		}
	} else {
        $DB_ADMIN_PWD=$Env:HIRS_MYSQL_ROOT_PWD
        Write-Output "Using system variable supplied password" | WriteAndLog
	}

    # Make sure root password is correct
    mysql -u root -p"$DB_ADMIN_PWD" -e 'quit' 2>&1 | WriteAndLog 

	if ($LastExitCode -eq 0) {
        Write-Output "The supplied MYSQL password for the root user has been verified"  | WriteAndLog
    } else {
        Write-Output "The supplied MYSQL password for the root user was incorrect" | WriteAndLog
        Write-Output "********** ACA Mysql setup aborted ********" | WriteAndLog
        exit 1
	}

	return $DB_ADMIN_PWD
}

Function set_mysql_tls () {
    # Check DB server setup. If ssl params dont exist then we need to add them.
	if (!(Get-Content $global:DB_CONF | Select-String "ssl")) {
		# Add TLS files to my.ini- Assumes [client] section at the end, and no [server] section
		Write-Output "Updating $global:DB_CONF with ssl parameters..." | WriteAndLog
        Write-Output "ssl_ca=$SSL_DB_RSA_CLIENT_CHAIN" >> $global:DB_CONF
		Write-Output "ssl_cert=$SSL_DB_RSA_CLIENT_CERT" >> $global:DB_CONF
        Write-Output "ssl_key=$SSL_DB_RSA_CLIENT_KEY" >> $global:DB_CONF
		Write-Output "[server]" >> $global:DB_CONF
		Write-Output "ssl_ca=$global:SSL_DB_RSA_SRV_CHAIN" >> $global:DB_CONF
		Write-Output "ssl_cert=$global:SSL_DB_RSA_SRV_CERT" >> $global:DB_CONF
		Write-Output "ssl_key=$global:SSL_DB_RSA_SRV_KEY" >> $global:DB_CONF
		ChangeFileBackslashToForwardSlash $global:DB_CONF
	} else {
        Write-Output "$global:DB_CONF contains existing entry for ssl. Skipping this step ..." | WriteAndLog
	}
}

# Process HIRS DB USER
Function set_hirs_db_pwd () {
    param (
		[Parameter(Mandatory=$true)]
        [string]$DB_ADMIN_PWD
    )
	if (!$DB_ADMIN_PWD) {
		Write-Output "Exiting script since this function has been called without supplying the database admin password" | WriteAndLog
		exit 1
	}

	$HIRS_DB_USER_EXISTS = check_hirs_db_user -DB_ADMIN_PWD:"$DB_ADMIN_PWD"

	if($HIRS_DB_USER_EXISTS -ne 1){
		$HIRS_DB_PASS=""

		#Check if the HIRS DB user password is being retrieved from the system environment ...
		if ($Env:HIRS_DB_PWD) {
			$HIRS_DB_PASS=$Env:HIRS_DB_PWD
			Write-Output "Using hirs_db password found in the environment variable HIRS_DB_PWD" | WriteAndLog
		} else{ #or if we are setting it to a random value
			$HIRS_DB_PASS=(create_random)
			Write-Output "Using randomly generated password for the HIRS_DB key password" | WriteAndLog
		}

		if(-not (find_property_value -file:"$global:HIRS_DATA_ACA_PROPERTIES_FILE" -key "hirs_db_username")){
			add_new_aca_property -file:"$global:HIRS_DATA_ACA_PROPERTIES_FILE" -newKeyAndValue:"hirs_db_username=hirs_db"
			Write-Output "Stored hirs_db username in the ACA properties file [$global:HIRS_DATA_ACA_PROPERTIES_FILE]" | WriteAndLog
		}

		if(-not (find_property_value -file:"$global:HIRS_DATA_ACA_PROPERTIES_FILE" -key "hirs_db_password")){
			add_new_aca_property -file:"$global:HIRS_DATA_ACA_PROPERTIES_FILE" -newKeyAndValue:"hirs_db_password=$HIRS_DB_PASS"
			Write-Output "Stored hirs_db password in the ACA properties file [$global:HIRS_DATA_ACA_PROPERTIES_FILE]" | WriteAndLog
		}

		if(-not (find_property_value -file:"$global:HIRS_DATA_SPRING_PROP_FILE" -key "hibernate.connection.username")){
			add_new_spring_property -file:"$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue:"hibernate.connection.username=hirs_db"
			Write-Output "Stored the hibernate connection username in the spring properties file [$global:HIRS_DATA_SPRING_PROP_FILE]" | WriteAndLog
		}

		if(-not (find_property_value -file:"$global:HIRS_DATA_SPRING_PROP_FILE" -key "hibernate.connection.password")){
			add_new_spring_property -file:"$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue:"hibernate.connection.password=$HIRS_DB_PASS"
			Write-Output "Stored the hibernate connection password property in the spring properties file [$global:HIRS_DATA_SPRING_PROP_FILE]" | WriteAndLog
		}
	}
	else {
		 Write-Output "hirs_db user already exists. Skipping this step." | WriteAndLog
	}
}

# Create a hirs_db with client side TLS enabled
Function create_hirs_db_with_tls () {
    param (
		[Parameter(Mandatory=$true)]
        [string]$DB_ADMIN_PWD
    )

	if (!$DB_ADMIN_PWD) {
		Write-Output "Exiting script since this function has been called without supplying the database admin password" | WriteAndLog
		exit 1
	}

	$HIRS_DB_EXISTS = check_hirs_db -DB_ADMIN_PWD:"$DB_ADMIN_PWD"

	#if the hirs_db has already been created, skip this step
	if($HIRS_DB_EXISTS -eq 1){
      Write-Output "hirs_db already exists. Skipping this step" | WriteAndLog
	} else { #othewrise create the hirs_db
	  $HIRS_PASS=find_property_value -file:"$global:HIRS_DATA_ACA_PROPERTIES_FILE" -key:"hirs_db_password"

	  if(!$HIRS_PASS){
		Write-Output "Exiting script since the property file does not have the hirs_db password" | WriteAndLog
		exit 1
	  }

      Write-Output "Creating hirs_db database" | WriteAndLog
      mysql -u root -p"$DB_ADMIN_PWD" -e "source $global:HIRS_REL_SCRIPTS_DB_CREATE_SQL"
      mysql -u root -p"$DB_ADMIN_PWD" -e "source $global:HIRS_REL_SCRIPTS_DB_SECURE_MYSQL_SQL"
      mysql -u root -p"$DB_ADMIN_PWD" -e "ALTER USER 'hirs_db'@'localhost' IDENTIFIED BY '$HIRS_PASS'; FLUSH PRIVILEGES;"
	  Write-Output "Finished creating the hirs_db database and setting the hirs_db pwd using the contents of the file [$global:HIRS_DATA_ACA_PROPERTIES_FILE]"
	}
}

Function create_hibernate_url () {
    param (
		[Parameter(Mandatory=$true)]
        [string]$ALG
    )
    
    if ($ALG -eq "RSA") {
       $CERT_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_rsa_3k_sha384_Cert_Chain.pem')
       $CLIENT_DB_P12=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_client_rsa_3k_sha384.p12')
    } elseif ($ALG -eq "ECC") {
       $CERT_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_ecc_512_sha384_Cert_Chain.pem')
       $CLIENT_DB_P12=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_db_client_ecc_512_sha384.p12')
    }
    
    $CONNECTOR_URL="hibernate.connection.url=jdbc:mariadb://localhost:3306/hirs_db?autoReconnect=true&user="+$global:ACA_PROPERTIES.'hirs_db_username'+"&password="+$global:ACA_PROPERTIES.'hirs_db_password'+"&sslMode=VERIFY_CA&serverSslCert=$CERT_CHAIN&keyStoreType=PKCS12&keyStorePassword="+$global:ACA_PROPERTIES.'hirs_pki_password'+"&keyStore=$CLIENT_DB_P12" | ChangeBackslashToForwardSlash
    
    # Save connector information to the application win properties file
    add_new_spring_property -file:"$global:HIRS_DATA_SPRING_PROP_FILE" -newKeyAndValue:"$CONNECTOR_URL"
}

# Setup the ssl settings in the my.ini settings file that's in the C:\\Program Files\MariaDB 11.1\data directory
set_mysql_tls

# Start the MariaDB service
start_mysqlsd -p

# Check for the MariaDB ADMIN password and set the MariaDB password for the root user
$DB_ADMIN_PWD=check_mysql_root_pwd

# Set the password for the hirs_db user in the aca properties and spring properties files
set_hirs_db_pwd -DB_ADMIN_PWD:"$DB_ADMIN_PWD"

# Create the hirs_db and hirs_db user with the values that were set in the aca properties file
create_hirs_db_with_tls -DB_ADMIN_PWD:"$DB_ADMIN_PWD"

# Create the hibernate url using the RSA algorithm and set the url in the aca.properties file
create_hibernate_url -ALG:"RSA"

# Reboot mariadb service
mysqld_reboot -p