# need to run rpmbuild with either:
# --define 'build6 1' --define 'dist .el6'
# --define 'build7 1' --define 'dist .el7'

# rpm runs scripts with $1 holding the number of currently installed version of the package in question:
# Install the first time:          1
# Upgrade:                         2 or higher (depending on the number of versions installed)
# Remove last version of package:  0
# from RedHat RPM Guide by Eric Foster-Johnston

Name            : HIRS
Version         : %{?VERSION}
Release         : %{?RELEASE}%{?dist}
Source          : %{name}-%{?GIT_HASH}.tar
Group           : System Environment/Base
License         : ASL 2.0
Summary         : HIRS
BuildArch       : noarch
BuildRoot       : %{_tmppath}/%{name}-%{version}-root
BuildRequires   : java-1.8.0-openjdk-devel

%description
Host Integrity at Runtime and Startup (HIRS) parent spec.

%prep
%setup -q -c

%define provisioner_package_name HIRS_Provisioner_TPM_1_2%{?PACKAGE_NAME_ADDENDUM}
%define __jar_repack 0

##########################
# HIRS_Provisioner_TPM_1_2
##########################
%package -n %{provisioner_package_name}
Summary         : Host Integrity at Runtime and Startup (HIRS) Provisioner
Group           : System Environment/Base

%if 0%{?build6}
Requires        : tpm_module, java-1.8.0, wget, util-linux, chkconfig, sed, initscripts, coreutils, dmidecode, paccor, bash%{?RPM_EXTRA_CLIENT_DEPENDENCIES}
%endif

%if 0%{?build7}
Requires        : tpm_module, java-1.8.0, wget, util-linux, chkconfig, sed, systemd, coreutils, dmidecode, paccor, bash%{?RPM_EXTRA_CLIENT_DEPENDENCIES}
%endif

%description -n %{provisioner_package_name}
Host Integrity at Runtime and Startup (HIRS) Provisioner.

%pre -n %{provisioner_package_name}
if [[ $(find /sys/devices -name "tpm0") ]]; then
    echo "TPM detected"
    if [ -f "/usr/lib/systemd/system/tcsd.service" ]; then
        echo "Starting tcsd service"
        systemctl start tcsd
        ret=$?
        if [[ $ret -ne 0 ]]; then
            echo "WARNING: FAILED TO START tcsd SERVICE, PROVISIONING WILL FAIL WITHOUT THIS SERVICE"
        fi
        echo "Adding tcsd (Trousers) to run levels 1,3,5, and 6"
        chkconfig --level 1356 tcsd on
    else
        echo "Starting tcsd service"
        service tcsd start
        ret=$?
        if [[ $ret -ne 0 ]]; then
            echo "WARNING: FAILED TO START tcsd SERVICE, PROVISIONING WILL FAIL WITHOUT THIS SERVICE"
        fi
        echo "Adding tcsd (Trousers) to run levels 1,3,5, and 6"
        chkconfig --level 1356 tcsd on
    fi
    if [ ! -d "/sys/kernel/security/tpm0" ]; then
        echo "Mounting security fs partition"
        sed -i '$a securityfs /sys/kernel/security securityfs rw,nosuid,nodev,noexec,relatime 0 0' /etc/fstab
        mount -a
        if [ -d "/sys/kernel/security/tpm0" ]; then
	    echo "SUCCESS: security fs partition mounted"
        fi
    fi
else
    echo "WARNING: UNABLE TO LOCATE TPM DEVICE, TPM PROVISIONING WILL FAIL"
fi

