To support the TCG RIM concept a new command line application alled the The tcg_rim_tool has been created. 
The tcg_ri_tool can be used to create NISTIR 8060 compatible SWID tags that adhere to the TCG PC Client RIM specification.
It also supports the ability to digitally sign the Base RIM file as the HIRS ACA will require a valid signature in order to
upload any RIM file.

# Building
To build this tool navigate to the tcg_eventlog-tool directory and use the following commmand: 
> ./gradlew clean build

# Usage
The tcg_eventlog_tool can be invoked using java:

java -jar build/lib.tools/tcg_rim_tool-1.0.jar -h

Current options for the tool can be found using the -h option.

Future packages will install a command line: rim.
