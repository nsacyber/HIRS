# System Test Driver to help with debugging.

from __future__ import print_function
import logging
import os
import sys
import unittest
import urllib3

from system_test_core import DEFAULT_IMA_POLICY, DEFAULT_TPM_POLICY, \
    HIRSPortal, AttestationCAPortal, collectors, \
    send_command, send_command_sha1sum, run_hirs_report, run_hirs_provisioner_tpm_1_2, \
    run_hirs_provisioner_tpm_2_0, parse_xml_with_stripped_namespaces, get_current_timestamp, \
    get_all_nodes_recursively, touch_random_file_and_remove, get_random_pcr_hex_value, \
    is_ubuntu_client, is_tpm_2_0, is_tpm_1_2 \

NUMBER_OF_PCRS = 24

suffix = os.environ.get('RANDOM_SYS_TEST_ID')
if suffix != None:
    print("Configuring with suffix: %s" % suffix)
    suffix = "-" + suffix
else:
    suffix = ""

# Change to point to your HIRS directory
#HOME_DIR = "/HIRS/"
HOME_DIR = "/workspace/git/python2to3-dev-3/"
HIRS_ACA_PORTAL_IP="172.17.0.2"
TPM_VERSION="2.0"
#TPM_VERSION="1.2"
# Change accordingly
#COLLECTOR_LIST = None
#COLLECTOR_LIST = ["IMA"]
COLLECTOR_LIST = ["TPM"]
#COLLECTOR_LIST = ["IMA", "TPM"]
#COLLECTOR_LIST = ["BASE_DELTA_GOOD"]
#COLLECTOR_LIST = ["BASE_DELTA_BAD"]

FORMAT = "%(asctime)-15s %(message)s"
provisioner_out = None

HIRS_ACA_PROVISIONER_IP="172.19.0.3"
HIRS_ACA_PROVISIONER_TPM2_IP="172.19.0.4"
TPM_ENABLED=True
IMA_ENABLED=False

HIRS_ACA_PORTAL_PORT="8443"
HIRS_BROKER_PORT="61616"
HIRS_ACA_PORTAL_CONTAINER_PORT="80"
HIRS_ACA_HOSTNAME="hirsaca"
HIRS_SUBNET="172.19.0.0/16"
CLIENT_OS="centos7"
CLIENT_HOSTNAME="hirs-client-"+ CLIENT_OS + "-tpm2"
CLIENT=CLIENT_HOSTNAME
SERVER_OS="$CLIENT_OS"
SERVER_HOSTNAME="hirs-appraiser-$SERVER_OS"

HIRS_ATTESTATION_CA_PORTAL_URL = "https://" + \
    HIRS_ACA_PORTAL_IP + ":" + \
    HIRS_ACA_PORTAL_PORT + \
    "/HIRS_AttestationCAPortal/"

CA_CERT_LOCATION = HOME_DIR + ".ci/setup/certs/ca.crt"
EK_CA_CERT_LOCATION = HOME_DIR + ".ci/setup/certs/ek_cert.der"
PBaseCertA_LOCATION = HOME_DIR + "PBaseCertA.der"
PBaseCertB_LOCATION = HOME_DIR + "PBaseCertB.der"
SIDeltaCertA1_LOCATION = HOME_DIR + "SIDeltaCertA1.der"
SIDeltaCertA2_resolved_LOCATION = HOME_DIR + "SIDeltaCertA2_resolved.der"
SIDeltaCertA2_LOCATION = HOME_DIR + "SIDeltaCertA2.der"
SIDeltaCertA3_LOCATION = HOME_DIR + "SIDeltaCertA3.der"
VARDeltaCertA1_LOCATION = HOME_DIR + "VARDeltaCertA1.der"
VARDeltaCertA2_LOCATION = HOME_DIR + "VARDeltaCertA2.der"
VARDeltaCertA2_resolved_LOCATION = HOME_DIR + "VARDeltaCertA2_resolved.der"
SIDeltaCertB1_LOCATION = HOME_DIR + "SIDeltaCertB1.der"
VARDeltaCertB1_LOCATION = HOME_DIR + "VARDeltaCertB1.der"

TEST_LOG_FILE= HOME_DIR + ".ci/system-tests/test_logs/system_test_" + CLIENT_OS + ".log"
LOG_LEVEL="logging.INFO"

print("Start of Log file: " + TEST_LOG_FILE)
logging.basicConfig(filename=TEST_LOG_FILE,level=eval(LOG_LEVEL), format=FORMAT)
logging.info("*****************beginning of system_test.py*****************")
logging.info("The Collector list is: " + ' '.join(COLLECTOR_LIST))
logging.info("The ACA Portal is: " + HIRS_ATTESTATION_CA_PORTAL_URL)

#Portal = HIRSPortal(HIRS_SERVER_URL)
AcaPortal = AttestationCAPortal(HIRS_ATTESTATION_CA_PORTAL_URL)

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class SystemTest(unittest.TestCase):

    @classmethod
    def setUpClass(self):
        """Set the class up"""

    @classmethod
    def tearDownClass(self):
        """Tears down the class"""

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

    @collectors(['IMA'], COLLECTOR_LIST)
    def test_03_small_ima_appraisal(self):
        """Test that appraisal works with a small hard-coded IMA baseline"""
        logging.info("***************** Beginning of small IMA appraisal test *****************")

    @collectors(['IMA'], COLLECTOR_LIST)
    def test_04_large_ima_appraisal(self):
          """Test that appraisal works with a full-size IMA baseline"""
          logging.info("***************** Beginning of large IMA appraisal test *****************")

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

    @collectors(['TPM', 'IMA'], COLLECTOR_LIST)
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
        provisioner_out = run_hirs_provisioner_tpm2(CLIENT)
        print("Initial provisioner run output: {0}".format(provisioner_out))

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
