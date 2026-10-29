using Google.Protobuf;
using Hirs.Pb;
using Serilog;
using System.Runtime.InteropServices;
using System.Text;

namespace hirs;

public class Provisioner(Settings settings, CLI cli) : IHirsProvisioner {
    private CLI Cli { get; } = cli;

    private Settings Settings { get; } = settings;

    private IHirsDeviceInfoCollector? DeviceInfoCollector {
        get;
        set;
    }

    private IHirsAcaClient? AcaClient {
        get;
        set;
    }

    private const string DefaultLDevIdPubKeyFileName = "ldevid.pub";
    private const string DefaultLDevIdPrivKeyFileName = "ldevid.priv";

    private const string DefaultAkCertFileName = "ak.pem";

    // One pair of AK files per template, so changing ak_template creates a new AK rather than reusing another kind
    private static string AkKeyFileName(AsymmetricKeyTemplate template, string extension) {
        return "ak_" + template.ToString().ToLowerInvariant() + extension;
    }
    private const string DefaultLDevIdCertFileName = "ldevid.pem";

    public IHirsAcaTpm ConnectTpm() {
        IHirsAcaTpm tpm = null;
        // If tpm device type is set on the command line
        if (Cli.Nix) {
            tpm = new CommandTpm(CommandTpm.Devices.NIX);
        } else if (Cli.Tcp && !String.IsNullOrWhiteSpace(Cli.Ip)) {
            string[] split = Cli.Ip.Split(":");
            if (split.Length == 2) {
                tpm = new CommandTpm(Cli.Sim, split[0], Int32.Parse(split[1]));
                Log.Debug("Connected to TPM via TCP at " + Cli.Ip);
            } else {
                Log.Error("ip input should have the format servername:port. The given input was '" + Cli.Ip + "'.");
            }
        } else if (Cli.Win) {
            tpm = new CommandTpm(CommandTpm.Devices.WIN);
        }

        // If command line not set, check if autodetect is enabled
        if ((tpm == null) && Settings.IsAutoDetectTpmEnabled()) {
            Log.Debug("Auto Detect TPM is Enabled. Starting search for the TPM.");
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) {
                try {
                    tpm = new CommandTpm(CommandTpm.Devices.WIN);
                    Log.Debug("Auto Detect found a WIN TPM Device.");
                } catch (Exception) {
                    Log.Debug("No WIN TPM Device found by auto detect.");
                }
            } else if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux)) {
                try {
                    tpm = new CommandTpm(CommandTpm.Devices.NIX);
                    Log.Debug("Auto Detect found a Linux TPM Device.");
                } catch (Exception) {
                    Log.Debug("No Linux TPM Device found by auto detect.");
                }
            }

            // if tpm still null, try to set up TcpTpmDevice on sim, catch exception
            if (tpm == null) {
                try {
                    string[] split = CommandTpm.DefaultSimulatorNamePort.Split(":");
                    tpm = new CommandTpm(true, split[0], Int32.Parse(split[1]));
                    Log.Debug("Auto Detect found a TPM simulator at " + CommandTpm.DefaultSimulatorNamePort + ".");
                } catch (Exception) {
                    Log.Debug("No TPM simulator found by auto detect.");
                }
            }
        } else if ((tpm != null) && Settings.IsAutoDetectTpmEnabled()) {
            Log.Debug(
                "Auto detect TPM was enabled in settings, but command line options were also given. Using command line options.");
        }

        // If TPM is still not set up, offer help message
        if (tpm == null) {
            Log.Fatal(
                "To connect to a TPM device on Windows, add the command line argument --win\n" +
                "To connect to a TPM device on LINUX, add the command line argument --nix\n" +
                "To connect to a TPM via TCP, add the command line arguments --tcp <address>:<port>\n" +
                "To connect to a TPM simulator at the default TCP socket of " +
                CommandTpm.DefaultSimulatorNamePort + ", add the command line arguments --tcp --sim\n" +
                "To connect to a TPM simulator at any other socket, add the command line arguments --tcp --sim <address>:<port>\n");
        }

        return tpm;
    }

    private void UseBuiltInClient(string addr) {
        AcaClient = new Client(addr);
    }

    public void SetClient(IHirsAcaClient client) {
        AcaClient = client;
    }

    public void UseClassicDeviceInfoCollector() {
        DeviceInfoCollector = new ClassicDeviceInfoCollector(Settings);
    }

    public void SetDeviceInfoCollector(IHirsDeviceInfoCollector? collector) {
        if (collector is null) {
            UseClassicDeviceInfoCollector();
        } else {
            DeviceInfoCollector = collector;
        }
    }

    private static string FormatCertificatePath(DeviceInfo dv, string certificateDirPath,
        string certificateFileName) {
        StringBuilder sb = new();
        if (dv.Hw != null) {
            if (dv.Hw.HasSystemSerialNumber &&
                !dv.Hw.SystemSerialNumber.Equals(ClassicDeviceInfoCollector.NOT_SPECIFIED)) {
                sb.Append($"{dv.Hw.SystemSerialNumber}-");
            }

            if (dv.Hw.HasManufacturer && !dv.Hw.Manufacturer.Equals(ClassicDeviceInfoCollector.NOT_SPECIFIED)) {
                sb.Append($"{dv.Hw.Manufacturer}-");
            }
        }

        sb.Append(certificateFileName);
        return Path.GetFullPath(Path.Join(certificateDirPath, sb.ToString()));
    }

    private void SaveCertificate(DeviceInfo dv, string fileName, string certificate, string description) {
        string? certificateDirPath = Settings.CertificateOutputDirectory;
        if (certificateDirPath == null) {
            return;
        }
        string certificateFilePath = FormatCertificatePath(dv, certificateDirPath, fileName);
        try {
            File.WriteAllText(certificateFilePath, certificate);
            Log.Debug("{Description} written to: {Path}", description, certificateFilePath);
        } catch (Exception) {
            Log.Debug("Failed to write {Description} to local file system.", description);
        }
    }

    private static byte[] GetMatchingEkCertificate(IHirsAcaTpm tpm, EndorsementKey ek) {
        uint? nvIndex = KeyTemplateCatalog.GetEkCertificateNvIndex(ek.Template);
        if (nvIndex is not { } index) {
            return [];
        }
        byte[] certificate = tpm.GetCertificateFromNvIndex(index);
        if (certificate is null or []) {
            Log.Information("------> No Endorsement Key Certificate found at 0x{Index:X} for {Template}. " +
                            "The ACA may have one uploaded for this TPM.", index, ek.Template);
            return [];
        }
        if (!EkCertificateMatcher.Matches(certificate, ek.Public)) {
            Log.Warning("The certificate at 0x{Index:X} does not certify the {Template} EK; it will not be sent.",
                index, ek.Template);
            return [];
        }
        return certificate;
    }

    private static byte[] ActivateCredential(ProvisioningKey akKey, ProvisioningKey ekKey,
        Func<uint, uint, byte[]> activate) {
        using KeyLease ak = akKey.Acquire();
        using KeyLease ek = ekKey.Acquire();
        return activate(ak.Handle, ek.Handle);
    }

    private static readonly Tpm2Lib.TpmAlgId[] DefaultPcrBanks = [Tpm2Lib.TpmAlgId.Sha1, Tpm2Lib.TpmAlgId.Sha256];

    /**
     * The PCR values of every SHA bank the TPM allocates, each under its bank's name (sha1, sha256, sha384, sha512).
     */
    private static string FormatPcrBanksForAca(IHirsAcaTpm tpm) {
        IReadOnlyList<Tpm2Lib.TpmAlgId> banks = tpm.GetPcrBanks();
        StringBuilder pcrsList = new();
        foreach (Tpm2Lib.TpmAlgId bank in banks.Count > 0 ? banks : DefaultPcrBanks) {
            CommandTpm.FormatPcrValuesForAca(tpm.GetPcrList(bank), PcrBankName(bank), out string bankValues);
            pcrsList.Append(bankValues);
        }
        return pcrsList.ToString();
    }

    private static string PcrBankName(Tpm2Lib.TpmAlgId bank) {
        return bank switch {
            Tpm2Lib.TpmAlgId.Sha1 => "sha1",
            Tpm2Lib.TpmAlgId.Sha256 => "sha256",
            Tpm2Lib.TpmAlgId.Sha384 => "sha384",
            Tpm2Lib.TpmAlgId.Sha512 => "sha512",
            _ => bank.ToString().ToLowerInvariant()
        };
    }

    public async Task<int> Provision(IHirsAcaTpm? tpm) {
        ClientExitCodes result = ClientExitCodes.SUCCESS;
        if (tpm != null && tpm.IsTpmPresent()) {
            Log.Information("--> Provisioning");
            ProvisioningKeys keys = new();
            EkTemplate ekTemplate = Settings.EkTemplate;
            ProvisioningKey? claimEkKey = null;
            byte[] ekPublicArea = [];
            byte[] ekc = [];
            if (Cli.ReplaceAK && tpm.EvictLegacyAttestationKeys() > 0) {
                Log.Information("----> Removed the Attestation Key that a previous provisioner version persisted.");
            }
            Log.Information("----> Gathering Endorsement Keys and Certificates.");
            foreach (EkTemplate template in KeyTemplateCatalog.SupportedEkTemplates) {
                if (template != ekTemplate && !KeyTemplateCatalog.IsLowRange(template) && !tpm.HasEndorsementKey(template)) {
                    Log.Debug("The TPM does not have EK {Template}; not creating it.", template);
                    continue;
                }
                EndorsementKey? ek = null;
                try {
                    ek = tpm.AcquireEndorsementKey(template);
                } catch (Exception e) when (template != ekTemplate) {
                    Log.Debug(e, "EK {Template} was not available; continuing without it.", template);
                }
                if (ek == null) {
                    Log.Debug("No EK available for template {Template}.", template);
                    continue;
                }

                // Released right away: an EK is loaded again only for the operation that uses it
                using (ek) {
                    byte[] publicArea = ek.PublicArea;
                    byte[] ekCertificate = GetMatchingEkCertificate(tpm, ek);
                    ProvisioningKey ekKey = keys.Add(KeyTemplateCatalog.BuildEkCandidate(publicArea, ek.Public,
                            ek.IsPersistent ? ek.Handle : null, ekCertificate),
                        () => KeyLease.ForEndorsementKey(tpm, template, publicArea));
                    if (template == ekTemplate) {
                        claimEkKey = ekKey;
                        ekPublicArea = publicArea;
                        ekc = ekCertificate;
                    }
                }
            }
            if (claimEkKey == null) {
                Log.Error("Could not obtain the {Template} Endorsement Key set by ek_template in the settings file.", ekTemplate);
                return (int)ClientExitCodes.TPM_ERROR;
            }

            List<byte[]> pcs = null, baseRims = null, supportRimELs = null, supportRimPCRs = null;
            if (Settings.HasEfiPrefix()) {
                Log.Information("----> Gathering artifacts from EFI.");
                pcs = Settings.GatherPlatformCertificatesFromEfi();
                baseRims = Settings.GatherRimBasesFromEfi();
                supportRimELs = Settings.GatherSupportRimeLsFromEfi();
                supportRimPCRs = Settings.GatherSupportRimpcRsFromEfi();
            }

            Log.Debug("Setting up the Client.");
            Uri acaAddress = Settings.AcaAddressPort;
            if (AcaClient == null) {
                UseBuiltInClient(acaAddress.AbsoluteUri);
            }

            Log.Information("----> Collecting device information.");
            DeviceInfo dv;
            try {
                dv = DeviceInfoCollector.CollectDeviceInfo(acaAddress.AbsoluteUri);
            } catch (Exception e) {
                throw new ProvisioningFailureException(ClientExitCodes.HW_COLLECTION_ERROR,
                    "Device information collection failed. Check the system information sources and permissions.",
                    e);
            }

            if (baseRims != null) {
                foreach (byte[] baseRim in baseRims) {
                    dv.Swidfile.Add(ByteString.CopyFrom(baseRim));
                }
            }

            if (supportRimELs != null) {
                foreach (byte[] supportRimEL in supportRimELs) {
                    dv.Logfile.Add(ByteString.CopyFrom(supportRimEL));
                }
            }

            if (supportRimPCRs != null) {
                foreach (byte[] supportRimPCR in supportRimPCRs) {
                    dv.Logfile.Add(ByteString.CopyFrom(supportRimPCR));
                }
            }

            Log.Debug("Gathering hardware component information:");
            string manifest = "";
            if (Settings.HasHardwareManifestPlugins()) {
                manifest = Settings.RunHardwareManifestCollectors();
            } else if (Settings.HasPaccorOutputFromFile()) {
                manifest = Settings.PaccorOutput;
            } else {
                Log.Warning("No hardware collectors nor paccor output file were identified.");
            }

            Log.Debug("Hardware component information that will be sent to the ACA: " + manifest);

            Log.Debug("Gathering the event log.");
            byte[] eventLog;
            if (Settings.HasEventLogFromFile()) {
                Log.Debug("  Using the event log identified in settings.");
                eventLog = Settings.EventLog;
            } else {
                Log.Debug("  Attempting to collect the event log from the system.");
                eventLog = tpm.GetEventLog();
            }

            if (eventLog is not null or []) {
                Log.Debug("Event log gathered is " + eventLog.Length + " bytes.");
                dv.Livelog = ByteString.CopyFrom(eventLog);
            }

            Log.Debug("Gathering PCR data from the TPM.");
            string pcrsList = FormatPcrBanksForAca(tpm);
            Log.Debug("Result of formatting pcr values for the ACA:");
            Log.Debug("\n" + pcrsList);
            dv.Pcrslist = ByteString.CopyFromUtf8(pcrsList);

            Log.Information("----> " + (Cli.ReplaceAK ? "Creating new" : "Verifying existence of") + " Attestation Key.");
            // Saved as files under the SRK, like the LDevID (TPM 2.0 Keys for Device Identity and Attestation, 6.6).
            string akPubPath = FormatCertificatePath(dv, Settings.CertificateOutputDirectory,
                AkKeyFileName(Settings.AkTemplate, ".pub"));
            string akPrivPath = FormatCertificatePath(dv, Settings.CertificateOutputDirectory,
                AkKeyFileName(Settings.AkTemplate, ".priv"));
            tpm.CreateAttestationKey(akPubPath, akPrivPath, Settings.AkTemplate, Cli.ReplaceAK);

            Log.Debug("Gathering AK PUBLIC.");
            byte[] akPublicArea = tpm.ReadPublicAreaFromFile(akPubPath);
            Tpm2Lib.TpmPublic akPub = CommandTpm.Marshal<Tpm2Lib.TpmPublic>(akPublicArea);
            ProvisioningKey akKey = keys.Add(KeyTemplateCatalog.BuildAsymmetricCandidate(akPublicArea, akPub,
                    null, KeyRole.Attestation, ProvisioningOrigin.Local),
                () => KeyLease.ForKeyFiles(tpm, akPubPath, akPrivPath, akPublicArea, "the AK"));

            Log.Information("----> " + (Cli.ReplaceLDevID ? "Creating new" : "Verifying existence of") +
                            " LDevID Key.");
            string ldevidPubPath = FormatCertificatePath(dv, Settings.CertificateOutputDirectory,
                DefaultLDevIdPubKeyFileName);
            string ldevidPrivPath = FormatCertificatePath(dv, Settings.CertificateOutputDirectory,
                DefaultLDevIdPrivKeyFileName);
            tpm.CreateLDevIDKey(ldevidPubPath, ldevidPrivPath, Cli.ReplaceLDevID);

            Log.Debug("Gathering LDevID PUBLIC.");
            byte[] ldevidPublicArea = tpm.ReadPublicAreaFromFile(ldevidPubPath);
            if (CommandTpm.CanMarshal<Tpm2Lib.TpmPublic>(ldevidPublicArea)) {
                Tpm2Lib.TpmPublic ldevidPub = CommandTpm.Marshal<Tpm2Lib.TpmPublic>(ldevidPublicArea);
                keys.Add(KeyTemplateCatalog.BuildAsymmetricCandidate(ldevidPublicArea, ldevidPub,
                        null, KeyRole.DeviceIdentity, ProvisioningOrigin.Local),
                    () => KeyLease.ForKeyFiles(tpm, ldevidPubPath, ldevidPrivPath, ldevidPublicArea, "the LDevID"));
            }

            Log.Debug("Create identity claim");
            IdentityClaim idClaim = AcaClient.CreateIdentityClaim(dv, akPublicArea, ekPublicArea, ekc, pcs,
                manifest, ldevidPublicArea, keys.Candidates.ToList());

            Log.Information("----> Sending identity claim to Attestation CA");
            IdentityClaimResponse icr = await AcaClient.PostIdentityClaim(idClaim);
            if (icr == null) {
                throw new AcaClientException("The ACA client did not return an identity-claim response.");
            }

            Log.Information("----> Received response. Attempting to decrypt nonce");
            if (icr.HasStatus) {
                if (icr.Status == ResponseStatus.Pass) {
                    Log.Debug("The ACA accepted the identity claim.");
                } else {
                    Log.Debug("The ACA did not accept the identity claim. See details on the ACA.");
                    if (icr.HasStatusDetails && !icr.StatusDetails.IsWhiteSpace()) {
                        Log.Error("Validation failed during identity-claim processing: {StatusDetails}",
                            icr.StatusDetails);
                    } else {
                        Log.Error(
                            "Validation failed during identity-claim processing. The ACA did not provide additional details.");
                    }

                    result = ClientExitCodes.PASS_1_STATUS_FAIL;
                    return (int)result;
                }
            }

            if (!icr.HasCredentialBlob) {
                Log.Error("The response from the ACA did not contain a CredentialBlob.");
                return (int)ClientExitCodes.MAKE_CREDENTIAL_BLOB_MALFORMED;
            }

            byte[] credentialBlob = icr.CredentialBlob.ToByteArray();
            Log.Debug("ACA delivered IdentityClaimResponse credentialBlob " +
                      BitConverter.ToString(credentialBlob));

            byte[] recoveredSecret;
            if (icr.HasEncryptedSecret) {
                byte[] encryptedSecret = icr.EncryptedSecret.ToByteArray(); // TPM2B_ENCRYPTED_SECRET
                Log.Debug("ACA delivered IdentityClaimResponse encryptedSecret " +
                          BitConverter.ToString(encryptedSecret));
                Log.Debug("Executing activateCredential.");
                recoveredSecret = ActivateCredential(akKey, claimEkKey, (ak, ek) =>
                    tpm.ActivateCredential(ak, ek, credentialBlob, encryptedSecret)); // credentialBlob is a TPM2B_ID_OBJECT
            } else {
                // Attempt compatibility fallback
                Log.Debug("No separate encrypted secret; reading the combined credential blob for compatibility with legacy ACA data.");
                if (!LegacyCredentialBlob.TrySplit(credentialBlob, out byte[] integrityHmac, out byte[] encIdentity,
                        out byte[] encryptedSecret, out string error)) {
                    Log.Error(error);
                    return (int)ClientExitCodes.MAKE_CREDENTIAL_BLOB_MALFORMED;
                }
                Log.Debug("Executing activateCredential.");
                recoveredSecret = ActivateCredential(akKey, claimEkKey, (ak, ek) =>
                    tpm.ActivateCredential(ak, ek, integrityHmac, encIdentity, encryptedSecret));
            }

            if (!recoveredSecret.Any()) {
                Log.Debug("Nonce could not be decrypted. ActivateCredential failed.");
                return (int)ClientExitCodes.PASS_1_STATUS_FAIL;
            }
                
            Log.Information("----> Nonce successfully decrypted. Sending attestation certificate request");
                
            uint[] selectPcrs = null;
            if (icr.HasPcrMask) {
                // For now, the ACA will send a comma separated selection of PCRs as a string
                try {
                    selectPcrs = [.. icr.PcrMask.Split(',').Select(uint.Parse)];
                } catch (Exception) {
                    Log.Warning("PcrMask was included in the IdentityClaimResponse, but could not be parsed." +
                                "Collecting quote over default PCR selection.");
                    Log.Debug("This PcrMask could not be parsed: " + icr.PcrMask);
                }
            }

            Log.Debug("Gathering quote.");
            CommandTpmQuoteResponse ctqr;
            using (KeyLease ak = akKey.Acquire()) {
                tpm.GetQuote(ak.Handle, Tpm2Lib.TpmAlgId.Sha256, recoveredSecret, out ctqr, selectPcrs);
            }
                
            CertificateRequest akCertReq = AcaClient.CreateAkCertificateRequest(recoveredSecret, ctqr);
            string certificate;
            Log.Debug("Communicate certificate request to the ACA.");
            CertificateResponse cr = await AcaClient.PostCertificateRequest(akCertReq);
            if (cr == null) {
                throw new AcaClientException("The ACA client did not return a certificate response.");
            }

            Log.Debug("Response received from the ACA regarding the certificate request.");
            if (cr.HasStatus) {
                if (cr.Status == ResponseStatus.Pass) {
                    Log.Debug("ACA returned a positive response to the Certificate Request.");
                } else {
                    Log.Debug("The ACA did not return any certificates. See details on the ACA.");
                    if (cr.HasStatusDetails && !cr.StatusDetails.IsWhiteSpace()) {
                        Log.Error("Validation failed during certificate processing: {StatusDetails}",
                            cr.StatusDetails);
                    } else {
                        Log.Error(
                            "Validation failed during certificate processing. The ACA did not provide additional details.");
                    }

                    result = ClientExitCodes.PASS_2_STATUS_FAIL;
                    return (int)result;
                }
            }

            if (cr.HasCertificate) {
                certificate = cr.Certificate; // contains certificate
                SaveCertificate(dv, DefaultAkCertFileName, certificate, "Attestation key certificate");
                Log.Debug("Printing attestation key certificate: " + certificate);
            }

            if (cr.HasLdevidCertificate) {
                certificate = cr.LdevidCertificate; // contains certificate
                SaveCertificate(dv, DefaultLDevIdCertFileName, certificate, "LDevID certificate");
                Log.Debug("Printing LDevID certificate: " + certificate);
            }
        } else {
            result = ClientExitCodes.TPM_ERROR;
            Log.Error("Could not provision because the TPM object was null.");
        }

        return (int)result;
    }
}