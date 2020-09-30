/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <HirsRuntimeException.h>
#include <RestfulClientProvisioner.h>
#include <Utils.h>
#include <Properties.h>

#include <cpr/cpr.h>

#include <sstream>
#include <string>
#include <utility>

using hirs::exception::HirsRuntimeException;
using hirs::log::Logger;
using hirs::pb::IdentityClaim;
using hirs::pb::IdentityClaimResponse;
using hirs::pb::CertificateRequest;
using hirs::pb::CertificateResponse;
using hirs::properties::Properties;
using hirs::json_utils::JSONFieldParser;
using hirs::string_utils::binaryToHex;
using std::string;
using std::stringstream;
using std::to_string;

const Logger RestfulClientProvisioner::LOGGER = Logger::getDefaultLogger();
const char * const RestfulClientProvisioner::PROP_FILE_LOC =
        "/etc/hirs/hirs-site.config";
const char * const RestfulClientProvisioner::PROP_ACA_FQDN
        = "ATTESTATION_CA_FQDN";
const char * const RestfulClientProvisioner::PROP_ACA_PORT
        = "ATTESTATION_CA_PORT";
const char * const RestfulClientProvisioner::ACA_ERROR_FIELDNAME
        = "error";

RestfulClientProvisioner::RestfulClientProvisioner() {
    Properties props(PROP_FILE_LOC);
    acaAddress = props.get(PROP_ACA_FQDN, "localhost");
    port = std::stoi(props.get(PROP_ACA_PORT, "8443"));
}

RestfulClientProvisioner::RestfulClientProvisioner(
        const std::string& acaAddress, int acaPort)
        : acaAddress(acaAddress), port(acaPort) {
}

string RestfulClientProvisioner::getAcaAddress() {
    return acaAddress;
}

string RestfulClientProvisioner::sendIdentityClaim(
        IdentityClaim identityClaim) {
    {
        stringstream logStream;
        logStream << "Sending the identity claim to " << acaAddress
                  << " on port " << port;
        LOGGER.info(logStream.str());
    }

    string identityClaimByteString;
    identityClaim.SerializeToString(&identityClaimByteString);

    // Send serialized Identity Claim to ACA
    LOGGER.info("Sending Serialized Identity Claim Binary");
    auto r = cpr::Post(cpr::Url{"https://" + acaAddress + ":" + to_string(port)
                                + "/HIRS_AttestationCA/identity-claim-tpm2/"
                                + "process"},
                       cpr::Body{identityClaimByteString},
                       cpr::Header{{"Content-Type",
                                           "application/octet-stream"},
                                   {"Accept",
                               "application/octet-stream, application/json"}},
                       cpr::VerifySsl{false});

    // Check ACA response, should be 200 if successful
    if (r.status_code == 200) {
        if (r.text.size() == 0) {
            return "";
        }

        IdentityClaimResponse response;
        response.ParseFromString(r.text);

        {
            // Convert the nonce blob to hex for logging
            string blobHex = binaryToHex(response.credential_blob());
            stringstream logStream;
            logStream << "Received nonce blob: " << blobHex;
            LOGGER.info(logStream.str());
        }

        // Return the wrapped nonce blob
        return response.credential_blob();

    } else {
        stringstream errormsg;
        errormsg << "Error communicating with ACA server. "
                 << "Received response code: " << to_string(r.status_code)
                 << "\n\nError message from ACA was: "
                 << JSONFieldParser::parseJsonStringField(r.text,
                                                          ACA_ERROR_FIELDNAME);
        throw HirsRuntimeException(errormsg.str(),
                                "RestfulClientProvisioner::sendIdentityClaim");
    }
}

string RestfulClientProvisioner::sendAttestationCertificateRequest(
        CertificateRequest certificateRequest) {
    string certificateRequestByteString;
    certificateRequest.SerializeToString(&certificateRequestByteString);

    // Send serialized certificate request to ACA
    LOGGER.info("Sending Serialized DeviceInfo Binary");
    auto r = cpr::Post(cpr::Url{"https://" + acaAddress + ":" + to_string(port)
                                + "/HIRS_AttestationCA"
                                + "/request-certificate-tpm2"},
                       cpr::Body{certificateRequestByteString},
                       cpr::Header{{"Content-Type",
                                           "application/octet-stream"},
                                   {"Accept",
                               "application/octet-stream, application/json"}},
                       cpr::VerifySsl{false});

    // Check ACA response, should be 200 if successful
    if (r.status_code == 200) {
        CertificateResponse response;
        response.ParseFromString(r.text);

        {
            // Convert the certificate to hex for logging
            string certificateHex = binaryToHex(response.certificate());
            stringstream logStream;
            logStream << "Received public certificate: " << certificateHex;
            LOGGER.info(logStream.str());
        }

        // Return the public attestation certificate
        return response.certificate();

    } else {
        stringstream errormsg;
        errormsg << "Error communicating with ACA server. "
                 << "Received response code: " << to_string(r.status_code)
                 << "\n\nError message from ACA was: "
                 << JSONFieldParser::parseJsonStringField(r.text,
                                                          ACA_ERROR_FIELDNAME);
        throw HirsRuntimeException(errormsg.str(),
                "RestfulClientProvisioner::sendAttestationCertificateRequest");
    }
}
