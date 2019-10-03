/**
 * Copyright (C) 2017-2018, U.S. Government
 */

/**
 * Main entry point for the TPM2_Provisioner. Handles the input from the
 * command line application and provisions the client for use with an
 * attestation credential authority.
*/
#include <unistd.h>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>
#include <Process.h>

#include "log4cplus/configurator.h"

#include "CommandTpm2.h"
#include "DeviceInfoCollector.h"
#include "HirsRuntimeException.h"
#include "RestfulClientProvisioner.h"
#include "Utils.h"
#include "Version.h"


using hirs::exception::HirsRuntimeException;
using hirs::file_utils::dirExists;
using hirs::log::Logger;
using hirs::tpm2::AsymmetricKeyType;
using hirs::tpm2::CommandTpm2;
using hirs::tpm2_tools_utils::Tpm2ToolsVersion;
using hirs::utils::Process;
using std::cout;
using std::cerr;
using std::endl;
using std::string;
using std::stringstream;

int provision() {
    Logger logger = Logger::getDefaultLogger();

    CommandTpm2 tpm2;
    tpm2.setAuthData();

    // get endorsement credential and endorsement key
    cout << "----> Collecting endorsement credential from TPM" << endl;
    string endorsementCredential = tpm2.getEndorsementCredentialDefault(
            AsymmetricKeyType::RSA);
    tpm2.createEndorsementKey();
    string ekPublicArea = tpm2.getEndorsementKeyPublicArea();

    // get attestation key
    cout << "----> Creating attestation key" << endl;
    tpm2.createAttestationKey();
    string akPublicArea = tpm2.getAttestationKeyPublicArea();

    // get platform credential
    cout << "----> Collecting platform credential from TPM" << endl;
    string platformCredential = tpm2.getPlatformCredentialDefault();
    std::vector<string> platformCredentials;
    platformCredentials.push_back(platformCredential);

    // collect device info
    cout << "----> Collecting device information" << endl;
    hirs::pb::DeviceInfo dv = DeviceInfoCollector::collectDeviceInfo();

    // send identity claim
    cout << "----> Sending identity claim to Attestation CA" << endl;
    hirs::pb::IdentityClaim identityClaim
            = tpm2.createIdentityClaim(dv, akPublicArea, ekPublicArea,
                                       endorsementCredential,
                                       platformCredentials);
    identityClaim.set_client_version(CLIENT_VERSION);
    string paccorOutputString =
            hirs::utils::Process::run(
                    "/opt/paccor/scripts/allcomponents.sh", "",
                    "TPM2_Provisioner.cpp", __LINE__);
    identityClaim.set_paccoroutput(paccorOutputString);
    RestfulClientProvisioner provisioner;
    string nonceBlob = provisioner.sendIdentityClaim(identityClaim);
    if (nonceBlob == "") {
        cout << "----> Provisioning failed.";
        cout << "Please refer to the Attestation CA for details." << endl;
        return 0;
    }

    // activateIdentity requires we read makeCredential output from a file
    cout << "----> Received response. Attempting to decrypt nonce" << endl;
    try {
        hirs::file_utils::writeBinaryFile(
                nonceBlob, CommandTpm2::kDefaultIdentityClaimResponseFilename);
    } catch (const std::invalid_argument& e) {
        logger.error(e.what());
        throw HirsRuntimeException("Provisioning failed.",
                                   "TPM2_Provisioner::provision");
    }
    string decryptedNonce = tpm2.activateIdentity();

    cout << "----> Nonce successfully decrypted. Sending attestation "
         << "certificate request" << endl;
    hirs::pb::CertificateRequest certificateRequest;
    certificateRequest.set_nonce(decryptedNonce);
    certificateRequest.set_quote(tpm2.getQuote("nothing", decryptedNonce));
    const string& akCertificateByteString
            = provisioner.sendAttestationCertificateRequest(certificateRequest);

    cout << "----> Storing attestation key certificate" << endl;
    tpm2.storeAKCertificate(akCertificateByteString);
    return 1;
}

void printHelp() {
    stringstream helpMessage;
    helpMessage << "TPM 2.0 Provisioner\n"
                << "Version " << CLIENT_VERSION << "\n\n"
                << "To run the provisioning process, "
                << "enter hirs-provisioner-tpm2 provision\n";
    cout << helpMessage.str() << endl;
}

int main(int argc, char** argv) {
    string log_directory = "/var/log/hirs/provisioner";

    // directory should be created by rpm install
    if (!dirExists(log_directory)) {
        cerr << "Log directory /var/log/hirs/provisioner does not "
             << "exist. Exiting";
        return 1;
    }

    log4cplus::initialize();
    log4cplus::PropertyConfigurator::doConfigure(
            "/etc/hirs/TPM2_Provisioner/log4cplus_config.ini");
    Logger mainLogger = Logger::getDefaultLogger();

    if (argc < 2) {
        printHelp();
        return 0;
    }
    string argument = argv[1];
    if (argument == "provision") {
        // Ensure we're running as root
        if (getuid() != 0) {
            string error = "Program must be run as root. Exiting";
            cerr << (error);
            mainLogger.error(error);
            return 1;
        }
        // Ensure either tpm2-abrmd or the old resourcemgr is running
        if (!Process::isRunning("tpm2-abrmd")
            && !Process::isRunning("resourcemgr")) {
            stringstream errorStream;
            errorStream << R"(Neither "tpm2-abmrd" nor the older )"
                        << R"("resourcemgr" daemon is currently running. )"
                        << "\nPlease ensure either is running before "
                        << "attempting provisioning.\n";
            cerr << (errorStream.str());
            mainLogger.error(errorStream.str());
            return 1;
        }
        cout << "--> Provisioning" << endl;
        try {
            if (provision()) {
                cout << "----> Provisioning successful" << endl;
            }
        } catch (HirsRuntimeException& hirsRuntimeException) {
            mainLogger.error(hirsRuntimeException.what());
            cout << "----> Fatal error during provisioning. See "
                 << "/var/log/hirs/provisioner/HIRS_ProvisionerTPM2.log for "
                         "details" << endl;
            return 1;
        }
    } else {
        printHelp();
    }

    log4cplus::Logger::shutdown();
}
