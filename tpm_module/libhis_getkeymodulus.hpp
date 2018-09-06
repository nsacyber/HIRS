#ifndef libhis_getkeymodulus_hpp
#define libhis_getkeymodulus_hpp

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

class libhis_getkeymodulus
{
public:
	libhis_getkeymodulus()
	{
		//set default values
		init_key_size = TSS_KEY_SIZE_DEFAULT;
		init_key_type = TSS_KEY_TYPE_DEFAULT;
		init_key_authorized = TSS_KEY_AUTHORIZATION;
		init_key_migratable = TSS_KEY_NOT_MIGRATABLE;
		init_key_volatile = TSS_KEY_VOLATILE;
		binitialized = false;

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

		//Create key policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Create key Policy", result);
	}

	void initidentity()
	{
		//set the type
		init_key_type = TSS_KEY_TYPE_IDENTITY;

		//set the key size
		init_key_size = TSS_KEY_SIZE_DEFAULT;

		//combine the init flags
		init_key = init_key_size | init_key_type | init_key_authorized | init_key_migratable | init_key_volatile;

		//Create key object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, init_key, &hkey_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Create key", result);

		binitialized = true;
	}

	void initsign(unsigned int in_size)
	{
		//set the type
		init_key_type = TSS_KEY_TYPE_SIGNING;

		//set the key size
		if(in_size == 0)
			init_key_size = TSS_KEY_SIZE_DEFAULT;
		else if(in_size == 512)
			init_key_size = TSS_KEY_SIZE_512;
		else if(in_size == 1024)
			init_key_size = TSS_KEY_SIZE_1024;
		else if(in_size == 2048)
			init_key_size = TSS_KEY_SIZE_2048;
		else if(in_size == 4096)
			init_key_size = TSS_KEY_SIZE_4096;
		else if(in_size == 8192)
			init_key_size = TSS_KEY_SIZE_8192;
		else if(in_size == 16384)
			init_key_size = TSS_KEY_SIZE_16384;
		else throw libhis_exception("Invalid key size", 400);

		//combine the init flags
		init_key = init_key_size | init_key_type | init_key_authorized | init_key_migratable | init_key_volatile;

		//Create key object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, init_key, &hkey_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Create key", result);

		binitialized = true;
	}

	void initbind(unsigned int in_size)
	{
		//set the type
		init_key_type = TSS_KEY_TYPE_BIND;

		//set the key size
		if(in_size == 0)
			init_key_size = TSS_KEY_SIZE_DEFAULT;
		else if(in_size == 512)
			init_key_size = TSS_KEY_SIZE_512;
		else if(in_size == 1024)
			init_key_size = TSS_KEY_SIZE_1024;
		else if(in_size == 2048)
			init_key_size = TSS_KEY_SIZE_2048;
		else if(in_size == 4096)
			init_key_size = TSS_KEY_SIZE_4096;
		else if(in_size == 8192)
			init_key_size = TSS_KEY_SIZE_8192;
		else if(in_size == 16384)
			init_key_size = TSS_KEY_SIZE_16384;
		else throw libhis_exception("Invalid key size", 400);

		//combine the init flags
		init_key = init_key_size | init_key_type | init_key_authorized | init_key_migratable | init_key_volatile;

		//Create key object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, init_key, &hkey_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Create key", result);

		binitialized = true;
	}

	void initstorage(unsigned int in_size)
	{
		//set the type
		init_key_type = TSS_KEY_TYPE_STORAGE;

		//set the key size
		if(in_size == 0)
			init_key_size = TSS_KEY_SIZE_DEFAULT;
		else if(in_size == 512)
			init_key_size = TSS_KEY_SIZE_512;
		else if(in_size == 1024)
			init_key_size = TSS_KEY_SIZE_1024;
		else if(in_size == 2048)
			init_key_size = TSS_KEY_SIZE_2048;
		else if(in_size == 4096)
			init_key_size = TSS_KEY_SIZE_4096;
		else if(in_size == 8192)
			init_key_size = TSS_KEY_SIZE_8192;
		else if(in_size == 16384)
			init_key_size = TSS_KEY_SIZE_16384;
		else throw libhis_exception("Invalid key size", 400);

		//combine the init flags
		init_key = init_key_size | init_key_type | init_key_authorized | init_key_migratable | init_key_volatile;

		//Create key object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, init_key, &hkey_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Create key", result);

		binitialized = true;
	}

