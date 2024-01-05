#Requires -RunAsAdministrator
# Powershell script to install the HIRS Acceptance Test on Windows
Write-Host "Installing the HIRS Acceptance Test (HAT)"
Write-Host "Checking for prerequisites..."

# Check for connectivity to github
$Github=Test-Connection -ComputerName www.github.com -Quiet 
if ($Github -ne 'True' ) {
	Write-Host "Cannot reach www.github.com, please check internet connection and Firewall settings"
    Write-Host "Exiting without installing HAT. Hit Any key to exit"
    $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
	exit;
} else {
      Write-Host "Github is accessible, continuing installation..."
}

# Check For Docker Services
$Service=Get-Service -Name Docker 
if ($Service.Status -ne 'Running') {
  Write-Host "Docker is either NOT running or NOT installed." 
  Write-Host "Please start or install Docker Desktop. See https://docs.docker.com/desktop/install/windows-install/";
  Write-Host "Exiting without installing HAT. Hit any key to exit"
  $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
  exit;
} else {
      Write-Host "Docker is running, continuing installation..."
}

# Check for previos install 
if (Test-Path -Path hirs) {
  Write-Host "The hirs folder exists under the current directory, aborting install."
  Write-Host "Exiting without installing HAT. Hit Any key to exit"
  $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
  exit
}

# Warn Admin that device needs to be attached for the next step and wait for connection
Write-Host "Please attach an Ethernet cable between this device and a powered target device for the next step. Hit Any Key to Continue"
$Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown") | out-null
Write-Host "Testing connection"
$adapter=Get-NetAdapter Ethernet -Physical
if ($adapter.status -ne "Up") {
  do {
    $adapter=Get-NetAdapter Ethernet -Physical
    Start-Sleep -seconds 10
    Write-Host "Waiting for an Ethernet connection..."
  } until ($adapter.status -eq "Up")
}

# Make Firwall Rules for ACA to operate
Write-Host "Adding Firewall rules"
netsh advfirewall firewall add rule name="ACA HTTPS" dir=in action=allow protocol=TCP localport=8443 | out-null
netsh advfirewall firewall add rule name="ACA HTTPS" dir=out action=allow protocol=TCP localport=8443 | out-null

# Make folder for necessary files
mkdir hirs | out-null
Push-Location  .\hirs\ | out-null

Write-Host "Retreiving Configuration Files"
wget https://raw.githubusercontent.com/nsacyber/HIRS/main/.ci/docker/compose-acceptance-test.yml -o compose-acceptance-test.yml
Write-Host "Retreiving Trust Stores"
wget https://raw.githubusercontent.com/nsacyber/HIRS/main/.ci/setup/certs/oem_certs.zip -o oem_certs.zip
wget https://raw.githubusercontent.com/nsacyber/HIRS/main/scripts/start_hat.ps1 -o start_hat.ps1
wget https://raw.githubusercontent.com/nsacyber/HIRS/main/scripts/remove_hat.ps1 -o remove_hat.ps1
#wget https://raw.githubusercontent.com/nsacyber/HIRS/v3_issue_645/.ci/setup/certs/oem_certs.zip -o oem_certs.zip
#wget https://raw.githubusercontent.com/nsacyber/HIRS/v3_issue_645/scripts/start_hat.ps1 -o start_hat.ps1
#wget https://raw.githubusercontent.com/nsacyber/HIRS/v3_issue_645/scripts/remove_hat.ps1 -o remove_hat.ps1

Expand-Archive -Path oem_certs.zip
Write-Host "Downloading images (This can take a while)"
docker pull ghcr.io/nsacyber/hirs/aca:latest
docker pull ghcr.io/nsacyber/hirs/hat:latest
Write-Host "Creating shortcut for starting the Acceptance Test (HAT start)"

# Create a shortcut to the start_hat.ps1 script
$WshShell = New-Object -comObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut("$Home\Desktop\start_hat.lnk")
$Shortcut.Targetpath = "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe"
$Shortcut.Arguments = "-ExecutionPolicy bypass  $Home\hirs\start_hat.ps1"
$Shortcut.Save()

# Start up the containers in a detached mode
 docker compose -f $Home\hirs\compose-acceptance-test.yml up --detach
# Wait for ACA to start
Write-Host "Waiting for ACA to start up on local host port 8443 ..."
Write-Host " Note that several TCP connect failure notices are expectred while the container boots up."
Start-Sleep -seconds 10  
  while ((Test-NetConnection -computername localhost -Port 8443 ).TcpTestSucceeded -eq $FALSE )  {   Start-Sleep -seconds 5  }
Write-Host "ACA is up!"
# Upload all files in the upload folder
Write-Host "Uploading OEM Certificates Chains to the ACA..."
Get-ChildItem ".\oem_certs\upload\"  | 
foreach-Object {
    $filename = $_.FullName
    Write-Host "Uploading $filename"
    curl.exe -k -F "file=@$filename" `
    "https://127.0.0.1:8443/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload"
}

# Done
Write-Host "HIRS Acceptance Test Installation complete."
Write-Host "Open up the HIRS ACA Portal on your browser using this url: https://localhost:8443"
Pop-Location | out-null