<#
.SYNOPSIS
    Starts the HIRS Acceptance Test (the ACA and HAT containers) on a Windows host that runs
    Docker Desktop with Windows containers, and makes the ACA portal reachable at
    https://localhost:8443.

.DESCRIPTION
    One script for the operator. It asks for administrator rights itself, then:

      1. Waits for Docker Desktop (starts it if it is not running, switches it to Windows
         containers if needed).
      2. Works out which wired adapter goes to the test devices and waits for its link.
         The HAT's PXE services cannot start without the link.
      3. Starts the existing aca and hat containers. Only if one is missing does it run
         docker compose, and then with --no-recreate, so an existing ACA database is never
         replaced by this script unless you pass -Recreate.
      4. Makes sure the HAT's PXE services came up and stay up.
      5. Points https://localhost:8443 at the ACA container. The container's address changes
         whenever it restarts, so this is redone on every run.
      6. Waits until the ACA portal answers, and makes this PC trust the portal's own TLS
         certificate (that one certificate only), so Edge and Chrome show no warning for it.
      7. If the ACA container was created during this run, its database is empty, so
         reset_hat.ps1 is run to set the test policy and load the trust certificates.
         Otherwise the policy and the number of trust certificates are only checked and shown.
      8. Opens the portal in the browser.

    This is also the installer: on a PC that has Docker Desktop but no HAT yet, running this
    script creates everything (containers, firewall rule for the portal, desktop shortcut).

    It is safe to run again at any time: it only fixes what is not already right.
    Every run is logged to a "logs" folder next to this script.

    Files it needs:
      compose-acceptance-test-windows.yml   next to this script, or in ..\.ci\docker (HIRS repository layout)
      reset_hat.ps1                         next to this script

.PARAMETER ComposeFile
    Compose file to use when a container has to be created. Default: see "Files it needs".

.PARAMETER Adapter
    Name of the wired adapter that goes to the test devices (see Get-NetAdapter). Only needed
    when the PC has more than one wired adapter. It is used when the HAT network is created;
    to move an existing HAT network to another adapter, combine it with -Recreate.

.PARAMETER StatusOnly
    Report the current state and change nothing. Does not need administrator rights.

.PARAMETER Recreate
    Remove and re-create both containers and the HAT network from the compose file (after
    changing an image tag or the compose file, or to repair a damaged HAT network).
    THIS ERASES THE ACA DATABASE: trust certificates, policy and reports. Asks for
    confirmation unless -Force is also given. The policy and the trust certificates are
    loaded again automatically (step 7).

.PARAMETER SkipCableCheck
    Do not wait for the Ethernet link, and accept -Adapter as given without checking that the
    adapter exists. Use it if this script misjudges the adapters on a particular PC.

.PARAMETER SkipAcaSetup
    Skip step 7: do not load policy and trust certificates into a new ACA, and do not check
    them on an existing one.

.PARAMETER SkipCertTrust
    Do not add the portal's TLS certificate to this PC's trusted certificates. The browser
    then shows a certificate warning for https://localhost:8443, as it always did.

.PARAMETER NoBrowser
    Do not open the ACA portal when finished.

.PARAMETER CreateShortcut
    Put a "Start HAT" shortcut for this script on the Desktop and exit.

.PARAMETER Pause
    Keep the window open at the end. Set automatically for the shortcut and after elevation.

.EXAMPLE
    .\start_hat.ps1

.EXAMPLE
    .\start_hat.ps1 -StatusOnly

.EXAMPLE
    .\start_hat.ps1 -Recreate        # after editing the image tag in the compose file
#>
[CmdletBinding()]
param(
    [string] $ComposeFile,
    [string] $Adapter,
    [switch] $StatusOnly,
    [switch] $Recreate,
    [switch] $Force,
    [switch] $SkipCableCheck,
    [switch] $SkipAcaSetup,
    [switch] $SkipCertTrust,
    [switch] $NoBrowser,
    [switch] $CreateShortcut,
    [switch] $Pause,
    [int] $DockerTimeoutMinutes = 5,
    [int] $AcaTimeoutMinutes = 10
)

# Native tools (docker, netsh) write progress to stderr. With 'Stop', Windows PowerShell 5.1
# would turn that into a terminating error, so failures are checked through exit codes instead.
$ErrorActionPreference = 'Continue'

$AcaContainer = 'aca'
$HatContainer = 'hat'
$NatNetwork   = 'nat'
$HatNetwork   = 'hat_network'
$AdapterOption = 'com.docker.network.windowsshim.interface'
# Fixed compose project name, so it does not depend on the folder the compose file is in.
$ComposeProject = 'hirs'
$FirewallRule = 'ACA HTTPS'
$ShortcutName = 'Start HAT.lnk'
# Where the portal's TLS certificate is trusted, and the label that marks the entry as ours.
# remove_hat.ps1 uses the same three values.
$CertStoreName     = 'TrustedPeople'
$CertStoreLocation = 'LocalMachine'
$CertLabel         = 'HIRS ACA portal (added by start_hat.ps1)'
$PortalPort   = 8443
$PortalPath   = '/HIRS_AttestationCAPortal/portal/index'
$PortalUrl    = "https://localhost:$PortalPort$PortalPath"

$script:DockerHelpShown   = $false
$script:ComposeConfigRead = $false
$script:ComposeConfig     = $null
$script:TranscriptOn      = $false

if (-not $ComposeFile) {
    # Next to this script (a plain install folder), else where the HIRS repository keeps it.
    $ComposeFile = Join-Path $PSScriptRoot 'compose-acceptance-test-windows.yml'
    $inRepo = Join-Path $PSScriptRoot '..\.ci\docker\compose-acceptance-test-windows.yml'
    if (-not (Test-Path -LiteralPath $ComposeFile) -and (Test-Path -LiteralPath $inRepo)) { $ComposeFile = $inRepo }
}
if (Test-Path -LiteralPath $ComposeFile) { $ComposeFile = (Resolve-Path -LiteralPath $ComposeFile).Path }
$ResetScript = Join-Path $PSScriptRoot 'reset_hat.ps1'

