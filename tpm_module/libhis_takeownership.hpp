#ifndef libhis_takeownership_hpp
#define libhis_takeownership_hpp

#ifdef WINDOWS
	#include "tspi.h"
	#include "tss_error.h"
	#include "tss_defines.h"
#endif
#ifdef LINUX
	#include <tss/tspi.h>
	#include <tss/tss_error.h>
	#include <tss/tss_defines.h>
#endif

#include "libhis_exception.hpp"

class libhis_takeownership
{
public:
	libhis_takeownership()
	{
		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//create EK object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, TSS_KEY_SIZE_DEFAULT, &hkey_ek);
		if(result != TSS_SUCCESS) throw libhis_exception("Create EK", result);

		//create an SRK object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, TSS_KEY_TSP_SRK, &hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Create SRK", result);

		//Create TPM policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_tpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Create TPM Policy", result);

		//Create SRK policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Create SRK Policy", result);
	}

	void takeownership(
		unsigned char	*auth_tpm_value,
		unsigned long	auth_tpm_size,
		bool			auth_tpm_sha1,
		unsigned char	*auth_srk_value,
		unsigned long	auth_srk_size,
		bool			auth_srk_sha1,
		unsigned char	*nonce)
	{
		//establish a session
		result = Tspi_Context_Connect(hcontext, 0);
		if(result != TSS_SUCCESS) throw libhis_exception("Connect Context", result);

		//get the TPM object
		result = Tspi_Context_GetTpmObject(hcontext, &htpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Get TPM Object", result);

		//set up TPM auth
		if(auth_tpm_sha1)
		{
			result = Tspi_Policy_SetSecret(hpolicy_tpm, TSS_SECRET_MODE_SHA1, auth_tpm_size, auth_tpm_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set TPM Secret SHA1", result);
		}
		else
		{
			result = Tspi_Policy_SetSecret(hpolicy_tpm, TSS_SECRET_MODE_PLAIN, auth_tpm_size, auth_tpm_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set TPM Secret Plain", result);
		}

		//assign the TPM auth to the TPM
		result = Tspi_Policy_AssignToObject(hpolicy_tpm, htpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Assign TPM Secret to TPM", result);

		//assign the TPM auth to the EK
		result = Tspi_Policy_AssignToObject(hpolicy_tpm, hkey_ek);
		if(result != TSS_SUCCESS) throw libhis_exception("Assign TPM Secret to EK", result);

		//set up nonce
		validation.ulExternalDataLength = 20;
		validation.rgbExternalData = nonce;

		try
		{
			//get the public EK
			result = Tspi_TPM_GetPubEndorsementKey(htpm, false, &validation, &hkey_ek);
			if(result != TSS_SUCCESS) throw libhis_exception("Get Public EK", result);
		}
		catch(libhis_exception &e)
		{
			//get the public EK the Atmel TPM in an Ultrabook way
			result = Tspi_TPM_GetPubEndorsementKey(htpm, true, &validation, &hkey_ek);
			if(result != TSS_SUCCESS) throw libhis_exception("Get Public EK", result);

			//let a second exception make its way upward (rare)
		}

		//set up SRK auth
		if(auth_srk_sha1)
		{
			result = Tspi_Policy_SetSecret(hpolicy_srk, TSS_SECRET_MODE_SHA1, auth_srk_size, auth_srk_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set SRK Secret SHA1", result);
		}
		else
		{
			result = Tspi_Policy_SetSecret(hpolicy_srk, TSS_SECRET_MODE_PLAIN, auth_srk_size, auth_srk_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set SRK Secret Plain", result);
		}

		//assign the SRK auth
		result = Tspi_Policy_AssignToObject(hpolicy_srk, hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Assign SRK Secret", result);

		//take ownership of the TPM
		result = Tspi_TPM_TakeOwnership(htpm, hkey_srk, hkey_ek);
		if(result != TSS_SUCCESS) throw libhis_exception("Take Ownership", result);

		//clean up validation data
		result = Tspi_Context_FreeMemory(hcontext, validation.rgbData);
		if(result != TSS_SUCCESS) throw libhis_exception("Clean up rgbData", result);

		result = Tspi_Context_FreeMemory(hcontext, validation.rgbValidationData);
		if(result != TSS_SUCCESS) throw libhis_exception("Clean up rgbValidationData", result);

		//test the SRK to make sure it actually works (required for NTru because TakeOwnership sometimes doesn't execute RegisterKey for SRK)
		TSS_UUID uuid_srk = TSS_UUID_SRK;
		result = Tspi_Context_LoadKeyByUUID(hcontext, TSS_PS_TYPE_SYSTEM, uuid_srk, &hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Test the SRK", result);

		return;
	}

	~libhis_takeownership()
	{
		//clean up SRK policy
		result = Tspi_Context_CloseObject(hcontext, hpolicy_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Close SRK Policy", result);

		//clean up TPM policy
		result = Tspi_Context_CloseObject(hcontext, hpolicy_tpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Close TPM Policy", result);

		//clean up SRK object
		result = Tspi_Context_CloseObject(hcontext, hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Close SRK", result);

		//clean up EK object
		result = Tspi_Context_CloseObject(hcontext, hkey_ek);
		if(result != TSS_SUCCESS) throw libhis_exception("Close EK", result);

		//close context
		result = Tspi_Context_Close(hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Close Context", result);
	}

private:
	TSS_RESULT		result;
	TSS_HCONTEXT	hcontext;
	TSS_HTPM		htpm;
	TSS_HKEY		hkey_ek,
					hkey_srk;
	TSS_HPOLICY		hpolicy_tpm,
					hpolicy_srk;
	TSS_VALIDATION	validation;
};

#endif
