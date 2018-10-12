/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#ifndef HIRS_PROVISIONERTPM2_INCLUDE_RESTFULCLIENTPROVISIONER_H_
#define HIRS_PROVISIONERTPM2_INCLUDE_RESTFULCLIENTPROVISIONER_H_

#include <Logger.h>
#include <ProvisionerTpm2.pb.h>

#include <string>

/**
 * Manages the sending of messages to the ACA and their relevant replies.
 */
class RestfulClientProvisioner {
 private:
    // Logger
    static const hirs::log::Logger LOGGER;

    static const char * const PROP_FILE_LOC;
    static const char * const PROP_ACA_FQDN;
    static const char * const PROP_ACA_PORT;
    static const char * const ACA_ERROR_FIELDNAME;


    /**
     * IP address of ACA
     */
    std::string acaAddress;
    /**
     * Port used by the ACA to service requests.
     */
    int port;

 public:
    RestfulClientProvisioner();

    RestfulClientProvisioner(const std::string& acaAddress, int acaPort);

    /**
     * Return the IP address of the ACA
     * @return the IP address of the ACA
     */
    std::string getAcaAddress();

    /**
     * Sends the identity claim to the ACA to initiate the identity claim
     * procedure. Return the wrapped challenge nonce reply from the ACA for
     * decoding with TPM2_ActivateCredential.
     *
     * @param identityClaim request containing deviceInfo, EK public area
     *  AK public area and optionally an Endorsement and/or Platform Credential
     * @returns the byte-encoded encrypted nonce blob
     */
    std::string sendIdentityClaim(hirs::pb::IdentityClaim identityClaim);

    /**
     * Sends the request to get the public Attestation Certificate from the
     * ACA. Contains the decrypted nonce returned from
     * RestfulClientProvisioner::sendIdentityClaim.
     *
     * @param certificateRequest request containing nonce
     * @return the byte-encoded public attestation certificate
     */
    std::string sendAttestationCertificateRequest(
            hirs::pb::CertificateRequest certificateRequest);
};

#endif  // HIRS_PROVISIONERTPM2_INCLUDE_RESTFULCLIENTPROVISIONER_H_
