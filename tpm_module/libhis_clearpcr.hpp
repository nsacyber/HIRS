#ifndef libhis_clearpcr_hpp
#define libhis_clearpc_hpp

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

class libhis_clearpcr
{
public:
	libhis_clearpcr()
	{
		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//Create TPM policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_tpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Create TPM Policy", result);

		//Create PCRS object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_PCRS, TSS_PCRS_STRUCT_INFO_SHORT, &hpcrs);
		if(result != TSS_SUCCESS) throw libhis_exception("Create PCRS", result);
	}

	void clearpcr(
		unsigned char	*auth_tpm_value,
		unsigned long	auth_tpm_size,
		bool			auth_tpm_sha1,
		unsigned char	*mask)
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

		//set up mask
		bool bitmask[24];
		for(short i = 0; i < 24; i++)
			bitmask[i] = 0;
		masktobitmask(mask, bitmask);

		//collect the PCR values
		UINT32	temp_size;
		BYTE	*temp_value;
		for(unsigned long i = 0; i < 24; i++)
		{
			if(bitmask[i])
			{
				result = Tspi_TPM_PcrRead(htpm, i, &temp_size, &temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("PCR value read", result);

				result = Tspi_PcrComposite_SelectPcrIndexEx(hpcrs, i, TSS_PCRS_DIRECTION_RELEASE);
				if(result != TSS_SUCCESS) throw libhis_exception("Set PCR composite index", result);

				result = Tspi_PcrComposite_SetPcrValue(hpcrs, i, temp_size, temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("Store PCR value in composite", result);

				result = Tspi_Context_FreeMemory(hcontext, temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("Clear temporary memory", result);
			}
		}

		//clear PCR value
		result = Tspi_TPM_PcrReset(htpm, hpcrs);
		if(result != TSS_SUCCESS) throw libhis_exception("Reset PCRs", result);

		return;
	}

	~libhis_clearpcr()
	{
		//clean up PCRS
		result = Tspi_Context_CloseObject(hcontext, hpcrs);
		if(result != TSS_SUCCESS) throw libhis_exception("Close PCRS", result);

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
	TSS_HPCRS		hpcrs;
};

#endif
