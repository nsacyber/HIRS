#include <sapi/tpm20.h>
#include <tcti/tcti-tabrmd.h>

#include <algorithm>
#include <cstring>
#include <iostream>
#include <memory>
#include <string>

using std::cout;
using std::endl;
using std::shared_ptr;
using std::string;

/**
 * SapiContext is a class to encapsulate the TSS2_SYS_CONTEXT and its
 * creation.
 */
class SapiContext {
    /**
     * The TCTI Context.
     */
    TSS2_TCTI_CONTEXT *tctiContext = nullptr;

    /**
     * The SAPI Context.
     */
    TSS2_SYS_CONTEXT *sapiContext = nullptr;

    /**
     * Private Constructor
     *
     * The constructor is private because the goal of the class is to manage
     * the memory of the TSS2_TCTI_CONTEXT and TSS2_SYS_CONTEXT that are
     * allocated, which are forced to be done through the static create()
     * function. Since create() returns a shared_ptr<SapiContext>, there is no
     * way to leak memory by forgetting to free anything created by this class.
     * @param tcti_ctx the TCTI Context needed for creating the SAPI Context
     * @param sapi_ctx the SAPI Context needed to interact with the TSS
     */
    SapiContext(TSS2_TCTI_CONTEXT* tcti_ctx, TSS2_SYS_CONTEXT* sapi_ctx)
            : tctiContext(tcti_ctx), sapiContext(sapi_ctx) {}

 public:
    /**
     * Destructor.
     */
    ~SapiContext() {free(sapiContext); free(tctiContext);}

    /**
     * Factory function for creating SapiContext objects and guaranteeing
     * that their memory will be freed by placing them into shared_ptr objects.
     *
     * @return a shared_ptr to a new SapiContext object
     */
    static shared_ptr<SapiContext> create() {
        size_t size;
        TSS2_RC rc = tss2_tcti_tabrmd_init(nullptr, &size);
        if (rc != TSS2_RC_SUCCESS) {
            return nullptr;
        }

        TSS2_TCTI_CONTEXT *tContext
                = reinterpret_cast<TSS2_TCTI_CONTEXT*>(calloc(1, size));
        if (tContext == nullptr) {
            return nullptr;
        }

        rc = tss2_tcti_tabrmd_init(tContext, &size);
        if (rc != TSS2_RC_SUCCESS) {
            free(tContext);
            return nullptr;
        }

        size = Tss2_Sys_GetContextSize(0);
        TSS2_SYS_CONTEXT *sContext
                = reinterpret_cast<TSS2_SYS_CONTEXT*>(calloc(1, size));
        if (sContext == nullptr) {
            free(tContext);
            return nullptr;
        }

        TSS2_ABI_VERSION abi_version = {
                .tssCreator = TSSWG_INTEROP,
                .tssFamily = TSS_SAPI_FIRST_FAMILY,
                .tssLevel = TSS_SAPI_FIRST_LEVEL,
                .tssVersion = TSS_SAPI_FIRST_VERSION,
        };

        rc = Tss2_Sys_Initialize(sContext, size, tContext, &abi_version);
        if (rc != TSS2_RC_SUCCESS) {
            free(sContext);
            free(tContext);
            return nullptr;
        }

        // To make sure the memory is not leaked, store the SapiContext in a
        // shared_ptr to make sure the destructor gets called and no copies
        // are made.
        return shared_ptr<SapiContext>(
                new SapiContext(tContext, sContext));
    }

    TSS2_SYS_CONTEXT * getPointer() {return sapiContext;}
};

/**
 * Returns the 4-byte string represented by the bytes of value.
 *
 * Assumes the bytes of value are reversed.
 * @param value the 4 bytes to be reversed and placed in the returned string
 * @return the string represented by value
 */
string reversedStringValue(UINT32 value) {
    string stringValue(sizeof value, 0);
    std::memcpy(&stringValue[0], &value, stringValue.size());  // copy bytes in
    std::reverse(stringValue.begin(), stringValue.end());  // reverse the bytes
    return stringValue;
}

/**
 * The tpm_version application prints two lines to the terminal:
 * 1.) The TPM Chip Version, and
 * 2.) The TPM Manufacturer (4 character abbreviation)
 *
 * @return 0 if successful; 1 if not
 */
int main(void) {
    TPMS_CAPABILITY_DATA capability_data;
    TPMI_YES_NO            more_data;
    shared_ptr<SapiContext> sapiContext = SapiContext::create();
    TSS2_RC rc;
    do {
        // The return code will be 4 bytes. The most significant byte
        // tells what software layer is reposible for a non-successful
        // attempt at executing the call. The other bytes provide the error
        // type. If we get a retry warning, we don't care which layer is
        // responsible, so we mask the lower 3 bytes and compare it to
        // TSS2_RC_RETRY to see if we need to try again.
        rc = Tss2_Sys_GetCapability(sapiContext->getPointer(), nullptr,
                               TPM_CAP_TPM_PROPERTIES,
                               PT_FIXED,
                               MAX_TPM_PROPERTIES,
                               &more_data,
                               &capability_data,
                               nullptr);
    } while ((rc & 0xfff) == TPM_RC_RETRY);

    // There is nothing we can do if an error occurred. The HIRS Client will
    // know how to respond if this happens. No need to log anything.
    if (rc != TSS2_RC_SUCCESS) {
        return 1;
    }

    // We only need to collect the following data from the TPM.
    string manufacturer;
    string majorVersion;
    float minorVersion;

    for (size_t i = 0; i < capability_data.data.tpmProperties.count; ++i) {
        TPMS_TAGGED_PROPERTY & p
                = capability_data.data.tpmProperties.tpmProperty[i];
        TPM_PT property = p.property;

        // All data is in the form of a UINT32, even if it represents a string.
        // For strings, the bytes are in the wrong endianness and are not
        // null-terminated.
        UINT32 value = p.value;

        switch (property) {
            case TPM_PT_FAMILY_INDICATOR:
                majorVersion = reversedStringValue(value);
                break;
            case TPM_PT_REVISION:
                // The minor version has two decimal places, but since it is
                // stored as an integer, it is stored as 100 times its value.
                minorVersion = value / 100.0f;
                break;
            case TPM_PT_MANUFACTURER:
                manufacturer = reversedStringValue(value);
                break;
        }
    }

    cout << "Chip Version: " << majorVersion << "." << minorVersion << endl;
    cout << "TPM Vendor ID: " << manufacturer << endl;
    return 0;
}
