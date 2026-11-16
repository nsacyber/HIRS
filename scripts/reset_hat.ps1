<#
.SYNOPSIS
    Resets the HIRS Acceptance Test ACA to a known state: clears old test data, sets the test
    policy, and loads the trust certificates. Every step is checked, and the script ends with a
    non-zero exit code and a red message if any of them did not take effect.

.DESCRIPTION
    1. Runs start_hat.ps1 so the containers are up and https://localhost:8443 reaches the ACA.
    2. Clears the ACA database: every table except the policy (and the Appraiser table). The
       table list is read from the database, so it follows the ACA version that is running.
    3. Sets the policy listed in $PolicySettings below, then reads it back to confirm it.
       Column names changed between ACA versions; the names are matched against the running
       database, and if a setting cannot be found NOTHING is changed and the script fails,
       instead of leaving the ACA on its default policy (all validation off) without telling you.
    4. Uploads the trust certificates, then counts the trust certificates the ACA holds and
       compares that with what was sent.

    Where the trust certificates come from (first one that exists):
      -CertFolder                               if given
      oem_certs\upload   next to this script    a folder you maintain yourself
      oem_certs.zip      next to this script,   its "upload" folder is used, unpacked to a
                         or in ..\.ci\setup\certs (HIRS repository layout)   temporary folder for this run

    start_hat.ps1 runs this script by itself when it has created a new ACA container, and runs
    it with -CheckOnly on every other start.

    Works from the folder that holds start_hat.ps1, or from the folder above a "hirs" folder.
    Every run is logged to the "logs" folder next to start_hat.ps1.

.PARAMETER SkipClear
    Do not clear the database; only set the policy and load the trust certificates. Used right
    after a new ACA container has been created.

.PARAMETER Force
    Do not ask for confirmation before clearing the database.

.PARAMETER CertFolder
    Folder with the trust certificates to upload. Default: see the description.

.PARAMETER CheckOnly
    Change nothing. Report how many trust certificates the ACA holds and how its policy compares
    with $PolicySettings. Does not need administrator rights.
    Exit code 0: fit for testing. 2: not fit (no trust certificates, or endorsement and platform
    validation both off). 1: could not be checked.

.PARAMETER NoStart
    Skip step 1. Used by start_hat.ps1, which has just done it.

.PARAMETER Pause
    Keep the window open at the end. Set automatically after elevation.

.EXAMPLE
    .\reset_hat.ps1

.EXAMPLE
    .\reset_hat.ps1 -CheckOnly
#>
[CmdletBinding()]
param(
    [switch] $SkipClear,
    [switch] $Force,
    [string] $CertFolder,
    [switch] $CheckOnly,
    [switch] $NoStart,
    [switch] $Pause
)

$ErrorActionPreference = 'Continue'

# ---------------------------------------------------------------------------------------------
# The policy this script sets. Change values here, nowhere else.
#   Names: the column name in the ACA database. Where an ACA version renamed a column, list the
#          names newest first; the first one that exists in the running ACA is used.
#   Value: 1 = on, 0 = off.
# Settings not listed here are left as they are.
# ---------------------------------------------------------------------------------------------
$PolicySettings = @(
    @{ Names = @('ecValidationEnabled');                                               Value = 1 },   # Endorsement credential validation
    @{ Names = @('pcValidationEnabled');                                               Value = 1 },   # Platform credential validation
    @{ Names = @('pcAttributeValidationEnabled');                                      Value = 1 },   # Platform attribute validation
    @{ Names = @('firmwareValidationEnabled');                                         Value = 0 },   # Firmware validation
    @{ Names = @('ignoreImaEnabled');                                                  Value = 1 },
    @{ Names = @('issueAttestationCertificateEnabled', 'issueAttestationCertificate'); Value = 1 },
    @{ Names = @('issueDevIdCertificateEnabled', 'issueDevIdCertificate');             Value = 1 },
    @{ Names = @('ignoretBootEnabled');                                                Value = 0 },
    @{ Names = @('ignoreGptEnabled');                                                  Value = 0 },
    @{ Names = @('ignoreOsEvtEnabled');                                                Value = 1 }
)

