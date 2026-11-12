# Shows what is in the ACA database: every table with its row count, and the policy record.
# READ ONLY. It changes nothing. (The previous version of this file also overwrote the policy.)

$ErrorActionPreference = 'Continue'
$PropertiesFile = 'C:\ProgramData\hirs\aca\aca.properties'

$password = $null
$lines = docker exec aca cmd /c type $PropertiesFile 2>$null
foreach ($line in $lines) {
    if ("$line" -match '^\s*hirs_db_password\s*=\s*(.+?)\s*$') { $password = $Matches[1] }
}
if (-not $password) {
    Write-Host "Could not read the database password from the 'aca' container. Is it running? (docker ps)" -ForegroundColor Red
    exit 1
}

Write-Host 'ACA database tables (name, rows)' -ForegroundColor Cyan
$tables = docker exec aca mysql -u hirs_db -D hirs_db "--password=$password" -N -B -e "select table_name from information_schema.tables where table_schema = database() and table_type = 'BASE TABLE' order by table_name"
foreach ($table in $tables) {
    $name = "$table".Trim()
    if ($name -notmatch '^[A-Za-z0-9_]+$') { continue }
    $count = docker exec aca mysql -u hirs_db -D hirs_db "--password=$password" -N -B -e "select count(*) from $name"
    Write-Host ('  {0,-55} {1}' -f $name, "$count".Trim())
}

Write-Host ''
Write-Host 'ACA policy settings' -ForegroundColor Cyan
# \G prints one setting per line, whatever columns this ACA version has.
docker exec aca mysql -u hirs_db -D hirs_db "--password=$password" -e 'select * from PolicySettings\G'
