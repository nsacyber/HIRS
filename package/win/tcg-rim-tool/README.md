This README is part of the ZIP_Files.zip generated from hirs_package_linux.yml the goal of this zip is to have the TCG RIM tool, and the TCG Eventlog tool available on windows. 

To get the tools running on windows follow the instructions below:  

1- Unzip "ZIP_Files" 

2- open powershell as administrator and navigate to the hirstools folder. 

3- run the following command: ```powershell -ExecutionPolicy Bypass -File '.\create_hirstools_desktop_shortcut.ps1'```

4- double-click the HIRS_tools shortcut on your desktop. 

To run the rim tool try the following commands:

> rim -c base -a .\tcg_rim_tool\Base_Rim_Config.json -l .\tcg_rim_tool\TpmLog.bin -k .\tcg_rim_tool\PC_OEM1_rim_signer_rsa_3k_sha384.key -p .\tcg_rim_tool\PC_OEM1_rim_signer_rsa_3k_sha384.pem -o baseRim.swidtag

> rim -v .\baseRim.swidtag -p .\tcg_rim_tool\PC_OEM1_rim_signer_rsa_3k_sha384.pem -t .\tcg_rim_tool\PC_OEM1_Cert_Chain.pem -l .\tcg_rim_tool\TpmLog.bin



To run the eventlog tool:

elt -f  C:\Windows\Logs\MeasuredBoot\[.log file here] -e

Eventlog files are found here windows:
C:\Windows\Logs\MeasuredBoot

Example Command would be: 
> elt -f  C:\Windows\Logs\MeasuredBoot\000000001-000000001.log -e (file name needs to match on on your system)