# Tables that are never cleared.
$KeepTables = @('PolicySettings', 'Appraiser')

$AcaContainer   = 'aca'
$DbUser         = 'hirs_db'
$DbName         = 'hirs_db'
$PropertiesFile = 'C:\ProgramData\hirs\aca\aca.properties'
$PortalPort     = 8443
$UploadUrl      = "https://127.0.0.1:$PortalPort/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain/upload"
$TrustCertType  = 'CertificateAuthorityCredential'

$script:TranscriptOn = $false
$script:DbPassword   = $null
$script:TempCerts    = $null
$script:Failures     = New-Object System.Collections.ArrayList

# ---------------------------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------------------------
function Write-Step { param([string] $Text) Write-Host ''; Write-Host "==> $Text" -ForegroundColor Cyan }
function Write-Ok   { param([string] $Text) Write-Host "    [ OK ] $Text" -ForegroundColor Green }
function Write-Note { param([string] $Text) Write-Host "    [NOTE] $Text" -ForegroundColor Yellow }
function Write-Line { param([string] $Text) Write-Host "    $Text" }
function Write-Fail {
    param([string] $Text)
    Write-Host "    [FAIL] $Text" -ForegroundColor Red
    [void] $script:Failures.Add($Text)
}

function Exit-Script {
    param([int] $Code)
    if ($script:TempCerts -and (Test-Path -LiteralPath $script:TempCerts)) {
        Remove-Item -LiteralPath $script:TempCerts -Recurse -Force -ErrorAction SilentlyContinue
    }
    if ($script:TranscriptOn) { try { Stop-Transcript | Out-Null } catch { } }
    if ($Pause) { Write-Host ''; Read-Host 'Press Enter to close this window' | Out-Null }
    exit $Code
}

