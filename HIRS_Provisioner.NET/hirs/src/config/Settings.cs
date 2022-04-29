using Microsoft.Extensions.Configuration;
using Serilog;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;

namespace hirs {
    public class Settings {
        public enum Options {
            paccor_output_file,
            aca_address_port,
            efi_prefix,
            auto_detect_tpm,
            event_log_path,
            hardware_manifest_name
        }

        private static readonly string DEFAULT_SETTINGS_FILE = "appsettings.json";
        private static readonly string EFI_ARTIFACT_PATH_COMPAT = "/boot/tcg/";
        private static readonly string EFI_ARTIFACT_PATH = "/EFI/tcg/";


        private string settingsFile;
        private string paccor_output_file;
        private Uri aca_address_port;
        private string efi_prefix;
        private bool auto_detect_tpm;
        private string event_log_path;
        private readonly List<IHardwareManifest> hardwareManifests;
        private readonly string hardware_manifest_name;

        public Settings() : this(Settings.DEFAULT_SETTINGS_FILE) { }
        public Settings(string file) {
            settingsFile = file;
            try {
                IConfiguration configuration = readSettingsFile();
                Log.Logger = new LoggerConfiguration().ReadFrom.Configuration(configuration).CreateLogger();

                if (!string.IsNullOrWhiteSpace(configuration[Options.hardware_manifest_name.ToString()])) {
                    hardware_manifest_name = $"{ configuration[Options.hardware_manifest_name.ToString()] }";
                    List<string> names = Regex.Replace(hardware_manifest_name, @"\s", "").Split(',').ToList();
                    HardwareManifestPluginManager plugins = new HardwareManifestPluginManager();
                    hardwareManifests = plugins.LoadPlugins(names);
                }
                
                if (!string.IsNullOrWhiteSpace(configuration[Options.paccor_output_file.ToString()])) {
                    paccor_output_file = $"{ configuration[Options.paccor_output_file.ToString()] }";
                } else {
                    Log.Warning(Options.paccor_output_file.ToString() + " not set in the settings file.");
                }
                if (!string.IsNullOrWhiteSpace(configuration[Options.aca_address_port.ToString()])) {
                    string aca_address_port_str = $"{ configuration[Options.aca_address_port.ToString()] }";
                    parseAcaAddress(aca_address_port_str);
                }
                if (!HasAcaAddress()) {
                    Log.Error(Options.aca_address_port.ToString() + " not set in the settings file. No server to talk to. Looking for the format: \"https://<aca_server>:<port>\"");
                }
                if (!string.IsNullOrWhiteSpace(configuration[Options.efi_prefix.ToString()])) {
                    efi_prefix = $"{ configuration[Options.efi_prefix.ToString()] }";
                    checkEfiPrefix();
                }
                if (efi_prefix == null) {
                    Log.Warning(Options.efi_prefix.ToString() + " not set in the settings file. Will not attempt to scan for TCG platform artifacts.");
                }
                if (!string.IsNullOrWhiteSpace(configuration[Options.auto_detect_tpm.ToString()])) {
                    string auto_detect_tpm_str = $"{ configuration[Options.auto_detect_tpm.ToString()] }";
                    parseAutoDetectTpm(auto_detect_tpm_str);
                } else {
                    auto_detect_tpm = false;
                    Log.Warning(Options.auto_detect_tpm.ToString() + " not set in the settings file. Setting to default of false.");
                }
                if (!string.IsNullOrWhiteSpace(configuration[Options.event_log_path.ToString()])) {
                    event_log_path = $"{ configuration[Options.event_log_path.ToString()] }";
                    checkEventLogPath();
                }
                if (event_log_path == null) {
                    Log.Warning(Options.event_log_path.ToString() + " not set in the settings file.");
                } else {
                    Log.Warning(Options.event_log_path.ToString() + " was set in the settings file. Will look for the event log at: " + event_log_path);
                }
            } catch (Exception e) {
                if (Log.Logger == null) {
                    Console.WriteLine("Could not set up logging.");
                }
                Log.Error(e, "Error reading the settings file.");
                throw;
            }
        }
        private string GetBasePath() {
            return AppContext.BaseDirectory;
            //using var processModule = Process.GetCurrentProcess().MainModule;
            //return Path.GetDirectoryName(processModule?.FileName);
        }

        private IConfiguration readSettingsFile() {
            string basePath = GetBasePath();
            IConfiguration configuration = new ConfigurationBuilder()
                                .SetBasePath(basePath)
                                .AddJsonFile(settingsFile, false, true)
                                .Build();
            Log.Debug("Reading settings file: " + settingsFile + " from " + basePath);
            return configuration;
        }

