#!/bin/bash
# Best-effort diagnostics, including when setup failed before logs were created.
# docker cp also works after a container stops and avoids writing via a bind mount.
echo "*** Extracting ACA, Provisioner.NET and TPM simulator logs ..."
mkdir -p logs/aca logs/provisioner logs/tpm || exit 1
docker cp hirs-aca1:/var/log/hirs/. logs/aca/ || true
docker cp hirs-aca1:/tmp/hirs-aca-startup.log logs/aca/ || true
docker exec hirs-provisioner1-tpm2 bash -c '
  mkdir -p /tmp/hirs-provisioner-logs
  cp -p /hirs/hirs*.log /tmp/hirs-provisioner-logs/
' || true
docker cp hirs-provisioner1-tpm2:/tmp/hirs-provisioner-logs/. logs/provisioner/ || true
simulator="${HIRS_CI_TPM_SIM:-ibmswtpm2}"
state_dir="${HIRS_CI_TPM_SIM_STATE_DIR:-/tmp/hirs-tpm}"
docker cp "hirs-provisioner1-tpm2:$state_dir/$simulator/server.log" "logs/tpm/$simulator.log" || true
