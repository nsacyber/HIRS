/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <CommandTpm2.h>
#include <DeviceInfoCollector.h>
#include <HirsRuntimeException.h>
#include <Process.h>
#include <Utils.h>

#include <chrono>
#include <fstream>
#include <string>
#include <sstream>
#include <thread>
#include <utility>
#include <vector>
#include <iostream>
#include <iomanip>

using hirs::exception::HirsRuntimeException;
using hirs::file_utils::fileToString;
using hirs::file_utils::writeBinaryFile;
using hirs::file_utils::fileExists;
using hirs::log::Logger;
using hirs::pb::IdentityClaim;
using hirs::tpm2::CommandTpm2;
using hirs::string_utils::binaryToHex;
using hirs::string_utils::contains;
using hirs::string_utils::longToHex;
using hirs::string_utils::hexToLong;
using hirs::tpm2_tools_utils::Tpm2ToolsVersion;
using hirs::tpm2_tools_utils::Tpm2ToolsOutputParser;
using hirs::utils::Process;
using std::chrono::milliseconds;
using std::cout;
using std::endl;
using std::string;
using std::stringstream;
using std::ifstream;
using std::this_thread::sleep_for;
using std::to_string;
using std::vector;

const Logger CommandTpm2::LOGGER = Logger::getDefaultLogger();

const int CommandTpm2::kMaxRetryCommandAttempts = 5;

const char* const CommandTpm2::kTpm2ToolsTakeOwnershipCommand
    = "tpm2_takeownership";
const char* const CommandTpm2::kTpm2ToolsNvDefineCommand = "tpm2_nvdefine";
const char* const CommandTpm2::kTpm2ToolsNvListCommand = "tpm2_nvlist";
const char* const CommandTpm2::kTpm2ToolsNvReadCommand = "tpm2_nvread";
const char* const CommandTpm2::kTpm2ToolsNvReleaseCommand = "tpm2_nvrelease";
const char* const CommandTpm2::kTpm2ToolsNvWriteCommand = "tpm2_nvwrite";
const char* const CommandTpm2::kTpm2ToolsGetPubAkCommand = "tpm2_getpubak";
const char* const CommandTpm2::kTpm2ToolsGetPubEkCommand = "tpm2_getpubek";
const char* const CommandTpm2::kTpm2ToolsListPersistentCommand
    = "tpm2_listpersistent";
const char* const CommandTpm2::kTpm2ToolsReadPublicCommand = "tpm2_readpublic";
const char* const CommandTpm2::kTpm2ToolsActivateCredential
    = "tpm2_activatecredential";
const char* const CommandTpm2::kTpm2ToolsEvictControlCommand
    = "tpm2_evictcontrol";
const char* const CommandTpm2::kTpm2ToolsGetQuoteCommand = "tpm2_quote";
const char* const CommandTpm2::kTpm2ToolsPcrListCommand = "tpm2_pcrlist";

/**
 * The value for the TPM_RC_RETRY was obtained from Table 16 (pgs. 37-41) of
 * the "Trusted Platform Module Library Part 2: Structures" Revision 1.38
 * document.
 */
const char* const CommandTpm2::kTpm2RetryCommandCode = "0x922";

const char* const CommandTpm2::kWellKnownSecret = "00";

/**
 * The value for kDefaultAttributeValue can be understood by checking Part 2 of
 * the TPM 2.0 Specification, Table 204: Definition of (UINT32) TPMA_NV Bits.
 * The value of 0x2000A means the following 3 bits are set:
 * Bit 1:  The index data can be written if Owner Authorization is provided.
 * Bit 3:  Authorizations to change the Index contents that require USER role
 *         may not be provided with a policy session.
 * Bit 17: The index data can be read if Owner Authorization is provided.
 */
const char* const CommandTpm2::kDefaultAttributeValue = "0x2000A";

/**
 * The following algorithm IDs were obtained from Table 9 (pgs. 26-28) of
 * the "Trusted Platform Module Library Part 2: Structures" Revision 1.38
 * document.
 */
