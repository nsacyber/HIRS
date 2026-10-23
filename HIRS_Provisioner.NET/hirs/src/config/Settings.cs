using HardwareManifestPlugin;
using HardwareManifestPluginManager;
using Hirs.Pb;
using Microsoft.Extensions.Configuration;
using Serilog;
using System.Runtime.InteropServices;

namespace hirs;

public class Settings {
    // These Options names are used directly in the settings file. Keep formatting.
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
        linux_product_serial_file,
        certificate_output_directory,
        ek_template
    }

    public static readonly string DEFAULT_SETTINGS_FILE = "appsettings.json";
    public static readonly string EFI_ARTIFACT_PATH_COMPAT = "/boot/tcg/";
    public static readonly string EFI_ARTIFACT_PATH = "/EFI/tcg/";
    public static readonly string EFI_ARTIFACT_LINUX_PREFIX = "/boot/efi";
    public static readonly string DEFAULT_CERTIFICATE_OUTPUT_PATH = "/EFI/hirs";

    private readonly string settingsFile;
    private readonly IConfiguration configFromSettingsFile;

    // Storage of options collected from the settingsFile, with some default values
    public virtual string PaccorOutput {
        get; private set;
    }
    public virtual Uri AcaAddressPort {
        get; private set;
    }
    public string EfiPrefix {
        get; private set;
    }
    public bool AutoDetectTpm {
        get; private set;
    }
    public virtual byte[] EventLog {
        get; private set;
    }
    public virtual string LinuxBiosVendor {
        get; private set;
    }
    public virtual string LinuxBiosVersion {
        get; private set;
    }
    public virtual string LinuxBiosDate {
        get; private set;
    }
    public virtual string LinuxSysVendor {
        get; private set;
    }
    public virtual string LinuxProductName {
        get; private set;
    }
    public virtual string LinuxProductVersion {
        get; private set;
    }
    public virtual string LinuxProductSerial {
        get; private set;
    }
    public virtual string CertificateOutputDirectory {
        get; private set;
    }
    public virtual EkTemplate EkTemplate {
        get; private set;
    } = EkTemplate.EkL1; // default RSA-2048 Low Template
    private List<IHardwareManifestPlugin> hardwareManifests = [];
    private Dictionary<string, string> hardwareManifestCollectorsWithArgs = new();
    private bool hardwareManifestCollectionSwidEnforced;

    private Settings() : this(DEFAULT_SETTINGS_FILE) { }
    /// <summary>
    /// </summary>
    /// <param name="file">The path to the appsettings.json file on the file system.</param>
    private Settings(string file) {
        settingsFile = file;
        configFromSettingsFile = ReadSettingsFile();
    }

    public static Settings LoadSettingsFromDefaultFile() {
        return new Settings();
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

            CheckCertificateOutputDirectory();

            IngestEventLogFromFile();

            StoreCustomDeviceInfoCollectorOptions();

            CheckEkTemplate();

        } catch (Exception e) {
            Log.Error(e, "Error reading the settings file.");
            throw;
        }
    }

    #region Hardware Manifest
    private void ConfigureHardwareManifestManagement() {
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.hardware_manifest_collectors)])) {
            Log.Debug("Configuring Hardware Manifest Plugin Manager");
            string hardwareManifestCollectors = $"{ configFromSettingsFile[nameof(Options.hardware_manifest_collectors)] }";
            hardwareManifestCollectorsWithArgs = ParseHardwareManifestCollectorsString(hardwareManifestCollectors);
            // Collectors are identified by Name.
            // Multiple collectors can be identified with a comma delimiter between collector names.
            // There is a field in the HardwareManifestPlugin Interface that must match this Name.
            // Each Name can be optionally followed by a space and command-line style arguments.
            //   Those arguments must not break the JSON encoding of the settings file.
            //   ex: collector1_name -a --b=c,collector2_name,collector3_name
            // If SWID enforcement is enabled, Collectors must also pass validation prior to loading.
            // Once loaded, the command-line arguments are passed directly to the collector by the Configure method of the Interface.
            List<string> names = [.. hardwareManifestCollectorsWithArgs.Keys];
            if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.hardware_manifest_collection_swid_enforced)])) {
                string hardwareManifestCollectionSwidEnforcedStr = $"{ configFromSettingsFile[nameof(Options.hardware_manifest_collection_swid_enforced)] }";
                hardwareManifestCollectionSwidEnforced = bool.Parse(hardwareManifestCollectionSwidEnforcedStr);
                Log.Debug("SWID enforcement of Hardware Manifest Plugins are " + (hardwareManifestCollectionSwidEnforced ? "en" : "dis") + "abled in settings.");
            }
            hardwareManifests = HardwareManifestPluginManagerUtils.LoadPlugins(names, hardwareManifestCollectionSwidEnforced);
            CleanHardwareManifestCollectors();
            Log.Debug("Finished configuring the Hardware Manifest Plugin Manager.");
        } else {
            Log.Debug("Hardware Manifest Plugin Manager will not be used. No collectors were identified in settings.");
        }
    }

    private static Dictionary<string, string> ParseHardwareManifestCollectorsString(string hardwareManifestCollectors) {
        List<string> names = [.. hardwareManifestCollectors.Split(',').Select(s => s.Trim())];
        return names.Select(name => name.Split(' ', 2)) // Split on first space
            .ToDictionary(parts => parts[0], parts => parts.Length == 2 ? parts[1] : "");
    }

    private void CleanHardwareManifestCollectors() {
        List<string> names = [.. hardwareManifests.Select(x => x.Name)];
        Dictionary<string, string> dict = names.ToDictionary(name => name, name => hardwareManifestCollectorsWithArgs[name]);
        hardwareManifestCollectorsWithArgs.Clear();
        hardwareManifestCollectorsWithArgs = dict;
    }

    public virtual string RunHardwareManifestCollectors() {
        Log.Debug("Gathering data from loaded hardware manifest collectors.");
        HardwareManifestProto.ManifestV2 manifestJson = new();
        foreach (IHardwareManifestPlugin manifest in hardwareManifests) {
            try {
                Log.Debug("  Configuring " + manifest.Name);
                Log.Debug("  Gathering from " + manifest.Name);
                if (manifest.GatherHardwareIdentifiers()) {
                    manifestJson.MergeFrom(manifest.ManifestV2);
                }
            } catch (Exception e) {
                throw new ProvisioningFailureException(ClientExitCodes.HW_COLLECTION_ERROR,
                    $"Hardware manifest collection failed for '{manifest.Name}'. Check the collector and its permissions.", e);
            }
        }
            
        string manifestString = manifestJson.ToString();
        return manifestString;
    }
    #endregion

    #region Ingest paccor data from file
    private void IngestPaccorDataFromFile() {
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.paccor_output_file)])) {
            Log.Debug("Checking location of the paccor output file.");
            string paccorOutputPath = $"{ configFromSettingsFile[nameof(Options.paccor_output_file)] }";
            if (DoesFileExist(paccorOutputPath, out paccorOutputPath)) {
                if (HasHardwareManifestPlugins()) {
                    Log.Warning("The settings file specified hardware manifest collectors and a paccor output file. Fresh data is preferred over data from a file. If you want to use the file data, clear the collectors field from the settings file.");
                } else {
                    Log.Debug("Retrieving components from " + nameof(Options.paccor_output_file) + ".");
                    PaccorOutput = File.ReadAllText(paccorOutputPath);
                    if (string.IsNullOrWhiteSpace(PaccorOutput)) {
                        Log.Warning(nameof(Options.paccor_output_file) + " Paccor output was empty. Cannot perform Platform Attribute validation.");
                    } else {
                        Log.Debug("Output file contains:\n" + PaccorOutput);
                    }
                }
            }
        } else {
            Log.Debug(nameof(Options.paccor_output_file) + " not set in the settings file.");
        }
    }
    #endregion

    #region ACA Address
    private void ParseAcaAddress() {
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.aca_address_port)])) {
            Log.Debug("Parsing the ACA Address.");
            string acaAddressPortStr = $"{ configFromSettingsFile[nameof(Options.aca_address_port)] }";
            if (!string.IsNullOrWhiteSpace(acaAddressPortStr)) {
                AcaAddressPort = new Uri(acaAddressPortStr);
                Log.Debug("  Found " + AcaAddressPort);
            }
        }
        if (!HasAcaAddress()) {
            Log.Error(nameof(Options.aca_address_port) + " not set in the settings file. No HIRS ACA server to talk to. Looking for the format: \"https://<aca_server>:<port>\"");
        }
    }
    #endregion

    #region Auto Detect TPM
    private void CheckAutoDetectTpm() {
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.auto_detect_tpm)])) {
            Log.Debug("Checking Auto Detect TPM setting.");
            string autoDetectTpmStr = $"{ configFromSettingsFile[nameof(Options.auto_detect_tpm)] }";
            try {
                AutoDetectTpm = bool.Parse(autoDetectTpmStr);
                Log.Debug(" Auto Detect TPM is " + (AutoDetectTpm ? "en" : "dis") + "abled.");
            } catch (FormatException) {
                AutoDetectTpm = false;
                Log.Warning(nameof(Options.auto_detect_tpm) + " did not contain a readable true/false setting. Setting to default of false.");
            }
        } else {
            AutoDetectTpm = false;
            Log.Debug(nameof(Options.auto_detect_tpm) + " not set in the settings file. Setting to default of false.");
        }
    }
    #endregion

    #region EK Template
    private void CheckEkTemplate() {
        string ekTemplateStr = configFromSettingsFile[nameof(Options.ek_template)];
        if (string.IsNullOrWhiteSpace(ekTemplateStr)) {
            Log.Debug(nameof(Options.ek_template) + " not set in the settings file. Using " + EkTemplate + ".");
            return;
        }
        // Accepts the C# name (EkL2) or the protobuf name (EK_L2)
        EkTemplate = KeyTemplateCatalog.SupportedEkTemplates
            .FirstOrDefault(template => template.ToString().Equals(ekTemplateStr.Replace("_", "").Trim(),
                StringComparison.OrdinalIgnoreCase));
        if (EkTemplate == EkTemplate.Unspecified) {
            throw new ArgumentException(nameof(Options.ek_template) + " '" + ekTemplateStr + "' is not supported. Use one of: "
                                        + string.Join(", ", KeyTemplateCatalog.SupportedEkTemplates) + ".");
        }
        Log.Debug("  The identity claim will use " + EkTemplate + ".");
    }
    #endregion

    #region
    private void CheckCertificateOutputDirectory() {
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.certificate_output_directory)])) {
            Log.Debug("Checking Certificate Output Directory setting.");
            CertificateOutputDirectory = $"{ configFromSettingsFile[nameof(Options.certificate_output_directory)] }";
            if (!string.IsNullOrWhiteSpace(CertificateOutputDirectory)) {
                // Attempt to write to Certificate Output Directory
                if (!Directory.Exists(CertificateOutputDirectory)) {
                    try {
                        Directory.CreateDirectory(CertificateOutputDirectory);
                        Log.Debug("  Created directory: " + CertificateOutputDirectory);
                    }
                    catch (Exception e) {
                        Log.Debug("  Could not create directory: " + CertificateOutputDirectory);
                        CertificateOutputDirectory = null;
                    }
                }
            }
        }
        if (CertificateOutputDirectory == null) {
            if (HasEfiPrefix()) {
                CertificateOutputDirectory = Path.GetFullPath(Path.Join(EfiPrefix, DEFAULT_CERTIFICATE_OUTPUT_PATH));
                Log.Debug("  Certificate Output Directory not set. Attempting to use EFI Prefix setting.");
                // Attempt to write to default EFI path (fallback)
                if (!Directory.Exists(CertificateOutputDirectory)) {
                    try {
                        Directory.CreateDirectory(CertificateOutputDirectory);
                        Log.Debug("  Created directory: " + CertificateOutputDirectory);
                    }
                    catch (Exception e) {
                        Log.Debug("  Could not create directory: " + CertificateOutputDirectory);
                        CertificateOutputDirectory = null;
                    }
                }
            }
            if (CertificateOutputDirectory == null) {
                // Use current working directory (fallback)
                Log.Warning(nameof(Options.certificate_output_directory) + " not set in the settings file. Defaulting to current working directory.");
                CertificateOutputDirectory = "";
            }
            else {
                Log.Debug("  Will write certificates to " + CertificateOutputDirectory);
            }
        } else {
            Log.Debug("  Will write certificates to " + CertificateOutputDirectory);
        }
    }
    #endregion

    #region EFI
    private void CheckEfiPrefix() {
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.efi_prefix)])) {
            Log.Debug("Checking EFI Prefix setting.");
            EfiPrefix = $"{ configFromSettingsFile[nameof(Options.efi_prefix)] }";
            if (string.IsNullOrWhiteSpace(EfiPrefix)) { // If not explicitly set in appsettings, try to use default EFI location on Linux
                if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux)) {
                    EfiPrefix = EFI_ARTIFACT_LINUX_PREFIX + EFI_ARTIFACT_PATH;
                }
            } else {
                if (!Directory.Exists(EfiPrefix)) {
                    Log.Debug(nameof(Options.efi_prefix) + ": " + EfiPrefix + " did not exist.");
                    EfiPrefix = null;
                }
            }
        }
        if (EfiPrefix == null) {
            Log.Warning(nameof(Options.efi_prefix) + " not set in the settings file. Will not attempt to scan for artifacts in EFI.");
        } else {
            Log.Debug("  Will scan for artifacts in " + EfiPrefix);
        }
    }
        
    public virtual List<byte[]> GatherPlatformCertificatesFromEfi() {
        // According to FIM: EFIPREFIX/boot/tcg/{cert,pccert,platform}
        List<byte[]> platformCerts = null;
        if (!string.IsNullOrWhiteSpace(EfiPrefix)) {
            EnumerationOptions enumOpts = new() {
                MatchCasing = MatchCasing.CaseInsensitive,
                RecurseSubdirectories = true
            };
            List<FileInfo> files = [];
            string[] paths = ["cert", "pccert", "platform"];
            foreach (string subdir in paths) {
                string path = EfiPrefix + EFI_ARTIFACT_PATH + subdir;
                if (Directory.Exists(path)) {
                    files.AddRange(new DirectoryInfo(path).GetFiles("*base*", enumOpts));
                    files.AddRange(new DirectoryInfo(path).GetFiles("*delta*", enumOpts));
                } else {
                    path = EfiPrefix + EFI_ARTIFACT_PATH_COMPAT + subdir;
                    if (Directory.Exists(path)) {
                        files.AddRange(new DirectoryInfo(path).GetFiles("*base*", enumOpts));
                        files.AddRange(new DirectoryInfo(path).GetFiles("*delta*", enumOpts));
                    }
                }
            }
            if (files.Count > 0) { // if none found, don't initialize platformCerts
                // At least one base platform cert found
                platformCerts = [];
                foreach (FileInfo file in files) {
                    platformCerts.Add(File.ReadAllBytes(file.FullName));
                    Log.Debug("gatherPlatformCertificatesFromEFI: Gathering " + file.FullName);
                }
            }
        } else {
            Log.Warning("gatherPlatformCertificatesFromEFI was called without verifying HasEfiPrefix.");
        }
        Log.Debug("Found " + (platformCerts?.Count ?? 0) + " platform certs.");
        return platformCerts;
    }

    public virtual List<byte[]> GatherRimBasesFromEfi() {
        // According to PC Client RIM
        List<byte[]> baseRims = null;

        // /boot/tcg/manifest/swidtag	Base RIM Files
        //      <name of the tag creator> + <product name> + <RIM version>.swidtag
        if (!string.IsNullOrWhiteSpace(EfiPrefix)) {
            EnumerationOptions enumOpts = new() {
                MatchCasing = MatchCasing.CaseInsensitive,
                RecurseSubdirectories = true
            };
            List<FileInfo> files = [];

            string[] paths = ["manifest", "swidtag"];
            const string ext = "*swidtag";
            foreach (string subdir in paths) {
                string path = EfiPrefix + EFI_ARTIFACT_PATH + subdir;
                if (Directory.Exists(path)) {
                    files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                } else {
                    path = EfiPrefix + EFI_ARTIFACT_PATH_COMPAT + subdir;
                    if (Directory.Exists(path)) {
                        files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                    }
                }
            }
            if (files.Count != 0) { // if none found, don't initialize baseRims
                // At least one base platform cert found
                baseRims = [];
                foreach (FileInfo file in files) {
                    baseRims.Add(File.ReadAllBytes(file.FullName));
                    Log.Debug("gatherRIMBasesFromEFI: Gathering " + file.FullName);
                }
            }
        } else {
            Log.Warning("gatherRIMBasesFromEFI was called without verifying HasEfiPrefix.");
        }
        Log.Debug("Found " + (baseRims?.Count ?? 0) + " base RIMs.");
        return baseRims;
    }

    public virtual List<byte[]> GatherSupportRimeLsFromEfi() {
        // According to PC Client RIM
        List<byte[]> supportRimELs = null;
        // /boot/tcg/manifest/rim Support RIM Files
        //      <name of the tag creator> + <product name> + <product version>.rimel
        if (!string.IsNullOrWhiteSpace(EfiPrefix)) {
            EnumerationOptions enumOpts = new() {
                MatchCasing = MatchCasing.CaseInsensitive,
                RecurseSubdirectories = true
            };
            List<FileInfo> files = [];

            string[] paths = ["manifest", "rim"];
            const string ext = "*rimel";
            foreach (string subdir in paths) {
                string path = EfiPrefix + EFI_ARTIFACT_PATH + subdir;
                if (Directory.Exists(path)) {
                    files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                } else {
                    path = EfiPrefix + EFI_ARTIFACT_PATH_COMPAT + subdir;
                    if (Directory.Exists(path)) {
                        files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                    }
                }
            }
            if (files.Count != 0) { // if none found, don't initialize baseRims
                // At least one base platform cert found
                supportRimELs = [];
                foreach (FileInfo file in files) {
                    supportRimELs.Add(File.ReadAllBytes(file.FullName));
                    Log.Debug("gatherSupportRIMELsFromEFI: Gathering " + file.FullName);
                }
            }
        } else {
            Log.Warning("gatherSupportRIMELsFromEFI was called without verifying HasEfiPrefix.");
        }
        Log.Debug("Found " + (supportRimELs?.Count ?? 0) + " support rimel files.");
        return supportRimELs;
    }

    public virtual List<byte[]> GatherSupportRimpcRsFromEfi() {
        // According to PC Client RIM
        List<byte[]> supportRimPcrs = null;
        // /boot/tcg/manifest/rim Support RIM Files
        //      <name of the tag creator> + <product name> + <product version>.rimpcr
        if (!string.IsNullOrWhiteSpace(EfiPrefix)) {
            EnumerationOptions enumOpts = new() {
                MatchCasing = MatchCasing.CaseInsensitive,
                RecurseSubdirectories = true
            };
            List<FileInfo> files = [];

            string[] paths = ["manifest", "rim"];
            const string ext = "*rimpcr";
            foreach (string subdir in paths) {
                string path = EfiPrefix + EFI_ARTIFACT_PATH + subdir;
                if (Directory.Exists(path)) {
                    files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                } else {
                    path = EfiPrefix + EFI_ARTIFACT_PATH_COMPAT + subdir;
                    if (Directory.Exists(path)) {
                        files.AddRange(new DirectoryInfo(path).GetFiles(ext, enumOpts));
                    }
                }
            }
            if (files.Count != 0) { // if none found, don't initialize baseRims
                // At least one base platform cert found
                supportRimPcrs = [];
                foreach (FileInfo file in files) {
                    supportRimPcrs.Add(File.ReadAllBytes(file.FullName));
                    Log.Debug("gatherSupportRIMPCRsFromEFI: Gathering " + file.FullName);
                }
            }
        } else {
            Log.Warning("gatherSupportRIMPCRsFromEFI was called without verifying HasEfiPrefix.");
        }
        Log.Debug("Found " + (supportRimPcrs?.Count ?? 0) + " support rimpcr files.");
        return supportRimPcrs;
    }
    #endregion

    #region Ingest Event Log from File
    private void IngestEventLogFromFile() {
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.event_log_file)])) {
            Log.Debug("Checking location of the event log.");
            string eventLogPath = $"{ configFromSettingsFile[nameof(Options.event_log_file)] }";
            if (DoesFileExist(eventLogPath, out eventLogPath)) {
                Log.Debug("Retrieving the Event Log. ");
                EventLog = File.ReadAllBytes(eventLogPath);
                if (EventLog.Length == 0) {
                    Log.Warning(nameof(Options.event_log_file) + " The event log was empty.");
                }
            }
        } else {
            Log.Debug(nameof(Options.event_log_file) + " not set in the settings file.");
        }

    }
    #endregion

    #region Store Custom Device Info Collector Options
    private void StoreCustomDeviceInfoCollectorOptions() {
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.linux_bios_vendor_file)])) {
            Log.Debug("Custom bios vendor file specified for the Device Info Collector on Linux.");
            string path = $"{ configFromSettingsFile[nameof(Options.linux_bios_vendor_file)] }";
            LinuxBiosVendor = ReadFileText(path);
        }
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.linux_bios_version_file)])) {
            Log.Debug("Custom bios version file specified for the Device Info Collector on Linux.");
            string path = $"{ configFromSettingsFile[nameof(Options.linux_bios_version_file)] }";
            LinuxBiosVersion = ReadFileText(path);
        }
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.linux_sys_vendor_file)])) {
            Log.Debug("Custom hardware manufacturer file specified for the Device Info Collector on Linux.");
            string path = $"{ configFromSettingsFile[nameof(Options.linux_sys_vendor_file)] }";
            LinuxSysVendor = ReadFileText(path);
        }
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.linux_product_name_file)])) {
            Log.Debug("Custom hardware product name file specified for the Device Info Collector on Linux.");
            string path = $"{ configFromSettingsFile[nameof(Options.linux_product_name_file)] }";
            LinuxProductName = ReadFileText(path);
        }
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.linux_bios_date_file)])) {
            Log.Debug("Custom hardware bios date file specified for the Device Info Collector on Linux.");
            string path = $"{ configFromSettingsFile[nameof(Options.linux_bios_date_file)] }";
            LinuxBiosDate = ReadFileText(path);
        }
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.linux_product_version_file)])) {
            Log.Debug("Custom hardware product version file specified for the Device Info Collector on Linux.");
            string path = $"{ configFromSettingsFile[nameof(Options.linux_product_version_file)] }";
            LinuxProductVersion = ReadFileText(path);
        }
        if (!string.IsNullOrWhiteSpace(configFromSettingsFile[nameof(Options.linux_product_serial_file)])) {
            Log.Debug("Custom hardware product serial file specified for the Device Info Collector on Linux.");
            string path = $"{ configFromSettingsFile[nameof(Options.linux_product_serial_file)] }";
            LinuxProductSerial = ReadFileText(path);
        }
    }
    #endregion
    public static bool DoesFileExist(string path, out string outFullPath) {
        bool found = false;
        outFullPath = "";
        if (!string.IsNullOrWhiteSpace(path)) {
            outFullPath = Path.GetFullPath(path);
            found = File.Exists(outFullPath);
            if (!found) {
                Log.Debug("  File identified in settings did not exist: " + outFullPath);
            } else {
                Log.Debug("  File exists: " + outFullPath);
            }
        }
        return found;
    }

    public static string ReadFileText(string path) {
        string text = "";
        if (DoesFileExist(path, out string fullPath)) {
            Log.Debug("  Reading file: " + fullPath + ".");
            text = File.ReadAllText(fullPath);
            Log.Debug("   " + (string.IsNullOrWhiteSpace(text) ? "File was empty." : text));
        }
        return text;
    }

    public bool HasHardwareManifestPlugins() {
        return hardwareManifests.Count > 0;
    }
    public bool HasPaccorOutputFromFile() {
        return !string.IsNullOrEmpty(PaccorOutput);
    }
    public bool HasAcaAddress() {
        return AcaAddressPort != null;
    }
    public bool HasEfiPrefix() {
        return !string.IsNullOrEmpty(EfiPrefix);
    }
    public bool IsAutoDetectTpmEnabled() {
        return AutoDetectTpm;
    }
    public bool HasEventLogFromFile() {
        return EventLog is { Length: > 0 };
    }
}