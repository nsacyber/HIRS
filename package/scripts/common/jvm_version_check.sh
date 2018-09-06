# only argument to this script is the java command to use in this version check
JAVA_SPEC_VER=`$1 -XshowSettings:properties -version 2>&1 | grep java.specification.version | grep -Eo "[0-9]\.[0-9]+"`
case "$JAVA_SPEC_VER" in
    "1.8"|"1.9"|"1.10"|"1.11"|"1.12" ) ;;
    * ) echo "HIRS needs to be run with a JVM supporting at least specification 1.8.  Found $JAVA_SPEC_VER." && exit 1 ;;
esac
