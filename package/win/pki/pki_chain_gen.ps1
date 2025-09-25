#!/bin/bash
# Script to generate a PKI Stack (Root, Intermediate, and LEAF CAs) and a Base RIM Signer
# creates a folder based upon the actor name and places certs under an algoithm specific folder (e.g. rsa_certs)
# PARAMS: 
# 1. Actor name string (e.g. "Server Manufacturer") 
# 2. Algorithm string (e.g. rsa or ecc)
# 3. Key Bit Size string (e.g. 2048)
# 4. Hash Algorithm string (e.g. sha256)
# 5. PKI password used to protect PKI keys and certs
#
# Examples:
#    pki_chain_gen.sh "PC Manufacturer" rsa 2048 sha256 "password" 
#    pki_chain_gen.sh "DISK Manufacturer" ecc 256 sha512 "password"
#
# A KeyStore and Trust Store are created for by Java Applications. Both will use the supplied password. 

param (
	[Parameter(Mandatory=$true)]
    [string]$ACTOR,
	[Parameter(Mandatory=$true)]
	[string]$ASYM_ALG,
	[Parameter(Mandatory=$true)]
	[string]$ASYM_SIZE,
	[Parameter(Mandatory=$true)]
	[string]$HASH_ALG,
	[Parameter(Mandatory=$true)]
	[string]$PASS,
	[Parameter(Mandatory=$true)]
	[string]$LOG_FILE
)

$ACA_SCRIPTS_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path $ACA_SCRIPTS_HOME '..' 'aca' 'aca_common.ps1')

# Load other scripts
. $ACA_COMMON_SCRIPT

# Read aca.properties
read_aca_properties $global:HIRS_DATA_ACA_PROPERTIES_FILE

# Parameter check 
if (!$ACTOR -or !$ASYM_ALG -or !$ASYM_SIZE -or !$HASH_ALG -or ($ACTOR -eq "-h") -or ($ACTOR -eq "--help")) {
   Write-Output "parameter missing to pki_chain_gen.sh, exiting pki setup" | WriteAndLog
   exit 1;
}
if ($LOG_FILE) {
	$global:LOG_FILE=$LOG_FILE
} else {
	set_up_log
}

$ACTOR_ALT=("$ACTOR" -replace " ","_")
$ROOT_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$ACTOR test root ca"
$INT_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$ACTOR test intermediate ca"
$LEAF_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$ACTOR test ca"
$SIGNER_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$ACTOR test signer"
$SERVER_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$ACTOR aca"
$TRUSTSTORE="$global:HIRS_DATA_CERTIFICATES_DIR\$ACTOR_ALT\TrustStore.jks"
$TRUSTSTORE_P12="$global:HIRS_DATA_CERTIFICATES_DIR\$ACTOR_ALT\TrustStore.p12"
$KEYSTORE="$global:HIRS_DATA_CERTIFICATES_DIR\$ACTOR_ALT\KeyStore.jks"

if (($ASYM_ALG -ne "rsa") -and ($ASYM_ALG -ne "ecc")) {
	Write-Output "$ASYM_ALG is an unsupported assymetric algorithm, exiting pki setup" | WriteAndLog
	exit 1;
}

switch ($ASYM_SIZE) {
	256 { 
	    $KSIZE="256"
        $ECC_NAME="secp256k1"
		Break
	}
    384 {
		$KSIZE="384"
        $ECC_NAME="secp384r1"
	    Break
	}
    512 {
		$KSIZE="512"
        $ECC_NAME="secp521r1"
		Break
	}
    2048 {
		$KSIZE="2k"
		Break
	}
    3072 {
		$KSIZE="3k"
		Break
	}
    4096 {
		$KSIZE="4k"
		Break
	}
    Default {
        Write-Output "$ASYM_SIZE is an unsupported key size, exiting pki setup" | WriteAndLog
        exit 1
    } 
}

# Use algorithm and key size to create unique file paths and Distinguished names
$NAME="$ACTOR $ASYM_ALG $KSIZE $HASH_ALG"
$CERT_FOLDER="$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"+"_certs"
$PKI_ROOT="$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER\$ACTOR_ALT"+"_"+"root_ca_"+"$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"
$PKI_INT="$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER\$ACTOR_ALT"+"_intermediate_ca_"+"$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"
$PKI_CA1="$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER\$ACTOR_ALT"+"_leaf_ca1_"+"$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"
$PKI_CA2="$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER\$ACTOR_ALT"+"_leaf_ca2_"+"$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"
$PKI_CA3="$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER\$ACTOR_ALT"+"_leaf_ca3_"+"$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"
$RIM_SIGNER="$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER\$ACTOR_ALT"+"_rim_signer_"+"$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"
$TLS_SERVER="$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER\$ACTOR_ALT"+"_aca_tls_"+"$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"
$DB_SERVER="$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER\$ACTOR_ALT"+"_db_srv_"+"$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"
$DB_CLIENT="$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER\$ACTOR_ALT"+"_db_client"+"_"+"$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"
$TRUST_STORE_FILE="$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER\$ACTOR_ALT"+"_"+"$ASYM_ALG"+"_"+"$KSIZE"+"_"+"$HASH_ALG"+"_Cert_Chain.pem"

