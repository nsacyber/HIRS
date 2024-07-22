#!/bin/bash
# This script will run all the tests in rim/scrips directory. it will ignore specified files.
# counters that will provide information about the script status.
testsFailed=0
testsPassed=0
testsRan=0
# Capture location of this script to allow from invocation from any location
scriptDir=$(dirname -- "$(readlink -f -- "${BASH_SOURCE[0]}")")
# go to the script directory so everything runs smoothly ...
pushd $scriptDir > /dev/null


# adding the verbose option.
while [[ $# -gt 0 ]]; do
  case $1 in
    '-v'|'--verbose')
      ARG_VERBOSE=YES
      echo "verbose parameters"
      shift # past argument
      ;;
    '-*'|'--*')
      echo "Unknown option $1"
      exit 1
      ;;
    *)
      echo "Unknown argument $1"
      exit 1
      shift # past argument
      ;;
  esac
done

#List of files in the scripts directory to ignore.
exclude=("run_all_tests.sh"  "rim_functions.sh")

#loop through the test/rim/scripts directory
for script in *.sh; do
   #ignoring specified (non test) files.
   if [[ ! "${exclude[*]}" =~ $script  ]]; then
    ((testsRan++))
    echo ""
    echo "----------------"
    echo "RUNNING $script"

    if [ -n "$ARG_VERBOSE" ]; then
      ./"$script"
    else
      ./"$script" >/dev/null
    fi

    #checking the exit stats of the script (test).
    if [ $? -eq 0 ];then
      if [ -z "$ARG_VERBOSE" ]; then
        echo "PASSED  $script"
      fi
      echo "----------------"
      ((testsPassed++))
    else
      if [ -z "$ARG_VERBOSE" ]; then
        echo -e "\033[31mFAILED  $script\033[0m"
      fi
      echo "----------------"
      ((testsFailed++))
    fi
  else
    echo ""
    echo "----------------"
    echo "skipping $script"
    echo "----------------"
  fi

done

#return to whatever directory you started at
popd > /dev/null

#test results
echo ""
echo "**** Test Results *****"
echo "Number of tests ran    = $testsRan"
echo "Number of tests passed = $testsPassed"
echo "Number of tests failed = $testsFailed"

#tests status
if [ "$testsFailed" -eq 0 ]; then
  exit 0
else
  exit 1
fi