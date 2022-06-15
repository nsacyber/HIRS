# Wait for ACA to boot

HIRS_ACA_PORTAL_IP=localhost
HIRS_ACA_PORTAL_PORT=8443

echo "Waiting for ACA to spin up at address ${HIRS_ACA_PORTAL_IP} on port ${HIRS_ACA_PORTAL_PORT} ..."
until [ "`curl --silent --connect-timeout 1 -I -k https://${HIRS_ACA_PORTAL_IP}:${HIRS_ACA_PORTAL_PORT}/HIRS_AttestationCAPortal | grep '302 Found'`" != "" ]; do
  sleep 5;
done
echo "ACA is up!"