const char* const CommandTpm2::kRsaAlgorithmId = "0x01";
const char* const CommandTpm2::kEccAlgorithmId = "0x23";

/**
 * The following default memory address was obtained from Table 28 (pg. 57) of
 * the "Trusted Platform Module Library Part 2: Structures" Revision 1.38
 * document.
 */
const char* const CommandTpm2::kDefaultOwnerAuthHandle = "0x40000001";

/**
 * The following default memory addresses were obtained from Table 2 (pg. 29) of
 * the "TCG TPM v2.0 Provisioning Guidance", Revision 1.0 document.
 */
const char* const CommandTpm2::kDefaultRsaEkCredentialHandle = "0x1c00002";
const char* const CommandTpm2::kDefaultEccEkCredentialHandle = "0x1c0000a";
const char* const CommandTpm2::kDefaultPlatformCredentialHandle = "0x1c90000";
const char* const CommandTpm2::kDefaultEkHandle = "0x81010001";
const char* const CommandTpm2::kDefaultAkHandle = "0x81010002";

const char* const CommandTpm2::kAKCertificateHandle = "0x1c0000c";

const char* const CommandTpm2::kDefaultAkCertFilename =
        "/etc/hirs/ak.cer";
const char* const CommandTpm2::kDefaultAkNameFilename = "ak.name";
const char* const CommandTpm2::kDefaultAkPubFilename = "ak.pub";
const char* const CommandTpm2::kDefaultEkPubFilename = "ek.pub";

const char* const CommandTpm2::kDefaultIdentityClaimResponseFilename
        = "identityClaimResponse";
const char* const CommandTpm2::kDefaultActivatedIdentityFilename
        = "activatedIdentity.secret";
const char* const CommandTpm2::kTpm2DefaultQuoteFilename = "/tmp/quote.bin";
const char* const CommandTpm2::kTpm2DefaultSigFilename = "/tmp/sig.bin";
const char* const CommandTpm2::kTpm2Sha256SigAlgorithm = "sha256";

/**
 * Constructor to create an interface to TPM 2.0 devices.
 * @param version version of tpm2-tools to be used when making commands
 */
CommandTpm2::CommandTpm2(const Tpm2ToolsVersion& version)
        : version(version) {
}

/**
 * Method to set the auth data (passwords) of the TPM 2.0 device.
 *
 * @throws HirsRuntimeException on failure
 */
void CommandTpm2::setAuthData() {
    stringstream argsStream;

    switch (version) {
        case Tpm2ToolsVersion::VERSION_1_1_0:
        case Tpm2ToolsVersion::VERSION_2_1_0:
            argsStream << " -X -o " << kWellKnownSecret
                       << " -e " << kWellKnownSecret
                       << " -l " << kWellKnownSecret
                       << endl;
            break;
        case Tpm2ToolsVersion::VERSION_3:
            argsStream << " -o hex:" << kWellKnownSecret
                       << " -e hex:" << kWellKnownSecret
                       << " -l hex:" << kWellKnownSecret
                       << endl;
            break;
    }

    LOGGER.info("Attempting to set auth data.");
    runTpm2CommandWithRetry(kTpm2ToolsTakeOwnershipCommand, argsStream.str(),
                            __LINE__);
    LOGGER.info("Auth data set successfully.");
}

/**
 * Method to get the Endorsement Credential from a default address
 * as a byte-based, DER-encoded X509 credential.
 * @param keyType must be RSA or ECC
 * @return string of hex-encoded bytes representing DER-encoded X509 credential
 */
string CommandTpm2::getEndorsementCredentialDefault(
        const AsymmetricKeyType& keyType) {
    LOGGER.info("Attempting to retrieve endorsement credential");
    string endorsementCredential;
    switch (keyType) {
        case AsymmetricKeyType::RSA:
            endorsementCredential = getStoredCredential(
                    kDefaultRsaEkCredentialHandle);
            break;
        case AsymmetricKeyType::ECC:
            endorsementCredential = getStoredCredential(
                    kDefaultEccEkCredentialHandle);
            break;
    }
    if (endorsementCredential == "") {
        LOGGER.info("Unable to retrieve endorsement credential");
        cout << "------> Unable to retrieve endorsement credential" << endl;
    } else {
        LOGGER.info("Successfully retrieved endorsement credential");
    }
    return endorsementCredential;
}