# Ends the run with the list of everything that went wrong.
function Stop-WithFailures {
    Write-Host ''
    Write-Host 'THE RESET DID NOT COMPLETE. Do not test devices until this is fixed:' -ForegroundColor Red
    foreach ($f in $script:Failures) { Write-Host "    - $f" -ForegroundColor Red }
    Exit-Script 1
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

# The HAT folder: where start_hat.ps1 lives. This script may sit in it, or one level above a "hirs" folder.
function Resolve-HatFolder {
    foreach ($dir in @($PSScriptRoot, (Join-Path $PSScriptRoot 'hirs'))) {
        if (Test-Path -LiteralPath (Join-Path $dir 'start_hat.ps1')) { return $dir }
    }
    return $null
}

# Works out which folder of certificate files to upload. Returns Folder and Source (text for the
# operator), or $null. A zip is unpacked to a temporary folder that Exit-Script removes.
function Resolve-CertSource {
    param([string] $HatFolder)

    if ($CertFolder) {
        if (Test-Path -LiteralPath $CertFolder -PathType Container) {
            return [pscustomobject]@{ Folder = (Resolve-Path -LiteralPath $CertFolder).Path; Source = "the folder given with -CertFolder" }
        }
        return $null
    }

    $zip = $null
    foreach ($candidate in @((Join-Path $HatFolder 'oem_certs.zip'), (Join-Path $HatFolder '..\.ci\setup\certs\oem_certs.zip'))) {
        if (Test-Path -LiteralPath $candidate) { $zip = (Resolve-Path -LiteralPath $candidate).Path; break }
    }

    $folder = Join-Path $HatFolder 'oem_certs\upload'
    if ((Test-Path -LiteralPath $folder) -and @(Get-ChildItem -LiteralPath $folder -File).Count -gt 0) {
        if ($zip) {
            # The folder wins, so say so when the zip holds certificates the folder does not.
            try {
                Add-Type -AssemblyName System.IO.Compression.FileSystem
                $archive = [IO.Compression.ZipFile]::OpenRead($zip)
                try { $inZip = @($archive.Entries | Where-Object { $_.FullName -like 'upload/*' -and $_.Name } | ForEach-Object { $_.Name }) }
                finally { $archive.Dispose() }
                $inFolder  = @(Get-ChildItem -LiteralPath $folder -File | ForEach-Object { $_.Name })
                $onlyInZip = @($inZip | Where-Object { $inFolder -notcontains $_ })
                if ($onlyInZip.Count -gt 0) {
                    Write-Note ("oem_certs.zip has {0} certificate file(s) that the oem_certs\upload folder lacks. The folder is what gets uploaded, so these are NOT loaded: {1}" -f $onlyInZip.Count, ($onlyInZip -join ', '))
                }
            }
            catch { }
        }
        return [pscustomobject]@{ Folder = $folder; Source = 'the oem_certs\upload folder' }
    }

    if ($zip) {
        try {
            $script:TempCerts = Join-Path $env:TEMP ('hat_certs_{0}' -f ([guid]::NewGuid().ToString('N')))
            Expand-Archive -LiteralPath $zip -DestinationPath $script:TempCerts -Force -ErrorAction Stop
            $unpacked = Join-Path $script:TempCerts 'upload'
            if (Test-Path -LiteralPath $unpacked) { return [pscustomobject]@{ Folder = $unpacked; Source = $zip } }
            Write-Note "$zip has no 'upload' folder inside."
        }
        catch { Write-Note "Could not unpack ${zip}: $($_.Exception.Message)" }
    }
    return $null
}

# Reads the database password from the ACA's properties file inside the container.
function Get-DbPassword {
    $attempts = @(
        @('cmd', '/c', 'type', $PropertiesFile),
        @('pwsh', '-Command', "Get-Content $PropertiesFile"),
        @('powershell', '-Command', "Get-Content $PropertiesFile")
    )
    foreach ($command in $attempts) {
        $r = Invoke-Native 'docker' (@('exec', $AcaContainer) + $command)
        if ($r.Code -ne 0) { continue }
        foreach ($line in $r.Out) {
            if ($line -match '^\s*hirs_db_password\s*=\s*(.+?)\s*$') { return $Matches[1] }
        }
    }
    return $null
}

# Runs SQL in the ACA's database. Returns the exit code and the output lines (no header row,
# tab-separated columns). SQL text must not contain double quotes.
function Invoke-AcaSql {
    param([string] $Sql)
    $r = Invoke-Native 'docker' @('exec', $AcaContainer, 'mysql', '-u', $DbUser, '-D', $DbName,
        "--password=$($script:DbPassword)", '-N', '-B', '-e', $Sql)
    $lines = @($r.Out | Where-Object { $_ -and $_ -notmatch 'Using a password on the command line' })
    return [pscustomobject]@{ Code = $r.Code; Out = $lines }
}

# Single integer result, or $null when the query failed.
function Get-AcaSqlNumber {
    param([string] $Sql)
    $r = Invoke-AcaSql $Sql
    if ($r.Code -ne 0 -or $r.Out.Count -eq 0) { return $null }
    $n = 0
    if ([int]::TryParse(($r.Out | Select-Object -Last 1).Trim(), [ref] $n)) { return $n }
    return $null
}

# Finds the real name of a table or column, whatever its letter case.
function Find-Name {
    param([string[]] $Available, [string] $Wanted)
    foreach ($name in $Available) { if ($name -ieq $Wanted) { return $name } }
    return $null
}

# -CheckOnly: read-only report on whether the ACA is fit for testing. Ends the script.
function Invoke-CheckOnly {
    $script:DbPassword = Get-DbPassword
    if (-not $script:DbPassword) {
        Write-Note "Could not read the database password from the '$AcaContainer' container, so the policy and trust certificates were not checked."
        Exit-Script 1
    }
    $tablesResult = Invoke-AcaSql "select table_name from information_schema.tables where table_schema = database() and table_type = 'BASE TABLE'"
    $tables       = @($tablesResult.Out | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '^[A-Za-z0-9_]+$' })
    $policyTable  = Find-Name $tables 'PolicySettings'
    $certTable    = Find-Name $tables 'Certificate'
    if ($tablesResult.Code -ne 0 -or -not $policyTable -or -not $certTable) {
        Write-Note 'Could not read the ACA database, so the policy and trust certificates were not checked.'
        Exit-Script 1
    }

    $columnsResult = Invoke-AcaSql "select column_name from information_schema.columns where table_schema = database() and table_name = '$policyTable'"
    $columns = @($columnsResult.Out | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '^[A-Za-z0-9_]+$' })
    $plan = @()
    foreach ($setting in $PolicySettings) {
        foreach ($candidate in $setting.Names) {
            $column = Find-Name $columns $candidate
            if ($column) { $plan += [pscustomobject]@{ Column = $column; Value = [string] $setting.Value }; break }
        }
    }
    $values = @()
    if ($plan.Count -gt 0) {
        $check = Invoke-AcaSql ("select {0} from {1}" -f (($plan | ForEach-Object { '{0}+0' -f $_.Column }) -join ', '), $policyTable)
        if ($check.Code -eq 0 -and $check.Out.Count -ge 1) { $values = @(($check.Out | Select-Object -Last 1) -split "`t" | ForEach-Object { $_.Trim() }) }
    }
    $held = Get-AcaSqlNumber "select count(*) from $certTable where DTYPE = '$TrustCertType'"
    if ($held -eq $null -or $values.Count -ne $plan.Count -or $plan.Count -eq 0) {
        Write-Note 'Could not read the policy or count the trust certificates, so they were not checked.'
        Exit-Script 1
    }

    $current = @{}
    for ($i = 0; $i -lt $plan.Count; $i++) { $current[$plan[$i].Column] = $values[$i] }
    $notFit = $false

    if ($held -gt 0) { Write-Ok "The ACA holds $held trust certificates." }
    else {
        Write-Fail 'The ACA holds no trust certificates, so every device would fail validation. Run reset_hat.ps1.'
        $notFit = $true
    }

    $names = @{ ecValidationEnabled = 'endorsement'; pcValidationEnabled = 'platform'; pcAttributeValidationEnabled = 'platform attributes'; firmwareValidationEnabled = 'firmware' }
    $summary = foreach ($key in 'ecValidationEnabled', 'pcValidationEnabled', 'pcAttributeValidationEnabled', 'firmwareValidationEnabled') {
        if ($current.ContainsKey($key)) { '{0} {1}' -f $names[$key], $(if ($current[$key] -eq '1') { 'ON' } else { 'off' }) }
    }
    if ($current['ecValidationEnabled'] -ne '1' -and $current['pcValidationEnabled'] -ne '1') {
        Write-Fail 'Endorsement and platform validation are both off: the ACA would give devices certificates without checking anything. Run reset_hat.ps1.'
        $notFit = $true
    }
    else { Write-Ok ("Validation: {0}." -f ($summary -join ', ')) }

    $different = @($plan | Where-Object { $current[$_.Column] -ne $_.Value } | ForEach-Object { '{0} is {1} (reset_hat.ps1 sets {2})' -f $_.Column, $current[$_.Column], $_.Value })
    if ($different.Count -gt 0 -and -not $notFit) {
        Write-Note ("The policy differs from what reset_hat.ps1 sets, which is fine if it was changed on purpose: {0}." -f ($different -join '; '))
    }

    if ($notFit) { Exit-Script 2 }
    Exit-Script 0
}

