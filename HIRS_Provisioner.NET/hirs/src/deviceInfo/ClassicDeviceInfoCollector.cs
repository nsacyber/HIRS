using Hirs.Pb;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Management;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Text;
using System.IO;
using Serilog;

namespace hirs {
    public class ClassicDeviceInfoCollector : IHirsDeviceInfoCollector {
        public static readonly string NOT_SPECIFIED = "Not Specified";

        public ClassicDeviceInfoCollector() {
        }

        public string fileToString(string path, string def) {
            string result;
            try {
                result = File.ReadAllText(path).Trim();
            } catch {
                result = def;
            }
            if (string.IsNullOrWhiteSpace(result)) {
                result = def;
            }
            return result;
        }

        public DeviceInfo collectDeviceInfo(string acaAddress) {
            DeviceInfo dv = new DeviceInfo();
            dv.Fw = collectFirmwareInfo();
            dv.Hw = collectHardwareInfo();
            dv.Nw = collectNetworkInfo(acaAddress);
            dv.Os = collectOsInfo();
            return dv;
        }

        public FirmwareInfo collectFirmwareInfo() {
            FirmwareInfo fw = new FirmwareInfo();

            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) {
                ManagementScope myScope = new ManagementScope("root\\CIMV2");
                ManagementObjectSearcher s = new ManagementObjectSearcher("SELECT * FROM Win32_BIOS");
                fw.BiosVendor = NOT_SPECIFIED;
                fw.BiosVersion = NOT_SPECIFIED;
                fw.BiosReleaseDate = NOT_SPECIFIED;
                foreach (ManagementObject o in s.Get()) {
                    string manufacturer = (string)o.GetPropertyValue("Manufacturer");
                    string version = (string)o.GetPropertyValue("Version");
                    string releasedate = (string)o.GetPropertyValue("ReleaseDate");
                    fw.BiosVendor = string.IsNullOrEmpty(manufacturer) ? NOT_SPECIFIED : manufacturer;
                    fw.BiosVersion = string.IsNullOrEmpty(version) ? NOT_SPECIFIED : version;
                    fw.BiosReleaseDate = string.IsNullOrEmpty(releasedate) ? NOT_SPECIFIED : releasedate;
                    break;
                }
            } else if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux)) {
                fw.BiosVendor = fileToString("/sys/class/dmi/id/bios_vendor", NOT_SPECIFIED);
                fw.BiosVersion = fileToString("/sys/class/dmi/id/bios_version", NOT_SPECIFIED);
                fw.BiosReleaseDate = fileToString("/sys/class/dmi/id/bios_date", NOT_SPECIFIED);
            } else if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX)) {
                // tbd
            } else {
                // tbd
            }

            Log.Debug("Bios Vendor: " + fw.BiosVendor);
            Log.Debug("Bios Version: " + fw.BiosVersion);
            Log.Debug("Bios Date: " + fw.BiosReleaseDate);

            return fw;
        }

        

        public HardwareInfo collectHardwareInfo() {
            HardwareInfo hw = new HardwareInfo();

            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) {
                ManagementScope myScope = new ManagementScope("root\\CIMV2");
                ManagementObjectSearcher s = new ManagementObjectSearcher("SELECT * FROM Win32_ComputerSystemProduct");
                hw.Manufacturer = NOT_SPECIFIED;
                hw.ProductName = NOT_SPECIFIED;
                hw.ProductVersion = NOT_SPECIFIED;
                hw.SystemSerialNumber = NOT_SPECIFIED;
                foreach (ManagementObject o in s.Get()) {
                    string vendor = (string)o.GetPropertyValue("Vendor");
                    string name = (string)o.GetPropertyValue("Name");
                    string version = (string)o.GetPropertyValue("Version");
                    string identifyingnumber = (string)o.GetPropertyValue("IdentifyingNumber");
                    hw.Manufacturer = string.IsNullOrWhiteSpace(vendor) ? NOT_SPECIFIED : vendor;
                    hw.ProductName = string.IsNullOrWhiteSpace(name) ? NOT_SPECIFIED : name;
                    hw.ProductVersion = string.IsNullOrWhiteSpace(version) ? NOT_SPECIFIED : version;
                    hw.SystemSerialNumber = string.IsNullOrWhiteSpace(identifyingnumber) ? NOT_SPECIFIED : identifyingnumber;
                    break;
                }
            } else if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux)) {
                hw.Manufacturer = fileToString("/sys/class/dmi/id/sys_vendor", NOT_SPECIFIED);
                hw.ProductName = fileToString("/sys/class/dmi/id/product_name", NOT_SPECIFIED);
                hw.ProductVersion = fileToString("/sys/class/dmi/id/product_version", NOT_SPECIFIED);
                hw.SystemSerialNumber = fileToString("/sys/class/dmi/id/product_serial", NOT_SPECIFIED);
            } else if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX)) {
                // tbd
            } else {
                // tbd
            }

            Log.Debug("System Manufacturer: " + hw.Manufacturer);
            Log.Debug("Product Name: " + hw.ProductName);
            Log.Debug("Product Version: " + hw.ProductVersion);
            Log.Debug("System Serial Number: " + hw.SystemSerialNumber);

            return hw;
        }

        public NetworkInfo collectNetworkInfo(string acaAddress) {
            NetworkInfo nw = new NetworkInfo();
            nw.Hostname = Dns.GetHostName();

            NetworkInterface iface = NetworkInterface
                .GetAllNetworkInterfaces()
                .Where(nic => nic.OperationalStatus == OperationalStatus.Up && nic.NetworkInterfaceType != NetworkInterfaceType.Loopback)
                .FirstOrDefault();

            nw.MacAddress = iface.GetPhysicalAddress().ToString();

            nw.ClearIpAddress();
            // First attempt to find local ip by connecting ACA
            if (string.IsNullOrWhiteSpace(acaAddress)) {
                Uri uri = new Uri(acaAddress);
                try {
                    using (Socket socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, 0)) {
                        socket.Connect(uri.Host, uri.Port);
                        IPEndPoint endPoint = socket.LocalEndPoint as IPEndPoint;
                        nw.IpAddress = endPoint.Address.ToString();
                    }
                } catch {
                    Log.Debug("Not connected to the internet. Trying another search.");
                }
            }
            // Second attempt to find local ip by scanning first interface that is up and not a loopback address 
            if (!nw.HasIpAddress) {
                foreach (UnicastIPAddressInformation ip in iface.GetIPProperties().UnicastAddresses) {
                    if (ip.Address.AddressFamily == AddressFamily.InterNetwork) {
                        nw.IpAddress = ip.Address.ToString();
                        break;
                    }
                }
            }

            Log.Debug("Network Info IP: " + nw.IpAddress);
            Log.Debug("Network Info MAC: " + nw.MacAddress);
            Log.Debug("Network Info Hostname: " + nw.Hostname);
            
            return nw;
        }

        public OsInfo collectOsInfo() {
            OsInfo info = new OsInfo();

            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) {
                info.OsName = OSPlatform.Windows.ToString();
            } else if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux)) {
                info.OsName = OSPlatform.Linux.ToString();
            } else if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX)) {
                info.OsName = OSPlatform.OSX.ToString();
            } else {
                info.OsName = RuntimeInformation.OSDescription;
            }
            info.OsVersion = RuntimeInformation.OSDescription;
            info.OsArch = RuntimeInformation.OSArchitecture.ToString();
            info.Distribution = RuntimeInformation.FrameworkDescription;
            info.DistributionRelease = RuntimeInformation.FrameworkDescription;

            Log.Debug("OS Name: " + info.OsName);
            Log.Debug("OS Version: " + info.OsVersion);
            Log.Debug("Architecture: " + info.OsArch);
            Log.Debug("Distribution: " + info.Distribution);
            Log.Debug("Distribution Release: " + info.DistributionRelease);

            return info;
        }
    }
}