# ---------------------------------------------------------------------------------------------
# Output helpers
# ---------------------------------------------------------------------------------------------
function Write-Step { param([string] $Text) Write-Host ''; Write-Host "==> $Text" -ForegroundColor Cyan }
function Write-Ok   { param([string] $Text) Write-Host "    [ OK ] $Text" -ForegroundColor Green }
function Write-Note { param([string] $Text) Write-Host "    [NOTE] $Text" -ForegroundColor Yellow }
function Write-Fail { param([string] $Text) Write-Host "    [FAIL] $Text" -ForegroundColor Red }
function Write-Line { param([string] $Text) Write-Host "    $Text" }

function Exit-Script {
    param([int] $Code)
    if ($script:TranscriptOn) { try { Stop-Transcript | Out-Null } catch { } }
    if ($Pause) { Write-Host ''; Read-Host 'Press Enter to close this window' | Out-Null }
    exit $Code
}

# Runs a native command and returns its exit code and all output lines (stdout and stderr) as text.
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

# ---------------------------------------------------------------------------------------------
# Docker
# ---------------------------------------------------------------------------------------------
# 'windows', 'linux', or $null when the engine does not answer.
function Get-EngineOs {
    $r = Invoke-Native 'docker' @('version', '--format', '{{.Server.Os}}')
    if ($r.Code -ne 0) { return $null }
    $os = @($r.Out | Where-Object { $_ -match '^\s*(windows|linux)\s*$' }) | Select-Object -Last 1
    if ($os) { return $os.Trim() }
    return $null
}

function Wait-DockerEngine {
    $deadline = (Get-Date).AddMinutes($DockerTimeoutMinutes)
    $launched = $false
    $switched = $false
    $lastNote = Get-Date

    while ($true) {
        $os = Get-EngineOs
        if ($os -eq 'windows') { return $true }

        if ($os -eq 'linux') {
            if (-not $switched) {
                Write-Line 'Docker is set to Linux containers. Switching it to Windows containers...'
                $cli = Join-Path $env:ProgramFiles 'Docker\Docker\DockerCli.exe'
                if (-not (Test-Path -LiteralPath $cli)) {
                    Write-Fail "Cannot switch automatically ($cli not found)."
                    Write-Line 'Right-click the Docker icon in the system tray and choose "Switch to Windows containers...", then run this again.'
                    $script:DockerHelpShown = $true
                    return $false
                }
                & $cli -SwitchWindowsEngine 2>&1 | Out-Null
                $switched = $true
            }
        }
        elseif (-not $launched -and -not (Get-Process -Name 'Docker Desktop' -ErrorAction SilentlyContinue)) {
            $desktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
            if (Test-Path -LiteralPath $desktop) {
                Write-Line 'Docker Desktop is not running. Starting it...'
                # Through explorer.exe so that Docker Desktop is not started with administrator rights.
                Start-Process -FilePath 'explorer.exe' -ArgumentList ('"{0}"' -f $desktop)
            }
            else {
                Write-Note 'Docker Desktop is not running and was not found in Program Files. Start it by hand.'
            }
            $launched = $true
        }

        if ((Get-Date) -gt $deadline) { return $false }
        if (((Get-Date) - $lastNote).TotalSeconds -ge 20) {
            Write-Line 'Still waiting for Docker...'
            $lastNote = Get-Date
        }
        Start-Sleep -Seconds 5
    }
}

# Container status ('running', 'exited', 'restarting', 'created', ...) or $null when there is no such container.
function Get-ContainerStatus {
    param([string] $Name)
    $r = Invoke-Native 'docker' @('container', 'inspect', '-f', '{{.State.Status}}', $Name)
    if ($r.Code -ne 0 -or $r.Out.Count -eq 0) { return $null }
    return ($r.Out | Select-Object -Last 1).Trim()
}

function Get-ContainerField {
    param([string] $Name, [string] $Template)
    $r = Invoke-Native 'docker' @('container', 'inspect', '-f', $Template, $Name)
    if ($r.Code -ne 0 -or $r.Out.Count -eq 0) { return $null }
    return ($r.Out | Select-Object -Last 1).Trim()
}

function Show-LogTail {
    param([string] $Name, [int] $Lines = 15)
    $r = Invoke-Native 'docker' @('logs', '--tail', "$Lines", $Name)
    foreach ($line in $r.Out) { Write-Host "        | $line" -ForegroundColor DarkGray }
}

# ---------------------------------------------------------------------------------------------
# Wired adapter and Ethernet link
# ---------------------------------------------------------------------------------------------
function Get-WiredAdapters {
    return @(Get-NetAdapter -Physical -ErrorAction SilentlyContinue | Where-Object { $_.MediaType -eq '802.3' })
}

# The adapter an existing HAT network was created on, or $null (no network yet, or a network
# created by an older compose file that let Docker choose).
function Get-HatNetworkAdapter {
    $r = Invoke-Native 'docker' @('network', 'inspect', '-f', '{{json .Options}}', $HatNetwork)
    if ($r.Code -ne 0 -or $r.Out.Count -eq 0) { return $null }
    try {
        $options = (($r.Out | Select-Object -Last 1) | ConvertFrom-Json)
        $value = $options.$AdapterOption
        if ($value) { return [string] $value }
    }
    catch { }
    return $null
}

function Test-HatNetworkExists {
    return ((Invoke-Native 'docker' @('network', 'inspect', '-f', '{{.Name}}', $HatNetwork)).Code -eq 0)
}

