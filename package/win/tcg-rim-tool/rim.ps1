# Script to run the tcg_rim_tool in java

$JavaParams = @{
    FilePath =  'java'
    ArgumentList = @(
        '-jar "{0}"' -f "$PWD\tcg_rim_tool-2.1.0.jar"
        "$args"
    )
}

Start-Process @JavaParams  -NoNewWindow -Wait