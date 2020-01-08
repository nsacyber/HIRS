Name:           tcg_rim_tool
Version:        1.0
Release:        1%{?dist}
Summary:        A java command-line tool to create PC client root RIM

License:        ASL 2.0
URL:            https://github.com/nsacyber/HIRS
Source0:     	%{name}.tar.gz   

BuildRequires:  java-headless >= 1:1.8.0

%description
This tool will generate a root RIM file for PC clients in accordance with the schema located at http://standards.iso.org/iso/19770/-2/2015/schema.xsd.  The generated RIM can either be empty if no arguments are given, or contain a payload if an input file is provided.  The tool can also verify a given RIMfile against the schema. Use -h or --help to see a list of commands and uses.


%prep
%setup -q -c -n %{name}


%build
./gradlew build

%install
mkdir -p %{buildroot}/opt/hirs/rim/
cp build/libs/%{name}-%{version}.jar %{buildroot}/opt/hirs/rim/

%files
/opt/hirs/rim/%{name}-%{version}.jar

%changelog
* Mon Jan 6 2020 chubtub
- First change
