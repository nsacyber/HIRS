
namespace hirs {
    public enum ClientExitCodes {
        SUCCESS = 0, // Full successful program completion 
        FAIL = 1, // Unknown/Generic failure resulting in exit 
        USER_ERROR = 20, // Generic user error 
        MISSING_CONFIG = 21, // Config file missing 
        ACA_UNREACHABLE = 22, // Nothing found at the address specified 
        NOT_PRIVILEGED = 23, // Client not run as root 
        EXTERNAL_APP_ERROR = 40, // Generic external application error 
        TPM_ERROR = 41, // Encountered error with the TPM, log the TPM Return Code 
        HW_COLLECTION_ERROR = 42, // Encountered error when gathering hardware details 
        PROVISIONING_ERROR = 60, // Generic provisioning error | 
        PASS_1_STATUS_FAIL = 61,
        PASS_2_STATUS_FAIL = 62,
        MAKE_CREDENTIAL_BLOB_MALFORMED = 63 // The TPM2_MakeCredential blob was not correct 
    }
}
