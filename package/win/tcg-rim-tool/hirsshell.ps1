# Script to start a new shell with a rim alias 
$StartInfo = new-object System.Diagnostics.ProcessStartInfo
$StartInfo.FileName = "$pshome\powershell.exe"
$StartInfo.Arguments = "-NoExit -Command 
               `$Host.UI.RawUI.WindowTitle=`'TCG RIM TOOL`'; 
               Set-Alias elt '$PWD\eventlog.ps1';
	       Set-Alias rim '$PWD\rim.ps1';
               echo 'The TCG RIM TOOL is intended for testing TCG Defined PC Client Reference Integrity Manifests (RIMs)';
               echo 'for usage type: rim -h';
	       echo 'for eventlog usage type: elt -h'
               Set-Location -Path $PWD;
               function prompt {'HIRS > '};" 
[System.Diagnostics.Process]::Start($StartInfo)

