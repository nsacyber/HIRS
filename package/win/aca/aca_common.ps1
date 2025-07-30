# Globally set options
#     HIRS System directories, if installed via MSI
#         C:\Program Files\hirs      # Executables
#             bin
#                 HIRS_AttestationCA_Portal.war
#             scripts
#                 See HIRS Relative directories description below
#     HIRS Data directories, installed by these scripts
#         C:\ProgramData\hirs        # Configuration Files, Logs
#             aca
#             certificates
#                 HIRS
#                     ecc_512_sha384_certs
#                     rsa_3k_sha384_certs
#             json
#             log
#     Other files needed:
#         C:/MariaDB 11.1/data/my.ini
#             If mysql is installed somewhere else, update DB_CONF below.
$global:HIRS_SYS_HOME=(Join-Path $Env:ProgramFiles "hirs")
$global:HIRS_INSTALL_SCRIPTS_DIR=(Join-Path $Env:ProgramFiles "scripts")
$global:HIRS_INSTALL_SCRIPTS_DB_DIR=(Join-Path $Env:ProgramFiles "db")
$global:HIRS_DATA_DIR=(Join-Path $Env:ProgramData "hirs")
$global:HIRS_CONF_DIR=(Join-Path $global:HIRS_DATA_DIR "aca")
$global:HIRS_DATA_ACA_PROPERTIES_FILE=(Join-Path $global:HIRS_CONF_DIR 'aca.properties')
$global:HIRS_DATA_SPRING_PROP_FILE=(Join-Path $global:HIRS_CONF_DIR 'application.win.properties')
$global:HIRS_DATA_CERTIFICATES_DIR=(Join-Path $global:HIRS_DATA_DIR "certificates")
$global:HIRS_DATA_CERTIFICATES_HIRS_DIR=(Join-Path $global:HIRS_DATA_CERTIFICATES_DIR "HIRS")
$global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH=(Join-Path $HIRS_DATA_CERTIFICATES_HIRS_DIR "rsa_3k_sha384_certs")
$global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH=(Join-Path $HIRS_DATA_CERTIFICATES_HIRS_DIR "ecc_512_sha384_certs")
$global:HIRS_DATA_LOG_DIR=(Join-Path $global:HIRS_DATA_DIR "log")
$global:HIRS_DATA_INSTALL_LOG_NAME=(Join-Path $global:HIRS_DATA_LOG_DIR ("hirs_aca_install_"+(Get-Date -Format "yyyy-MM-dd")+'.log'))
$global:HIRS_CONF_DEFAULT_PROPERTIES_DIR=(Join-Path $global:HIRS_CONF_DIR "default-properties")
#         Db Configuration files
$global:DB_CONF=(Join-Path $Env:ProgramFiles 'MariaDB 11.1' 'data' 'my.ini')
#         Default Server Side Certificates
$global:SSL_DB_SRV_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_rsa_3k_sha384_Cert_Chain.pem')
$global:SSL_DB_SRV_CERT=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_srv_rsa_3k_sha384.pem')
$global:SSL_DB_SRV_KEY=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_srv_rsa_3k_sha384.key')
#         Default Client Side Certificates
$global:SSL_DB_CLIENT_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_rsa_3k_sha384_Cert_Chain.pem')
$global:SSL_DB_CLIENT_CERT=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_client_rsa_3k_sha384.pem')
$global:SSL_DB_CLIENT_KEY=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_client_rsa_3k_sha384.key')
#     HIRS Relative directories assumed structure
#         package
#           linux
#             aca
#             db
#               db_create.sql
#               secure_mysql.sql
#             pki
#               ca.conf
#           win
#             aca
#               aca_bootRun.ps1
#               aca_common.ps1         # This script. You are here.
#               aca_setup.ps1
#               aca_win_config.ps1
#             db
#               db_create.ps1
#               mysql_util.ps1
#             pki
#               pki_chain_gen.ps1
#               pki_setup.ps1
$global:HIRS_REL_WIN_ACA_HOME=(Split-Path -parent $PSCommandPath)
$global:HIRS_REL_WIN_HOME=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME ..)
$global:HIRS_REL_PACKAGE_HOME=(Join-Path -Resolve $global:HIRS_REL_WIN_HOME ..)
$global:HIRS_REL_SCRIPTS_HOME=(Join-Path -Resolve $global:HIRS_REL_PACKAGE_HOME 'linux')
$global:HIRS_REL_SCRIPTS_ACA_HOME=(Join-Path -Resolve $global:HIRS_REL_SCRIPTS_HOME 'aca')
$global:HIRS_REL_SCRIPTS_DB_HOME=(Join-Path -Resolve $global:HIRS_REL_SCRIPTS_HOME 'db')
$global:HIRS_REL_SCRIPTS_DB_CREATE_SQL=(Join-Path -Resolve $global:HIRS_REL_SCRIPTS_DB_HOME 'db_create.sql')
$global:HIRS_REL_SCRIPTS_DB_SECURE_MYSQL_SQL=(Join-Path -Resolve $global:HIRS_REL_SCRIPTS_DB_HOME 'secure_mysql.sql')
$global:HIRS_REL_SCRIPTS_PKI_HOME=(Join-Path -Resolve $global:HIRS_REL_SCRIPTS_HOME 'pki')
$global:HIRS_REL_SCRIPTS_PKI_CA_CONF=(Join-Path -Resolve $global:HIRS_REL_SCRIPTS_PKI_HOME 'ca.conf')
$global:HIRS_REL_WIN_ACA_BOOTRUN=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME 'aca_bootRun.ps1')
$global:HIRS_REL_WIN_ACA_COMMON=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME 'aca_common.ps1')
$global:HIRS_REL_WIN_ACA_SETUP=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME 'aca_setup.ps1')
$global:HIRS_REL_WIN_ACA_SETUP=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME 'aca_win_config.ps1')
$global:HIRS_REL_WIN_DB_HOME=(Join-Path -Resolve $global:HIRS_REL_WIN_HOME 'db')
$global:HIRS_REL_WIN_DB_CREATE=(Join-Path -Resolve $global:HIRS_REL_WIN_DB_HOME 'db_create.ps1')
$global:HIRS_REL_WIN_DB_MYSQL_UTIL=(Join-Path -Resolve $global:HIRS_REL_WIN_DB_HOME 'mysql_util.ps1')
$global:HIRS_REL_WIN_PKI_HOME=(Join-Path -Resolve $global:HIRS_REL_WIN_HOME 'pki')
$global:HIRS_REL_WIN_PKI_CHAIN_GEN=(Join-Path -Resolve $global:HIRS_REL_WIN_PKI_HOME 'pki_chain_gen.ps1')
$global:HIRS_REL_WIN_PKI_SETUP=(Join-Path -Resolve $global:HIRS_REL_WIN_PKI_HOME 'pki_setup.ps1')

