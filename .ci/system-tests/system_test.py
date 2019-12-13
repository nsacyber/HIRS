# system_test.py - implements a group of tests that run appraisals on a client and server

# TODO: test_01-test_11 will need to be implemented when the additional HIRS
# projects are imported to the new GitHub repo. The test code is commented out for now.

import binascii
from ConfigParser import SafeConfigParser
import datetime
import json
import os
import shlex
import subprocess
import unittest
import re
import requests
import logging
import pprint
import hashlib
import random
import uuid
import time
import sys
import argparse

from system_test_core import HIRSPortal, AttestationCAPortal, collectors, \
   send_command, send_command_sha1sum, run_hirs_report, run_hirs_provisioner_tpm_1_2, \
   run_hirs_provisioner_tpm_2_0, parse_xml_with_stripped_namespaces, \
   get_all_nodes_recursively, touch_random_file_and_remove, get_random_pcr_hex_value, \
   get_current_timestamp, is_ubuntu_client, is_tpm_2_0, is_tpm_1_2, \
   DEFAULT_IMA_POLICY, DEFAULT_TPM_POLICY, \
   make_simple_ima_baseline, make_baseline_from_xml, \
   make_simple_ima_blacklist_baseline, \
   make_simple_ima_blacklist_baseline_with_hash, \
   make_simple_ima_blacklist_baseline_with_file_and_hash, \
   make_simple_ima_blacklist_baseline_with_updated_file_and_hash

NUMBER_OF_PCRS = 24

suffix = os.environ.get('RANDOM_SYS_TEST_ID')
if suffix != None:
    print("Configuring with suffix: %s" % suffix)
    suffix = "-" + suffix
else:
    suffix = ""

COLLECTOR_LIST = os.environ.get('ENABLED_COLLECTORS').split(',')
CLIENT = os.environ.get('CLIENT_HOSTNAME')
CLIENT_OS = os.environ.get('CLIENT_OS')
TPM_VERSION = os.environ.get('TPM_VERSION')
HIRS_SERVER_URL = "https://TBD/HIRS_Portal/"
HIRS_ATTESTATION_CA_PORTAL_URL = "https://" + \
   os.environ.get('HIRS_ACA_PORTAL_IP') +":" + \
   os.environ.get('HIRS_ACA_PORTAL_PORT') + \
   "/HIRS_AttestationCAPortal/"
TEST_LOG_FILE = os.environ.get('TEST_LOG')
LOG_LEVEL = os.environ.get('LOG_LEVEL')

CA_CERT_LOCATION = "/HIRS/.ci/setup/certs/ca.crt"
EK_CA_CERT_LOCATION = "/HIRS/.ci/setup/certs/ek_cert.der"
PBaseCertA_LOCATION = "/var/hirs/pc_generation/PBaseCertA.der"
PBaseCertB_LOCATION = "/var/hirs/pc_generation/PBaseCertB.der"
SIDeltaCertA1_LOCATION = "/var/hirs/pc_generation/SIDeltaCertA1.der"
SIDeltaCertA2_LOCATION = "/var/hirs/pc_generation/SIDeltaCertA2.der"
SIDeltaCertA2_resolved_LOCATION = "/var/hirs/pc_generation/SIDeltaCertA2_resolved.der"
SIDeltaCertA3_LOCATION = "/var/hirs/pc_generation/SIDeltaCertA3.der"
VARDeltaCertA1_LOCATION = "/var/hirs/pc_generation/VARDeltaCertA1.der"
VARDeltaCertA2_LOCATION = "/var/hirs/pc_generation/VARDeltaCertA2.der"
VARDeltaCertA2_resolved_LOCATION = "/var/hirs/pc_generation/VARDeltaCertA2_resolved.der"
SIDeltaCertB1_LOCATION = "/var/hirs/pc_generation/SIDeltaCertB1.der"
VARDeltaCertB1_LOCATION = "/var/hirs/pc_generation/VARDeltaCertB1.der"

USB_STORAGE_FILE_HASH = "e164c378ceb45a62642730be5eb3169a6bfc2d6d"
USB_STORAGE_FILE_HASH_2 = "e164c378ceb45a62642730be5eb3169a6bfc1234"
FORMAT = "%(asctime)-15s %(message)s"
provisioner_out = None

logging.basicConfig(filename=TEST_LOG_FILE,level=eval(LOG_LEVEL), format=FORMAT)
logging.info("***************** Beginning of system_test.py *****************")
logging.info("The ACA Portal is: " + HIRS_ATTESTATION_CA_PORTAL_URL)

Portal = HIRSPortal(HIRS_SERVER_URL)
AcaPortal = AttestationCAPortal(HIRS_ATTESTATION_CA_PORTAL_URL)

requests.packages.urllib3.disable_warnings()

class SystemTest(unittest.TestCase):

   @classmethod
   def setUpClass(self):
      """Set the class up"""

   def setUp(self):
      """Set the systems tests state up for testing"""
      AcaPortal.disable_supply_chain_validations()

   def tearDown(self):
      """Tears down the state for testing"""

   def test_01_attestation_ca_portal_online(self):
      """Test that the Attestation CA Portal is online and accessible by making a GET request.
          If not online, an exception will be raised since the response code is non-200"""
      logging.info("***************** Beginning of attestation ca portal online test *****************")
      AcaPortal.check_is_online()

   @collectors(['IMA', 'TPM'], COLLECTOR_LIST)
   def test_02_empty_baselines(self):
      """Test that appraisal succeeds with empty IMA and TPM baselines"""
      logging.info("***************** Beginning of empty baseline test *****************")
# 		Portal.set_default_policies(ima_policy=DEFAULT_IMA_POLICY, tpm_policy=DEFAULT_TPM_POLICY)
# 		result = run_hirs_report(CLIENT)
# 		self.assertTrue(result)
# 		self.assertEqual(0, Portal.get_alert_count_from_latest_report())

   @collectors(['IMA'], COLLECTOR_LIST)
   def test_03_small_ima_appraisal(self):
      """Test that appraisal works with a small hard-coded IMA baseline

      steps:
        - upload a small hard-coded required set (two records)
        - make a policy that points to that baseline as its required set
        - set the default device group to point to that policy
        - run a report from the client machine using vagrant ssh
      """
      logging.info("***************** Beginning of small IMA appraisal test *****************")
