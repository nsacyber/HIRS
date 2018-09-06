#ifndef libhis_verifysignature_hpp
#define libhis_verifysignature_hpp

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
#include "libhis_utils.hpp"

class libhis_verifysignature
{
public:
	libhis_verifysignature()
	{
		//set default values
		init_key_size = TSS_KEY_SIZE_DEFAULT;
		init_key_type = TSS_KEY_TYPE_DEFAULT;
		init_key_authorized = TSS_KEY_AUTHORIZATION;
		init_key_migratable = TSS_KEY_NOT_MIGRATABLE;
		init_key_volatile = TSS_KEY_VOLATILE;
		init_key_scheme = 0;
		binitialized = false;

		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//create an SRK object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, TSS_KEY_TSP_SRK, &hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Create SRK", result);

		//Create SRK policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Create SRK Policy", result);

		//Create key policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Create key Policy", result);

		//create hash object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_HASH, TSS_HASH_SHA1, &hhash);
		if(result != TSS_SUCCESS) throw libhis_exception("Create hash object", result);
	}

	void initsign(unsigned int in_size, unsigned int in_scheme)
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

		//set the signature scheme
		if(in_scheme == 0)
			init_key_scheme = TSS_SS_RSASSAPKCS1V15_SHA1;
		else if(in_scheme == 1)
			init_key_scheme = TSS_SS_RSASSAPKCS1V15_DER;
		else
			init_key_scheme = TSS_SS_NONE;

		//combine the init flags
		init_key = init_key_size | init_key_type | init_key_authorized | init_key_migratable | init_key_volatile;

		//Create key object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, init_key, &hkey_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Create key", result);

		//Set the signature scheme
		result = Tspi_SetAttribUint32(hkey_key, TSS_TSPATTRIB_KEY_INFO, TSS_TSPATTRIB_KEYINFO_SIGSCHEME, init_key_scheme);
		if(result != TSS_SUCCESS) throw libhis_exception("Set signature scheme", result);

		binitialized = true;
	}

	void verifysignature(
		unsigned char	*auth_srk_value,
		unsigned long	auth_srk_size,
		bool			auth_srk_sha1,
		unsigned char	*auth_key_value,
		unsigned long	auth_key_size,
		bool			auth_key_sha1,
		unsigned char	*uuid_key_value,
		unsigned char	*hash,
		unsigned char	*signature_value,
		unsigned long	signature_size)
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

		//set hash value
		result = Tspi_Hash_UpdateHashValue(hhash, 20, hash);
		if(result != TSS_SUCCESS) throw libhis_exception("Set hash value", result);

		//verify signature data
		result = Tspi_Hash_VerifySignature(hhash, hkey_key, signature_size, signature_value);
		if(result != TSS_SUCCESS) throw libhis_exception("Verify signature", result);
	}

	~libhis_verifysignature()
	{
		//clean up hash object
		result = Tspi_Context_CloseObject(hcontext, hhash);
		if(result != TSS_SUCCESS) throw libhis_exception("Close hash object", result);

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

		//clean up SRK object
		result = Tspi_Context_CloseObject(hcontext, hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Close SRK", result);

		//close context
		result = Tspi_Context_Close(hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Close Context", result);
	}

private:
	TSS_RESULT		result;
	TSS_HCONTEXT	hcontext;
	TSS_HTPM		htpm;
	TSS_HKEY		hkey_srk,
					hkey_key;
	TSS_HPOLICY		hpolicy_srk,
					hpolicy_key;
	TSS_UUID		uuid_key;
	TSS_HHASH		hhash;
	UINT32			init_key,
					init_key_size,
					init_key_type,
					init_key_authorized,
					init_key_migratable,
					init_key_volatile,
					init_key_scheme;
	bool			binitialized;
};

#endif
