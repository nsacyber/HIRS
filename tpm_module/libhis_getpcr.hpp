#ifndef libhis_getpcr_hpp
#define libhis_getpcr_hpp

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

class libhis_getpcr
{
public:
	libhis_getpcr()
	{
		//create a context object
		result = Tspi_Context_Create(&hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Create Conntext", result);

		//Create PCRS object
		result = Tspi_Context_CreateObject(hcontext, TSS_OBJECT_TYPE_PCRS, TSS_PCRS_STRUCT_INFO_SHORT, &hpcrs);
		if(result != TSS_SUCCESS) throw libhis_exception("Create PCRS", result);
	}

	void getpcr(
		unsigned char	*mask,
		unsigned char	*&output_pcrs_value,
		unsigned long	&output_pcrs_size)
	{
		//establish a session
		result = Tspi_Context_Connect(hcontext, 0);
		if(result != TSS_SUCCESS) throw libhis_exception("Connect Context", result);

		//get the TPM object
		result = Tspi_Context_GetTpmObject(hcontext, &htpm);
		if(result != TSS_SUCCESS) throw libhis_exception("Get TPM Object", result);

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

		return;
	}

	~libhis_getpcr()
	{
		//clean up PCRS
		result = Tspi_Context_CloseObject(hcontext, hpcrs);
		if(result != TSS_SUCCESS) throw libhis_exception("Close PCRS", result);

		//close context
		result = Tspi_Context_Close(hcontext);
		if(result != TSS_SUCCESS) throw libhis_exception("Close Context", result);
	}

private:
	TSS_RESULT		result;
	TSS_HCONTEXT	hcontext;
	TSS_HTPM		htpm;
	TSS_HPCRS		hpcrs;
};

#endif
