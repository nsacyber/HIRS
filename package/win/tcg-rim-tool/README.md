This README is part of the ZIP_Files.zip generated from hirs_package_linux.yml the goal of this zip is to have the TCG RIM tool, and the TCG Eventlog tool available on windows. 

To get the tools running on windows follow the instructions below:   

1- Unzip the main folder ("ZIP_Files"), followed by unzipping both the tcg_eventlog_tool.zip and tcg_rim_tool.zip in the working directory. 

2- Open PowerShell as administrator and run > Set-ExecutionPolicy unrestricted
To verify run > Get-ExecutionPolicy and it should be set to "unrestricted"

3- Right click on create_hirs_desktop_shortcut.ps1 and run with PowerShell, a PowerShell terminal will pop-up, if prompted type "R" to run the script.

4- HIRS_tools.ps1 should appear on your desktop, right click on it and run with PowerShell . if prompted type "R" to run the script.

5- The HIRS terminal should popup.

6- (optional) To remove the warning messages when running the rim tool and the event log tool:
Open PowerShell as administrator, navigate to the working directory, and run the following:
> Unblock-File -Path .\rim.ps1  
> Unblock-File -Path .\eventLog.ps1

To run the rim tool try the following commands

> rim -c base -a .\tcg_rim_tool\Base_Rim_Config.json -l .\tcg_rim_tool\TpmLog.bin -k .\tcg_rim_tool\PC_OEM1_rim_signer_rsa_3k_sha384.key -p .\tcg_rim_tool\PC_OEM1_rim_signer_rsa_3k_sha384.pem -o baseRim.swidtag

> rim -v .\baseRim.swidtag -p .\tcg_rim_tool\PC_OEM1_rim_signer_rsa_3k_sha384.pem -t .\tcg_rim_tool\PC_OEM1_Cert_Chain.pem -l .\tcg_rim_tool\TpmLog.bin



To run the eventlog tool:

elt -f  C:\Windows\Logs\MeasuredBoot\[.log file here] -e

Eventlog files are found here windows:
C:\Windows\Logs\MeasuredBoot

Example Command would be: 
> elt -f  C:\Windows\Logs\MeasuredBoot\000000001-000000001.log -e (file name needs to match on on your system)