        private void checkPaccorOutputFile() {
            Log.Debug("Checking that the " + Options.paccor_output_file.ToString() + " exists.");
            if (!string.IsNullOrWhiteSpace(paccor_output_file)) {
                paccor_output_file = Path.GetFullPath(paccor_output_file);
                if (!File.Exists(paccor_output_file)) {
                    paccor_output_file = null;
                    Log.Debug(Options.paccor_output_file.ToString() + ": " + paccor_output_file + " did not exist.");
                }
            } else {
                Log.Debug(Options.paccor_output_file.ToString() + " was not set in the settings file.");
            }
        }

        public virtual string getPaccorOutput() {
            Log.Debug("Retrieving JSON-formatted components from " + Options.paccor_output_file.ToString() + ".");
            string paccorOutput = "";
            checkPaccorOutputFile();
            if (paccor_output_file != null) {
                paccorOutput = File.ReadAllText(paccor_output_file);
            }
            if (string.IsNullOrWhiteSpace(paccorOutput)) {
                Log.Warning(Options.paccor_output_file.ToString() + " Paccor output was empty. Cannot perform Platform Attribute validation.");
            } // TODO JSON Schema validation?
            return paccorOutput;
        }

        public virtual string getHardwareManifest() {
            Log.Debug("Running hardware manifest plugins on " + string.Join(",", hardwareManifests) + ".");
            string manifestJson = "";
            foreach (IHardwareManifest manifest in hardwareManifests) {
                try {
                    manifestJson = string.Join(manifestJson, manifest.asJsonString());
                } catch (Exception e) {
                    Log.Debug($"Problem retrieving hardware manifest from {manifest.Name}.", e.InnerException);
                }
                
            }
            //TODO: Verify JSON?
            return manifestJson;
        }


        public virtual string getPaccorOutputFile() {
            return paccor_output_file;
        }
        public virtual Uri getAcaAddress() {
            return aca_address_port;
        }

        private void parseAcaAddress(string acaAddressStr) {
            Log.Debug("Parsing " + Options.aca_address_port.ToString() + ".");
            if (!string.IsNullOrWhiteSpace(acaAddressStr)) {
                aca_address_port = new Uri(acaAddressStr);
                Log.Debug(Options.aca_address_port.ToString() + ": " + aca_address_port);
            }
        }

        private void checkEfiPrefix() {
            Log.Debug("Checking that the " + Options.efi_prefix.ToString() + " path exists.");
            if (string.IsNullOrWhiteSpace(efi_prefix)) { // If not explicitly set in appsettings, try to use default EFI location on Linux
                if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux)) {
                    efi_prefix = "/boot/efi/EFI";
                }
            }
            
            if (!string.IsNullOrWhiteSpace(efi_prefix)) {
                if (!Directory.Exists(efi_prefix)) {
                    efi_prefix = null;
                    Log.Debug(Options.efi_prefix.ToString() + ": " + efi_prefix + " did not exist.");
                }
            } else {
                Log.Debug(Options.efi_prefix.ToString() + " was not set in the settings file.");
            }
        }

        private void parseAutoDetectTpm(string autoDetectTpmStr) {
            try {
                auto_detect_tpm = Boolean.Parse(autoDetectTpmStr);
                Log.Debug(Options.auto_detect_tpm.ToString() + ": " + (auto_detect_tpm ? "true" : "false"));
            } catch (FormatException) {
                auto_detect_tpm = false;
                Log.Warning(Options.auto_detect_tpm.ToString() + " did not contain what the .NET API considers a TrueString nor a FalseString. Setting to default of false.");
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
                if (files.Count() > 0) { // if none found, don't initialize platformCerts
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

        private void checkEventLogPath() {
            Log.Debug("Checking that the " + Options.event_log_path.ToString() + " path exists.");
            if (!string.IsNullOrWhiteSpace(event_log_path)) {
                if (!File.Exists(event_log_path)) {
                    event_log_path = null;
                    Log.Debug(Options.event_log_path.ToString() + ": " + event_log_path + " did not exist.");
                }
            } else {
                Log.Debug(Options.event_log_path.ToString() + " was not set in the settings file.");
            }
        }

        public virtual byte[] gatherEventLogFromAppsettingsPath() {
            byte[] file = null;
            if (!string.IsNullOrWhiteSpace(event_log_path)) {
                file = File.ReadAllBytes(event_log_path);
            }
            return file;
        }

        public bool HasPaccorOutputFile() {
            return (paccor_output_file != null);
        }
        public bool HasAcaAddress() {
            return (aca_address_port != null);
        }
        public bool HasEfiPrefix() {
            return (efi_prefix != null);
        }
        public bool IsAutoDetectTpmEnabled() {
            return auto_detect_tpm;
        }
        public bool hasEventLogPath() {
            return (event_log_path != null);
        }
    }
}
