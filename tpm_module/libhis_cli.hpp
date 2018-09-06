#ifndef libhis_cli_hpp
#define libhis_cli_hpp

#include "libhis_takeownership.hpp"
#include "libhis_changeownership.hpp"
#include "libhis_clearownership.hpp"
#include "libhis_createek.hpp"
#include "libhis_changesrksecret.hpp"
#include "libhis_collateidentityrequest.hpp"
#include "libhis_activateidentity.hpp"
#include "libhis_quote.hpp"
#include "libhis_seal.hpp"
#include "libhis_unseal.hpp"
#include "libhis_getrandombytes.hpp"
#include "libhis_createkey.hpp"
#include "libhis_changekeyauth.hpp"
#include "libhis_getkeyblob.hpp"
#include "libhis_getkeymodulus.hpp"
#include "libhis_getpubkey.hpp"
#include "libhis_clearkey.hpp"
#include "libhis_getpcr.hpp"
#include "libhis_extendpcr.hpp"
#include "libhis_clearpcr.hpp"
#include "libhis_setnvdata.hpp"
#include "libhis_getnvdata.hpp"
#include "libhis_clearnvdata.hpp"
#include "libhis_sign.hpp"
#include "libhis_verifysignature.hpp"
#include "libhis_bind.hpp"
#include "libhis_unbind.hpp"

#include "libhis_exception.hpp"

#include <iostream>
#include <iomanip>
#include <string>
#include <exception>
#include <fstream>
#include <time.h>

#ifdef LINUX
	#include <string.h>
	#include <stdlib.h>
#endif

using namespace std;

/*************************************************************************
	GLOBAL CONSTANT BLOCK
 *************************************************************************/
const char cVersion[] = "3.13";	//identify the source revision
const char cTpmVersion[] = "1.2";	//identify the tpm spec level supported

/**
 * 
 * @param argumentCount
 * @param argumentValues
 */
class libhis_cli
{
/*************************************************************************
	PUBLIC FUNCTION BLOCK
 *************************************************************************/
public:
        /**
         * Constructor tasked with initializing the class. You must run this
         * first followed by the cli function. The rest is controlled by command
         * inputs.
         * @param argumentCount
         * @param argumentValues
         */
	libhis_cli(int argumentCount, char **argumentValues)
	{
		//set defaults
		bdebug = false;
		blog = false;
		bhelp = false;
		bmode = false;
		bversion = false;
		bzeros = false;
		breadable = false;
		imode = 0;
		iresult = 0;

		//associate inputs
		argc = argumentCount;
		argv = argumentValues;

		//populate booleans
		for(int i = 0; i < argc; i++)
		{
			if(strcasecmp(argv[i], "-h") == 0 || strcasecmp(argv[i], "-help") == 0 || strcasecmp(argv[i], "--help") == 0)
				bhelp = true;
			if(strcasecmp(argv[i], "-r") == 0 || strcasecmp(argv[i], "-readable") == 0 || strcasecmp(argv[i], "--readable") == 0)
				breadable = true;
			else if(strcasecmp(argv[i], "-d") == 0 || strcasecmp(argv[i], "-debug") == 0 || strcasecmp(argv[i], "--debug") == 0)
				bdebug = true;
			else if(strcasecmp(argv[i], "-f") == 0 || strcasecmp(argv[i], "-file") == 0 || strcasecmp(argv[i], "--log") == 0)
				blog = true;
			else if(strcasecmp(argv[i], "-m") == 0 || strcasecmp(argv[i], "-mode") == 0 || strcasecmp(argv[i], "--mode") == 0)
			{
				i++;								//step i forward
				if(i < argc && atoi(argv[i]) > 0)	//check bounds and positive mode value
				{
					imode = atoi(argv[i]);
					bmode = true;
				}
				else
					bmode = false;
			}
			else if(strcasecmp(argv[i], "-v") == 0 || strcasecmp(argv[i], "-version") == 0 || strcasecmp(argv[i], "--version") == 0)
				bversion = true;
			else if(strcasecmp(argv[i], "-z") == 0 || strcasecmp(argv[i], "-zeros") == 0 || strcasecmp(argv[i], "--zeros") == 0)
				bzeros = true;
		}
	}

        /**
         * Worker function that actually accomplishes stuff. This function will
         * return a result code.
         * @return 
         */
	unsigned long cli()
	{
		try
		{
			if(bversion)
				cout << cVersion << endl;
			else
			{
				if(bmode)
				{
					switch(imode)
					{
						//valid mode handling cases
						case 1:
							takeownership();
							break;
						case 2:
							changeownership();
							break;
						case 3:
							clearownership();
							break;
						case 4:
							createek();
							break;
						case 5:
							changesrksecret();
							break;
						case 6:
							collateidentityrequest();
							break;
						case 7:
							activateidentity();
							break;
						case 8:
							quote();
							break;
						case 9:
							quote2();
							break;
						case 10:
							seal();
							break;
						case 11:
							seal2();
							break;
						case 12:
							unseal();
							break;
						case 13:
							getrandombytes();
							break;
						case 14:
							createkey();
							break;
						case 15:
							changekeyauth();
							break;
						case 16:
							getkeyblob();
							break;
						case 17:
							getmodulus();
							break;
						case 18:
							clearkey();
							break;
						case 19:
							getpcr();
							break;
						case 20:
							extendpcr();
							break;
						case 21:
							clearpcr();
							break;
						case 22:
							setnvdata();
							break;
						case 23:
							getnvdata();
							break;
						case 24:
							clearnvdata();
							break;
						case 25:
							sign();
							break;
						case 26:
							verifysignature();
							break;
						case 27:
							bind();
							break;
						case 28:
							unbind();
							break;
						case 29:
							getpubkey();
							break;
							
						//catch everything else case
						default:
							throw libhis_exception("Invalid mode argument", 300);
					}
				}
				else
					printHelp();
			}
		}
		catch(libhis_exception &e)
		{
			iresult = e.result;		//update the return code

			if(bdebug)			//print out error message if debugging is on
                        {
				cerr << e.what() << ' ' << e.result << endl;
                                error_helper(e.result);
                        }

			if(blog)			//write error file if logging is on
			{
				try
				{
					//set up the output file
					fstream file;
					file.open("tpm_module.txt", fstream::out | fstream::app);
					if(!file.is_open()) throw libhis_exception("Can't open log file", 290);

					//set up a time object to put in the output file
					time_t rawtime;
					struct tm* timeinfo;
					time(&rawtime);
					timeinfo = localtime(&rawtime);

					//write exception information
					file << e.what() << ' ' << e.result << ' ' << asctime(timeinfo);

					//close output file
					file.close();
				}
				catch(exception f)
				{
					//tell the user something went wrong with the output file
					cerr << "Output error: "  << f.what() << endl;
					iresult += 100000;
				}
			}
		}

		return iresult;		//careful -- sometimes Linux mucks with this value
	}

        /**
         * Default destructor. Nothing to do.
         */
	~libhis_cli()
	{
	}

/*************************************************************************
	PRIVATE VARIABLE BLOCK
 *************************************************************************/
private:
	enum	authType
	{
		AUTH_NEW,
		AUTH_TPM,
		AUTH_SRK,
		AUTH_IK,
		AUTH_SIGN,
		AUTH_BIND,
		AUTH_STOR,
		AUTH_ENC,
		AUTH_KEY
	};

	enum keyType
	{
		KEY_EK,
		KEY_SRK,
		KEY_IK,
		KEY_STOR,
		KEY_BIND,
		KEY_SIGN
	};

	bool	bdebug,
                blog,
                bhelp,
                bmode,
                bversion,
                bzeros,
                breadable;
	int     imode,
		argc;
	char	**argv;

	unsigned long	 iresult;

/*************************************************************************
	PRIVATE FUNCTION BLOCK
 *************************************************************************/
	/*
	 * Print Help
	 * Tell the user about everything they can do with this program.
	 */
	void printHelp()
	{
		cout << "TPM (Trusted Platform Module) Module" << endl
			 << "  Version is " << cVersion << endl
			 << "  TPM spec support level is " << cTpmVersion << endl
			 << endl
			 << "Mode List:" << endl
			 << "   1   Take Ownership of TPM" << endl
			 << "   2   Change Owner Authorization Data" << endl
			 << "   3   Clear Ownership (Disables TPM)" << endl
			 << "   4   Create EK" << endl
			 << "   5   Change SRK Authorization Data" << endl
			 << "   6   Collate Identity Request (Create Identity Key)" << endl			//check
			 << "   7   Activate Identity (Create Identity Key Certificate)" << endl	//check
			 << "   8   Quote" << endl
			 << "   9   Quote 2" << endl
			 << "  10   Seal Data (Encrypt Data to Current Platform State)" << endl
			 << "  11   Seal 2 (Seal Against Future PCRs)" << endl
			 << "  12   Unseal Data" << endl
			 << "  13   Generate Random Bytes" << endl
			 << "  14   Create Signing, Binding, or Storage Key" << endl
			 << "  15   Change Key Authorization Data" << endl
			 << "  16   Get Keyblob" << endl
			 << "  17   Get Key Modulus" << endl
			 << "  18   Clear Key" << endl
			 << "  19   Get PCR" << endl
			 << "  20   Extend PCR (Update PCR Value)" << endl
			 << "  21   Clear PCR" << endl												//locality
			 << "  22   Set NVRAM Data" << endl											//check
			 << "  23   Get NVRAM Data" << endl											//check
			 << "  24   Clear NVRAM Data" << endl										//check
			 << "  25   Sign Data" << endl
			 << "  26   Verify Signed Data" << endl
			 << "  27   Bind" << endl
			 << "  28   Unbind" << endl
			 << "  29   Get Public Key" << endl
			 << endl
			 << "Default Commands List:" << endl
			 << "  -m <int> | -mode <int>    Set a mode from list above" << endl
			 << "  -h | -help                Display help, can combine with mode" << endl
			 << "  -v | -version             Display software version info" << endl
			 << "  -d | -debug               Enable console debugging" << endl
			 << "  -f | -file                Write debugging info to file" << endl
			 << "  -z | -zeros               Automatically fills in auth data with zeros" << endl
			 << "  -r | -readable            Make output human-readable with delimeters" << endl
			 << "  -nr | -nonce_random       Populate nonce with TPM's random byte generator" << endl
			 << endl
			 << "Example Commands:" << endl
			 << "  Take ownership of TPM using a specific nonce and zeros for auth data:" << endl
			 << "  tpm_module -m 1 -n 0123456789012345678901234567890123456789 -z" << endl
			 << endl
			 << "  Get help with collate identity request" << endl
			 << "  tpm_module -m 6 -h" << endl
			 << endl
			 << "  Generate a quote2 using the first 16 PCRs, random nonce, identity key with" << endl
			 << "    simple UUID, awful password, and omitted srk auth as zeros:" << endl
			 << "  tpm_module -m 9 -p ffff00 -nr -u 00000000-0000-0000-0000-040000000001" << endl
			 << "    -authp_ik password -z" << endl
			 << endl;
	}