	/*
	 * @Deprecated
	 */
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

	/*
	 * @Deprecated
	 */
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

	void getkeymodulus(
		unsigned char	*auth_srk_value,
		unsigned long	auth_srk_size,
		bool			auth_srk_sha1,
		unsigned char	*auth_key_value,
		unsigned long	auth_key_size,
		bool			auth_key_sha1,
		unsigned char	*uuid_key_value,
		unsigned char	*&output_value,
		unsigned long	&output_size)
	{
		//establish a session
		result = Tspi_Context_Connect(hcontext, 0);
		if(result != TSS_SUCCESS) throw libhis_exception("Connect Context", result);

		//get the TPM object
		result = Tspi_Context_GetTpmObject(hcontext, &htpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Get TPM Object", result);

		//load the SRK
		TSS_UUID uuid_srk = TSS_UUID_SRK;
		result = Tspi_Context_LoadKeyByUUID(hcontext, TSS_PS_TYPE_SYSTEM, uuid_srk, &hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Load SRK", result);

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

		//Set up the key UUID
		hextouuid(uuid_key_value, uuid_key);

		//Get the key by UUID
		result = Tspi_Context_GetKeyByUUID(hcontext, TSS_PS_TYPE_SYSTEM, uuid_key, &hkey_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Get key by UUID", result);

		//set up key auth
		if(auth_key_sha1)
		{
			result = Tspi_Policy_SetSecret(hpolicy_key, TSS_SECRET_MODE_SHA1, auth_key_size, auth_key_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set key Secret SHA1", result);
		}
		else
		{
			result = Tspi_Policy_SetSecret(hpolicy_key, TSS_SECRET_MODE_PLAIN, auth_key_size, auth_key_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set key Secret Plain", result);
		}

		//assign the key auth
		result = Tspi_Policy_AssignToObject(hpolicy_key, hkey_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Assign key Secret", result);

		//Unwrap the key
		result = Tspi_Key_LoadKey(hkey_key, hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Unwrap key", result);

		//get the keyblob
		UINT32	size;
		BYTE	*value;
		result = Tspi_GetAttribData(hkey_key, TSS_TSPATTRIB_RSAKEY_INFO, TSS_TSPATTRIB_KEYINFO_RSA_MODULUS, &size, &value);
		if(result != TSS_SUCCESS) throw libhis_exception("Get modulus", result);

		//copy out the results
		output_size = size;
		output_value = new unsigned char[size];
		for(unsigned long i = 0; i < size; i++)
			output_value[i] = value[i];

		//clean up dynamic memory
		result = Tspi_Context_FreeMemory(hcontext, value);
		if(result != TSS_SUCCESS) throw libhis_exception("Cleanup dynamic memory", result);

		return;
	}

	~libhis_getkeymodulus()
	{
		//clean up key policy
		result = Tspi_Context_CloseObject(hcontext, hpolicy_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Close key Policy", result);

		if(binitialized)
		{
			//clean up key
			result = Tspi_Context_CloseObject(hcontext, hkey_key);
			if(result != TSS_SUCCESS) throw libhis_exception("Close key", result);
		}

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
					hkey_srk,
					hkey_key,
					hkey_unregister;
	TSS_HPOLICY		hpolicy_tpm,
					hpolicy_srk,
					hpolicy_key;
	TSS_VALIDATION	validation;
	TSS_UUID		uuid_key;
	UINT32			init_key,
					init_key_size,
					init_key_type,
					init_key_authorized,
					init_key_migratable,
					init_key_volatile;
	bool			binitialized;
};

#endif
