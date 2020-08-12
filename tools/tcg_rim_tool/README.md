To support the [TCG RIM concept](https://trustedcomputinggroup.org/wp-content/uploads/TCG_RIM_Model_v1-r13_2feb20.pdf) a new command line application alled the The tcg_rim_tool has been created. 
The tcg_rim_tool can be used to create NISTIR 8060 compatible SWID tags that adhere to the [TCG PC Client RIM specification](https://trustedcomputinggroup.org/wp-content/uploads/TCG_PC_Client_RIM_r0p15_15june2020.pdf).
It also supports the ability to digitally sign the Base RIM file as the HIRS ACA will require a valid signature in order to upload any RIM file.

# Building
To build this tool navigate to the tcg_eventlog-tool directory and use the following commmand: 
> ./gradlew clean build

# Packaging

To package the tcg_rim_tool use the [package.sh](https://github.com/nsacyber/HIRS/blob/master/tools/tcg_rim_tool/package.sh) script to produce an RPM file for Linux distrobustions that support thw RPM package manager. The rpm file will be located in the rpmbuild/RPMS/x86_64/ directory if the package script was sucessful.
Although packaging for other distributions is not currently avialble the tool can be built an run on other systems that support java and gradle, such as windows 10.

# Usage

The tcg_rim_tool rpm will create a rim commandline shortcut. This can be invoked from a command line:
> rim -h

The tcg_eventlog_tool also can be invoked using java from the tcg_eventlog_tool directory:

> java -jar build/libs/tools/tcg_rim_tool-1.0.jar -h

Current options for the tool can be found using the -h option.
