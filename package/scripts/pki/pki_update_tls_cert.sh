#!/bin/bash

CN=$1
PASS=$2
ACTOR="HIRS"
ACTOR_ALT=${ACTOR// /_}
ASYM_ALG="rsa"
ASYM_SIZE=3072
KSIZE="3k"
HASH_ALG="sha384"
CERT_FOLDER="/etc/hirs/certificates/HIRS/$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"_certs
#CERT_FOLDER="."
EXTENSION="server_extensions"
TRUSTSTORE="/etc/hirs/certificates/HIRS/TrustStore.jks"

echo "CERT_FOLDER is $CERT_FOLDER"


if [ -z "${CN}" ] || [ -z "${PASS}" ] || [ "${CN}" == "-h" ] || [ "${CN}" == "--help" ]; then
   echo "parameter missing to pki_tls_update.sh, exiting"
   exit 1;
fi

TLS_DN="/C=US/ST=MD/L=Columbia/O="$ACTOR"/CN=$CN"

TLS_SERVER="$CERT_FOLDER"/"$ACTOR_ALT"_aca_tls_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"
PKI_CA3="$CERT_FOLDER"/"$ACTOR_ALT"_leaf_ca3_"$ASYM_ALG"_"$KSIZE"_"$HASH_ALG"

echo "TLS_SERVER is $TLS_SERVER"
create_cert () {
   CERT_PATH="$1"
   ISSUER="$2"
   SUBJ_DN="$3"
   ISSUER_KEY="$ISSUER".key
   ISSUER_CERT="$ISSUER".pem
   ALIAS=${CERT_PATH#*/}    # Use filename without path as an alias

   pushd /etc/hirs/certificates/HIRS

#   if [ "$CERT_TYPE" == "rim_signer" ]; then
#      EXTENSION="signer_extensions"
#   else
#      EXTENSION="ca_extensions"
#   fi

   echo "Updating cert for "$CERT_PATH".pem using $ISSUER_KEY with a DN="$SUBJ_DN" using $EXTENSION."

  if [ "$ASYM_ALG" == "rsa" ]; then
       openssl req -newkey rsa:"$ASYM_SIZE" \
            -keyout "$CERT_PATH".key \
            -out "$CERT_PATH".csr  -subj "$SUBJ_DN" \
            -passout pass:"$PASS" 
#&> /dev/null
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
           -notext                          
    popd

#&> /dev/null
   # Increment the cert serial number
   awk -F',' '{printf("%s\t%d\n",$1,$2+1)}' ./ca/serial.txt &> /dev/null
   # remove csr file
   rm -f "$CERT_PATH".csr
   # remove all cert from TrustStore.jks
   keytool -delete -noprompt -alias hirs_aca_tls_rsa_3k_sha384 -keystore $TRUSTSTORE -storepass $PASS
   # insert new cert into TrustStore.jks with same alias 
   keytool -import -file ""$CERT_PATH".pem" -alias hirs_aca_tls_rsa_3k_sha384 -keystore $TRUSTSTORE -storepass $PASS
}

create_cert "$TLS_SERVER" "$PKI_CA3" "$TLS_DN"