/**
 * Method to get the Platform Credential from a default address
 * as a byte-based, DER-encoded X509 credential. If platform credential
 * does not exist, an empty string will be returned.
 *
 * @return string of hex-encoded bytes representing DER-encoded X509
 * credential or an empty string if no credential exists
 */
string CommandTpm2::getPlatformCredentialDefault() {
    LOGGER.info("Attempting to retrieve platform credential");
    string platformCredential = getStoredCredential(
            kDefaultPlatformCredentialHandle);
    if (platformCredential == "") {
        LOGGER.info("Unable to retrieve platform credential");
        cout << "------> Unable to retrieve platform credential" << endl;
    } else {
        LOGGER.info("Successfully retrieved platform credential");
    }
    return platformCredential;
}

/**
 * Method to get a stored credential such as the Endorsement Credential and
 * Platform Credential as a byte-based, DER-encoded X509 credential.
 *
 * @param credentialHandle NVRAM address for Endorsement key pair
 * @throws HirsRuntimeException on failure
 * @return string of hex-encoded bytes representing DER-encoded X509 credential
 */
string CommandTpm2::getStoredCredential(
        const string& credentialHandle) {
    LOGGER.info("Attempting to determine key size.");
    uint16_t dataSize = getNvIndexDataSize(credentialHandle);

    if (dataSize == 0) {
        stringstream errStream;
        errStream << "Could not parse NV List command. It did not contain the "
                  << "handle: " << credentialHandle;
        LOGGER.warn(errStream.str());
        return "";
    }

    stringstream logstream;
    logstream << "Key size acquired. Attempting credential retrieval at "
              << "address " << credentialHandle;
    LOGGER.info(logstream.str());
    string credential = readNvIndex(credentialHandle, dataSize);

    if (credential.empty()) {
        stringstream errStream;
        errStream << "Could not parse NV Read command. Verify the size and "
                  << "location were correct: " << to_string(dataSize)
                  << " bytes at " << credentialHandle;
        LOGGER.warn(errStream.str());
        return "";
    }

    LOGGER.info("Credential retrieval successful.");
    return credential;
}

/**
 * Method to generate an Endorsement Key (EK) pair at the default address of
 * 0x81010001, if one does not already exist at that location.
 * Currently, this uses tpm2_tools and, consequently, creates a file containing
 * the public area of the newly created EK pair.
 *
 * @param keyType determines the algorithm used to generate the EK pair
 */
void CommandTpm2::createEndorsementKey(const AsymmetricKeyType& keyType) {
    LOGGER.info("Creating Endorsement Key.");
    if (hasPersistentObject(kDefaultEkHandle)) {
        LOGGER.info("Endorsement key already exists at default address.");
        createPublicAreaFile(kDefaultEkHandle, kDefaultEkPubFilename);
        return;
    }

    LOGGER.info("Attempting to create EK at: " + string(kDefaultEkHandle));
    stringstream argsStream;
    switch (keyType) {
        case AsymmetricKeyType::RSA:
            argsStream << " -g " << kRsaAlgorithmId;
            break;
        case AsymmetricKeyType::ECC:
            argsStream << " -g " << kEccAlgorithmId;
            break;
    }
    argsStream << " -H " << kDefaultEkHandle
               << " -f " << kDefaultEkPubFilename
               << endl;

    runTpm2CommandWithRetry(kTpm2ToolsGetPubEkCommand, argsStream.str(),
                            __LINE__);
    LOGGER.info("Endorsement Key successfully created.");
}

/**
 * Method to retrieve the public area of a TPM's endorsement key as a
 * byte-encoded string.
 *
 * @return string of bytes representing an endorsement key public area
 */
