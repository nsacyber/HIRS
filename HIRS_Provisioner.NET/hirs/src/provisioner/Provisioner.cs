using CommandLine;
using Google.Protobuf;
using Hirs.Pb;
using Microsoft.Extensions.Configuration;
using Newtonsoft.Json;
using Serilog;
using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;

namespace hirs {

    public class Provisioner : IHirsProvisioner {
        private CLI cli = null;
        private Settings settings = null;
        private IHirsDeviceInfoCollector deviceInfoCollector = null;
        private IHirsAcaClient acaClient = null;

        public Provisioner(Settings settings, CLI cli) {
            setSettings(settings);
            setCLI(cli);
        }

        public void setSettings(Settings settings) {
            if (settings == null) {
                Log.Error("Unknown error. Settings were supposed to have been parsed.");
            }
            this.settings = settings;
        }

        public void setCLI(CLI cli) {
            if (cli == null) {
                Log.Error("Unknown error. CLI arguments were supposed to have been parsed.");
            }
            this.cli = cli;
        }

        public IHirsAcaTpm connectTpm() {
            IHirsAcaTpm tpm = null;
            // If tpm device type is set on the command line
            if (cli.nix) {
                tpm = new CommandTpm(CommandTpm.Devices.NIX);
            } else if (cli.tcp && cli.ip != null) {
                string[] split = cli.ip.Split(":");
                if (split.Length == 2) {
                    tpm = new CommandTpm(cli.sim, split[0], Int32.Parse(split[1]));
                    Log.Debug("Connected to TPM via TCP at " + cli.ip);
                } else {
                    Log.Error("ip input should have the format servername:port. The given input was '" + cli.ip + "'.");
                }
            } else if (cli.win) {
                tpm = new CommandTpm(CommandTpm.Devices.WIN);
            }

            // If command line not set, check if auto detect is enabled
            if ((tpm == null) && settings.IsAutoDetectTpmEnabled()) {
                Log.Debug("Auto Detect TPM is Enabled. Starting search for the TPM.");
                if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) {
                    try {
                        tpm = new CommandTpm(CommandTpm.Devices.WIN);
                        Log.Warning("Auto Detect found a WIN TPM Device.");
                    } catch (Exception e) {
                        Log.Warning("No WIN TPM Device found by auto detect.");
                    }
                } else if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux)) {
                    try {
                        tpm = new CommandTpm(CommandTpm.Devices.NIX);
                        Log.Warning("Auto Detect found a Linux TPM Device.");
                    } catch (Exception e) {
                        Log.Warning("No Linux TPM Device found by auto detect.");
                    }
                }