#    Saved values
# $Env:HIRS_MYSQL_ROOT_PWD
# $Env:HIRS_PKI_PWD
$global:ACA_PROPERTIES=$null
$global:SPRING_PROPERTIES=$null

# Common utility functions
Function read_aca_properties () {
    # This converts the ACA properties file into a hash table
    # Values are accessed by key like this: $propertyValue=$global:ACA_PROPERTIES.'example.property.key'
    param (
        [string]$file = $null
    )
    if (!$global:ACA_PROPERTIES -and $file -and [System.IO.File]::Exists($file)) {
        $file_content=(Get-Content $file -Raw)
        if ($file_content) { # File is not empty
            # $file_content=([Regex]::Escape($file_content) -replace "(\\r)?\\n",[Environment]::NewLine)
            # $global:ACA_PROPERTIES=(ConvertFrom-StringData($file_content))
            $global:ACA_PROPERTIES=(Get-Content -Path $file -Raw | ConvertFrom-StringData)
        } else { # File is empty
            # Initialize empty hash table
            $global:ACA_PROPERTIES=@{}
        }
    } elseif ($file -and ![System.IO.File]::Exists($file)) {
        $msg="Warning: ACA properties file not found. The path provided was: $file"
        if ($global:LOG_FILE) {
            Write-Output "$msg" | WriteAndLog
        } else {
            Write-Host "$msg"
        }
    }
}

