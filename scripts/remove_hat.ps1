#Requires -RunAsAdministrator
# Powershell script to install the HIRS Acceptance Test on Windows

$Service = Get-Service -Name Docker
if ($Service.Status -ne 'Running') {
  Write-Host "Docker is either NOT running or NOT installed." 
  Write-Host "Please start Docker Desktop."
  Write-Host "Exiting without removing the HAT. Hit Any Key to exit"
  $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
  Exit;
} else {
      Write-Host "Docker is running, continuing HAT removal..."
}

# remove Firewall Rules
Write-Host "Removing HAT FW Rule ACA HTTPS"
netsh advfirewall firewall delete rule name="ACA HTTPS"

# remove HAT Docker containers and images
$IsAcaRunning = docker container inspect -f '{{.State.Running}}' aca 2>&1 | out-null
$IsHatRunning = docker container inspect -f '{{.State.Running}}' hat 2>&1 | out-null

if ($IsHatRunning -eq $TRUE) {
  Write-Host "Shutting down the HAT container"
  docker stop hat
}

if ($IsAcaRunning -eq $TRUE) {
  Write-Host "Shutting down the ACA container"
  docker stop aca
}

Write-Host "Removing HAT images"

#docker image rm ghcr.io/nsacyber/hirs/aca:latest
#docker image rm ghcr.io/nsacyber/hirs/hat:alpha6

Write-Host "Removing local HAT folder and files"
cd ..
if (Test-Path -LiteralPath hirs) {
  Remove-Item -LiteralPath hirs -Recurse
}

Write-Host "Removing HAT Deskstop Shortcut"
Remove-Item "$Home\Desktop\start_hat.lnk" -Force

Write-Host "HAT has been removed from the system"