                // if tpm still null, try set up TcpTpmDevice on sim, catch exception
                if (tpm == null) {
                    try {
                        string[] split = CommandTpm.DefaultSimulatorNamePort.Split(":");
                        tpm = new CommandTpm(true, split[0], Int32.Parse(split[1]));
                        Log.Warning("Auto Detect found a TPM simulator at " + CommandTpm.DefaultSimulatorNamePort + ".");
                    } catch (Exception e) {
                        Log.Warning("No TPM simulator found by auto detect.");
                    }
                }
            } else if ((tpm != null) && settings.IsAutoDetectTpmEnabled()) {
                Log.Warning("Auto detect TPM was enabled in settings, but command line options were also given. Using command line options.");
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

        public void useBuiltInClient(string addr) {
            acaClient = new Client(addr);
        }

        public void setClient(IHirsAcaClient client) {
            acaClient = client;
        }

        public void useClassicDeviceInfoCollector() {
            deviceInfoCollector = new ClassicDeviceInfoCollector();
        }

        public void setDeviceInfoCollector(IHirsDeviceInfoCollector collector) {
            if (collector == null) {
                useClassicDeviceInfoCollector();
            } else {
                deviceInfoCollector = collector;
            }
        }

        public async Task<int> provision(IHirsAcaTpm tpm) {
            int result = 0;
            if (tpm != null) {
                List<byte[]> pcs = null, baseRims = null, supportRimELs = null, supportRimPCRs = null;
                if (settings.HasEfiPrefix()) {
                    Log.Debug("Gathering artifacts from EFI.");
                    pcs = settings.gatherPlatformCertificatesFromEFI();
                    baseRims = settings.gatherRIMBasesFromEFI();
                    supportRimELs = settings.gatherSupportRIMELsFromEFI();
                    supportRimPCRs = settings.gatherSupportRIMPCRsFromEFI();
                }
                
                Log.Debug("Gathering EK Certificate.");
                byte[] name = null, qualifiedName = null, ekPublicArea = null;
                byte[] ekc = tpm.getCertificateFromNvIndex(CommandTpm.DefaultEkcNvIndex);
                Log.Debug("Checking EK PUBLIC");
                tpm.createEndorsementKey(CommandTpm.DefaultEkHandle); // Will not create key if obj already exists at handle
                ekPublicArea = tpm.readPublicArea(CommandTpm.DefaultEkHandle, out name, out qualifiedName);

                Log.Debug(cli.replaceAK ? "Creating AK." : "Verifying AK.");
                tpm.createAttestationKey(CommandTpm.DefaultEkHandle, CommandTpm.DefaultAkHandle, cli.replaceAK);

                Log.Debug("Gathering AK PUBLIC.");
                byte[] akPublicArea = tpm.readPublicArea(CommandTpm.DefaultAkHandle, out name, out qualifiedName);

                Log.Debug("Gather Device Info");
                Uri acaAddress = settings.getAcaAddress();
                DeviceInfo dv = deviceInfoCollector.collectDeviceInfo(acaAddress.AbsoluteUri);
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

                Log.Debug("Gather Event log");
                byte[] eventLog = settings.gatherEventLogFromAppsettingsPath();
                if (eventLog == null) {
                    eventLog = tpm.GetEventLog();
                }
                if (eventLog != null) {
                    dv.Livelog = ByteString.CopyFrom(eventLog);
                }

                Log.Debug("Set up Client.");
                if (acaClient == null) {
                    useBuiltInClient(acaAddress.AbsoluteUri);
                }

                Log.Debug("Gather hardware manifest:");
                string manifest = settings.getHardwareManifest();
                if (manifest == null) {
                    manifest = settings.getPaccorOutput();
                }
                Log.Debug(manifest);

                Log.Debug("Gather PCR List.");
                string pcrsList, pcrsSha1, pcrsSha256;
                CommandTpm.formatPcrValuesForAca(tpm.getPcrList(Tpm2Lib.TpmAlgId.Sha1), "sha1", out pcrsSha1);
                CommandTpm.formatPcrValuesForAca(tpm.getPcrList(Tpm2Lib.TpmAlgId.Sha256), "sha256", out pcrsSha256);
                pcrsList = pcrsSha1 + pcrsSha256;
                Log.Debug("Result of formatting pcr values for the ACA:");
                Log.Debug("\n" + pcrsList);
                dv.Pcrslist = ByteString.CopyFromUtf8(pcrsList);

                Log.Debug("Create identity claim");
                IdentityClaim idClaim = acaClient.createIdentityClaim(dv, akPublicArea, ekPublicArea, ekc, pcs, manifest);

                Log.Debug("Communicate identity claim to the ACA.");
                //Task<IdentityClaimResponse> task = Task.Run(() => client.postIdentityClaim(idClaim));
                //task.Wait();
                IdentityClaimResponse icr = await acaClient.postIdentityClaim(idClaim); // task.Result;
                Log.Debug("Response received from the ACA regarding the identity claim.");

                byte[] integrityHMAC = null, encIdentity = null, encryptedSecret = null;
                if (icr.HasCredentialBlob) {
                    byte[] credentialBlob = icr.CredentialBlob.ToByteArray(); // contains nonce
                    Log.Debug("ACA delivered IdentityClaimResponse credentialBlob " + BitConverter.ToString(credentialBlob));
                    int credentialBlobLen = credentialBlob[0] | (credentialBlob[1] << 8); // The size value does not include the 2 blob len bytes
                    int integrityHmacLen = ((credentialBlob[2] << 8) | credentialBlob[3]);// + 2; // +2 for the integrityHMAC Digest size bytes
                    integrityHMAC = new byte[integrityHmacLen];
                    Array.Copy(credentialBlob, 4, integrityHMAC, 0, integrityHmacLen);
                    int encIdentityLen = credentialBlobLen - integrityHmacLen - 2;
                    encIdentity = new byte[encIdentityLen];
                    Array.Copy(credentialBlob, 4 + integrityHmacLen, encIdentity, 0, encIdentityLen);
                    // The following offsets are bound tightly to the way makecredential is implemented on the ACA.
                    int encryptedSecretLen = (credentialBlob[134] | (credentialBlob[135] << 8));// + 2; // +2 because the size bytes are to be included
                    encryptedSecret = new byte[encryptedSecretLen];
                    Array.Copy(credentialBlob, 136, encryptedSecret, 0, encryptedSecretLen);
                    Log.Debug("Prepared values to give to activateCredential.");
                    Log.Debug("    integrityHMAC: " + BitConverter.ToString(integrityHMAC));
                    Log.Debug("    encIdentity: " + BitConverter.ToString(encIdentity));
                    Log.Debug("    encryptedSecret: " + BitConverter.ToString(encryptedSecret));
                } else if (result == 0) {
                    result = 102;
                    Log.Error("The response from the ACA did not contain a CredentialBlob. Look at the ACA logs.");
                }

                if (integrityHMAC != null && encIdentity != null && encryptedSecret != null) {
                    Log.Debug("Executing activateCredential.");
                    byte[] recoveredSecret = tpm.activateCredential(CommandTpm.DefaultAkHandle, CommandTpm.DefaultEkHandle, integrityHMAC, encIdentity, encryptedSecret);
                    CommandTpmQuoteResponse ctqr;
                    Log.Debug("Gathering quote.");
                    uint[] selectPcrs = null;
                    if (icr.HasPcrMask) {
                        // For now, the ACA will send a comma separated selection of PCRs as a string
                        try {
                            selectPcrs = (uint[])icr.PcrMask.Split(',').Select(uint.Parse).ToList().ToArray();
                        } catch (Exception e) {
                            Log.Warning("PcrMask was included in the IdentityClaimResponse, but could not be parsed." +
                                        "Collecting quote over default PCR selection.");
                            Log.Debug("This PcrMask could not be parsed: " + icr.PcrMask);
                        }
                    }
                    tpm.getQuote(CommandTpm.DefaultAkHandle, Tpm2Lib.TpmAlgId.Sha256, recoveredSecret, out ctqr, selectPcrs);
                    Log.Debug("Create certificate request and include recovered secret");
                    CertificateRequest akCertReq = acaClient.createAkCertificateRequest(recoveredSecret, ctqr);
                    byte[] certificate;
                    Log.Debug("Communicate certificate request to the ACA.");
                    //var task2 = Task.Run(() => client.postCertificateRequest(akCertReq));
                    //task2.Wait();
                    CertificateResponse cr = await acaClient.postCertificateRequest(akCertReq); //task2.Result;
                    Log.Debug("Response received from the ACA regarding the certificate request.");

                    certificate = cr.Certificate.ToByteArray(); // contains certificate

                    Log.Debug("issued certificate: " + BitConverter.ToString(certificate));
                } else if (result == 0) {
                    result = 103;
                    Log.Error("Credential elements could not be extracted from the ACA's response. Ensure your acaAddress is accurate and the ACA is the expected version. Submit a bug report.");
                }
            } else {
                result = 104;
            }
            return result;
        }

    }
}
