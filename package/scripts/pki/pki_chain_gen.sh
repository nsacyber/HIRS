!/bin/bash
# Script to generate a PKI Stack (Root, Intermediate, and LEAF CAs) and a Base RIM Signer
# creates a folder based upon the actor name and places certs under an algoithm specific folder (e.g. rsa_certs)
# PARAMS: 
# 1. Actor name string (e.g. "Server Manufacturer") 
# 2. Algorithm string (e.g. rsa or ecc)
# 3. Key Bit Size string (e.g. 2048)
# 4. Hash Algorithm string (e.g. sha256)
#
# Examples:
#    make_pki.sh "PC Manufacturer" rsa 2048 sha256 
#    make_pki.sh "DISK Manufacturer" ecc 256 sha512
#

ACTOR=$1
ACTOR_ALT=${ACTOR// /_}
ASYM_ALG=$2
ASYM_SIZE=$3
HASH_ALG=$4
ROOT_DN="/C=US/ST=MD/L=Bethseda/O="$ACTOR"/CN="$ACTOR" test root ca"
INT_DN="/C=US/ST=MD/L=Bethseda/O="$ACTOR"/CN="$ACTOR" test intermediate ca"
LEAF_DN="/C=US/ST=MD/L=Bethseda/O="$ACTOR"/CN="$ACTOR" test ca"
SIGNER_DN="/C=US/ST=MD/L=Bethseda/O="$ACTOR"/CN="$ACTOR" test signer"
SERVER_DN="/C=US/ST=MD/L=Bethseda/O="$ACTOR"/CN="$ACTOR" aca"
PASS="xrb204k"

print_help () {
 echo "
make_pki.sh - creates a pki certificate chain

Usage:
  make_pki.sh \"Actor Name\" \"Asymmetric Algorithm\" \"Key Size\" \"Hash Algorithm\"

Required Parameters:
  Actor Name              Device Manufacturer
  Asymmetric Algorithm    rsa or ecc
  Key Size (in bits)      (rsa) 2048, 3072, or 4096
                          (ecc) 256, 384, or 512
  Hash Algorithm          sha256, sha384, or sha512
 "
exit 1;
}

# Parameter check 
if [ -z "${ACTOR}" ] || [ -z "${ASYM_ALG}" ] || [ -z "${ASYM_SIZE}" ] || [ -z "${HASH_ALG}" ] || [ "${ACTOR}" == "-h" ] || [ "${ACTOR}" == "--help" ]; then
   print_help
   exit 1;
fi

if ! { [ $ASYM_ALG == "rsa" ] || [ $ASYM_ALG == "ecc" ]; }; then
       echo "$ASYM_ALG is an unsupported assymetric algorithm"
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
       echo "$ASYM_SIZE is an unsupported key size"
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
TRUST_STORE_FILE="$CERT_FOLDER"/"$ACTOR_ALT"_Cert_Chain.pem

ROOT_DN="/C=US/ST=OR/L=Beaverton/O="$ACTOR"/CN="$NAME" test root ca"
INT_DN="/C=US/ST=OR/L=Beaverton/O="$ACTOR"/CN="$NAME" test intermediate ca"
LEAF_DN="/C=US/ST=OR/L=Beaverton/O="$ACTOR"/CN="$NAME" test ca"
SIGNER_DN="/C=US/ST=OR/L=Beaverton/O="$ACTOR"/CN="$NAME" test signer"
TLS_DN="/C=US/ST=OR/L=Beaverton/O="$ACTOR"/CN="$NAME" portal"

# Add check for existing folder and halt if it exists
if [ -d "$ACTOR_ALT"/"$CERT_FOLDER" ]; then
   echo "Folder for $CERT_FOLDER exists, exiting..."
   exit 1;
fi

# Intialize sub folders
echo "Creating PKI for $ACTOR_ALT using $KSIZE $ASYM_ALG and $HASH_ALG..."

mkdir -p "$ACTOR_ALT" "$ACTOR_ALT"/"$CERT_FOLDER" "$ACTOR_ALT"/ca/certs
cp ca.conf "$ACTOR_ALT"/.
pushd "$ACTOR_ALT"
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
   ISSUER_KEY="$ISSUER".key
   ISSUER_CERT="$ISSUER".pem

   if [ "$CERT_TYPE" == "rim_signer" ]; then
      EXTENSION="signer_extensions"
   else
      EXTENSION="ca_extensions"
   fi

   echo "Creating cert for "$CERT_TYPE" using $ISSUER_KEY with a DN="$SUBJ_DN"..."

   if [ "$ASYM_ALG" == "rsa" ]; then 
       openssl req -newkey rsa:"$ASYM_SIZE" \
            -keyout "$CERT_PATH".key \
            -out "$CERT_PATH".csr  -subj "$SUBJ_DN" \
            -passout pass:"$PASS"
   else
       openssl ecparam -genkey -name "$ECC_NAME" -out "$CERT_PATH".key
       openssl req -new -key "$CERT_PATH".key -out "$CERT_PATH".csr -$HASH_ALG  -subj "$SUBJ_DN"    
   fi
   openssl ca -config ca.conf \
           -keyfile "$ISSUER_KEY" \
           -md $HASH_ALG \
           -cert "$ISSUER_CERT" \
           -extensions "$EXTENSION" \
           -out "$CERT_PATH".pem \
           -in "$CERT_PATH".csr \
           -passin pass:"$PASS" \
           -batch
   #increment the cert serial number
   awk -F',' '{printf("%s\t%d\n",$1,$2+1)}' ./ca/serial.txt
   # remove csr file
   rm "$CERT_PATH".csr
}

create_cert_chain () {

   # Create an intermediate CA, Sign with Root CA
   create_cert "$PKI_INT" "$PKI_ROOT" "$INT_DN"

   # Create a Leaf CA (CA1), Sign with intermediate CA
   create_cert "$PKI_CA1" "$PKI_INT" "$LEAF_DN"1

   # Create a Leaf CA (CA2), Sign with intermediate CA

   create_cert "$PKI_CA2" "$PKI_INT" "$LEAF_DN"2

   # Create a Leaf CA (CA3), Sign with intermediate CA

   create_cert "$PKI_CA3" "$PKI_INT" "$LEAF_DN"3

   # Create a RIM Signer
   create_cert "$RIM_SIGNER" "$PKI_CA2" "$SIGNER_DN"

   # Create a ACA Sever Cert for TLS use
   create_cert "$TLS_SERVER" "$PKI_CA3" "$TLS_DN"

   # Create Cert trust store by adding the Intermediate and root certs 
   cat "$PKI_CA1.pem" "$PKI_CA2.pem"  "$PKI_INT.pem" "$PKI_ROOT.pem" >  "$TRUST_STORE_FILE"

   echo "Checking signer cert using tust store..." 
   openssl verify -CAfile "$TRUST_STORE_FILE" $RIM_SIGNER.pem
}

if [ "$ASYM_ALG" == "rsa" ]; then 
   # Create Root CA key pair and self signed cert
    openssl genrsa -out "$PKI_ROOT".key -passout pass:"$PASS" "$ASYM_SIZE"

   # Create a self signed CA certificate
   openssl req -new -config ca.conf -x509 -days 3650 -key "$PKI_ROOT".key -subj "$ROOT_DN" \
          -extensions ca_extensions -out "$PKI_ROOT".pem \
          -passout pass:"$PASS"
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