$ROOT_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$NAME test root ca"
$INT_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$NAME test intermediate ca"
$LEAF_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$NAME test ca"
$SIGNER_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$NAME test signer"
$TLS_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$NAME portal"
$DB_SRV_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$NAME DB Server"
$DB_CLIENT_DN="/C=US/ST=MD/L=Columbia/O=$ACTOR/CN=$NAME DB Client"

# Add check for existing folder and halt if it exists
if ([System.IO.Directory]::Exists("$ACTOR_ALT/$CERT_FOLDER")) {
   Write-Output "Folder for $CERT_FOLDER exists, exiting..." | WriteAndLog
   exit 1
}

# Intialize sub folders
Write-Output "Creating PKI for $ACTOR_ALT using $KSIZE $ASYM_ALG and $HASH_ALG..." | WriteAndLog

New-Item -ItemType Directory -Path $global:HIRS_DATA_CERTIFICATES_HIRS_DIR -Force | Out-Null
New-Item -ItemType Directory -Path "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\$CERT_FOLDER" -Force | Out-Null
New-Item -ItemType Directory -Path "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\ca\certs" -Force | Out-Null
Copy-Item $global:HIRS_DATA_CERTIFICATES_DIR\ca.conf $global:HIRS_DATA_CERTIFICATES_HIRS_DIR | WriteAndLog

if (-not (Test-Path "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\ca\db")) {
    New-Item -ItemType File -Path "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\ca\db"
} else {
    Write-Output "File already exists: $global:HIRS_DATA_CERTIFICATES_HIRS_DIR\ca\db"
}

if (-not (Test-Path "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\openssl-san.cnf")) {
    New-Item -ItemType File -Path "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\openssl-san.cnf"
} else {
    Write-Output "File already exists: $global:HIRS_DATA_CERTIFICATES_HIRS_DIR\openssl-san.cnf"
}

if (![System.IO.File]::Exists("$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\ca\serial.txt")) {
     Write-Output "01" > "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\ca\serial.txt" | WriteAndLog
}