# Decides which adapter matters and waits for its link.
# Returns Ok, Name (adapter to use; '' when it could not be narrowed down), Waited (link was down at first).
function Wait-Cable {
    $result = [pscustomobject]@{ Ok = $true; Name = ''; Waited = $false }

    $wired = @(Get-WiredAdapters)
    $names = @($wired | ForEach-Object { $_.Name })
    $bound = $null
    if (-not $Recreate) { $bound = Get-HatNetworkAdapter }

    $watch = $null      # one adapter name, or $null = any wired adapter
    if ($bound) {
        $watch = $bound
        if ($Adapter -and $Adapter -ne $bound) {
            Write-Note "The HAT network is already on adapter '$bound'. -Adapter '$Adapter' only takes effect together with -Recreate."
        }
    }
    elseif ($Adapter) { $watch = $Adapter }
    elseif ($names.Count -eq 1) { $watch = $names[0] }

    if ($SkipCableCheck) {
        # The operator's word is taken as given: no detection, no waiting.
        if ($watch) { $result.Name = $watch }
        return $result
    }

    if ($watch -and $names -notcontains $watch) {
        Write-Fail "Wired adapter '$watch' was not found on this PC."
        if ($names.Count -gt 0) { Write-Line ("Wired adapters here: {0}" -f ($names -join ', ')) }
        else { Write-Line 'No wired Ethernet adapter is present (USB adapter unplugged?).' }
        if ($bound) { Write-Line 'The HAT network was created on that adapter. Reconnect it, or run this script with -Recreate to rebuild the network (that erases the ACA database).' }
        $result.Ok = $false
        return $result
    }
    if ($names.Count -eq 0) {
        Write-Note 'No wired Ethernet adapter was detected, so the cable check is skipped.'
        return $result
    }

    $lastNote = (Get-Date).AddMinutes(-1)
    while ($true) {
        $up = @(Get-WiredAdapters | Where-Object { $_.Status -eq 'Up' } | ForEach-Object { $_.Name })
        if ($watch) {
            if ($up -contains $watch) { $result.Name = $watch; return $result }
        }
        elseif ($up.Count -eq 1) { $result.Name = $up[0]; return $result }
        elseif ($up.Count -gt 1) {
            # Several wired adapters have a link and nothing says which one goes to the devices.
            $result.Name = ''
            $script:AmbiguousAdapters = $up
            return $result
        }

        $result.Waited = $true
        if (((Get-Date) - $lastNote).TotalSeconds -ge 15) {
            $which = if ($watch) { " on '$watch'" } else { '' }
            Write-Host "    [WAIT] No Ethernet link$which. Connect the cable from this PC to a powered-on test device or switch." -ForegroundColor Yellow
            $lastNote = Get-Date
        }
        Start-Sleep -Seconds 2
    }
}

# ---------------------------------------------------------------------------------------------
# HAT (PXE services)
# ---------------------------------------------------------------------------------------------
function Get-HatSnapshot {
    $raw = Get-ContainerField $HatContainer '{{.State.Status}}|{{.State.StartedAt}}|{{.RestartCount}}|{{if .State.Health}}{{.State.Health.Status}}{{end}}'
    if (-not $raw) { return $null }
    $p = $raw -split '\|'
    return [pscustomobject]@{
        Status    = $p[0]
        StartedAt = $(if ($p.Count -gt 1) { $p[1] } else { '' })
        Restarts  = $(if ($p.Count -gt 2) { $p[2] } else { '' })
        Health    = $(if ($p.Count -gt 3) { $p[3] } else { '' })
    }
}

# Seconds since the container's main process was started, or a large number when unknown.
function Get-SecondsSinceStart {
    param([string] $StartedAt)
    try {
        $t = [datetime]::Parse(($StartedAt -replace '\.\d+', ''), [Globalization.CultureInfo]::InvariantCulture,
            [Globalization.DateTimeStyles]::AdjustToUniversal -bor [Globalization.DateTimeStyles]::AssumeUniversal)
        return ([datetime]::UtcNow - $t).TotalSeconds
    }
    catch { return 99999 }
}

# Makes sure the HAT container is running and STAYS running. PyPXE exits within seconds when it
# cannot bind its network sockets (cable unplugged), and with a restart policy Docker then keeps
# restarting it, so one "running" reading is not proof. Returns Running and Confirmed (the PXE
# services reported that they started, or Docker reports the container healthy).
function Confirm-Hat {
    param([bool] $Watch)

    $snap = Get-HatSnapshot
    if (-not $snap) { return [pscustomobject]@{ Running = $false; Confirmed = $false } }

    # Already running for a while and nothing changed: trust it, do not make the operator wait.
    if (-not $Watch -and $snap.Status -eq 'running' -and (Get-SecondsSinceStart $snap.StartedAt) -ge 30) {
        return [pscustomobject]@{ Running = $true; Confirmed = ($snap.Health -eq 'healthy') }
    }

    $starts        = 0
    $kicked        = $false
    $noted         = $false
    $lastStartedAt = ''
    $stableSamples = 0

    for ($i = 0; $i -lt 50; $i++) {            # up to about 2.5 minutes
        $snap = Get-HatSnapshot
        if (-not $snap) { break }

        if ($snap.Status -eq 'running') {
            if ($snap.StartedAt -ne $lastStartedAt) { $lastStartedAt = $snap.StartedAt; $stableSamples = 0 }
            $stableSamples++

            $confirmed = ($snap.Health -eq 'healthy')
            if (-not $confirmed -and $snap.StartedAt) {
                $logs = Invoke-Native 'docker' @('logs', '--since', $snap.StartedAt, $HatContainer)
                $confirmed = (@($logs.Out | Where-Object { $_ -match 'successfully initialized' }).Count -gt 0)
            }
            if ($confirmed)             { return [pscustomobject]@{ Running = $true; Confirmed = $true } }
            if ($stableSamples -ge 6)   { return [pscustomobject]@{ Running = $true; Confirmed = $false } }   # ~18 s without exiting
        }
        elseif ($snap.Status -eq 'restarting') {
            $stableSamples = 0
            if (-not $noted) {
                Write-Line 'The PXE services are not staying up yet. Docker is retrying...'
                $noted = $true
            }
            if (-not $kicked) {
                # Skip Docker's growing wait between retries now that the link is there.
                Invoke-Native 'docker' @('restart', $HatContainer) | Out-Null
                $kicked = $true
            }
        }
        else {
            # exited / created / dead: no restart policy (older compose file), or Docker gave up.
            $stableSamples = 0
            if ($starts -ge 3) { break }
            if ($starts -gt 0) {
                Write-Note "The HAT container stopped right after starting (attempt $starts of 3). Its last log lines:"
                Show-LogTail $HatContainer 8
            }
            $r = Invoke-Native 'docker' @('start', $HatContainer)
            $starts++
            if ($r.Code -ne 0) {
                Write-Note "docker start $HatContainer reported:"
                foreach ($line in $r.Out) { Write-Line "  $line" }
            }
        }
        Start-Sleep -Seconds 3
    }

    Write-Note 'Last HAT log lines:'
    Show-LogTail $HatContainer 12
    return [pscustomobject]@{ Running = $false; Confirmed = $false }
}

