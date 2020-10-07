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

    static std::string collectTcgLog();
};
#endif  // HIRS_PROVISIONERTPM2_INCLUDE_DEVICEINFOCOLLECTOR_H_
