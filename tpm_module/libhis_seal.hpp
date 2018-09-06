#ifndef libhis_seal_hpp
#define libhis_seal_hpp

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

class libhis_seal
{
public:
	libhis_seal()
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

		//Create PCRS object
#ifdef WINDOWS
		//Windows and NTru are capable of unsealing all PCRS structures so use 1.2 LONG for full 24 PCR support
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_PCRS, TSS_PCRS_STRUCT_INFO_LONG, &hpcrs);
		if(result != TSS_SUCCESS) throw libhis_exception("Create PCRS", result);
#endif
#ifdef LINUX
		//Linux and Trousers CANNOT unseal 1.2 LONG or SHORT PCRS structures so use the legacy 1.1 structure with 16 PCR limit
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_PCRS, TSS_PCRS_STRUCT_INFO, &hpcrs);
		if(result != TSS_SUCCESS) throw libhis_exception("Create PCRS", result);
#endif

		//Create ENCData object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_ENCDATA, TSS_ENCDATA_SEAL, &hencdata);
		if(result != TSS_SUCCESS) throw libhis_exception("Create ENCData Object", result);

		//Create ENCData policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_enc);
		if(result != TSS_SUCCESS) throw libhis_exception("Create ENCData Policy", result);
	}

	void seal(
		unsigned char	*auth_srk_value,
		unsigned long	auth_srk_size,
		bool			auth_srk_sha1,
		unsigned char	*auth_enc_value,
		unsigned long	auth_enc_size,
		bool			auth_enc_sha1,
		unsigned char	*mask,
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

		//set up mask
		bool bitmask[24];
		for(short i = 0; i < 24; i++)
			bitmask[i] = 0;
		masktobitmask(mask, bitmask);

		//collect the PCR values
		UINT32	temp_size;
		BYTE	*temp_value;
#ifdef WINDOWS
		for(unsigned long i = 0; i < 24; i++)		//all PCRs available in SHORT or LONG
#endif
#ifdef LINUX
		for(unsigned long i = 0; i < 16; i++)		//we cannot use all PCRs in Linux mode due to legacy PCRS structure
#endif
		{
			if(bitmask[i])
			{
				result = Tspi_TPM_PcrRead(htpm, i, &temp_size, &temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("PCR value read", result);

				//don't do this for regular seal because creation PCRs are meaningless in this context
				//result = Tspi_PcrComposite_SelectPcrIndexEx(hpcrs, i, TSS_PCRS_DIRECTION_CREATION);
				//if(result != TSS_SUCCESS) throw libhis_exception("Set PCR composite index at creation", result);

#ifdef WINDOWS
				//use EX functions because we are PCRS LONG
				result = Tspi_PcrComposite_SelectPcrIndexEx(hpcrs, i, TSS_PCRS_DIRECTION_RELEASE);
				if(result != TSS_SUCCESS) throw libhis_exception("Set PCR composite index at release", result);
#endif
#ifdef LINUX
				//cannot use EX functions
				result = Tspi_PcrComposite_SelectPcrIndex(hpcrs, i);
				if(result != TSS_SUCCESS) throw libhis_exception("Set PCR composite index legacy mode", result);
#endif
				result = Tspi_PcrComposite_SetPcrValue(hpcrs, i, temp_size, temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("Store PCR value in composite", result);

				result = Tspi_Context_FreeMemory(hcontext, temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("Clear temporary memory", result);
			}
		}

		//Seal data
		result = Tspi_Data_Seal(hencdata, hkey_srk, payload_size, payload_value, hpcrs);
		if(result != TSS_SUCCESS) throw libhis_exception("Seal data", result);

		//Get the sealed data blob
		BYTE	*value;
		UINT32	size;
		result = Tspi_GetAttribData(hencdata, TSS_TSPATTRIB_ENCDATA_BLOB, TSS_TSPATTRIB_ENCDATABLOB_BLOB, &size, &value);
		if(result != TSS_SUCCESS) throw libhis_exception("Get sealed data blob", result);

		//copy over memory
		output_enc_size = size;
		output_enc_value = new unsigned char[size];
		for(unsigned long i = 0; i < size; i++)
			output_enc_value[i] = value[i];

		//clean up dynamic memory
		result = Tspi_Context_FreeMemory(hcontext, value);
		if(result != TSS_SUCCESS) throw libhis_exception("Clear dynamic memory", result);

		return;
	}

	void seal2(
		unsigned char	*auth_srk_value,
		unsigned long	auth_srk_size,
		bool			auth_srk_sha1,
		unsigned char	*auth_enc_value,
		unsigned long	auth_enc_size,
		bool			auth_enc_sha1,
		unsigned char	*mask,
		unsigned char	*payload_value,
		unsigned long	payload_size,
		unsigned char	*release_value,
		unsigned long	release_size,
		unsigned char	*&output_enc_value,
		unsigned long	&output_enc_size)
	{
#ifdef LINUX
		//don't even let the users do seal2 because TSS_PCRS_STRUCT_INFO_LONG does not work for unsealing in Linux
		if(result != TSS_SUCCESS) throw libhis_exception("Command disabled in Linux due to TSS_PCRS_STRUCT_INFO_LONG unseal defect", TPM_E_DISABLED_CMD);
#endif
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

		//set up mask
		bool bitmask[24];
		for(short i = 0; i < 24; i++)
			bitmask[i] = 0;
		masktobitmask(mask, bitmask);

		//collect the PCR values
		UINT32	temp_size;
		BYTE	*temp_value;
		short	counter = 0;
		for(unsigned long i = 0; i < 24; i++)
		{
			if(bitmask[i])
			{
				//set the creation value
				result = Tspi_TPM_PcrRead(htpm, i, &temp_size, &temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("PCR value read", result);

				result = Tspi_PcrComposite_SelectPcrIndexEx(hpcrs, i, TSS_PCRS_DIRECTION_CREATION);
				if(result != TSS_SUCCESS) throw libhis_exception("Set PCR composite index at creation", result);

				result = Tspi_PcrComposite_SetPcrValue(hpcrs, i, temp_size, temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("Store PCR value in composite", result);

				result = Tspi_Context_FreeMemory(hcontext, temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("Clear temporary memory", result);

				//set the release value
				temp_value = new unsigned char[20];
				for(short j = 0; j < 20; j++)
					temp_value[j] = release_value[j + counter * 20];

				result = Tspi_PcrComposite_SelectPcrIndexEx(hpcrs, i, TSS_PCRS_DIRECTION_RELEASE);
				if(result != TSS_SUCCESS) throw libhis_exception("Set PCR composite index at release", result);

				result = Tspi_PcrComposite_SetPcrValue(hpcrs, i, temp_size, temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("Store PCR value in composite", result);

				delete [] temp_value;
			}
		}

		//Seal data
		result = Tspi_Data_Seal(hencdata, hkey_srk, payload_size, payload_value, hpcrs);
		if(result != TSS_SUCCESS) throw libhis_exception("Seal data", result);

		//Get the sealed data blob
		BYTE	*value;
		UINT32	size;
		result = Tspi_GetAttribData(hencdata, TSS_TSPATTRIB_ENCDATA_BLOB, TSS_TSPATTRIB_ENCDATABLOB_BLOB, &size, &value);
		if(result != TSS_SUCCESS) throw libhis_exception("Get sealed data blob", result);

		//copy over memory
		output_enc_size = size;
		output_enc_value = new unsigned char[size];
		for(unsigned long i = 0; i < size; i++)
			output_enc_value[i] = value[i];

		//clean up dynamic memory
		result = Tspi_Context_FreeMemory(hcontext, value);
		if(result != TSS_SUCCESS) throw libhis_exception("Clear dynamic memory", result);

		return;
	}

	~libhis_seal()
	{
		//clean up ENCData policy
		result = Tspi_Context_CloseObject(hcontext, hpolicy_enc);
		if(result != TSS_SUCCESS) throw libhis_exception("Close ENCData Policy", result);

		//Clean up ENCData
		result = Tspi_Context_CloseObject(hcontext, hencdata);
		if(result != TSS_SUCCESS) throw libhis_exception("Close ENCData", result);

		//clean up PCRS
		result = Tspi_Context_CloseObject(hcontext, hpcrs);
		if(result != TSS_SUCCESS) throw libhis_exception("Close PCRS", result);

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
