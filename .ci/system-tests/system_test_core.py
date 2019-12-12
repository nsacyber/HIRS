# Defines core methods shared amongst system test scripts

import sets
import unittest
import shlex
import subprocess
import os
import binascii
import requests
import logging
import random
import time
import datetime
import json
import pprint
import xml.etree.ElementTree as ET
from StringIO import StringIO

DEFAULT_GROUP_NAME = "Default Group"
DEFAULT_TPM_POLICY = "Test TPM Policy"
DEFAULT_IMA_POLICY = "Test IMA Policy"
CACHED_XML_REPORT = None

APPRAISAL_SUCCESS_MESSAGE = "Appraisal passed"

class HIRSPortal:
    def __init__(self, hirs_server_url):
        self.server_url = hirs_server_url

    def request(self, method, path, params={}, data={}, files={}, expected_status_codes=[200], operation=None, verify=False):
        return web_request(self.server_url, method, path, params, data, files, expected_status_codes, operation, verify)

    def set_default_policies(self, tpm_policy="No Policy",
                             ima_policy="No Policy"):
        """set the given policies to be the policies for the default group."""
        payload = {"description": "default group modified for systems tests",
                   "name": DEFAULT_GROUP_NAME}
        # TODO this will report failure if the group already exists. Not sure how to avoid this
        request_result = self.request("post", "portal/group/create", data=payload)
        self.set_tpm_ima_policy(DEFAULT_GROUP_NAME, tpm_policy, ima_policy)

    def set_tpm_ima_policy(self, group_name=DEFAULT_GROUP_NAME, tpm_policy=None, ima_policy=None):
        """set the TPM and IMA policy for the group"""
        payload = {"name": group_name,
                   "ima": ima_policy,
                   "tpm": tpm_policy,
                   "optionRadio" : "existingImaPolicy",
                   "policyName" : ""}
        self.request("post", "portal/group/update/policies", data=payload)

        payload = {"name": group_name,
                   "ima": ima_policy,
                   "tpm": tpm_policy,
                   "optionRadio" : "existingTpmPolicy",
                   "policyName" : ""}
        self.request("post", "portal/group/update/policies", data=payload)

    def set_group_appraisal_wait_setting(self, group_name=DEFAULT_GROUP_NAME,
                                         is_client_waiting='checked'):
        """set the specified group's client wait for appraisal setting to the specified value."""
        self.request("post", "portal/group/editWaitForAppraisalCompletion", data={"groupName": group_name, "enabled" : is_client_waiting})

    def get_latest_report(self):
        """Retrieves the latest report that was created for the given client.

        The retrieved report is cached. Calling run_hirs_report will clear the
        latest report from the cache.
        """
        global CACHED_XML_REPORT
        if CACHED_XML_REPORT:
            logging.info("found cached XML report")
            return CACHED_XML_REPORT

        logging.info("cached XML report not found, retrieving latest report from"
                     "the server")

        latest_report_id = self.get_latest_report_summary()['report']['id']
        logging.info("requesting raw report")

        request_result = self.request("get", "portal/report/xml/raw?uuid=" + latest_report_id, operation="get latest report")
        CACHED_XML_REPORT = request_result.text
        return CACHED_XML_REPORT

    def get_alert_count_from_latest_report(self):
        """ Retrieves the alert count from the latest report. """
        return self.get_alerts_from_latest_report()['recordsTotal']

    def get_alerts_from_latest_report(self):
        """ Retrieves the alert list from the latest report. """
        latest_report_id = self.get_latest_report_summary()['report']['id']
        return self.request("get", "portal/alerts/list?report=" + latest_report_id).json()

    def start_on_demand(self, group_name="Default%20Group"):
        self.request("get", "portal/on-demand/group/" + group_name)

    def get_latest_report_summary(self):
        """Pull the latest report summary from the Portal."""
        all_reports = self.request("get", "portal/report/list").json()['data']
        if len(all_reports) == 0:
            return None
        return max(all_reports, key=lambda report: report['timestamp'])

    def get_devices(self):
        """Get devices Portal."""
        return self.request("get", "portal/devices/list").json()

    def report_contains_ima_record(self, filename, sha_hash, report_id):
        """Check whether the report with the given id contains the given filename
           and hash.
        """
        logging.info("checking if report with ID {} contains file {} with hash {}".format(
            report_id, filename, sha_hash))
        ima_records = self.request("get", "portal/report/list/imaRecords", params={'scope': 'REPORT', 'id': report_id}).json()['data']

        def record_matcher(record):
            # check for IMA records with this hash, and if the filename is in the record's path
            # (works for full or partial path)
            return (record['hash']['digestString'] == sha_hash) and (filename in record['path'])

        matching_records = filter(record_matcher, ima_records)
        return len(matching_records) > 0

    def upload_payload(self, payload):
        json_path = "tmp.json"
        json_file = open(json_path, 'w')
        json_file.write(json.dumps(payload))
        json_file.close()
        post_file = {'file': open(json_path, 'rb')}
        logging.debug("uploading policy:\n{0}".format(pprint.pformat(payload)))
        response = self.request("post", "portal/policies/import", files=post_file, operation="upload policy")
        post_file['file'].close()
        os.remove(json_path)
        return payload["name"]

    def add_ima_policy(self, required_set=None, whitelist=None, blacklist=None, ignore=None, unknown_fail="false", delta_reports_enabled="false", policy_name_prefix=""):
        timestamp = get_current_timestamp()
        policy_name = "{0}_IMA_systems_test_policy_{1}".format(policy_name_prefix, timestamp)
        policy_description = "IMA policy for systems testing"
        payload = {"name": policy_name,
                   "description": policy_description,
                   "type": "IMA"}

        required_payload, whitelist_payload, ignore_payload, blacklist_payload = [], [], [], []

        if required_set is not None:
            required_payload.append(required_set)

        if whitelist is not None:
            whitelist_payload.append(whitelist)

        if blacklist is not None:
            blacklist_payload.append(blacklist)

        if ignore is not None:
            ignore_payload.append(ignore)

        ima_payload = {
            "deltaReportEnable": delta_reports_enabled,
            "failOnUnknowns": unknown_fail,
            "validatePcr": "false",
            "checkSubsequentBaselines": "true",
            "partialPathEnable": "true",
            "required": required_payload,
            "whitelist": whitelist_payload,
            "blacklist": blacklist_payload,
            "ignoreSet": ignore_payload
        }
        payload.update(ima_payload)

        return self.upload_payload(payload)

    def add_tpm_wl_policy(self, baseline, policy_name_prefix=""):
        timestamp = get_current_timestamp()
        policy_name = "{0}_TPM_systems_test_wl_policy_{1}".format(policy_name_prefix, timestamp)
        policy_description = "TPM white list policy for systems testing"
        payload = {"name": policy_name,
                   "description": policy_description,
                   "type": "TPM"}

        tpm_payload = {"appraiserPcrMask": 0xffffff,
                       "reportPcrMask": 0xffffff,
                       "appraiseFullReport": "true",
                       "validateSignature": "true",
                       "white-list-baselines": [baseline]}
        payload.update(tpm_payload)

        return self.upload_payload(payload)

    def add_tpm_bl_policy(self, baseline, policy_name_prefix=""):
        timestamp = get_current_timestamp()
        policy_name = "{0}_TPM_systems_test_bl_policy_{1}".format(policy_name_prefix, timestamp)
        policy_description = "TPM black list policy for systems testing"
        payload = {"name": policy_name,
                   "description": policy_description,
                   "type": "TPM"}

        tpm_payload = {"appraiserPcrMask": 0xffffff,
                       "reportPcrMask": 0xffffff,
                       "appraiseFullReport": "true",
                       "validateSignature": "true",
                       "black-list-baselines": [baseline]}
        payload.update(tpm_payload)

        return self.upload_payload(payload)

    def add_to_ima_baseline(self, baseline_name, file_path, file_hash):
        self.request("post", "portal/baselines/record/ima/add", data={'name': baseline_name, 'path': file_path, 'hash': file_hash}, operation="add to IMA baseline")

    def upload_csv_baseline(self, baseline_path, appraiser_type):
        post_file = {'file': open(baseline_path, 'rb')}
        current_time = datetime.datetime.now()
        baseline_name = baseline_path.split('.')[0] + '_' + str(current_time.hour) + '-' + str(current_time.minute) + '-' + str(current_time.second)
        self.request("post", "uploadImaCsv", data={'baselineName': baseline_name, 'optionsRadios': appraiser_type}, files=post_file, operation="upload baseline")
        if request_result != 200:
            logging.error("upload baseline return code: {0}, response text:\n"
                          "{1}".format(request_result.status_code, request_result.text))
        post_file['file'].close()
        subprocess.call("rm " + baseline_path, shell=True)
        return baseline_name

    """Creates a Yum repository, configures it with a URL, triggers an update, and waits for the update to complete via Portal endpoints."""
    def configure_yum_repository(self, baseline_name, base_url):
        self.request("post", "portal/repository/create", params={'name':baseline_name,'type':'Yum'}, operation="create Yum repository")
        self.request("post", "portal/repository/update/url", params={'name':baseline_name,'baseUrl':base_url}, operation="set URL of Yum repository")
        self.request("post", "portal/repository/job/trigger", params={'name':baseline_name}, operation="update Yum repository")

        # 4. wait for update to finish
        update_complete = False
        max_wait_time_seconds = 240
        sleep_time_seconds = 5
        counter = 1
        while not update_complete:
            time.sleep(sleep_time_seconds)

            if counter * sleep_time_seconds >= max_wait_time_seconds:
                msg = "Timeout waiting for repository update: {0} seconds".format(max_wait_time_seconds)
                logging.error(msg)
                raise RuntimeError(msg)

            counter += 1
            request_result = self.request("get", "portal/repository/job/check", params={'name':baseline_name}, operation="check status of repo update job")
            update_complete = not json.loads(request_result.text)['jobCurrentlyRunning']

    """Creates a BroadRepoImaBaseline repository, configures it with a repository, and updates the baseline from the repository's contents via Portal endpoints."""
    def create_broad_ima_baseline(self, baseline_name, repository_name):
        self.request("post", "portal/baselines/create", params={'name':baseline_name,'type':'broad'}, operation="create broad baseline")
        self.request("post", "portal/baselines/update/repositories", params={'name':baseline_name,'repositories':[repository_name]}, operation="add repository to broad baseline")
        self.request("post", "portal/baselines/triggerupdate", params={'name':baseline_name}, operation="update broad repository from its repository")

    """Creates a new Policy with the given type and name via Portal endpoints."""
    def create_policy(self, name, policy_type):
        self.request("post", "portal/policies/create", params={'name':name,'type':policy_type}, operation="create new policy")

    """Enables or disables partial path checking for an IMA policy."""
    def set_partial_paths_for_ima_policy(self, policy_name, enabled):
        checked = 'unchecked'
        if enabled:
            checked = 'checked'
        self.request("post", "portal/policies/update", params={'name':policy_name,'partial':checked}, operation="update policy's partial path setting")

    """Enables or disables kernel detection for a TPM policy."""
    def set_kernel_setting(self, policy_name, kernel_detect_enabled, kernel_alert_enabled, kernel_alert_severity="UNSPECIFIED"):
        kernel_detect_checked = 'false'
        if kernel_detect_enabled:
            kernel_detect_checked = 'true'
        kernel_alert_checked = 'false'
        if kernel_alert_enabled:
            kernel_alert_checked = 'true'
        self.request("post", "portal/policies/update/editKernelDetectSettings", params={'name':policy_name,'kernelDetectToggle':kernel_detect_checked,'kernelAlertToggle':kernel_alert_checked,'kernelAlertSeverity':kernel_alert_severity}, operation="update policy's kernel detection setting")

    """Creates a new Policy with the given type and name via Portal endpoints."""
    def add_baseline_to_required_sets(self, policy_name, baseline_name):
        self.request("post", "portal/policies/update", params={'name':policy_name,'required':[baseline_name]}, operation="add baseline to required sets")

    def get_alerts(self):
        return self.request("get", "portal/alerts/list").json()