# =============================================================================================
# Main
# =============================================================================================
if ($CheckOnly) {
    Invoke-CheckOnly      # ends the script
}

$hatFolder = Resolve-HatFolder
if (-not $hatFolder) {
    Write-Fail "start_hat.ps1 was not found in '$PSScriptRoot' or in its 'hirs' subfolder. Keep this script with the other HAT scripts."
    Exit-Script 1
}
if ($CertFolder -and (Test-Path -LiteralPath $CertFolder)) { $CertFolder = (Resolve-Path -LiteralPath $CertFolder).Path }

if (-not (Test-IsAdmin)) {
    $forward = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"{0}"' -f $PSCommandPath), '-Pause')
    if ($CertFolder) { $forward += '-CertFolder'; $forward += ('"{0}"' -f $CertFolder) }
    if ($SkipClear) { $forward += '-SkipClear' }
    if ($NoStart)   { $forward += '-NoStart' }
    if ($Force)     { $forward += '-Force' }
    try {
        Start-Process -FilePath 'powershell.exe' -Verb RunAs -ArgumentList $forward -ErrorAction Stop
    }
    catch {
        Write-Host '    [FAIL] Administrator rights are needed and were not granted. Nothing was changed.' -ForegroundColor Red
        Exit-Script 1
    }
    exit 0
}

