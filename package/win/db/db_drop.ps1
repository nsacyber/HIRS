# Helper ps1 script to the aca_remove_setup that drops the hirs_db database 

param(
    [Parameter(Mandatory=$true)]
    [string] $DB_ADMIN_PWD
)

if(!(New-Object Security.Principal.WindowsPrincipal(
		[Security.Principal.WindowsIdentity]::GetCurrent())
	).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
	Write-Host "This script requires root.  Please run as root" 
	exit 1
}

Write-Host "Dropping the hirs_db database"

$ACA_SCRIPTS_HOME = Join-Path -Path (Split-Path -Parent $PSCommandPath) -ChildPath "..\aca"
$ACA_COMMON_SCRIPT=(Join-Path $ACA_SCRIPTS_HOME 'aca_common.ps1')

# load other scripts
. $ACA_COMMON_SCRIPT
. $global:HIRS_REL_WIN_DB_MYSQL_UTIL

# Run multiple flush commands in one go
mysql -u root --password=$DB_ADMIN_PWD -e "FLUSH HOSTS; FLUSH LOGS; FLUSH STATUS; FLUSH PRIVILEGES; FLUSH USER_RESOURCES;"

# Drop the user
mysql -u root --password=$DB_ADMIN_PWD -e "DROP USER 'hirs_db'@'localhost';"

# Drop the database if it exists
mysql -u root --password=$DB_ADMIN_PWD -e "DROP DATABASE IF EXISTS hirs_db;"

# Output confirmation message
Write-Host "hirs_db database and hirs_db user removed"

Write-Host "Resetting mysql root password to empty"

# Set empty password for root@localhost
mysql -u root --password=$DB_ADMIN_PWD -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '';"

# The new password should be an empty string
$DB_ADMIN_PWD = ""

# Flush logs
mysql -u root --password=$DB_ADMIN_PWD -e "FLUSH LOGS;"

# Remove key, cert and truststore entries from my.ini file

Write-Host "Removing hirs tls references from mariadb configuration files"

# Read all lines except those containing "hirs"
$filteredLines = Get-Content $global:DB_CONF | Where-Object { $_ -notmatch "hirs" }

# Overwrite the original file with the filtered content
$filteredLines | Set-Content $global:DB_CONF

Write-Host "Restarting MariaDB"

mysqld_reboot