# ---------------------------------------------------------------------------------------------
# ACA portal
# ---------------------------------------------------------------------------------------------
function Get-AcaNatIp {
    $ip = Get-ContainerField $AcaContainer ('{{.NetworkSettings.Networks.' + $NatNetwork + '.IPAddress}}')
    if ($ip -and $ip -match '^\d{1,3}(\.\d{1,3}){3}$') { return $ip }
    return $null
}

function Get-PortProxyTarget {
    $r = Invoke-Native 'netsh.exe' @('interface', 'portproxy', 'show', 'v4tov4')
    foreach ($line in $r.Out) {
        if ($line -match ('^\s*127\.0\.0\.1\s+' + $PortalPort + '\s+(\S+)\s+(\d+)\s*$')) {
            return ('{0}:{1}' -f $Matches[1], $Matches[2])
        }
    }
    return $null
}

# Points 127.0.0.1:8443 at the ACA container's address on the Docker NAT network. That address
# changes whenever the container is re-created or restarted, which is why this is done every run.
function Set-PortalMapping {
    param([switch] $Quiet)

    $ip = Get-AcaNatIp
    if (-not $ip) { return $false }

    $wanted = '{0}:{1}' -f $ip, $PortalPort
    if ((Get-PortProxyTarget) -eq $wanted) {
        if (-not $Quiet) { Write-Ok "localhost:$PortalPort already points at the ACA ($ip)." }
        return $true
    }

    $svc = Get-Service -Name 'iphlpsvc' -ErrorAction SilentlyContinue
    if ($svc -and $svc.Status -ne 'Running') { Start-Service -Name 'iphlpsvc' -ErrorAction SilentlyContinue }

    Invoke-Native 'netsh.exe' @('interface', 'portproxy', 'delete', 'v4tov4',
        "listenport=$PortalPort", 'listenaddress=127.0.0.1') | Out-Null
    $add = Invoke-Native 'netsh.exe' @('interface', 'portproxy', 'add', 'v4tov4',
        "listenport=$PortalPort", 'listenaddress=127.0.0.1', "connectport=$PortalPort", "connectaddress=$ip")
    if ($add.Code -ne 0 -or (Get-PortProxyTarget) -ne $wanted) {
        Write-Fail "Could not point localhost:$PortalPort at the ACA ($ip):"
        foreach ($line in $add.Out) { Write-Line "  $line" }
        return $false
    }
    Write-Ok "localhost:$PortalPort now points at the ACA ($ip)."
    return $true
}

# HTTP status code from the portal through localhost, or 0 when there is no answer.
function Get-PortalHttpCode {
    if (-not (Get-Command 'curl.exe' -ErrorAction SilentlyContinue)) {
        # Very old Windows 10 builds have no curl.exe: fall back to "does the port accept a connection".
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $iar = $client.BeginConnect('127.0.0.1', $PortalPort, $null, $null)
            if ($iar.AsyncWaitHandle.WaitOne(3000) -and $client.Connected) { return 200 }
            return 0
        }
        catch { return 0 }
        finally { $client.Close() }
    }
    $r = Invoke-Native 'curl.exe' @('-k', '-s', '-o', 'NUL', '-m', '8', '-w', '%{http_code}',
        "https://127.0.0.1:$PortalPort$PortalPath")
    $code = 0
    [void][int]::TryParse((($r.Out -join '').Trim()), [ref] $code)
    return $code
}

function Wait-Aca {
    $start    = Get-Date
    $deadline = $start.AddMinutes($AcaTimeoutMinutes)
    $lastNote = Get-Date
    $notRunning = 0

    while ($true) {
        $code = Get-PortalHttpCode
        if ($code -ge 200 -and $code -lt 400) { return $true }

        # With a restart policy 'restarting' is a normal passing state; only a container that
        # stays down is a failure.
        $status = Get-ContainerStatus $AcaContainer
        if ($status -eq 'running' -or $status -eq 'restarting') { $notRunning = 0 } else { $notRunning++ }
        if ($notRunning -ge 3) {
            Write-Fail "The ACA container is not running (status: $status). Its last log lines:"
            Show-LogTail $AcaContainer 20
            return $false
        }
        if ((Get-Date) -gt $deadline) {
            Write-Fail "The ACA portal did not answer within $AcaTimeoutMinutes minutes. Its last log lines:"
            Show-LogTail $AcaContainer 20
            return $false
        }
        if (((Get-Date) - $lastNote).TotalSeconds -ge 20) {
            Write-Line ('The ACA is still starting ({0:N0} seconds so far; a few minutes is normal)...' -f ((Get-Date) - $start).TotalSeconds)
            # The container's address can change if Docker restarted it while we were waiting.
            Set-PortalMapping -Quiet | Out-Null
            $lastNote = Get-Date
        }
        Start-Sleep -Seconds 5
    }
}

# ---------------------------------------------------------------------------------------------
# Portal certificate (so the browser shows no warning for https://localhost:8443)
# ---------------------------------------------------------------------------------------------
# Only the portal's own server certificate is trusted, in the "Trusted People" store: that
# trusts this one certificate and nothing signed by anyone. The ACA's CA certificates are NOT
# installed on purpose. The Windows ACA image creates its keys when the image is built, so
# everyone who has the image has the CA's private keys; a PC that trusted that CA would accept
# a forged certificate for any web site.

