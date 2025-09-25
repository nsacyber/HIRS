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
$global:HIRS_DATA_LOG_DIR=(Join-Path $global:HIRS_DATA_DIR "log")
$global:HIRS_DATA_INSTALL_LOG_NAME=(Join-Path $global:HIRS_DATA_LOG_DIR ("hirs_aca_install_"+(Get-Date -Format "yyyy-MM-dd")+'.log'))
$global:HIRS_CONF_DEFAULT_PROPERTIES_DIR=(Join-Path $global:HIRS_CONF_DIR "default-properties")
$global:HIRS_DATA_WIN_VERSION_FILE=(Join-Path $global:HIRS_CONF_DIR 'VERSION')

#         ACA Property Keys
$global:ACA_PROPERTIES_PKI_PWD_PROPERTY_NAME="hirs_pki_password"
$global:ACA_PROPERTIES_MYSQL_ADMIN_PWD_PROPERTY_NAME="mysql_admin_password"
$global:ACA_PROPERTIES_HIRS_DB_USERNAME_PROPERTY_NAME="hirs_db_username"
$global:ACA_PROPERTIES_HIRS_DB_PWD_PROPERTY_NAME="hirs_db_password"


#         Spring Property Keys
$global:SPRING_PROPERTIES_HIBERNATE_CONNECTION_USERNAME_PROPERTY_NAME="hibernate.connection.username"
$global:SPRING_PROPERTIES_HIBERNATE_CONNECTION_PWD_PROPERTY_NAME="hibernate.connection.password"
$global:SPRING_PROPERTIES_SSL_KEY_STORE_PWD_PROPERTY_NAME="server.ssl.key-store-password"
$global:SPRING_PROPERTIES_SSL_KEY_TRUST_STORE_PWD_PROPERTY_NAME="server.ssl.trust-store-password"

#         DB Configuration file
$global:DB_CONF = (Resolve-Path ([System.IO.Path]::Combine($Env:ProgramFiles, 'MariaDB 11.1', 'data', 'my.ini'))).Path

#         RSA Certificates Directory
$global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH=(Join-Path $HIRS_DATA_CERTIFICATES_HIRS_DIR "rsa_3k_sha384_certs")

#         RSA Server Side Certificates (Default)
$global:SSL_DB_RSA_SRV_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_rsa_3k_sha384_Cert_Chain.pem')
$global:SSL_DB_RSA_SRV_CERT=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_srv_rsa_3k_sha384.pem')
$global:SSL_DB_RSA_SRV_KEY=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_srv_rsa_3k_sha384.key')

#         RSA Client Side Certificates (Default)
$global:SSL_DB_RSA_CLIENT_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_rsa_3k_sha384_Cert_Chain.pem')
$global:SSL_DB_RSA_CLIENT_CERT=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_client_rsa_3k_sha384.pem')
$global:SSL_DB_RSA_CLIENT_KEY=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_db_client_rsa_3k_sha384.key')

#         RSA PKI Certificates
$global:RSA_HIRS_ROOT=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_root_ca_rsa_3k_sha384.pem')
$global:RSA_HIRS_INTERMEDIATE=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_intermediate_ca_rsa_3k_sha384.pem')
$global:RSA_HIRS_CA1=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_leaf_ca1_rsa_3k_sha384.pem')
$global:RSA_HIRS_CA2=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_leaf_ca2_rsa_3k_sha384.pem')
$global:RSA_HIRS_CA3=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_leaf_ca3_rsa_3k_sha384.pem')
$global:RSA_RIM_SIGNER=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_rim_signer_rsa_3k_sha384.pem')
$global:RSA_WEB_TLS_CERT=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_RSA_PATH 'HIRS_aca_tls_rsa_3k_sha384.pem')

#         ECC Certificates Directory       
$global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH=(Join-Path $HIRS_DATA_CERTIFICATES_HIRS_DIR "ecc_512_sha384_certs")

#         ECC Server Side Certificates 
$global:SSL_DB_ECC_SRV_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_ecc_512_sha384_Cert_Chain.pem')
$global:SSL_DB_ECC_SRV_CERT=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_db_srv_ecc_512_sha384.pem')
$global:SSL_DB_ECC_SRV_KEY=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_db_srv_ecc_512_sha384.key')

