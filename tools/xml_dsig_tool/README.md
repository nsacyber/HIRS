The xml_dsig_tool is a Windows command line application that provides the ability to perform basic cryptographic functions per the W3C XML Signature Syntax and Processing Version 1.1. The functions include:

sign : append an enveloped signature to an unsigned XML document
validate : validate a signed base rim's signature (NOTE: cryptographic validation only, this tool does not validate the RIM structure)


# Build and package
 - Install Visual Studio
  - The recommended project name is "xml_dsig_tool" so that the resulting executable file will be appropriately named xml_dsig_tool.exe.
 - Install NuGet packages:
   - System.CommandLine.2.0.0-beta4 (check "Include Prerelease" next to search bar)
   - System.Security.Cryptography.X509Certificates
   - System.Security.Cryptography.Xml
 - Publish executable
   - https://docs.microsoft.com/en-us/dotnet/core/tutorials/publishing-with-visual-studio?pivots=dotnet-6-0
 - Install support files to .exe directory
   - privateRimKey.pem
   - RimSignCert.pem
   - unsigned.xml


# Running xml_dsig_tool
Navigate to the .exe directory and run the following commands

help

sign --file unsigned.xml --private-key privateKey.pem

validate --file signed_unsigned.xml --certificate RimSignCert.pem