# The certificate the portal presents on localhost, exactly as a browser receives it.
function Get-PortalCertificate {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        # Every step is time-limited: something that accepts the connection and then stays
        # silent must not hang this script.
        $pending = $client.BeginConnect('127.0.0.1', $PortalPort, $null, $null)
        if (-not $pending.AsyncWaitHandle.WaitOne(5000) -or -not $client.Connected) { return $null }
        $client.EndConnect($pending)
        $client.ReceiveTimeout = 10000
        $client.SendTimeout    = 10000
        $acceptAny = [System.Net.Security.RemoteCertificateValidationCallback] { param($s, $c, $ch, $e) $true }
        $ssl = New-Object System.Net.Security.SslStream($client.GetStream(), $false, $acceptAny)
        try {
            $ssl.AuthenticateAsClient('localhost', $null, [System.Security.Authentication.SslProtocols]::Tls12, $false)
            return (New-Object System.Security.Cryptography.X509Certificates.X509Certificate2($ssl.RemoteCertificate))
        }
        finally { $ssl.Dispose() }
    }
    catch { return $null }
    finally { $client.Close() }
}

# Why a certificate must not be trusted, or $null when it is fine.
function Get-PortalCertificateProblem {
    param($Certificate)
    $basic = $Certificate.Extensions | Where-Object { $_ -is [System.Security.Cryptography.X509Certificates.X509BasicConstraintsExtension] }
    if ($basic -and $basic.CertificateAuthority) { return 'it is a CA certificate, which this script never trusts' }
    $now = Get-Date
    if ($now -lt $Certificate.NotBefore -or $now -gt $Certificate.NotAfter) {
        return ('it is only valid from {0:yyyy-MM-dd} to {1:yyyy-MM-dd}' -f $Certificate.NotBefore, $Certificate.NotAfter)
    }
    $names = $Certificate.Extensions | Where-Object { $_.Oid.Value -eq '2.5.29.17' }
    if (-not $names -or $names.Format($false) -notmatch '(^|[^A-Za-z0-9.-])localhost($|[^A-Za-z0-9.-])') { return 'it is not issued for the name localhost' }
    return $null
}

function Open-PortalCertStore {
    param([string] $Access)
    $store = New-Object System.Security.Cryptography.X509Certificates.X509Store($CertStoreName, $CertStoreLocation)
    $store.Open([System.Security.Cryptography.X509Certificates.OpenFlags] $Access)
    return $store
}

# Trusts the portal's current certificate and drops one this script added for an earlier ACA.
# Never stops the start: on any problem the browser simply shows its warning, as before.
function Update-PortalCertificateTrust {
    try {
        $cert = Get-PortalCertificate
        if (-not $cert) {
            Write-Note "Could not read the portal's certificate, so the browser may show a certificate warning."
            return
        }
        $problem = Get-PortalCertificateProblem $cert
        if ($problem) {
            Write-Note "The portal's certificate was not added to this PC's trusted certificates, because $problem. The browser will show a certificate warning."
            return
        }

        $store = Open-PortalCertStore 'ReadWrite'
        try {
            $present = @($store.Certificates | Where-Object { $_.Thumbprint -eq $cert.Thumbprint }).Count -gt 0
            $stale   = @($store.Certificates | Where-Object { $_.FriendlyName -eq $CertLabel -and $_.Thumbprint -ne $cert.Thumbprint })
            foreach ($old in $stale) { $store.Remove($old) }
            if (-not $present) {
                $cert.FriendlyName = $CertLabel
                $store.Add($cert)
                if ($stale.Count -gt 0) { Write-Ok 'The ACA has a new portal certificate. This PC now trusts the new one in place of the old one.' }
                else { Write-Ok "This PC now trusts the portal's certificate. Edge and Chrome should open the portal without a certificate warning." }
            }
        }
        finally { $store.Close() }
    }
    catch { Write-Note "Could not update the trusted portal certificate: $($_.Exception.Message)" }
}

# For -StatusOnly: reports, changes nothing.
function Show-PortalCertificateTrust {
    try {
        $cert = Get-PortalCertificate
        if (-not $cert) { return }
        $store = Open-PortalCertStore 'ReadOnly'
        try { $present = @($store.Certificates | Where-Object { $_.Thumbprint -eq $cert.Thumbprint }).Count -gt 0 }
        finally { $store.Close() }
        if ($present) { Write-Ok "The portal's certificate is trusted on this PC." }
        else { Write-Note "The portal's certificate is not trusted on this PC, so the browser shows a certificate warning. Running this script without -StatusOnly adds it." }
    }
    catch { }
}

# ---------------------------------------------------------------------------------------------
# Compose file helpers (best effort; used only for warnings)
# ---------------------------------------------------------------------------------------------
function Get-ComposeService {
    param([string] $Service)
    if (-not $script:ComposeConfigRead) {
        $script:ComposeConfigRead = $true
        $script:ComposeConfig = $null
        if (Test-Path -LiteralPath $ComposeFile) {
            try {
                $json = & docker compose -p $ComposeProject -f $ComposeFile config --format json 2>$null
                if ($LASTEXITCODE -eq 0 -and $json) { $script:ComposeConfig = ($json -join "`n") | ConvertFrom-Json }
            }
            catch { $script:ComposeConfig = $null }
        }
    }
    if (-not $script:ComposeConfig) { return $null }
    try { return $script:ComposeConfig.services.$Service } catch { return $null }
}

