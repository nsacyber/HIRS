#ifndef libhis_utils_hpp
#define libhis_utils_hpp

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

void hextouuid(unsigned char *hex, TSS_UUID &uuid)
{
	//process the unsigned long leading the UUID
	for(short i = 0; i < 8; i++)
	{
		if(hex[i] >= 48 && hex[i] <= 57)
			hex[i] -= 48;
		else if(hex[i] >= 65 && hex[i] <= 70)
			hex[i] -= 55;
		else if(hex[i] >= 97 && hex[i] <= 102)
			hex[i] -= 87;
		else
			throw libhis_exception("UUID validation failure", 420);
	}

	uuid.ulTimeLow = hex[0] * 268435456 + hex[1] * 16777216 + hex[2] * 1048576 +
		hex[3] * 65536 + hex[4] * 4096 + hex[5] * 256 + hex[6] * 16 + hex[7];

	//process the unsigned short for midtime
	for(short i = 9; i < 13; i++)
	{
		if(hex[i] >= 48 && hex[i] <= 57)
			hex[i] -= 48;
		else if(hex[i] >= 65 && hex[i] <= 70)
			hex[i] -= 55;
		else if(hex[i] >= 97 && hex[i] <= 102)
			hex[i] -= 87;
		else
			throw libhis_exception("UUID validation failure", 421);
	}

	uuid.usTimeMid = hex[9] * 4096 + hex[10] * 256 + hex[11] * 16 + hex[12];

	//process the unsigned short for hightime
	for(short i = 14; i < 18; i++)
	{
		if(hex[i] >= 48 && hex[i] <= 57)
			hex[i] -= 48;
		else if(hex[i] >= 65 && hex[i] <= 70)
			hex[i] -= 55;
		else if(hex[i] >= 97 && hex[i] <= 102)
			hex[i] -= 87;
		else
			throw libhis_exception("UUID validation failure", 422);
	}

	uuid.usTimeHigh = hex[14] * 4096 + hex[15] * 256 + hex[16] * 16 + hex[17];

	//process bClockSeqHigh
	for(short i = 19; i < 21; i++)
	{
		if(hex[i] >= 48 && hex[i] <= 57)
			hex[i] -= 48;
		else if(hex[i] >= 65 && hex[i] <= 70)
			hex[i] -= 55;
		else if(hex[i] >= 97 && hex[i] <= 102)
			hex[i] -= 87;
		else
			throw libhis_exception("UUID validation failure", 423);
	}

	uuid.bClockSeqHigh = hex[19] * 16 + hex[20];

	//process bClockSeqLow
	for(short i = 21; i < 23; i++)
	{
		if(hex[i] >= 48 && hex[i] <= 57)
			hex[i] -= 48;
		else if(hex[i] >= 65 && hex[i] <= 70)
			hex[i] -= 55;
		else if(hex[i] >= 97 && hex[i] <= 102)
			hex[i] -= 87;
		else
			throw libhis_exception("UUID validation failure", 424);
	}

	uuid.bClockSeqLow = hex[21] * 16 + hex[22];

	//process final 6 byte array
	for(short i = 24; i < 36; i++)
	{
		if(hex[i] >= 48 && hex[i] <= 57)
			hex[i] -= 48;
		else if(hex[i] >= 65 && hex[i] <= 70)
			hex[i] -= 55;
		else if(hex[i] >= 97 && hex[i] <= 102)
			hex[i] -= 87;
		else
			throw libhis_exception("UUID validation failure", 425);
	}

	uuid.rgbNode[0] = hex[24] * 16 + hex[25];
	uuid.rgbNode[1] = hex[26] * 16 + hex[27];
	uuid.rgbNode[2] = hex[28] * 16 + hex[29];
	uuid.rgbNode[3] = hex[30] * 16 + hex[31];
	uuid.rgbNode[4] = hex[32] * 16 + hex[33];
	uuid.rgbNode[5] = hex[34] * 16 + hex[35];

	return;
}

/*
 * masktobitmask function that does it the screwed up TCG way
 */
