# Script to start the docker continers used for the HIRS Acceptance Test

$DockerProc = Get-process "*docker desktop*"
  if ($DockerProc.Count -eq 0 ) {
  	Write-Host "Docker Service is not started, please start Docker Desktop."
	Write-Host "Exiting without starting HAT. Hit any key to exit"
    $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
	exit
}

$IsAcaRunning = docker container inspect -f '{{.State.Running}}' aca 2>&1
$IsHatRunning = docker container inspect -f '{{.State.Running}}' hat 2>&1

if ($IsHatRunning -eq $TRUE) {
	Write-Host "HAT container is already running"
}

if ($IsAcaRunning -eq $TRUE) {
	Write-Host "ACA container is already running"
}

if ( ($IsHatRunning -eq $TRUE) -and ($IsAcaRunning -eq $TRUE) ) {
  Write-Host "ACA and Hat container are already started, exiting" 
  Write-Host "You can check container status in the Docker Desktop."
  Write-Host "Use the following URL in your Browser to view the ACA Portal: https://localhost:8443"
  Write-Host "Hit any key to exit"
  $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
  exit
 } else {
   Write-Host "Starting ACA and HAT containers..."
   docker compose -f $Home\hirs\compose-acceptance-test.yml up --detach
 }

Write-Host "HIRS Acceptance Test Servers Have been started."
Write-Host "You can check container status in the Docker Desktop."
Write-Host "Use the following URL in your Browser to view the ACA Portal: https://localhost:8443"
Write-Host "Hit any key"
$Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
exit;