# Warns when a container is not what the compose file now describes. Nothing is changed here:
# bringing a container in line means re-creating it, which is the operator's decision.
function Show-ComposeDrift {
    foreach ($name in $AcaContainer, $HatContainer) {
        $service = Get-ComposeService $name
        if (-not $service) { continue }
        $wanted = [string] $service.image
        $actual = Get-ContainerField $name '{{.Config.Image}}'
        if (-not $wanted -or -not $actual) { continue }

        if ($actual -ne $wanted) {
            Write-Note "The '$name' container was created from '$actual', but the compose file now says '$wanted'."
            Write-Line '       To switch to the new image run this script with -Recreate (that erases the ACA database).'
            continue
        }
        $containerImageId = Get-ContainerField $name '{{.Image}}'
        $local = Invoke-Native 'docker' @('image', 'inspect', '-f', '{{.Id}}', $wanted)
        if ($local.Code -eq 0 -and $local.Out.Count -gt 0 -and $containerImageId) {
            $localId = ($local.Out | Select-Object -Last 1).Trim()
            if ($localId -and $localId -ne $containerImageId) {
                Write-Note "A newer '$wanted' image has been downloaded, but the '$name' container still runs the older one."
                Write-Line '       To switch to it run this script with -Recreate (that erases the ACA database).'
                continue
            }
        }
        $wantedPolicy = [string] $service.restart
        $actualPolicy = Get-ContainerField $name '{{.HostConfig.RestartPolicy.Name}}'
        if ($wantedPolicy -and $actualPolicy -ne $null -and $wantedPolicy -ne $actualPolicy) {
            Write-Note "The '$name' container was created before the compose file gained 'restart: $wantedPolicy', so Docker will not restart it by itself."
            Write-Line '       It still works with this script. To pick the setting up, run this script with -Recreate (that erases the ACA database).'
        }
    }
}

# ---------------------------------------------------------------------------------------------
# Status report (also the whole of -StatusOnly)
# ---------------------------------------------------------------------------------------------
function Show-Status {
    Write-Step 'Current state'

    $os = Get-EngineOs
    if ($os -eq 'windows')   { Write-Ok 'Docker is running with Windows containers.' }
    elseif ($os -eq 'linux') { Write-Fail 'Docker is running, but set to Linux containers.' }
    else                     { Write-Fail 'Docker is not answering (Docker Desktop not started yet?).' }

    $wired = @(Get-WiredAdapters)
    $bound = $null
    if ($os) { $bound = Get-HatNetworkAdapter }
    if ($bound) {
        $a = @($wired | Where-Object { $_.Name -eq $bound })
        if ($a.Count -eq 0)             { Write-Fail "The HAT network is on adapter '$bound', which is not present." }
        elseif ($a[0].Status -eq 'Up')  { Write-Ok   "Ethernet link is up on '$bound' (the HAT network's adapter)." }
        else                            { Write-Fail "No Ethernet link on '$bound' (the HAT network's adapter): cable unplugged, or the device at the other end is off." }
    }
    else {
        $up = @($wired | Where-Object { $_.Status -eq 'Up' })
        if ($up.Count -gt 0)        { Write-Ok ("Ethernet link is up ({0})." -f (($up | ForEach-Object { $_.Name }) -join ', ')) }
        elseif ($wired.Count -gt 0) { Write-Fail 'Ethernet cable is not connected (or the device at the other end is off).' }
        else                        { Write-Note 'No wired Ethernet adapter detected.' }
    }

    if (-not $os) { return }

    foreach ($name in $AcaContainer, $HatContainer) {
        $status = Get-ContainerStatus $name
        if (-not $status) { Write-Fail "Container '$name' does not exist yet."; continue }
        $image  = Get-ContainerField $name '{{.Config.Image}}'
        $health = Get-ContainerField $name '{{if .State.Health}}{{.State.Health.Status}}{{end}}'
        $policy = Get-ContainerField $name '{{.HostConfig.RestartPolicy.Name}}'
        $extra  = "image $image"
        if ($health) { $extra += ", health: $health" }
        if ($policy) { $extra += ", restart policy: $policy" }
        if ($status -eq 'running') { Write-Ok   "Container '$name' is running ($extra)." }
        else                       { Write-Fail "Container '$name' is $status ($extra)." }
    }

    if ((Get-ContainerStatus $AcaContainer) -eq 'running') {
        $ip     = Get-AcaNatIp
        $target = Get-PortProxyTarget
        if ($ip -and $target -eq ('{0}:{1}' -f $ip, $PortalPort)) { Write-Ok "localhost:$PortalPort points at the ACA ($ip)." }
        else { Write-Fail "localhost:$PortalPort does not point at the ACA (ACA is at '$ip', mapping is '$target'). Run this script without -StatusOnly to fix it." }

        $code = Get-PortalHttpCode
        if ($code -ge 200 -and $code -lt 400) { Write-Ok "ACA portal answers: $PortalUrl" }
        else { Write-Fail "ACA portal does not answer on localhost (HTTP code $code)." }
    }
}

# =============================================================================================
# Main
# =============================================================================================
function New-HatShortcut {
    $desktop = [Environment]::GetFolderPath('Desktop')
    $shell   = New-Object -ComObject WScript.Shell
    $lnk     = $shell.CreateShortcut((Join-Path $desktop $ShortcutName))
    $lnk.TargetPath       = "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe"
    $lnk.Arguments        = '-NoProfile -ExecutionPolicy Bypass -File "{0}" -Pause' -f $PSCommandPath
    $lnk.WorkingDirectory = $PSScriptRoot
    $lnk.Description      = 'Start the HIRS Acceptance Test (ACA + HAT)'
    $lnk.Save()
    return $lnk.FullName
}

if ($CreateShortcut) {
    Write-Host "Created shortcut: $(New-HatShortcut)"
    Exit-Script 0
}

if (-not (Get-Command 'docker' -ErrorAction SilentlyContinue)) {
    Write-Fail 'The docker command was not found. Install Docker Desktop (or open a new window if it was just installed).'
    Exit-Script 1
}

if ($StatusOnly) {
    Show-Status
    if ((Get-ContainerStatus $AcaContainer) -eq 'running') { Show-PortalCertificateTrust }
    if (-not $SkipAcaSetup -and (Test-Path -LiteralPath $ResetScript) -and (Get-ContainerStatus $AcaContainer) -eq 'running') {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $ResetScript -CheckOnly 2>&1 | ForEach-Object { Write-Host "$_" }
    }
    Exit-Script 0
}

