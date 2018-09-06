#ifndef libhis_changeownership_hpp
#define libhis_changeownership_hpp

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

class libhis_changeownership
{
public:
	libhis_changeownership()
	{
		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//Create TPM policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_tpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Create TPM Policy", result);

		//Create new policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_new);
		if(result != TSS_SUCCESS) throw libhis_exception("Create New Policy", result);
	}

	void changeownership(
		unsigned char *auth_tpm_value,
		unsigned long auth_tpm_size,
		bool auth_tpm_sha1,
		unsigned char *auth_new_value,
		unsigned long auth_new_size,
		bool auth_new_sha1)
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

		//set up new auth
		if(auth_new_sha1)
		{
			result = Tspi_Policy_SetSecret(hpolicy_new, TSS_SECRET_MODE_SHA1, auth_new_size, auth_new_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set New Secret SHA1", result);
		}
		else
		{
			result = Tspi_Policy_SetSecret(hpolicy_new, TSS_SECRET_MODE_PLAIN, auth_new_size, auth_new_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set New Secret Plain", result);
		}

		//change the TPM secret
		result = Tspi_ChangeAuth(htpm, 0, hpolicy_new);
		if(result != TSS_SUCCESS) throw libhis_exception("Change TPM Secret", result);

		return;
	}

	~libhis_changeownership()
	{
		//clean up new policy
		result = Tspi_Context_CloseObject(hcontext, hpolicy_new);
		if(result != TSS_SUCCESS) throw libhis_exception("Close New Policy", result);

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
	TSS_HPOLICY		hpolicy_tpm,
					hpolicy_new;
};

#endif
