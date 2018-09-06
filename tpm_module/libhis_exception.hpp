#ifndef libhis_exception_hpp
#define libhis_exception_hpp


#ifdef WINDOWS
	#include <exception>
	using namespace std;

	class libhis_exception : public exception
	{
	public:
		libhis_exception(const char *message, int value) : exception(message)
		{
			result = value;
		}

		int	result;
	};
#endif

#ifdef LINUX
	#include <exception>
	#include <string>
	using namespace std;

	class libhis_exception
	{
	public:
		libhis_exception(string inmessage, int value)
		{
			message = inmessage;
			result = value;
		}

		~libhis_exception() {}

		string what()
		{
			return message;
		}

		int	result;
		string message;
	};
#endif

#endif
