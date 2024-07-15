
# Create a shortcut to start the RIM shell
$WshShell = New-Object -comObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut("$Home\Desktop\HIRS_tools.lnk")
$Shortcut.TargetPath = "$PWD\hirsshell.ps1"
$Shortcut.WorkingDirectory =  "$PWD"
$Shortcut.Save()
