#!/bin/bash
#Rim system test support functions.
#exit status functions for rim create and rim verify.
rim_create_status(){
  if [ $1 -eq 0 ]; then
      echo "********"
      echo "RIM create passed, attempting to verify the signature on base rim file..."
      echo "********"
  else
      echo "********"
      echo "FAILED: rim creation failed"
      echo "********"
      ((failCount++))
  fi
}

rim_verify_status(){
  if [ $1 -eq 0 ]; then
      echo "********"
      echo "RIM Verify passed!"
      #echo "********"
  else
      echo "********"
      echo "FAILED: rim verify failed"
      #echo "********"
      ((failCount++))
  fi
}

rim_create_fail_test(){
    if [ $1 -ne 0 ]; then
        echo "********"
        echo "PASSED: RIM create FAILED as expected."
        #echo "********"
        exit 0
    else
        echo "********"
        echo "FAILED: RIM create PASSED expected FAIL."
        #echo "********"
        exit 1
    fi
}

rim_verify_fail_test(){
    if [ $1 -ne 0 ]; then
        echo "********"
        echo "PASSED: RIM verify FAILED as expected."
        #echo "********"
        exit 0
    else
        echo "********"
        echo "FAILED: RIM verify PASSED expected FAIL."
        #echo "********"
        exit 1
    fi
}

check_req_attributes() {
  local element="$1"
  shift
  local attributes=("$@")
  for attribute in "${attributes[@]}"; do
    ((num_tests++))
    if grep -q "$element.*$attribute=" "$BASE_RIM"; then
      echo "The $element element HAS the REQUIRED '$attribute' attribute."
      ((num_tests_pass++))
    else
      echo -e "\033[31mError: The $element element is MISSING the REQUIRED '$attribute' attribute.\033[0m"
      exitStatus=1
    fi
  done
}

check_opt_attributes() {
  local element="$1"
  shift
  local attributes=("$@")
  for attribute in "${attributes[@]}"; do
    if grep -q "$element.*$attribute=" "$BASE_RIM"; then
      echo "The $element element HAS the OPTIONAL '$attribute' attribute."
    else
      echo -e "\033[33mThe $element element is MISSING the OPTIONAL '$attribute' attribute.\033[0m"
    fi
  done
}

check_element() {
  local element="$1"
  ((num_tests++))
  if grep -q "$1" "$BASE_RIM"; then
    echo "************"
    echo "$element element exists checking for REQUIRED attributes... "
    ((num_tests_pass++))
  else
    echo -e "\033[31mERROR: $element element is missing\033[0m"
    exitStatus=1
  fi
}

# checks parent tag for REQUIRED elements/attributes found in child tags.
check_tag_req(){
    local element="$1"
    local tag_block="$2"
    shift 2
    local attributes=("$@")

    for attribute in "${attributes[@]}"; do
      ((num_tests++))
      if echo "$tag_block" | grep -q "$attribute"; then
        echo "The $element element HAS the REQUIRED '$attribute' attribute."
        ((num_tests_pass++))
      else
        echo -e "\033[31mError: The $element element is MISSING the REQUIRED '$attribute' attribute.\033[0m"
        exitStatus=1
      fi
    done
}
# checks parent tag for OPTIONAL elements/attributes found in child tags.
check_tag_opt(){
    local element="$1"
    local tag_block="$2"
    shift 2
    local attributes=("$@")

    for attribute in "${attributes[@]}"; do
      if echo "$tag_block" | grep -q "$attribute"; then
        echo "The $element element HAS the REQUIRED '$attribute' attribute."
      else
        echo -e "\033[33mThe $element element is MISSING the OPTIONAL '$attribute' attribute.\033[0m"
      fi
    done
}