# Changing the localhost mapping needs administrator rights: relaunch elevated with the same options.
if (-not (Test-IsAdmin)) {
    $forward = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"{0}"' -f $PSCommandPath), '-Pause',
                 '-ComposeFile', ('"{0}"' -f $ComposeFile),
                 '-DockerTimeoutMinutes', "$DockerTimeoutMinutes", '-AcaTimeoutMinutes', "$AcaTimeoutMinutes")
    if ($Adapter)        { $forward += '-Adapter'; $forward += ('"{0}"' -f $Adapter) }
    if ($Recreate)       { $forward += '-Recreate' }
    if ($Force)          { $forward += '-Force' }
    if ($SkipCableCheck) { $forward += '-SkipCableCheck' }
    if ($SkipAcaSetup)   { $forward += '-SkipAcaSetup' }
    if ($SkipCertTrust)  { $forward += '-SkipCertTrust' }
    if ($NoBrowser)      { $forward += '-NoBrowser' }
    try {
        Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $forward -ErrorAction Stop
    }
    catch {
        Write-Fail 'Administrator rights are needed and were not granted. Nothing was changed.'
        Exit-Script 1
    }
    exit 0
}

try {
    $logDir = Join-Path $PSScriptRoot 'logs'
    if (-not (Test-Path -LiteralPath $logDir)) { New-Item -ItemType Directory -Path $logDir -ErrorAction Stop | Out-Null }
    Start-Transcript -Path (Join-Path $logDir ('start_hat_{0}.log' -f (Get-Date -Format 'yyyyMMdd_HHmmss'))) -ErrorAction Stop | Out-Null
    $script:TranscriptOn = $true
}
catch { Write-Note "Could not start a log file ($($_.Exception.Message)). Continuing without one." }

Write-Host 'Starting the HIRS Acceptance Test' -ForegroundColor White

# --- 1. Docker ---------------------------------------------------------------------------------
Write-Step 'Docker'
if (-not (Wait-DockerEngine)) {
    if (-not $script:DockerHelpShown) {
        Write-Fail "Docker did not become ready within $DockerTimeoutMinutes minutes."
        Write-Line 'Open Docker Desktop, wait until it says it is running, then run this again.'
    }
    Exit-Script 1
}
Write-Ok 'Docker is running with Windows containers.'

# --- 2. Adapter and Ethernet link --------------------------------------------------------------
Write-Step 'Ethernet cable'
$script:AmbiguousAdapters = @()
$cable = Wait-Cable
if (-not $cable.Ok) { Exit-Script 1 }
if ($cable.Name -and -not $SkipCableCheck) { Write-Ok "Ethernet link is up on '$($cable.Name)'." }
elseif ($SkipCableCheck)                   { Write-Note 'Cable check skipped (-SkipCableCheck).' }
elseif (@($script:AmbiguousAdapters).Count -gt 1) {
    Write-Note ("Several wired adapters have a link ({0}); cannot tell which one goes to the test devices." -f ($script:AmbiguousAdapters -join ', '))
}
if ($cable.Waited) { Start-Sleep -Seconds 3 }   # let the container network notice the link

# The HAT network is created on one specific adapter. Settle that before anything is removed or created.
function Stop-IfAdapterUnknown {
    if ($cable.Name) { return }
    Write-Fail 'The adapter for the HAT network must be named, because this PC does not have exactly one usable wired adapter.'
    Get-WiredAdapters | ForEach-Object { Write-Line ("  {0,-20} {1}" -f $_.Name, $_.Status) }
    Write-Line 'Run this script again with  -Adapter "<name of the adapter that goes to the test devices>".'
    Write-Line 'Nothing was changed.'
    Exit-Script 1
}

# --- 3. Containers -----------------------------------------------------------------------------
Write-Step 'Containers'
$acaBefore = Get-ContainerStatus $AcaContainer
$hatBefore = Get-ContainerStatus $HatContainer

if ($Recreate) {
    if (-not (Test-Path -LiteralPath $ComposeFile)) {
        Write-Fail "Compose file not found: $ComposeFile"
        Exit-Script 1
    }
    Stop-IfAdapterUnknown
    if (-not $Force) {
        Write-Host ''
        Write-Host '    -Recreate removes both containers. The ACA database (trust certificates, policy,' -ForegroundColor Yellow
        Write-Host '    validation reports) is erased and cannot be recovered.' -ForegroundColor Yellow
        if ((Read-Host '    Type y to continue') -ne 'y') {
            Write-Line 'Nothing was changed.'
            Exit-Script 0
        }
    }
    Write-Line 'Removing the containers and the HAT network...'
    & docker compose -p $ComposeProject -f $ComposeFile down 2>&1 | ForEach-Object { Write-Line "  $_" }
    # In case they were created outside this compose project:
    foreach ($name in $AcaContainer, $HatContainer) {
        if (Get-ContainerStatus $name) { Invoke-Native 'docker' @('rm', '-f', $name) | Out-Null }
    }
    if (Test-HatNetworkExists) { Invoke-Native 'docker' @('network', 'rm', $HatNetwork) | Out-Null }
    $acaBefore = $null
    $hatBefore = $null
}

$acaCreated = $false
$startedHat = $false
if ($acaBefore -and $hatBefore) {
    foreach ($name in $AcaContainer, $HatContainer) {
        $status = Get-ContainerStatus $name
        if ($status -eq 'running')    { Write-Ok "Container '$name' is already running."; continue }
        if ($status -eq 'restarting') { Write-Line "Container '$name' is being restarted by Docker."; continue }
        $r = Invoke-Native 'docker' @('start', $name)
        if ($name -eq $HatContainer) { $startedHat = $true }
        if ($r.Code -eq 0) { Write-Ok "Container '$name' started." }
        elseif ($name -eq $HatContainer) { Write-Note "Container '$name' did not start on the first try; retrying below." }
        else {
            Write-Fail "Container '$name' could not be started:"
            foreach ($line in $r.Out) { Write-Line "  $line" }
            Write-Line 'If the message mentions the network, the HAT network is damaged: run this script with -Recreate'
            Write-Line '(that erases the ACA database; the policy and trust certificates are then loaded again).'
            Exit-Script 1
        }
    }
}
else {
    if (-not (Test-Path -LiteralPath $ComposeFile)) {
        Write-Fail "A container has to be created, but the compose file was not found: $ComposeFile"
        Exit-Script 1
    }
    # The HAT network is created on one specific adapter; never let that be a guess.
    if (-not (Test-HatNetworkExists)) {
        Stop-IfAdapterUnknown
        $env:HAT_ADAPTER = $cable.Name
        Write-Line "The HAT network will use adapter '$($cable.Name)'."
    }
    Write-Line 'Creating the containers (the first time this downloads the images, which can take a while)...'
    & docker compose -p $ComposeProject -f $ComposeFile up --detach --no-recreate 2>&1 | ForEach-Object { Write-Line "  $_" }
    if ($LASTEXITCODE -ne 0) {
        Write-Fail "docker compose up failed (exit code $LASTEXITCODE). See the messages above."
        Write-Line 'If it says a container name is already in use, or the network cannot be created, run this'
        Write-Line 'script with -Recreate (that erases the ACA database).'
        Exit-Script 1
    }
    $acaCreated = (-not $acaBefore)
    $startedHat = $true
    Write-Ok 'Containers created.'
}