void masktobitmask(unsigned char *mask, bool binarray[24])
{
	int sequence[] = {1, 0, 3, 2, 5, 4};
	int i;

	//convert hex values to binary values while validating
	for(short j = 0; j < 6; j++)
	{
		i = sequence[j];

		switch(mask[i])
		{
			case 48:	//0
			{
				break;
			}
			case 49:	//1
			{
				binarray[j * 4 + 0] = true;
				break;
			}
			case 50:	//2
			{
				binarray[j * 4 + 1] = true;
				break;
			}
			case 51:	//3
			{
				binarray[j * 4 + 0] = true;
				binarray[j * 4 + 1] = true;
				break;
			}
			case 52:	//4
			{
				binarray[j * 4 + 2] = true;
				break;
			}
			case 53:	//5
			{
				binarray[j * 4 + 0] = true;
				binarray[j * 4 + 2] = true;
				break;
			}
			case 54:	//6
			{
				binarray[j * 4 + 1] = true;
				binarray[j * 4 + 2] = true;
				break;
			}
			case 55:	//7
			{
				binarray[j * 4 + 0] = true;
				binarray[j * 4 + 1] = true;
				binarray[j * 4 + 2] = true;
				break;
			}
			case 56:	//8
			{
				binarray[j * 4 + 3] = true;
				break;
			}
			case 57:	//9
			{
				binarray[j * 4 + 0] = true;
				binarray[j * 4 + 3] = true;
				break;
			}
			case 65:	//a
			case 97:	//A
			{
				binarray[j * 4 + 1] = true;
				binarray[j * 4 + 3] = true;
				break;
			}
			case 66:	//b
			case 98:	//B
			{
				binarray[j * 4 + 0] = true;
				binarray[j * 4 + 1] = true;
				binarray[j * 4 + 3] = true;
				break;
			}
			case 67:	//c
			case 99:	//C
			{
				binarray[j * 4 + 2] = true;
				binarray[j * 4 + 3] = true;
				break;
			}
			case 68:	//d
			case 100:	//D
			{
				binarray[j * 4 + 0] = true;
				binarray[j * 4 + 2] = true;
				binarray[j * 4 + 3] = true;
				break;
			}
			case 69:	//e
			case 101:	//E
			{
				binarray[j * 4 + 1] = true;
				binarray[j * 4 + 2] = true;
				binarray[j * 4 + 3] = true;
				break;
			}	
			case 70:	//f
			case 102:	//F
			{
				binarray[j * 4 + 0] = true;
				binarray[j * 4 + 1] = true;
				binarray[j * 4 + 2] = true;
				binarray[j * 4 + 3] = true;
				break;
			}
			default:
			{
				throw libhis_exception("Mask validation failure", 430);
			}
		}
	}

	return;
}

/*
 * Original masktobitmask function.
 */
/*void masktobitmask(unsigned char *mask, bool binarray[24])
{
	//convert hex values to binary values while validating
	for(short i = 0; i < 6; i++)
	{
		switch(mask[i])
		{
			case 48:	//0
			{
				break;
			}
			case 49:	//1
			{
				binarray[i * 4 + 3] = true;
				break;
			}
			case 50:	//2
			{
				binarray[i * 4 + 2] = true;
				break;
			}
			case 51:	//3
			{
				binarray[i * 4 + 3] = true;
				binarray[i * 4 + 2] = true;
				break;
			}
			case 52:	//4
			{
				binarray[i * 4 + 1] = true;
				break;
			}
			case 53:	//5
			{
				binarray[i * 4 + 3] = true;
				binarray[i * 4 + 1] = true;
				break;
			}
			case 54:	//6
			{
				binarray[i * 4 + 2] = true;
				binarray[i * 4 + 1] = true;
				break;
			}
			case 55:	//7
			{
				binarray[i * 4 + 3] = true;
				binarray[i * 4 + 2] = true;
				binarray[i * 4 + 1] = true;
				break;
			}
			case 56:	//8
			{
				binarray[i * 4 + 0] = true;
				break;
			}
			case 57:	//9
			{
				binarray[i * 4 + 3] = true;
				binarray[i * 4 + 0] = true;
				break;
			}
			case 65:	//a
			case 97:	//A
			{
				binarray[i * 4 + 2] = true;
				binarray[i * 4 + 0] = true;
				break;
			}
			case 66:	//b
			case 98:	//B
			{
				binarray[i * 4 + 3] = true;
				binarray[i * 4 + 2] = true;
				binarray[i * 4 + 0] = true;
				break;
			}
			case 67:	//c
			case 99:	//C
			{
				binarray[i * 4 + 1] = true;
				binarray[i * 4 + 0] = true;
				break;
			}
			case 68:	//d
			case 100:	//D
			{
				binarray[i * 4 + 3] = true;
				binarray[i * 4 + 1] = true;
				binarray[i * 4 + 0] = true;
				break;
			}
			case 69:	//e
			case 101:	//E
			{
				binarray[i * 4 + 2] = true;
				binarray[i * 4 + 1] = true;
				binarray[i * 4 + 0] = true;
				break;
			}
			case 70:	//f
			case 102:	//F
			{
				binarray[i * 4 + 3] = true;
				binarray[i * 4 + 2] = true;
				binarray[i * 4 + 1] = true;
				binarray[i * 4 + 0] = true;
				break;
			}
			default:
			{
				throw libhis_exception("Mask validation failure", 1);
			}
		}
	}

	return;
}*/

#endif