string CommandTpm2::getEndorsementKeyPublicArea() {
    LOGGER.info("Attempting to read EK public area from file: "
                + string(kDefaultEkPubFilename));
    string binaryEncodedPublicArea = getPublicArea(kDefaultEkPubFilename);

    LOGGER.info("Public area successfully read.");
    return binaryEncodedPublicArea;
}

/**
 * Method to generate an Attestation Key (AK) pair
 * (AKA a restricted-use signing key pair.) Calls down to
 * tpm2-tools to generate the ak and persist it in TPM
 * storage as a child key under the default Endorsement Key.
 * Generates two files: one containing the ak public data and
 * one containing the ak name. Ak is persisted at the default location.
 * The current algorithm for key generation defaults to RSA.
 */
void CommandTpm2::createAttestationKey() {
    if (hasPersistentObject(kDefaultAkHandle)) {
        LOGGER.info(string("Attestation key already exists at default address")
                    + "\nFlushing key...");
        flushPersistentObject(kDefaultAkHandle);
    }

    stringstream argsStream;
    argsStream << " -E " << kDefaultEkHandle
               << " -k " << kDefaultAkHandle
               << " -f " << kDefaultAkPubFilename
               << " -n " << kDefaultAkNameFilename
               << endl;

    LOGGER.info("Running getpubak with arguments: "
                + argsStream.str());
    runTpm2CommandWithRetry(kTpm2ToolsGetPubAkCommand, argsStream.str(),
                            __LINE__);
    LOGGER.info("AK created successfully");
}

/**
 * Method to get the byte-encoded public key portion of the AK pair.
 * Assumes createAk has been called and default filenames were used.
 * Takes generated public data and name file and packages them into
 * a protobuf data structure for transmission.
 *
 * @return protobuf encoded Attestation Public Key Data
 */
string CommandTpm2::getAttestationKeyPublicArea() {
    LOGGER.info("Attempting to read AK public area from file: "
                + string(kDefaultAkPubFilename));
    string binaryEncodedPublicArea = getPublicArea(kDefaultAkPubFilename);

    LOGGER.info("Public area successfully read.");
    return binaryEncodedPublicArea;
}

/**
 * Method to create identity claim to send to the Attestation Certificate
 * Authority (ACA).
 *
 * @param deviceInfo device specific info that can be verified
 * @param akPublicArea the public key area blob for the AK
 * @param ekPublicArea the public key area blob for the endorsement key
 * @param endorsementCredential endorsement credential for verification
 * @param platformCredentials platform credentials for verification
 */
IdentityClaim CommandTpm2::createIdentityClaim(
        const hirs::pb::DeviceInfo& deviceInfo,
        const string& akPublicArea,
        const string& ekPublicArea,
        const string& endorsementCredential,
        const vector<string>& platformCredentials) {
    IdentityClaim identityClaim;
    identityClaim.set_allocated_dv(new hirs::pb::DeviceInfo(deviceInfo));
    identityClaim.set_ak_public_area(akPublicArea);
    identityClaim.set_ek_public_area(ekPublicArea);
    if (endorsementCredential != "") {
        identityClaim.set_endorsement_credential(endorsementCredential);
    }
    for (const auto& platformCredential : platformCredentials) {
        if (platformCredential != "") {
            identityClaim.add_platform_credential(platformCredential);
        }
    }
    return identityClaim;
}

/**
 * Method to activate a given attested identity with the TPM.
 * Decodes a nonce blob provided by the ACA when the Identity Claim Request
 * was made.
 *
 * @returns the decrypted, binary encoded nonce
 */
