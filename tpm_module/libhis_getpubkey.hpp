#ifndef libhis_getpubkey_hpp
#define libhis_getpubkey_hpp

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

class libhis_getpubkey
{
public:
	libhis_getpubkey()
	{
		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//create EK object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, TSS_KEY_SIZE_DEFAULT, &hkey_ek);
		if(result != TSS_SUCCESS) throw libhis_exception("Create EK", result);

		//Create TPM policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_tpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Create TPM Policy", result);
	}

	void getpubek(
		unsigned char	*auth_tpm_value,
		unsigned long	auth_tpm_size,
		bool			auth_tpm_sha1,
		unsigned char	*nonce,
		unsigned char	*&output_value,
		unsigned long	&output_size)
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
			result = Tspi_TPM_GetPubEndorsementKey(htpm, true, &validation, &hkey_ek);
			if(result != TSS_SUCCESS) throw libhis_exception("Get Public EK", result);
		}
		catch(libhis_exception &e)
		{
			//get the public EK the Atmel TPM in an Ultrabook way
			result = Tspi_TPM_GetPubEndorsementKey(htpm, false, &validation, &hkey_ek);
			if(result != TSS_SUCCESS) throw libhis_exception("Get Public EK", result);

			//let a second exception make its way upward (should be same error code)
		}

		//get the modulus
		UINT32	mod_size;
		BYTE	*mod_value;
		result = Tspi_GetAttribData(hkey_ek, TSS_TSPATTRIB_RSAKEY_INFO, TSS_TSPATTRIB_KEYINFO_RSA_MODULUS, &mod_size, &mod_value);
		if(result != TSS_SUCCESS) throw libhis_exception("Get EK Blob", result);

		//copy out the EK modulus
		output_size = mod_size;
		output_value = new unsigned char[mod_size];
		for(unsigned long i = 0; i < mod_size; i++)
			output_value[i] = mod_value[i];

		//clean up ek modulus
		result = Tspi_Context_FreeMemory(hcontext, mod_value);
		if(result != TSS_SUCCESS) throw libhis_exception("Clean up modulus data", result);
	}

	void getpubsrk(
		unsigned char	*auth_tpm_value,
		unsigned long	auth_tpm_size,
		bool			auth_tpm_sha1,
		unsigned char	*&output_value,
		unsigned long	&output_size)
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

		//set up key container
		UINT32	mod_size;
		BYTE	*mod_value;

		//get the public EK
		result = Tspi_TPM_OwnerGetSRKPubKey(htpm, &mod_size, &mod_value);
		if(result != TSS_SUCCESS) throw libhis_exception("Get Public SRK", result);

		//copy out the SRK modulus
		output_size = mod_size;
		output_value = new unsigned char[mod_size];
		for(unsigned long i = 0; i < mod_size; i++)
			output_value[i] = mod_value[i];

		//clean up SRK modulus
		result = Tspi_Context_FreeMemory(hcontext, mod_value);
		if(result != TSS_SUCCESS) throw libhis_exception("Clean up modulus data", result);
	}

private:
	TSS_RESULT		result;
	TSS_HCONTEXT	hcontext;
	TSS_HTPM		htpm;
	TSS_HKEY		hkey_ek;
	TSS_HPOLICY		hpolicy_tpm;
	TSS_VALIDATION	validation;
};

#endif
