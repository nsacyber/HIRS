#Requires -RunAsAdministrator
# Powershell script to install the HIRS Acceptance Test on Windows

# Check For Docker Services
$Service = Get-Service -Name Docker
if ($Service.Status -ne 'Running') {
  Write-Host "Docker is either NOT running or NOT installed." 
  Write-Host "Please start or install Docker Desktop. See https://docs.docker.com/desktop/install/windows-install/";
  Write-Host "Exiting without removing the HAT. Hit Any Key to exit"
  $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
  exit;
} else {
      Write-Host "Docker is running, continuing installation..."
}

# Check for previos install 
if (Test-Path -Path hirs) {
  Write-Host "The hirs folder exists under the current directory, aborting install."
  Write-Host "Exiting without removing the HAT. Hit Any Key to exit"
  $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
  exit
}

# Make Firwall Rules for ACA to operate
Write-Host "Adding Firewall rules"
netsh advfirewall firewall add rule name="ACA HTTPS" dir=in action=allow protocol=TCP localport=8443
netsh advfirewall firewall add rule name="ACA HTTPS" dir=out action=allow protocol=TCP localport=8443

# Make folder for necessary files
mkdir hirs | out-null
Push-Location  .\hirs\ | out-null

# Download necessary files
Write-Host "Reteiving Configuration Files"
wget https://raw.githubusercontent.com/nsacyber/HIRS/v3_issue_645/.ci/docker/compose-acceptance-test.yml -o compose-acceptance-test.yml
Write-Host "Retreiving Trust Stores"
wget https://raw.githubusercontent.com/nsacyber/HIRS/v3_issue_645/.ci/setup/certs/oem_certs.zip -o oem_certs.zip
#Copy-Item -Path ..\projects\github\HIRS\.ci\setup\certs\oem_certs.zip -Destination .
wget https://raw.githubusercontent.com/nsacyber/HIRS/v3_issue_645/scripts/start_hat.ps1 -o start_hat.ps1
#Copy-Item -Path ..\projects\github\HIRS\scripts\start_hat.ps1 -Destination .
wget https://raw.githubusercontent.com/nsacyber/HIRS/v3_issue_645/scripts/remove_hat.ps1 -o remove_hat.ps1
#Copy-Item -Path ..\projects\github\HIRS\scripts\remove_hat.ps1 -Destination .
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

# Warn Admin that device needs to be attached for the next step
Write-Host "Please attach ethernet cable to this device and target device for the next step . Hit Any Key to Continue"
$Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
# Start up the containers in a detached mode
 docker compose -f $Home\hirs\compose-acceptance-test.yml up --detach
# Wait for ACA to start
Write-Host "Waiting for ACA to start up on local host port 8443 ..."
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
Write-Host "Use the Desktop Shortcut to start the ACA and hat servers."
Pop-Location | out-null