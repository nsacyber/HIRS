#!/bin/bash

# Imported /etc/hirs should only be used if one doesn't exist
if [ ! -d "/etc/hirs" ]; then
    if [ -n "${HIRS_USE_IMPORTED_ETC_HIRS}" ]; then
        IMPORTED_ETC_HIRS_PATH="${HIRS_USE_IMPORTED_ETC_HIRS}"

        if [ -d "$IMPORTED_ETC_HIRS_PATH" ]; then
            cp -r "$IMPORTED_ETC_HIRS_PATH" /etc/hirs

	    find /etc/hirs -type d -exec chown root:root {} +
	    find /etc/hirs/certificates -type d -exec chown :mysql {} +
	    find /etc/hirs -type d -exec chmod -R g+rx {} +
	    find /etc/hirs -type f -exec chmod -R 644 {} +
	    chmod 755 /etc/hirs
        fi
    fi
fi 
