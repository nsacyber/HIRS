#This script will check remove all the directories/files/certificates and other miscelleaneous items that were created using the win setup script in a Windows environment

if(!(New-Object Security.Principal.WindowsPrincipal(
		[Security.Principal.WindowsIdentity]::GetCurrent())
	).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
	Write-Host "This script requires root.  Please run as root" 
	exit 1
}

Write-Host "Running the aca removal script ... "

# Import the db scripts from the win directories
$APP_HOME=(Split-Path -parent $PSCommandPath)
$MYSQL_WIN_DB_DROP_SCRIPT = (Resolve-Path ([System.IO.Path]::Combine($APP_HOME, '..', 'db', 'db_drop.ps1'))).Path
$MYSQL_WIN_UTIL_SCRIPT = (Resolve-Path ([System.IO.Path]::Combine($APP_HOME, '..','db','mysql_util.ps1'))).Path

. $MYSQL_WIN_UTIL_SCRIPT

$WIN_HIRS_HOME = "C:\ProgramData\hirs\"

Write-Host "Checking if the [C:\ProgramData\hirs\] directory exists"

if(-not (Test-Path -Path $WIN_HIRS_HOME)){
	Write-Host "C:\ProgramData\hirs\ does not exist. Aborting removal"
	exit 1
} else{
	Write-Host "Directory [$WIN_HIRS_HOME} exists"
}

Function retrieve_mysql_root_pwd (){
	$ACA_PROPERTIES_PATH = "C:\ProgramData\hirs\aca\aca.properties"

	if(-not (Test-Path $ACA_PROPERTIES_PATH)){
        Write-Host "The ACA property files does not exist. Aborting removal."
        exit 1
    }

    # Convert the contents of the aca properties file into a hash table
    $aca_prop_table = Get-Content -Path $ACA_PROPERTIES_PATH -Raw | ConvertFrom-StringData

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
. $MYSQL_WIN_DB_DROP_SCRIPT -DB_ADMIN_PWD:"$DB_ADMIN_PWD"

# remove the entire hirs directory which contains the rsa/ecc certificates, the logs and the war file
Write-Host "Removing the [C:\ProgramData\hirs\] directory"
Remove-Item -Path $WIN_HIRS_HOME -Recurse -Force

Write-Host "ACA setup removal complete"