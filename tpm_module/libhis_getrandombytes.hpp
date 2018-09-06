#ifndef libhis_getrandombytes_hpp
#define libhis_getrandombytes_hpp

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

class libhis_getrandombytes
{
public:
	libhis_getrandombytes()
	{
		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);
	}

	void getrandombytes(
		unsigned long bytes_size,
		unsigned char *&output_value)
	{
		//establish a session
		result = Tspi_Context_Connect(hcontext, 0);
		if(result != TSS_SUCCESS) throw libhis_exception("Connect Context", result);

		//get the TPM object
		result = Tspi_Context_GetTpmObject(hcontext, &htpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Get TPM Object", result);

		//get random bytes
		BYTE *bytes_value;
		result = Tspi_TPM_GetRandom(htpm, bytes_size, &bytes_value);
		if(result != TSS_SUCCESS) throw libhis_exception("Get Random Bytes", result);

		//copy C-style output into C++ format
		output_value = new unsigned char[bytes_size];
		for(unsigned long i = 0; i < bytes_size; i++)
		{
			output_value[i] = bytes_value[i];
		}

		//clean up random bytes
		result = Tspi_Context_FreeMemory(hcontext, bytes_value);
		if(result != TSS_SUCCESS) throw libhis_exception("Cleanup bytes", result);

		return;
	}

	~libhis_getrandombytes()
	{
		//close context
		result = Tspi_Context_Close(hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Close Context", result);
	}

private:
	TSS_RESULT		result;
	TSS_HCONTEXT	hcontext;
	TSS_HTPM		htpm;
};

#endif
