/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#ifndef HIRS_PROVISIONERTPM2_INCLUDE_COMMANDTPM2_H_
#define HIRS_PROVISIONERTPM2_INCLUDE_COMMANDTPM2_H_

#include <Logger.h>
#include <ProvisionerTpm2.pb.h>
#include <Tpm2ToolsUtils.h>

#include <Tss.h>

#include <string>
#include <vector>

namespace hirs {
namespace tpm2 {

enum class AsymmetricKeyType {
    RSA,
    ECC
};

/**
 * Manages the issuing of commands to tpm2-tools executables.
 */
class CommandTpm2 {
 private:
    static const hirs::log::Logger LOGGER;

    static const int kMaxRetryCommandAttempts;

    static const char* const kAKCertificateHandle;
    static const char* const kTpm2ToolsTakeOwnershipCommand;
    static const char* const kTpm2ToolsNvDefineCommand;
    static const char* const kTpm2ToolsNvListCommand;
    static const char* const kTpm2ToolsNvReadCommand;
    static const char* const kTpm2ToolsNvReleaseCommand;
    static const char* const kTpm2ToolsNvWriteCommand;
    static const char* const kTpm2ToolsGetPubAkCommand;
    static const char* const kTpm2ToolsGetPubEkCommand;
    static const char* const kTpm2ToolsListPersistentCommand;
    static const char* const kTpm2ToolsReadPublicCommand;
    static const char* const kTpm2ToolsActivateCredential;
    static const char* const kTpm2ToolsEvictControlCommand;
    static const char* const kTpm2RetryCommandCode;
    static const char* const kWellKnownSecret;
    static const char* const kRsaAlgorithmId;
    static const char* const kEccAlgorithmId;
    static const char* const kDefaultAttributeValue;
    static const char* const kDefaultOwnerAuthHandle;
    static const char* const kDefaultRsaEkCredentialHandle;
    static const char* const kDefaultEccEkCredentialHandle;
    static const char* const kDefaultPlatformCredentialHandle;
    static const char* const kDefaultEkHandle;
    static const char* const kDefaultAkHandle;
    static const char* const kDefaultAkCertFilename;
    static const char* const kDefaultAkNameFilename;
    static const char* const kDefaultAkPubFilename;
    static const char* const kDefaultEkPubFilename;
    static const char* const kTpm2ToolsGetQuoteCommand;
    static const char* const kTpm2DefaultQuoteFilename;
    static const char* const kTpm2DefaultSigFilename;
    static const char* const kTpm2DefaultSigAlgorithm;

    const hirs::tpm2_tools_utils::Tpm2ToolsVersion version;

    uint16_t getNvIndexDataSize(const std::string& nvIndex);

    std::string readNvIndex(const std::string& beginNvIndex,
                            const uint16_t& dataSize);

    bool hasNvIndexDefined(const std::string& nvIndex);

    void releaseNvIndex(const std::string& nvIndex);

    std::string createNvReadCommandArgs(const std::string& nvIndexValue,
                                        const uint16_t& offset,
                                        const uint16_t& readSize);

    std::string createNvWriteCommandArgs(const std::string& nvIndexValue,
                                         const std::string& writeFile);

    bool hasPersistentObject(const std::string& handle);

    void flushPersistentObject(const std::string& handle);

    void createPublicAreaFile(const std::string& keyHandle,
                              const std::string& filename);

    std::string getPublicArea(const std::string& filename);

    std::string runTpm2CommandWithRetry(const std::string& command,
                                            const std::string& args,
                                            int sourceCodeLineNumber);

 public:
    static const char* const kDefaultIdentityClaimResponseFilename;
    static const char* const kDefaultActivatedIdentityFilename;

    explicit CommandTpm2(
            const hirs::tpm2_tools_utils::Tpm2ToolsVersion& version
            = hirs::tpm2_tools_utils::Tpm2ToolsVersionChecker
            ::findTpm2ToolsVersion());

    void setAuthData();

    std::string getEndorsementCredentialDefault(
            const AsymmetricKeyType& keyType);

    std::string getPlatformCredentialDefault();

    std::string getStoredCredential(
            const std::string& credentialHandle);

    void createEndorsementKey(const AsymmetricKeyType& keyType =
                                        AsymmetricKeyType::RSA);

    std::string getEndorsementKeyPublicArea();

    void createAttestationKey();

    std::string getAttestationKeyPublicArea();

    hirs::pb::IdentityClaim createIdentityClaim(
            const hirs::pb::DeviceInfo& deviceInfo,
            const std::string& akPublicArea,
            const std::string& ekPublicArea,
            const std::string& endorsementCredential = {},
            const std::vector<std::string>& platformCredentials = {});

    std::string activateIdentity();

    void storeAKCertificate(const std::string& akCertificateByteString);

    std::string getQuote(TPML_PCR_SELECTION* pcr_selection,
            const std::string& nonce);
};

}  // namespace tpm2
}  // namespace hirs
#endif  // HIRS_PROVISIONERTPM2_INCLUDE_COMMANDTPM2_H_
