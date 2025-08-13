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

		if($service -and ($service.Status -eq 'Stopped')) {
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

	$service=(Get-Service MariaDB -ErrorAction SilentlyContinue)

	# if the mariadb service does exist
	if($service) {
		Write-Output "Stopping MariaDB service..." | WriteAndLog

		if ($service.Status -ne 'Stopped') {
        	$service.Stop() 2>&1 >> "$global:LOG_FILE"
        	$service.WaitForStatus('Stopped', '00:00:05')
    	} else {
          Write-Output "MariaDB service is already stopped." | WriteAndLog
		}

    	Write-Output "Starting MariaDB service..." | WriteAndLog

    	if ($service.Status -eq 'Stopped') {
            $service.Start() 2>&1 >> "$global:LOG_FILE"
       		$service.WaitForStatus('Running', '00:00:05')

        	if ($service.Status -eq 'Running') {
          	  Write-Output "MariaDB service started successfully." | WriteAndLog
       		} else {
            Write-Output "MariaDB failed to start within timeout." | WriteAndLog
        	}	
   	 	} else {
        	Write-Output "MariaDB service was not in a stopped state. Skipping start." | WriteAndLog
    	}
    } else {
		Write-Output "MariaDB service not found. Attempting to run mysqld.exe manually..." | WriteAndLog
		Start-Process mysqld.exe -WindowStyle Hidden 2>&1 | WriteAndLog
	}
	
}

Function check_hirs_db() {
	param(
		[Parameter(Mandatory=$true)]
		[string]$DB_ADMIN_PWD
	)

	if (!$DB_ADMIN_PWD) {
		Write-Output "Exiting script since this function cannot check if the hirs_db exists without supplying the database admin password" | WriteAndLog
		exit 1
	}

	$dbName = "hirs_db"
 	
	# Run the MySQL command to show databases and capture output
	$output = mysql -u root --password=$DB_ADMIN_PWD -e "SHOW DATABASES;" 2>&1

	# Check if output contains the database name
	$HIRS_DB_EXISTS = $output | Where-Object { $_ -match $dbName }
  
    if($HIRS_DB_EXISTS -and $HIRS_DB_EXISTS -eq $dbName){
		Write-Output "hirs_db database exists" | WriteAndLog
		return $true
	}
	
    Write-Output "hirs_db database does not exist" | WriteAndLog
	return $false
}

Function check_hirs_db_user() {
	param(
		[Parameter(Mandatory=$true)]
		[string]$DB_ADMIN_PWD
	)

	if (!$DB_ADMIN_PWD) {
		Write-Output "Exiting script since this function cannot check if the hirs_db user exists without the database admin password" | WriteAndLog
		exit 1
	}

	# Build the query
	$query = "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = 'hirs_db');"

	# Run the mysql command, capturing the output
	$HIRS_DB_USER_EXISTS = mysql -u root --password=$DB_ADMIN_PWD -sse $query 2>&1

	Write-Output $HIRS_DB_USER_EXISTS
  
    if($HIRS_DB_USER_EXISTS -and $HIRS_DB_USER_EXISTS -eq 1){
		Write-Output "hirs_db user exists" | WriteAndLog
		return $true
	}

    Write-Output "hirs_db user does not exist" | WriteAndLog
	return $false
}
