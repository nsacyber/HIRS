using HardwareManifestPlugin;
using HardwareManifestPluginManager;
using Microsoft.Extensions.Configuration;
using Serilog;
using System;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Collections.Generic;

namespace hirs {
    public class Settings {
        public enum Options {
            paccor_output_file,
            aca_address_port,
            efi_prefix,
            auto_detect_tpm,
            event_log_file,
            hardware_manifest_collectors,
            hardware_manifest_collection_swid_enforced,
            linux_bios_vendor_file,
            linux_bios_version_file,
            linux_bios_date_file,
            linux_sys_vendor_file,
            linux_product_name_file,
            linux_product_version_file,
            linux_product_serial_file
        }

        private static readonly string DEFAULT_SETTINGS_FILE = "appsettings.json";
        private static readonly string EFI_ARTIFACT_PATH_COMPAT = "/boot/tcg/";
        private static readonly string EFI_ARTIFACT_PATH = "/EFI/tcg/";
        private static readonly string EFI_ARTIFACT_LINUX_PREFIX = "/boot/efi";

        private readonly string settingsFile;
        private readonly IConfiguration configFromSettingsFile;

        // Storage of options collected from the settingsFile, with some default values
        public virtual string paccor_output {
            get; private set;
        }
        public virtual Uri aca_address_port {
            get; private set;
        }
        public string efi_prefix {
            get; private set;
        }
        public bool auto_detect_tpm {
            get; private set;
        }
        public virtual byte[] event_log {
            get; private set;
        }
        public virtual string linux_bios_vendor {
            get; private set;
        }
        public virtual string linux_bios_version {
            get; private set;
        }
        public virtual string linux_bios_date {
            get; private set;
        }
        public virtual string linux_sys_vendor {
            get; private set;
        }
        public virtual string linux_product_name {
            get; private set;
        }
        public virtual string linux_product_version {
            get; private set;
        }
        public virtual string linux_product_serial {
            get; private set;
        }
        private List<IHardwareManifest> hardwareManifests = new();
        private Dictionary<string, string> hardware_manifest_collectors_with_args = new();
        private bool hardware_manifest_collection_swid_enforced = false;

        private Settings() : this(Settings.DEFAULT_SETTINGS_FILE) { }
        /// <summary>
        /// </summary>
        /// <param name="file">The path to the appsettings.json file on the file system.</param>
        private Settings(string file) {
            settingsFile = file;
            configFromSettingsFile = ReadSettingsFile();
        }

        public static Settings LoadSettingsFromDefaultFile() {
            return new(DEFAULT_SETTINGS_FILE);
        }
        /// <summary>
        /// </summary>
        /// <param name="path">The path to the settings JSON file on the file system.</param>
        public static Settings LoadSettingsFromFile(string path) {
            Settings settings = new(path);
            return settings;
        }

        private static string GetBasePath() {
            return AppContext.BaseDirectory;
        }

        private IConfiguration ReadSettingsFile() {
            string basePath = GetBasePath();
            IConfiguration configuration = new ConfigurationBuilder()
                                .SetBasePath(basePath)
                                .AddJsonFile(settingsFile, false, true)
                                .Build();
            return configuration;
        }

        public void SetUpLog() {
            Log.Logger = new LoggerConfiguration().ReadFrom.Configuration(configFromSettingsFile).CreateLogger();
            Log.Debug("Reading settings file: " + Path.GetFullPath(Path.Combine(GetBasePath(), settingsFile)));
        }

        public void CompleteSetUp() {
            try {
                ConfigureHardwareManifestManagement();

                IngestPaccorDataFromFile();

                ParseAcaAddress();

                CheckAutoDetectTpm();

                CheckEfiPrefix();

                IngestEventLogFromFile();

                StoreCustomDeviceInfoCollectorOptions();

            } catch (Exception e) {
                if (Log.Logger == null) {
                    Console.WriteLine("Could not set up logging.");
                }
                Log.Error(e, "Error reading the settings file.");
                throw;
            }
        }

