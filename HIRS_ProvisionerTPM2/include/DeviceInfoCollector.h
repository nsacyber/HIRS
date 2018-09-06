/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#ifndef HIRS_PROVISIONERTPM2_INCLUDE_DEVICEINFOCOLLECTOR_H_
#define HIRS_PROVISIONERTPM2_INCLUDE_DEVICEINFOCOLLECTOR_H_

#include <Logger.h>
#include <ProvisionerTpm2.pb.h>

#include <utility>
#include <string>
#include <vector>

/**
 * Manages collection of device information for the client. Retrieves the OS,
 * network, hardware, firmware, and TPM info.
 */
class DeviceInfoCollector {
 private:
    DeviceInfoCollector() {}

    static const hirs::log::Logger LOGGER;

    static void addBaseboardInfoIfAvailable(hirs::pb::HardwareInfo* hwInfo);

    static void addBiosInfoIfAvailable(hirs::pb::HardwareInfo* hwInfo);

    static void addChassisInfoIfAvailable(hirs::pb::HardwareInfo* hwInfo);

    static void addHardDriveInfoIfAvailable(hirs::pb::HardwareInfo* hwInfo);

    static void addMemoryInfoIfAvailable(hirs::pb::HardwareInfo* hwInfo);

    static void addNicInfoIfAvailable(hirs::pb::HardwareInfo* hwInfo);

    static void addProcessorInfoIfAvailable(hirs::pb::HardwareInfo* hwInfo);

    static int getLshwDeviceCount(std::string className,
                                  std::string deviceType = "");

    static std::string getLshwDeviceField(int deviceNumber,
                                          std::string fieldName,
                                          std::string className,
                                          std::string deviceType);

    static std::vector<std::pair<std::string, std::string>> getNetworks();

    static hirs::pb::FirmwareInfo collectFirmwareInfo();

    static hirs::pb::HardwareInfo collectHardwareInfo();

    static std::string collectHostname();

    static hirs::pb::NetworkInfo collectNetworkInfo();

    static hirs::pb::OsInfo collectOsInfo();

 public:
    /**
     * Collect all device info from the system and return it in a filled out
     * DeviceInfo object.
     */
    static hirs::pb::DeviceInfo collectDeviceInfo();
};
#endif  // HIRS_PROVISIONERTPM2_INCLUDE_DEVICEINFOCOLLECTOR_H_
