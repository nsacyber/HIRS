<#
.SYNOPSIS
    Removes the HIRS Acceptance Test (HAT) from this PC.

.DESCRIPTION
    Removes the aca and hat containers (and with them the ACA database), the HAT network, the
    localhost:8443 mapping, the "ACA HTTPS" firewall rule and the desktop shortcuts.

.PARAMETER KeepFiles
    Never delete the install folder.

.PARAMETER Force
    Do not ask for confirmation.

.PARAMETER Pause
    Keep the window open at the end. Set automatically after elevation.
#>
[CmdletBinding()]
param(
    [switch] $KeepFiles,
    [switch] $Force,
    [switch] $Pause
)

$ErrorActionPreference = 'Continue'
$ComposeProject = 'hirs'      # the same fixed project name start_hat.ps1 uses

# The compose file next to this script (a plain install folder), else where the HIRS repository keeps it.
$LocalCompose = Join-Path $PSScriptRoot 'compose-acceptance-test-windows.yml'
$ComposeFile  = $LocalCompose
$inRepo = Join-Path $PSScriptRoot '..\.ci\docker\compose-acceptance-test-windows.yml'
if (-not (Test-Path -LiteralPath $ComposeFile) -and (Test-Path -LiteralPath $inRepo)) { $ComposeFile = $inRepo }

function Write-Ok   { param([string] $Text) Write-Host "    [ OK ] $Text" -ForegroundColor Green }
function Write-Note { param([string] $Text) Write-Host "    [NOTE] $Text" -ForegroundColor Yellow }
function Write-Line { param([string] $Text) Write-Host "    $Text" }

function Exit-Script {
    param([int] $Code)
    if ($Pause) { Write-Host ''; Read-Host 'Press Enter to close this window' | Out-Null }
    exit $Code
}

function Invoke-Native {
    param([string] $Exe, [string[]] $Arguments)
    $out = & $Exe @Arguments 2>&1 | ForEach-Object { "$_" }
    return [pscustomobject]@{ Code = $LASTEXITCODE; Out = @($out) }
}

function Test-IsAdmin {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    return (New-Object Security.Principal.WindowsPrincipal $identity).IsInRole(
        [Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-IsAdmin)) {
    $forward = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"{0}"' -f $PSCommandPath), '-Pause')
    if ($KeepFiles) { $forward += '-KeepFiles' }
    if ($Force)     { $forward += '-Force' }
    try { Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $forward -ErrorAction Stop }
    catch {
        Write-Host '    [FAIL] Administrator rights are needed and were not granted. Nothing was removed.' -ForegroundColor Red
        Exit-Script 1
    }
    exit 0
}

Write-Host 'Removing the HIRS Acceptance Test (HAT)' -ForegroundColor White

# Only a folder that is clearly a HAT install is ever deleted.
$deleteFolder = (-not $KeepFiles) -and ((Split-Path -Leaf $PSScriptRoot) -ieq 'hirs') -and (Test-Path -LiteralPath $LocalCompose)

if (-not $Force) {
    Write-Host ''
    Write-Host '    This removes the ACA and HAT containers. The ACA database (trust certificates, policy,' -ForegroundColor Yellow
    Write-Host '    validation reports) is erased and cannot be recovered.' -ForegroundColor Yellow
    if ($deleteFolder) { Write-Host "    The folder $PSScriptRoot is deleted as well." -ForegroundColor Yellow }
    if ((Read-Host '    Type y to continue') -ne 'y') {
        Write-Line 'Nothing was removed.'
        Exit-Script 0
    }
}

# --- Containers and network --------------------------------------------------------------------
$dockerOk = $false
if (Get-Command 'docker' -ErrorAction SilentlyContinue) {
    $dockerOk = ((Invoke-Native 'docker' @('version', '--format', '{{.Server.Os}}')).Code -eq 0)
}
if ($dockerOk) {
    if (Test-Path -LiteralPath $ComposeFile) {
        & docker compose -p $ComposeProject -f $ComposeFile down 2>&1 | ForEach-Object { Write-Line "  $_" }
    }
    # Whatever is left (for example containers that were created by hand).
    foreach ($name in 'aca', 'hat') {
        if ((Invoke-Native 'docker' @('container', 'inspect', '-f', '{{.Name}}', $name)).Code -eq 0) {
            Invoke-Native 'docker' @('rm', '-f', $name) | Out-Null
        }
    }
    if ((Invoke-Native 'docker' @('network', 'inspect', '-f', '{{.Name}}', 'hat_network')).Code -eq 0) {
        Invoke-Native 'docker' @('network', 'rm', 'hat_network') | Out-Null
    }

    $left = @()
    foreach ($name in 'aca', 'hat') {
        if ((Invoke-Native 'docker' @('container', 'inspect', '-f', '{{.Name}}', $name)).Code -eq 0) { $left += "container $name" }
    }
    if ((Invoke-Native 'docker' @('network', 'inspect', '-f', '{{.Name}}', 'hat_network')).Code -eq 0) { $left += 'network hat_network' }
    if ($left.Count -eq 0) { Write-Ok 'Containers and HAT network removed. The images were kept.' }
    else { Write-Note ("Could not remove: {0}. Remove them in Docker Desktop." -f ($left -join ', ')) }
}
else {
    Write-Note 'Docker is not running, so the containers and the HAT network were NOT removed.'
    Write-Line 'Start Docker Desktop and run this script again to remove them.'
}

# --- Host settings -----------------------------------------------------------------------------
Invoke-Native 'netsh.exe' @('interface', 'portproxy', 'delete', 'v4tov4', 'listenport=8443', 'listenaddress=127.0.0.1') | Out-Null
Write-Ok 'localhost:8443 mapping removed.'

try {
    Get-NetFirewallRule -DisplayName 'ACA HTTPS' -ErrorAction SilentlyContinue | Remove-NetFirewallRule -ErrorAction SilentlyContinue
    Write-Ok 'Firewall rule "ACA HTTPS" removed.'
}
catch { Write-Note "Could not remove the firewall rule: $($_.Exception.Message)" }

$desktop = [Environment]::GetFolderPath('Desktop')
foreach ($shortcut in 'Start HAT.lnk', 'start_hat.lnk') {
    $path = Join-Path $desktop $shortcut
    if (Test-Path -LiteralPath $path) { Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue }
}
Write-Ok 'Desktop shortcuts removed.'

# --- Files -------------------------------------------------------------------------------------
if ($deleteFolder -and $dockerOk) {
    Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
    Remove-Item -LiteralPath $PSScriptRoot -Recurse -Force -ErrorAction SilentlyContinue
    if (Test-Path -LiteralPath $PSScriptRoot) { Write-Note "Some files in $PSScriptRoot are in use and were left behind. Delete the folder by hand." }
    else { Write-Ok "Folder $PSScriptRoot deleted." }
}
elseif ($deleteFolder) {
    Write-Note "The folder $PSScriptRoot was kept, because the containers could not be removed yet."
}
else {
    Write-Note "The files in $PSScriptRoot were left in place."
}

Write-Host ''
Write-Host 'The HAT has been removed from this PC.' -ForegroundColor Green
Exit-Script 0