string CommandTpm2::activateIdentity() {
    string binaryEncodedNonce;
    // response blob has already been written to a file
    // verify file exists
    if (!fileExists(kDefaultIdentityClaimResponseFilename)) {
        throw HirsRuntimeException(
                "Identity claim response file does not exist",
                "CommandTpm2::activateIdentity");
    }

    // TPM2 Tools major version 3.X.X prepends 4 bytes of a MAGIC NUMBER and
    // 4 bytes of a version number to the file containing the cert and secret,
    // but the ACA does not, nor does the ACA know which version of TPM2 Tools
    // is running on the client machine. So we add the bytes here.
    if (version == Tpm2ToolsVersion::VERSION_3) {
        string s = fileToString(kDefaultIdentityClaimResponseFilename);
        union {
            UINT16 value;
            BYTE bytes[2];
        } uint16byteConverter;
        uint16byteConverter.bytes[0] = s[0];
        uint16byteConverter.bytes[1] = s[1];
        UINT16 numBytesInCred = uint16byteConverter.value;
        // Shift from Little Endian to Big Endian encoding for size of
        // credential structure and secret structure, respectively
        std::swap(s[0], s[1]);
        std::swap(s[134], s[135]);
        // Erase unnecessary zero padding due
        s.erase(2 + numBytesInCred, 134 - 2 - numBytesInCred);
        // Prepend header: MAGIC_NUMBER (0xBADCC0DE) + Version (0x00000001)
        s.insert(s.begin(), {static_cast<char>(0xBA), static_cast<char>(0xDC),
                             static_cast<char>(0xC0), static_cast<char>(0xDE),
                             0x00, 0x00, 0x00, 0x01});
        writeBinaryFile(s, kDefaultIdentityClaimResponseFilename);
    }

    stringstream argsStream;
    argsStream << " -H " << kDefaultAkHandle
               << " -k " << kDefaultEkHandle
               << " -f " << kDefaultIdentityClaimResponseFilename
               << " -o " << kDefaultActivatedIdentityFilename
               << endl;

    runTpm2CommandWithRetry(kTpm2ToolsActivateCredential, argsStream.str(),
                            __LINE__);

    try {
        binaryEncodedNonce = fileToString(kDefaultActivatedIdentityFilename);
    } catch (HirsRuntimeException& ex) {
        throw HirsRuntimeException("Unable to open Activate Identity file",
                                   "CommandTpm2::activateIdentity");
    }
    LOGGER.debug("Identity activated successfully");

    return binaryEncodedNonce;
}

/**
 * Stores the AK Certificate to the TPM.
 *
 * @param akCertificateByteString string containing the raw bytes of the certificate
 */
void CommandTpm2::storeAKCertificate(
        const string& akCertificateByteString) {
    if (hasNvIndexDefined(kAKCertificateHandle)) {
        LOGGER.info("AK Cert found at "
                    + string(kAKCertificateHandle)
                    + ". Releasing from NV Space...");
        releaseNvIndex(kAKCertificateHandle);
    }

    std::ofstream akCertificateFile(kDefaultAkCertFilename);
    akCertificateFile << akCertificateByteString;
    akCertificateFile.close();

    stringstream argsStream;
    size_t akCertificateByteStringSize = akCertificateByteString.size();
    argsStream << " -x " << kAKCertificateHandle
                           << " -a " << kDefaultOwnerAuthHandle
                           << " -t " << kDefaultAttributeValue
                           << " -s " << akCertificateByteStringSize
                           << endl;

    runTpm2CommandWithRetry(kTpm2ToolsNvDefineCommand, argsStream.str(),
                            __LINE__);

    try {
        LOGGER.debug(string("Beginning to write to NV Index: ")
                     + kAKCertificateHandle);
        LOGGER.debug("Provided data size: "
                     + to_string(akCertificateByteStringSize));
        string nvWriteArguments
                = createNvWriteCommandArgs(kAKCertificateHandle,
                                           kDefaultAkCertFilename);

        runTpm2CommandWithRetry(kTpm2ToolsNvWriteCommand, nvWriteArguments,
                                __LINE__);
    } catch (HirsRuntimeException& ex) {
        LOGGER.warn(string("Attempt to write AK Certificate to TPM failed.")
                    + string(" The following output was given:\n")
                    + string(ex.what()));
    }
}

/**
 * Private helper method that builds the command arguments for tpm2_tools
 * NV Write tool.
 *
 * @param nvIndex the index at which to peform the write command
 * @param writeFile the filename of the file to write to the NV index
 * @return the argument string to be affixed to the NV Write command
 */