#         ECC Client Side Certificates 
$global:SSL_DB_ECC_CLIENT_CHAIN=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_ecc_512_sha384_Cert_Chain.pem')
$global:SSL_DB_ECC_CLIENT_CERT=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_db_client_ecc_512_sha384.pem')
$global:SSL_DB_ECC_CLIENT_KEY=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_db_client_ecc_512_sha384.key')

#         ECC PKI Certificates
$global:ECC_HIRS_ROOT=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_root_ca_ecc_512_sha384.pem')
$global:ECC_HIRS_INTERMEDIATE=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_intermediate_ca_ecc_512_sha384.pem')
$global:ECC_HIRS_CA1=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_leaf_ca1_ecc_512_sha384.pem')
$global:ECC_HIRS_CA2=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_leaf_ca2_ecc_512_sha384.pem')
$global:ECC_HIRS_CA3=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_leaf_ca3_ecc_512_sha384.pem')
$global:ECC_RIM_SIGNER=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_rim_signer_ecc_512_sha384.pem')
$global:ECC_WEB_TLS_CERT=(Join-Path $global:HIRS_DATA_CERTIFICATES_HIRS_ECC_PATH 'HIRS_aca_tls_ecc_512_sha384.pem')

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
#               aca_check_setup.ps1
#               aca_common.ps1         # This script. You are here.
#               aca_remove_setup.ps1
#               aca_setup.ps1
#               aca_win_config.ps1
#             db
#               db_create.ps1
#               db_drop.ps1
#               mysql_util.ps1
#             pki
#               pki_chain_gen.ps1
#               pki_setup.ps1
$global:HIRS_REL_WIN_ACA_HOME=(Split-Path -parent $PSCommandPath)
$global:HIRS_REL_WIN_HOME=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME ..)
$global:HIRS_REL_PACKAGE_HOME=(Join-Path -Resolve $global:HIRS_REL_WIN_HOME ..)
$global:HIRS_REL_LINUX_HOME=(Join-Path -Resolve $global:HIRS_REL_PACKAGE_HOME 'linux')
$global:HIRS_REL_SCRIPTS_ACA_HOME=(Join-Path -Resolve $global:HIRS_REL_LINUX_HOME 'aca')
$global:HIRS_REL_SCRIPTS_LINUX_DB_HOME=(Join-Path -Resolve $global:HIRS_REL_LINUX_HOME 'db')
$global:HIRS_REL_SCRIPTS_DB_CREATE_SQL=(Join-Path -Resolve $global:HIRS_REL_SCRIPTS_LINUX_DB_HOME 'db_create.sql')
$global:HIRS_REL_SCRIPTS_DB_SECURE_MYSQL_SQL=(Join-Path -Resolve $global:HIRS_REL_SCRIPTS_LINUX_DB_HOME 'secure_mysql.sql')
$global:HIRS_REL_SCRIPTS_LINUX_PKI_HOME=(Join-Path -Resolve $global:HIRS_REL_LINUX_HOME 'pki')
$global:HIRS_REL_SCRIPTS_PKI_CA_CONF=(Join-Path -Resolve $global:HIRS_REL_SCRIPTS_LINUX_PKI_HOME 'ca.conf')

#           WIN ACA powershell scripts
$global:HIRS_REL_WIN_ACA_BOOTRUN=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME 'aca_bootRun.ps1')
$global:HIRS_REL_WIN_ACA_COMMON=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME 'aca_common.ps1')
$global:HIRS_REL_WIN_ACA_SETUP=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME 'aca_setup.ps1')
$global:HIRS_REL_WIN_ACA_CONFIG=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME 'aca_win_config.ps1')
$global:HIRS_REL_WIN_ACA_CHECK_SETUP=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME 'aca_check_setup.ps1')
$global:HIRS_REL_WIN_ACA_REMOVE_SETUP=(Join-Path -Resolve $global:HIRS_REL_WIN_ACA_HOME 'aca_remove_setup.ps1')

#           WIN DB powershell scripts
$global:HIRS_REL_WIN_DB_HOME=(Join-Path -Resolve $global:HIRS_REL_WIN_HOME 'db')
$global:HIRS_REL_WIN_DB_CREATE=(Join-Path -Resolve $global:HIRS_REL_WIN_DB_HOME 'db_create.ps1')
$global:HIRS_REL_WIN_DB_MYSQL_UTIL=(Join-Path -Resolve $global:HIRS_REL_WIN_DB_HOME 'mysql_util.ps1')
$global:HIRS_REL_WIN_DB_DROP=(Join-Path -Resolve $global:HIRS_REL_WIN_DB_HOME 'db_drop.ps1')

