/**
 * Copyright (C) 2017-2018, U.S. Government
 */
#include <DeviceInfoCollector.h>
#include <Utils.h>

#include <ifaddrs.h>
#include <netdb.h>
#include <sys/utsname.h>
#include <unistd.h>

#include <climits>
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <utility>
#include <vector>
#include <Process.h>
#include <HirsRuntimeException.h>

# define NOT_SPECIFIED "Not Specified"

using hirs::exception::HirsRuntimeException;
using hirs::file_utils::fileToString;
using hirs::file_utils::getFileAsOneLineOrEmptyString;
using hirs::log::Logger;
using hirs::string_utils::trimNewLines;
using std::ifstream;
using std::pair;
using std::string;
using std::stringstream;
using std::vector;

const Logger DeviceInfoCollector::LOGGER = Logger::getDefaultLogger();

hirs::pb::DeviceInfo DeviceInfoCollector::collectDeviceInfo() {
    hirs::pb::DeviceInfo dv;

    dv.mutable_fw()->CopyFrom(collectFirmwareInfo());
    dv.mutable_hw()->CopyFrom(collectHardwareInfo());
    dv.mutable_nw()->CopyFrom(collectNetworkInfo());
    dv.mutable_os()->CopyFrom(collectOsInfo());
    return dv;
}

hirs::pb::FirmwareInfo DeviceInfoCollector::collectFirmwareInfo() {
    hirs::pb::FirmwareInfo fw;

    fw.set_biosvendor(trimNewLines(
            fileToString("/sys/class/dmi/id/bios_vendor", NOT_SPECIFIED)));
    fw.set_biosversion(
            getFileAsOneLineOrEmptyString("/sys/class/dmi/id/bios_version"));
    fw.set_biosreleasedate(
            getFileAsOneLineOrEmptyString("/sys/class/dmi/id/bios_date"));

    LOGGER.info("Bios Vendor: " + fw.biosvendor());
    LOGGER.info("Bios Version: " + fw.biosversion());
    LOGGER.info("Bios Date: " + fw.biosreleasedate());

    return fw;
}