class AttestationCAPortal:
    def __init__(self, hirs_server_url):
        self.server_url = hirs_server_url

    def request(self, method, path, params={}, data={}, files={}, expected_status_codes=[200], operation=None, verify=False):
        return web_request(self.server_url, method, path, params, data, files, expected_status_codes, operation, verify)

    def check_is_online(self):
        return self.request("get", "portal/certificate-request/platform-credentials/list").json()

    def get_supply_chain_validation_summaries(self):
        return self.request("get", "portal/validation-reports/list").json()

    def disable_supply_chain_validations(self):

        # The initial POST request goes through, but the redirect from the server is attempted
        # which results in a 404, or possibly a 200 on centos7, apparently.
        self.request("post", "portal/policy/update-ec-validation",
                     expected_status_codes=[404, 200], params={'ecValidate': "unchecked",})
        self.request("post", "portal/policy/update-pc-validation",
                     expected_status_codes=[404, 200], params={'pcValidate': 'unchecked'})
        self.request("post", "portal/policy/update-pc-attribute-validation",
                     expected_status_codes=[404, 200], params={'pcAttributeValidate': 'unchecked'})

    def enable_supply_chain_validations(self):

        # The initial POST request goes through, but the redirect from the server is attempted
        # which results in a 404, or possibly a 200 on centos7, apparently.
        self.request("post", "portal/policy/update-ec-validation",
                     expected_status_codes=[404, 200], params={'ecValidate': "checked",})
        self.request("post", "portal/policy/update-pc-validation",
                     expected_status_codes=[404, 200], params={'pcValidate': 'checked'})
        self.request("post", "portal/policy/update-pc-attribute-validation",
                    expected_status_codes=[404, 200], params={'pcAttributeValidate': 'checked'})

    def enable_ec_validation(self):
        self.request("post", "portal/policy/update-ec-validation",
                     expected_status_codes=[404, 200], params={'ecValidate': "checked",})

    def get_devices(self):
        """Get devices from ACA portal."""
        return self.request("get", "portal/devices/list").json()

    def get_ek_certs(self):
        """Get EK certs from ACA portal."""
        return self.request("get", "portal/certificate-request/endorsement-key-credentials/list").json()

    def get_pk_certs(self):
        """Get PK certs from ACA portal."""
        return self.request("get", "portal/certificate-request/platform-credentials/list").json()

    def get_trust_chains(self):
        """Get trust chains from ACA portal."""
        return self.request("get", "portal/certificate-request/trust-chain/list").json()

    def upload_ca_cert(self, ca_cert_file):
        file = {'file': open(ca_cert_file, 'rb')}
        self.request("post", "portal/certificate-request/trust-chain/upload", files=file, operation="upload CA cert")

    def upload_pk_cert(self, pk_cert_file):
        file = {'file': open(pk_cert_file, 'rb')}
        self.request("post", "portal/certificate-request/platform-credentials/upload", files=file, operation="upload PK cert")

