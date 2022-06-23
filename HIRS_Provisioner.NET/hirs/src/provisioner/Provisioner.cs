using Google.Protobuf;
using Hirs.Pb;
using Serilog;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading.Tasks;

namespace hirs {

    public class Provisioner : IHirsProvisioner {
        private CLI cli = null;
        private Settings settings = null;
        private IHirsDeviceInfoCollector deviceInfoCollector = null;
        private IHirsAcaClient acaClient = null;

        public Provisioner() {
        }

        public Provisioner(Settings settings, CLI cli) {
            SetSettings(settings);
            SetCLI(cli);
        }

        public void SetSettings(Settings settings) {
            if (settings == null) {
                Log.Error("Unknown error. Settings were supposed to have been parsed.");
            }
            this.settings = settings;
        }

        public void SetCLI(CLI cli) {
            if (cli == null) {
                Log.Error("Unknown error. CLI arguments were supposed to have been parsed.");
            }
            this.cli = cli;
        }

        public IHirsAcaTpm ConnectTpm() {
            IHirsAcaTpm tpm = null;
            // If tpm device type is set on the command line
            if (cli.Nix) {
                tpm = new CommandTpm(CommandTpm.Devices.NIX);
            } else if (cli.Tcp && cli.Ip != null) {
                string[] split = cli.Ip.Split(":");
                if (split.Length == 2) {
                    tpm = new CommandTpm(cli.Sim, split[0], Int32.Parse(split[1]));
                    Log.Debug("Connected to TPM via TCP at " + cli.Ip);
                } else {
                    Log.Error("ip input should have the format servername:port. The given input was '" + cli.Ip + "'.");
                }
            } else if (cli.Win) {
                tpm = new CommandTpm(CommandTpm.Devices.WIN);
            }

            // If command line not set, check if auto detect is enabled
            if ((tpm == null) && settings.IsAutoDetectTpmEnabled()) {
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

                // if tpm still null, try set up TcpTpmDevice on sim, catch exception
                if (tpm == null) {
                    try {
                        string[] split = CommandTpm.DefaultSimulatorNamePort.Split(":");
                        tpm = new CommandTpm(true, split[0], Int32.Parse(split[1]));
                        Log.Debug("Auto Detect found a TPM simulator at " + CommandTpm.DefaultSimulatorNamePort + ".");
                    } catch (Exception) {
                        Log.Debug("No TPM simulator found by auto detect.");
                    }
                }
            } else if ((tpm != null) && settings.IsAutoDetectTpmEnabled()) {
                Log.Debug("Auto detect TPM was enabled in settings, but command line options were also given. Using command line options.");
            }
            
            // If TPM is still not set up, offer help message
            if (tpm == null) {
                Log.Fatal(
                    "To connect to a TPM device on Windows, add the command line argument --win\n" +
                    "To connect to a TPM device on LINUX, add the command line argument --nix\n" +
                    "To connect to a TPM via TCP, add the command line arguments --tcp <address>:<port>\n" +
                    "To connect to a TPM simulator at the default TCP socket of " + CommandTpm.DefaultSimulatorNamePort + ", add the command line arguments --tcp --sim\n" +
                    "To connect to a TPM simulator at any other socket, add the command line arguments --tcp --sim <address>:<port>\n");
            }
            return tpm;
        }

        public void UseBuiltInClient(string addr) {
            acaClient = new Client(addr);
        }

        public void SetClient(IHirsAcaClient client) {
            acaClient = client;
        }

        public void UseClassicDeviceInfoCollector() {
            deviceInfoCollector = new ClassicDeviceInfoCollector();
        }

        public void SetDeviceInfoCollector(IHirsDeviceInfoCollector collector) {
            if (collector == null) {
                UseClassicDeviceInfoCollector();
            } else {
                deviceInfoCollector = collector;
            }
        }

