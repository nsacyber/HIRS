#!/bin/bash

# Assumes HIRS is already installed
# Dependencies:
#  openssl, version 1.0.2 (must download from source)
#  tpm_module

# Download and install latest version of openssl from source, using these steps:
# cd /usr/local/src
# wget https://www.openssl.org/source/openssl-1.0.2-latest.tar.gz
# tar -zxf openssl-1.0.2-latest.tar.gz
# cd openssl-1.0.2*
# ./config
# make depend
# make
# make install
# mv /usr/bin/openssl /root
# ln -s /usr/local/ssl/bin/openssl /usr/bin/openssl

# The TPM only gives us the public key modulus, not a properly formatted key
PUBEK_MOD=$(tpm_module -m 29 -nr -d -t ek -z -d)
rc=$?
if [ "$rc" != "0" ]; then
    echo "Attempt to access the TPM's public Endorsement Key was unsuccessful"
    echo "Exiting"
    exit $rc;
fi

# Check user's permissions
if [ ! -w $(pwd) ] ; then
    echo "This user does not have permissions to write to this directory: $(pwd)"
    echo "Exiting"
    exit 1
fi

mkdir -p certs
cd certs

EKCERT_PATH="$(pwd)/ek.crt"
CAKEY_PATH="$(pwd)/ca.key"
CACERT_PATH="$(pwd)/ca.crt"

# Create the asn.1 definition file used to recreate the public key using the modulus
cat << @EOF > def.asn1
# Start with a SEQUENCE
asn1=SEQUENCE:pubkeyinfo

# pubkeyinfo contains an algorithm identifier and the public key wrapped
# in a BIT STRING
[pubkeyinfo]
algorithm=SEQUENCE:rsa_alg
pubkey=BITWRAP,SEQUENCE:rsapubkey

# algorithm ID for RSA is just an OID and a NULL
[rsa_alg]
algorithm=OID:rsaEncryption
parameter=NULL

# Actual public key: modulus and exponent
[rsapubkey]
n=INTEGER:0x$PUBEK_MOD

e=INTEGER:0x010001
@EOF

# openssl commands return v1 certificates by default
# create a .ext file to specify that we want v3
cat << @EOF > v3.ext
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
@EOF

# Create endorsement key given the asn1 file (DER is the default)
openssl asn1parse -genconf def.asn1 -out ek.der -noout
rm def.asn1

# Convert endorsement key from DER to PEM
openssl rsa -pubin -in ek.der -inform DER -out ek.pem -outform PEM
rm ek.der

# Create self-signed CA certificate
openssl req -newkey rsa:2048 -nodes -keyout $CAKEY_PATH -x509 -days 365 -out $CACERT_PATH \
-subj "/CN=www.example.com"

# openssl needs a private key to create a new certificate signing request
# we only have the public endorsement key, so create a temporary key pair here
openssl req -newkey rsa:2048 -out temp.csr -nodes -keyout temp.key -subj "/CN=www.example.com"
rm temp.key

# Create the certificate here, forcing the use of our endorsement key (this means our temp key isn't used anymore
errormessage=`openssl x509 -req -days 365 -in temp.csr -extfile v3.ext -CAcreateserial -CAkey $CAKEY_PATH -CA $CACERT_PATH -out $EKCERT_PATH -force_pubkey ek.pem 2>&1`
rc=$?
rm v3.ext temp.csr ek.pem
if [ "$rc" != "0" ]; then
    echo $errormessage | grep -q "unknown option -force_pubkey" && echo "ERROR: The option 'force_pubkey' is required, but was not found for this installation of openssl x509. Please install a newer version (1.0.2+) from source" || echo $errormessage
    echo "Cleaning up..."
    rm $CAKEY_PATH $CACERT_PATH
    cd ..
    rmdir certs --ignore-fail-on-non-empty
    exit $rc
fi
rm ca.srl

openssl x509 -text -in $EKCERT_PATH

echo "SUCCESS: EK cert created successfully at: $EKCERT_PATH"
echo "         CA key found at:                 $CAKEY_PATH"
echo "         self-signed CA cert found at:    $CACERT_PATH"
echo
