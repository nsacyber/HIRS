#ifndef libhis_activateidentity_hpp
#define libhis_activateidentity_hpp

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

class libhis_activateidentity
{
public:
	libhis_activateidentity()
	{
		//set default values
		init_ik_size = TSS_KEY_SIZE_DEFAULT;
		init_ik_type = TSS_KEY_TYPE_IDENTITY;
		init_ik_authorized = TSS_KEY_AUTHORIZATION;
		init_ik_migratable = TSS_KEY_NOT_MIGRATABLE;
		init_ik_volatile = TSS_KEY_VOLATILE;
		binitialized = false;

		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//create an SRK object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, TSS_KEY_TSP_SRK, &hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Create SRK", result);

		//Create TPM policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_tpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Create TPM Policy", result);

		//Create SRK policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Create SRK Policy", result);

		//Create IK policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_ik);
		if(result != TSS_SUCCESS) throw libhis_exception("Create IK Policy", result);
	}

	void init()
	{
		//combine the init flags
		init_ik = init_ik_size | init_ik_type | init_ik_authorized | init_ik_migratable | init_ik_volatile;

		//Create IK object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, init_ik, &hkey_ik);
		if(result != TSS_SUCCESS) throw libhis_exception("Create IK", result);

		binitialized = true;
	}

	void activateidentity(
		unsigned char	*auth_tpm_value,
		unsigned long	auth_tpm_size,
		bool			auth_tpm_sha1,
		unsigned char	*auth_srk_value,
		unsigned long	auth_srk_size,
		bool			auth_srk_sha1,
		unsigned char	*auth_ik_value,
		unsigned long	auth_ik_size,
		bool			auth_ik_sha1,
		unsigned char	*asym_value,
		unsigned long	asym_size,
		unsigned char	*sym_value,
		unsigned long	sym_size,
		unsigned char	*uuid_ik_value,
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

		//assign the TPM auth
		result = Tspi_Policy_AssignToObject(hpolicy_tpm, htpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Assign TPM Secret", result);
		
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

		//Set up the IK UUID
		hextouuid(uuid_ik_value, uuid_ik);

		//Get the IK by UUID
		result = Tspi_Context_GetKeyByUUID(hcontext, TSS_PS_TYPE_SYSTEM, uuid_ik, &hkey_ik);
		if(result != TSS_SUCCESS) throw libhis_exception("Get IK by UUID", result);

		//set up IK auth
		if(auth_ik_sha1)
		{
			result = Tspi_Policy_SetSecret(hpolicy_ik, TSS_SECRET_MODE_SHA1, auth_ik_size, auth_ik_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set IK Secret SHA1", result);
		}
		else
		{
			result = Tspi_Policy_SetSecret(hpolicy_ik, TSS_SECRET_MODE_PLAIN, auth_ik_size, auth_ik_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set IK Secret Plain", result);
		}

		//assign the IK auth
		result = Tspi_Policy_AssignToObject(hpolicy_ik, hkey_ik);
		if(result != TSS_SUCCESS) throw libhis_exception("Assign IK Secret", result);

		//Unwrap the IK
		result = Tspi_Key_LoadKey(hkey_ik, hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Unwrap IK", result);

		//Activate identity
		BYTE *value;
		UINT32 size;
		result = Tspi_TPM_ActivateIdentity(htpm, hkey_ik, asym_size, asym_value, sym_size, sym_value, &size, &value);
		if(result != TSS_SUCCESS) throw libhis_exception("Activate identity", result);

		//Copy memory because TSS uses malloc and free, but we're using new and delete
		output_size = size;
		output_value = new unsigned char[size];
		for(unsigned long i = 0; i < size; i++)
		{
			output_value[i] = value[i];
		}

		//clean up the TSS data -- CANNOT DO THIS; TSS MEMORY LEAK?
		//result = Tspi_Context_FreeMemory(hcontext, value);
		//if(result != TSS_SUCCESS) throw libhis_exception("Cleanup identity credential", result);

		return;
	}

	~libhis_activateidentity()
	{
		//clean up IK policy
		result = Tspi_Context_CloseObject(hcontext, hpolicy_ik);
		if(result != TSS_SUCCESS) throw libhis_exception("Close IK Policy", result);

		if(binitialized)
		{
			//clean up IK
			result = Tspi_Context_CloseObject(hcontext, hkey_ik);
			if(result != TSS_SUCCESS) throw libhis_exception("Close IK", result);
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

		//close context
		result = Tspi_Context_Close(hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Close Context", result);
	}

private:
	TSS_RESULT		result;
	TSS_HCONTEXT	hcontext;
	TSS_HTPM		htpm;
	TSS_HKEY		hkey_srk,
					hkey_ik;
	TSS_HPOLICY		hpolicy_tpm,
					hpolicy_srk,
					hpolicy_ik;
	TSS_UUID		uuid_ik;
	UINT32			init_ik,
					init_ik_size,
					init_ik_type,
					init_ik_authorized,
					init_ik_migratable,
					init_ik_volatile;
	bool			binitialized;
};

#endif
