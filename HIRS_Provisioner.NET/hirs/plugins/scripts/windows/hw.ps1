function pciParse($str) {
    $regex="^PCI\\VEN_(?<vendor>[0-9A-Fa-f]{4})&DEV_(?<product>[0-9A-Fa-f]{4})(?:&SUBSYS_(?<subsys1>[A-Za-z0-9]{4})(?<subsys2>[A-Za-z0-9]{4}))?(?:&REV_(?<revision>[A-Za-z0-9]+))?\\.*"

    $parse=$str -match $regex
    return $matches
}
function ideDiskParse($str) {
    $regex="^IDE\\[Dd][Ii][Ss][Kk]([0-9A-Za-z]+)[_]+(?:([0-9A-Za-z]+)[_]+)?([0-9A-Za-z_]{8})\\.*"

    $parse=$str -match $regex
    $vendor=""
    $product=""
    $revision=""
    $ptr=0
    switch ($matches.Count) {
        4 {$vendor=$matches[$ptr++].Trim("_")}
        Default {
            $product=$matches[$ptr++].Trim("_")
            $revision=$matches[$ptr++].Trim("_")
        }
    }

    $obj=[pscustomobject]@{
        vendor=$vendor
        product=$product
        revision=$revision
    }
    return $obj
}
function scsiDiskParse($str) {
    $regex="^SCSI\\[Dd][Ii][Ss][Kk](?<vendor>[0-9A-Za-z_-]{8})(?<product>[0-9A-Za-z_-]{16})(?<revision>[0-9A-Za-z_-]{4})\\.*"

    $parse=$str -match $regex

    return $matches
}
function isPCI($str) {
    $regex="^PCI\\.*"
    return ($str -match $regex)
}
function isIDE($str) {
    $regex="^IDE\\.*"
    return ($str -match $regex)
}
function isSCSI($str) {
    $regex="^SCSI\\.*"
    return ($str -match $regex)
}
function standardizeMACAddr($str) {
    $result=$str
    if (![string]::IsNullOrEmpty($str) -and ($str.Trim().Length -ne 0)) {
        $result=($str -replace "\s|\\n|\\r|\\t|:|-","").ToUpper()
    }
    return $result
}