	unsigned char* hexToBin(char *input)
	{
		//every hex string must have an even number of characters
		if((strlen(input) % 2) != 0) throw libhis_exception("Hex to Bin Invalid Length", 310);

		unsigned char *array = new unsigned char[(strlen(input) / 2)];	//new byte array
		unsigned long value = 0;								//variable to store hex value

		for(unsigned long i = 0; i < (strlen(input) / 2); i++)
		{
			//check first character
			if(input[i*2] >= 48 && input[i*2] <= 57)
			{
				value = (input[i*2] - 48) * 16;
			}
			else if(input[i*2] >= 65 && input[i*2] <= 70)
			{
				value = (input[i*2] - 55) * 16;
			}
			else if(input[i*2] >= 97 && input[i*2] <= 102)
			{
				value = (input[i*2] - 87) * 16;
			}
			else
			{	//validation failure so return null
				delete [] array;
				throw libhis_exception("Hex to Bin Character Validation Error", 311);
			}

			//check second character
			if(input[i*2+1] >= 48 && input[i*2+1] <= 57)
			{
				value += input[i*2+1] - 48;
			}
			else if(input[i*2+1] >= 65 && input[i*2+1] <= 70)
			{
				value += input[i*2+1] - 55;
			}
			else if(input[i*2+1] >= 97 && input[i*2+1] <= 102)
			{
				value += input[i*2+1] - 87;
			}
			else
			{	//validation failure so return null
				delete [] array;
				throw libhis_exception("Hex to Bin Character Validation Error", 312);
			}

			array[i] = value;							//set the byte values
		}

		return array;									//success!
	}

	void setupAuth(unsigned char *&value, unsigned long &size, bool &sha1, authType aType)
	{
		//loop over the argument array and return when match found
		for(int i = 0; i < argc; i++)
		{
			switch(aType)
			{
			case AUTH_NEW:
				{
					if(strcasecmp(argv[i], "-authp_new") == 0 && (i + 1) < argc)
					{
						value = (unsigned char*)argv[i + 1];
						size = strlen(argv[i + 1]);
						sha1 = false;
						return;
					}
					else if(strcasecmp(argv[i], "-auths_new") == 0 && (i + 1) < argc && strlen(argv[i + 1]) == 40)
					{
						value = hexToBin(argv[i + 1]);
						size = 20;
						sha1 = true;
						return;
					}
					break;
				}
			case AUTH_TPM:
				{
					if(strcasecmp(argv[i], "-authp_tpm") == 0 && (i + 1) < argc)
					{
						value = (unsigned char*)argv[i + 1];
						size = strlen(argv[i + 1]);
						sha1 = false;
						return;
					}
					else if(strcasecmp(argv[i], "-auths_tpm") == 0 && (i + 1) < argc && strlen(argv[i + 1]) == 40)
					{
						value = hexToBin(argv[i + 1]);
						size = 20;
						sha1 = true;
						return;
					}
					break;
				}
			case AUTH_SRK:
				{
					if(strcasecmp(argv[i], "-authp_srk") == 0 && (i + 1) < argc)
					{
						value = (unsigned char*)argv[i + 1];
						size = strlen(argv[i + 1]);
						sha1 = false;
						return;
					}
					else if(strcasecmp(argv[i], "-auths_srk") == 0 && (i + 1) < argc && strlen(argv[i + 1]) == 40)
					{
						value = hexToBin(argv[i + 1]);
						size = 20;
						sha1 = true;
						return;
					}
					break;
				}
			case AUTH_IK:
				{
					if(strcasecmp(argv[i], "-authp_ik") == 0 && (i + 1) < argc)
					{
						value = (unsigned char*)argv[i + 1];
						size = strlen(argv[i + 1]);
						sha1 = false;
						return;
					}
					else if(strcasecmp(argv[i], "-auths_ik") == 0 && (i + 1) < argc && strlen(argv[i + 1]) == 40)
					{
						value = hexToBin(argv[i + 1]);
						size = 20;
						sha1 = true;
						return;
					}
					break;
				}
			case AUTH_SIGN:
				{
					if(strcasecmp(argv[i], "-authp_sign") == 0 && (i + 1) < argc)
					{
						value = (unsigned char*)argv[i + 1];
						size = strlen(argv[i + 1]);
						sha1 = false;
						return;
					}
					else if(strcasecmp(argv[i], "-auths_sign") == 0 && (i + 1) < argc && strlen(argv[i + 1]) == 40)
					{
						value = hexToBin(argv[i + 1]);
						size = 20;
						sha1 = true;
						return;
					}
					break;
				}
			case AUTH_BIND:
				{
					if(strcasecmp(argv[i], "-authp_bind") == 0 && (i + 1) < argc)
					{
						value = (unsigned char*)argv[i + 1];
						size = strlen(argv[i + 1]);
						sha1 = false;
						return;
					}
					else if(strcasecmp(argv[i], "-auths_bind") == 0 && (i + 1) < argc && strlen(argv[i + 1]) == 40)
					{
						value = hexToBin(argv[i + 1]);
						size = 20;
						sha1 = true;
						return;
					}
					break;
				}
			case AUTH_STOR:
				{
					if(strcasecmp(argv[i], "-authp_stor") == 0 && (i + 1) < argc)
					{
						value = (unsigned char*)argv[i + 1];
						size = strlen(argv[i + 1]);
						sha1 = false;
						return;
					}
					else if(strcasecmp(argv[i], "-auths_stor") == 0 && (i + 1) < argc && strlen(argv[i + 1]) == 40)
					{
						value = hexToBin(argv[i + 1]);
						size = 20;
						sha1 = true;
						return;
					}
					break;
				}
			case AUTH_ENC:
				{
					if(strcasecmp(argv[i], "-authp_enc") == 0 && (i + 1) < argc)
					{
						value = (unsigned char*)argv[i + 1];
						size = strlen(argv[i + 1]);
						sha1 = false;
						return;
					}
					else if(strcasecmp(argv[i], "-auths_enc") == 0 && (i + 1) < argc && strlen(argv[i + 1]) == 40)
					{
						value = hexToBin(argv[i + 1]);
						size = 20;
						sha1 = true;
						return;
					}
					break;
				}
			case AUTH_KEY:
				{
					if(strcasecmp(argv[i], "-authp_key") == 0 && (i + 1) < argc)
					{
						value = (unsigned char*)argv[i + 1];
						size = strlen(argv[i + 1]);
						sha1 = false;
						return;
					}
					else if(strcasecmp(argv[i], "-auths_key") == 0 && (i + 1) < argc && strlen(argv[i + 1]) == 40)
					{
						value = hexToBin(argv[i + 1]);
						size = 20;
						sha1 = true;
						return;
					}
					break;
				}
			}
		}

		if(bzeros)
		{
			//if we made it here then default to well known secret
			value = new unsigned char[20];
			for(short i = 0; i < 20; i++) value[i] = 0x00;
			size = 20;
			sha1 = true;
			return;
		}

		//if we made it here then auth or zeros wasn't set
		throw libhis_exception("Auth argument missing", 320 + aType);
	}

	void setupNonce(unsigned char *&nonce)
	{
		//loop over the argument array and return when match found
		for(int i = 0; i < argc; i++)
		{
			if((strcasecmp(argv[i], "-n") == 0 || strcasecmp(argv[i], "-nonce") == 0) && (i + 1) < argc && strlen(argv[i + 1]) == 40)
			{
				nonce = hexToBin(argv[i + 1]);
				return;
			}
                        else if((strcasecmp(argv[i], "-nr") == 0 || strcasecmp(argv[i], "-nonce_random") == 0))
                        {
                            libhis_getrandombytes temp;
                            temp.getrandombytes(20, nonce);
                            return;
                        }
		}

		//we got here only if no nonce was provided so throw exception
		throw libhis_exception("Nonce argument missing", 330);
	}

	void setupOverwrite(bool &boverwrite)
	{
		for(int i = 0; i < argc; i++)
		{
			if(strcasecmp(argv[i], "-o") == 0 || strcasecmp(argv[i], "-overwrite") == 0)
				boverwrite = true;
		}

		return;
	}

	void setupUUID(unsigned char *&uuid)
	{
		//find uuid
		for(int i = 0; i < argc; i++)
		{
			if(((strcasecmp(argv[i], "-u") == 0 || strcasecmp(argv[i], "-uuid") == 0)) && i + 1 < argc && strlen(argv[i + 1]) == 36)
			{
				uuid = (unsigned char*)argv[i + 1];
				return;
			}
		}
			
		//can only get here if a UUID is not provided
		throw libhis_exception("UUID argument missing", 340);
	}

	void setupMask(unsigned char *&mask)
	{
		//find mask
		for(int i = 0; i < argc; i++)
		{
			if(((strcasecmp(argv[i], "-p") == 0 || strcasecmp(argv[i], "-pcrs") == 0)) && i + 1 < argc && strlen(argv[i + 1]) == 6)
			{
				mask = (unsigned char*)argv[i + 1];
				return;
			}
		}
			
		//can only get here if a mask is not provided
		throw libhis_exception("PCRS argument missing", 350);
	}

	void setupKeyType(int &keytype)
	{
		//find key type
		for(int i = 0; i < argc; i++)
		{
			if((strcasecmp(argv[i], "-t") == 0 || strcasecmp(argv[i], "-type") == 0) && i + 1 < argc)
			{
				if(strcasecmp(argv[i+1], "sign") == 0)
					keytype = KEY_SIGN;
				else if(strcasecmp(argv[i+1], "bind") == 0)
					keytype = KEY_BIND;
				else if(strcasecmp(argv[i+1], "identity") == 0 || strcasecmp(argv[i+1], "ik") == 0 || strcasecmp(argv[i+1], "aik") == 0)
					keytype = KEY_IK;
				else if(strcasecmp(argv[i+1], "storage") == 0 || strcasecmp(argv[i+1], "stor") == 0)
					keytype = KEY_STOR;
				else if(strcasecmp(argv[i+1], "ek") == 0)
					keytype = KEY_EK;
				else if(strcasecmp(argv[i+1], "srk") == 0)
					keytype = KEY_SRK;
				else throw libhis_exception("Key type argument invalid", 411);

				return;
			}
		}

		//can only get here if a key type is not provided
		throw libhis_exception("Key type argument missing", 360);
	}

	void setupLength(unsigned long &length, int keytype)
	{
		for(int i = 0; i < argc; i++)
		{
			if((strcasecmp(argv[i], "-l") == 0 || strcasecmp(argv[i], "-length") == 0) && i + 1 < argc)
			{
				length = atoi(argv[i + 1]);
				return;
			}
		}

		if(keytype != KEY_IK)
		{
			//can only get here if a key length is not provided
			//throw libhis_exception("Key length argument missing", 370);
			length = 2048;			//default to 2048 length
		}
	}

	void setupScheme(unsigned long &scheme, int keytype)
	{
		//find key scheme
		for(int i = 0; i < argc; i++)
		{
			if(((strcasecmp(argv[i], "-s") == 0 || strcasecmp(argv[i], "-scheme") == 0)) && i + 1 < argc)
			{
				if(keytype == KEY_SIGN)
				{
					if(strcasecmp(argv[i+1], "sha1") == 0)
						scheme = 0;
					else if(strcasecmp(argv[i+1], "der") == 0)
						scheme = 1;
					else
						//else throw libhis_exception("Key scheme invalid", 380);
						scheme = 0;		//default to SHA1

					return;
				}
				else if(keytype == KEY_BIND)
				{
					if(strcasecmp(argv[i+1], "pkcs") == 0)
						scheme = 0;
					else if(strcasecmp(argv[i+1], "soap") == 0)
						scheme = 1;
					else if(strcasecmp(argv[i+1], "cnt") == 0)
						scheme = 2;
					else if(strcasecmp(argv[i+1], "ofb") == 0)
						scheme = 3;
					else if(strcasecmp(argv[i+1], "pad") == 0)
						scheme = 4;
					else
						//else throw libhis_exception("Key scheme invalid", 380);
						scheme = 0;		//default to PKCS

					return;
				}
				else if(keytype == KEY_STOR)
				{
					if(strcasecmp(argv[i+1], "system") == 0)
						scheme = 0;
					else if(strcasecmp(argv[i+1], "user") == 0)
						scheme = 1;
					else
						//else throw libhis_exception("Key scheme invalid", 380);
						scheme = 0;		//default to system storage

					return;
				}
			}
		}

		//can only get here if a key type is not provided
		throw libhis_exception("Key scheme argument missing", 380);
	}

