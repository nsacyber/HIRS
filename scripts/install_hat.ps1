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
docker pull ghcr.io/nsacyber/hirs/hat:alpha6
Write-Host "Creating shortcut for starting the Acceptance Test (HAT start)"

# Create a shortcut to the start_hat.ps1 script
$WshShell = New-Object -comObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut("$Home\Desktop\start_hat.lnk")
$Shortcut.Targetpath = "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe"
$Shortcut.Arguments = "-ExecutionPolicy bypass  $Home\hirs\start_hat.ps1"
$Shortcut.Save()

# Done
Write-Host "HIRS Acceptance Test Installation complete."
Write-Host "Use the Desktop Shortcut to start the ACA and hat servers."
Pop-Location | out-null