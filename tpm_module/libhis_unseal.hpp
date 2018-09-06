#ifndef libhis_unseal_hpp
#define libhis_unseal_hpp

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

class libhis_unseal
{
public:
	libhis_unseal()
	{
		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//create an SRK object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, TSS_KEY_TSP_SRK, &hkey_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Create SRK", result);

		//Create SRK policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Create SRK Policy", result);

		//Create ENCData object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_ENCDATA, TSS_ENCDATA_SEAL, &hencdata);
		if(result != TSS_SUCCESS) throw libhis_exception("Create ENCData Object", result);

		//Create ENCData policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_enc);
		if(result != TSS_SUCCESS) throw libhis_exception("Create ENCData Policy", result);
	}

	void unseal(
		unsigned char	*auth_srk_value,
		unsigned long	auth_srk_size,
		bool			auth_srk_sha1,
		unsigned char	*auth_enc_value,
		unsigned long	auth_enc_size,
		bool			auth_enc_sha1,
		unsigned char	*payload_value,
		unsigned long	payload_size,
		unsigned char	*&output_enc_value,
		unsigned long	&output_enc_size)
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

		//install the encrypted data blob into hencdata object
		result = Tspi_SetAttribData(hencdata, TSS_TSPATTRIB_ENCDATA_BLOB, TSS_TSPATTRIB_ENCDATABLOB_BLOB, payload_size, payload_value);
		if(result != TSS_SUCCESS) throw libhis_exception("Set encrypted data blob", result);

		//set up ENCData auth
		if(auth_enc_sha1)
		{
			result = Tspi_Policy_SetSecret(hpolicy_enc, TSS_SECRET_MODE_SHA1, auth_enc_size, auth_enc_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set ENCData Secret SHA1", result);
		}
		else
		{
			result = Tspi_Policy_SetSecret(hpolicy_enc, TSS_SECRET_MODE_PLAIN, auth_enc_size, auth_enc_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Set ENCData Secret Plain", result);
		}

		//assign the ENCData auth
		result = Tspi_Policy_AssignToObject(hpolicy_enc, hencdata);
		if(result != TSS_SUCCESS) throw libhis_exception("Assign ENCData Secret", result);

		//unseal the data
		BYTE	*value;
		UINT32	size;
		result = Tspi_Data_Unseal(hencdata, hkey_srk, &size, &value);
		if(result != TSS_SUCCESS) throw libhis_exception("Unseal", result);

		output_enc_size = size;
		output_enc_value = new unsigned char[size];
		for(unsigned long i = 0; i < size; i++)
			output_enc_value[i] = value[i];

		//clean up dynamic memory
		result = Tspi_Context_FreeMemory(hcontext, value);
		if(result != TSS_SUCCESS) throw libhis_exception("Clear dynamic memory", result);

		return;
	}

	~libhis_unseal()
	{
		//clean up ENCData policy
		result = Tspi_Context_CloseObject(hcontext, hpolicy_enc);
		if(result != TSS_SUCCESS) throw libhis_exception("Close ENCData Policy", result);

		//Clean up ENCData
		result = Tspi_Context_CloseObject(hcontext, hencdata);
		if(result != TSS_SUCCESS) throw libhis_exception("Close ENCData", result);

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
	TSS_HKEY		hkey_srk;
	TSS_HPOLICY		hpolicy_srk,
					hpolicy_enc;
	TSS_HPCRS		hpcrs;
	TSS_HENCDATA	hencdata;
};

#endif
