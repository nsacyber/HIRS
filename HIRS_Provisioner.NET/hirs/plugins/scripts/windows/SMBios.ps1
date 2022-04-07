#
# This method converts the raw SMBIOS data into an associative array indexed by type.
# 
# Usage:  $smbios=(Get-SMBiosStructures)
#         $smbios[$Type]
#
# Adapted from SysToolsLib Powershell Library released under Apache 2.0 License
# https://github.com/JFLarvoire/SysToolsLib/blob/master/PowerShell/Library.ps1#Get-SMBiosStructures
# 
Function Get-SMBiosStructures() {
  $structs = @{}
  $data = (Get-WmiObject -Class MSSMBios_RawSMBiosTables -Namespace root\wmi -ErrorAction SilentlyContinue).SMBiosData
  $i = 0
  while (($data[$i+1] -ne $null) -and ($data[$i+1] -ne 0)) { # While the structure has non-0 length
    $i0 = $i
    $n = $data[$i]   # Structure type
    $l = $data[$i+1] # Structure length
    $i += $l # Count bytes from the start of the structure to the beginning of the strings section
    if ($data[$i] -eq 0) {$i++} # If there's no trailing string, count the extra NUL
	$strings=@()
    while ($data[$i] -ne 0) { # Count the size of the string section
      $s = ""
      while ($data[$i] -ne 0) { $s += [char]$data[$i++] } # Count the byte length of each string
	  $strings += $s
      $i++ # Count the string terminator NUL
    }
    $i1 = $i
	
	$obj=[pscustomobject]@{
	    type=$n
		data=@($data[$i0..$i1])
		strings=$strings
	}
	
	if ($structs["$n"] -eq $null) {
	    $structs["$n"] = @()
	}
    if ($l -gt 0) {
        $structs["$n"] += $obj
    }
    $i++ # Count the final NUL of the table, and get ready for the next table
  }
  return @($structs)
}

Function Get-SMBiosString($struct, $type, $refbyte) {
    $str=""
    if ($struct[$type] -ne $null -and $struct[$type].data -ne $null -and $struct[$type].strings -ne $null) {
        $strref=$struct[$type].data[$refbyte]
        $len=@($struct[$type].strings).Count
        if ($strref -le $len  -and $strref -gt 0) {
            $str=@($struct[$type].strings)[$struct[$type].data[$refbyte]-1]
        }
    }
    return $str
}

# Example:
# $smbios=(Get-SMBiosStructures)
# echo $smbios["3"]
# echo $smbios["17"]
# echo $smbios["3"].strings
# echo @($smbios["3"].strings)[$smbios["3"].data[4]-1]
# $platformManufacturer=(Get-SMBiosString $smbios "1" 0x4)
# $platformModel=(Get-SMBiosString $smbios "1" 0x5)
# $platformVersion=(Get-SMBiosString $smbios "1" 0x6)
# $platformSerial=(Get-SMBiosString $smbios "1" 0x7)
# echo $platformManufacturer
# echo $platformModel
# echo $platformVersion
# echo $platformSerial
