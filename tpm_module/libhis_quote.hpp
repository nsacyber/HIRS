#ifndef libhis_quote_hpp
#define libhis_quote_hpp

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

class libhis_quote
{
public:
	libhis_quote()
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

		//Create SRK policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_srk);
		if(result != TSS_SUCCESS) throw libhis_exception("Create SRK Policy", result);

		//Create IK policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_ik);
		if(result != TSS_SUCCESS) throw libhis_exception("Create IK Policy", result);
	}

	void init(bool bshort)
	{
		if(bshort)
		{
			//Create PCRS object
			result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_PCRS, TSS_PCRS_STRUCT_INFO_SHORT, &hpcrs);
			if(result != TSS_SUCCESS) throw libhis_exception("Create PCRS", result);
		}
		else
		{
			//Create PCRS object
			result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_PCRS, TSS_PCRS_STRUCT_INFO, &hpcrs);
			if(result != TSS_SUCCESS) throw libhis_exception("Create PCRS", result);
		}

		//combine the init flags
		init_ik = init_ik_size | init_ik_type | init_ik_authorized | init_ik_migratable | init_ik_volatile;

		//Create IK object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, init_ik, &hkey_ik);
		if(result != TSS_SUCCESS) throw libhis_exception("Create IK", result);

		binitialized = true;
	}

	void quote(
		unsigned char	*auth_srk_value,
		unsigned long	auth_srk_size,
		bool			auth_srk_sha1,
		unsigned char	*auth_ik_value,
		unsigned long	auth_ik_size,
		bool			auth_ik_sha1,
		unsigned char	*nonce,
		unsigned char	*uuid_ik_value,
		unsigned char	*mask,
		unsigned char	*&output_pcrs_value,
		unsigned long	&output_pcrs_size,
		unsigned char	*&output_quote_value,
		unsigned long	&output_quote_size,
		unsigned char	*&output_sig_value,
		unsigned long	&output_sig_size)
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

		//set up nonce
		validation.ulExternalDataLength = 20;
		validation.rgbExternalData = nonce;

		//set up mask
		bool bitmask[24];
		for(short i = 0; i < 24; i++)
			bitmask[i] = 0;
		masktobitmask(mask, bitmask);

		//prepare the PCR output array
		short	counter = 0;
		for(short i = 0; i < 24; i++)
			if(bitmask[i]) counter++;
		output_pcrs_size = counter * 20;
		output_pcrs_value = new unsigned char[counter * 20];

		//collect the PCR values
		UINT32	temp_size;
		BYTE	*temp_value;
		counter = 0;
		for(unsigned long i = 0; i < 24; i++)
		{
			if(bitmask[i])
			{
				result = Tspi_TPM_PcrRead(htpm, i, &temp_size, &temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("PCR value read", result);

				result = Tspi_PcrComposite_SelectPcrIndex(hpcrs, i);
				if(result != TSS_SUCCESS) throw libhis_exception("Set PCR composite index", result);

				result = Tspi_PcrComposite_SetPcrValue(hpcrs, i, temp_size, temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("Store PCR value in composite", result);

				for(unsigned long j = 0; j < 20; j++)
					output_pcrs_value[counter * 20 + j] = temp_value[j];

				counter++;

				result = Tspi_Context_FreeMemory(hcontext, temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("Clear temporary memory", result);
			}
		}

		//quote
		result = Tspi_TPM_Quote(htpm, hkey_ik, hpcrs, &validation);
		if(result != TSS_SUCCESS) throw libhis_exception("Quote", result);

		//copy values
		output_quote_size = validation.ulDataLength;
		output_quote_value = new unsigned char[validation.ulDataLength];
		for(unsigned long i = 0; i < validation.ulDataLength; i++)
			output_quote_value[i] = validation.rgbData[i];

		result = Tspi_Context_FreeMemory(hcontext, validation.rgbData);
		if(result != TSS_SUCCESS) throw libhis_exception("Clean up digest", result);

		output_sig_size = validation.ulValidationDataLength;
		output_sig_value = new unsigned char [validation.ulValidationDataLength];
		for(unsigned long i = 0; i < validation.ulValidationDataLength; i++)
			output_sig_value[i] = validation.rgbValidationData[i];

		result = Tspi_Context_FreeMemory(hcontext, validation.rgbValidationData);
		if(result != TSS_SUCCESS) throw libhis_exception("Clean up signature", result);

		return;
	}

	void quote2(
		unsigned char	*auth_srk_value,
		unsigned long	auth_srk_size,
		bool			auth_srk_sha1,
		unsigned char	*auth_ik_value,
		unsigned long	auth_ik_size,
		bool			auth_ik_sha1,
		unsigned char	*nonce,
		unsigned char	*uuid_ik_value,
		unsigned char	*mask,
		unsigned char	*&output_pcrs_value,
		unsigned long	&output_pcrs_size,
		unsigned char	*&output_quote_value,
		unsigned long	&output_quote_size,
		unsigned char	*&output_sig_value,
		unsigned long	&output_sig_size,
                bool            bCapVersion)
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

		//set up nonce
		validation.ulExternalDataLength = 20;
		validation.rgbExternalData = nonce;

		//set up mask
		bool bitmask[24];
		for(short i = 0; i < 24; i++)
			bitmask[i] = 0;
		masktobitmask(mask, bitmask);

		//prepare the PCR output array
		short	counter = 0;
		for(short i = 0; i < 24; i++)
			if(bitmask[i]) counter++;
		output_pcrs_size = counter * 20;
		output_pcrs_value = new unsigned char[counter * 20];

		//collect the PCR values
		UINT32	temp_size;
		BYTE	*temp_value;
		counter = 0;
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

				for(unsigned long j = 0; j < 20; j++)
					output_pcrs_value[counter * 20 + j] = temp_value[j];

				counter++;

				result = Tspi_Context_FreeMemory(hcontext, temp_value);
				if(result != TSS_SUCCESS) throw libhis_exception("Clear temporary memory", result);
			}
		}

		//quote2
		BYTE*	version_value;
		UINT32	version_size;

		//read PCR 10 again right before collecting the quote
		if(bitmask[10])
		{
			//reread PCR 10
			result = Tspi_TPM_PcrRead(htpm, 10, &temp_size, &temp_value);
			if(result != TSS_SUCCESS) throw libhis_exception("PCR value read", result);

			//read quote2
			result = Tspi_TPM_Quote2(htpm, hkey_ik, bCapVersion, hpcrs, &validation, &version_size, &version_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Quote2", result);

			//set value of PCR 10 in the PCR Composite
			result = Tspi_PcrComposite_SetPcrValue(hpcrs, 10, temp_size, temp_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Store PCR value in composite", result);

			for(unsigned long j = 0; j < 20; j++)
				output_pcrs_value[10 * 20 + j] = temp_value[j];

			result = Tspi_Context_FreeMemory(hcontext, temp_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Clear temporary memory", result);
		} else {
			//read quote2 without rereading PCR 10
			result = Tspi_TPM_Quote2(htpm, hkey_ik, bCapVersion, hpcrs, &validation, &version_size, &version_value);
			if(result != TSS_SUCCESS) throw libhis_exception("Quote2", result);
		}

		//copy values
		output_quote_size = validation.ulDataLength;
		output_quote_value = new unsigned char[validation.ulDataLength];
		for(unsigned long i = 0; i < validation.ulDataLength; i++)
			output_quote_value[i] = validation.rgbData[i];

		result = Tspi_Context_FreeMemory(hcontext, validation.rgbData);
		if(result != TSS_SUCCESS) throw libhis_exception("Clean up digest", result);

		output_sig_size = validation.ulValidationDataLength;
		output_sig_value = new unsigned char [validation.ulValidationDataLength];
		for(unsigned long i = 0; i < validation.ulValidationDataLength; i++)
			output_sig_value[i] = validation.rgbValidationData[i];

		result = Tspi_Context_FreeMemory(hcontext, validation.rgbValidationData);
		if(result != TSS_SUCCESS) throw libhis_exception("Clean up signature", result);

		result = Tspi_Context_FreeMemory(hcontext, version_value);
		if(result != TSS_SUCCESS) throw libhis_exception("Clean up version info", result);

		return;
	}

	~libhis_quote()
	{
		//clean up IK policy
		result = Tspi_Context_CloseObject(hcontext, hpolicy_ik);
		if(result != TSS_SUCCESS) throw libhis_exception("Close IK Policy", result);

		if(binitialized)
		{
			//clean up PCRS
			result = Tspi_Context_CloseObject(hcontext, hpcrs);
			if(result != TSS_SUCCESS) throw libhis_exception("Close PCRS", result);

			//clean up IK
			result = Tspi_Context_CloseObject(hcontext, hkey_ik);
			if(result != TSS_SUCCESS) throw libhis_exception("Close IK", result);
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
					hkey_ik;
	TSS_HPOLICY		hpolicy_srk,
					hpolicy_ik;
	TSS_NONCE		nonce;
	TSS_VALIDATION	validation;
	TSS_UUID		uuid_ik;
	TSS_HPCRS		hpcrs;
	UINT32			init_ik,
					init_ik_size,
					init_ik_type,
					init_ik_authorized,
					init_ik_migratable,
					init_ik_volatile;
	bool			binitialized;
};

#endif