# Function to add Cert to Truststore and key to Keystore
Function add_to_stores () {
    param (
		[Parameter(Mandatory=$true)]
        [string]$CERT_PATH
    )

    $ALIAS=[System.IO.Path]::GetFileName($CERT_PATH)    # Use filename without path as an alias
	Write-Output "$CERT_PATH key objects will use the alias `"$ALIAS`" in trust stores." | WriteAndLog 
    Write-Output "Exporting key and certificate to PKCS12." | WriteAndLog 
    # Add the cert and key to the key store.. make a p12 file to import into te keystore
    openssl pkcs12 -export -in "$CERT_PATH.pem" -inkey "$CERT_PATH.key" -out "tmpkey.p12" -passin "pass:$PASS" -macalg SHA256 -keypbe AES-256-CBC -certpbe AES-256-CBC -passout "pass:$PASS"  2>&1 | WriteAndLog
    Write-Output "Adding $ALIAS to the $KEYSTORE" | WriteAndLog 
	# Use the p12 file to import into a java keystore via keytool
    keytool -importkeystore -srckeystore "tmpkey.p12" -destkeystore $KEYSTORE -srcstoretype pkcs12 -srcstorepass $PASS -deststoretype jks -deststorepass $PASS -noprompt -alias 1 -destalias "$ALIAS"  2>&1 | WriteAndLog
    Write-Output "Adding $ALIAS to the $TRUSTSTORE" | WriteAndLog 
	# Import the cert into a java trust store via keytool
    keytool -import -keystore $TRUSTSTORE -storepass $PASS -file "$CERT_PATH.pem"  -noprompt -alias "$ALIAS" 2>&1 | WriteAndLog
    # Remove the temp p1 file.
    Write-Output "Cleaning up after storing $ALIAS" | WriteAndLog 
	Remove-Item "tmpkey.p12"
} 

# Function to create an Intermediate Key, CSR, and Certificate
# PARMS: 
# 1. Cert Type String
# 2. Issuer Key File Name
# 3. Subject Distinguished Name String

Function create_cert () {
    param (
		[Parameter(Mandatory=$true)]
        [string]$CERT_PATH,
		[Parameter(Mandatory=$true)]
        [string]$ISSUER,
		[Parameter(Mandatory=$true)]
        [string]$SUBJ_DN,
		[Parameter(Mandatory=$true)]
        [string]$EXTENSION
    )
	
	$ISSUER_KEY="$ISSUER.key"
	$ISSUER_CERT="$ISSUER.pem"
	$ALIAS=[System.IO.Path]::GetFileName($CERT_PATH)    # Use filename without path as an alias
	Write-Output "Creating key pair and CSR with DN=`"$SUBJ_DN`"." | WriteAndLog
	Write-Output "    Key pair will be saved at location $CERT_PATH\" | WriteAndLog
	Write-Output "    Key will use $ASYM_ALG $ASYM_SIZE and HASH Alg $HASH_ALG" | WriteAndLog 
	Write-Output "    Certificate will be issued by $ISSUER_CERT and its key $ISSUER_KEY." | WriteAndLog 
	Write-Output "    Key objects will use the alias `"$ALIAS`" in trust stores." | WriteAndLog 

    # Database doesnt support encypted key so create DB without passwords 
	if ("$SUBJ_DN" -match "DB") {
		Write-Output "This key is intended for use with the database." | WriteAndLog 
		if ("$ASYM_ALG" -eq "rsa") {
            openssl genrsa -out "$CERT_PATH.key" "$ASYM_SIZE" 2>&1 | WriteAndLog
            openssl req -new -key "$CERT_PATH.key" -out "$CERT_PATH.csr" -subj "$SUBJ_DN" 2>&1 | WriteAndLog
		} else {
	        openssl ecparam -genkey -name "$ECC_NAME" -out "$CERT_PATH.key" 2>&1 | WriteAndLog
	        openssl req -new -key "$CERT_PATH.key" -out "$CERT_PATH.csr" -$HASH_ALG  -subj "$SUBJ_DN" 2>&1 | WriteAndLog
		}
	} else {
		if ("$ASYM_ALG" -eq "rsa") {
            openssl req -newkey rsa:"$ASYM_SIZE" -keyout "$CERT_PATH.key" -out "$CERT_PATH.csr"  -subj "$SUBJ_DN" -passout "pass:$PASS" 2>&1 | WriteAndLog
		} else {
	        openssl genpkey -algorithm "EC" -pkeyopt ec_paramgen_curve:P-521 -aes256 --pass "pass:$PASS" -out "$CERT_PATH.key" 2>&1 | WriteAndLog 
	        openssl req -new -key "$CERT_PATH.key" -passin "pass:$PASS" -out "$CERT_PATH.csr" -$HASH_ALG  -subj "$SUBJ_DN" 2>&1 | WriteAndLog 
		}
	}

	Write-Output "Sending CSR to the CA." | WriteAndLog
	Push-Location "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR" | WriteAndLog
	openssl ca -config ca.conf -keyfile "$ISSUER_KEY" -md $HASH_ALG -cert "$ISSUER_CERT" -extensions "$EXTENSION" -out "$CERT_PATH.pem" -in "$CERT_PATH.csr" -passin "pass:$PASS" -batch -notext 2>&1 | WriteAndLog       
	Pop-Location | WriteAndLog
    # Increment the cert serial number
    $SERIAL=(Get-Content "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR\ca\serial.txt")
    Write-Output "Cert Serial Number = $SERIAL" | WriteAndLog
    Write-Output "Exporting key and certificate to PKCS12." | WriteAndLog 
	# Add the cert and key to the key store. make a p12 file to import into te keystore
    openssl pkcs12 -export -in "$CERT_PATH.pem" -inkey "$CERT_PATH.key" -out "tmpkey.p12" -passin "pass:$PASS" -macalg SHA256 -keypbe AES-256-CBC -certpbe AES-256-CBC -passout "pass:$PASS" 2>&1 | WriteAndLog 
    Write-Output "Adding $ALIAS to $KEYSTORE" | WriteAndLog 
	# Use the p12 file to import into a java keystore via keytool
    keytool -importkeystore -srckeystore "tmpkey.p12" -destkeystore $KEYSTORE -srcstoretype pkcs12 -srcstorepass $PASS -deststoretype jks -deststorepass $PASS -noprompt -alias 1 -destalias "$ALIAS" 2>&1 | WriteAndLog  
    # Import the cert into a java trust store via keytool
    Write-Output "Adding $ALIAS to $TRUSTSTORE" | WriteAndLog 
	keytool -import -keystore $TRUSTSTORE -storepass $PASS -file "$CERT_PATH.pem"  -noprompt -alias "$ALIAS" 2>&1 | WriteAndLog 
    Write-Output "Cleaning up after storing $ALIAS" | WriteAndLog 
	# Remove the temp p12 file.
    Remove-Item "tmpkey.p12"# remove csr file
    Remove-Item "$CERT_PATH.csr"
}

Function create_cert_chain () {

	# Create an intermediate CA, Sign with Root CA
	create_cert "$PKI_INT" "$PKI_ROOT" "$INT_DN" "ca_extensions"

	# Create a Leaf CA (CA1), Sign with intermediate CA
	create_cert "$PKI_CA1" "$PKI_INT" ("$LEAF_DN"+1) "ca_extensions"

	# Create a Leaf CA (CA2), Sign with intermediate CA
	create_cert "$PKI_CA2" "$PKI_INT" ("$LEAF_DN"+2) "ca_extensions"

	# Create a Leaf CA (CA3), Sign with intermediate CA
	create_cert "$PKI_CA3" "$PKI_INT" ("$LEAF_DN"+3) "ca_extensions"

	# Create a RIM Signer
	create_cert "$RIM_SIGNER" "$PKI_CA2" "$SIGNER_DN" "signer_extensions"

	# Create a ACA Sever Cert for TLS use
	create_cert "$TLS_SERVER" "$PKI_CA3" "$TLS_DN" "server_extensions"

	# Create a DB Sever Cert for TLS use
	create_cert "$DB_SERVER" "$PKI_CA3" "$DB_SRV_DN" "server_extensions"

	# Create a ACA Sever Cert for TLS use
	create_cert "$DB_CLIENT" "$PKI_CA3" "$DB_CLIENT_DN" "server_extensions"

    Write-Output "Creating concatenated PEM file." | WriteAndLog
	# Create Cert trust store by adding the Intermediate and root certs 
	Get-Content "$PKI_CA1.pem", "$PKI_CA2.pem", "$PKI_CA3.pem", "$PKI_INT.pem", "$PKI_ROOT.pem" | Set-Content "$TRUST_STORE_FILE"

    Write-Output "Verifying the concatenated PEM file works." | WriteAndLog
	# Checking signer cert using trust store...
	openssl verify -CAfile "$TRUST_STORE_FILE" "$RIM_SIGNER.pem" | WriteAndLog

    Write-Output "Creating a PKCS12 file for the database client." | WriteAndLog
	# Make JKS files for the mysql DB connector. P12 first then JKS...
	openssl pkcs12 -export -in "$DB_CLIENT.pem" -inkey "$DB_CLIENT.key" -macalg SHA256 -keypbe AES-256-CBC -certpbe AES-256-CBC -passin "pass:$PASS" -passout "pass:$PASS" -name "mysqlclientkey" -out "$DB_CLIENT.p12" 2>&1 | WriteAndLog

    Write-Output "Converting the database client PKCS12 file to JKS." | WriteAndLog
	keytool -importkeystore -srckeystore "$DB_CLIENT.p12" -srcstoretype PKCS12 -srcstorepass $PASS -destkeystore "$DB_CLIENT.jks" -deststoretype jks -deststorepass $PASS 2>&1 | WriteAndLog
}

if ("$ASYM_ALG" -eq "rsa") {
	# Create Root CA key pair and self signed cert
	Write-Output "Generating RSA Root CA ...." | WriteAndLog
	openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -aes256 --pass "pass:$PASS" -out "$PKI_ROOT.key" 2>&1 | WriteAndLog

	# Create a self signed CA certificate
	Push-Location "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR" | WriteAndLog
	openssl req -new -config ca.conf -x509 -days 3650 -key "$PKI_ROOT.key" -subj "$ROOT_DN" -extensions ca_extensions -out "$PKI_ROOT.pem" -passin "pass:$PASS" 2>&1 | WriteAndLog
	Pop-Location | WriteAndLog
	# Add the CA root cert to the Trust and Key stores
	add_to_stores "$PKI_ROOT"
	# Create an intermediate CA, 2 Leaf CAs, and Signer Certs 
	create_cert_chain 
}

if ("$ASYM_ALG" -eq "ecc") {
	# Create Root CA key pair and self signed cert
	Write-Output "Generating Ecc Root CA ...." | WriteAndLog
	openssl genpkey -algorithm "EC" -pkeyopt ec_paramgen_curve:P-521 -aes256 --pass "pass:$PASS" -out "$PKI_ROOT.key" 2>&1 | WriteAndLog

	# Create a self signed CA certificate
	Push-Location "$global:HIRS_DATA_CERTIFICATES_HIRS_DIR" | WriteAndLog
	openssl req -new -config ca.conf -x509 -days 3650 -key "$PKI_ROOT.key" -subj "$ROOT_DN" -extensions ca_extensions -out "$PKI_ROOT.pem" -passin "pass:$PASS" 2>&1 | WriteAndLog
	Pop-Location | WriteAndLog
	# Add the CA root cert to the Trust and Key stores
	add_to_stores "$PKI_ROOT"
	# Create an intermediate CA, 2 Leaf CAs, and Signer Certs 
	create_cert_chain
}