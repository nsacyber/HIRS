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
ROOT_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$ACTOR" test root ca"
INT_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$ACTOR" test intermediate ca"
LEAF_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$ACTOR" test ca"
SIGNER_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$ACTOR" test signer"
SERVER_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$ACTOR" aca"
# Capture location of the script to allow from invocation from any location
SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )
TRUSTSTORE=TrustStore.jks
KEYSTORE=KeyStore.jks

# Parameter check 
if [ -z "${ACTOR}" ] || [ -z "${ASYM_ALG}" ] || [ -z "${ASYM_SIZE}" ] || [ -z "${HASH_ALG}" ] || [ "${ACTOR}" == "-h" ] || [ "${ACTOR}" == "--help" ]; then
   echo "parameter missing to pki_chain_gen.sh, exiting pki setup"
   exit 1;
fi

if ! { [ $ASYM_ALG == "rsa" ] || [ $ASYM_ALG == "ecc" ]; }; then
       echo "$ASYM_ALG is an unsupported assymetric algorithm, exiting pki setup"
       exit 1
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
       echo "$ASYM_SIZE is an unsupported key size, exiting pki setup"
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
TRUST_STORE_FILE="$CERT_FOLDER"/"$ACTOR_ALT"_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"_Cert_Chain.pem

ROOT_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" test root ca"
INT_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" test intermediate ca"
LEAF_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" test ca"
SIGNER_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN="$NAME" test signer"
TLS_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN=localhost"

# Add check for existing folder and halt if it exists
if [ -d "$ACTOR_ALT"/"$CERT_FOLDER" ]; then
   echo "Folder for $CERT_FOLDER exists, exiting..."
   exit 1;
fi

# Intialize sub folders
echo "Creating PKI for $ACTOR_ALT using $KSIZE $ASYM_ALG and $HASH_ALG..."

mkdir -p "$ACTOR_ALT" "$ACTOR_ALT"/"$CERT_FOLDER" "$ACTOR_ALT"/ca/certs
cp ca.conf "$ACTOR_ALT"/.
pushd "$ACTOR_ALT" &> /dev/null
touch ca/db
if [ ! -f "ca/serial.txt" ]; then
     echo "01" > ca/serial.txt
fi

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

   echo "Creating cert using "$ISSUER_KEY" with a DN="$SUBJ_DN"..."

   if [ "$ASYM_ALG" == "rsa" ]; then 
       openssl req -newkey rsa:"$ASYM_SIZE" \
            -keyout "$CERT_PATH".key \
            -out "$CERT_PATH".csr  -subj "$SUBJ_DN" \
            -passout pass:"$PASS" &> /dev/null
   else
       openssl ecparam -genkey -name "$ECC_NAME" -out "$CERT_PATH".key &> /dev/null
       openssl req -new -key "$CERT_PATH".key -out "$CERT_PATH".csr -$HASH_ALG  -subj "$SUBJ_DN" &> /dev/null    
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
           -notext                          &> /dev/null
   # Increment the cert serial number
   awk -F',' '{printf("%s\t%d\n",$1,$2+1)}' ./ca/serial.txt &> /dev/null
   # remove csr file
   rm -f "$CERT_PATH".csr
   # Add the cert and key to the key store. make a p12 file to import into te keystore
   openssl pkcs12 -export -in "$CERT_PATH".pem -inkey "$CERT_PATH".key -out tmpkey.p12 -passin pass:"$PASS" -passout pass:$PASS
   # Use the p12 file to import into a java keystore via keytool
   keytool -importkeystore -srckeystore tmpkey.p12 -destkeystore $KEYSTORE -srcstoretype pkcs12 -srcstorepass $PASS -deststoretype jks -deststorepass $PASS -noprompt -alias 1 -destalias "$ALIAS" &> /dev/null
   # Import the cert into a java trust store via keytool
   keytool -import -keystore $TRUSTSTORE -storepass $PASS -file "$CERT_PATH".pem  -noprompt -alias "$ALIAS" &> /dev/null
   # Remove the temp p1 file.
   rm tmpkey.p12
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

   # Create Cert trust store by adding the Intermediate and root certs 
   cat "$PKI_CA1.pem" "$PKI_CA2.pem" "$PKI_CA3.pem" "$PKI_INT.pem" "$PKI_ROOT.pem" >  "$TRUST_STORE_FILE"

 # echo "Checking signer cert using tust store..." 
   openssl verify -CAfile "$TRUST_STORE_FILE" $RIM_SIGNER.pem
}

if [ "$ASYM_ALG" == "rsa" ]; then 
   # Create Root CA key pair and self signed cert
   openssl genrsa -out "$PKI_ROOT".key -passout pass:"$PASS" "$ASYM_SIZE" &> /dev/null

   # Create a self signed CA certificate
   openssl req -new -config ca.conf -x509 -days 3650 -key "$PKI_ROOT".key -subj "$ROOT_DN" \
          -extensions ca_extensions -out "$PKI_ROOT".pem \
          -passout pass:"$PASS"   &> /dev/null
   # Create an intermediate CA, 2 Leaf CAs, and Signer Certs 
   create_cert_chain
fi

if [ "$ASYM_ALG" == "ecc" ]; then
    # Create Root CA key pair and self signed cert
    openssl ecparam -genkey -name "$ECC_NAME" -out "$PKI_ROOT".key

    # Create a self signed CA certificate
    openssl req -new -config ca.conf -x509 -days 3650 -key "$PKI_ROOT".key -subj "$ROOT_DN" \
          -extensions ca_extensions -out "$PKI_ROOT".pem \
          -passout pass:"$PASS"
    # Create an intermediate CA, 2 Leaf CAs, and Signer Certs 
   create_cert_chain
fi