def web_request(server_url, method, path, params={}, data={}, files={}, expected_status_codes=[200], operation=None, verify=False):
    url = server_url + path
    if method not in ['get', 'post']:
        raise ValueError("Method " + method + " not valid.")
    request_response = getattr(requests, method)(url, params=params, data=data, files=files, verify=verify)

    request_msg = method + " " + url
    if operation == None:
        operation = request_msg
    else:
        operation += " (" + request_msg + ")"

    check_request_response(expected_status_codes, request_response, operation)
    return request_response

"""Checks a requests response to see if its status code matches the expected status code.  If it does, this method returns True.  If it does not, this
method will log the error and return False."""
def check_request_response(expected_status_codes, request_result, operation):
    if not request_result.status_code in expected_status_codes:
        message = "Unable to " + operation + ": {0}, response text:\n{1}".format(request_result.status_code, request_result.text)
        logging.error(message)
        raise RuntimeError(message)

def collectors(collectors, collector_list):
    enabled_collectors = sets.Set(collector_list)
    tested_collectors = sets.Set(collectors)
    if tested_collectors.issubset(enabled_collectors):
        return lambda func: func
    return unittest.skip("{0} collector isn't enabled".format(tested_collectors.difference(enabled_collectors)))

def send_command(full_command, accept_nonzero_status=False):
    parsed_command = shlex.split(full_command)
    p = subprocess.Popen(parsed_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    client_out, client_err = p.communicate()
    if p.returncode != 0 and not accept_nonzero_status:
        logging.error("Command: " + full_command + " exited with return code " + str(p.returncode))
        logging.error(str(client_out))
        logging.error(str(client_err))
        raise RuntimeError("Command exited with a nonzero status, out:\n" + str(client_out) + "\nerr:\n" + str(client_err))
    return client_out

def send_command_sha1sum(full_command, accept_nonzero_status=False):
    sha1sum_command = shlex.split(full_command)
    head_command = ['head', '-c40']
    p1 = subprocess.Popen(sha1sum_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    p2 = subprocess.Popen(head_command, stdin=p1.stdout,stdout=subprocess.PIPE)
    p1.stdout.close()
    client_out, client_err = p2.communicate()
    if p2.returncode != 0 and not accept_nonzero_status:
        logging.error("Command: " + full_command + " exited with return code " + str(p2.returncode))
        logging.error(str(client_out))
        logging.error(str(client_err))
        raise RuntimeError("Command exited with a nonzero status, out:\n" + str(client_out) + "\nerr:\n" + str(client_err))
    return client_out

def run_hirs_report(client_hostname):
    """Runs a hirs report for the specified client host name.
    The cached xml report is cleared.

    Returns true if the client output indicated appraisal success. false otherwise
    """
    client_out = run_hirs_report_and_clear_cache_V2(client_hostname)
    if APPRAISAL_SUCCESS_MESSAGE in client_out:
        logging.info("Report appraisal passed")
        return True
    else:
        logging.info("Report appraisal unsuccessful: " + client_out)
        return False

def run_hirs_report_and_clear_cache(client_hostname):
    """Runs a hirs report for the specified client host name.
    The cached xml report is cleared.

    Returns the client output text from running the command.
    """

    logging.info("running hirs report over ssh on {0}".format(client_hostname))
    client_out = send_command("sudo hirs report",accept_nonzero_status=True)
    global CACHED_XML_REPORT
    if CACHED_XML_REPORT:
        logging.info("clearing cached XML report")
        CACHED_XML_REPORT = None
    return client_out

def run_hirs_provisioner_tpm_1_2(client_hostname):
    """Runs the hirs provisioner TPM 1.2"""
    logging.info("running hirs provisioner TPM 1.2 on {0}".format(client_hostname))
    client_out = send_command("hirs-provisioner provision")
    return client_out

def run_hirs_provisioner_tpm_2_0(client_hostname):
    """Runs the hirs provisioner TPM 2.0
    """
    logging.info("running hirs provisioner TPM 2.0 on {0}".format(client_hostname))
    client_out = send_command("hirs-provisioner-tpm2 provision")
    return client_out

def parse_xml_with_stripped_namespaces(raw_xml_string):
    """Parses the raw XML text in to an XML node element.
        Strips namespaces which conflict with recusive tree search.
    """
    it = ET.iterparse(StringIO(raw_xml_string))
    for _, el in it:
        if '}' in el.tag:
            el.tag = el.tag.split('}', 1)[1]  # strip all namespaces
        for at in el.attrib.keys(): # strip namespaces of attributes too
            if '}' in at:
                newat = at.split('}', 1)[1]
                el.attrib[newat] = el.attrib[at]
                del el.attrib[at]
    return it.root


def get_all_nodes_recursively(tree_node, node_name):
    return tree_node.findall('.//' + node_name)

def touch_random_file_and_remove(client_hostname):
    """Write a random string to a random filename in /tmp/, read it as root, then delete it.
    """
    random_number = str(int(random.random() * 100000))
    filename = "/tmp/on_demand_test_file{}.txt".format(random_number)

    echo_command = "echo {} > {}".format(random_number, filename)
    cat_command = "sudo cat {}".format(filename)
    sha_command = "sha1sum {}".format(filename)
    rm_command = "rm {}".format(filename)

    combined_command = "{};{};{};{}".format(echo_command, cat_command, sha_command, rm_command)
    sha_hash = command_output.split()[1]

    return (filename, sha_hash)

def make_simple_ima_baseline():
    timestamp = get_current_timestamp()

    if CLIENT_OS == "centos6":
        records = [{"path": "/lib/udev/console_init",
                    "hash": send_command_sha1sum("sha1sum /lib/udev/console_init")},
                   {"path": "/bin/mknod",
                    "hash": send_command_sha1sum("sha1sum /bin/mknod")}]
    elif CLIENT_OS == "centos7":
        records = [{"path": "/lib/systemd/rhel-readonly",
            "hash": send_command_sha1sum("sha1sum /lib/systemd/rhel-readonly")},
           {"path": "/bin/sort",
            "hash": send_command_sha1sum("sha1sum /bin/sort")}]
    elif CLIENT_OS == "ubuntu16":
        records = [{"path": "/lib/systemd/systemd-udevd",
            "hash": send_command_sha1sum("sha1sum /lib/systemd/systemd-udevd")},
           {"path": "/bin/udevadm",
            "hash": send_command_sha1sum("sha1sum /bin/udevadm")}]
    else:
        logging.error("unsupported client os type: %s",  CLIENT_OS)

    simple_baseline = {"name": "simple_ima_baseline_{0}".format(timestamp),
                       "description": "a simple hard-coded ima baseline for systems testing",
                       "records": records}
    return simple_baseline

def make_baseline_from_xml(xml_report, appraiser_type):
    """search the xml for records and add each one to a dictionary."""
    timestamp = get_current_timestamp()
    baseline_name = "full_{0}_baseline_{1}".format(appraiser_type, timestamp)
    baseline_description = "{0} baseline created by parsing an xml report and uploaded for systems testing".format(appraiser_type)
    baseline = {"name": baseline_name, "description": baseline_description}
    baseline["records"] = []
    tree = parse_xml_with_stripped_namespaces(xml_report)

    if appraiser_type == "TPM":
        pcr_tags = get_all_nodes_recursively(tree, "PcrValue")
        for pcr_tag in pcr_tags:
            tpm_digest = get_all_nodes_recursively(pcr_tag, "digest")[0].text
            parsed_record = {}
            parsed_record["pcr"] = pcr_tag.attrib['PcrNumber']
            parsed_record["hash"] = binascii.hexlify(binascii.a2b_base64(tpm_digest))
            baseline["records"].append(parsed_record)
    if appraiser_type == "IMA":
        ima_records = get_all_nodes_recursively(tree, "imaRecords")
        for ima_record in ima_records:
            ima_path = get_all_nodes_recursively(ima_record, "path")[0].text
            ima_digest = get_all_nodes_recursively(ima_record, "digest")[0].text
            parsed_record = {}
            parsed_record['path'] = ima_path
            hash64 = ima_digest
            parsed_record["hash"] = (
                binascii.hexlify(binascii.a2b_base64(hash64)))
            baseline["records"].append(parsed_record)
    logging.info("created {0} baseline from xml with {1} records".format(
                 appraiser_type, str(len(baseline["records"]))))
    return baseline

def make_simple_ima_blacklist_baseline():
    return {
            "name": "simple_ima_blacklist_baseline_{0}".format(get_current_timestamp()),
            "description": "a simple blacklist ima baseline for systems testing",
            "records": [{"path": "/boot/usb-storage-foo.ko"}]
            #"records": [{"path": "usb-storage-foo.ko"}]
    }

def make_simple_ima_blacklist_baseline_with_hash():
    return {
        "name": "simple_ima_blacklist_baseline_{0}".format(get_current_timestamp()),
        "description": "a simple blacklist ima baseline for systems testing",
        "records": [{"hash": USB_STORAGE_FILE_HASH}]
    }

def make_simple_ima_blacklist_baseline_with_file_and_hash():
    return {
        "name": "simple_ima_blacklist_baseline_{0}".format(get_current_timestamp()),
        "description": "a simple blacklist ima baseline for systems testing",
        "records": [{"path": "usb-storage_2.ko",
                     "hash": USB_STORAGE_FILE_HASH}]
    }

def make_simple_ima_blacklist_baseline_with_updated_file_and_hash():
    return {
        "name": "simple_ima_blacklist_baseline_{0}".format(get_current_timestamp()),
        "description": "a simple blacklist ima baseline for systems testing",
        "records": [{"path": "test-file",
                     "hash": USB_STORAGE_FILE_HASH_2}]
    }

def get_random_pcr_hex_value():
    """ Gets a random TPM PCR value by combining 2 UUIDs and getting a substring
    """
    # get 40 hex chars
    return str(binascii.b2a_hex(os.urandom(20)))

def get_current_timestamp():
    current_time = datetime.datetime.now()
    return current_time.strftime('%H-%M-%S')

def is_ubuntu_client(client_os):
    return client_os in ["ubuntu14", "ubuntu16"]

def is_tpm_1_2(tpm_version):
    return tpm_version in ["1.2"]

def is_tpm2(tpm_version):
    return tpm_version in ["2.0", "2"]
