Name:           ACA_SwidTag
Version:        1.0
Release:        1%{?dist}
Summary:        A java command-line tool to create swidtags

License:        ASL 2.0
URL:            https://github.com/nsacyber/HIRS
Source0:     	%{name}.tar.gz   

BuildRequires:  java-headless >= 1:1.8.0

%description
This tool will generate a valid swidtag file in accordance with the schema located at http://standards.iso.org/iso/19770/-2/2015/schema.xsd.  The generated swidtag can either be empty if no arguments are given, or contain a payload if an input file is specified.  The tool can also verify a given swidtag file against the schema. Use -h or --help to see a list of commands and uses.


%prep
%setup -q -c -n %{name}


%build
./gradlew build

%install
mkdir -p %{buildroot}/opt/hirs/swidtag/
cp build/libs/%{name}-%{version}.jar %{buildroot}/opt/hirs/swidtag/

%files
/opt/hirs/swidtag/%{name}-%{version}.jar

%changelog
* Mon Dec 23 2019 chubtub
- First change