hirs::pb::HardwareInfo DeviceInfoCollector::collectHardwareInfo() {
    hirs::pb::HardwareInfo hw;

    // Need root to read some of these, will be "" otherwise
    hw.set_manufacturer(trimNewLines(
            fileToString("/sys/class/dmi/id/sys_vendor", NOT_SPECIFIED)));
    hw.set_productname(trimNewLines(
            fileToString("/sys/class/dmi/id/product_name", NOT_SPECIFIED)));
    hw.set_productversion(
            getFileAsOneLineOrEmptyString("/sys/class/dmi/id/product_version"));
    hw.set_systemserialnumber(
            getFileAsOneLineOrEmptyString("/sys/class/dmi/id/product_serial"));

    LOGGER.info("System Manufacturer: " + hw.manufacturer());
    LOGGER.info("Product Name: " + hw.productname());
    LOGGER.info("Product Version: " + hw.productversion());
    LOGGER.info("System Serial Number: " + hw.systemserialnumber());

    addChassisInfoIfAvailable(&hw);
    google::protobuf::RepeatedPtrField<hirs::pb::ComponentInfo> chassisInfo
            = hw.chassisinfo();
    for (int i = 0; i < chassisInfo.size(); i++) {
        LOGGER.info("Chassis Manufacturer: "
                    + chassisInfo.Get(i).manufacturer());
        LOGGER.info("Chassis Model: " + chassisInfo.Get(i).model());
        LOGGER.info(
                "Chassis Serial Number: " + chassisInfo.Get(i).serialnumber());
        LOGGER.info("Chassis Version: " + chassisInfo.Get(i).revision());
    }

    addBaseboardInfoIfAvailable(&hw);
    google::protobuf::RepeatedPtrField<hirs::pb::ComponentInfo> baseboardInfo
            = hw.baseboardinfo();
    for (int i = 0; i < baseboardInfo.size(); i++) {
        LOGGER.info(
                "Baseboard Manufacturer: " + baseboardInfo.Get(i).manufacturer
                        ());
        LOGGER.info("Baseboard Model: " + baseboardInfo.Get(i).model());
        LOGGER.info("Baseboard Serial Number: " +
                    baseboardInfo.Get(i).serialnumber());
        LOGGER.info("Baseboard Version: " + baseboardInfo.Get(i).revision());
    }

    addProcessorInfoIfAvailable(&hw);
    google::protobuf::RepeatedPtrField<hirs::pb::ComponentInfo> processorInfo
            = hw.processorinfo();
    for (int i = 0; i < processorInfo.size(); i++) {
        LOGGER.info("Processor Manufacturer: "
                + processorInfo.Get(i).manufacturer());
        LOGGER.info("Processor Model: " + processorInfo.Get(i).model());
        LOGGER.info("Processor Serial Number: " +
                    processorInfo.Get(i).serialnumber());
        LOGGER.info("Processor Version: " + processorInfo.Get(i).revision());
    }

    addBiosInfoIfAvailable(&hw);
    google::protobuf::RepeatedPtrField<hirs::pb::ComponentInfo> biosInfo
            = hw.biosoruefiinfo();
    for (int i = 0; i < biosInfo.size(); i++) {
        LOGGER.info("BIOS Manufacturer: " + biosInfo.Get(i).manufacturer());
        LOGGER.info("BIOS Model: " + biosInfo.Get(i).model());
        LOGGER.info("BIOS Version: " + biosInfo.Get(i).revision());
    }

    addNicInfoIfAvailable(&hw);
    google::protobuf::RepeatedPtrField<hirs::pb::ComponentInfo> nicInfo
            = hw.nicinfo();
    for (int i = 0; i < nicInfo.size(); i++) {
        LOGGER.info("NIC Manufacturer: " + nicInfo.Get(i).manufacturer());
        LOGGER.info("NIC Model: " + nicInfo.Get(i).model());
        LOGGER.info("NIC Serial Number: " + nicInfo.Get(i).serialnumber());
        LOGGER.info("NIC Version: " + nicInfo.Get(i).revision());
    }

    addHardDriveInfoIfAvailable(&hw);
    google::protobuf::RepeatedPtrField<hirs::pb::ComponentInfo> hdInfo
            = hw.harddriveinfo();
    for (int i = 0; i < hdInfo.size(); i++) {
        LOGGER.info("Hard Drive " + std::to_string(i) + " Manufacturer: "
                    + hdInfo.Get(i).manufacturer());
        LOGGER.info("Hard Drive " + std::to_string(i) + " Model: "
                    + hdInfo.Get(i).model());
        LOGGER.info("Hard Drive " + std::to_string(i) + " Serial Number: "
                    + hdInfo.Get(i).serialnumber());
        LOGGER.info("Hard Drive " + std::to_string(i) + " Version: "
                    + hdInfo.Get(i).revision());
    }

    addMemoryInfoIfAvailable(&hw);
    google::protobuf::RepeatedPtrField<hirs::pb::ComponentInfo> memInfo
            = hw.memoryinfo();
    for (int i = 0; i < memInfo.size(); i++) {
        LOGGER.info("Memory Unit " + std::to_string(i) + " Manufacturer: "
                    + memInfo.Get(i).manufacturer());
        LOGGER.info("Memory Unit " + std::to_string(i) + " Model: "
                    + memInfo.Get(i).model());
        LOGGER.info("Memory Unit " + std::to_string(i) + " Serial Number: "
                    + memInfo.Get(i).serialnumber());
    }

    return hw;
}

hirs::pb::NetworkInfo DeviceInfoCollector::collectNetworkInfo() {
    hirs::pb::NetworkInfo nw;
    nw.set_hostname(collectHostname());

    // Get IP and MAC address, will be empty if no non-loopback interface
    vector<pair<string, string>> x = getNetworks();

    // Choose first non-loopback address/MAC pair
    for (auto const & addressPair : x) {
        string ip = addressPair.first;
        string mac = addressPair.second;
        if (ip != "127.0.0.1" && ip != "::1") {
            nw.set_ipaddress(ip);
            nw.set_macaddress(mac);
            break;
        }
    }

    LOGGER.info("Network Info IP: " + nw.ipaddress());
    LOGGER.info("Network Info MAC: " + nw.macaddress());
    LOGGER.info("Network Info Hostname: " + nw.hostname());

    return nw;
}