%post -n %{provisioner_package_name}
# copy default property files into /etc/hirs if not present
mkdir -p /etc/hirs/
cp -n /opt/hirs/default-properties/provisioner/* /etc/hirs/

# copy common scripts into /opt/hirs/scripts/common
cp -f /opt/hirs/scripts/common/provisioner/* /opt/hirs/scripts/common/

echo 'Creating symlink for hirs-provisioner command'
ln -s -f /usr/share/hirs/provisioner/tpm_aca_provision /usr/sbin/tpm_aca_provision
chmod +x /usr/share/hirs/provisioner/tpm_aca_provision
ln -s -f /usr/share/hirs/provisioner/hirs-provisioner.sh /usr/sbin/hirs-provisioner
chmod +x /usr/share/hirs/provisioner/hirs-provisioner.sh
hirs-provisioner -c

%postun -n %{provisioner_package_name}
# don't run these during an upgrade
if [ "$1" = "0" ]; then
    rm -rf /etc/hirs/provisioner
    rm -rf /etc/hirs/certificates
    rm -f /usr/sbin/hirs-provisioner
    rm -rf /usr/share/hirs/provisioner

    rm -rf /var/log/hirs/provisioner

    # if there are no more HIRS packages remaining,
    # remove all HIRS directories
    if [[ -z `rpm -qa "HIRS*" | grep -v HIRS_Provisioner_TPM_1_2` ]]; then
        rm -rf /etc/hirs
        rm -rf /opt/hirs
        rm -rf /usr/share/hirs
        rm -rf /var/log/hirs
    fi
fi

%files -n %{provisioner_package_name}
%license NOTICE
/etc/hirs/provisioner
%attr(664, root, root) /opt/hirs/default-properties/provisioner/logging.properties
%attr(774, root, root) /opt/hirs/scripts/common/provisioner/
/usr/share/hirs/provisioner
%{_mandir}/man1/hirs-provisioner.1.gz

####################
# HIRS_AttestationCA
####################

%package -n HIRS_AttestationCA
Summary         : Host Integrity at Runtime and Startup (HIRS) Attestation Certificate Authority (HIRS AttestationCA)
Group           : System Environment/Base

%if 0%{?build6}
Requires        : mysql-server, openssl, tomcat6, java-1.8.0, rpmdevtools, coreutils, initscripts, chkconfig, sed, grep, iptables
Prefix          : /usr/share/tomcat6
%endif

%if 0%{?build7}
Requires        : mariadb-server, openssl, tomcat, java-1.8.0, rpmdevtools, coreutils, initscripts, chkconfig, sed, grep, firewalld, policycoreutils
Prefix          : /usr/share/tomcat
%endif

%description -n HIRS_AttestationCA
Host Integrity at Runtime and Startup (HIRS) Attestation CA. Installs and creates keys for HIRS Attestation CA to support generating AIKs

%pre -n HIRS_AttestationCA
if [ ! -d $RPM_INSTALL_PREFIX ]; then
    echo "error: Tomcat directory not found. Re-run this rpm installation with --prefix=\"<absolute-tomcat-directory>\""
    exit 1
fi

%post -n HIRS_AttestationCA
# copy default property files into /etc/hirs if not present
mkdir -p /etc/hirs
cp -n /opt/hirs/default-properties/attestationca/* /etc/hirs/

# loop over common scripts and place into /opt/hirs/scripts/common
mkdir -p /opt/hirs/scripts/common/
cp -f /opt/hirs/scripts/common/aca/* /opt/hirs/scripts/common/

# run these only on a fresh install of the package
if [ $1 == 1 ]; then
    # open necessary ports
    sh /opt/hirs/scripts/common/firewall_configure_tomcat.sh

    # Allow Tomcat to use port 3306 to communicate with MySQL
    %if 0%{?build7}
    if [ selinuxenabled ]; then
        semodule -i /opt/hirs/extras/aca/tomcat-mysql-hirs.pp
    fi
    %endif

    # create trust stores, configure tomcat and db
    sh /opt/hirs/scripts/common/ssl_configure.sh server

    # create the database
    sh /opt/hirs/scripts/common/db_create.sh
fi

# modify mysql schema accordingly on upgrade
if [ $1 -gt 1 ]; then
    #update version number on portal banner
    echo %{?DISPLAY_VERSION} | tee '%{prefix}/webapps/HIRS_AttestationCAPortal/WEB-INF/classes/VERSION'

    echo "Upgrading hirs_db schema!"
    if [ %{version} == "1.0.4" ]; then
	if (mysql -u root hirs_db < /opt/hirs/scripts/common/upgrade_schema_1.0.4.sql); then
		echo "Upgrade to version 1.0.4"
	else
		echo "Error upgrading HIRS database schema to 1.0.4!"
		exit 1;
	fi
    elif [ %{version} == "1.1.0" ]; then
	if (mysql -u root hirs_db < /opt/hirs/scripts/common/upgrade_schema_1.0.4.sql && \
	    mysql -u root hirs_db < /opt/hirs/scripts/common/upgrade_schema_1.1.0.sql); then
		echo "Upgrade to version 1.1.0"
	else
		echo "Error upgrading HIRS database schema to 1.1.0!"
		exit 1;
	fi
    elif [ %{version} == "1.1.1" ]; then
	if (mysql -u root hirs_db < /opt/hirs/scripts/common/upgrade_schema_1.0.4.sql && \
	    mysql -u root hirs_db < /opt/hirs/scripts/common/upgrade_schema_1.1.0.sql && \
	    mysql -u root hirs_db < /opt/hirs/scripts/common/upgrade_schema_1.1.1.sql); then
		echo "Upgrade to version 1.1.1"
	else
		echo "Error upgrading HIRS database schema to 1.1.1!"
		exit 1;
	fi
    fi
fi

sh /opt/hirs/scripts/aca/certificate_generate.sh

%preun -n HIRS_AttestationCA
# don't run these during an upgrade
if [ $1 == 0 ]; then
    # if the Server isn't installed, deconfigure Tomcat and MySQL SSL and drop the database
    if [[ -z `rpm -qa HIRS_Server` ]]; then
        echo 'Restoring Tomcat and MySQL configuration'
        sh /opt/hirs/scripts/common/ssl_deconfigure.sh server

        echo 'Dropping local HIRS database'
        sh /opt/hirs/scripts/common/db_drop.sh
    fi
fi

%postun -n HIRS_AttestationCA
# don't run these during an upgrade
if [ $1 == 0 ]; then
    # Removes WARS from the Tomcat installation as well as ACA configuration files and certificates
    # (/etc/hirs/aca), and ACA installation (/opt/hirs/attestation-ca). Do not run during an upgrade
    rm -f %{prefix}/webapps/HIRS_AttestationCA*.war
    rm -rf %{prefix}/webapps/HIRS_AttestationCA*
    rm -rf /etc/hirs/aca
    rm -rf /opt/hirs/attestation-ca

    # if the Server and Appraiser are not installed, remove certificates directory
    if [[ -z `rpm -qa "HIRS_(Server|Appraiser)"` ]]; then
        rm -rf /etc/hirs/certificates
    fi

    # if there are no more HIRS packages remaining,
    # remove all HIRS directories
    if [[ -z `rpm -qa "HIRS*" | grep -v HIRS_AttestationCA` ]]; then
        rm -rf /etc/hirs
        rm -rf /opt/hirs
        rm -rf /usr/share/hirs
        rm -rf /var/log/hirs
    fi
fi

%files -n HIRS_AttestationCA
%license NOTICE
%attr(664, root, tomcat) %{prefix}/webapps/HIRS_AttestationCA.war
%attr(664, root, tomcat) %{prefix}/webapps/HIRS_AttestationCAPortal.war
%attr(774, root, tomcat) /etc/hirs/aca/
%attr(664, root, tomcat) /opt/hirs/default-properties/attestationca/logging.properties
%attr(664, root, tomcat) /opt/hirs/default-properties/attestationca/banner.properties
%attr(664, root, tomcat) /opt/hirs/default-properties/attestationca/persistence.properties
%attr(664, root, tomcat) /opt/hirs/default-properties/component-class.json
%attr(664, root, tomcat) /opt/hirs/default-properties/vendor-table.json
%attr(774, root, tomcat) /opt/hirs/scripts/common/aca
%attr(774, root, tomcat) /opt/hirs/scripts/aca
%attr(774, root, tomcat) /opt/hirs/extras/aca/tomcat-mysql-hirs.pp
%attr(774, root, tomcat) /opt/hirs/extras/aca/tomcat-mysql-hirs.te

####################
# Build and install
####################

%build
./gradlew -PpluginDir=%{?PLUGIN_SOURCE} -PdisplayVersion=%{?DISPLAY_VERSION} :HIRS_Provisioner:installDist :HIRS_AttestationCA:war :HIRS_AttestationCAPortal:war

%install
# prepare provisioner for packaging
cd HIRS_Provisioner
mkdir -p %{buildroot}/usr/share/hirs/provisioner
mkdir -p %{buildroot}/%{_mandir}/man1
cp -r build/install/HIRS_Provisioner/* %{buildroot}/usr/share/hirs/provisioner

sed -i '/exec "$JAVACMD" "$@"/i /opt/hirs/scripts/common/jvm_version_check.sh $JAVACMD' %{buildroot}/usr/share/hirs/provisioner/bin/HIRS_Provisioner

mkdir -p %{buildroot}/etc/hirs/provisioner/certs
cp scripts/install/hirs-provisioner.sh %{buildroot}/usr/share/hirs/provisioner/
cp scripts/install/tpm_aca_provision %{buildroot}/usr/share/hirs/provisioner/
cp hirs-provisioner-config.sh %{buildroot}/etc/hirs/provisioner
cp create-ek-cert.sh %{buildroot}/etc/hirs/provisioner
cp src/main/resources/defaults.properties %{buildroot}/etc/hirs/provisioner/provisioner.properties
cp -r setup %{buildroot}/etc/hirs/provisioner/
gzip -c man/hirs-provisioner.1 > %{buildroot}/%{_mandir}/man1/hirs-provisioner.1.gz

mkdir -p %{buildroot}/opt/hirs/scripts/common/provisioner
cp ../scripts/common/jvm_version_check.sh %{buildroot}/opt/hirs/scripts/common/provisioner/

# copy common scripts
mkdir -p %{buildroot}/opt/hirs/scripts/common/aca
cp ../scripts/common/* %{buildroot}/opt/hirs/scripts/common/aca/

# prepare ACA for packaging
cd ../HIRS_AttestationCA
mkdir -p %{buildroot}/opt/hirs/scripts/aca
cp ../scripts/aca/* %{buildroot}/opt/hirs/scripts/aca
mkdir -p %{buildroot}/opt/hirs/attestation-ca/
mkdir -p %{buildroot}/etc/hirs/aca/certificates/
mkdir -p %{buildroot}/etc/hirs/aca/client-files/
mkdir -p %{buildroot}%{prefix}/webapps/
cp build/libs/HIRS_AttestationCA.war %{buildroot}%{prefix}/webapps/
cp src/main/resources/defaults.properties %{buildroot}/etc/hirs/aca/aca.properties

# prepare ACA Portal for packaging
cd ../HIRS_AttestationCAPortal
mkdir -p %{buildroot}%{prefix}/webapps/
cp build/libs/HIRS_AttestationCAPortal.war %{buildroot}%{prefix}/webapps/
# note: no ACA Portal specific resource files to copy yet...

# creates the home directory for activemq user so SELinux doesn't complain
mkdir -p %{buildroot}/srv/activemq
mkdir -p %{buildroot}/etc/hirs/portal

cd ..

# copy over the properties files
mkdir -p %{buildroot}/opt/hirs/default-properties/provisioner
cp HIRS_Utils/src/main/resources/logging.properties %{buildroot}/opt/hirs/default-properties/provisioner/logging.properties

mkdir -p %{buildroot}/opt/hirs/default-properties/attestationca
cp HIRS_Utils/src/main/resources/persistence.properties %{buildroot}/opt/hirs/default-properties/attestationca/
cp HIRS_Utils/src/main/resources/logging.properties %{buildroot}/opt/hirs/default-properties/attestationca/
cp HIRS_Utils/src/main/resources/banner.properties %{buildroot}/opt/hirs/default-properties/attestationca/
cp HIRS_Utils/src/main/resources/component-class.json %{buildroot}/opt/hirs/default-properties/
cp HIRS_Utils/src/main/resources/vendor-table.json %{buildroot}/opt/hirs/default-properties/

# install extras
mkdir -p %{buildroot}/opt/hirs/extras
cp -r extras/ %{buildroot}/opt/hirs/
