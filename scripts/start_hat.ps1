# Script to start the docker continers used for the HIRS Acceptance Test

$DockerProc = Get-process "*docker desktop*"
  if ($DockerProc.Count -eq 0 ) {
  	Write-Host "Docker Service is not started, please start Docker desktop."
	Write-Host "Exiting without starting Acceptance Test. Hit Any Key to exit"
    $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
	exit
}

$IsAcaRunning = docker container inspect -f '{{.State.Running}}' aca 2>&1 | out-null
$IsHatRunning = docker container inspect -f '{{.State.Running}}' hat 2>&1 | out-null

if ($IsHatRunning -eq $TRUE) {
	Write-Host "HAT container is already running"
}

if ($IsAcaRunning -eq $TRUE) {
	Write-Host "ACA container is already running"
}

if ( ($IsHatRunning -eq $TRUE) -and ($IsAcaRunning -eq $TRUE) ) {
  Write-Host  "ACA and Hat container are already started, exiting"
  Write-Host  "Hit any key to exit"
  $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
  exit
 } else {
   Write-Host "Starting ACA and HAT containers..."
   docker compose -f $Home\hirs\compose-acceptance-test.yml up
 }

Write-Host "HIRS Acceptance Test Servers Have been started."
Write-Host "You can check container status in the Docker Desktop."
Write-Host "Use the following URL in your Browser to view the ACA Portal: https://172.16.1.75:8443"
Write-Host "Hit Any Key to continue"
$Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")