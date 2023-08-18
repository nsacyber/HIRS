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

ACTOR=$1
ACTOR_ALT=${ACTOR// /_}
ASYM_ALG=$2
ASYM_SIZE=$3
HASH_ALG=$4
PASS=$5
LOG_FILE=$6
ROOT_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$ACTOR" test root ca"
INT_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$ACTOR" test intermediate ca"
LEAF_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$ACTOR" test ca"
SIGNER_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$ACTOR" test signer"
SERVER_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$ACTOR" aca"
# Capture location of the script to allow from invocation from any location
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
TRUSTSTORE=TrustStore.jks
TRUSTSTORE_P12=TrustStore.p12
KEYSTORE=KeyStore.jks

# Parameter check 
if [ -z "${ACTOR}" ] || [ -z "${ASYM_ALG}" ] || [ -z "${ASYM_SIZE}" ] || [ -z "${HASH_ALG}" ] || [ "${ACTOR}" == "-h" ] || [ "${ACTOR}" == "--help" ]; then
   echo "parameter missing to pki_chain_gen.sh, exiting pki setup" | tee -a "$LOG_FILE"
   exit 1;
fi

if ! { [ $ASYM_ALG == "rsa" ] || [ $ASYM_ALG == "ecc" ]; }; then
       echo "$ASYM_ALG is an unsupported assymetric algorithm, exiting pki setup" | tee -a "$LOG_FILE"
       exit 1;
fi

if [ -z ${LOG_FILE} ]; then
       LOG_FILE="/dev/null"
fi

case $ASYM_SIZE in
     256)  KSIZE=256
           ECC_NAME="secp256k1";;   
     384)  KSIZE=384
           ECC_NAME="secp384r1";;    
     512)  KSIZE=512
           ECC_NAME="secp521r1";; 
     2048) KSIZE=2k;;
     3072) KSIZE=3k;;
     4096) KSIZE=4k;;
     *) 
       echo "$ASYM_SIZE is an unsupported key size, exiting pki setup" | tee -a "$LOG_FILE"
       exit 1;;
esac

# Use algorithm and key size to create unique file paths and Distinguished names
NAME="$ACTOR $ASYM_ALG $KSIZE $HASH_ALG"
CERT_FOLDER="$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"_certs
PKI_ROOT="$CERT_FOLDER"/"$ACTOR_ALT"_root_ca_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"
PKI_INT="$CERT_FOLDER"/"$ACTOR_ALT"_intermediate_ca_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"
PKI_CA1="$CERT_FOLDER"/"$ACTOR_ALT"_leaf_ca1_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"
PKI_CA2="$CERT_FOLDER"/"$ACTOR_ALT"_leaf_ca2_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"
PKI_CA3="$CERT_FOLDER"/"$ACTOR_ALT"_leaf_ca3_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"
RIM_SIGNER="$CERT_FOLDER"/"$ACTOR_ALT"_rim_signer_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"
TLS_SERVER="$CERT_FOLDER"/"$ACTOR_ALT"_aca_tls_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"
DB_SERVER="$CERT_FOLDER"/"$ACTOR_ALT"_db_srv_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"
DB_CLIENT="$CERT_FOLDER"/"$ACTOR_ALT"_db_client_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"
TRUST_STORE_FILE="$CERT_FOLDER"/"$ACTOR_ALT"_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"_Cert_Chain.pem

ROOT_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" test root ca"
INT_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" test intermediate ca"
LEAF_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" test ca"
SIGNER_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" test signer"
TLS_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" portal"
DB_SRV_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" DB Server"
DB_CLIENT_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" DB Client"

# Add check for existing folder and halt if it exists
if [ -d "$ACTOR_ALT"/"$CERT_FOLDER" ]; then
   echo "Folder for $CERT_FOLDER exists, exiting..." | tee -a "$LOG_FILE"
   exit 1;
fi

# Intialize sub folders
echo "Creating PKI for $ACTOR_ALT using $KSIZE $ASYM_ALG and $HASH_ALG..." | tee -a "$LOG_FILE"

mkdir -p "$ACTOR_ALT" "$ACTOR_ALT"/"$CERT_FOLDER" "$ACTOR_ALT"/ca/certs
cp ca.conf "$ACTOR_ALT"/.
pushd "$ACTOR_ALT" &> /dev/null
touch ca/db
touch openssl-san.cnf
if [ ! -f "ca/serial.txt" ]; then
     echo "01" > ca/serial.txt | tee -a "$LOG_FILE"
fi

