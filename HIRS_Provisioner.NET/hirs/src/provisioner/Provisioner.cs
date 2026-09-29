using Google.Protobuf;
using Hirs.Pb;
using Serilog;
using System.Runtime.InteropServices;
using System.Text;

namespace hirs {
    public class Provisioner : IHirsProvisioner {
        private CLI Cli { get; }

        private Settings Settings { get; }

        private IHirsDeviceInfoCollector? deviceInfoCollector {
            get;
            set;
        } = null;

        private IHirsAcaClient? acaClient {
            get;
            set;
        } = null;

        private const string DefaultLDevIDPubKeyFileName = "ldevid.pub";
        private const string DefaultLDevIDPrivKeyFileName = "ldevid.priv";

        private const string DefaultAKCertFileName = "ak.pem";
        private const string DefaultLDevIDCertFileName = "ldevid.pem";

        public Provisioner(Settings settings, CLI cli) {
            Settings = settings;
            Cli = cli;
        }

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
            acaClient = new Client(addr);
        }

        public void SetClient(IHirsAcaClient client) {
            acaClient = client;
        }

        public void UseClassicDeviceInfoCollector() {
            deviceInfoCollector = new ClassicDeviceInfoCollector(Settings);
        }

        public void SetDeviceInfoCollector(IHirsDeviceInfoCollector? collector) {
            if (collector is null) {
                UseClassicDeviceInfoCollector();
            } else {
                deviceInfoCollector = collector;
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

        private static byte[] GetMatchingEkCertificate(IHirsAcaTpm tpm, EndorsementKey ek) {
            uint? nvIndex = KeyTemplateCatalog.GetEkCertificateNvIndex(ek.Template);
            if (nvIndex is not uint index) {
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

        public async Task<int> Provision(IHirsAcaTpm? tpm) {
            // EKs regenerated into transient handles stay loaded for the whole provisioning run.
            List<EndorsementKey> ekLeases = new();
            try {
                return await Provision(tpm, ekLeases);
            } finally {
                foreach (EndorsementKey ek in ekLeases) {
                    ek.Dispose();
                }
            }
        }

        private async Task<int> Provision(IHirsAcaTpm? tpm, List<EndorsementKey> ekLeases) {
            ClientExitCodes result = ClientExitCodes.SUCCESS;
            if (tpm != null && tpm.IsTpmPresent()) {
                Log.Information("--> Provisioning");
                // One key candidate per EK template this provisioner has. An EK is identified by its
                // public area and the public key of its certificate, not by a handle. The certificate
                // for each EK is read from the NV index defined for its template and is only sent when
                // it certifies that EK.
                List<KeyCandidate> keyCandidates = new();
                EndorsementKey? l1Ek = null;
                byte[] ekc = [];
                Log.Information("----> Gathering Endorsement Keys and Certificates.");
                foreach (EkTemplate template in KeyTemplateCatalog.SupportedEkTemplates) {
                    EndorsementKey? ek = null;
                    try {
                        ek = tpm.AcquireEndorsementKey(template);
                    } catch (Exception e) when (template != EkTemplate.EkL1) {
                        // Only the L-1 EK is required (legacy protocol, AK parent).
                        Log.Debug(e, "EK {Template} was not available; continuing without it.", template);
                    }
                    if (ek == null) {
                        Log.Debug("No EK available for template {Template}.", template);
                        continue;
                    }
                    ekLeases.Add(ek);

                    byte[] ekCertificate = GetMatchingEkCertificate(tpm, ek);
                    keyCandidates.Add(KeyTemplateCatalog.BuildEkCandidate(ek.PublicArea, ek.Public,
                        ek.IsPersistent ? (uint?)ek.Handle : null, ekCertificate));
                    if (template == EkTemplate.EkL1) {
                        l1Ek = ek;
                        ekc = ekCertificate;
                    }
                }
                if (l1Ek == null) {
                    Log.Error("Could not obtain the L-1 Endorsement Key from the TPM.");
                    return (int)ClientExitCodes.TPM_ERROR;
                }
                byte[] ekPublicArea = l1Ek.PublicArea;

                Log.Information("----> " + (Cli.ReplaceAK ? "Creating new" : "Verifying existence of") + " Attestation Key.");
                tpm.CreateAttestationKey(l1Ek.Handle, CommandTpm.DefaultAkHandle, Cli.ReplaceAK);

                Log.Debug("Gathering AK PUBLIC.");
                Tpm2Lib.TpmPublic akPub = tpm.ReadPublicArea(CommandTpm.DefaultAkHandle, out byte[] _, out byte[] _);
                byte[] akPublicArea = akPub;
                keyCandidates.Add(KeyTemplateCatalog.BuildAsymmetricCandidate(akPublicArea, akPub,
                    CommandTpm.DefaultAkHandle, KeyRole.Attestation, ProvisioningOrigin.Local));

                Log.Debug("Checking SRK PUBLIC");
                tpm.CreateStorageRootKey(CommandTpm
                    .DefaultSrkHandle); // Will not create key if obj already exists at handle
                byte[] _ = tpm.ReadPublicArea(CommandTpm.DefaultSrkHandle, out byte[] _, out byte[] _);

                List<byte[]> pcs = null, baseRims = null, supportRimELs = null, supportRimPCRs = null;
                if (Settings.HasEfiPrefix()) {
                    Log.Information("----> Gathering artifacts from EFI.");
                    pcs = Settings.gatherPlatformCertificatesFromEFI();
                    baseRims = Settings.gatherRIMBasesFromEFI();
                    supportRimELs = Settings.gatherSupportRIMELsFromEFI();
                    supportRimPCRs = Settings.gatherSupportRIMPCRsFromEFI();
                }

                Log.Debug("Setting up the Client.");
                Uri acaAddress = Settings.aca_address_port;
                if (acaClient == null) {
                    UseBuiltInClient(acaAddress.AbsoluteUri);
                }

                Log.Information("----> Collecting device information.");
                DeviceInfo dv;
                try {
                    dv = deviceInfoCollector.CollectDeviceInfo(acaAddress.AbsoluteUri);
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
                    manifest = Settings.paccor_output;
                } else {
                    Log.Warning("No hardware collectors nor paccor output file were identified.");
                }

                Log.Debug("Hardware component information that will be sent to the ACA: " + manifest);

                Log.Debug("Gathering the event log.");
                byte[] eventLog;
                if (Settings.HasEventLogFromFile()) {
                    Log.Debug("  Using the event log identified in settings.");
                    eventLog = Settings.event_log;
                } else {
                    Log.Debug("  Attempting to collect the event log from the system.");
                    eventLog = tpm.GetEventLog();
                }

                if (eventLog is not null or []) {
                    Log.Debug("Event log gathered is " + eventLog.Length + " bytes.");
                    dv.Livelog = ByteString.CopyFrom(eventLog);
                }

                Log.Debug("Gathering PCR data from the TPM.");
                string pcrsList, pcrsSha1, pcrsSha256;
                CommandTpm.FormatPcrValuesForAca(tpm.GetPcrList(Tpm2Lib.TpmAlgId.Sha1), "sha1", out pcrsSha1);
                CommandTpm.FormatPcrValuesForAca(tpm.GetPcrList(Tpm2Lib.TpmAlgId.Sha256), "sha256", out pcrsSha256);
                pcrsList = pcrsSha1 + pcrsSha256;
                Log.Debug("Result of formatting pcr values for the ACA:");
                Log.Debug("\n" + pcrsList);
                dv.Pcrslist = ByteString.CopyFromUtf8(pcrsList);

                Log.Information("----> " + (Cli.ReplaceLDevID ? "Creating new" : "Verifying existence of") +
                                " LDevID Key.");
                string ldevidPubPath = FormatCertificatePath(dv, Settings.certificate_output_directory,
                    DefaultLDevIDPubKeyFileName);
                string ldevidPrivPath = FormatCertificatePath(dv, Settings.certificate_output_directory,
                    DefaultLDevIDPrivKeyFileName);
                tpm.CreateLDevIDKey(CommandTpm.DefaultSrkHandle, ldevidPubPath, ldevidPrivPath, Cli.ReplaceLDevID);

                Log.Debug("Gathering LDevID PUBLIC.");
                byte[] ldevidPublicArea = tpm.ConvertLDevIDPublic(ldevidPubPath);
                if (CommandTpm.CanMarshal<Tpm2Lib.TpmPublic>(ldevidPublicArea)) {
                    Tpm2Lib.TpmPublic ldevidPub = CommandTpm.Marshal<Tpm2Lib.TpmPublic>(ldevidPublicArea);
                    keyCandidates.Add(KeyTemplateCatalog.BuildAsymmetricCandidate(ldevidPublicArea, ldevidPub,
                        0, KeyRole.DeviceIdentity, ProvisioningOrigin.Local));
                }

                Log.Debug("Create identity claim");
                IdentityClaim idClaim = acaClient.CreateIdentityClaim(dv, akPublicArea, ekPublicArea, ekc, pcs,
                    manifest, ldevidPublicArea, keyCandidates);

                Log.Information("----> Sending identity claim to Attestation CA");
                IdentityClaimResponse icr = await acaClient.PostIdentityClaim(idClaim);
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

                if (!icr.HasEncryptedSecret) {
                    Log.Error("The response from the ACA did not contain a EncryptedSecret.");
                    return (int)ClientExitCodes.MAKE_CREDENTIAL_ENCRYPTED_SECRET_MALFORMED;
                }

                byte[] credentialBlob = icr.CredentialBlob.ToByteArray(); // TPM2B_ID_OBJECT; look for the nonce
                byte[] encryptedSecret = icr.EncryptedSecret.ToByteArray(); // TPM2B_ENCRYPTED_SECRET
                Log.Debug("ACA delivered IdentityClaimResponse credentialBlob " +
                          BitConverter.ToString(credentialBlob));
                Log.Debug("ACA delivered IdentityClaimResponse encryptedSecret " +
                          BitConverter.ToString(encryptedSecret));

                Log.Debug("Executing activateCredential.");
                byte[] recoveredSecret = tpm.ActivateCredential(CommandTpm.DefaultAkHandle, l1Ek.Handle,
                    credentialBlob, encryptedSecret);

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
                tpm.GetQuote(CommandTpm.DefaultAkHandle, Tpm2Lib.TpmAlgId.Sha256, recoveredSecret,
                    out CommandTpmQuoteResponse ctqr, selectPcrs);
                
                CertificateRequest akCertReq = acaClient.CreateAkCertificateRequest(recoveredSecret, ctqr);
                string certificate;
                Log.Debug("Communicate certificate request to the ACA.");
                CertificateResponse cr = await acaClient.PostCertificateRequest(akCertReq);
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
                    String certificateDirPath = Settings.certificate_output_directory;
                    if (!string.IsNullOrWhiteSpace(certificateDirPath)) {
                        String certificateFilePath =
                            FormatCertificatePath(dv, certificateDirPath, DefaultAKCertFileName);
                        try {
                            File.WriteAllText(certificateFilePath, certificate);
                            Log.Debug("Attestation key certificate written to local file system: {0}",
                                certificateFilePath);
                        } catch (Exception) {
                            Log.Debug("Failed to write attestation key certificate to local file system.");
                        }
                    }

                    Log.Debug("Printing attestation key certificate: " + certificate);
                }

                if (cr.HasLdevidCertificate) {
                    certificate = cr.LdevidCertificate; // contains certificate
                    String ldevidCertificateDirPath = Settings.certificate_output_directory;
                    if (!string.IsNullOrWhiteSpace(ldevidCertificateDirPath)) {
                        String certificateFilePath =
                            FormatCertificatePath(dv, ldevidCertificateDirPath, DefaultLDevIDCertFileName);
                        try {
                            File.WriteAllText(certificateFilePath, certificate);
                            Log.Debug("LDevID certificate written to local file system: {0}", certificateFilePath);
                        } catch (Exception) {
                            Log.Debug("Failed to write LDevID certificate to local file system.");
                        }
                    }
                    Log.Debug("Printing LDevID certificate: " + certificate);
                }
            } else {
                result = ClientExitCodes.TPM_ERROR;
                Log.Error("Could not provision because the TPM object was null.");
            }

            return (int)result;
        }
    }
}
