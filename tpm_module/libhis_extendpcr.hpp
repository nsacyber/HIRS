#ifndef libhis_extendpcr_hpp
#define libhis_extendpcr_hpp

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

class libhis_extendpcr
{
public:
	libhis_extendpcr()
	{
		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//Create TPM policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_tpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Create TPM Policy", result);
	}

	void extendpcr(
		unsigned char	*auth_tpm_value,
		unsigned long	auth_tpm_size,
		bool			auth_tpm_sha1,
		unsigned long	pcr_index,
		unsigned char	*hash,
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

		//extend PCR value
		UINT32	size;
		BYTE	*value;
		result = Tspi_TPM_PcrExtend(htpm, pcr_index, 20, hash, 0, &size, &value);
		if(result != TSS_SUCCESS) throw libhis_exception("Extend PCR", result);

		//convert memory
		output_size = size;
		output_value = new unsigned char[size];
		for(unsigned long i = 0; i < size; i++)
			output_value[i] = value[i];

		//free dynamic memory
		result = Tspi_Context_FreeMemory(hcontext, value);
		if(result != TSS_SUCCESS) throw libhis_exception("Free dynamic memory", result);

		return;
	}

	~libhis_extendpcr()
	{
		//clean up TPM policy
		result = Tspi_Context_CloseObject(hcontext, hpolicy_tpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Close TPM Policy", result);

		//close context
		result = Tspi_Context_Close(hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Close Context", result);
	}

private:
	TSS_RESULT		result;
	TSS_HCONTEXT	hcontext;
	TSS_HTPM		htpm;
	TSS_HPOLICY		hpolicy_tpm;
};

#endif