vector<pair<string, string>> DeviceInfoCollector::getNetworks() {
    struct ifaddrs* ifaddr, * ifa;
    int ret;
    char host[NI_MAXHOST];
    string hostStr;
    vector<pair<string, string>> interfaces;
    int family;

    // if command fails, return empty list
    if (getifaddrs(&ifaddr) == -1) {
        LOGGER.error("getifaddrs failed");
        return interfaces;
    }

    /*
    * Walk through linked list, maintaining head pointer so we can free
    * list later
    */
    for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next) {
        /*
         * Check for ifa_addr == NULL here.  This is true on at least
         * some systems when a tun0/P-t-P interface is up (aka, a VPN).
         * My reading of the man page indicates that should not be the
         * case, but ubuntu 12.04 returns NULL for all ifa_* entries.
         * And the example checks for NULL here anyway, so do so and
         * skip the interface if necessary.
         */
        if (!ifa->ifa_addr)
            continue;

        family = ifa->ifa_addr->sa_family;

        if (family != AF_INET && family != AF_INET6)
            continue;

        if (family == AF_INET || family == AF_INET6) {
            memset(host, 0, NI_MAXHOST);

            ret = getnameinfo(ifa->ifa_addr,
                              (ifa->ifa_addr->sa_family == AF_INET) ?
                              sizeof(struct sockaddr_in) :
                              sizeof(struct sockaddr_in6),
                              host, NI_MAXHOST, NULL, 0, NI_NUMERICHOST);
            hostStr = string(host);

            if (ret) {
                LOGGER.error("getnameinfo failed");
                continue;
            }

            // get MAC address.. doesn't seem to be a portable/posix way.
            char scratch[256];
            string macaddr;
            snprintf(scratch, sizeof(scratch), "/sys/class/net/%s/address",
                     ifa->ifa_name);

            macaddr = fileToString(scratch);

            if (macaddr.empty()) {
                LOGGER.error("Error reading MAC address");
                continue;
            }
            macaddr.erase(remove(macaddr.begin(), macaddr.end(), '\n'),
                          macaddr.end());

            LOGGER.debug("Adding address pair IP: " + string(host) +
                         " MAC: " + macaddr);

            interfaces.push_back(make_pair(hostStr, macaddr));
        }
    }

    freeifaddrs(ifaddr);
    return interfaces;
}

string DeviceInfoCollector::collectHostname() {
    char host[HOST_NAME_MAX];
    int ret = gethostname(host, HOST_NAME_MAX);

    if (ret) {
        LOGGER.error("gethostname failed");
        return "";
    }

    string hostStr(host);

    LOGGER.debug("Found hostname: " + hostStr);

    return hostStr;
}

hirs::pb::OsInfo DeviceInfoCollector::collectOsInfo() {
    hirs::pb::OsInfo info;

    ifstream releaseFile;
    string line;
    releaseFile.open("/etc/os-release");
    if (releaseFile.is_open()) {
        while (getline(releaseFile, line)) {
            stringstream ss(line);
            string item;
            vector<string> tokens;
            char* delim = const_cast<char*>("=");
            while (getline(ss, item, *delim)) {
                tokens.push_back(item);
            }
            if (tokens.size() > 0 && tokens.at(0) == "ID") {
                info.set_distribution(tokens.at(1));
            } else if (tokens.size() > 0 && tokens.at(0) == "VERSION_ID") {
                info.set_distributionrelease(tokens.at(1));
            }
        }
        releaseFile.close();
    } else {
        LOGGER.error("/etc/os-release read failed");
    }

    struct utsname uts;
    int ret = uname(&uts);
    if (ret) {
        LOGGER.error("Uname read failed");
    } else {
        info.set_osname(uts.sysname);
        info.set_osversion(uts.version);
        info.set_distributionrelease(uts.release);
        info.set_osarch(uts.machine);
    }

    LOGGER.info("OS Name: " + info.osname());
    LOGGER.info("OS Version: " + info.osversion());
    LOGGER.info("Architecture: " + info.osarch());
    LOGGER.info("Distribution: " + info.distribution());
    LOGGER.info("Distribution Release: " + info.distributionrelease());

    return info;
}