#           WIN PKI powershell scripts
$global:HIRS_REL_WIN_PKI_HOME=(Join-Path -Resolve $global:HIRS_REL_WIN_HOME 'pki')
$global:HIRS_REL_WIN_PKI_CHAIN_GEN=(Join-Path -Resolve $global:HIRS_REL_WIN_PKI_HOME 'pki_chain_gen.ps1')
$global:HIRS_REL_WIN_PKI_SETUP=(Join-Path -Resolve $global:HIRS_REL_WIN_PKI_HOME 'pki_setup.ps1')
$global:HIRS_RELEASE_VERSION_FILE = (Join-Path -Resolve $global:HIRS_REL_PACKAGE_HOME '..\VERSION')

#           ACA Properties Files
$HIRS_PORTAL_PATH = (Resolve-Path "$global:HIRS_REL_PACKAGE_HOME\..\HIRS_AttestationCAPortal\src\main\resources").Path

$global:HIRS_REL_PORTAL_LOG4J_SPRING_XML = Join-Path $HIRS_PORTAL_PATH "log4j2-spring.xml"
$global:HIRS_REL_PORTAL_APPLICATION_SPRING_PROPERTIES = Join-Path $HIRS_PORTAL_PATH "application.properties"
$global:HIRS_REL_PORTAL_LOG4J_SPRING_LINUX_XML = Join-Path $HIRS_PORTAL_PATH "log4j2-spring.linux.xml"
$global:HIRS_REL_PORTAL_APPLICATION_LINUX_SPRING_PROPERTIES = Join-Path $HIRS_PORTAL_PATH "application.linux.properties"
$global:HIRS_REL_PORTAL_LOG4J_SPRING_WIN_XML = Join-Path $HIRS_PORTAL_PATH "log4j2-spring.win.xml"
$global:HIRS_REL_PORTAL_APPLICATION_WIN_SPRING_PROPERTIES = Join-Path $HIRS_PORTAL_PATH "application.win.properties"
$global:HIRS_REL_WIN_PKI_CA_CONF = Join-Path $global:HIRS_REL_WIN_PKI_HOME "ca.conf"

#    Saved values
# $Env:HIRS_MYSQL_ROOT_PWD
# $Env:HIRS_PKI_PWD
$global:ACA_PROPERTIES=$null
$global:SPRING_PROPERTIES=$null

# Below are the common utility functions that are used by the other ACA powershell scripts

Function read_aca_properties () {
    # This converts the ACA properties file into a hash table
    # Values are accessed by key like this: $propertyValue=$global:ACA_PROPERTIES.'example.property.key'
    param (
        [Parameter(Mandatory=$true)]
        [string]$file
    )
    if (!$global:ACA_PROPERTIES -and $file -and [System.IO.File]::Exists($file)) {
        $file_content=(Get-Content $file -Raw)
        if ($file_content) { # File is not empty
            $global:ACA_PROPERTIES=(Get-Content -Path $file -Raw | ConvertFrom-StringData)
        } else { # File is empty
            # Initialize empty hash table
            $global:ACA_PROPERTIES=@{}
        }
    } elseif ($file -and ![System.IO.File]::Exists($file)) {
        $msg="Warning: ACA properties file not found. The path provided was: [$file]"
        if ($global:LOG_FILE) {
            Write-Output "$msg" | WriteAndLog
        } else {
            Write-Host "$msg"
        }
    }
}

Function add_new_aca_property () {
    param (
        [Parameter(Mandatory=$true)]
        [string]$file,
        [Parameter(Mandatory=$true)]
        [string]$newKeyAndValue
    )

    if (-not $file -or -not $newKeyAndValue -or -not (Test-Path $file)) {
        Write-Output "Exiting script while attempting to add a new ACA property
        since the provided file [$file] does not exist and/or the provided key-value pair have not been supplied" | WriteAndLog
        exit 1
    }

    $msg="Writing KeyValue pair to $file"
    if ($global:LOG_FILE) {
        Write-Output "$msg" | WriteAndLog
    } else {
        Write-Host "$msg"
    }

    Write-Host "NOT LOGGED: KeyValue pair [$newKeyAndValue] has been added to file [$file]"
    Write-Output "$newKeyAndValue" >> $file
    $global:ACA_PROPERTIES=$null
    read_aca_properties $file
}