try {
    $logDir = Join-Path $hatFolder 'logs'
    if (-not (Test-Path -LiteralPath $logDir)) { New-Item -ItemType Directory -Path $logDir -ErrorAction Stop | Out-Null }
    Start-Transcript -Path (Join-Path $logDir ('reset_hat_{0}.log' -f (Get-Date -Format 'yyyyMMdd_HHmmss'))) -ErrorAction Stop | Out-Null
    $script:TranscriptOn = $true
}
catch { Write-Note "Could not start a log file ($($_.Exception.Message)). Continuing without one." }

Write-Host 'Resetting the HIRS Acceptance Test (HAT)' -ForegroundColor White

# --- 0. Things that can be checked before touching anything -----------------------------------
$certSource = Resolve-CertSource $hatFolder
$certFiles  = @()
if ($certSource) { $certFiles = @(Get-ChildItem -LiteralPath $certSource.Folder -File | Sort-Object Name) }
if ($certFiles.Count -eq 0) {
    if ($CertFolder) { Write-Fail "No trust certificates found in '$CertFolder'. Nothing was changed." }
    else { Write-Fail "No trust certificates found: there is no oem_certs\upload folder and no oem_certs.zip for '$hatFolder'. Nothing was changed." }
    Stop-WithFailures
}
Write-Line ("Trust certificates: {0} files from {1}" -f $certFiles.Count, $certSource.Source)
if (-not (Get-Command 'curl.exe' -ErrorAction SilentlyContinue)) {
    Write-Fail 'curl.exe was not found (it is part of Windows 10 version 1803 and later). Nothing was changed.'
    Stop-WithFailures
}

# --- 1. Containers up, portal reachable --------------------------------------------------------
if (-not $NoStart) {
    Write-Step 'Starting the Acceptance Test (start_hat.ps1)'
    $startScript = Join-Path $hatFolder 'start_hat.ps1'
    # -SkipAcaSetup: this script is about to do that part itself.
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $startScript -NoBrowser -SkipAcaSetup 2>&1 | ForEach-Object { Write-Host "  $_" }
    if ($LASTEXITCODE -ne 0) {
        Write-Fail "start_hat.ps1 did not finish successfully (exit code $LASTEXITCODE). Nothing was reset."
        Stop-WithFailures
    }
}

# --- 2. Database access ------------------------------------------------------------------------
Write-Step 'ACA database'
$script:DbPassword = Get-DbPassword
if (-not $script:DbPassword) {
    Write-Fail "Could not read the database password from $PropertiesFile inside the '$AcaContainer' container. Nothing was reset."
    Stop-WithFailures
}

$tablesResult = Invoke-AcaSql "select table_name from information_schema.tables where table_schema = database() and table_type = 'BASE TABLE' order by table_name"
$tables = @($tablesResult.Out | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '^[A-Za-z0-9_]+$' })
if ($tablesResult.Code -ne 0 -or $tables.Count -eq 0) {
    Write-Fail 'Could not list the tables in the ACA database. Nothing was reset. The database said:'
    foreach ($line in $tablesResult.Out) { Write-Line "  $line" }
    Stop-WithFailures
}

$policyTable = Find-Name $tables 'PolicySettings'
$certTable   = Find-Name $tables 'Certificate'
if (-not $policyTable) { Write-Fail 'The ACA database has no PolicySettings table. Nothing was reset.' }
if (-not $certTable)   { Write-Fail 'The ACA database has no Certificate table. Nothing was reset.' }
if ($script:Failures.Count -gt 0) { Stop-WithFailures }
Write-Ok ("Connected. The database has {0} tables." -f $tables.Count)

