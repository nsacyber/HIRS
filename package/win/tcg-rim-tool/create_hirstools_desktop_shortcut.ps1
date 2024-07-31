# Unzip tools in the working directory
Expand-Archive -Path ..\tcg_rim_tool.zip -DestinationPath .\tcg_rim_tool
Expand-Archive -Path ..\tcg_eventlog_tool.zip -DestinationPath .\tcg_eventlog_tool
# Create a shortcut to start the RIM shell
$WshShell = New-Object -comObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut("$Home\Desktop\HIRS_tools.lnk")
$Shortcut.TargetPath = "$PWD\RunTools.bat"
$Shortcut.WorkingDirectory =  "$PWD"
$Shortcut.Save()