void DeviceInfoCollector::addChassisInfoIfAvailable(
        hirs::pb::HardwareInfo* hwInfo) {
    int numChassis = atoi(RUN_PROCESS_OR_THROW("dmidecode", "-t 3 "
            "| grep 'Manufacturer:' | wc -l").c_str());

    for (int chassisNumber = 1; chassisNumber <= numChassis; chassisNumber++) {
        // Manufacturer and Model are required if Chassis is to
        // be included at all.
        try {
            hirs::pb::ComponentInfo chassisInfo;
            chassisInfo.set_manufacturer(
                    RUN_PROCESS_OR_THROW("dmidecode", "-t 3 | "
                            "grep 'Manufacturer:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(chassisNumber) + "p"));
            chassisInfo.set_model(
                    RUN_PROCESS_OR_THROW("dmidecode", "-t 3 | "
                            "grep 'Type:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(chassisNumber) + "p"));

            // Serial number is optional
            try {
                chassisInfo.set_serialnumber(
                        RUN_PROCESS_OR_THROW("dmidecode", "-t 3 | "
                            "grep 'Serial Number:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(chassisNumber) + "p"));
            } catch (const HirsRuntimeException& e) {
                LOGGER.warn(e.what());
            }

            // Chassis version is optional
            try {
                chassisInfo.set_revision(
                        RUN_PROCESS_OR_THROW("dmidecode", "-t 3 | "
                            "grep 'Version:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(chassisNumber) + "p"));
            } catch (const HirsRuntimeException& e) {
                LOGGER.warn(e.what());
            }
            hwInfo->add_chassisinfo();
            hwInfo->mutable_chassisinfo(chassisNumber - 1)
                    ->CopyFrom(chassisInfo);
        } catch (const HirsRuntimeException& e) {
            LOGGER.warn(e.what());
        }
    }
}

void DeviceInfoCollector::addBaseboardInfoIfAvailable(
        hirs::pb::HardwareInfo* hwInfo) {
    int numBaseboards = atoi(RUN_PROCESS_OR_THROW("dmidecode", "-t 2 "
            "| grep 'Manufacturer:' | wc -l").c_str());

    for (int baseboardNumber = 1; baseboardNumber <= numBaseboards;
         baseboardNumber++) {
        hirs::pb::ComponentInfo baseboardInfo;

        // Manufacturer and Model required if Baseboard is to be
        // included at all.
        try {
            baseboardInfo.set_manufacturer(
                    RUN_PROCESS_OR_THROW("dmidecode", "-t 2 | "
                            "grep 'Manufacturer:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(baseboardNumber) + "p"));
            baseboardInfo.set_model(
                    RUN_PROCESS_OR_THROW("dmidecode", "-t 2 | "
                            "grep 'Product Name:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(baseboardNumber) + "p"));

            // Serial number is optional
            try {
                baseboardInfo.set_serialnumber(
                        RUN_PROCESS_OR_THROW("dmidecode", "-t 2 | "
                            "grep 'Serial Number:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(baseboardNumber) + "p"));
            } catch (const HirsRuntimeException& e) {
                LOGGER.warn(e.what());
            }

            // Baseboard version is optional
            try {
                baseboardInfo.set_revision(
                        RUN_PROCESS_OR_THROW("dmidecode", "-t 2 | "
                                "grep 'Version:' "
                                "| sed -e 's/[^:]*:[ ]*//' -n -e "
                                + std::to_string(baseboardNumber) + "p"));
            } catch (const HirsRuntimeException& e) {
                LOGGER.warn(e.what());
            }
            hwInfo->add_baseboardinfo();
            hwInfo->mutable_baseboardinfo(baseboardNumber - 1)
                    ->CopyFrom(baseboardInfo);
        } catch (const HirsRuntimeException& e) {
            LOGGER.warn(e.what());
        }
    }
}

void DeviceInfoCollector::addProcessorInfoIfAvailable(
        hirs::pb::HardwareInfo* hwInfo) {
    int numProcessors = atoi(RUN_PROCESS_OR_THROW("dmidecode", "-t 4 "
            "| grep 'Manufacturer:' | wc -l").c_str());

    for (int processorNumber = 1; processorNumber <= numProcessors;
         processorNumber++) {
        hirs::pb::ComponentInfo processorInfo;

        // Manufacturer and Model required if Processor is to be
        // included at all.
        try {
            processorInfo.set_manufacturer(
                    RUN_PROCESS_OR_THROW("dmidecode", "-t 4 | "
                            "grep 'Manufacturer:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(processorNumber) + "p"));
            processorInfo.set_model(
                    RUN_PROCESS_OR_THROW("dmidecode", "-t 4 | "
                            "grep 'Family:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(processorNumber) + "p"));

            // Serial number is optional
            try {
                processorInfo.set_serialnumber(
                        RUN_PROCESS_OR_THROW("dmidecode", "-t 4 | "
                            "grep 'Serial Number:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(processorNumber) + "p"));
            } catch (const HirsRuntimeException& e) {
                LOGGER.warn(e.what());
            }

            // Processor version is optional
            try {
                processorInfo.set_revision(
                        RUN_PROCESS_OR_THROW("dmidecode", "-t 4 | "
                                "grep 'Version:' "
                                "| sed -e 's/[^:]*:[ ]*//' -n -e "
                                + std::to_string(processorNumber) + "p"));
            } catch (const HirsRuntimeException& e) {
                LOGGER.warn(e.what());
            }
            hwInfo->add_processorinfo();
            hwInfo->mutable_processorinfo(processorNumber - 1)
                    ->CopyFrom(processorInfo);
        } catch (const HirsRuntimeException& e) {
            LOGGER.warn(e.what());
        }
    }
}

void DeviceInfoCollector::addBiosInfoIfAvailable(
        hirs::pb::HardwareInfo* hwInfo) {
    int numBios = atoi(RUN_PROCESS_OR_THROW("dmidecode", "-t bios "
            "| grep Vendor | wc -l").c_str());

    for (int biosNumber = 1; biosNumber <= numBios; biosNumber++) {
        hirs::pb::ComponentInfo biosOrUefiInfo;

        // Manufacturer and Model are required if BIOS is to be included at all.
        try {
            biosOrUefiInfo.set_manufacturer(
                    RUN_PROCESS_OR_THROW("dmidecode",
                            "-t bios| grep 'Vendor:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(biosNumber) + "p"));

            // TODO(apl.dev4): don't know how to know BIOS vs. UEFI
            biosOrUefiInfo.set_model("BIOS");

            // BIOS version is optional
            try {
                biosOrUefiInfo.set_revision(
                        RUN_PROCESS_OR_THROW("dmidecode", "-t bios"
                            "| grep 'Version:' "
                            "| sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(biosNumber) + "p"));
            } catch (const HirsRuntimeException& e) {
                LOGGER.warn(e.what());
            }

            hwInfo->add_biosoruefiinfo();
            hwInfo->mutable_biosoruefiinfo(biosNumber - 1)
                    ->CopyFrom(biosOrUefiInfo);
        } catch (const HirsRuntimeException& e) {
            LOGGER.warn(e.what());
        }
    }
}

void DeviceInfoCollector::addNicInfoIfAvailable(
        hirs::pb::HardwareInfo* hwInfo) {
    int numNICs = atoi(RUN_PROCESS_OR_THROW("lshw", "-class network "
            "| grep vendor | wc -l").c_str());

    for (int nicNumber = 1; nicNumber <= numNICs; nicNumber++) {
        hirs::pb::ComponentInfo nicInfo;

        // Manufacturer and Model are required if NIC info is to be
        // included at all.
        try {
            nicInfo.set_manufacturer(
                    RUN_PROCESS_OR_THROW("lshw", "-class network "
                            "| grep vendor | sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(nicNumber) + "p"));
            nicInfo.set_model(
                    RUN_PROCESS_OR_THROW("lshw", "-class network "
                            "| grep product | sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(nicNumber) + "p"));

            // Serial number is optional
            try {
                nicInfo.set_serialnumber(
                    RUN_PROCESS_OR_THROW("lshw", "-class network "
                            "| grep 'serial:' | sed -e 's/[^:]*: //' -n -e "
                            + std::to_string(nicNumber) + "p"));
            } catch (const HirsRuntimeException& e) {
                LOGGER.warn(e.what());
            }

            // NIC version is optional
            try {
                nicInfo.set_revision(
                        RUN_PROCESS_OR_THROW("lshw", "-class network "
                            "| grep version: | sed -e 's/[^:]*:[ ]*//' -n -e "
                            + std::to_string(nicNumber) + "p"));
            } catch (const HirsRuntimeException& e) {
                LOGGER.warn(e.what());
            }

            hwInfo->add_nicinfo();
            hwInfo->mutable_nicinfo(nicNumber - 1)->CopyFrom(nicInfo);
        } catch (const HirsRuntimeException& e) {
            LOGGER.warn(e.what());
        }
    }
}

int DeviceInfoCollector::getLshwDeviceCount(string className,
                                            string deviceType) {
    stringstream argStream;
    argStream << "-class " << className
              << " | awk"
              << " -vdev_type=" << deviceType
              << " \'match($0, \"^  \\\\*-\" dev_type) {++count}"
              << " END {print count}\'";
    return atoi(RUN_PROCESS_OR_THROW("lshw", argStream.str()).c_str());
}

string DeviceInfoCollector::getLshwDeviceField(int deviceNumber,
                                               string fieldName,
                                               string className,
                                               string deviceType) {
    stringstream argStream;
    argStream << "-class " << className
              << " | awk"
              << " -vdev_type=" << deviceType
              << " -vdevice_idx=" << deviceNumber
              << " -vfield=" << fieldName
              << " \'"
              << " match($0, \"^[ ]+\\\\*-\"){show=0}"
              << " match($0, \"^  \\\\*-\" dev_type){++dev;show=1}"
              << " field \":\" == $1"
              << " && show==1 && device_idx==dev{$1=\"\";print }\'";
    string value = RUN_PROCESS_OR_THROW("lshw", argStream.str());

    if (value.empty()) {
        return NOT_SPECIFIED;
    }
    return value;
}

void DeviceInfoCollector::addHardDriveInfoIfAvailable(
        hirs::pb::HardwareInfo* hwInfo) {
    int numHardDrives = getLshwDeviceCount("disk", "disk");

    for (int hdNumber = 1; hdNumber <= numHardDrives; hdNumber++) {
        try {
            hirs::pb::ComponentInfo hardDriveInfo;

            hardDriveInfo.set_manufacturer(
                getLshwDeviceField(hdNumber, "vendor", "disk", "disk"));

            hardDriveInfo.set_model(
                getLshwDeviceField(hdNumber, "product", "disk", "disk"));

            hardDriveInfo.set_serialnumber(
                getLshwDeviceField(hdNumber, "serial", "disk", "disk"));

            hardDriveInfo.set_revision(
                getLshwDeviceField(hdNumber, "version", "disk", "disk"));

            hwInfo->add_harddriveinfo();
            hwInfo->mutable_harddriveinfo(hdNumber - 1)->CopyFrom(
                    hardDriveInfo);
        } catch (const HirsRuntimeException& e) {
            LOGGER.warn(e.what());
        }
    }
}

void DeviceInfoCollector::addMemoryInfoIfAvailable(
        hirs::pb::HardwareInfo* hwInfo) {
    int numDimms = atoi(RUN_PROCESS_OR_THROW("dmidecode", "-t 17 "
            "| grep Manufacturer | wc -l").c_str());

    for (int dimmNumber = 1; dimmNumber <= numDimms; dimmNumber++) {
        try {
            hirs::pb::ComponentInfo memoryInfo;

            memoryInfo.set_manufacturer(
                    RUN_PROCESS_OR_THROW("dmidecode",
                                         "-t 17 | grep 'Manufacturer:' "
                                         "| sed -e 's/[^:]*:[ ]*//' -n -e "
                                         + std::to_string(dimmNumber) + "p"));

            memoryInfo.set_model(
                    RUN_PROCESS_OR_THROW("dmidecode",
                                         "-t 17 | grep 'Part Number:' "
                                         "| sed -e 's/[^:]*:[ ]*//' -n -e "
                                         + std::to_string(dimmNumber) + "p"));
            try {
                memoryInfo.set_serialnumber(
                        RUN_PROCESS_OR_THROW("dmidecode",
                                             "-t 17 | grep 'Serial Number:' "
                                             "| sed -e 's/[^:]*:[ ]*//' -n -e "
                                             + std::to_string(dimmNumber) +
                                             "p"));
            } catch (const HirsRuntimeException& e) {
                LOGGER.warn(e.what());
            }

            hwInfo->add_memoryinfo();
            hwInfo->mutable_memoryinfo(dimmNumber - 1)->CopyFrom(memoryInfo);
        } catch (const HirsRuntimeException& e) {
            LOGGER.warn(e.what());
        }
    }
}