# Work out the policy statement now, so a version mismatch stops the run before anything is cleared.
$columnsResult = Invoke-AcaSql "select column_name from information_schema.columns where table_schema = database() and table_name = '$policyTable'"
$columns = @($columnsResult.Out | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '^[A-Za-z0-9_]+$' })
$policyPlan = @()
foreach ($setting in $PolicySettings) {
    $column = $null
    foreach ($candidate in $setting.Names) {
        $column = Find-Name $columns $candidate
        if ($column) { break }
    }
    if ($column) { $policyPlan += [pscustomobject]@{ Column = $column; Value = [int] $setting.Value } }
    else { Write-Fail ("This ACA version has no policy setting named {0}. Nothing was reset." -f ($setting.Names -join ' or ')) }
}
if ($script:Failures.Count -gt 0) {
    Write-Line 'Policy settings this ACA version does have:'
    Write-Line ('  ' + (($columns | Sort-Object) -join ', '))
    Write-Line 'Correct the $PolicySettings list at the top of this script.'
    Stop-WithFailures
}
# The ACA creates its policy record while it starts; on a brand-new ACA allow it a minute.
$policyRows = $null
for ($try = 0; $try -lt 12; $try++) {
    $policyRows = Get-AcaSqlNumber "select count(*) from $policyTable"
    if ($policyRows -ne 0) { break }
    Start-Sleep -Seconds 5
}
if ($policyRows -ne 1) {
    Write-Fail "Expected exactly one policy record in the ACA, found '$policyRows'. Nothing was reset. (The ACA creates it while starting; wait a minute and run this again.)"
    Stop-WithFailures
}

# --- 3. Clear old data -------------------------------------------------------------------------
Write-Step 'Clearing old test data'
if ($SkipClear) {
    Write-Note 'Skipped (-SkipClear).'
}
else {
    $keep  = @($tables | Where-Object { $t = $_; @($KeepTables | Where-Object { $_ -ieq $t }).Count -gt 0 })
    $clear = @($tables | Where-Object { $keep -notcontains $_ })

    if (-not $Force) {
        Write-Host ''
        Write-Host '    This erases every certificate, device, reference manifest and validation report in the ACA.' -ForegroundColor Yellow
        Write-Host '    It cannot be undone. The policy record is kept and then set to the test policy.' -ForegroundColor Yellow
        if ((Read-Host '    Type y to continue') -ne 'y') {
            Write-Line 'Nothing was changed.'
            Exit-Script 0
        }
    }

    $sql = 'set foreign_key_checks=0; ' + (($clear | ForEach-Object { "truncate table $_;" }) -join ' ') + ' set foreign_key_checks=1;'
    $r = Invoke-AcaSql $sql
    if ($r.Code -ne 0) {
        Write-Fail 'Clearing the database failed. The database said:'
        foreach ($line in $r.Out) { Write-Line "  $line" }
        Stop-WithFailures
    }
    $left = Get-AcaSqlNumber "select count(*) from $certTable"
    if ($left -ne 0) {
        Write-Fail "The certificate table still holds '$left' rows after clearing."
        Stop-WithFailures
    }
    Write-Ok ("Cleared {0} tables. Kept: {1}." -f $clear.Count, ($keep -join ', '))
}