	void setupNVIndex(unsigned long &index)
	{
		for(int i = 0; i < argc; i++)
		{
			if((strcasecmp(argv[i], "-i") == 0 || strcasecmp(argv[i], "-index") == 0) && i + 1 < argc)
			{
					if(strcasecmp(argv[i+1], "ec") == 0)
						index = 0;
					else if(strcasecmp(argv[i+1], "cc") == 0)
						index = 1;
					else if(strcasecmp(argv[i+1], "pc") == 0)
						index = 2;
					else if(strcasecmp(argv[i+1], "pcc") == 0)
						index = 3;
					else throw libhis_exception("NV index argument invalid", 391);
				return;
			}
		}

		//can only get here if a key length is not provided
		throw libhis_exception("NVRAM index argument missing", 390);
	}

	void takeownership()
	{
		if(bhelp)
		{
			cout << "Take Ownership Mode" << endl
				 << "  Takes ownership of the TPM if not already taken. Normally returns 8 when already taken." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
                                 << "  -nonce <hex[40]> | -nonce_random         Nonce data as hex SHA1 hash" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  No output." << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool		auth_tpm_sha1 = false;
		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool		auth_srk_sha1 = false;
		unsigned char	*nonce = 0;

		try
		{
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupNonce(nonce);

			libhis_takeownership temp;
			temp.takeownership(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, auth_srk_value, auth_srk_size, auth_srk_sha1, nonce);
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(nonce != 0) delete [] nonce;
			throw e;
		}

		if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(nonce != 0) delete [] nonce;
		return;
	}

	void changeownership()
	{
		if(bhelp)
		{
			cout << "Change Owner Authorization Secret Mode" << endl
				 << "  Changes the owner auth data. Can also switch from sha1 to plain and back." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
				 << "  -auths_new <hex[40]> | -authp_new <str>  New owner auth in SHA1 or Plain mode" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  No output." << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool		auth_tpm_sha1 = false;
		unsigned char	*auth_new_value = 0;
		unsigned long	auth_new_size = 0;
		bool		auth_new_sha1 = false;

		try
		{
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
			setupAuth(auth_new_value, auth_new_size, auth_new_sha1, AUTH_NEW);

			libhis_changeownership temp;
			temp.changeownership(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, auth_new_value, auth_new_size, auth_new_sha1);
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
			if(auth_new_sha1 && auth_new_value != 0) delete [] auth_new_value;
			throw e;
		}

		if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
		if(auth_new_sha1 && auth_new_value != 0) delete [] auth_new_value;
		return;
	}

	void clearownership()
	{
		if(bhelp)
		{
			cout << "Clear Ownership and Disable TPM Mode" << endl
				 << "  Clears the owner authorization data and disables TPM." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
                                 << "  -clr | -clear                            Required flag confirms intent to clear" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  No output." << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool		auth_tpm_sha1 = false;
                bool            bConfirmedClear = false;

		try
		{
                        //NIARL_TPM_MODULE has mode 3 as collate identity request, so make sure user didn't accidentally trigger clear here when they intended collate identity request
                        for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-clr") == 0 || strcasecmp(argv[i], "-clear") == 0)) && i + 1 < argc)
				{
                                    bConfirmedClear = true;
				}
			}
                        
                        if(!bConfirmedClear) throw new libhis_exception("Clear TPM requires -clr | -clear flag.", 500);
                    
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);

			libhis_clearownership temp;
			temp.clearownership(auth_tpm_value, auth_tpm_size, auth_tpm_sha1);
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
			throw e;
		}

		if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
		return;
	}

	void createek()
	{
		if(bhelp)
		{
			cout << "Create EK (Endorsement Key) Mode" << endl
				 << "  Creates an EK if it doesn't already exist." << endl
				 << endl
				 << "Input:" << endl
				 << "  -nonce <hex[40]> | -nonce_random         Nonce data as hex SHA1 hash" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  No output." << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*nonce = 0;

		try
		{
			setupNonce(nonce);

			libhis_createek temp;
			temp.createek(nonce);
		}
		catch(libhis_exception &e)
		{
			if(nonce != 0) delete [] nonce;
			throw e;
		}

		if(nonce != 0) delete [] nonce;
		return;
	}

	void changesrksecret()
	{
		if(bhelp)
		{
			cout << "Change SRK (Storage Root Key) Authorization Secret Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_new <hex[40]> | -authp_new <str>  New SRK auth in SHA1 or Plain mode" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  No output." << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool		auth_tpm_sha1 = false;
		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool		auth_srk_sha1 = false;
		unsigned char	*auth_new_value = 0;
		unsigned long	auth_new_size = 0;
		bool		auth_new_sha1 = false;

		try
		{
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_new_value, auth_new_size, auth_new_sha1, AUTH_NEW);

			libhis_changesrksecret temp;
			temp.changesrksecret(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, auth_srk_value, auth_srk_size, auth_srk_sha1, auth_new_value, auth_new_size, auth_new_sha1);
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_new_sha1 && auth_new_value != 0) delete [] auth_new_value;
			throw e;
		}

		if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_new_sha1 && auth_new_value != 0) delete [] auth_new_value;
		return;
	}

	void collateidentityrequest()
	{
		if(bhelp)
		{
			cout << "Collate Identity Request (Create Idenity Key) Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_ik <hex[40]> | -authp_ik <str>    IK auth in SHA1 or Plain mode" << endl
				 << "  -p <hex> | -acak <hex>                   Attestation CA public Key blob" << endl
				 << "  -l <str> | -label <str>                  IK creation label" << endl
				 << "  -u <hex[36]> | -uuid <hex[36]>           IK UUID for storage and retrieval" << endl
				 << "  -e <hex[n]> | -ekc <hex[n]>   (optional) Load EKC as hex datablob argument" << endl
				 << "  -pc <hex[n]>                  (optional) Load PC as hex datablob argument" << endl
				 << "  -n | -nvram                   (optional) Flag to load EKC and/or PC from NVRAM" << endl
				 << "    Note: Do not use -e|-ekc|-pc and -n|-nvram together. Will throw error." << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << "  -o | -overwrite                          Overwrite existing key at same UUID" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  Hex[n] identity request" << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool		auth_tpm_sha1 = false;
		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool		auth_srk_sha1 = false;
		unsigned char	*auth_ik_value = 0;
		unsigned long	auth_ik_size = 0;
		bool		auth_ik_sha1 = false;
		unsigned char	*label_ik_value = 0;
		unsigned long	label_ik_size = 0;
		unsigned char	*key_acak_value = 0;
		unsigned long	key_acak_size = 0;
		unsigned char	*uuid_ik_value = 0;
		bool		uuid_overwrite = false;
		unsigned char	*ekc_value = 0;
		unsigned long	ekc_size = 0;
		unsigned char   *pc_value = 0;
		unsigned long   pc_size = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_ik_value, auth_ik_size, auth_ik_sha1, AUTH_IK);
			setupOverwrite(uuid_overwrite);
			setupUUID(uuid_ik_value);

			//find acak
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-p") == 0 || strcasecmp(argv[i], "-acak") == 0)) && i + 1 < argc)
				{
					key_acak_value = hexToBin(argv[i + 1]);
					key_acak_size = strlen(argv[i + 1]) / 2;
				}
			}
			if(key_acak_value == 0) throw libhis_exception("ACAK argument", 410);

