#!bin/bash

# outline:
# 1. Run the tcg_rim_tool to check the validity of the rim using cmd line
# 2. Run the event_log_tool diff cmdline
# 3. Output results

function eventcheck_help() {
  echo "Event Check: Checks a TCG defined Event Log agianst a Integrity Reference Manifest for a Linux Device with a TPM 2.0" 
  echo "usage: eventcheck -r [file] - p [file] -s [file] -l [file]";
  echo "Options"
  echo "-r --rim <path> : Reference Integrity Manifest (RIM) <path> Reference Integrity Manifest (RIM) Base RIM file holding OEM product information.";
  echo "-p --publicCertificate <path> : Public key certificate path used to validate the rim file."; 
  echo "-s --supportRim <path> : PC Client defined support RIM file holding the reference data provided by the OEM of the product.";
  echo "-l --log <path> : Event Log of the device being tested. Will default to latest event log if parameter is not supplied."; 
  echo "-h --help : help listing";
}

 while [[ "$#" -gt 0 ]]; do
    case $1 in
        -p|--publicCertificate) oem_cert="$2"; shift ;;
        -r|--rim) oem_rim=$2; shift ;;
        -s|--supportRim) support_rim=$2; shift ;;
        -l|--log) event_log=$2; shift ;;
        -h|--help) eventcheck_help; exit 0 ;;
        *) echo "Unknown parameter passed: $1"; eventcheck_help; exit 1 ;;
    esac
    shift
done
# Check for required parameters 
if ${oem_rim+"false"}; then
   echo "Error: Base RIM file needs to be specified using the -r parameter";
   echo "Exiting without processing.";
   exit 1;
fi

if ${support_rim+"false"}; then
   echo "Error: Support RIM file needs to be specified using the -s parameter";
   echo "Exiting without processing.";
   exit 1;
fi

if ${oem_cert+"false"}; then
   echo "Error: OEM Public Key Certificate Chain file needs to be specified using the -p parameter";
   echo "Exiting without processing.";
   exit 1;
fi
# If event log not specified, then use the local devices log (if present)
if ${event_log+"false"}; then
   ech0 "Event log not specified attempting to use local devices event log...";
   event_log="/sys/kernel/security/tpm0/binary_bios_measurements";
   if [ ! -f  $event_log ]; then
       kver=$(uname -r); 
       echo "Error opening default event log file, sudo may be required.";
       echo "  Note kernel version must be greater than 4.18 to produce an Event log. Current verion is $kver.";
       echo "Exiting without processing.";
       exit 1;
   fi
fi

echo "OEM Certificate Chain = $oem_cert";
echo "Base RIM = $oem_rim";
echo "Support RIM = $support_rim";
echo "eventlog = $event_log";

echo "Checking the RIM signature and OEM Certificate Chain";

java -jar ../tcg_rim_tool/build/libs/tools/tcg_rim_tool-1.0.jar -v $oem_rim -p $oem_cert 

if [ $? -ne 0 ]; then 
   exit 1;
fi

echo "Comparing RIM against the specified Event Log";

java -jar ../tcg_eventlog_tool/build/libs/tools/tcg_eventlog_tool-1.0.jar -d $support_rim $event_log

echo " ";
echo "Event Check against RIM complete"