# --- 4. HAT ------------------------------------------------------------------------------------
Write-Step 'HAT (PXE services)'
$watch = $startedHat
if ($cable.Waited -and $hatBefore -eq 'running' -and (Get-ContainerStatus $HatContainer) -eq 'running') {
    # It was started while the cable was unplugged, so its services may not be listening.
    # No device can be in the middle of a network boot here: the link was down a moment ago.
    Write-Line 'The HAT was started before the cable was connected. Restarting it...'
    Invoke-Native 'docker' @('restart', $HatContainer) | Out-Null
    $watch = $true
}

$hat = Confirm-Hat -Watch $watch
if (-not $hat.Running) {
    Write-Fail 'The HAT container will not stay running, so devices cannot network boot.'
    Write-Line 'Check that the Ethernet cable is connected to a powered-on device, then run this again.'
    Exit-Script 1
}
if ($hat.Confirmed) { Write-Ok 'HAT is running and its PXE services started.' }
else                { Write-Ok 'HAT is running.' }

# --- 5. Portal mapping + 6. ACA ----------------------------------------------------------------
Write-Step 'ACA portal'
try {
    if (-not (Get-NetFirewallRule -DisplayName $FirewallRule -ErrorAction SilentlyContinue)) {
        New-NetFirewallRule -DisplayName $FirewallRule -Direction Inbound  -Action Allow -Protocol TCP -LocalPort $PortalPort -ErrorAction Stop | Out-Null
        New-NetFirewallRule -DisplayName $FirewallRule -Direction Outbound -Action Allow -Protocol TCP -LocalPort $PortalPort -ErrorAction Stop | Out-Null
        Write-Ok "Firewall rule '$FirewallRule' (TCP $PortalPort) added."
    }
}
catch { Write-Note "Could not add the firewall rule '$FirewallRule': $($_.Exception.Message)" }

$mapped = $false
for ($i = 0; $i -lt 30 -and -not $mapped; $i++) {
    if (Get-AcaNatIp) { $mapped = Set-PortalMapping } else { Start-Sleep -Seconds 2 }
}
if (-not $mapped) {
    Write-Note "Could not find the ACA's address on the '$NatNetwork' network, so localhost was not remapped."
}

Write-Line 'Waiting for the ACA portal to answer...'
if (-not (Wait-Aca)) { Exit-Script 1 }
Write-Ok "ACA portal is up: $PortalUrl"
if (-not $SkipCertTrust) { Update-PortalCertificateTrust }

# --- 7. Policy and trust certificates ----------------------------------------------------------
# A new ACA has an empty trust store and the default policy, which validates nothing: devices
# would be given certificates without any supply chain check. Never leave it in that state.
$acaReady = $true
if ($SkipAcaSetup) {
    if ($acaCreated) { Write-Note 'The ACA container is new and -SkipAcaSetup was given: it has no trust certificates and the default policy.' }
}
elseif (-not (Test-Path -LiteralPath $ResetScript)) {
    if ($acaCreated) {
        Write-Fail "reset_hat.ps1 was not found next to this script, so the new ACA has no trust certificates and the default policy."
        $acaReady = $false
    }
    else { Write-Note 'reset_hat.ps1 was not found next to this script, so the policy and trust certificates were not checked.' }
}
elseif ($acaCreated) {
    Write-Step 'Policy and trust certificates (new ACA)'
    Write-Line 'The ACA container is new, so its database is empty. Running reset_hat.ps1...'
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $ResetScript -SkipClear -Force -NoStart 2>&1 | ForEach-Object { Write-Host "  $_" }
    if ($LASTEXITCODE -ne 0) {
        Write-Fail 'The policy and the trust certificates were NOT loaded into the new ACA.'
        $acaReady = $false
    }
}
else {
    Write-Step 'Policy and trust certificates'
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $ResetScript -CheckOnly 2>&1 | ForEach-Object { Write-Host "$_" }
    if ($LASTEXITCODE -eq 2)     { $acaReady = $false }      # checked, and not fit for testing
    elseif ($LASTEXITCODE -ne 0) { Write-Note 'The policy and the trust certificates could not be checked.' }
}

# --- Finish ------------------------------------------------------------------------------------
Show-ComposeDrift

if ($acaCreated) {
    try {
        if (-not (Test-Path -LiteralPath (Join-Path ([Environment]::GetFolderPath('Desktop')) $ShortcutName))) {
            Write-Ok "Desktop shortcut created: $(New-HatShortcut)"
        }
    }
    catch { Write-Note "Could not create the desktop shortcut: $($_.Exception.Message)" }
}

if (-not $NoBrowser) {
    # Through explorer.exe so the browser is not started with administrator rights.
    Start-Process -FilePath 'explorer.exe' -ArgumentList ('"{0}"' -f $PortalUrl)
}

Write-Host ''
if (-not $acaReady) {
    Write-Host 'The containers are running, but the ACA is NOT ready for testing devices.' -ForegroundColor Red
    Write-Host 'Fix the problem shown above, then run reset_hat.ps1.' -ForegroundColor Red
    Exit-Script 1
}
Write-Host 'The HIRS Acceptance Test is ready. Devices can be network booted now.' -ForegroundColor Green
Exit-Script 0
