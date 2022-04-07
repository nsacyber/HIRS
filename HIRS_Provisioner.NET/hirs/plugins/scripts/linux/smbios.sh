#!/bin/bash
dmidecodeHandles () {
    type="${1}"
    str=$(dmidecode -t "$type" | grep -e '^Handle.*' | sed 's/Handle \(0x[0-9A-F^,]*\),.*/\1/' | tr "\n\r\t" ' ' | sed -e 's/^[[:space:]]*//' | sed -e 's/[[:space:]]*$//')
    old="$IFS"
    IFS=' '
    tableHandles=($str)
    IFS="$old"
}
dmidecodeData () {
    handle="${1}"
    if  [[ $handle =~ ^0x[0-9A-Fa-f]+$ ]]; then
        str=$(dmidecode -H "$handle" -u | awk '/Header and Data:/{f=1;next} /Strings/{f=0} f' | tr "\n\r\t" ' ' | tr -s ' ' | sed -e 's/^[[:space:]]*//' | sed -e 's/[[:space:]]*$//')
        old="$IFS"
        IFS=' '
        tableData=($str)
        IFS="$old"
    fi
}
dmidecodeStrings () {
    handle="${1}"
    if  [[ $handle =~ ^0x[0-9A-Fa-f]+$ ]]; then
        str=$(dmidecode -H "$handle" -u | awk '/Strings/{f=1;next} /^\w+$/{f=0} f' | sed 's/^[^"]*$//g' | sed 's/^\w+//g' | sed '/^[[:space:]]*$/d')
        old="$IFS"
        IFS=$'\n'
        tableStrings=($str)
        IFS="$old"
    fi
}
dmidecodeParseHandle () {
    handle="${1}"
    dmidecodeData "$handle"
    dmidecodeStrings "$handle"
}
dmidecodeNumHandles () {
    printf "${#tableHandles[@]}"
}
dmidecodeParseTypeAssumeOneHandle () {
    type="${1}"
    dmidecodeHandles "$type" > /dev/null
    dmidecodeParseHandle "${tableHandles[0]}"
}
dmidecodeGetByte () {
    index="${1}"
    index=$(printf "%d" $index)
    printf "${tableData[$index]}"
}
dmidecodeGetString () {
    strref="${1}"
    str=""
    if [[ $strref =~ ^[0-9A-Fa-f]+$ ]]; then
        strrefDec=$(printf "%d" "0x""$strref")
        lenDec=$(printf "%d" "0x"${#tableStrings[@]})
        if [ $strrefDec -le $lenDec ] && [ $strrefDec -gt 0 ]; then
            str="${tableStrings[$strrefDec-1]}"
            str=$(printf "$str" | sed 's/^[ \t]*"\?//;s/"\?[ \t]*$//')
        fi
    fi
    printf "$str"
}


# Examples:
#dmidecodeHandles "1"
#numHandles=$(dmidecodeNumHandles)
#echo $numHandles
#echo ${tableHandles[*]}

#dmidecodeStrings "${tableHandles[0]}"

#echo ${tableStrings[0]}

#dmidecodeData "${tableHandles[0]}"

#manufacturer=$(dmidecodeGetString $(dmidecodeGetByte "0x4"))
#model=$(dmidecodeGetByte "0x6")
#model=$(printf "%d" "0x""$model") # Convert to decimal
#model=$(dmidecodeGetString $(dmidecodeGetByte "0x5"))
#serial=$(dmidecodeGetString $(dmidecodeGetByte "0x7"))
#revision=$(dmidecodeGetString $(dmidecodeGetByte "0x6"))
#echo $manufacturer
#echo $model
#echo $serial
#echo $revision
