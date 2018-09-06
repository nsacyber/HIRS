Name            : tpm_module
Vendor          : U.S. Government
Summary         : Tool used to interface with the TPM
Version         : %{?VERSION}
Release         : %{?RELEASE}
Source          : tpm_module-%{?GIT_HASH}.tar

Group           : System Environment/Base
License         : ASL 2.0

Requires        : tpm-tools, trousers
BuildRequires   : cpp
BuildRequires   : gcc-c++
BuildRequires   : trousers-devel

BuildArch       : x86_64
BuildRoot       : %{_tmppath}/%{name}-%{version}-root

%description
Trusted Platform Module (TPM) interface module. This software is designed to provide a platform-independent interface to a client's TPM. It imlpements functionality similar to and exceeding that of tpm-tools in some cases. This software is intended for use with the HIR reporting infrastructure to help clients generate integrity reports based on TPM data.

%prep
%setup -q -n %{name}-%{?GIT_HASH}

%build
rm -f main.d main.o tpm_module
make

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/usr/bin
mkdir -p $RPM_BUILD_ROOT/usr/share/man/man1
cp tpm_module $RPM_BUILD_ROOT/usr/bin/
gzip -c man/tpm_module.1 > $RPM_BUILD_ROOT/usr/share/man/man1/tpm_module.1.gz

%clean
rm -rf $RPM_BUILD_ROOT

%files
%license ../NOTICE
/usr/bin/tpm_module
/usr/share/man/man1/tpm_module.1.gz

%changelog
* Thu Feb 5 2015 3.11
- Modify to support new directory structure
* Mon May 5 2014 3.11
- Set up automatic builds
* Wed Jan 8 2014 3.10
- Added more comments, legal disclaimers, and changed behavior of error output.
* Wed Dec 4 2013 3.09
- Tweaked toggling of TSS_CAP_VERSION_INFO. Added additional debugging messages to describe errors.
* Mon Dec 2 2013 3.08
- TSS_CAP_VERSION_INFO is no longer required on quote2. Other tweaks for platform compatibility.
* Tue Oct 1 2013 3.08
- Improved commenting and documentation, fixed help info, changed behavior of clearing function
* Tue Jul 17 2012 3.07
- Fixed mask utility function handling of 'F' (70, not 80)
* Wed May 2 2012 3.06
- Corrected bug in changekeyauth function
* Tue May 1 2012 3.05
- Initial package release