# 		baseline = make_simple_ima_baseline()
# 		policy_name = Portal.add_ima_policy(required_set=baseline, policy_name_prefix='small_ima')
# 		Portal.set_default_policies(ima_policy=policy_name)
# 		result = run_hirs_report(CLIENT)
# 		self.assertTrue(result)

   @collectors(['IMA'], COLLECTOR_LIST)
   def test_04_large_ima_appraisal(self):
      """Test that appraisal works with a full-size IMA baseline

         steps:
          - generate an XML report or use a cached one
          - convert the IMA part of the report into a csv baseline
          - upload the csv file as an IMA baseline
          - make a policy that points to that baseline as its required set
          - set the default device group to point to that policy
          - run a report from the client machine using vagrant ssh
      """
      logging.info("***************** Beginning of large IMA appraisal test *****************")
# 		empty_ima_policy = Portal.add_ima_policy(required_set=None, policy_name_prefix="empty")
# 		Portal.set_default_policies(ima_policy=empty_ima_policy,
# 							  tpm_policy=DEFAULT_TPM_POLICY)
# 		run_hirs_report(CLIENT)
# 	 	xml_report = Portal.get_latest_report()
# 		baseline = make_baseline_from_xml(xml_report, "IMA")
# 		policy_name = Portal.add_ima_policy(required_set=baseline, unknown_fail="true", policy_name_prefix="large_ima")
# 	 	Portal.set_default_policies(ima_policy=policy_name)
# 		result = run_hirs_report(CLIENT)
# 		after_alerts = Portal.get_alerts_from_latest_report()
# 		new_alert_count = after_alerts['recordsTotal']
# 		logging.info("{0} new alerts generated by latest report".format(new_alert_count))
# 		if new_alert_count > 0:
# 		 	logging.warning("new alert count: " + str(new_alert_count))
# 			 #logging.debug("new alerts:\n{0}".format(pprint.pformat(after_alerts['data'][0:new_alert_count])))
# 		self.assertTrue(True)

   @collectors(['IMA'], COLLECTOR_LIST)
   def test_05_small_ima_appraisal_required_set_missing(self):
      """Test that appraisal results in an appropriate alert generation when a required set file is missing

         steps:
           - upload a small hard-coded required set (two records)
           - add a fictitious file to the baseline
           - make a policy that points to that baseline as its required set
           - set the default device group to point to that policy
           - run a report from the client machine using vagrant ssh
           - make sure it failed and that one appropriate alert was thrown
      """
      logging.info("***************** Beginning of small IMA appraisal test with required set missing *****************")
# 		baseline = make_simple_ima_baseline()
# 		baseline["name"] = "ima_baseline_missing_required_record_{0}".format(get_current_timestamp())
# 		random_hash = str(hashlib.sha1(str(random.random())).hexdigest())
# 		missing_file = "/required_directory/required_file"
# 		baseline["records"].append({"path": missing_file, "hash": random_hash})
# 		policy_name = Portal.add_ima_policy(required_set=baseline, policy_name_prefix="small_ima_req")
# 		Portal.set_default_policies(ima_policy=policy_name)
#
# 		result = run_hirs_report(CLIENT)
# 		self.assertFalse(result)
# 		after_alerts = Portal.get_alerts_from_latest_report()
# 		new_alert_count = after_alerts['recordsTotal']
# 		self.assertEqual(new_alert_count, 1)
#
# 		# find the alert with the most recent createTime
# 		latest_alert = max(after_alerts['data'], key=lambda alert: alert['createTime'])
# 		self.assertTrue("MISSING_RECORD" in latest_alert['type'])
# 		self.assertTrue(random_hash in latest_alert['expected'])
# 		self.assertTrue(missing_file in latest_alert['expected'])

   @collectors(['IMA'], COLLECTOR_LIST)
   def test_06_tpm_white_list_appraisal(self):
      """Test that appraisal works with a TPM white list baseline

         steps:
           - run hirs report to generate an XML report for baseline creation
           - download the latest report in XML format
           - convert the TPM part of the report into a json baseline
           - make a policy that points to that json TPM white list baseline
           - set the default device group to point to that policy
           - run a report from the client machine
       """
      logging.info("***************** Beginning of TPM white list appraisal test *****************")