        public async Task<int> Provision(IHirsAcaTpm tpm) {
            ClientExitCodes result = ClientExitCodes.SUCCESS;
            if (tpm != null) {
                Log.Information("--> Provisioning");
                Log.Information("----> Gathering Endorsement Key Certificate.");
                byte[] ekc = tpm.GetCertificateFromNvIndex(CommandTpm.DefaultEkcNvIndex);
                if (ekc.Length == 0) {
                    Log.Information("------> No Endorsement Key Certificate found at the expected index. The ACA may have one uploaded for this TPM.");
                }
                Log.Debug("Checking EK PUBLIC");
                tpm.CreateEndorsementKey(CommandTpm.DefaultEkHandle); // Will not create key if obj already exists at handle
                byte[] ekPublicArea = tpm.ReadPublicArea(CommandTpm.DefaultEkHandle, out byte[] name, out byte[] qualifiedName);

                Log.Information("----> " + (cli.ReplaceAK ? "Creating new" : "Verifying existence of") + " Attestation Key.");
                tpm.CreateAttestationKey(CommandTpm.DefaultEkHandle, CommandTpm.DefaultAkHandle, cli.ReplaceAK);

                Log.Debug("Gathering AK PUBLIC.");
                byte[] akPublicArea = tpm.ReadPublicArea(CommandTpm.DefaultAkHandle, out name, out qualifiedName);

                List<byte[]> pcs = null, baseRims = null, supportRimELs = null, supportRimPCRs = null;
                if (settings.HasEfiPrefix()) {
                    Log.Information("----> Gathering artifacts from EFI.");
                    pcs = settings.gatherPlatformCertificatesFromEFI();
                    baseRims = settings.gatherRIMBasesFromEFI();
                    supportRimELs = settings.gatherSupportRIMELsFromEFI();
                    supportRimPCRs = settings.gatherSupportRIMPCRsFromEFI();
                }

                Log.Debug("Setting up the Client.");
                Uri acaAddress = settings.aca_address_port;
                if (acaClient == null) {
                    UseBuiltInClient(acaAddress.AbsoluteUri);
                }

                Log.Information("----> Collecting device information.");
                DeviceInfo dv = deviceInfoCollector.CollectDeviceInfo(acaAddress.AbsoluteUri);
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
                if (settings.HasHardwareManifestPlugins()) {
                    manifest = settings.RunHardwareManifestCollectors();
                } else if (settings.HasPaccorOutputFromFile()) {
                    manifest = settings.paccor_output;
                } else {
                    Log.Warning("No hardware collectors nor paccor output file were identified.");
                }
                Log.Debug("Hardware component information that will be sent to the ACA: " + manifest);

                Log.Debug("Gathering the event log.");
                byte[] eventLog;
                if (settings.HasEventLogFromFile()) {
                    Log.Debug("  Using the event log identified in settings.");
                    eventLog = settings.event_log;
                } else {
                    Log.Debug("  Attempting to collect the event log from the system.");
                    eventLog = tpm.GetEventLog();
                }
                    
                if (eventLog != null) {
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

                Log.Debug("Create identity claim");
                IdentityClaim idClaim = acaClient.CreateIdentityClaim(dv, akPublicArea, ekPublicArea, ekc, pcs, manifest);

                Log.Information("----> Sending identity claim to Attestation CA");
                IdentityClaimResponse icr = await acaClient.PostIdentityClaim(idClaim);
                Log.Information("----> Received response. Attempting to decrypt nonce");
                if (icr.HasStatus) {
                    if (icr.Status == ResponseStatus.Pass) {
                        Log.Debug("The ACA accepted the identity claim.");
                    } else {
                        Log.Debug("The ACA did not accept the identity claim. See details on the ACA.");
                        result = ClientExitCodes.PASS_1_STATUS_FAIL;
                        return (int)result;
                    }
                }

                byte[] integrityHMAC = null, encIdentity = null, encryptedSecret = null;
                if (icr.HasCredentialBlob) {
                    byte[] credentialBlob = icr.CredentialBlob.ToByteArray(); // look for the nonce
                    Log.Debug("ACA delivered IdentityClaimResponse credentialBlob " + BitConverter.ToString(credentialBlob));
                    int credentialBlobLen = credentialBlob[0] | (credentialBlob[1] << 8); 
                    int integrityHmacLen = (credentialBlob[2] << 8) | credentialBlob[3]; 
                    integrityHMAC = new byte[integrityHmacLen];
                    Array.Copy(credentialBlob, 4, integrityHMAC, 0, integrityHmacLen);
                    int encIdentityLen = credentialBlobLen - integrityHmacLen - 2;
                    encIdentity = new byte[encIdentityLen];
                    Array.Copy(credentialBlob, 4 + integrityHmacLen, encIdentity, 0, encIdentityLen);
                    // The following offsets are bound tightly to the way makecredential is implemented on the ACA.
                    int encryptedSecretLen = credentialBlob[134] | (credentialBlob[135] << 8);
                    encryptedSecret = new byte[encryptedSecretLen];
                    Array.Copy(credentialBlob, 136, encryptedSecret, 0, encryptedSecretLen);
                    Log.Debug("Prepared values to give to activateCredential.");
                    Log.Debug("    integrityHMAC: " + BitConverter.ToString(integrityHMAC));
                    Log.Debug("    encIdentity: " + BitConverter.ToString(encIdentity));
                    Log.Debug("    encryptedSecret: " + BitConverter.ToString(encryptedSecret));
                } else {
                    result = ClientExitCodes.MAKE_CREDENTIAL_BLOB_MALFORMED;
                    Log.Error("The response from the ACA did not contain a CredentialBlob.");
                }

                if (integrityHMAC != null && encIdentity != null && encryptedSecret != null) {
                    Log.Debug("Executing activateCredential.");
                    byte[] recoveredSecret = tpm.ActivateCredential(CommandTpm.DefaultAkHandle, CommandTpm.DefaultEkHandle, integrityHMAC, encIdentity, encryptedSecret);
                    Log.Debug("Gathering quote.");
                    uint[] selectPcrs = null;
                    if (icr.HasPcrMask) {
                        // For now, the ACA will send a comma separated selection of PCRs as a string
                        try {
                            selectPcrs = icr.PcrMask.Split(',').Select(uint.Parse).ToList().ToArray();
                        } catch (Exception) {
                            Log.Warning("PcrMask was included in the IdentityClaimResponse, but could not be parsed." +
                                        "Collecting quote over default PCR selection.");
                            Log.Debug("This PcrMask could not be parsed: " + icr.PcrMask);
                        }
                    }
                    tpm.GetQuote(CommandTpm.DefaultAkHandle, Tpm2Lib.TpmAlgId.Sha256, recoveredSecret, out CommandTpmQuoteResponse ctqr, selectPcrs);
                    Log.Information("----> Nonce successfully decrypted. Sending attestation certificate request");
                    CertificateRequest akCertReq = acaClient.CreateAkCertificateRequest(recoveredSecret, ctqr);
                    byte[] certificate;
                    Log.Debug("Communicate certificate request to the ACA.");
                    CertificateResponse cr = await acaClient.PostCertificateRequest(akCertReq);
                    Log.Debug("Response received from the ACA regarding the certificate request.");
                    if (cr.HasStatus) {
                        if (cr.Status == ResponseStatus.Pass) {
                            Log.Debug("ACA returned a positive response to the Certificate Request.");
                        } else {
                            Log.Debug("The ACA did not return any certificates. See details on the ACA.");
                            result = ClientExitCodes.PASS_2_STATUS_FAIL;
                            return (int)result;
                        }
                    }
                    if (cr.HasCertificate) {
                        certificate = cr.Certificate.ToByteArray(); // contains certificate
                        Log.Debug("Printing attestation key certificate: " + BitConverter.ToString(certificate));
                    }
                } else {
                    result = ClientExitCodes.MAKE_CREDENTIAL_BLOB_MALFORMED;
                    Log.Error("Credential elements could not be extracted from the ACA's response.");
                }
            } else {
                result = ClientExitCodes.TPM_ERROR;
                Log.Error("Could not provision because the TPM object was null.");
            }
            return (int)result;
        }

    }
}
