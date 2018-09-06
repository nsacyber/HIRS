#!/usr/bin/env bash

set -e

if [ -f /sbin/dracut ]; then
    DRACUT='/sbin/dracut'
elif [ -f /usr/bin/dracut ]; then
    DRACUT='/usr/bin/dracut'
else
    echo "dracut not found on system"
    exit 1
fi

# executs dracut to create an initramfs image given a kernel version and the kernel module dir
$DRACUT --kmoddir $KMODDIR $TARGET_FILEPATH $COMPLETE_VERSION

find $WORKING_DIR ! -name $TARGET_FILEPATH # -exec rm -rf {} \;

CENTOS_REL=`cat /etc/redhat-release`
if [[ "$CENTOS_REL" =~ "release 6" ]]; then
    gzip -dc $TARGET_FILEPATH | cpio -id
elif [[ "$CENTOS_REL" =~ "release 7" ]]; then
    /usr/lib/dracut/skipcpio $TARGET_FILEPATH | zcat | cpio -id --no-absolute-filenames
else
    echo "Unsupported version of CentOS: $CENTOS_REL"
    exit 1
fi