        #region Hardware Manifest
        private void ConfigureHardwareManifestManagement() {
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.hardware_manifest_collectors.ToString()])) {
                Log.Debug("Configuring Hardware Manifest Plugin Manager");
                string hardware_manifest_collectors = $"{ configFromSettingsFile[Options.hardware_manifest_collectors.ToString()] }";
                hardware_manifest_collectors_with_args = ParseHardwareManifestCollectorsString(hardware_manifest_collectors);
                // Collectors are identified by Name.
                // Multiple collectors can be identified with a comma delimiter between collector names.
                // There is a field in the HardwareManifestPlugin Interface that must match this Name.
                // Each Name can be optionally followed by a space and command-line style arguments.
                //   Those arguments must not break the JSON encoding of the settings file.
                //   ex: collector1_name -a --b=c,collector2_name,collector3_name
                // If SWID enforcement is enabled, Collectors must also pass validation prior to loading.
                // Once loaded, the command-line arguments are passed directly to the collector by the Configure method of the Interface.
                List<string> names = hardware_manifest_collectors_with_args.Keys.ToList();
                if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.hardware_manifest_collection_swid_enforced.ToString()])) {
                    string hardware_manifest_collection_swid_enforced_str = $"{ configFromSettingsFile[Options.hardware_manifest_collection_swid_enforced.ToString()] }";
                    hardware_manifest_collection_swid_enforced = Boolean.Parse(hardware_manifest_collection_swid_enforced_str);
                    Log.Debug("SWID enforcement of Hardware Manifest Plugins are " + (hardware_manifest_collection_swid_enforced ? "en" : "dis") + "abled in settings.");
                }
                hardwareManifests = HardwareManifestPluginManagerUtils.LoadPlugins(names, hardware_manifest_collection_swid_enforced);
                CleanHardwareManifestCollectors();
                Log.Debug("Finished configuring the Hardware Manifest Plugin Manager.");
            } else {
                Log.Debug("Hardware Manifest Plugin Manager will not be used. No collectors were identified in settings.");
            }
        }

        private static Dictionary<string, string> ParseHardwareManifestCollectorsString(string hardware_manifest_collectors) {
            Dictionary<string, string> dict = new();
            List<string> names = hardware_manifest_collectors.Split(',').Select(s => s.Trim()).ToList();
            foreach (string name in names) {
                string[] parts = name.Split(' ', 2); // split on first space
                dict.Add(parts[0], parts.Length == 2 ? parts[1] : "");
            }
            return dict;
        }

        private void CleanHardwareManifestCollectors() {
            List<string> names = hardwareManifests.Select(x => x.Name).ToList();
            Dictionary<string, string> dict = new();
            foreach (string name in names) {
                dict.Add(name, hardware_manifest_collectors_with_args[name]);
            }
            hardware_manifest_collectors_with_args.Clear();
            hardware_manifest_collectors_with_args = dict;
        }

        public virtual string RunHardwareManifestCollectors() {
            Log.Debug("Gathering data from loaded hardware manifest collectors.");
            string manifestJson = "";
            foreach (IHardwareManifest manifest in hardwareManifests) {
                try {
                    Log.Debug("  Configuring " + manifest.Name);
                    if (hardware_manifest_collectors_with_args.ContainsKey(manifest.Name)) {
                        manifest.Configure(CLI.SplitArgs(hardware_manifest_collectors_with_args[manifest.Name]));
                    }
                    // TODO: Combine JSON Better
                    // OR Return proto objects
                    Log.Debug("  Gathering from " + manifest.Name);
                    manifestJson = string.Join(manifestJson, manifest.GatherHardwareManifestAsJsonString());
                } catch (Exception e) {
                    Log.Debug($"Problem retrieving hardware manifest from {manifest.Name}.", e.InnerException);
                }
            }
            //TODO: Verify JSON?
            return manifestJson;
        }
        #endregion

        #region Ingest paccor data from file
        private void IngestPaccorDataFromFile() {
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.paccor_output_file.ToString()])) {
                Log.Debug("Checking location of the paccor output file.");
                string paccor_output_path = $"{ configFromSettingsFile[Options.paccor_output_file.ToString()] }";
                if (DoesFileExist(paccor_output_path, out paccor_output_path)) {
                    if (HasHardwareManifestPlugins()) {
                        Log.Warning("The settings file specified hardware manifest collectors and a paccor output file. Fresh data is preferred over data from a file. If you want to use the file data, clear the collectors field from the settings file.");
                    } else {
                        Log.Debug("Retrieving components from " + Options.paccor_output_file.ToString() + ".");
                        paccor_output = File.ReadAllText(paccor_output_path);
                        if (string.IsNullOrWhiteSpace(paccor_output)) {
                            Log.Warning(Options.paccor_output_file.ToString() + " Paccor output was empty. Cannot perform Platform Attribute validation.");
                        } else {
                            Log.Debug("Output file contains:\n" + paccor_output);
                        }
                    }
                }
            } else {
                Log.Debug(Options.paccor_output_file.ToString() + " not set in the settings file.");
            }
        }
        #endregion

        #region ACA Address
        private void ParseAcaAddress() {
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.aca_address_port.ToString()])) {
                Log.Debug("Parsing the ACA Address.");
                string aca_address_port_str = $"{ configFromSettingsFile[Options.aca_address_port.ToString()] }";
                if (!string.IsNullOrWhiteSpace(aca_address_port_str)) {
                    aca_address_port = new Uri(aca_address_port_str);
                    Log.Debug("  Found " + aca_address_port);
                }
            }
            if (!HasAcaAddress()) {
                Log.Error(Options.aca_address_port.ToString() + " not set in the settings file. No HIRS ACA server to talk to. Looking for the format: \"https://<aca_server>:<port>\"");
            }
        }
        #endregion

        #region Auto Detect TPM
        private void CheckAutoDetectTpm() {
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.auto_detect_tpm.ToString()])) {
                Log.Debug("Checking Auto Detect TPM setting.");
                string auto_detect_tpm_str = $"{ configFromSettingsFile[Options.auto_detect_tpm.ToString()] }";
                try {
                    auto_detect_tpm = Boolean.Parse(auto_detect_tpm_str);
                    Log.Debug(" Auto Detect TPM is " + (auto_detect_tpm ? "en" : "dis") + "abled.");
                } catch (FormatException) {
                    auto_detect_tpm = false;
                    Log.Warning(Options.auto_detect_tpm.ToString() + " did not contain a readable true/false setting. Setting to default of false.");
                }
            } else {
                auto_detect_tpm = false;
                Log.Debug(Options.auto_detect_tpm.ToString() + " not set in the settings file. Setting to default of false.");
            }
        }
        #endregion

        #region EFI
        private void CheckEfiPrefix() {
            if (configFromSettingsFile[Options.efi_prefix.ToString()] != null) {
                Log.Debug("Checking EFI Prefix setting.");
                efi_prefix = $"{ configFromSettingsFile[Options.efi_prefix.ToString()] }";
                if (string.IsNullOrWhiteSpace(efi_prefix)) { // If not explicitly set in appsettings, try to use default EFI location on Linux
                    if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux)) {
                        efi_prefix = EFI_ARTIFACT_LINUX_PREFIX + EFI_ARTIFACT_PATH;
                    }
                } else {
                    if (!Directory.Exists(efi_prefix)) {
                        Log.Debug(Options.efi_prefix.ToString() + ": " + efi_prefix + " did not exist.");
                        efi_prefix = null;
                    }
                }
            }
            if (efi_prefix == null) {
                Log.Warning(Options.efi_prefix.ToString() + " not set in the settings file. Will not attempt to scan for artifacts in EFI.");
            } else {
                Log.Debug("  Will scan for artifacts in " + efi_prefix);
            }
        }
        
        public virtual List<byte[]> gatherPlatformCertificatesFromEFI() {
            // According to FIM: EFIPREFIX/boot/tcg/{cert,pccert,platform}
            List<byte[]> platformCerts = null;
            if (!string.IsNullOrWhiteSpace(efi_prefix)) {
                EnumerationOptions enumOpts = new EnumerationOptions();
                enumOpts.MatchCasing = MatchCasing.CaseInsensitive;
                enumOpts.RecurseSubdirectories = true;
                List<FileInfo> files = new List<FileInfo>();
                string[] paths = { "cert", "pccert", "platform" };
                foreach (string subdir in paths) {
                    string path = efi_prefix + EFI_ARTIFACT_PATH + subdir;
                    if (Directory.Exists(path)) {
                        files.AddRange(new DirectoryInfo(path).GetFiles("*base*", enumOpts));
                        files.AddRange(new DirectoryInfo(path).GetFiles("*delta*", enumOpts));
                    } else {
                        path = efi_prefix + EFI_ARTIFACT_PATH_COMPAT + subdir;
                        if (Directory.Exists(path)) {
                            files.AddRange(new DirectoryInfo(path).GetFiles("*base*", enumOpts));
                            files.AddRange(new DirectoryInfo(path).GetFiles("*delta*", enumOpts));
                        }
                    }
                }
                if (files.Count > 0) { // if none found, don't initialize platformCerts
                    // At least one base platform cert found
                    platformCerts = new List<byte[]>();
                    foreach (FileInfo file in files) {
                        platformCerts.Add(File.ReadAllBytes(file.FullName));
                        Log.Debug("gatherPlatformCertificatesFromEFI: Gathering " + file.FullName);
                    }
                }
            } else {
                Log.Warning("gatherPlatformCertificatesFromEFI was called without verifying HasEfiPrefix.");
            }
            Log.Debug("Found " + (platformCerts == null ? 0 : platformCerts.Count) + " platform certs.");
            return platformCerts;
        }

        public virtual List<byte[]> gatherRIMBasesFromEFI() {
            // According to PC Client RIM
            List<byte[]> baseRims = null;

            // /boot/tcg/manifest/swidtag	Base RIM Files
            //      <name of the tag creator> + <product name> + <RIM version>.swidtag
            if (!string.IsNullOrWhiteSpace(efi_prefix)) {
                EnumerationOptions enumOpts = new EnumerationOptions();
                enumOpts.MatchCasing = MatchCasing.CaseInsensitive;
                enumOpts.RecurseSubdirectories = true;
                List<FileInfo> files = new List<FileInfo>();

                string[] paths = { "manifest", "swidtag" };
                string ext = "*swidtag";
                foreach (string subdir in paths) {
                    string path = efi_prefix + EFI_ARTIFACT_PATH + subdir;
                    if (Directory.Exists(path)) {
                        files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                    } else {
                        path = efi_prefix + EFI_ARTIFACT_PATH_COMPAT + subdir;
                        if (Directory.Exists(path)) {
                            files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                        }
                    }
                }
                if (files.Count() > 0) { // if none found, don't initialize baseRims
                    // At least one base platform cert found
                    baseRims = new List<byte[]>();
                    foreach (FileInfo file in files) {
                        baseRims.Add(File.ReadAllBytes(file.FullName));
                        Log.Debug("gatherRIMBasesFromEFI: Gathering " + file.FullName);
                    }
                }
            } else {
                Log.Warning("gatherRIMBasesFromEFI was called without verifying HasEfiPrefix.");
            }
            Log.Debug("Found " + (baseRims == null ? 0 : baseRims.Count) + " base RIMs.");
            return baseRims;
        }

        public virtual List<byte[]> gatherSupportRIMELsFromEFI() {
            // According to PC Client RIM
            List<byte[]> supportRimELs = null;
            // /boot/tcg/manifest/rim  Support RIM Files
            //      <name of the tag creator> + <product name> + <product version>.rimel
            if (!string.IsNullOrWhiteSpace(efi_prefix)) {
                EnumerationOptions enumOpts = new EnumerationOptions();
                enumOpts.MatchCasing = MatchCasing.CaseInsensitive;
                enumOpts.RecurseSubdirectories = true;
                List<FileInfo> files = new List<FileInfo>();

                string[] paths = { "manifest", "rim" };
                string ext = "*rimel";
                foreach (string subdir in paths) {
                    string path = efi_prefix + EFI_ARTIFACT_PATH + subdir;
                    if (Directory.Exists(path)) {
                        files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                    } else {
                        path = efi_prefix + EFI_ARTIFACT_PATH_COMPAT + subdir;
                        if (Directory.Exists(path)) {
                            files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                        }
                    }
                }
                if (files.Count() > 0) { // if none found, don't initialize baseRims
                    // At least one base platform cert found
                    supportRimELs = new List<byte[]>();
                    foreach (FileInfo file in files) {
                        supportRimELs.Add(File.ReadAllBytes(file.FullName));
                        Log.Debug("gatherSupportRIMELsFromEFI: Gathering " + file.FullName);
                    }
                }
            } else {
                Log.Warning("gatherSupportRIMELsFromEFI was called without verifying HasEfiPrefix.");
            }
            Log.Debug("Found " + (supportRimELs == null ? 0 : supportRimELs.Count) + " support rimel files.");
            return supportRimELs;
        }

        public virtual List<byte[]> gatherSupportRIMPCRsFromEFI() {
            // According to PC Client RIM
            List<byte[]> supportRimPCRs = null;
            // /boot/tcg/manifest/rim  Support RIM Files
            //      <name of the tag creator> + <product name> + <product version>.rimpcr
            if (!string.IsNullOrWhiteSpace(efi_prefix)) {
                EnumerationOptions enumOpts = new EnumerationOptions();
                enumOpts.MatchCasing = MatchCasing.CaseInsensitive;
                enumOpts.RecurseSubdirectories = true;
                List<FileInfo> files = new List<FileInfo>();

                string[] paths = { "manifest", "rim" };
                string ext = "*rimpcr";
                foreach (string subdir in paths) {
                    string path = efi_prefix + EFI_ARTIFACT_PATH + subdir;
                    if (Directory.Exists(path)) {
                        files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                    } else {
                        path = efi_prefix + EFI_ARTIFACT_PATH_COMPAT + subdir;
                        if (Directory.Exists(path)) {
                            files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                        }
                    }
                }
                if (files.Count() > 0) { // if none found, don't initialize baseRims
                    // At least one base platform cert found
                    supportRimPCRs = new List<byte[]>();
                    foreach (FileInfo file in files) {
                        supportRimPCRs.Add(File.ReadAllBytes(file.FullName));
                        Log.Debug("gatherSupportRIMPCRsFromEFI: Gathering " + file.FullName);
                    }
                }
            } else {
                Log.Warning("gatherSupportRIMPCRsFromEFI was called without verifying HasEfiPrefix.");
            }
            Log.Debug("Found " + (supportRimPCRs == null ? 0 : supportRimPCRs.Count) + " support rimpcr files.");
            return supportRimPCRs;
        }
        #endregion

        #region Ingest Event Log from File
        private void IngestEventLogFromFile() {
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.event_log_file.ToString()])) {
                Log.Debug("Checking location of the event log.");
                string event_log_path = $"{ configFromSettingsFile[Options.event_log_file.ToString()] }";
                if (DoesFileExist(event_log_path, out event_log_path)) {
                    Log.Debug("Retrieving the Event Log. ");
                    event_log = File.ReadAllBytes(event_log_path);
                    if (event_log == null || event_log.Length == 0) {
                        Log.Warning(Options.event_log_file.ToString() + " The event log was empty.");
                    }
                }
            } else {
                Log.Debug(Options.event_log_file.ToString() + " not set in the settings file.");
            }

        }
        #endregion

        #region Store Custom Device Info Collector Options
        private void StoreCustomDeviceInfoCollectorOptions() {
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.linux_bios_vendor_file.ToString()])) {
                Log.Debug("Custom bios vendor file specified for the Device Info Collector on Linux.");
                string path = $"{ configFromSettingsFile[Options.linux_bios_vendor_file.ToString()] }";
                linux_bios_vendor = ReadFileText(path);
            }
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.linux_bios_version_file.ToString()])) {
                Log.Debug("Custom bios version file specified for the Device Info Collector on Linux.");
                string path = $"{ configFromSettingsFile[Options.linux_bios_version_file.ToString()] }";
                linux_bios_version = ReadFileText(path);
            }
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.linux_sys_vendor_file.ToString()])) {
                Log.Debug("Custom hardware manufacturer file specified for the Device Info Collector on Linux.");
                string path = $"{ configFromSettingsFile[Options.linux_sys_vendor_file.ToString()] }";
                linux_sys_vendor = ReadFileText(path);
            }
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.linux_product_name_file.ToString()])) {
                Log.Debug("Custom hardware product name file specified for the Device Info Collector on Linux.");
                string path = $"{ configFromSettingsFile[Options.linux_product_name_file.ToString()] }";
                linux_product_name = ReadFileText(path);
            }
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.linux_product_version_file.ToString()])) {
                Log.Debug("Custom hardware product version file specified for the Device Info Collector on Linux.");
                string path = $"{ configFromSettingsFile[Options.linux_product_version_file.ToString()] }";
                linux_product_version = ReadFileText(path);
            }
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[Options.linux_product_serial_file.ToString()])) {
                Log.Debug("Custom hardware product serial file specified for the Device Info Collector on Linux.");
                string path = $"{ configFromSettingsFile[Options.linux_product_serial_file.ToString()] }";
                linux_product_serial = ReadFileText(path);
            }
        }
        #endregion
        public static bool DoesFileExist(string path, out string out_full_path) {
            bool found = false;
            out_full_path = "";
            if (!string.IsNullOrWhiteSpace(path)) {
                out_full_path = Path.GetFullPath(path);
                found = File.Exists(out_full_path);
                if (!found) {
                    Log.Debug("  File identified in settings did not exist: " + out_full_path);
                } else {
                    Log.Debug("  File exists: " + out_full_path);
                }
            }
            return found;
        }

        public static string ReadFileText(string path) {
            string text = "";
            if (DoesFileExist(path, out string full_path)) {
                Log.Debug("  Reading file: " + full_path + ".");
                text = File.ReadAllText(full_path);
                Log.Debug("   " + (string.IsNullOrWhiteSpace(text) ? "File was empty." : text));
            }
            return text;
        }

        public bool HasHardwareManifestPlugins() {
            return hardwareManifests.Count > 0;
        }
        public bool HasPaccorOutputFromFile() {
            return !string.IsNullOrEmpty(paccor_output);
        }
        public bool HasAcaAddress() {
            return aca_address_port != null;
        }
        public bool HasEfiPrefix() {
            return !string.IsNullOrEmpty(efi_prefix);
        }
        public bool IsAutoDetectTpmEnabled() {
            return auto_detect_tpm;
        }
        public bool HasEventLogFromFile() {
            return event_log != null && event_log.Length > 0;
        }
    }
}
