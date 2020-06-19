Name:           tcg_rim_tool
Version:        1.0
Release:        1%{?dist}
Summary:        A java command-line tool to create PC client root RIM

License:        ASL 2.0
URL:            https://github.com/nsacyber/HIRS
Source0:     	%{name}.tar.gz   

BuildRequires:  java-headless >= 1:1.8.0

%description
This tool will generate a base RIM file for PC clients in accordance with the schema located at http://standards.iso.org/iso/19770/-2/2015/schema.xsd.  The generated RIM can either be empty if no arguments are given, or contain a payload if an input file is provided.  The tool can also verify a given RIMfile against the schema. Use -h or --help to see a list of commands and uses.

%prep
%setup -q -c -n %{name}

%pre
rm -f /opt/hirs/rimtool/%{name}*.jar

%build
./gradlew build

%install
mkdir -p %{buildroot}/opt/hirs/rimtool/ %{buildroot}/usr/local/bin
cp build/libs/%{name}-%{version}.jar %{buildroot}/opt/hirs/rimtool/
cp ./rim_fields.json %{buildroot}/opt/hirs/rimtool/
cp ./keystore.jks %{buildroot}/opt/hirs/rimtool/
cp -r ./scripts/ %{buildroot}/opt/hirs/rimtool/
ln -sf /opt/hirs/rimtool/scripts/rimtool.sh %{buildroot}/usr/local/bin/rim

%files
/opt/hirs/rimtool/%{name}-%{version}.jar
/opt/hirs/rimtool/rim_fields.json
/opt/hirs/rimtool/keystore.jks
/opt/hirs/rimtool/scripts
/usr/local/bin/rim

%attr(755, root, root) /opt/hirs/rimtool/scripts/rimtool.sh

%changelog
* Mon Jun 15 2020 chubtub
- First release
* Mon Jan 6 2020 chubtub
- First change