# Function to add Cert to Truststore and key to Keystore
add_to_stores () {
   CERT_PATH=$1
   ALIAS=${CERT_PATH#*/}    # Use filename without path as an alias
   echo "Addding $ALIAS to the $TRUSTSTORE and $KEYSTORE" | tee -a "$LOG_FILE" 
   # Add the cert and key to the key store. make a p12 file to import into te keystore
   openssl pkcs12 -export -in "$CERT_PATH".pem -inkey "$CERT_PATH".key -out tmpkey.p12 -passin pass:"$PASS" -aes256 -passout pass:$PASS  >> "$LOG_FILE" 2>&1
   # Use the p12 file to import into a java keystore via keytool
   keytool -importkeystore -srckeystore tmpkey.p12 -destkeystore $KEYSTORE -srcstoretype pkcs12 -srcstorepass $PASS -deststoretype jks -deststorepass $PASS -noprompt -alias 1 -destalias -J-Dcom.redhat.fips=false "$ALIAS" >> "$LOG_FILE" 2>&1 
   # Import the cert into a java trust store via keytool
   keytool -import -keystore $TRUSTSTORE -storepass $PASS -file "$CERT_PATH".pem  -noprompt -alias "$ALIAS" -J-Dcom.redhat.fips=false >> "$LOG_FILE" 2>&1
   # Remove the temp p1 file.
   rm tmpkey.p12
} 

# Function to create an Intermediate Key, CSR, and Certificate
# PARMS: 
# 1. Cert Type String
# 2. Issuer Key File Name
# 3. Subject Distinguished Name String

create_cert () {
   CERT_PATH="$1"
   ISSUER="$2"
   SUBJ_DN="$3"
   EXTENSION="$4"
   ISSUER_KEY="$ISSUER".key
   ISSUER_CERT="$ISSUER".pem
   ALIAS=${CERT_PATH#*/}    # Use filename without path as an alias    

   echo "Creating cert using "$ISSUER_KEY" with a DN="$SUBJ_DN"..." | tee -a "$LOG_FILE"

   # Database doesnt support encypted key so create DB without passwords 
   if [[ "$SUBJ_DN" = *"DB"* ]]; then
       if [ "$ASYM_ALG" == "rsa" ]; then 
           openssl genrsa -out "$CERT_PATH".key "$ASYM_SIZE" >> "$LOG_FILE" 2>&1
           openssl req -new -key "$CERT_PATH".key \
                -out "$CERT_PATH".csr  -subj "$SUBJ_DN" >> "$LOG_FILE" 2>&1
	   else
	       openssl ecparam -genkey -name "$ECC_NAME" -out "$CERT_PATH".key  >> "$LOG_FILE" 2>&1
	       openssl req -new -key "$CERT_PATH".key -out "$CERT_PATH".csr -$HASH_ALG  -subj "$SUBJ_DN" >> "$LOG_FILE" 2>&1
	   fi
   else
       if [ "$ASYM_ALG" == "rsa" ]; then 
           openssl req -newkey rsa:"$ASYM_SIZE" \
                -keyout "$CERT_PATH".key \
                -out "$CERT_PATH".csr  -subj "$SUBJ_DN" \
                -passout pass:"$PASS"  >> "$LOG_FILE" 2>&1
	   else
	       openssl genpkey -algorithm "EC" -pkeyopt ec_paramgen_curve:P-521 -aes256 --pass "pass:$PASS" -out "$CERT_PATH".key 
	       openssl req -new -key "$CERT_PATH".key -passin "pass:$PASS" -out "$CERT_PATH".csr -$HASH_ALG  -subj "$SUBJ_DN" 
	   fi
	 
   fi
     openssl ca -config ca.conf \
           -keyfile "$ISSUER_KEY" \
           -md $HASH_ALG \
           -cert "$ISSUER_CERT" \
           -extensions "$EXTENSION" \
           -out "$CERT_PATH".pem \
           -in "$CERT_PATH".csr \
           -passin pass:"$PASS" \
           -batch \
           -notext       >> "$LOG_FILE" 2>&1       
   # Increment the cert serial number
   SERIAL=$(awk -F',' '{printf("%s\t%d\n",$1,$2+1)}' ./ca/serial.txt)
   echo "Cert Serial Number = $SERIAL" >> "$LOG_FILE";
   # remove csr file
   rm -f "$CERT_PATH".csr
   # Add the cert and key to the key store. make a p12 file to import into te keystore
   openssl pkcs12 -export -in "$CERT_PATH".pem -inkey "$CERT_PATH".key -out tmpkey.p12 -passin pass:"$PASS" -aes256 -passout pass:$PASS  >> "$LOG_FILE" 2>&1
   # Use the p12 file to import into a java keystore via keytool
   keytool -importkeystore -srckeystore tmpkey.p12 -destkeystore $KEYSTORE -srcstoretype pkcs12 -srcstorepass $PASS -deststoretype jks -deststorepass $PASS -noprompt -alias 1 -destalias -J-Dcom.redhat.fips=false "$ALIAS" >> "$LOG_FILE" 2>&1 
   # Import the cert into a java trust store via keytool
   keytool -import -keystore $TRUSTSTORE -storepass $PASS -file "$CERT_PATH".pem  -noprompt -alias "$ALIAS" -J-Dcom.redhat.fips=false >> "$LOG_FILE" 2>&1
   # Remove the temp p1 file.
   rm -f tmpkey.p12 &>/dev/null
}

create_cert_chain () {

   # Create an intermediate CA, Sign with Root CA
   create_cert "$PKI_INT" "$PKI_ROOT" "$INT_DN" "ca_extensions"

   # Create a Leaf CA (CA1), Sign with intermediate CA
   create_cert "$PKI_CA1" "$PKI_INT" "$LEAF_DN"1 "ca_extensions"

   # Create a Leaf CA (CA2), Sign with intermediate CA

   create_cert "$PKI_CA2" "$PKI_INT" "$LEAF_DN"2 "ca_extensions"

   # Create a Leaf CA (CA3), Sign with intermediate CA

   create_cert "$PKI_CA3" "$PKI_INT" "$LEAF_DN"3 "ca_extensions"

   # Create a RIM Signer
   create_cert "$RIM_SIGNER" "$PKI_CA2" "$SIGNER_DN" "signer_extensions"

   # Create a ACA Sever Cert for TLS use
   create_cert "$TLS_SERVER" "$PKI_CA3" "$TLS_DN" "server_extensions"

   # Create a DB Sever Cert for TLS use
   create_cert "$DB_SERVER" "$PKI_CA3" "$DB_SRV_DN" "server_extensions"
   
   # Create a ACA Sever Cert for TLS use
   create_cert "$DB_CLIENT" "$PKI_CA3" "$DB_CLIENT_DN" "server_extensions"
   
   # Create Cert trust store by adding the Intermediate and root certs 
   cat "$PKI_CA1.pem" "$PKI_CA2.pem" "$PKI_CA3.pem" "$PKI_INT.pem" "$PKI_ROOT.pem" >  "$TRUST_STORE_FILE"

 # echo "Checking signer cert using tust store..." 
   openssl verify -CAfile "$TRUST_STORE_FILE" $RIM_SIGNER.pem | tee -a "$LOG_FILE"
   
   # Make JKS files for the mysql DB connector. P12 first then JKS...
   openssl pkcs12 -export -in $DB_CLIENT.pem -inkey $DB_CLIENT.key -aes256 \
        -passin pass:"$PASS"-passout pass:$PASS -aes256  \
        -name "mysqlclientkey" -out $DB_CLIENT.p12

   keytool -importkeystore -srckeystore $DB_CLIENT.p12 -srcstoretype PKCS12 \
         -srcstorepass $PASS -destkeystore $DB_CLIENT.jks -deststoretype JKS -deststorepass $PASS
         
   # Make a p12 TrustStore 
   keytool -importkeystore -srckeystore $TRUSTSTORE -destkeystore $TRUSTSTORE_P12 \
         -srcstoretype JKS -deststoretype PKCS12 -srcstorepass $pass -deststorepass $pass -noprompt
}

if [ "$ASYM_ALG" == "rsa" ]; then
   # Create Root CA key pair and self signed cert
   echo "Generating RSA Root CA ...." | tee -a "$LOG_FILE"
   openssl genrsa -out "$PKI_ROOT".key -aes256 -passout pass:"$PASS" "$ASYM_SIZE" >> "$LOG_FILE" 2>&1
   
   # Create a self signed CA certificate
   openssl req -new -config ca.conf -x509 -days 3650 -key "$PKI_ROOT".key -subj "$ROOT_DN" \
          -extensions ca_extensions -out "$PKI_ROOT".pem \
          -passin pass:"$PASS" >> "$LOG_FILE" 2>&1
   # Add the CA root cert to the Trust and Key stores
   add_to_stores $PKI_ROOT
   # Create an intermediate CA, 2 Leaf CAs, and Signer Certs 
   create_cert_chain 
fi

if [ "$ASYM_ALG" == "ecc" ]; then
    # Create Root CA key pair and self signed cert
    echo "Generating Ecc Root CA ...." | tee -a "$LOG_FILE"
    #openssl ecparam -genkey -name "$ECC_NAME" -out "$PKI_ROOT".key >> "$LOG_FILE" 2>&1
    openssl genpkey -algorithm "EC" -pkeyopt ec_paramgen_curve:P-521 -aes256 --pass "pass:$PASS" -out "$PKI_ROOT".key 
    
    # Create a self signed CA certificate
    openssl req -new -config ca.conf -x509 -days 3650 -key "$PKI_ROOT".key -subj "$ROOT_DN" \
          -extensions ca_extensions -out "$PKI_ROOT".pem \
          -passin pass:"$PASS" >> "$LOG_FILE" 2>&1
    # Add the CA root cert to the Trust and Key stores
    add_to_stores $PKI_ROOT
    # Create an intermediate CA, 2 Leaf CAs, and Signer Certs 
   create_cert_chain
fi