string CommandTpm2::createNvWriteCommandArgs(const string& nvIndex,
                                             const string& writeFile) {
    stringstream argumentsStringStream;
    argumentsStringStream << " -x " << nvIndex
                          << " -a " << kDefaultOwnerAuthHandle
                          << " ";

    switch (version) {
        case Tpm2ToolsVersion::VERSION_1_1_0:
        case Tpm2ToolsVersion::VERSION_2_1_0:
            argumentsStringStream << "-f ";
            break;
    }

    argumentsStringStream << writeFile
                          << endl;
    return argumentsStringStream.str();
}

/**
 * Method to get a quote (signed pcr selection) from the TPM 2.0 device.
 *
 * @param akLocation location of an activated AK pair
 * @param pcrSelection selection of pcrs to sign
 */
string CommandTpm2::getQuote(const string& pcr_selection,
                    const string& nonce) {
    string quote;
    stringstream argsStream;
    int result = 0;
    for (size_t count = 0; count < nonce.length(); ++count) {
        result *=2;
        result += nonce[count] == '1'? 1 : 0;
    }

    stringstream ss;
    ss << std::hex << std::setw(8) << std::setfill('0') << result;
    string hexNonce(ss.str());

    argsStream << " -k " << kDefaultAkHandle
              << " -g " << kTpm2Sha256SigAlgorithm
              << " -l " << pcr_selection
              << " -q " << hexNonce  // this needs to be a hex string
              << endl;

    LOGGER.info("Running tpm2_quote with arguments: " + argsStream.str());
    quote = runTpm2CommandWithRetry(kTpm2ToolsGetQuoteCommand,
                            argsStream.str(),
                            __LINE__);
    LOGGER.info("TPM Quote successful");

    return quote;
}

/**
 * Method to get the full list of pcrs from the TPM.
 *
 */
string CommandTpm2::getPcrList() {
    string pcrslist;
    stringstream argsStream;

    argsStream << endl;

    LOGGER.info("Running tpm2_pcrlist with arguments: " + argsStream.str());
    pcrslist = runTpm2CommandWithRetry(kTpm2ToolsPcrListCommand,
                            argsStream.str(),
                            __LINE__);
    LOGGER.info("TPM PCR List successful");

    return pcrslist;
}

/**
 * Private helper method to offload the process of running tpm2_nvlist
 * and parsing the output for the data size at a particular nvIndex.
 *
 * @param nvIndex the memory address whose data size is of interest
 * @throws HirsRuntimeException if tpm2_nvlist is not run successfully
 * @return the size of the data at nvIndex, or 0 if it's not found
 */
uint16_t CommandTpm2::getNvIndexDataSize(const string& nvIndex) {
    string listOutput;
    try {
        listOutput = runTpm2CommandWithRetry(kTpm2ToolsNvListCommand, "",
                                             __LINE__);
    } catch (HirsRuntimeException& ex) {
        // Due to bug in tpm2-tools 2.1.0, check to see if error was success
        if (contains(ex.what(), "NV indexes defined.")) {
            listOutput = ex.what();
        } else {
            throw;
        }
    }
    return Tpm2ToolsOutputParser::parseNvDataSize(nvIndex, listOutput);
}

/**
 * Private helper method to offload the process of running tpm2_nvread
 * and converting the data output to a hex-encoded byte string.
 *
 * @param nvIndex the starting memory address to read data from
 * @param dataSize the amount of data to read at the nvIndex
 * @throws HirsRuntimeException if tpm2_nvread is not run successfully
 * @return the output in a hex-encoded byte string, or an empty string if
 * nothing is found at nvIndex
 */
