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
            std::vector<string> tokens;
            char* delim = const_cast<char*>("=");
            while (getline(ss, item, *delim)) {
                tokens.push_back(item);
            }
            for (int i=0; i < tokens.size(); i++) {
                if (tokens[i] == "ID") {
                    info.set_distribution(tokens[i+1]);
                }
                if (tokens[i] == "VERSION_ID") {
                    info.set_distributionrelease(tokens[i+1]);
                }
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