# --- 4. Policy ---------------------------------------------------------------------------------
Write-Step 'Policy'
$assignments = ($policyPlan | ForEach-Object { '{0}={1}' -f $_.Column, $_.Value }) -join ', '
$r = Invoke-AcaSql "update $policyTable set $assignments"
if ($r.Code -ne 0) {
    Write-Fail 'Setting the policy failed. The database said:'
    foreach ($line in $r.Out) { Write-Line "  $line" }
}
else {
    # Read back as numbers (+0), which works whether the ACA stored the flags as bit or as tinyint.
    $selectList = ($policyPlan | ForEach-Object { '{0}+0' -f $_.Column }) -join ', '
    $check  = Invoke-AcaSql "select $selectList from $policyTable"
    $values = @()
    if ($check.Code -eq 0 -and $check.Out.Count -ge 1) { $values = @(($check.Out | Select-Object -Last 1) -split "`t") }
    if ($values.Count -ne $policyPlan.Count) {
        Write-Fail 'The policy was written but could not be read back to confirm it. The database said:'
        foreach ($line in $check.Out) { Write-Line "  $line" }
    }
    else {
        $wrong = 0
        for ($i = 0; $i -lt $policyPlan.Count; $i++) {
            $actual = $values[$i].Trim()
            $state  = if ($policyPlan[$i].Value -eq 1) { 'on ' } else { 'off' }
            if ($actual -eq [string] $policyPlan[$i].Value) { Write-Line ("  {0}  {1}" -f $state, $policyPlan[$i].Column) }
            else { $wrong++; Write-Fail ("Policy setting {0} is '{1}', expected {2}." -f $policyPlan[$i].Column, $actual, $policyPlan[$i].Value) }
        }
        if ($wrong -eq 0) { Write-Ok ("Policy set and confirmed ({0} settings)." -f $policyPlan.Count) }
    }
}
if ($script:Failures.Count -gt 0) { Stop-WithFailures }

# --- 5. Trust certificates ---------------------------------------------------------------------
Write-Step 'Trust certificates'

# What we are about to send, counted as distinct certificates (two files can hold the same one).
$thumbprints = @{}
foreach ($file in $certFiles) {
    try {
        $cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2 $file.FullName
        $thumbprints[$cert.Thumbprint] = $true
    }
    catch { Write-Note "Could not read '$($file.Name)' as a certificate here; it is uploaded anyway." }
}

$heldBefore = Get-AcaSqlNumber "select count(*) from $certTable where DTYPE = '$TrustCertType'"

$sent = 0
foreach ($file in $certFiles) {
    if ($file.Name -match '[;,"]') {
        Write-Fail "'$($file.Name)' was not uploaded: its name contains ; , or a quote, which the upload tool cannot handle. Rename the file."
        continue
    }
    $r = Invoke-Native 'curl.exe' @('-k', '-s', '-o', 'NUL', '-m', '60', '-w', '%{http_code}', '-F', "file=@$($file.FullName)", $UploadUrl)
    $code = 0
    [void][int]::TryParse((($r.Out -join '').Trim()), [ref] $code)
    if ($code -ge 200 -and $code -lt 400) { $sent++ }
    else { Write-Fail "Uploading '$($file.Name)' failed (HTTP code $code)." }
}
Write-Line ("Uploaded {0} of {1} files." -f $sent, $certFiles.Count)

# The upload always answers with a redirect, even when the ACA rejects the file, so the real
# check is what the ACA now holds.
$held = Get-AcaSqlNumber "select count(*) from $certTable where DTYPE = '$TrustCertType'"
if ($held -eq $null) {
    Write-Fail 'Could not count the trust certificates in the ACA after uploading.'
}
elseif ($held -lt $thumbprints.Count) {
    Write-Fail ("The ACA holds {0} trust certificates, but {1} different certificates were sent. Open the Trust Chain page in the portal to see which are missing." -f $held, $thumbprints.Count)
}
else {
    Write-Ok ("The ACA holds {0} trust certificates ({1} different certificates were sent)." -f $held, $thumbprints.Count)
    if ($heldBefore -gt 0) {
        Write-Note ("The ACA already held {0} trust certificates before this upload, so the count cannot prove that every file was accepted. Run without -SkipClear for a full check." -f $heldBefore)
    }
}

# --- Finish ------------------------------------------------------------------------------------
if ($script:Failures.Count -gt 0) { Stop-WithFailures }

Write-Host ''
if ($SkipClear) { Write-Host 'HAT reset complete: policy confirmed, trust certificates loaded.' -ForegroundColor Green }
else            { Write-Host 'HAT reset complete: data cleared, policy confirmed, trust certificates loaded.' -ForegroundColor Green }
Exit-Script 0