string CommandTpm2::readNvIndex(const string& nvIndex,
                                const uint16_t& dataSize) {
    LOGGER.info("Beginning to read at NV Index: " + nvIndex);
    LOGGER.info("Provided dataSize: " + to_string(dataSize));
    uint16_t maxNvBufferSize = 128;
    uint16_t nvReadIterations = dataSize / maxNvBufferSize;
    uint16_t nvBufferRemainder = dataSize % maxNvBufferSize;

    uint16_t offset = 0;
    stringstream nvReadOutput;
    for (int i = 0; i <= nvReadIterations; i++) {
        string nvReadArguments;
        if (i == nvReadIterations) {
            nvReadArguments = createNvReadCommandArgs(nvIndex, offset,
                                                      nvBufferRemainder);
        } else {
            nvReadArguments = createNvReadCommandArgs(nvIndex, offset,
                                                      maxNvBufferSize);
        }
        LOGGER.info("Command args: " + nvReadArguments);

        string rawNvReadOutput = runTpm2CommandWithRetry(
                kTpm2ToolsNvReadCommand, nvReadArguments, __LINE__);

        switch (version) {
            case Tpm2ToolsVersion::VERSION_1_1_0:
            case Tpm2ToolsVersion::VERSION_2_1_0:
                nvReadOutput << Tpm2ToolsOutputParser::parseNvReadOutput(
                        rawNvReadOutput);
                break;
            case Tpm2ToolsVersion::VERSION_3:
                nvReadOutput << rawNvReadOutput;
                break;
        }

        if (i != nvReadIterations) {
            offset += maxNvBufferSize;
        }
    }
    return nvReadOutput.str();
}

/**
 * Private helper method to determine if an NV Index has been previously
 * defined.
 *
 * @param nvIndex memory address at which to check for defined NV Index
 * @throws HirsRuntimeException if failed to retrieve dataSize successfully
 * @return true, if a definition exists at the given index / false, otherwise
 */
bool CommandTpm2::hasNvIndexDefined(const string& nvIndex) {
    uint16_t dataSize = getNvIndexDataSize(nvIndex);
    return dataSize != 0;
}

/**
 * Private helper method to offload the process of running tpm2_nvrelease
 * on an NV Index. This should release the NV Index from the
 * TPM and allow the space to be redefined and used again.
 *
 * @param nvIndex NV memory address to release
 * @throws HirsRuntimeException if tpm2_nvrelease is not run successfully
 */
void CommandTpm2::releaseNvIndex(const string& nvIndex) {
    LOGGER.info("Releasing NV Index at: " + nvIndex);
    stringstream argsStream;
    switch (version) {
        case Tpm2ToolsVersion::VERSION_1_1_0:
        case Tpm2ToolsVersion::VERSION_2_1_0:
            argsStream << " -X -P " << kWellKnownSecret;
            break;
        case Tpm2ToolsVersion::VERSION_3:
            argsStream << " -P hex:" << kWellKnownSecret;
            break;
    }
    argsStream << " -a " << kDefaultOwnerAuthHandle
               << " -x " << nvIndex;

    runTpm2CommandWithRetry(kTpm2ToolsNvReleaseCommand, argsStream.str(),
                            __LINE__);
    LOGGER.info("NV Index released successfully");
}

/**
 * Private helper method that builds the command arguments for tpm2_tools
 * NV Read tool.
 *
 * @param nvIndex the index at which to peform the read command
 * @param readSize the size (in bytes) to pull with the read command
 * @return the argument string to be affixed to the NV Read command
 */
string CommandTpm2::createNvReadCommandArgs(const string& nvIndex,
                                            const uint16_t& offset,
                                            const uint16_t& readSize) {
    stringstream argumentsStringStream;
    argumentsStringStream << " -x " << nvIndex
                          << " -a " << kDefaultOwnerAuthHandle
                          << " -o " << to_string(offset)
                          << " -s " << to_string(readSize)
                          << endl;
    return argumentsStringStream.str();
}

/**
 * Private helper method to offload the process of running tpm2_listpersistent
 * and determining if a persistent object exists on the TPM at the given
 * memory address.
 *
 * @param handle memory address at which to check for a persistent object
 * @throws HirsRuntimeException if tpm2_listpersistent is not run successfully
 * @return true, if an object exists at the given handle / false, otherwise
 */
