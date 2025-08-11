Function check_for_container () {
    param (
        [switch]$p, [switch]$PRINT_STATUS = $false
    )
	$PRINT_STATUS = $p -or $PRINT_STATUS;
	
	if((Get-ItemProperty -path HKLM:System\CurrentControlSet\Control).ContainerType) {
		$global:DOCKER_CONTAINER=$true
		if($PRINT_STATUS){
          Write-Output "This is running in a Docker container" | WriteAndLog
		}
	} else {
		$global:DOCKER_CONTAINER=$false
		if($PRINT_STATUS) {
          Write-Output "This is not running in a Docker container" | WriteAndLog
		}
	}
}

Function check_mariadb_install () {
    param (
        [switch]$p, [switch]$PRINT_STATUS = $false
    )
	$PRINT_STATUS = $p -or $PRINT_STATUS;
	
    if (Get-Command "mysql.exe" -ErrorAction SilentlyContinue) {
		$global:MYSQL_INSTALLED=$true
		if ($PRINT_STATUS) {
		    Write-Output "mysql is installed" | WriteAndLog
		}
	} else {
	    $global:MYSQL_INSTALLED=$false
		if ($PRINT_STATUS) {
            Write-Output "mysql has NOT been installed, aborting install" | WriteAndLog
		}
        exit 1;
	}
}

Function start_mysqlsd () {
    param (
        [switch]$p, [switch]$PRINT_STATUS = $false
    )
	$PRINT_STATUS = $p -or $PRINT_STATUS

	$DB_STATUS=(check_mysql $PRINT_STATUS)
	
	$service=(Get-Service MariaDB -ErrorAction SilentlyContinue)
	
	# Check if mysql is already running, if not initialize
	if(!$DB_STATUS -or ($DB_STATUS -and $DB_STATUS.HasExited)) {
		if ($PRINT_STATUS) {
			Write-Output "Running the mariadb db installer..." | WriteAndLog
		}
		& mariadb-install-db.exe 2>&1 | WriteAndLog
		if ($PRINT_STATUS) {
			Write-Output "Attempting to start mysql..." | WriteAndLog
		}
		if($service -and ($service.Status -eq [System.ServiceProcess.ServiceControllerStatus]::Stopped)) {
			$service.Start() 2>&1 | WriteAndLog
		} else {
			Start-Process mysqld.exe -WindowStyle Hidden 2>&1 | WriteAndLog
		}
		$DB_STATUS=(check_mysql $PRINT_STATUS)
	}
}

Function check_mysql () {
    param (
        [switch]$p, [switch]$PRINT_STATUS = $false
    )
	$PRINT_STATUS = $p -or $PRINT_STATUS;
	
	$DB_STATUS=(Get-Process -Name mysqld -ErrorAction SilentlyContinue)
	if ($PRINT_STATUS) {
		("The DB {0} running." -f ('is not', 'is')[$DB_STATUS]) | WriteAndLog
	}
	return $DB_STATUS;
}

# Removed check_mysql_root: Looked redundant to db_create:check_mysql_root_pwd

Function check_db_cleared () {
	mysql -u root -e 'quit' &> $null
	if ($LastExitCode -eq 0) {
        Write-Output "  Empty root password verified" | WriteAndLog
    } else {
        Write-Output "  Mysql Root password is not empty" | WriteAndLog
    }
	
    $HIRS_DB_USER_EXISTS=(mysql -uroot -sse "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = 'hirs_db')")
    if ($HIRS_DB_USER_EXISTS -eq 1) {
        Write-Output "  hirs_db user exists" | WriteAndLog
    } else {
        Write-Output "  hirs_db user does not exist" | WriteAndLog
    }
}

Function mysqld_reboot () {
    param (
        [switch]$p, [switch]$PRINT_STATUS = $false
    )
	$PRINT_STATUS = $p -or $PRINT_STATUS;
    if ($PRINT_STATUS) {
		Write-Output "Attempting to restart mysql..." | WriteAndLog
	}
	if($service) {
		$service.Stop() 2>&1 >> "$global:LOG_FILE"
		$service.Start() 2>&1 >> "$global:LOG_FILE"
	} else {
		Start-Process mysqld.exe -WindowStyle Hidden 2>&1 | WriteAndLog
	}
}