			//find label
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-l") == 0 || strcasecmp(argv[i], "-label") == 0)) && i + 1 < argc)
				{
					label_ik_value = (unsigned char*)argv[i + 1];
					label_ik_size = strlen(argv[i + 1]);
				}
			}
			if(label_ik_value == 0) throw libhis_exception("Label argument", 411);

			//get the EKC and PC from NVRAM if desired
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-n") == 0 || strcasecmp(argv[i], "-nvram") == 0)) && i + 1 < argc)
				{
					libhis_getnvdata temp;
					temp.getnvdata(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, 0, ekc_value, ekc_size);
					libhis_getnvdata temp2;
					temp2.getnvdata(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, 2, pc_value, pc_size);
				}
			}

			//get the EKC if desired
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-e") == 0 || strcasecmp(argv[i], "-ekc") == 0)) && i + 1 < argc)
				{
					if(ekc_value != 0)
						throw libhis_exception("NVRAM and EKC argument collision.", 412);

					ekc_value = hexToBin(argv[i + 1]);
					ekc_size = strlen(argv[i + 1]) / 2;
				}
			}

			for(int i = 0; i < argc; i++)
			{   // PC requires EKC
				if((ekc_size > 0) && (strcasecmp(argv[i], "-pc") == 0) && i + 1 < argc)
				{
					if(pc_value != 0)
						throw libhis_exception("NVRAM and PC argument collision.", 412);

					pc_value = hexToBin(argv[i + 1]);
					pc_size = strlen(argv[i + 1]) / 2;
				} else if ((ekc_size <= 0) && (strcasecmp(argv[i], "-pc") == 0) && i + 1 < argc) {
					throw libhis_exception("PC expects EKC to be provided", 412);
				}
			}

			libhis_collateidentityrequest temp;
			temp.init();
			temp.collateidentityrequest(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, auth_srk_value, auth_srk_size, auth_srk_sha1, auth_ik_value, auth_ik_size, auth_ik_sha1, label_ik_value, label_ik_size, key_acak_value, key_acak_size, uuid_ik_value, uuid_overwrite, ekc_value, ekc_size, pc_value, pc_size, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_ik_sha1 && auth_ik_value != 0) delete [] auth_ik_value;
			if(output_value != 0) delete [] output_value;
			if(ekc_value != 0) delete [] ekc_value;
			throw e;
		}

		if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_ik_sha1 && auth_ik_value != 0) delete [] auth_ik_value;
		if(output_value != 0) delete [] output_value;
		if(ekc_value != 0) delete [] ekc_value;
		return;
	}

	void activateidentity()
	{
		if(bhelp)
		{
			cout << "Activiate Identity Request (Create Idenity Key Cert) Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_ik <hex[40]> | -authp_ik <str>    IK auth in SHA1 or Plain mode" << endl
				 << "  -a <hex> | -asym <hex>                   ACA Asymmetric response blob" << endl
				 << "  -s <hex> | -sym <hex>                    ACA Symmetric response blob" << endl
				 << "  -u <hex[36]> | -uuid <hex[36]>           IK UUID for storage and retrieval" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  Hex[n] identity credential." << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool			auth_tpm_sha1 = false;
		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_ik_value = 0;
		unsigned long	auth_ik_size = 0;
		bool			auth_ik_sha1 = false;
		unsigned char	*asym_value = 0;
		unsigned long	asym_size = 0;
		unsigned char	*sym_value = 0;
		unsigned long	sym_size = 0;
		unsigned char	*uuid_ik_value = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_ik_value, auth_ik_size, auth_ik_sha1, AUTH_IK);
			setupUUID(uuid_ik_value);

			//find ASYM
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-a") == 0 || strcasecmp(argv[i], "-asym") == 0)) && i + 1 < argc)
				{
					asym_value = hexToBin(argv[i + 1]);
					asym_size = strlen(argv[i + 1]) / 2;
				}
			}
			if(asym_value == 0) throw libhis_exception("ASYM argument", 410);

			//find SYM
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-s") == 0 || strcasecmp(argv[i], "-sym") == 0)) && i + 1 < argc)
				{
					sym_value = hexToBin(argv[i + 1]);
					sym_size = strlen(argv[i + 1]) / 2;
				}
			}
			if(sym_value == 0) throw libhis_exception("SYM argument", 411);

			libhis_activateidentity temp;
			temp.init();
			temp.activateidentity(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, auth_srk_value, auth_srk_size, auth_srk_sha1, auth_ik_value, auth_ik_size, auth_ik_sha1, asym_value, asym_size, sym_value, sym_size, uuid_ik_value, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_ik_sha1 && auth_ik_value != 0) delete [] auth_ik_value;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_tpm_sha1 && auth_tpm_value != 0) delete [] auth_tpm_value;
		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_ik_sha1 && auth_ik_value != 0) delete [] auth_ik_value;
		if(output_value != 0) delete [] output_value;
		return;
	}

	void quote()
	{
		if(bhelp)
		{
			cout << "Quote Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_ik <hex[40]> | -authp_ik <str>    IK auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36]> | -uuid <hex[36]>           IK UUID for storage and retrieval" << endl
				 << "  -n <hex[40]> | -nonce <hex[40]> | -nr    Nonce data as hex SHA1 hash" << endl
				 << "  -p <hex[6]> | -pcrs <hex[6]>             PCR selection mask low to high" << endl
				 << "    NOTE: PCR selection mask must be 6 characters (3 bytes). Under Trousers" << endl
				 << "      all data will be properly quoted. NTru will ignore third byte. You" << endl
				 << "      must always provide 6 characters (3 bytes) even for NTru." << endl
				 << "    NOTE: The TCG mask is as follows:" << endl
				 << "      7,6,5,4,3,2,1,0,15,14,13,12,11,10,9,8" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  TPM_QUOTE_INFO hex datablob" << endl
				 << "  Signature Hex Datablob" << endl
				 << "  One PCR SHA1 Hex Datablob Per Line" << endl
				 << "    NOTE: All outputs on same line without delimiters. Use -r to add newlines." << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_ik_value = 0;
		unsigned long	auth_ik_size = 0;
		bool			auth_ik_sha1 = false;
		unsigned char	*nonce = 0;
		unsigned char	*uuid_ik_value = 0;
		unsigned char	*mask = 0;
		unsigned char	*output_pcrs_value = 0;
		unsigned long	output_pcrs_size = 0;
		unsigned char	*output_quote_value = 0;
		unsigned long	output_quote_size = 0;
		unsigned char	*output_sig_value = 0;
		unsigned long	output_sig_size = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_ik_value, auth_ik_size, auth_ik_sha1, AUTH_IK);
			setupNonce(nonce);
			setupUUID(uuid_ik_value);
			setupMask(mask);

			libhis_quote temp;
			temp.init(false);
			temp.quote(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_ik_value, auth_ik_size, auth_ik_sha1, nonce, uuid_ik_value, mask, output_pcrs_value, output_pcrs_size, output_quote_value, output_quote_size, output_sig_value, output_sig_size);

			for(unsigned long i = 0; i < output_quote_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_quote_value[i];

			if(breadable) cout << endl;

			for(unsigned long i = 0; i < output_sig_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_sig_value[i];

			if(breadable) cout << endl;

			for(unsigned long i = 0; i < output_pcrs_size; i++)
			{
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_pcrs_value[i];
				if(breadable && (i + 1) % 20 == 0) cout << endl;
			}
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_ik_sha1 && auth_ik_value != 0) delete [] auth_ik_value;
			if(nonce != 0) delete [] nonce;
			if(output_pcrs_value != 0) delete [] output_pcrs_value;
			if(output_quote_value != 0) delete [] output_quote_value;
			if(output_sig_value != 0) delete [] output_sig_value;
			throw e;
		}

		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_ik_sha1 && auth_ik_value != 0) delete [] auth_ik_value;
		if(nonce != 0) delete [] nonce;
		if(output_pcrs_value != 0) delete [] output_pcrs_value;
		if(output_quote_value != 0) delete [] output_quote_value;
		if(output_sig_value != 0) delete [] output_sig_value;
		return;
	}

	void quote2()
	{
		if(bhelp)
		{
			cout << "Quote 2 Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_ik <hex[40]> | -authp_ik <str>    IK auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36]> | -uuid <hex[36]>           IK UUID for storage and retrieval" << endl
				 << "  -n <hex[40]> | -nonce <hex[40]> | -nr    Nonce data as hex SHA1 hash" << endl
				 << "  -p <hex[6]> | -pcrs <hex[6]>             PCR selection mask" << endl
                                 << "  -c | -capVerInfo                         Disable append TPM_CAP_VERSION_INFO to quote" << endl
				 << "    NOTE: TCG mask is as follows:" << endl
				 << "      7,6,5,4,3,2,1,0,15,14,13,12,11,10,9,8,23,22,21,20,19,18,17,16" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
                                 << "  -r | -readable                           Adds newlines to output" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  TPM_QUOTE_INFO2 in hex (concatenated with TSS_CAP_VERSION_INFO if enabled)" << endl
				 << "  Signature Hex Datablob" << endl
				 << "  Sequential listing of PCR values" << endl
				 << "    NOTE: All outputs on same line without delimiters. Use -r to add newlines." << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool		auth_srk_sha1 = false;
		unsigned char	*auth_ik_value = 0;
		unsigned long	auth_ik_size = 0;
		bool		auth_ik_sha1 = false;
		unsigned char	*nonce = 0;
		unsigned char	*uuid_ik_value = 0;
		unsigned char	*mask = 0;
		unsigned char	*output_pcrs_value = 0;
		unsigned long	output_pcrs_size = 0;
		unsigned char	*output_quote_value = 0;
		unsigned long	output_quote_size = 0;
		unsigned char	*output_sig_value = 0;
		unsigned long	output_sig_size = 0;
                bool            bCapVersion = true;     //flipped for backwards compatibility with 3.0x

		try
		{
                        //TPM_CAP_VERSION_INFO can cause Broadcom TPMs to crash and Infineon TPMS to output invalid, non-printing characters
                        for(int i = 0; i < argc; i++)
			{
				if((strcasecmp(argv[i], "-c") == 0 || strcasecmp(argv[i], "-capVerInfo") == 0))
				{
                                    bCapVersion = false;
				}
			}
                    
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_ik_value, auth_ik_size, auth_ik_sha1, AUTH_IK);
			setupNonce(nonce);
			setupUUID(uuid_ik_value);
			setupMask(mask);

			libhis_quote temp;
			temp.init(true);
			temp.quote2(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_ik_value, auth_ik_size, auth_ik_sha1, nonce, uuid_ik_value, mask, output_pcrs_value, output_pcrs_size, output_quote_value, output_quote_size, output_sig_value, output_sig_size, bCapVersion);

			for(unsigned long i = 0; i < output_quote_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_quote_value[i];

			if(breadable) cout << endl;

			for(unsigned long i = 0; i < output_sig_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_sig_value[i];

			if(breadable) cout << endl;

			for(unsigned long i = 0; i < output_pcrs_size; i++)
			{
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_pcrs_value[i];
				if(breadable && (i + 1) % 20 == 0) cout << endl;
			}
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_ik_sha1 && auth_ik_value != 0) delete [] auth_ik_value;
			if(nonce != 0) delete [] nonce;
			if(output_pcrs_value != 0) delete [] output_pcrs_value;
			if(output_quote_value != 0) delete [] output_quote_value;
			if(output_sig_value != 0) delete [] output_sig_value;
			throw e;
		}

		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_ik_sha1 && auth_ik_value != 0) delete [] auth_ik_value;
		if(nonce != 0) delete [] nonce;
		if(output_pcrs_value != 0) delete [] output_pcrs_value;
		if(output_quote_value != 0) delete [] output_quote_value;
		if(output_sig_value != 0) delete [] output_sig_value;
		return;
	}

	/*
	 * Seal
	 * Traditional data sealing mechanism. The SRK and current platform PCR
	 * values will be used to encrypt data. Encrypted data may only be
	 * decrypted if the system has the same PCR state and same SRK.
	 */
	void seal()
	{
		if(bhelp)
		{
			cout << "Seal Mode" << endl
				 << "  Uses current PCR state to encrypt data. Can only be decrypted with same PCRS." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_enc <hex[40]> | -authp_enc <str>  ENCdata auth in SHA1 or Plain mode" << endl
				 << "    NOTE: Windows allows all 24 PCRS. Linux allows only 16 PCRS." << endl
				 << "  -p <hex[6]> | -pcrs <hex[6]>   (Windows) PCR selection mask low to high" << endl
				 << "  -p <hex[4]00> | -pcrs <hex[4]00> (Linux) PCR selection mask low to high" << endl
				 << "    NOTE: TCG mask is as follows:" << endl
				 << "      7,6,5,4,3,2,1,0,15,14,13,12,11,10,9,8,23,22,21,20,19,18,17,16" << endl
				 << "  -e <hex[n]> | -encdata <hex[n]>          Datablob to be sealed" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  Hex[n] encrypted datablob" << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_enc_value = 0;
		unsigned long	auth_enc_size = 0;
		bool			auth_enc_sha1 = false;
		unsigned char	*mask = 0;
		unsigned char	*payload_value = 0;
		unsigned long	payload_size = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_enc_value, auth_enc_size, auth_enc_sha1, AUTH_ENC);
			setupMask(mask);

			//find unencrypted payload
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-e") == 0 || strcasecmp(argv[i], "-encdata") == 0)) && i + 1 < argc)
				{
					payload_value = hexToBin(argv[i + 1]);
					payload_size = strlen(argv[i + 1]) / 2;
				}
			}
			if(payload_value == 0) throw libhis_exception("ENC payload argument", 410);

			libhis_seal temp;
			temp.seal(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_enc_value, auth_enc_size, auth_enc_sha1, mask, payload_value, payload_size, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_enc_sha1 && auth_enc_value != 0) delete [] auth_enc_value;
			if(payload_value != 0) delete [] payload_value;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_enc_sha1 && auth_enc_value != 0) delete [] auth_enc_value;
		if(output_value != 0) delete [] output_value;
		return;
	}

	/*
	 * Seal 2
	 * Seals data against PCR values that differ from the present ones. This
	 * allows encrypted data to be decrypted at some later or different
	 * platform state. The SRK must remain constant though! Really useful for
	 * dual boot situations or for keying up "golden" images.
	 */
	void seal2()
	{
		if(bhelp)
		{
			cout << "Seal2 Mode" << endl
				 << "  Seals PCRS against user-defined release values. **Not available on Linux." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_enc <hex[40]> | -authp_enc <str>  ENCdata auth in SHA1 or Plain mode" << endl
				 << "  -p <hex[6]> | -pcrs <hex[6]>             PCR selection mask low to high" << endl
				 << "  -e <hex[n]> | -encdata <hex[n]>          Datablob to be sealed" << endl
				 << "  -r <hex[40xn]> | -release <hex[40xn]>    PCR values at release" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  Hex[n] encrypted datablob" << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_enc_value = 0;
		unsigned long	auth_enc_size = 0;
		bool			auth_enc_sha1 = false;
		unsigned char	*mask = 0;
		unsigned char	*payload_value = 0;
		unsigned long	payload_size = 0;
		unsigned char	*release_value = 0;
		unsigned long	release_size = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_enc_value, auth_enc_size, auth_enc_sha1, AUTH_ENC);
			setupMask(mask);

			//find unencrypted payload
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-e") == 0 || strcasecmp(argv[i], "-encdata") == 0)) && i + 1 < argc)
				{
					payload_value = hexToBin(argv[i + 1]);
					payload_size = strlen(argv[i + 1]) / 2;
				}
			}
			if(payload_value == 0) throw libhis_exception("ENC payload argument", 410);

			//find pcr release state list
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-r") == 0 || strcasecmp(argv[i], "-release") == 0)) && i + 1 < argc && strlen(argv[i+1]) % 20 == 0)
				{
					//cout <<  << endl;
					release_value = hexToBin(argv[i + 1]);
					release_size = strlen(argv[i + 1]) / 2;
				}
			}
			if(release_value == 0) throw libhis_exception("PCR release argument size error", 410);

			//run a size comparison against the mask and release_value
			bool bitmask[24];
			for(short i = 0; i < 24; i++)
				bitmask[i] = 0;
			masktobitmask(mask, bitmask);

			unsigned short counter = 0;
			for(short i = 0; i < 24; i++)
			{
				if(bitmask[i])
					counter++;
			}

			if(counter * 20 != release_size) throw libhis_exception("PCR release argument count mismatch", 410);

			libhis_seal temp;
			temp.seal2(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_enc_value, auth_enc_size, auth_enc_sha1, mask, payload_value, payload_size, release_value, release_size, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_enc_sha1 && auth_enc_value != 0) delete [] auth_enc_value;
			if(payload_value != 0) delete [] payload_value;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_enc_sha1 && auth_enc_value != 0) delete [] auth_enc_value;
		if(output_value != 0) delete [] output_value;
		return;
	}

	/*
	 * Unseal
	 * Unseals sealed data. Always uses the current set of PCRs and SRK to
	 * decrypt with. Supports both Seal and Seal2.
	 */
	void unseal()
	{
		if(bhelp)
		{
			cout << "Unseal Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_enc <hex[40]> | -authp_enc <str>  ENCdata auth in SHA1 or Plain mode" << endl
				 << "  -e <hex> | -encdata <hex>                Datablob to be unsealed" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  Hex[n] decrypted Hex Datablob" << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_enc_value = 0;
		unsigned long	auth_enc_size = 0;
		bool			auth_enc_sha1 = false;
		unsigned char	*payload_value = 0;
		unsigned long	payload_size = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_enc_value, auth_enc_size, auth_enc_sha1, AUTH_ENC);

			//find unencrypted payload
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-e") == 0 || strcasecmp(argv[i], "-encdata") == 0)) && i + 1 < argc)
				{
					payload_value = hexToBin(argv[i + 1]);
					payload_size = strlen(argv[i + 1]) / 2;
				}
			}
			if(payload_value == 0) throw libhis_exception("ENC payload argument", 410);

			libhis_unseal temp;
			temp.unseal(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_enc_value, auth_enc_size, auth_enc_sha1, payload_value, payload_size, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_enc_sha1 && auth_enc_value != 0) delete [] auth_enc_value;
			if(payload_value != 0) delete [] payload_value;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_enc_sha1 && auth_enc_value != 0) delete [] auth_enc_value;
		if(output_value != 0) delete [] output_value;
		return;
	}

	/*
	 * Get Random Bytes
	 * Acquires random data using the TPM's hardware random generator.
	 */
	void getrandombytes()
	{
		if(bhelp)
		{
			cout << "Get Random Bytes Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -b <int> | -bytes <int>                  Byte count integer" << endl
				 << endl
				 << "Outputs:" << endl
				 << "  Hex[n] random datablob" << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned long	bytes_size = 0;
		unsigned char	*bytes_value = 0;

		try
		{
			//find the byte count
			for(int i = 0; i < argc; i++)
			{
				if((strcasecmp(argv[i], "-b") == 0 || strcasecmp(argv[i], "-bytes") == 0) && (i + 1) < argc)
				{
					bytes_size = strtoul(argv[i + 1], 0, 10);
					break;
				}
			}
			if(bytes_size == 0) throw libhis_exception("Byte count argument", 410);

			libhis_getrandombytes temp;
			temp.getrandombytes(bytes_size, bytes_value);

			for(unsigned long i = 0; i < bytes_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)bytes_value[i];
		}
		catch(libhis_exception &e)
		{
			if(bytes_value != 0) delete [] bytes_value;
			throw e;
		}

		if(bytes_value != 0) delete [] bytes_value;
		return;
	}

	/*
	 * Create Key
	 * Creates a signing or binding key in multiple sizes. Storage keys are not
	 * supported since vendor compliance is spotty. All keys are stored under
	 * the SRK.
	 */
	void createkey()
	{
		if(bhelp)
		{
			cout << "Create Key Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_key <hex[40]> | -authp_key <str>  New key auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36] | -uuid <hex[36]>            Key UUID for storage and retrieval" << endl
				 << "  -t <str> | -type <str>                   Key type SIGN, BIND, or STORAGE" << endl
				 << "  -l <int> | -length <int>                 Key length 512, 1024, 2048, 4096, 8192" << endl
				 << "  -s <str> | -scheme <str>                 Scheme for key functionality" << endl
				 << "    Signing key only: SHA1 [default] or DER signing method" << endl
				 << "    Binding key only: PKCS [default], SOAP, CNT, OFB, or PAD encryption method" << endl
				 << "    Storage key only: SYSTEM [default] or USER key storage location" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << "  -o | -overwrite                          Overwrite existing key at same UUID" << endl
				 << endl
				 << "Output:" << endl
				 << "  No output." << endl
				 << endl
				 << "Note:" << endl
				 << "  Storage keys can be created, manipulated, and cleared. However, they cannot be used to actually" << endl
				 << "  store other keys in this version. Key hierarchies are not supported by all TSS versions. At the" << endl
				 << "  time of development the leading 2 TSS solutions did not support key hierarchies therefore the" << endl
				 << "  ability to use them is not part of this software implementation. See code comments." << endl
				 /*
				  * DEVELOPERS -- If TSS solutions are fixed in the future simply allow storage keys to be
				  *               used as alternatives to the SRK in key management functions and also
				  *               signing and binding functions. You can accomplish this by asking for the
				  *               storage UUID. If the user enters SRK use the SRK. If the user enters a
				  *               valid UUID then load the associated storage key.
				  */
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_key_value = 0;
		unsigned long	auth_key_size = 0;
		bool			auth_key_sha1 = false;
		int				key_type = 0;
		unsigned char	*uuid_key_value = 0;
		unsigned long	key_length = 0;
		unsigned long	key_scheme = 0;
		bool			uuid_overwrite = false;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_KEY);
			setupUUID(uuid_key_value);
			setupKeyType(key_type);
			setupLength(key_length, key_type);
			setupScheme(key_scheme, key_type);

			libhis_createkey temp;

			if(key_type == KEY_SIGN)
				temp.initsign(key_length, key_scheme);
			else if(key_type == KEY_BIND)
				temp.initbind(key_length, key_scheme);
			else if(key_type == KEY_STOR)
				temp.initstorage(key_length, key_scheme);

			temp.createkey(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, uuid_key_value, uuid_overwrite);

		}
		catch(libhis_exception &e)
		{
			if(auth_srk_value != 0 && auth_srk_sha1) delete [] auth_srk_value;
			if(auth_key_value != 0 && auth_key_sha1) delete [] auth_key_value;
			throw e;
		}

		if(auth_srk_value != 0 && auth_srk_sha1) delete [] auth_srk_value;
		if(auth_key_value != 0 && auth_key_sha1) delete [] auth_key_value;
		return;
	}

	/*
	 * Change Key Authorization
	 * Authorization data can be changed for any identity, signing, binding,
	 * or storage key excluding the SRK (Note: Storage keys not supported by
	 * this program due to TSS vendors not always supporting key heirarchies
	 * for some bizarre reason -- it's part of the 1.2 spec, how is it still
	 * not supported?).
	 */
	void changekeyauth()
	{
		if(bhelp)
		{
			cout << "Change Key Auth Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_key <hex[40]> | -authp_key <str>  Key auth in SHA1 or Plain mode" << endl
				 << "  -auths_new <hex[40]> | -authp_new <str>  New key auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36] | -uuid <hex[36]>            Key UUID for storage and retrieval" << endl
				 << "  -t <str> | -type <str>                   Key type IDENTITY, SIGN, BIND, or STORAGE" << endl
				 << "  -l <int> | -length <int>                 Key length 512, 1024, 2048, 4096, 8192" << endl
				 << "    NOTE: length not used for identity keys" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  No output." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_key_value = 0;
		unsigned long	auth_key_size = 0;
		bool			auth_key_sha1 = false;
		unsigned char	*auth_new_value = 0;
		unsigned long	auth_new_size = 0;
		bool			auth_new_sha1 = false;
		unsigned char	*uuid_key_value = 0;
		int				key_type = 0;
		unsigned long	key_length = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_KEY);
			setupAuth(auth_new_value, auth_new_size, auth_new_sha1, AUTH_NEW);
			setupUUID(uuid_key_value);
			setupKeyType(key_type);
			setupLength(key_length, key_type);

			libhis_changekeyauth temp;

			if(key_type == KEY_IK)
				temp.initidentity();
			else if(key_type == KEY_SIGN)
				temp.initsign(key_length);
			else if(key_type == KEY_BIND)
				temp.initbind(key_length);
			else if(key_type == KEY_STOR)
				temp.initstorage(key_length);

			temp.changekeyauth(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, uuid_key_value, auth_new_value, auth_new_size, auth_new_sha1);
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_value != 0 && auth_srk_sha1) delete [] auth_srk_value;
			if(auth_key_value != 0 && auth_key_sha1) delete [] auth_key_value;
			if(auth_new_value != 0 && auth_new_sha1) delete [] auth_new_value;
			throw e;
		}

		if(auth_srk_value != 0 && auth_srk_sha1) delete [] auth_srk_value;
		if(auth_key_value != 0 && auth_key_sha1) delete [] auth_key_value;
		if(auth_new_value != 0 && auth_new_sha1) delete [] auth_new_value;
		return;
	}

	/*
	 * Get Keyblob
	 * Gets the keyblob for some binding, signing, and identity keys depending
	 * on key initialization values. Cannot get the keyblob for EK or SRK.
	 */
	void getkeyblob()
	{
		if(bhelp)
		{
			cout << "Get Key Blob Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_key <hex[40]> | -authp_key <str>  Key auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36] | -uuid <hex[36]>            Key UUID for storage and retrieval" << endl
				 << "  -t <str> | -type <str>                   Key type IDENTITY, SIGN, BIND, or STORAGE" << endl
				 << "  -l <int> | -length <int>                 Key length 512, 1024, 2048, 4096, 8192" << endl
				 << "    NOTE: length not used for identity keys" << endl
				 << "    NOTE: You cannot get the keyblob of an EK or SRK!" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  Hex[n] keyblob." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_key_value = 0;
		unsigned long	auth_key_size = 0;
		bool			auth_key_sha1 = false;
		unsigned char	*uuid_key_value = 0;
		int				key_type = 0;
		unsigned long	key_length = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_KEY);
			setupUUID(uuid_key_value);
			setupKeyType(key_type);
			setupLength(key_length, key_type);

			libhis_getkeyblob temp;

			if(key_type == KEY_IK)
				temp.initidentity();
			else if(key_type == KEY_SIGN)
				temp.initsign(key_length);
			else if(key_type == KEY_BIND)
				temp.initbind(key_length);
			else if(key_type == KEY_STOR)
				temp.initstorage(key_length);

			temp.getkeyblob(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, uuid_key_value, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_value != 0 && auth_srk_sha1) delete [] auth_srk_value;
			if(auth_key_value != 0 && auth_key_sha1) delete [] auth_key_value;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_srk_value != 0 && auth_srk_sha1) delete [] auth_srk_value;
		if(auth_key_value != 0 && auth_key_sha1) delete [] auth_key_value;
		if(output_value != 0) delete [] output_value;
	}

	/*
	 * Get Modulus
	 * Gets the key modulus of TPM and TSS-protected keys. Handles identity,
	 * signing, and binding keys. For legacy reasons EK and SRK support are
	 * also provided. EK and SRK return the public key, not the modulus, so
	 * they are no longer a documented feature of this mode.
	 */
	void getmodulus()
	{
		if(bhelp)
		{
			cout << "Get Key Modulus Mode" << endl
				 << "Acquires the modulus for an RSA key protected by the TPM. See Get Public Key for" << endl
				 << "acquiring the EK and SRK public key. EK public key will still be returned by" << endl
				 << "this function but is deprecated." << endl
				 << endl
				 //<< "Input Required For All Keys:" << endl
				 << "  -t <str> | -type <str>                   Key type IDENTITY, SIGN, BIND, or STORAGE" << endl
				 //<< endl
				 //<< "Required Inputs for both EK and SRK" << endl
				 //<< "  -auths_owner <hex[40]> | -authp_owner <str> Owner auth in SHA1 or Plain mode" << endl
				 //<< endl
				 //<< "Required Inputs for EK only" << endl
				 //<< "  -n <hex[40]> | -nonce <hex[40]> | -nr    Nonce data as hex SHA1 hash" << endl
				 //<< endl
				 //<< "Required Inputs For Identity, Sign, and Bind keys" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_key <hex[40]> | -authp_key <str>  Key auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36] | -uuid <hex[36]>            Key UUID for storage and retrieval" << endl
				 << "  -l <int> | -length <int>                 Key length 512, 1024, 2048, 4096, 8192" << endl
				 << "    NOTE: length not used for identity keys" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  Hex[n] key modulus." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool			auth_tpm_sha1 = false;
		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_key_value = 0;
		unsigned long	auth_key_size = 0;
		bool			auth_key_sha1 = false;
		unsigned char	*uuid_key_value = 0;
		int				key_type = 0;
		unsigned long	key_length = 0;
		unsigned char	*nonce = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupKeyType(key_type);

			libhis_getkeymodulus temp;

			if(key_type == KEY_IK)
			{
				setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
				setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_KEY);
				setupUUID(uuid_key_value);
				setupLength(key_length, key_type);

				temp.initidentity();
				temp.getkeymodulus(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, uuid_key_value, output_value, output_size);
			}
			else if(key_type == KEY_SIGN)
			{
				setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
				setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_KEY);
				setupUUID(uuid_key_value);
				setupLength(key_length, key_type);

				temp.initsign(key_length);
				temp.getkeymodulus(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, uuid_key_value, output_value, output_size);
			}
			else if(key_type == KEY_BIND)
			{
				setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
				setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_KEY);
				setupUUID(uuid_key_value);
				setupLength(key_length, key_type);

				temp.initbind(key_length);
				temp.getkeymodulus(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, uuid_key_value, output_value, output_size);
			}
			else if(key_type == KEY_STOR)
			{
				setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
				setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_KEY);
				setupUUID(uuid_key_value);
				setupLength(key_length, key_type);

				temp.initstorage(key_length);
				temp.getkeymodulus(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, uuid_key_value, output_value, output_size);
			}
			else if(key_type == KEY_EK)
			{
				setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
				setupNonce(nonce);

				temp.getpubek(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, nonce, output_value, output_size);
			}
			else if(key_type == KEY_SRK)
			{
				setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);

				temp.getpubsrk(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, output_value, output_size);
			}

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
			if(auth_srk_value != 0 && auth_srk_sha1) delete [] auth_srk_value;
			if(auth_key_value != 0 && auth_key_sha1) delete [] auth_key_value;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
		if(auth_srk_value != 0 && auth_srk_sha1) delete [] auth_srk_value;
		if(auth_key_value != 0 && auth_key_sha1) delete [] auth_key_value;
		if(output_value != 0) delete [] output_value;
		return;
	}

	/*
	 * Clear Key
	 * Clears a key from the TSS's key hierarchy. Frees up its UUID for another
	 * use.
	 */
	void clearkey()
	{
		if(bhelp)
		{
			cout << "Clear Key Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>  SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_key <hex[40]> | -authp_key <str>  Key auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36] | -uuid <hex[36]>            Key UUID for storage and retrieval" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  No output." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_key_value = 0;
		unsigned long	auth_key_size = 0;
		bool			auth_key_sha1 = false;
		unsigned char	*uuid_key_value = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_KEY);
			setupUUID(uuid_key_value);

			libhis_clearkey temp;

			temp.clearkey(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, uuid_key_value);
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_value != 0 && auth_srk_sha1) delete [] auth_srk_value;
			if(auth_key_value != 0 && auth_key_sha1) delete [] auth_key_value;
			throw e;
		}

		if(auth_srk_value != 0 && auth_srk_sha1) delete [] auth_srk_value;
		if(auth_key_value != 0 && auth_key_sha1) delete [] auth_key_value;
		return;
	}

	/*
	 * Get PCR
	 * Queries the TPM for PCR values. Similar to a Quote without the signature
	 * or identity key. Does not give the same assurance value as a Quote, but
	 * good for a quick glipse of the values. Do NOT use this where a Quote is
	 * appropriate. Getting PCRs and then signing them is not as strong as a
	 * Quote.
	 */
	void getpcr()
	{
		if(bhelp)
		{
			cout << "Get PCR Mode" << endl
				 << "  Reads in PCR values and displays them." << endl
				 << endl
				 << "Input:" << endl
				 << "  -p <hex[6]> | -pcrs <hex[6]>             PCR selection mask low to high" << endl
				 << "    NOTE: 7 6 5 4 3 2 1 0 15 14 13 12 11 10 9 8 23 22 21 20 19 18 17 16" << endl
				 << endl
				 << "Output:" << endl
				 << "  Hex[n] list of PCRS concatenated on one line. Use -r to delimit PCRs with newlines." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*mask = 0;
		unsigned char	*output_pcrs_value = 0;
		unsigned long	output_pcrs_size = 0;

		try
		{
			setupMask(mask);

			libhis_getpcr temp;

			temp.getpcr(mask, output_pcrs_value, output_pcrs_size);

			for(unsigned long i = 0; i < output_pcrs_size; i++)
			{
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_pcrs_value[i];
				if(breadable && (i + 1) % 20 == 0) cout << endl;
			}
		}
		catch(libhis_exception &e)
		{
			if(output_pcrs_value != 0) delete [] output_pcrs_value;
			throw e;
		}

		if(output_pcrs_value != 0) delete [] output_pcrs_value;
		return;
	}

	/*
	 * Extend PCR
	 * Adds a SHA1 hash to a PCR's measurement log and extends the current
	 * register value.
	 */
	void extendpcr()
	{
		if(bhelp)
		{
			cout << "Extend PCR Mode" << endl
				 << "  Extend any PCR any time." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
				 << "  -i <int> | -index <int>                  Index of PCR to be extended" << endl
				 << "    NOTE: 0 to 23 index number selection. Mask positions are:" << endl
				 << "          7 6 5 4 3 2 1 0 15 14 13 12 11 10 9 8 23 22 21 20 19 18 17 16" << endl
				 << "  -p <hex[40]> | -payload <hex[40]>        SHA1 hash to extend into PCR" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  Hex[40] new PCR value." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool			auth_tpm_sha1 = false;
		unsigned long	index = -1;
		unsigned char	*hash = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);

			//find the index
			for(int i = 0; i < argc; i++)
			{
				if((strcasecmp(argv[i], "-i") == 0 || strcasecmp(argv[i], "-index") == 0) && (i + 1) < argc)
				{
					index = atoi(argv[i + 1]);
					break;
				}
			}
			if(index < 0 || index > 23) throw libhis_exception("Index number argument missing", 410);

			//find the hash
			for(int i = 0; i < argc; i++)
			{
				if((strcasecmp(argv[i], "-p") == 0 || strcasecmp(argv[i], "-payload") == 0) && (i + 1) < argc)
				{
					hash = hexToBin(argv[i + 1]);
					break;
				}
			}
			if(hash == 0) throw libhis_exception("Hash argument missing", 410);

			libhis_extendpcr temp;

			temp.extendpcr(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, index, hash, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
			if(hash != 0) delete [] hash;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
		if(hash != 0) delete [] hash;
		if(output_value != 0) delete [] output_value;
		return;
	}

	/*
	 * Clear PCR
	 * Attempts to clear a PCR value. Normally PCRs between 0 and 15 cannot be
	 * cleared due to locality issues. This function does not support locality
	 * control as it was unclear just how to do that. Probably a good idea to
	 * implement locality support in future versions of this function. FIXME
	 */
	void clearpcr()
	{
		if(bhelp)
		{
			cout << "Clear PCR Mode" << endl
				 << "   Does not normally work except on the final 8 PCRs." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
				 << "  -p <hex[6]> | -pcrs <hex[6]>             PCR selection mask low to high" << endl
				 << "    NOTE: 7 6 5 4 3 2 1 0 15 14 13 12 11 10 9 8 23 22 21 20 19 18 17 16" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  No output." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool			auth_tpm_sha1 = false;
		unsigned char	*mask = 0;

		try
		{
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
			setupMask(mask);

			libhis_clearpcr temp;
			temp.clearpcr(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, mask);
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
			throw e;
		}

		if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
		return;
	}

	/*
	 * Set NVDATA
	 * Writes a block of data to the TPM's non-volatile memory (NVRAM). Auth
	 * data is required to enforce parity between Windows and Linux.
	 */
	void setnvdata()
	{
		if(bhelp)
		{
			cout << "Set NVData Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
				 << "  -i <str> | -index <str>                  EK, CC, PC, or PCC sets index" << endl
				 << "  -p <hex> | -payload <hex>                Data to be written" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  No output." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool			auth_tpm_sha1 = false;
		unsigned long	index = -1;
		unsigned char	*payload_value = 0;
		unsigned long	payload_size = 0;

		try
		{
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
			setupNVIndex(index);

			//find NVData payload
			for(int i = 0; i < argc; i++)
			{
				if(((strcasecmp(argv[i], "-p") == 0 || strcasecmp(argv[i], "-payload") == 0)) && i + 1 < argc)
				{
					payload_value = hexToBin(argv[i + 1]);
					payload_size = strlen(argv[i + 1]) / 2;
				}
			}
			if(payload_value == 0) throw libhis_exception("NVData payload argument", 410);

			libhis_setnvdata temp;
			temp.setnvdata(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, index, payload_value, payload_size);
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
			if(payload_value != 0) delete [] payload_value;
			throw e;
		}

		if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
		if(payload_value != 0) delete [] payload_value;
		return;
	}

	/*
	 * Get NVDATA
	 * Pulls data from a block of TPM non-volatile memory (NVRAM). Non-
	 * destructive.
	 */
	void getnvdata()
	{
		if(bhelp)
		{
			cout << "Get NVData Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
				 << "  -i <str> | -index <str>                  EK, CC, PC, or PCC sets index" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  Hex[n] NVRAM stored data blob." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool			auth_tpm_sha1 = false;
		unsigned long	index = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
			setupNVIndex(index);

			libhis_getnvdata temp;
			temp.getnvdata(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, index, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
		if(output_value != 0) delete [] output_value;
		return;
	}

	/*
	 * Clear NVDATA
	 * Invalidates a block of NVDATA address space. Some TPMs will immediately
	 * zero this space, but not all will. Recommended that random garbage data
	 * be written to the address space and then cleared.
	 */
	void clearnvdata()
	{
		if(bhelp)
		{
			cout << "Clear NVData Mode" << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_tpm <hex[40]> | -authp_tpm <str>  TPM owner auth in SHA1 or Plain mode" << endl
				 << "  -i <str> | -index <str>                  EK, CC, PC, or PCC sets index" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  No output." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool			auth_tpm_sha1 = false;
		unsigned long	index = 0;

		try
		{
			setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
			setupNVIndex(index);

			libhis_clearnvdata temp;
			temp.clearnvdata(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, index);
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
			throw e;
		}

		if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
		return;
	}

	/*
	 * Sign
	 * Signs data using a TPM-based signature key.
	 */
	void sign()
	{
		if(bhelp)
		{
			cout << "Sign Data Mode" << endl
				 << "  Sign a hash using a TPM signature key." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>   SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_sign <hex[40]> | -authp_sign <str> Signing key auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36] | -uuid <hex[36]>             Key UUID for storage and retrieval" << endl
				 << "  -l <int> | -length <int>                  Key length 512, 1024, 2048, 4096, 8192" << endl
				 << "  -s <str> | -scheme <str>                  Signing scheme" << endl
				 << "    Signing key only: SHA1 or DER" << endl
				 << "  -p <hex[40]> | -payload <hex[40]>         Hash to be signed" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  Hex[n] signature value." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_key_value = 0;
		unsigned long	auth_key_size = 0;
		bool			auth_key_sha1 = false;
		unsigned char	*uuid_key_value = 0;
		unsigned long	key_length = 0;
		unsigned long	key_scheme = 0;
		unsigned char	*hash = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_SIGN);
			setupUUID(uuid_key_value);
			setupLength(key_length, KEY_SIGN);
			setupScheme(key_scheme, KEY_SIGN);

			//find the encrypted data blob
			for(int i = 0; i < argc; i++)
			{
				if((strcasecmp(argv[i], "-p") == 0 || strcasecmp(argv[i], "-payload") == 0) && (i + 1) < argc && strlen(argv[i + 1]) == 40)
				{
					hash = hexToBin(argv[i + 1]);
					break;
				}
			}
			if(hash == 0) throw libhis_exception("Hash argument", 410);

			libhis_sign temp;
			temp.initsign(key_length, key_scheme);

			temp.sign(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, uuid_key_value, hash, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_key_sha1 && auth_key_value != 0) delete [] auth_key_value;
			if(hash != 0) delete [] hash;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_key_sha1 && auth_key_value != 0) delete [] auth_key_value;
		if(hash != 0) delete [] hash;
		if(output_value != 0) delete [] output_value;
		return;
	}

	/*
	 * Verify Signature
	 * Verifies a signature against a blob of data and provided key.
	 */
	void verifysignature()
	{
		if(bhelp)
		{
			cout << "Verify Signature Mode" << endl
				 << "  Verify a signature using a TPM signing key." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>   SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_sign <hex[40]> | -authp_sign <str> Signing key auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36] | -uuid <hex[36]>             Key UUID for storage and retrieval" << endl
				 << "  -l <int> | -length <int>                  Key length 512, 1024, 2048, 4096, 8192" << endl
				 << "  -s <str> | -scheme <str>                  SHA1 or DER signing scheme" << endl
				 << "  -o <hex[40]> | -original <hex[40]>        Original hash that was signed" << endl
				 << "  -p <hex> | -payload <hex>                 Signature to be verified" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  No output. Check the return value. 0 means success. Non-zero means fail." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_key_value = 0;
		unsigned long	auth_key_size = 0;
		bool			auth_key_sha1 = false;
		unsigned char	*uuid_key_value = 0;
		unsigned long	key_length = 0;
		unsigned long	key_scheme = 0;
		unsigned char	*hash = 0;
		unsigned char	*signature_value = 0;
		unsigned long	signature_size = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_SIGN);
			setupUUID(uuid_key_value);
			setupLength(key_length, KEY_SIGN);
			setupScheme(key_scheme, KEY_SIGN);

			//find the encrypted data blob
			for(int i = 0; i < argc; i++)
			{
				if((strcasecmp(argv[i], "-o") == 0 || strcasecmp(argv[i], "-original") == 0) && (i + 1) < argc && strlen(argv[i + 1]) == 40)
				{
					hash = hexToBin(argv[i + 1]);
					break;
				}
			}
			if(hash == 0) throw libhis_exception("Hash argument missing", 410);

			//find the encrypted data blob
			for(int i = 0; i < argc; i++)
			{
				if((strcasecmp(argv[i], "-p") == 0 || strcasecmp(argv[i], "-payload") == 0) && (i + 1) < argc)
				{
					signature_value = hexToBin(argv[i + 1]);
					signature_size = strlen(argv[i + 1]) / 2;
					break;
				}
			}
			if(signature_size == 0) throw libhis_exception("Signature argument", 411);

			libhis_verifysignature temp;
			temp.initsign(key_length, key_scheme);

			temp.verifysignature(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, uuid_key_value, hash, signature_value, signature_size);
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_key_sha1 && auth_key_value != 0) delete [] auth_key_value;
			if(hash != 0) delete [] hash;
			if(signature_value != 0) delete [] signature_value;
			throw e;
		}

		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_key_sha1 && auth_key_value != 0) delete [] auth_key_value;
		if(hash != 0) delete [] hash;
		if(signature_value != 0) delete [] signature_value;
		return;
	}

	/*
	 * Bind
	 * Uses a binding key protected by the TPM to encrypt data. Protects
	 * encrypted data with an authorization value.
	 */
	void bind()
	{
		if(bhelp)
		{
			cout << "Bind Data Mode" << endl
				 << "  Uses a TPM binding key to encrypt data." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>   SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_bind <hex[40]> | -authp_bind <str> Binding key auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36] | -uuid <hex[36]>             Key UUID for storage and retrieval" << endl
				 << "  -l <int> | -length <int>                  Key length 512, 1024, 2048, 4096, 8192" << endl
				 << "  -s <str> | -scheme <str>                  Binding scheme" << endl
				 << "    Binding key only: PKCS, SOAP, CNT, OFB, or PAD" << endl
				 << "  -p <hex> | -payload <hex>                 Payload data to be bouund" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  Hex[n] encrypted datablob." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_key_value = 0;
		unsigned long	auth_key_size = 0;
		bool			auth_key_sha1 = false;
		unsigned char	*auth_enc_value = 0;
		unsigned long	auth_enc_size = 0;
		bool			auth_enc_sha1 = false;
		unsigned char	*uuid_key_value = 0;
		unsigned long	key_length = 0;
		unsigned long	key_scheme = 0;
		unsigned char	*payload_value = 0;
		unsigned long	payload_size = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_BIND);
			setupAuth(auth_enc_value, auth_enc_size, auth_enc_sha1, AUTH_ENC);
			setupUUID(uuid_key_value);
			setupLength(key_length, KEY_BIND);
			setupScheme(key_scheme, KEY_BIND);

			//find the encrypted data blob
			for(int i = 0; i < argc; i++)
			{
				if((strcasecmp(argv[i], "-p") == 0 || strcasecmp(argv[i], "-payload") == 0) && (i + 1) < argc)
				{
					payload_value = hexToBin(argv[i + 1]);
					payload_size = strlen(argv[i + 1]) / 2;
					break;
				}
			}
			if(payload_size == 0) throw libhis_exception("Payload argument", 410);

			libhis_bind temp;
			temp.initbind(key_length, key_scheme);

			temp.bind(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, auth_enc_value, auth_enc_size, auth_enc_sha1, uuid_key_value, payload_value, payload_size, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_key_sha1 && auth_key_value != 0) delete [] auth_key_value;
			if(auth_enc_sha1 && auth_enc_value != 0) delete [] auth_enc_value;
			if(payload_value != 0) delete [] payload_value;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_key_sha1 && auth_key_value != 0) delete [] auth_key_value;
		if(auth_enc_sha1 && auth_enc_value != 0) delete [] auth_enc_value;
		if(payload_value != 0) delete [] payload_value;
		if(output_value != 0) delete [] output_value;
		return;
	}

	/*
	 * Unbind
	 * This function will decrypt data encrypted by a TPM-based binding key.
	 * TCG has their own format for encrypted data blobs controlled by binding
	 * keys.
	 */
	void unbind()
	{
		if(bhelp)
		{
			cout << "Unbind Data Mode" << endl
				 << "  Decrypt a data blob that was encrypted with a TPM binding key." << endl
				 << endl
				 << "Input:" << endl
				 << "  -auths_srk <hex[40]> | -authp_srk <str>   SRK auth in SHA1 or Plain mode" << endl
				 << "  -auths_bind <hex[40]> | -authp_bind <str> Binding key auth in SHA1 or Plain mode" << endl
				 << "  -u <hex[36] | -uuid <hex[36]>             Key UUID for storage and retrieval" << endl
				 << "  -l <int> | -length <int>                  Key length 512, 1024, 2048, 4096, 8192" << endl
				 << "  -s <str> | -scheme <str>                  Binding scheme" << endl
				 << "    Binding key only: PKCS, SOAP, CNT, OFB, or PAD" << endl
				 << "  -p <hex> | -payload <hex>                 Payload data to be unbound" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                              Set missing auth values to zero hash" << endl
				 << endl
				 << "Output:" << endl
				 << "  Hex[n] decrypted datablob." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_srk_value = 0;
		unsigned long	auth_srk_size = 0;
		bool			auth_srk_sha1 = false;
		unsigned char	*auth_key_value = 0;
		unsigned long	auth_key_size = 0;
		bool			auth_key_sha1 = false;
		unsigned char	*auth_enc_value = 0;
		unsigned long	auth_enc_size = 0;
		bool			auth_enc_sha1 = false;
		unsigned char	*uuid_key_value = 0;
		unsigned long	key_length = 0;
		unsigned long	key_scheme = 0;
		unsigned char	*payload_value = 0;
		unsigned long	payload_size = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupAuth(auth_srk_value, auth_srk_size, auth_srk_sha1, AUTH_SRK);
			setupAuth(auth_key_value, auth_key_size, auth_key_sha1, AUTH_BIND);
			setupAuth(auth_enc_value, auth_enc_size, auth_enc_sha1, AUTH_ENC);
			setupUUID(uuid_key_value);
			setupLength(key_length, KEY_BIND);
			setupScheme(key_scheme, KEY_BIND);

			//find the encrypted data blob
			for(int i = 0; i < argc; i++)
			{
				if((strcasecmp(argv[i], "-p") == 0 || strcasecmp(argv[i], "-payload") == 0) && (i + 1) < argc)
				{
					payload_value = hexToBin(argv[i + 1]);
					payload_size = strlen(argv[i + 1]) / 2;
					break;
				}
			}
			if(payload_size == 0) throw libhis_exception("Payload argument", 410);

			libhis_unbind temp;
			temp.initbind(key_length, key_scheme);

			temp.unbind(auth_srk_value, auth_srk_size, auth_srk_sha1, auth_key_value, auth_key_size, auth_key_sha1, auth_enc_value, auth_enc_size, auth_enc_sha1, uuid_key_value, payload_value, payload_size, output_value, output_size);

			for(unsigned long i = 0; i < output_size; i++)
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
		}
		catch(libhis_exception &e)
		{
			if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
			if(auth_key_sha1 && auth_key_value != 0) delete [] auth_key_value;
			if(auth_enc_sha1 && auth_enc_value != 0) delete [] auth_enc_value;
			if(payload_value != 0) delete [] payload_value;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_srk_sha1 && auth_srk_value != 0) delete [] auth_srk_value;
		if(auth_key_sha1 && auth_key_value != 0) delete [] auth_key_value;
		if(auth_enc_sha1 && auth_enc_value != 0) delete [] auth_enc_value;
		if(payload_value != 0) delete [] payload_value;
		if(output_value != 0) delete [] output_value;
		return;
	}

	/*
	 * Get Public Key
	 * This function will acquire public keys for the EK and SRK. Ownership is
	 * required for getting both keys.
	 */
	void getpubkey()
	{
		if(bhelp)
		{
			cout << "Get Public Key Mode" << endl
				 << "Acquires the public key (modulus and public exponent) for the EK and SRK." << endl
				 << endl
				 << "Input Required For All Keys:" << endl
				 << "  -t <str> | -type <str>                  Key type EK or SRK" << endl
				 << "  -auths_owner <hex[40]> | -authp_owner   <str> Owner auth in SHA1 or Plain mode" << endl
				 << endl
				 << "Required Inputs for EK Only" << endl
				 << "  -auths_owner <hex[40]> | -authp_owner   <str> Owner auth in SHA1 or Plain mode" << endl
				 << "  -nonce <hex[40]> | -nonce_random        Nonce data as hex SHA1 hash" << endl
				 << endl
				 << "Optional:" << endl
				 << "  -z | -zeros                             Set missing auth values to zero hash" << endl
				 << "  -nr | -nonce_random                     TPM random byte generator nonce" << endl
				 << endl
				 << "Output:" << endl
				 << "  Hex[n] public key." << endl
				 << endl;
			throw libhis_exception("Help argument set", 400);
		}

		unsigned char	*auth_tpm_value = 0;
		unsigned long	auth_tpm_size = 0;
		bool			auth_tpm_sha1 = false;
		int				key_type = 0;
		unsigned char	*nonce = 0;
		unsigned char	*output_value = 0;
		unsigned long	output_size = 0;

		try
		{
			setupKeyType(key_type);

			libhis_getpubkey temp;

			if(key_type == KEY_EK)
			{
				setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);
				setupNonce(nonce);

				temp.getpubek(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, nonce, output_value, output_size);
			}
			else if(key_type == KEY_SRK)
			{
				setupAuth(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, AUTH_TPM);

				temp.getpubsrk(auth_tpm_value, auth_tpm_size, auth_tpm_sha1, output_value, output_size);
			}
			else
			{
				throw libhis_exception("Key type not valid for this function. Use EK or SRK.", 412);
			}

			for(unsigned long i = 0; i < output_size; i++)
			{
				cout << setbase(16) << setw(2) << setfill('0') << (int)output_value[i];
			}
			cout << endl;
		}
		catch(libhis_exception &e)
		{
			if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
			if(output_value != 0) delete [] output_value;
			throw e;
		}

		if(auth_tpm_value != 0 && auth_tpm_sha1) delete [] auth_tpm_value;
		if(output_value != 0) delete [] output_value;
		return;
	}

	/*
	 * Error Helper
	 * Converts numeric error codes into textual representations. Attempts to
	 * describe the meaning of error codes and provide clues as to the cause.
	 */
	void error_helper(int result)
	{
		switch(result)
		{
			case 1:
				cerr << "TPM_E_AUTHFAIL -- An authorization data value is invalid." << endl;
				break;
			case 2:
				cerr << "TPM_E_BADINDEX" << endl;
				break;
			case 3:
				cerr << "TPM_E_BAD_PARAMETER -- A provided parameter does not meet specification. Normally provided data is not in the correct TCG structure." << endl;
				break;
			case 4:
				cerr << "TPM_E_AUDITFAILURE" << endl;
				break;
			case 5:
				cerr << "TPM_E_CLEAR_DISABLED" << endl;
				break;
			case 6:
				cerr << "TPM_E_DEACTIVATED -- TPM is deactivated. Go to BIOS and reactivate. This could require power cycling." << endl;
				break;
			case 7:
				cerr << "TPM_E_DISABLED -- TPM has been disabled. Go to BIOS and enable TPM. Also remember to active it! This could require power cycling." << endl;
				break;
			case 8:
				cerr << "TPM_E_DISABLED_CMD -- Command disabled because it probably already ran or is no longer applicable. Very common when trying to take ownership when ownership already exists or when trying to get the public EK without owner auth after ownership is established." << endl;
				break;
			case 9:
				cerr << "TPM_E_FAIL" << endl;
				break;
			case 10:
				cerr << "TPM_E_BAD_ORDINAL -- TPM firmware does not support this command." << endl;
				break;
			case 11:
				cerr << "TPM_E_INSTALL_DISABLED" << endl;
				break;
			case 12:
				cerr << "TPM_E_INVALID_KEYHANDLE" << endl;
				break;
			case 13:
				cerr << "TPM_E_KEYNOTFOUND -- No key for this UUID." << endl;
				break;
			case 14:
				cerr << "TPM_E_INAPPROPRIATE_ENC -- Invalid encrypted data or implementation defect in TPM firmware. You may also have told this software to use an encryption scheme not supported this TPM." << endl;
				break;
			case 15:
				cerr << "TPM_E_MIGRATEFAIL" << endl;
				break;
			case 16:
				cerr << "TPM_E_INVALID_PCR_INFO" << endl;
				break;
			case 17:
				cerr << "TPM_E_NOSPACE" << endl;
				break;
			case 18:
				cerr << "TPM_E_NOSRK -- Enable TPM, activate TPM, and take ownership to create SRK. Your TPM is probably not set correctly in the BIOS or you accidentally cleared and disabled it." << endl;
				break;
			case 19:
				cerr << "TPM_E_NOTSEALED_BLOB" << endl;
				break;
			case 20:
				cerr << "TPM_E_OWNER_SET -- Ownership already established for this TPM. Not necessarily an error since you can share ownership with multiple sources." << endl;
				break;
			case 21:
				cerr << "TPM_E_RESOURCES" << endl;
				break;
			case 22:
				cerr << "TPM_E_SHORTRANDOM" << endl;
				break;
			case 23:
				cerr << "TPM_E_SIZE" << endl;
				break;
			case 24:
				cerr << "TPM_E_WRONGPCRVAL" << endl;
				break;
			case 25:
				cerr << "TPM_E_BAD_PARAM_SIZE " << endl;
				break;
			case 26:
				cerr << "TPM_E_SHA_THREAD" << endl;
				break;
			case 27:
				cerr << "TPM_E_SHA_ERROR" << endl;
				break;
			case 28:
				cerr << "TPM_E_FAILEDSELFTEST -- Bad state. Try disabling and then re-enabling TPM." << endl;
				break;
			case 29:
				cerr << "TPM_E_AUTH2FAIL" << endl;
				break;
			case 30:
				cerr << "TPM_E_BADTAG" << endl;
				break;
			case 31:
				cerr << "TPM_E_IOERROR -- Check TPM kernel module or driver. On Linux this is tpm_tis module. This error can also indicate an unsupported TPM function on older TPMs." << endl;
				break;
			case 32:
				cerr << "TPM_E_ENCRYPT_ERROR -- In the case of binding data it is possible this data may be too large. Break it apart. In the case of identity provisioning it is possible the EK and AIK certificates are not valid for the current keys. Make them again." << endl;
				break;
			case 33:
				cerr << "TPM_E_DECRYPT_ERROR -- In the case of binding data it is possible this data might not have been encrypted by a TPM. Use TCG structures next time. In the case of identity provisioning this error can indicate an invalid EK or AIK certificate." << endl;
				break;
			case 34:
				cerr << "TPM_E_INVALID_AUTHHANDLE" << endl;
				break;
			case 35:
				cerr << "TPM_E_NO_ENDORSEMENT -- Create an endorsement key and try again." << endl;
				break;
			case 36:
				cerr << "TPM_E_INVALID_KEYUSAGE -- This key UUID is not valid for this key command." << endl;
				break;
			case 37:
				cerr << "TPM_E_WRONG_ENTITYTYPE" << endl;
				break;
			case 38:
				cerr << "TPM_E_INVALID_POSTINIT" << endl;
				break;
			case 39:
				cerr << "TPM_E_INAPPROPRIATE_SIG" << endl;
				break;
			case 40:
				cerr << "TPM_E_BAD_KEY_PROPERTY" << endl;
				break;
			case 41:
				cerr << "TPM_E_BAD_MIGRATION" << endl;
				break;
			case 42:
				cerr << "TPM_E_BAD_SCHEME" << endl;
				break;
			case 43:
				cerr << "TPM_E_BAD_DATASIZE" << endl;
				break;
			case 44:
				cerr << "TPM_E_BAD_MODE" << endl;
				break;
			case 45:
				cerr << "TPM_E_BAD_PRESENCE" << endl;
				break;
			case 46:
				cerr << "TPM_E_BAD_VERSION" << endl;
				break;
			case 47:
				cerr << "TPM_E_NO_WRAP_TRANSPORT" << endl;
				break;
			case 48:
				cerr << "TPM_E_AUDITFAIL_UNSUCCESSFUL" << endl;
				break;
			case 49:
				cerr << "TPM_E_AUDITFAIL_SUCCESSFUL" << endl;
				break;
			case 50:
				cerr << "TPM_E_NOTRESETABLE" << endl;
				break;
			case 51:
				cerr << "TPM_E_NOTLOCAL" << endl;
				break;
			case 52:
				cerr << "TPM_E_BAD_TYPE" << endl;
				break;
			case 53:
				cerr << "TPM_E_INVALID_RESOURCE" << endl;
				break;
			case 54:
				cerr << "TPM_E_NOTFIPS" << endl;
				break;
			case 55:
				cerr << "TPM_E_INVALID_FAMILY" << endl;
				break;
			case 56:
				cerr << "TPM_E_NO_NV_PERMISSION -- NTrue does not require auth data on NVRAM access. Trousers does. This software _always_requires NVRAM auth data as a result. This error can happen when another program sets NVRAM data without auth." << endl;
				break;
			case 57:
				cerr << "TPM_E_REQUIRES_SIGN" << endl;
				break;
			case 58:
				cerr << "TPM_E_KEY_NOTSUPPORTED" << endl;
				break;
			case 59:
				cerr << "TPM_E_AUTH_CONFLICT" << endl;
				break;
			case 60:
				cerr << "TPM_E_AREA_LOCKED -- TXT will lock parts of NVRAM when enabled. You must disable TXT to unlock the NVRAM for writing of policies and other data. Then you may re-lock with TXT afterwards." << endl;
				break;
			case 61:
				cerr << "TPM_E_BAD_LOCALITY" << endl;
				break;
			case 62:
				cerr << "TPM_E_READ_ONLY" << endl;
				break;
			case 63:
				cerr << "TPM_E_PER_NOWRITE" << endl;
				break;
			case 64:
				cerr << "TPM_E_FAMILYCOUNT" << endl;
				break;
			case 65:
				cerr << "TPM_E_WRITE_LOCKED" << endl;
				break;
			case 66:
				cerr << "TPM_E_BAD_ATTRIBUTES" << endl;
				break;
			default:
				break;
		}

		return;
	}
};

#endif