bool CommandTpm2::hasPersistentObject(const string& handle) {
    string listOutput
            = runTpm2CommandWithRetry(kTpm2ToolsListPersistentCommand, "",
                                      __LINE__);
    return Tpm2ToolsOutputParser::parsePersistentObjectExists(handle,
                                                              listOutput);
}

/**
 * Private helper method to offload the process of running tpm2_evictcontrol
 * on a persistent object. This should flush the persistent object from the
 * TPM should there be an object at the specified handle.
 *
 * @param handle memory address at which to flush a persistent object
 * @throws HirsRuntimeException if tpm2_evictcontrol is not run successfully
 */
void CommandTpm2::flushPersistentObject(const string& handle) {
    stringstream argsStream;
    argsStream << " -A " << "o"  // Owner Auth Flag
                         << " -H " << handle
                         << " -S " << handle
                         << endl;

    LOGGER.info("Running evictcontrol with arguments: "
                + argsStream.str());
    runTpm2CommandWithRetry(kTpm2ToolsEvictControlCommand, argsStream.str(),
                            __LINE__);
    LOGGER.info("Object flushed successfully");
}

/**
 * Private helper method that reads and then writes the public area
 * for a key in the TPM to a file in the local directory.
 *
 * @param keyHandle memory address at which the public key is stored
 * @param filename name to be given to the file that stores the key's public
 *  area
 */
void CommandTpm2::createPublicAreaFile(const string& keyHandle,
                                       const string& filename) {
    // Note: we always need to write the file in the event tpm2-tools has been
    // updated between provisioner runs. Thus, no short circuit logic to check
    // for the file can be performed and avoid rewriting it can be performed.
    if (fileExists(filename)) {
        LOGGER.info("Public area file exists. Deleting for rewrite.");
        remove(filename.c_str());
    }

    LOGGER.info("Creating public area file.");
    stringstream argumentsStringStream;
    argumentsStringStream << " -H " << keyHandle
                          << " -o " << filename
                          << endl;

    runTpm2CommandWithRetry(kTpm2ToolsReadPublicCommand,
                            argumentsStringStream.str(),
                            __LINE__);
    LOGGER.info("Public area file successfully created.");
}

string CommandTpm2::getPublicArea(const std::string& filename) {
    string binaryEncodedPublicArea;

    // need to read data from files
    try {
        binaryEncodedPublicArea = fileToString(filename);
    } catch (HirsRuntimeException& ex) {
        throw HirsRuntimeException("Unable to open public area file",
                                   "CommandTpm2::getPublicArea");
    }

    // TPM2 Tools versions 1.1.0 and 2.1.0 affix 2 bytes of zeroes to files
    // containing a public area, but the ACA does not know which version of
    // TPM2 Tools is running on the client machine. So we remove the extra
    // bytes here.
    switch (version) {
        case Tpm2ToolsVersion::VERSION_1_1_0:
        case Tpm2ToolsVersion::VERSION_2_1_0:
            binaryEncodedPublicArea.erase(binaryEncodedPublicArea.end() - 2,
                                          binaryEncodedPublicArea.end());
            break;
    }

    LOGGER.debug("Successfully read public data");

    return binaryEncodedPublicArea;
}

string CommandTpm2::runTpm2CommandWithRetry(const string& command,
                                            const string& args,
                                            int sourceCodeLineNumber) {
    string tpmErrorCode;
    for (int i = 0;; ++i) {
        try {
            return hirs::utils::Process::run(command, args, "CommandTpm2.cpp",
                                             sourceCodeLineNumber);
        } catch (HirsRuntimeException& ex) {
            tpmErrorCode = Tpm2ToolsOutputParser::parseTpmErrorCode(ex.what());

            if (tpmErrorCode == kTpm2RetryCommandCode &&
                    i < kMaxRetryCommandAttempts) {
                LOGGER.warn("Waiting 100 ms and Retrying Command: " + command);
                sleep_for(milliseconds(100));
                continue;
            }
            throw;
        }
    }
}
