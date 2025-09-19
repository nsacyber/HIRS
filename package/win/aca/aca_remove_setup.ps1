#This script will check remove all the directories/files/certificates and other miscelleaneous items that were created using the win setup script in a Windows environment

if(!(New-Object Security.Principal.WindowsPrincipal(
		[Security.Principal.WindowsIdentity]::GetCurrent())
	).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
	Write-Host "This script requires root.  Please run as root" 
	exit 1
}

Write-Host "----------------------------------------------------------------------"
Write-Host ""
Write-Host "Running the aca removal script ... "

$ACA_SCRIPTS_HOME=(Split-Path -parent $PSCommandPath)
$ACA_COMMON_SCRIPT=(Join-Path $ACA_SCRIPTS_HOME 'aca_common.ps1')

# load other scripts
. $ACA_COMMON_SCRIPT
. $global:HIRS_REL_WIN_DB_MYSQL_UTIL

Write-Host "Checking if the [$global:HIRS_DATA_DIR] directory exists"

if(-not (Test-Path -Path $global:HIRS_DATA_DIR)){
	Write-Host "$global:HIRS_DATA_DIR does not exist. Aborting removal"
	exit 1
} else{
	Write-Host "Directory [$global:HIRS_DATA_DIR] exists"
}

Function retrieve_mysql_root_pwd () {
	if(-not (Test-Path $global:HIRS_DATA_ACA_PROPERTIES_FILE)){
        Write-Host "The ACA property files does not exist. Aborting removal."
        exit 1
    }

    # Convert the contents of the aca properties file into a hash table
    $aca_prop_table = Get-Content -Path $global:HIRS_DATA_ACA_PROPERTIES_FILE -Raw | ConvertFrom-StringData

	if(-not $aca_prop_table){
		Write-Host "Unable to create a hash table using the provided aca properties file. Aborting removal."
		exit 1
	}

	if($aca_prop_table.ContainsKey("mysql_admin_password") -and $aca_prop_table["mysql_admin_password"]){
		return $aca_prop_table["mysql_admin_password"]
	}
}

# check if mariadb is installed
Write-Host "Checking if mariadb is installed"
check_mariadb_install

# retrieve the mysql admin password
$DB_ADMIN_PWD = retrieve_mysql_root_pwd

if(-not $DB_ADMIN_PWD){
	Write-Host "DB admin password could not be found. Aborting removal."
	exit 1
} else{
	Write-Host "DB admin password has been found."
}

# remove the hrs-db and hirs_db user (execute the .\db_drop.ps1 script)
Write-Host "Calling the db_drop.ps1 script to drop the hirs_db"
. $global:HIRS_REL_WIN_DB_DROP -DB_ADMIN_PWD:"$DB_ADMIN_PWD"

# remove the entire hirs directory which contains the rsa/ecc certificates, the logs and the war file
Write-Host "Removing the [$global:HIRS_DATA_DIR] directory"
Remove-Item -Path $global:HIRS_DATA_DIR -Recurse -Force

Write-Host "ACA setup removal complete"

Write-Host "----------------------------------------------------------------------"
Write-Host ""