Function find_property_value(){
    param (
        [Parameter(Mandatory=$true)]
        [string]$file,
        [Parameter(Mandatory=$true)]
        [string]$key
    )

    # Check file exists and parameters are not empty/null
    if (-not $file -or -not $key -or -not (Test-Path $file)) {
        Write-Output "Exiting script while attempting to find an ACA property's value
        since the provided file [$file] does not exist and/or the provided key have not been supplied" | WriteAndLog
        exit 1
    }

    # Read all lines and check if any line starts with key=
    $match = Get-Content $file | Where-Object { $_ -match "^$key=" } | Select-Object -First 1

    # Extract the part after the '=' sign (for the first match only)
    $value = $match -replace "^$key=", ""

    # If the script was able to find the value that's associated with the provided key
    if($value) {
        Write-Host "NOT LOGGED: The value [$value] has been found to be associated with the key [$key]"  
        if($file -eq $global:HIRS_DATA_ACA_PROPERTIES_FILE ) {
          # Reset the global aca property hashmap and reload
          $global:ACA_PROPERTIES = $null
          Write-Output "Resetting and reloading the aca properties table" | WriteAndLog
          read_aca_properties $file
        } elseif($file -eq $global:HIRS_DATA_SPRING_PROP_FILE){
          # Reset the global spring property hashmap and reload
          $global:SPRING_PROPERTIES = $null
          Write-Output "Resetting and reloading the spring properties table" | WriteAndLog
          read_spring_properties $file
        }
    } else {
        Write-Host "NOT LOGGED: There are no values associated with the provided key [$key]"
    }
    
    return $value
}

Function read_spring_properties () {
    # This converts the application properties file into a hash table
    # Values are accessed by key like this: $propertyValue=$global:SPRING_PROPERTIES.'example.property.key'
    param (
        [Parameter(Mandatory=$true)]
        [string]$file
    )
    if (!$global:SPRING_PROPERTIES -and $file -and [System.IO.File]::Exists($file)) {
        $file_content=(Get-Content $file -Raw)
        if ($file_content) { # File is not empty
            $global:SPRING_PROPERTIES=(Get-Content -Path $file -Raw | ConvertFrom-StringData)
        } else { # File is empty
            # Initialize empty hash table
            $global:SPRING_PROPERTIES=@{}
        }
    } elseif ($file -and ![System.IO.File]::Exists($file)) {
        $msg="Warning: Spring properties file not found. The path provided was: [$file]"
        if ($global:LOG_FILE) {
            Write-Output "$msg" | WriteAndLog
        } else {
            Write-Host "$msg"
        }
    }
}

Function add_new_spring_property () {
    param (
        [Parameter(Mandatory=$true)]
        [string]$file,
        [Parameter(Mandatory=$true)]
        [string]$newKeyAndValue
    )

    if (-not $file -or -not $newKeyAndValue -or -not (Test-Path $file)) {
        Write-Output "Exiting script while attempting to add a new Spring property
        since the provided file [$file] does not exist and/or the provided key-value pair have not been supplied" | WriteAndLog
        exit 1
    }

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

Function create_random () {
     # Step 1: Generate a string of 100 random numbers concatenated together
    $randomData = -join (1..100 | ForEach-Object { Get-Random })

    # Step 2: Create a SHA512 hashing object
    $sha512 = [System.Security.Cryptography.SHA512]::Create()

    # Step 3: Convert the random string into a byte array using UTF8 encoding
    $bytes = [Text.Encoding]::UTF8.GetBytes($randomData)

    # Step 4: Compute the SHA512 hash of the byte array, producing a byte array hash
    $hashBytes = $sha512.ComputeHash($bytes)

    # Step 5: Convert each byte in the hash to a two-digit hexadecimal string and join them all into one string
    return -join ($hashBytes | ForEach-Object { $_.ToString("x2") })
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
        [Parameter(Mandatory=$true)]
        [string]$file
    )
    (Get-Content $file) -replace "\\","/" | Set-Content $file
}
