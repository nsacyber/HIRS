#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

#TODO Remove when finished testing
#export HIRS_ACA_PORTAL_IP="rd8ul-31673ab.dod.mil"
#export HIRS_ACA_PORTAL_PORT="8443"

export CLIENT_OS=centos7
export CLIENT_HOSTNAME=hirs-client-$CLIENT_OS-tpm2

export SERVER_OS=$CLIENT_OS
export SERVER_HOSTNAME=hirs-appraiser-$SERVER_OS

export ENABLED_COLLECTORS=BASE_DELTA
export TPM_VERSION=2.0

$SCRIPT_DIR/systems-test.core.sh
