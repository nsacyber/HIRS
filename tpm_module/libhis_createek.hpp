#ifndef libhis_createek_hpp
#define libhis_createek_hpp

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

class libhis_createek
{
public:
	libhis_createek()
	{
		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//create EK object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_RSAKEY, TSS_KEY_SIZE_DEFAULT, &hkey_ek);
		if(result != TSS_SUCCESS) throw libhis_exception("Create EK object handle", result);
	}

	void createek(
		unsigned char	*nonce)
	{
		//establish a session
		result = Tspi_Context_Connect(hcontext, 0);
		if(result != TSS_SUCCESS) throw libhis_exception("Connect Context", result);

		//get the TPM object
		result = Tspi_Context_GetTpmObject(hcontext, &htpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Get TPM Object", result);

		//TSS requires external data to be set for EK creation
		validation.ulExternalDataLength = 20;
		validation.rgbExternalData = nonce;

		//create EK
		result = Tspi_TPM_CreateEndorsementKey(htpm, hkey_ek, 0);
		if(result != TSS_SUCCESS) throw libhis_exception("Create EK", result);
	}

	~libhis_createek()
	{
		//clean up EK object
		result = Tspi_Context_CloseObject(hcontext, hkey_ek);
		if(result != TSS_SUCCESS) throw libhis_exception("Close EK object handle", result);

		//close context
		result = Tspi_Context_Close(hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Close Context", result);
	}

private:
	TSS_RESULT		result;
	TSS_HCONTEXT	hcontext;
	TSS_HTPM		htpm;
	TSS_HKEY		hkey_ek;
	TSS_VALIDATION	validation;
};

#endif
