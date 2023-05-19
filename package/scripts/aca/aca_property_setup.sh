#!/bin/bash

# Create aca.poperties file

pki_password=$1

rm  -f /etc/hirs/aca.properties 
aca_prop_file="/etc/hirs/aca.properties"

echo  '# *** ACA Directories ***
aca.directories.root         = /etc/hirs/
aca.directories.certificates = ${aca.directories.root}/certificates' > $aca_prop_file

echo  '# *** Certificate and Key Properties ***
aca.setup.keys.rsa.keySize         =  3072
aca.setup.keys.ecc.keySize         =  512
aca.setup.certificates.validity    =  3652
aca.setup.certificates.subjectName =  HIRS_AttestationCA
aca.setup.certificates.expiration  =  ${aca.setup.certificates.validity}' >>  $aca_prop_file

echo  '# *** Keystore properties ***
aca.keyStore.alias     =  HIRS_ACA_KEY
aca.keyStore.rsa.alias =  hirs_leaf_ca1_rsa_3072_sha384
aca.keyStore.ecc.alias =  hirs_leaf_ca1_ecc_512_sha384 
aca.keyStore.location  =  ${aca.directories.certificates}/keyStore.jks
aca.keyStore.password  =  '$pki_password >> $aca_prop_file

