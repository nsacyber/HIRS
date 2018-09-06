#ifndef libhis_getnvdata_hpp
#define libhis_getnvdata_hpp

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
#include <trousers/trousers.h>

class libhis_getnvdata
{
public:
	libhis_getnvdata()
	{
		//set defaults
		nvstore_index = 0;

		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//Create TPM policy
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_POLICY, TSS_POLICY_USAGE, &hpolicy_tpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Create TPM Policy", result);

		//Create NVSTore object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_NV, 0, &hnvstore);
		if(result != TSS_SUCCESS) throw libhis_exception("Create NVStore object", result);
	}

	void getnvdata(
		unsigned char	*auth_tpm_value,
		unsigned long	auth_tpm_size,
		bool			auth_tpm_sha1,
		unsigned long	nv_index,
		unsigned char	*&nv_value,
		unsigned long	&nv_size)
	{
		//set up the index value
		bool nv_platform = false;
		if(nv_index == 0)
			nvstore_index = TPM_NV_INDEX_EKCert;
		else if(nv_index == 1)
			nvstore_index = TPM_NV_INDEX_TPM_CC;
		else if(nv_index == 2) {
			nvstore_index = TPM_NV_INDEX_PlatformCert;
			nv_platform = true;
		}
		else if(nv_index == 3) {
			nvstore_index = TPM_NV_INDEX_Platform_CC;
			nv_platform = true;
		}
		else
			nvstore_index = nv_index;

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

		// Check if the NV area is locked.  Must be performed after TPM AUTH.
		TSS_BOOL nvLocked;
		result = Tspi_TPM_GetStatus(htpm, TSS_TPMSTATUS_NV_LOCK, &nvLocked);
		if (result != TSS_SUCCESS) throw libhis_exception("Check TPM NV Lock", result);

		// If locked, set the bit in the index to retrieve the requested data.  else, unset that bit.
		nvstore_index = ((nvLocked == TRUE) && !nv_platform) ? nvstore_index + TSS_NV_DEFINED : nvstore_index & ~TSS_NV_DEFINED;

		//assign the TPM auth to the NVStore
		result = Tspi_Policy_AssignToObject(hpolicy_tpm, hnvstore);
		if(result != TSS_SUCCESS) throw libhis_exception("Assign TPM Secret to NVStore", result);

		//force NVData to be readable by the owner only
		result = Tspi_SetAttribUint32(hnvstore, TSS_TSPATTRIB_NV_PERMISSIONS, 0, TPM_NV_PER_OWNERREAD | TPM_NV_PER_OWNERWRITE);
		if(result != TSS_SUCCESS) throw libhis_exception("Requier owner auth on NVStore read/write", result);

		//set the read address
		result = Tspi_SetAttribUint32(hnvstore, TSS_TSPATTRIB_NV_INDEX, 0, nvstore_index);
		if(result != TSS_SUCCESS) throw libhis_exception("Set NVStore index", result);

		//get the size
		UINT32	size = 0;
		BYTE	*value = 0;

#ifdef WINDOWS
		//read the size of the data at the index
		result = Tspi_GetAttribUint32(hnvstore, TSS_TSPATTRIB_NV_DATASIZE, 0, &size);
		if(result != TSS_SUCCESS) throw libhis_exception("WINDOWS: Get size of NVStore object", result);
#endif
#ifdef LINUX
		UINT32 ulResultLen; // stores the length of the data returned by GetCapability
		// Retrieves a TPM_NV_DATA_PUBLIC structure that indicates the values for the specified NV area.
		// The NV area is identified by the nvstore_index.
		result = Tspi_TPM_GetCapability(htpm, TSS_TPMCAP_NV_INDEX, sizeof(UINT32),
				(BYTE *)&nvstore_index, &ulResultLen, &value);
		if(result == TSS_SUCCESS) {
			UINT64 off = 0;
			// value which is a BYTE* must be converted into its TSS Data Structure
			TPM_NV_DATA_PUBLIC *nvDataPublicStruct = new TPM_NV_DATA_PUBLIC();
			// Trousers converts the data blob into the struct
			result = Trspi_UnloadBlob_NV_DATA_PUBLIC(&off, value, nvDataPublicStruct);
			if(result != TSS_SUCCESS) {
				delete nvDataPublicStruct;
				throw libhis_exception("LINUX: Problems converting data blob to NV Public Data object", result);
			}
			// Save off the size of the data stored in the NV area.
			size = nvDataPublicStruct->dataSize;
			// Free the memory.
			delete nvDataPublicStruct;
		}
#endif

		if(size > 0) {
			//read the nvdata
			result = Tspi_NV_ReadValue(hnvstore, 0, &size, &value);
			if(result != TSS_SUCCESS) throw libhis_exception("Read NVStore space", result);

			//copy out the values
			nv_size = size;
			nv_value = new unsigned char[size];
			for(unsigned long i = 0; i < size; i++)
				nv_value[i] = value[i];
		}

		//cleanup
		result = Tspi_Context_FreeMemory(hcontext, value);
		// I'm not sure if this error message is useful.  But it was stopping the process unnecessarily.
		//if(result != TSS_SUCCESS) throw libhis_exception("Clean memory", result);
	}

	~libhis_getnvdata()
	{
		//clean up NVStoer
		result = Tspi_Context_CloseObject(hcontext, hnvstore);
		if(result != TSS_SUCCESS) throw libhis_exception("Close NVStore object", result);

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
	TSS_HNVSTORE	hnvstore;
	UINT32			nvstore_index;
};

#endif