# 		empty_ima_policy = Portal.add_ima_policy(required_set=None)
# 		Portal.set_default_policies(ima_policy=empty_ima_policy,
# 						  tpm_policy=DEFAULT_TPM_POLICY)
# 		result = run_hirs_report(CLIENT)
# 		self.assertTrue(result)
# 		xml_report = Portal.get_latest_report()
# 		baseline = make_baseline_from_xml(xml_report, "TPM")
# 		policy_name = Portal.add_tpm_wl_policy(baseline, policy_name_prefix="good")
# 		Portal.set_default_policies(tpm_policy=policy_name)
# 		result = run_hirs_report(CLIENT)
# 		self.assertTrue(result)
# 		self.assertEqual(0, Portal.get_alert_count_from_latest_report())
#
# 		# create a new baseline with random PCR values
# 		baseline_bad_tpm_pcr = make_baseline_from_xml(xml_report, "TPM")
# 		for pcr_index in range(0, NUMBER_OF_PCRS):
# 			baseline_bad_tpm_pcr["records"][pcr_index]["hash"] = get_random_pcr_hex_value()
#
# 		policy_name = Portal.add_tpm_wl_policy(baseline_bad_tpm_pcr, policy_name_prefix='bad_vals')
# 		Portal.set_default_policies(tpm_policy=policy_name)
# 		result = run_hirs_report(CLIENT)
# 		self.assertFalse(result)
# 		self.assertEqual(NUMBER_OF_PCRS, Portal.get_alert_count_from_latest_report())
#
# 		after_alerts = Portal.get_alerts()
#
# 		# for the set of new alerts, verify the alert fields for each PCR value
# 		# the order of the alerts it not necessarily PCR 0, 1, 2... , so we must index
# 		# in to the hash table correctly
# 		for alert_index in range(0, NUMBER_OF_PCRS):
# 			pcr_alert = after_alerts["data"][alert_index]
# 			alert_details = pcr_alert["details"]
# 			pcr_int = int(re.findall(r'\d+', alert_details)[0])
#
# 			logging.info("Checking TPM alert for PCR %s", pcr_int)
#
# 			self.assertTrue("WHITE_LIST_PCR_MISMATCH" in pcr_alert['type'])
# 			self.assertTrue("TPM_APPRAISER" in pcr_alert['source'])
# 			baseline_hash = baseline_bad_tpm_pcr["records"][pcr_int]["hash"]
# 			reported_hash = baseline["records"][pcr_int]["hash"]
#
# 			self.assertTrue(baseline_hash in pcr_alert['expected'])
# 			self.assertTrue(reported_hash in pcr_alert['received'])

   @collectors(['IMA'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_07_ima_blacklist_appraisal(self):
      """Test that appraisal works with a small IMA blacklist baseline

      steps:
        - upload a policy with a small hard-coded blacklist baseline
        - set the default device group to point to that policy
        - run a report from the client machine and ensure the appraisal passes
        - touch a file on the client that is contained in the blacklist
        - run another report from the client machine and ensure the appraisal fails
      """
      logging.info("***************** Beginning of blacklist IMA appraisal test *****************")
#      baseline = make_simple_ima_blacklist_baseline()
# 		policy_name = Portal.add_ima_policy(blacklist=baseline, policy_name_prefix='small_ima_blacklist')
# 		Portal.set_default_policies(ima_policy=policy_name)
#
# 		result = run_hirs_report(CLIENT)
# 		self.assertTrue(result)
#
# 		send_command('touch /boot/usb-storage-foo.ko')
# 		#send_command('sudo cat /tmp/usb-storage-foo.ko')
# 		result = run_hirs_report(CLIENT)
# 		self.assertFalse(result)
#
# 		after_alerts = Portal.get_alerts_from_latest_report()
# 		new_alert_count = after_alerts['recordsTotal']
# 		self.assertEqual(new_alert_count, 1)
#
# 		# find the alert with the most recent createTime
# 		latest_alert = after_alerts['data'][0]
# 		self.assertTrue("IMA_BLACKLIST_PATH_MATCH" in latest_alert['type'])
# 		self.assertTrue("usb-storage-foo.ko" in latest_alert['expected'])
#
# 		#
# 		# create ima blacklist baseline that contains a hash and generate alert upon detection
# 		#
#
# 		# create file and add content to file
# 		send_command('touch /tmp/usb-storage_2.ko')
# 		send_command('echo blacklist >> /tmp/usb-storage_2.ko')
# 		policy_name = Portal.add_ima_policy(blacklist=None,
# 											 policy_name_prefix='empty')
# 		Portal.set_default_policies(ima_policy=policy_name)
#
# 		# send report to verify successful appraisal
# 		result = run_hirs_report(CLIENT)
# 		self.assertTrue(result)
#
# 		 # create blacklist baseline with hash and update policy
# 		baseline = make_simple_ima_blacklist_baseline_with_hash();
# 		policy_name = Portal.add_ima_policy(blacklist=baseline,
# 											 policy_name_prefix='small_ima_blacklist_with_hash')
# 		Portal.set_default_policies(ima_policy=policy_name)
#
# 		# trigger measurement of file and run hirs report
# 		send_command('sudo cat /tmp/usb-storage_2.ko')
# 		result = run_hirs_report(CLIENT)
# 		self.assertFalse(result)
#
# 		after_alerts = Portal.get_alerts_from_latest_report()
# 		new_alert_count = after_alerts['recordsTotal']
# 		self.assertEqual(new_alert_count, 1)
#
# 		# find the alert with the most recent createTime
# 		latest_alert = after_alerts['data'][0]
# 		self.assertTrue("IMA_BLACKLIST_HASH_MATCH" in latest_alert['type'])
# 		self.assertTrue(USB_STORAGE_FILE_HASH in latest_alert['expected'])
#
# 		#
# 		# create ima blacklist baseline that contains a file and hash and generate alert upon detection
# 		#
# 		policy_name = Portal.add_ima_policy(blacklist=None,
# 											policy_name_prefix='empty')
# 		Portal.set_default_policies(ima_policy=policy_name)
#
# 		# send report to verify successful appraisal
# 		result = run_hirs_report(CLIENT)
# 		self.assertTrue(result)
#
# 		# create blacklist baseline with file and hash and update policy
# 		baseline = make_simple_ima_blacklist_baseline_with_file_and_hash();
# 		policy_name = Portal.add_ima_policy(blacklist=baseline,
# 											policy_name_prefix='small_ima_blacklist_with_file_and_hash')
# 		Portal.set_default_policies(ima_policy=policy_name)
#
# 		result = run_hirs_report(CLIENT)
# 		self.assertFalse(result)
#
# 		after_alerts = Portal.get_alerts_from_latest_report()
# 		new_alert_count = after_alerts['recordsTotal']
# 		self.assertEqual(new_alert_count, 1)
#
# 		# find the alert with the most recent createTime
# 		latest_alert = after_alerts['data'][0]
# 		self.assertTrue("IMA_BLACKLIST_PATH_AND_HASH_MATCH" in latest_alert['type'])
# 		self.assertTrue("usb-storage_2.ko" in latest_alert['expected'])
# 		self.assertTrue(USB_STORAGE_FILE_HASH in latest_alert['expected'])
#
# 		#
# 		# change ima blacklist baseline file and hash and verify alert is not generated
# 		#
#
# 		# create blacklist baseline with file and hash and update policy
# 		baseline = make_simple_ima_blacklist_baseline_with_updated_file_and_hash();
# 		policy_name = Portal.add_ima_policy(blacklist=baseline,
# 											policy_name_prefix='small_ima_blacklist_with_updated_file_and_hash')
# 		Portal.set_default_policies(ima_policy=policy_name)
#
# 		result = run_hirs_report(CLIENT)
# 		self.assertTrue(result)

   @collectors(['IMA'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_08_delta_reports_required_set(self):
      """Test that appraisal works with delta reports and required sets.

         steps:
         - Run hirs report with an empty required set and delta reports
           enabled
         - Check first report for success and to make sure the test files
           are not there
         - Add the two test files (foo-file and foo-bar-file) to the required
           set with a hashes that indicates the files are empty
         - create foo-file and read it as root so it is measured by IMA
         - Run second hirs report
         - Check for failed appraisal (foo-bar-file hasn't been created yet)
         - Check that the report includes foo-file, but not foo-bar-file
         - Create foo-bar-file and read it as root
         - Run third hirs report
         - Check for failed appraisal (foo-file was in the previous report,
           so it won't be included in this one.
         - Check that foo-bar-file is in this report, but not foo-file
      """

      logging.info("***************** Beginning of Delta Reports required set appraisal test *****************")
# 		unique_name = uuid.uuid4().hex
# 		baseline_name = 'delta-reports-required-baseline-' + unique_name
# 		foo_file_name = 'foo-file-' + unique_name
# 		foo_bar_file_name = 'foo-bar-file-' + unique_name
# 		test_hash = 'a94a8fe5ccb19ba61c4c0873d391e987982fbbd3'
#
# 		baseline = {"name": baseline_name,
# 					"description": "a simple hard-coded ima baseline "
# 					"for delta reports systems testing",
# 					"records": []}
#
# 		ima_policy = Portal.add_ima_policy(required_set=baseline, delta_reports_enabled="true", policy_name_prefix="delta_with_required_set")
# 		Portal.set_default_policies(ima_policy=ima_policy)
# 		run_hirs_report(CLIENT)
# 		report = Portal.get_latest_report()
# 		found_foo_file = foo_file_name in report
# 		found_foo_bar_file = foo_bar_file_name in report
# 		self.assertFalse(found_foo_file)
# 		self.assertFalse(found_foo_bar_file)
#
# 		Portal.add_to_ima_baseline(baseline_name, foo_file_name, test_hash)
# 		Portal.add_to_ima_baseline(baseline_name, foo_bar_file_name, test_hash)
#
# 		#create foo_file_name. Don't create foo_bar_file_name yet.
# 		#send_vagrant_command('echo {0} > {1}'.format("test", foo_file_name), CLIENT)
# 		#send_vagrant_command('sudo cat {0}'.format(foo_file_name), CLIENT)
# 		send_command('echo {0} > {1}'.format("test", foo_file_name))
# 		send_command('sudo cat {0}'.format(foo_file_name))
#
# 		result = run_hirs_report(CLIENT)
# 		self.assertFalse(result, msg="report should fail - " + foo_bar_file_name + " not present")
# 		report = Portal.get_latest_report()
# 		found_foo_file = foo_file_name in report
# 		found_foo_bar_file = foo_bar_file_name in report
# 		self.assertTrue(found_foo_file)
# 		self.assertFalse(found_foo_bar_file)
#
# 		send_vagrant_command('echo {0} > {1}'.format("test", foo_bar_file_name), CLIENT)
# 		send_vagrant_command('sudo cat {0}'.format(foo_bar_file_name), CLIENT)
# 		result = run_hirs_report(CLIENT)
# 		self.assertFalse(result, msg="delta reporting should fail becuase foo_file was in an earlier report")
# 		report = Portal.get_latest_report()
# 		found_foo_file = foo_file_name in report
# 		found_foo_bar_file = foo_bar_file_name in report
# 		self.assertFalse(found_foo_file)
# 		self.assertTrue(found_foo_bar_file)
#
# 		send_vagrant_command('rm {0}'.format(foo_file_name), CLIENT)
# 		send_vagrant_command('rm {0}'.format(foo_bar_file_name), CLIENT)

   @collectors(['IMA'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_09_delta_reports_whitelist(self):
      """Test that appraisal works with delta reports. Each report should be
         appraised individually. Checks that a failed appraisal can be followed
         by a successful appraisal if there are no errors in the second delta
         report.

         steps:
         - Run hirs report with an empty required set and delta reports
           enabled
         - Check first report for success and to make sure the test files
           are not there
         - Add a test file (foo-file) to the whitelist with a hash that
           indicates the file is empty
         - Create foo-file with contents and read it as root so it is
           measured by IMA
         - Run second hirs report
         - Check for failed appraisal (foo-file should be a whitelist
           mismatch because the file isn't empty)
         - Check that the report includes foo-file
         - Run third hirs report
         - Check for successful appraisal (the mismatch was in the previous
           report so it won't be included in this one.
         - Check that foo-file is not in this report
      """

      logging.info("***************** Beginning of Delta Reports whitelist appraisal test *****************")
# 		unique_name = uuid.uuid4().hex
# 		baseline_name = 'delta-reports-whitelist-baseline-' + unique_name
# 		foo_file_name = 'foo-file-' + unique_name
# 		foo_bar_file_name = 'foo-bar-file-' + unique_name
# 		test_hash = 'a94a8fe5ccb19ba61c4c0873d391e987982fbbd3'
#
# 		baseline = {"name": baseline_name,
# 					 "description": "a simple hard-coded ima baseline "
# 					 "for delta reports systems testing",
# 					 "records": []}
#
# 		ima_policy = Portal.add_ima_policy(whitelist=baseline, delta_reports_enabled="true", policy_name_prefix="delta_with_whitelist")
# 		Portal.set_default_policies(ima_policy=ima_policy)
# 		run_hirs_report(CLIENT)
# 		report = Portal.get_latest_report()
# 		found_foo_file = foo_file_name in report
# 		self.assertFalse(found_foo_file)
#
# 		Portal.add_to_ima_baseline(baseline_name, foo_file_name, test_hash)
#
# 		#create foo_file_name. Don't create foo_bar_file_name yet.
# 		send_vagrant_command('echo \'foo-file\' > {0}'.format(foo_file_name), CLIENT)
# 		send_vagrant_command('sudo cat {0}'.format(foo_file_name), CLIENT)
#
# 		result = run_hirs_report(CLIENT)
# 		self.assertFalse(result, msg="report should fail - whitelist mismatch for " + foo_bar_file_name)
# 		report = Portal.get_latest_report()
# 		found_foo_file = foo_file_name in report
# 		self.assertTrue(found_foo_file)
#
# 		result = run_hirs_report(CLIENT)
# 		self.assertTrue(result, msg="delta reporting should pass because the mismatched record should be found in a previous report")
# 		report = Portal.get_latest_report()
# 		found_foo_file = foo_file_name in report
# 		self.assertFalse(found_foo_file)
#
# 		send_vagrant_command('rm {0}'.format(foo_file_name), CLIENT)

   @collectors(['IMA', 'TPM'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_10_on_demand(self):
      """Test that on-demand (server-initiated) appraisal works.

         steps:
         - push a simple ima baseline
         - set the policy
         - touch a random file, take the hash, then remove it
         - kick off an on-demand report on the server for the default device group
         - sleep to let the appraisal finish
         - pull the generated report
            - check that it passed appraisal
            - check that it has the random filename and hash
            - check that it contains a TPM Report
         """
      logging.info("***************** Beginning of on-demand test *****************")
# 		baseline = make_simple_ima_baseline()
# 		policy_name = Portal.add_ima_policy(required_set=baseline, delta_reports_enabled="false", policy_name_prefix='on_demand')
# 		logging.info('on demand policy name: %s', policy_name)
# 		Portal.set_default_policies(ima_policy=policy_name, tpm_policy=DEFAULT_TPM_POLICY)
# 		first_report_summary = Portal.get_latest_report_summary()
#
# 		(filename, sha_hash) = touch_random_file_and_remove(CLIENT)
# 		partial_filename = filename.split('/')[-1]
# 		logging.info("touched file {} with hash {}".format(filename, sha_hash))
# 		Portal.start_on_demand()
# 		logging.info("started on-demand appraisal")
#
# 		latest_report_summary = None
#
# 		attempts = 0
# 		while latest_report_summary == None or latest_report_summary['report']['id'] == first_report_summary['report']['id']:
# 			 attempts += 1
# 			 time.sleep(20)
# 			 latest_report_summary = Portal.get_latest_report_summary()
# 			 if attempts == 6:
# 				 self.fail("No new report summary was found after 120 seconds; failing.")
#
# 		self.assertEqual(latest_report_summary["hirsAppraisalResult"]["appraisalStatus"], 'PASS')
#
# 		self.assertTrue(Portal.report_contains_ima_record(
# 			 partial_filename, sha_hash, latest_report_summary['report']['id']))
# 		sub_reports = latest_report_summary['report']['reports']
# 		self.assertTrue(any(sr for sr in sub_reports if 'TPMReport' in sr['reportType']),
# 						 "report summary should contain a TPMReport as a sub-report")

   @collectors(['IMA'], COLLECTOR_LIST)
   @unittest.skip("SELinux issues are preventing repo sync from working")
   def test_11_failing_ima_appraisal_broad_repo_baseline(self):
      """Test that an appraisal not containing expected packages in a broad repo IMA baseline fails.

         steps:
         - Create a Yum repository with a local file URL and sync it
         - Create a broad baseline using the Yum repository
         - Add the baseline to the required set for the default IMA policy
         - Run a HIRS report and ensure it fails
         - Ensure that at least one of the expected alerts has been generated
         """
      logging.info("***************** Beginning of broad repo failing appraisal test *****************")
# 		repo_name = "Test Yum Repository"
# 		baseline_name = "Test Broad Baseline"
# 		policy_name = "Test Broad Repo IMA Policy"
# 		repo_url = 'file:///flamethrower/Systems_Tests/resources/repositories/small_yum_repo'
#
# 		Portal.configure_yum_repository(repo_name, repo_url)
# 		Portal.create_broad_ima_baseline(baseline_name, repo_name)
# 		Portal.create_policy(policy_name, "IMA")
# 		Portal.add_baseline_to_required_sets(policy_name, baseline_name)
# 		Portal.set_tpm_ima_policy(ima_policy=policy_name, tpm_policy=DEFAULT_TPM_POLICY)
#
# 		self.assertFalse(run_hirs_report(CLIENT))
# 		alerts = Portal.get_alerts_from_latest_report()
# 		self.assertTrue(alerts_contain(alerts['data'], {
# 			 'source': 'IMA_APPRAISER',
# 			 'type': 'MISSING_RECORD',
# 			 'expected': '(/usr/lib64/glusterfs/3.7.6/xlator/features/quota.so, SHA-1 - 0xc9b5e8df6b50f2f58ea55fd41a962393d9eeec94)',
# 		}))

   @collectors(['IMA'], COLLECTOR_LIST)
   @unittest.skip("SELinux issues are preventing repo sync from working")
   @unittest.skipIf(is_ubuntu_client(CLIENT_OS), "Skipping this test due to client OS " + CLIENT_OS)
   def test_12_successful_ima_appraisal_broad_repo_baseline(self):
      """Test that an appraisal containing expected packages in a broad repo IMA baseline passes.
         This test only works on CentOS 6 and 7.

         steps:
         - Create a Yum repository with a local file URL and sync it
         - Create a broad baseline using the Yum repository
         - Add the baseline to the required set for the default IMA policy
         - Install RPMs in repository to client machine and read them with root to ensure their placement in the IMA log
         - Run a HIRS report and ensure it passes
         - Ensure that there are no new alerts
         """
      logging.info("***************** Beginning of broad repo successful appraisal test *****************")
# 		repo_name = "Test Yum Repository"
# 		baseline_name = "Test Broad Baseline"
# 		policy_name = "Test Broad Repo IMA Policy"
# 		repo_url = 'file:///flamethrower/Systems_Tests/resources/repositories/two_package_yum_repo'
#
# 		Portal.configure_yum_repository(repo_name, repo_url)
# 		Portal.create_broad_ima_baseline(baseline_name, repo_name)
# 		Portal.create_policy(policy_name, "IMA")
# 		Portal.add_baseline_to_required_sets(policy_name, baseline_name)
# 		Portal.set_partial_paths_for_ima_policy(policy_name, True)
# 		Portal.set_tpm_ima_policy(ima_policy=policy_name, tpm_policy=DEFAULT_TPM_POLICY)
#
# 		if CLIENT_OS in ["centos6", "centos7"]:
# 			 send_vagrant_command("sudo rpm -i --force /flamethrower/Systems_Tests/resources/repositories/two_package_yum_repo/SimpleTest1-1-1.noarch.rpm", CLIENT)
# 			 send_vagrant_command("sudo rpm -i --force /flamethrower/Systems_Tests/resources/repositories/two_package_yum_repo/SimpleTest2-1-1.noarch.rpm", CLIENT)
# 		else:
# 			 logging.error("unsupported client os: %s",  CLIENT_OS)
#
# 		send_vagrant_command("sudo find /opt/simpletest -type f -exec head {} \;", CLIENT)
#
# 		self.assertTrue(run_hirs_report(CLIENT))
# 		self.assertEqual(Portal.get_alert_count_from_latest_report(), 0)

   @collectors(['TPM'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_1_2(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_13_tpm_1_2_initial_provision(self):
      """Test that running the TPM 1.2 hirs provisioner works"""
      logging.info("***************** Beginning of initial TPM 1.2 provisioner run *****************")

      # Run the provisioner to ensure that it provisions successfully
      provisioner_out = run_hirs_provisioner_tpm_1_2(CLIENT)
      print("Initial TPM 1.2 provisioner run output: {0}".format(provisioner_out))

   @collectors(['TPM'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_14_tpm_2_0_initial_provision(self):
      """Test that running the TPM 2.0 hirs provisioner works"""
      logging.info("***************** Beginning of initial TPM 2.0 provisioner run *****************")

      # Run the provisioner to ensure that it provisions successfully
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)
      print("Initial TPM 2.0 provisioner run output: {0}".format(provisioner_out))

   @collectors(['TPM'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_15_device_info_report_stored_after_provisioning(self):
      """Test that running the hirs provisioner results in storing a device info report for
         the device in the DB"""
      logging.info("***************** Beginning of device info report test *****************")

      logging.info("Getting devices from ACA portal...")
      aca_portal_devices = AcaPortal.get_devices()
      self.assertEqual(aca_portal_devices['recordsTotal'], 1)

   @collectors(['TPM'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_16_supply_chain_validation_summary_stored_after_second_provisioning(self):
      """Test that running the hirs provisioner, a second time, results in storing a supply chain validation
         record in the database"""
      logging.info("***************** Beginning of supply chain validation summary test *****************")

      logging.info("Uploading CA cert: " + CA_CERT_LOCATION)
      AcaPortal.upload_ca_cert(CA_CERT_LOCATION)
      AcaPortal.enable_supply_chain_validations()

      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)
      print("Second provisioner run output: {0}".format(provisioner_out))

      supply_chain_validation_summaries = AcaPortal.get_supply_chain_validation_summaries()
      # verify this is one SCVS record indicating PASS
      self.assertEqual(supply_chain_validation_summaries['recordsTotal'], 2)
      self.assertEqual(supply_chain_validation_summaries['data'][0]['overallValidationResult'], "PASS")
      self.assertEqual(supply_chain_validation_summaries['data'][1]['overallValidationResult'], "PASS")

      # verify device has been updated with supply chain appraisal result
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

   @collectors(['TPM'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_17_ek_info_report(self):
      """Test that running the hirs provisioner results in storing EK certs info report for
         the device in the DB"""
      logging.info("***************** Beginning of Endorsement Certs info report test *****************")

      logging.info("Getting EK Certs from ACA portal...")
      cert_list = AcaPortal.get_ek_certs()
      self.assertEqual(cert_list['recordsTotal'], 1)
      self.assertEqual(cert_list['data'][0]['credentialType'], "TCPA Trusted Platform Module Endorsement")

   @collectors(['TPM'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_18_pk_info_report(self):
      """Test that running the hirs provisioner results in storing PK certs info report for
         the device in the DB"""
      logging.info("***************** Beginning Platform Certs info report test *****************")

      logging.info("Getting PK Certs from ACA portal...")
      cert_list = AcaPortal.get_pk_certs()
      self.assertEqual(cert_list['recordsTotal'], 1)
      self.assertEqual(cert_list['data'][0]['credentialType'], "TCG Trusted Platform Endorsement")

   @collectors(['TPM'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_19_trust_chain_info_report(self):
      """Test that running the hirs provisioner results in storing trust chains info report for
         the device in the DB"""
      logging.info("***************** Beginning of Trust Chain info report test *****************")
      logging.info("Getting Trust Chains from ACA portal...")
      trust_chain_list = AcaPortal.get_trust_chains()
      self.assertEqual(trust_chain_list['recordsTotal'], 1)

   @collectors(['BASE_DELTA_GOOD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_A1_base_delta(self):
      """Test Delta Certificates A1 - Provisioning with Good Base Platform Cert (via Platform Cert on TPM Emulator)"""
      logging.info("***************** test_20_A1 - Beginning of delta certificate test *****************")
      logging.info("Provisioning with Good Base Platform Cert (via Platform Cert on TPM Emulator)")

      logging.info("Check if ACA is online...")
      AcaPortal.check_is_online()

      logging.info("Uploading CA Cert: " + CA_CERT_LOCATION)
      AcaPortal.upload_ca_cert(CA_CERT_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)

      print("test_20_A1_base_delta run output: {0}".format(provisioner_out))

      # Verify device supply chain appraisal result is PASS
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

   @collectors(['BASE_DELTA_GOOD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_A2_base_delta(self):
      """Test Delta Certificates A2 - Attempt to upload Base cert with holder already having a Base Platform Cert associated with it"""
      logging.info("***************** test_20_A2 - Beginning of delta certificate test *****************")
      logging.info("Attempt to upload PBaseCertB, with PBaseCertA already loaded in the ACA.")

      print("test_20_A2_base_delta. PBaseCertA has already been loaded. Attempting to upload second Platform Cert: %s" % (PBaseCertB_LOCATION))

      # Confirm there is one Platform Base Cert already loaded
      cert_list = AcaPortal.get_pk_certs()
      self.assertEqual(cert_list['recordsTotal'], 1)
      print("Number of Platform Certs: %d" % (cert_list['recordsTotal']))
      self.assertEqual(cert_list['data'][0]['credentialType'], "TCG Trusted Platform Endorsement")
      self.assertEqual(cert_list['data'][0]['platformType'], "Base")

      # Try uploading a second Platform Base Cert
      print("Attempting to upload a second Platform Base Cert...")
      AcaPortal.upload_pk_cert(PBaseCertB_LOCATION)

      # Confirm Platform Base Cert has not been loaded
      cert_list = AcaPortal.get_pk_certs()
      self.assertEqual(cert_list['recordsTotal'], 1)
      print("Number of Platform Certs: %d" % (cert_list['recordsTotal']))
      self.assertEqual(cert_list['data'][0]['credentialType'], "TCG Trusted Platform Endorsement")
      self.assertEqual(cert_list['data'][0]['platformType'], "Base")

      if (cert_list['recordsTotal'] == 1):
         print ("SUCCESS.\n")
      else:
         print ("FAILED.\n")

   @collectors(['BASE_DELTA_GOOD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_A3_base_delta(self):
      """Test Delta Certificates A3 - Provisioning with Good Base Platform Cert Base and 1 Delta Cert"""
      logging.info("***************** test_20_A3 - Beginning of delta certificate test *****************")
      logging.info("Provisioning with Good Base Platform Cert Base and 1 Delta Cert")

      # Verify device supply chain appraisal result is PASS
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

      # Upload the SIDeltaCertA1 and provision
      AcaPortal.upload_pk_cert(SIDeltaCertA1_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)
      print("test_20_A3_base_delta run output: {0}".format(provisioner_out))

      supply_chain_validation_summaries = AcaPortal.get_supply_chain_validation_summaries()
      # Verify this is one SCVS record indicating PASS
      self.assertEqual(supply_chain_validation_summaries['recordsTotal'], 2)
      self.assertEqual(supply_chain_validation_summaries['data'][0]['overallValidationResult'], "PASS")
      self.assertEqual(supply_chain_validation_summaries['data'][1]['overallValidationResult'], "PASS")

      # Verify device has been updated with supply chain appraisal result
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

   @collectors(['BASE_DELTA_GOOD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_A4_base_delta(self):
      """Test Delta Certificates A4 - Provisioning with Good Base Platform Cert Base and 2 Delta Certs"""
      logging.info("***************** test_20_A4 - Beginning of delta certificate test *****************")
      logging.info("Provisioning with Good Base Platform Cert Base and 2 Delta Certs")

      # Verify device supply chain appraisal result is PASS
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

      # Upload the VARDeltaCertA1 and provision
      AcaPortal.upload_pk_cert(VARDeltaCertA1_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)

      print("test_20_A4_base_delta run output: {0}".format(provisioner_out))
      supply_chain_validation_summaries = AcaPortal.get_supply_chain_validation_summaries()

      # Verify this is one SCVS record indicating PASS
      self.assertEqual(supply_chain_validation_summaries['recordsTotal'], 3)
      self.assertEqual(supply_chain_validation_summaries['data'][0]['overallValidationResult'], "PASS")
      self.assertEqual(supply_chain_validation_summaries['data'][1]['overallValidationResult'], "PASS")
      self.assertEqual(supply_chain_validation_summaries['data'][2]['overallValidationResult'], "PASS")

      # Verify device has been updated with supply chain appraisal result
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

   @collectors(['BASE_DELTA_GOOD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_A5_base_delta(self):
      """Test Delta Certificates A5 - Provisioning with Good Base Platform Cert and 1 Bad Delta Cert"""
      logging.info("***************** test_20_A5 - Beginning of delta certificate test *****************")
      logging.info("Provisioning with Good Base Platform Cert and 1 Bad Delta Cert")

      # TODO: Determine if we need this test

#		 # Verify device supply chain appraisal result is PASS
#		 devices = AcaPortal.get_devices()
#		 self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")
#
#		 # Upload the VARDelta cert and provision
#		 AcaPortal.upload_pk_cert(SIDeltaCertA2_LOCATION)
#		 AcaPortal.enable_supply_chain_validations()
#		 provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)
#
#		 print("test_19_A4_base_delta SHOULD FAIL provisioning!!")
#		 print("test_19_A4_base_delta run output: {0}".format(provisioner_out))
#
#		 # Provisioning should fail since the Delta contains a bad component.
#		 self.assertIn("Provisioning failed", format(provisioner_out))

   @collectors(['BASE_DELTA_GOOD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_A6_base_delta(self):
      """Test Delta Certificates A6 - Provisioning with Good Base Platform, 2 Good Delta Certs and 1 Bad Delta Cert"""
      logging.info("***************** test_20_A6 - Beginning of delta certificate test *****************")
      logging.info("Provisioning with Good Base Platform, 2 Good Delta Certs and 1 Bad Delta Cert")

      # Verify device supply chain appraisal result is PASS
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

      # Upload the SIDeltaCertA2 and provision
      AcaPortal.upload_pk_cert(SIDeltaCertA2_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)

      print("test_20_A6_base_delta SHOULD FAIL provisioning using: %s" % (SIDeltaCertA2_LOCATION))
      print("test_20_A6_base_delta run output: {0}".format(provisioner_out))

      # Provisioning should fail since the Delta contains a bad component.
      self.assertIn("Provisioning failed", format(provisioner_out))

      # Upload the SIDeltaCertA2_resolved and provision
      AcaPortal.upload_pk_cert(SIDeltaCertA2_resolved_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)

      print("test_20_A6_base_delta SHOULD PASS provisioning using: %s" % (SIDeltaCertA2_resolved_LOCATION))
      print("test_20_A6_base_delta run output: {0}".format(provisioner_out))

       # Verify device has been updated with supply chain appraisal result
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

   @collectors(['BASE_DELTA_GOOD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_A7_base_delta(self):
      """Test Delta Certificates A7 - Provisioning with Good Base Platform, 2 Good Delta Certs and
         1 Bad Delta Cert with non present component"""
      logging.info("***************** test_20_A7 - Beginning of delta certificate test *****************")
      logging.info("Provisioning with Good Base Platform, 2 Good Delta Certs and 1 Bad Delta Cert with non present component")

      # Upload the VARDeltaCertA2 and provision
      AcaPortal.upload_pk_cert(VARDeltaCertA2_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)

      print("test_20_A7_base_delta SHOULD FAIL provisioning using: %s" % (VARDeltaCertA2_LOCATION))
      print("test_20_A7_base_delta run output: {0}".format(provisioner_out))

      # Provisioning should fail since the Delta contains a component thats not in the Base
      self.assertIn("Provisioning failed", format(provisioner_out))

      # Upload the VARDeltaCertA2_resolved and provision
      AcaPortal.upload_pk_cert(VARDeltaCertA2_resolved_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)

      print("test_20_A7_base_delta SHOULD PASS provisioning using: %s" % (VARDeltaCertA2_resolved_LOCATION))
      print("test_20_A7_base_delta run output: {0}".format(provisioner_out))

       # Verify device has been updated with supply chain appraisal result
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

   @collectors(['BASE_DELTA_GOOD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_A8_base_delta(self):
      """Test Delta Certificates A8 - Provisioning with Good Base Platform, 2 Good Delta Certs with 1 Delta cert
         replacing component from previous, using the Delta as a base certificate"""
      logging.info("***************** test_20_A8 - Beginning of delta certificate test *****************")
      logging.info("Provisioning with Good Base Platform, 2 Good Delta Certs with 1 Delta cert replacing component from previous, using the Delta as a base certificate")

      # Upload the SIDeltaCertA3 and provision
      AcaPortal.upload_pk_cert(SIDeltaCertA3_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)

      print("test_20_A8_base_delta run output: {0}".format(provisioner_out))

      # Verify device has been updated with supply chain appraisal result
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

   @collectors(['BASE_DELTA_BAD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_B1_base_delta(self):
      """Test Base/Delta Certificates B1 - Provisioning with Bad Platform Cert Base """
      logging.info("***************** test_20_B1 - Beginning of delta certificate test *****************")
      logging.info("Provisioning with Bad Platform Cert Base")

      logging.info("Check if ACA is online...")
      AcaPortal.check_is_online()

      logging.info("Uploading CA cert: " + CA_CERT_LOCATION)
      AcaPortal.upload_ca_cert(CA_CERT_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)

      print("test_20_B1_base_delta SHOULD FAIL provisioning using: %s" % (PBaseCertB_LOCATION))
      print("test_20_B1_base_delta run output: {0}".format(provisioner_out))

      # Provisioning should fail since the PC contains FAULTY components.
      self.assertIn("Provisioning failed", format(provisioner_out))

   @collectors(['BASE_DELTA_BAD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_B2_base_delta(self):
      """Test Base/Delta Certificates B2 - Provisioning with Bad Platform Cert Base and 1 Good delta with 1 bad component unresolved"""
      logging.info("***************** test_20_B2 - Beginning of delta certificate test *****************")
      logging.info("Provisioning with Bad Platform Cert Base and 1 Good delta with 1 bad component unresolved")

      # Verify device supply chain appraisal result is FAIL
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "FAIL")

      # Upload the SIDeltaCertB1 and provision
      AcaPortal.upload_pk_cert(SIDeltaCertB1_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)

      print("test_20_B2_base_delta SHOULD FAIL provisioning using: %s" % (SIDeltaCertB1_LOCATION))
      print("test_20_B2_base_delta run output: {0}".format(provisioner_out))

      # Provisioning should fail since the delta contains FAULTY component.
      self.assertIn("Provisioning failed", format(provisioner_out))

   @collectors(['BASE_DELTA_BAD'], COLLECTOR_LIST)
   @unittest.skipIf(not is_tpm_2_0(TPM_VERSION), "Skipping this test due to TPM Version " + TPM_VERSION)
   def test_20_B3_base_delta(self):
      """Test Base/Delta Certificates B3 - Provisioning with Bad Platform Cert Base and 2 Good delta with all component resolved"""
      logging.info("***************** test_20_B3 - Beginning of delta certificate test *****************")
      logging.info("Provisioning with Bad Platform Cert Base and 2 Good delta with all component resolved")

      # Verify device supply chain appraisal result is FAIL
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "FAIL")

      # Upload the VARDeltaCertB1 and provision
      AcaPortal.upload_pk_cert(VARDeltaCertB1_LOCATION)
      AcaPortal.enable_supply_chain_validations()
      provisioner_out = run_hirs_provisioner_tpm_2_0(CLIENT)

      print("test_20_B3_base_delta run output: {0}".format(provisioner_out))

      # Verify device has been updated with supply chain appraisal of PASS
      devices = AcaPortal.get_devices()
      self.assertEqual(devices['data'][0]['device']['supplyChainStatus'], "PASS")

if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(SystemTest)
    ret = not unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful()
    sys.exit(ret)
