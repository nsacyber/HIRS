#ifndef libhis_createkey_hpp
#define libhis_createkey_hpp

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

class libhis_createkey
{
public:
	libhis_createkey()
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

	void initbind(unsigned int in_size, unsigned int in_scheme)
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

		//set the encryption scheme
		if(in_scheme == 0)
			init_key_scheme = TSS_ES_RSAESPKCSV15;
		else if(in_scheme == 1)
			init_key_scheme = TSS_ES_RSAESOAEP_SHA1_MGF1;
		else if(in_scheme == 2)
			init_key_scheme = TSS_ES_SYM_CNT;
		else if(in_scheme == 3)
			init_key_scheme = TSS_ES_SYM_OFB;
		else if(in_scheme == 4)
			init_key_scheme = TSS_ES_SYM_CBC_PKCS5PAD;
		else
			init_key_scheme = TSS_ES_NONE;

		//combine the init flags
		init_key = init_key_size | init_key_type | init_key_authorized | init_key_migratable | init_key_volatile;

		//Create key object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, init_key, &hkey_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Create key", result);

		//Set the encryption scheme
		result = Tspi_SetAttribUint32(hkey_key, TSS_TSPATTRIB_KEY_INFO, TSS_TSPATTRIB_KEYINFO_ENCSCHEME, init_key_scheme);
		if(result != TSS_SUCCESS) throw libhis_exception("Set encryption scheme", result);

		binitialized = true;
	}

	void initstorage(unsigned int in_size, unsigned int in_location)
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

		//set the encryption scheme
		if(in_location == 0)
			init_key_location = TSS_PS_TYPE_SYSTEM;
		else
			init_key_location = TSS_PS_TYPE_USER;

		//combine the init flags
		init_key = init_key_size | init_key_type | init_key_authorized | init_key_migratable | init_key_volatile | init_key_location;

		//Create key object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, init_key, &hkey_key);
		if(result != TSS_SUCCESS) throw libhis_exception("Create key", result);

		binitialized = true;
	}

	void createkey(
		unsigned char *auth_srk_value,
		unsigned long auth_srk_size,
		bool auth_srk_sha1,
		unsigned char *auth_key_value,
		unsigned long auth_key_size,
		bool auth_key_sha1,
		unsigned char *uuid_key_value,
		bool uuid_overwrite)
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

		//create the key
		result = Tspi_Key_CreateKey(hkey_key, hkey_srk, 0);

		//Unwrap the newly generated key
		result = Tspi_Key_LoadKey(hkey_key, hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Unwrap key", result);

		//Set up the key UUID
		hextouuid(uuid_key_value, uuid_key);

		try
		{
			//save key
			result = Tspi_Context_RegisterKey(hcontext, hkey_key, TSS_PS_TYPE_SYSTEM, uuid_key, TSS_PS_TYPE_SYSTEM, uuid_srk);
			if(result != TSS_SUCCESS) throw libhis_exception("Save key By UUID", result);
		}
		catch(libhis_exception &e)
		{
			if(uuid_overwrite)
			{
				//Unregister the existing key
				result = Tspi_Context_UnregisterKey(hcontext, TSS_PS_TYPE_SYSTEM, uuid_key, &hkey_unregister);
				if(result != TSS_SUCCESS) throw libhis_exception("Unregister slot", result);

				//Register a new key
				result = Tspi_Context_RegisterKey(hcontext, hkey_key, TSS_PS_TYPE_SYSTEM, uuid_key, TSS_PS_TYPE_SYSTEM, uuid_srk);
				if(result != TSS_SUCCESS) throw libhis_exception("Resave key By UUID", result);
			}
			else throw e;
		}

		return;
	}

	~libhis_createkey()
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
					hkey_key,
					hkey_unregister;
	TSS_HPOLICY		hpolicy_srk,
					hpolicy_key;
	TSS_UUID		uuid_key;
	UINT32			init_key,
					init_key_size,
					init_key_type,
					init_key_authorized,
					init_key_migratable,
					init_key_volatile,
					init_key_scheme,
					init_key_location;
	bool			binitialized;
};

#endif
