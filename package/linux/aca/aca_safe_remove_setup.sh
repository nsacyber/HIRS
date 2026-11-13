#!/bin/bash

if rpm -qa --qf '%{NAME}-%{VERSION}-%{RELEASE}.%{ARCH}\n' | grep -qE '^HIRS_AttestationCA-.*\.x86_64$'; then
  echo "HIRS_AttestationCA RPM is already installed, aborting install."
  exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$SCRIPT_DIR/aca_remove_setup.sh" "$@"