Function add_new_aca_property () {
    param (
        [string]$file = $null,
        [string]$newKeyAndValue = $null
    )
    if ($global:ACA_PROPERTIES -and $file -and $newKeyAndValue -and [System.IO.File]::Exists($file)) {
        $msg="Writing KeyValue pair to $file"
        if ($global:LOG_FILE) {
            Write-Output "$msg" | WriteAndLog
        } else {
            Write-Host "$msg"
        }
        Write-Host "NOT LOGGED: KeyValue pair: $newKeyAndValue to file $file"
        Write-Output "$newKeyAndValue" >> $file
        $global:ACA_PROPERTIES=$null
        read_aca_properties $file
    }
}

Function read_spring_properties () {
    # This converts the application properties file into a hash table
    # Values are accessed by key like this: $propertyValue=$global:SPRING_PROPERTIES.'example.property.key'
    param (
        [string]$file = $null
    )
    if (!$global:SPRING_PROPERTIES -and $file -and [System.IO.File]::Exists($file)) {
        $file_content=(Get-Content $file -Raw)
        if ($file_content) { # File is not empty
            #$file_content=([Regex]::Escape($file_content) -replace "(\\r)?\\n",[Environment]::NewLine)
            #$global:SPRING_PROPERTIES=(ConvertFrom-StringData($file_content))
            $global:SPRING_PROPERTIES=(Get-Content -Path $file -Raw | ConvertFrom-StringData)
        } else { # File is empty
            # Initialize empty hash table
            $global:SPRING_PROPERTIES=@{}
        }
    } elseif ($file -and ![System.IO.File]::Exists($file)) {
        $msg="Warning: Spring properties file not found. The path provided was: $file"
        if ($global:LOG_FILE) {
            Write-Output "$msg" | WriteAndLog
        } else {
            Write-Host "$msg"
        }
    }
}

Function add_new_spring_property () {
    param (
        [string]$file = $null,
        [string]$newKeyAndValue = $null
    )
    if ($global:SPRING_PROPERTIES -and $file -and $newKeyAndValue -and [System.IO.File]::Exists($file)) {
        $msg="Writing KeyValue pair to $file"
        if ($global:LOG_FILE) {
            Write-Output "$msg" | WriteAndLog
        } else {
            Write-Host "$msg"
        }
        Write-Host "NOT LOGGED: KeyValue pair: $newKeyAndValue to file $file"
        Write-Output "$newKeyAndValue" >> $file
        $global:SPRING_PROPERTIES=$null
        read_spring_properties $file
    }
}

Function create_random () {
    return (1..100 | % { Get-Random } | sha512sum | tr -dc 'a-zA-Z0-9')
}

Function set_up_log () {
    if (![System.IO.Directory]::Exists($global:HIRS_DATA_LOG_DIR)) {
        New-Item -ItemType Directory -Path $global:HIRS_DATA_LOG_DIR -Force | Out-Null
    }
    $global:LOG_FILE=$global:HIRS_DATA_INSTALL_LOG_NAME
    
    if (-not (Test-Path $global:LOG_FILE)) {
      New-Item -ItemType File -Path $global:LOG_FILE
    } else {
      Write-Output "File already exists: $global:LOG_FILE"
    }
}

Function print_all_variables () {
    # intended for debugging
    # this will print all variables and their values in the current context
    Get-Variable | Out-String
}

Function WriteAndLog () {
    param(
        [Parameter(Mandatory = $true, ValueFromPipeline = $true, Position=0)]
        [string]$msg
    )
    # EXPECTS set_up_log() to be run and $global:LOG_FILE to be defined
    Write-Host "$msg"
    "$msg" >> "$global:LOG_FILE"
}

Function ChangeBackslashToForwardSlash () {
    param(
        [Parameter(Mandatory = $true, ValueFromPipeline = $true, Position=0)]
        [string]$msg
    )
    Write-Output ($msg -replace "\\","/")
}

Function ChangeFileBackslashToForwardSlash () {
    param(
        [string]$file = $null
    )
    (Get-Content $file) -replace "\\","/" | Set-Content $file
}
