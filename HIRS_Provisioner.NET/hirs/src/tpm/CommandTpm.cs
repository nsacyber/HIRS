using Serilog;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Security.Principal;
using Tpm2Lib;

namespace hirs {
    public class CommandTpm : IHirsAcaTpm {
        public enum Devices {
            NIX,
            TCP,
            WIN
        }

        /// <summary>
        /// If using a TCP connection, the default DNS name/IP address for the
        /// simulator.
        /// </summary>
        public const string DefaultSimulatorNamePort = "127.0.0.1:2321";

        public const uint DefaultEkcNvIndex = 0x1c00002;
        public const uint DefaultEkHandle = 0x81010001;
        public const uint DefaultAkHandle = 0x81010002;
        
        private readonly Tpm2 tpm;

        private readonly Boolean simulator;

        private List<AuthSession> sessionTracking = new List<AuthSession>();

        /**
         * For TCP TpmDevices
         */
        public CommandTpm(Boolean sim, string ip, int port) {
            simulator = sim;
            Tpm2Device tpmDevice = new TcpTpmDevice(ip, port);
            tpm = tpmSetupByType(tpmDevice);
        }

        /**
         * For a TPM device on Linux and Windows
         */
        public CommandTpm(Devices dev) {
            Tpm2Device tpmDevice = null;
            switch (dev) {
                case Devices.NIX:
                    // LinuxTpmDevice will first try to connect tpm2-abrmd and second try to connect directly to device
                    tpmDevice = new LinuxTpmDevice();
                    break;
                case Devices.WIN:
                    tpmDevice = new TbsDevice();
                    break;
                default:
                    Log.Error("Unknown option selected in CommandTpm(Devices) constructor.");
                    break;
            }
            tpm = tpmSetupByType(tpmDevice);
        }

        public CommandTpm(Tpm2 tpm) {
            this.tpm = tpm;
        }

        ~CommandTpm() {
            if (tpm != null) {
                tpm.Dispose();
            }
        }

        public byte[] getCertificateFromNvIndex(uint index) {
            Log.Debug("getCertificateFromNvIndex 0x" + index.ToString("X"));
            byte[] certificate = null;
            
            TpmHandle nvHandle = new TpmHandle(index);
            try {
                byte[] nvName; // not used for this function. have to collect from NvReadPublic. 
                NvPublic obj = tpm.NvReadPublic(nvHandle, out nvName);
                if (obj != null) {
                    byte[] indexData = nvBufferedRead(TpmHandle.RhOwner, nvHandle, obj.dataSize, 0);
                    if (indexData != null) {
                        certificate = extractFirstCertificate(indexData); // the nvIndex could contain random fill around the certificate
                        if (certificate != null) {
                            Log.Debug("getEndorsementKeyCertificate: Read: " + BitConverter.ToString(certificate));
                        } else {
                            Log.Warning("getEndorsementKeyCertificate: No certificate found within data at index.");
                        }
                    } else {
                        Log.Warning("getEndorsementKeyCertificate: Could not read any data.");
                    }
                } else {
                    Log.Warning("getEndorsementKeyCertificate: Nothing found at index: " + DefaultEkcNvIndex);
                }
            } catch (TpmException e) {
                Log.Error(e, "getEndorsementKeyCertificate TPM error");
            }
            return certificate;
        }

        private byte[] nvBufferedRead(TpmHandle authHandle, TpmHandle nvIndex, ushort size, ushort offset) {
            ushort maxReadSize = 256;
            byte[] buffer = new byte[size];

            ushort ptr = 0;
            while (offset < size) {
                int r;
                int q = Math.DivRem(size - offset, maxReadSize, out r);
                ushort sizeToRead = q > 0 ? maxReadSize : (ushort)r;
                byte[] block = tpm.NvRead(authHandle, nvIndex, sizeToRead, offset);
                Array.Copy(block, 0, buffer, ptr, sizeToRead);
                offset += sizeToRead;
                ptr += sizeToRead;
            }
            return buffer;
        }

        private byte[] extractFirstCertificate(byte[] data) {
            byte[] extracted = null;

            if (data != null) {
                // search for first instance of 30 82
                int pos = 0;
                bool found = false;
                while (pos < (data.Length - 1)) {
                    if (data[pos] == 0x30) {
                        if (data[pos + 1] == 0x82) {
                            found = true;
                            break;
                        }
                    }
                    pos++;
                }

                // find the size of the structure.
                // 30 82 means the size will be described in next 2 bytes
                // Data from NV should be BIG ENDIAN since 3082 was found in step 1.
                int size = 0;
                if (found && data.Length > (pos + 3)) {
                    byte[] sizeBuffer = new byte[2];
                    sizeBuffer[0] = data[pos + 3];
                    sizeBuffer[1] = data[pos + 2];
                    size = 4 + BitConverter.ToInt16(sizeBuffer); // 4 bytes added to final count for pos+3
                }

                // copy the structure to the output buffer
                if (size > 0) {
                    extracted = new byte[size];
                    Array.Copy(data, pos, extracted, 0, size);
                }
            }

            return extracted;
        }

        // allows client to access the readpublic function
        public TpmPublic readPublicArea(uint handleInt, out byte[] name, out byte[] qualifiedName) {
            TpmHandle handle = new TpmHandle(handleInt);
            TpmPublic obj = null;
            byte[] localName = null, localQualifiedName = null;
            name = null;
            qualifiedName = null;
            try {
                obj = tpm.ReadPublic(handle, out localName, out localQualifiedName);
                name = localName;
                qualifiedName = localQualifiedName;
            } catch (TpmException e) {
                Log.Debug(e, "readPublicArea");
            }
            return obj;
        }

        public static TpmPublic generateEKTemplateL1() {
            TpmAlgId nameAlg = TpmAlgId.Sha256;
            ObjectAttr attributes = ObjectAttr.FixedTPM | ObjectAttr.FixedParent | ObjectAttr.SensitiveDataOrigin | ObjectAttr.AdminWithPolicy | ObjectAttr.Restricted | ObjectAttr.Decrypt;
            byte[] auth_policy = { // Template L-1
                0x83, 0x71, 0x97, 0x67, 0x44, 0x84,
                0xB3, 0xF8, 0x1A, 0x90, 0xCC, 0x8D,
                0x46, 0xA5, 0xD7, 0x24, 0xFD, 0x52,
                0xD7, 0x6E, 0x06, 0x52, 0x0B, 0x64,
                0xF2, 0xA1, 0xDA, 0x1B, 0x33, 0x14,
                0x69, 0xAA
            };
            // ASYM: RSA 2048 with NULL scheme, SYM: AES-128 with CFB mode
            RsaParms rsa = new RsaParms(new SymDefObject(TpmAlgId.Aes, 128, TpmAlgId.Cfb), new NullAsymScheme(), 2048, 0);
            // unique buffer must be filled with 0 for the EK Template L-1.
            byte[] zero256 = new byte[256];
            Array.Fill<byte>(zero256, 0x00);
            Tpm2bPublicKeyRsa unique = new Tpm2bPublicKeyRsa(zero256);
            TpmPublic inPublic = new TpmPublic(nameAlg, attributes, auth_policy, rsa, unique);
            return inPublic;
        }

        public void createEndorsementKey(uint ekHandleInt) {
            TpmHandle ekHandle = new TpmHandle(ekHandleInt);

            TpmPublic existingObject = null;
            byte[] name, qualifiedName;
            try {
                existingObject = tpm.ReadPublic(ekHandle, out name, out qualifiedName);
                Log.Debug("EK already exists.");
                return;
            } catch (TpmException) {
                Log.Debug("Verified EK does not exist at expected handle. Creating EK.");
            }

            SensitiveCreate inSens = new SensitiveCreate(); // key password (no params = no key password)
            TpmPublic inPublic = CommandTpm.generateEKTemplateL1(); 
            TpmPublic outPublic;
            CreationData creationData;
            byte[] creationHash;
            TkCreation ticket;

            TpmHandle newTransientEkHandle = tpm.CreatePrimary(TpmRh.Endorsement, inSens, inPublic, new byte[] { }, new PcrSelection[] { }, out outPublic,
                                            out creationData, out creationHash, out ticket);

            Log.Debug("New EK Handle: " + BitConverter.ToString(newTransientEkHandle));
            Log.Debug("New EK PUB Name: " + BitConverter.ToString(outPublic.GetName()));
            Log.Debug("New EK PUB 2BREP: " + BitConverter.ToString(outPublic.GetTpm2BRepresentation()));

            // Make the object persistent
            tpm.EvictControl(TpmRh.Owner, newTransientEkHandle, ekHandle);
            Log.Debug("Successfully made the new EK persistent at handle " + BitConverter.ToString(ekHandle) + ".");

            tpm.FlushContext(newTransientEkHandle);
            Log.Debug("Flushed the context for the transient EK.");
        }

        private RsaParms akRsaParms() {
            TpmAlgId digestAlg = TpmAlgId.Sha256;
            RsaParms parms = new RsaParms(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null), new SchemeRsassa(digestAlg), 2048, 0);
            return parms;
        }

        private ObjectAttr akAttributes() {
            ObjectAttr attrib = ObjectAttr.Restricted | ObjectAttr.Sign | ObjectAttr.FixedParent | ObjectAttr.FixedTPM
                    | ObjectAttr.SensitiveDataOrigin | ObjectAttr.UserWithAuth;
            return attrib;
        }

        private TpmPublic generateAKTemplate(TpmAlgId nameAlg) {
            RsaParms rsa = akRsaParms();
            ObjectAttr attributes = akAttributes();
            TpmPublic inPublic = new TpmPublic(nameAlg, attributes, null, rsa, new Tpm2bPublicKeyRsa());
            return inPublic;
        }

        public void createAttestationKey(uint ekHandleInt, uint akHandleInt, bool replace) {
            TpmHandle ekHandle = new TpmHandle(ekHandleInt);
            TpmHandle akHandle = new TpmHandle(akHandleInt);

            TpmPublic existingObject = null;
            byte[] name, qualifiedName;
            try {
                existingObject = tpm.ReadPublic(akHandle, out name, out qualifiedName);
            } catch (TpmException e) {
                Log.Warning(e, "createAttestationKey: Problem reading public AK handle.");
            }

            if (replace && existingObject != null) {
                // Clear the object
                tpm.EvictControl(TpmRh.Owner, akHandle, akHandle);
                Log.Debug("Removed previous AK.");
            } else if (!replace && existingObject != null) {
                // Do Nothing
                Log.Debug("AK exists at expected handle. Flag is set to not replace it.");
                return;
            }

            // Create a new key and make it persistent at akHandle
            byte[] wellKnownSecret = new byte[] { 00 };
            TpmAlgId nameAlg = TpmAlgId.Sha256;

            SensitiveCreate inSens = new SensitiveCreate(); // do better: generate random, store in file with permissions to read only for admin? store in TPM?
            TpmPublic inPublic = generateAKTemplate(nameAlg); //policyAIK.GetPolicyDigest()
            TpmPublic outPublic;
            CreationData creationData;
            byte[] creationHash;
            TkCreation ticket;

            var policyEK = new PolicyTree(nameAlg);
            policyEK.SetPolicyRoot(new TpmPolicySecret(TpmRh.Endorsement, false, 0, null, null));

            AuthSession sessEK = tpm.StartAuthSessionEx(TpmSe.Policy, nameAlg);
            sessEK.RunPolicy(tpm, policyEK);

            TpmPrivate kAK = tpm[sessEK].Create(ekHandle, inSens, inPublic, null, null, out outPublic,
                                            out creationData, out creationHash, out ticket);

            Log.Debug("New AK PUB Name: " + BitConverter.ToString(outPublic.GetName()));
            Log.Debug("New AK PUB 2BREP: " + BitConverter.ToString(outPublic.GetTpm2BRepresentation()));
            Log.Debug("New AK PUB unique: " + BitConverter.ToString((Tpm2bPublicKeyRsa)(outPublic.unique)));

            tpm.FlushContext(sessEK);

            sessEK = tpm.StartAuthSessionEx(TpmSe.Policy, nameAlg);
            sessEK.RunPolicy(tpm, policyEK);

            TpmHandle hAK = tpm[sessEK].Load(ekHandle, kAK, outPublic);

            tpm.EvictControl(TpmRh.Owner, hAK, akHandle);
            Log.Debug("Created and persisted new AK at handle 0x" + akHandle.handle.ToString("X") + ".");

            tpm.FlushContext(sessEK);
        }

        public Tpm2bDigest[] getPcrList(TpmAlgId pcrBankDigestAlg, uint[] pcrs = null) {
            if (pcrs == null) {
                pcrs = new uint[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 };
            }
            Log.Debug("Retrieving PCR LIST for pcrs: " + string.Join(",", pcrs));

            PcrSelection[] pcrSelection = new PcrSelection[] {
                new PcrSelection(pcrBankDigestAlg, pcrs, (uint)pcrs.Length)
            };
            Tpm2bDigest[] pcrValues = multiplePcrRead(pcrSelection[0]);
            return pcrValues;
        }

        // qualifying data hashed with SHA256, quote set to use RSASSA scheme with SHA256-- TODO: enable usage of ECC and other digest alg
        // if no pcrs are requested (parameter pcrs == null), all pcrs wil be returned. The function does not check if any pcr is available before asking for the quote
        public void getQuote(uint akHandleInt, TpmAlgId pcrBankDigestAlg, byte[] nonce, out CommandTpmQuoteResponse ctqr, uint[] pcrs = null) {
            TpmHandle akHandle = new TpmHandle(akHandleInt);

            if (pcrs == null) {
                pcrs = new uint[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 };
            }
            Log.Debug("Retrieving TPM quote for pcrs: " + string.Join(",", pcrs));


            PcrSelection[] pcrSelection = new PcrSelection[] {
                new PcrSelection(pcrBankDigestAlg, pcrs, (uint)pcrs.Length)
            };

            // for test only
            TpmHash qualifyingData = TpmHash.FromData(TpmAlgId.Sha256, nonce);

            ISignatureUnion localQuoteSig;
            Attest localQuotedInfo = tpm.Quote(akHandle, qualifyingData, new SchemeRsassa(TpmAlgId.Sha256), pcrSelection, out localQuoteSig);
            Tpm2bDigest[] localPcrValues = multiplePcrRead(pcrSelection[0]);

            byte[] name, qualifiedName;
            TpmPublic pub = tpm.ReadPublic(akHandle, out name, out qualifiedName);

            bool verified = pub.VerifyQuote(TpmAlgId.Sha256, pcrSelection, localPcrValues, qualifyingData, localQuotedInfo, localQuoteSig, qualifiedName);
            Log.Debug("Quote " + (verified ? "was" : "was not") + " verified.");
            ctqr = null;
            if (verified) {
                ctqr = new CommandTpmQuoteResponse(localQuotedInfo, localQuoteSig, localPcrValues);
            }
        }

        public Tpm2bDigest[] multiplePcrRead(PcrSelection pcrs) {
            if (pcrs == null) {
                return new Tpm2bDigest[0];
            }

            List<Tpm2bDigest> pcrValues = new List<Tpm2bDigest>();

            const int MAX_NUM_PCRS_PER_READ = 8;  // anticipate TPM has a limit on the number of PCRs read at a time
            Queue<uint> selectedPcrs = new Queue<uint>(pcrs.GetSelectedPcrs());

            while (selectedPcrs.Count() > 0) {
                int numPcrsToRead = (selectedPcrs.Count() > MAX_NUM_PCRS_PER_READ) ? MAX_NUM_PCRS_PER_READ : selectedPcrs.Count();
                
                List<uint> subset = new List<uint>();
                for (int i = 0; i < numPcrsToRead; i++) {
                    subset.Add(selectedPcrs.Dequeue());
                }
                PcrSelection selection = new PcrSelection(pcrs.hash, subset.ToArray());
                PcrSelection[] pcrsIn = { // Need to wrap into an array
                    selection
                };
                PcrSelection[] pcrsOut;
                Tpm2bDigest[] pcrValuesRetrieved;
                uint count = tpm.PcrRead(pcrsIn, out pcrsOut, out pcrValuesRetrieved); // TODO incorporate check on count to handle race condition
                if (pcrValuesRetrieved != null && pcrValuesRetrieved.Length > 0) {
                    pcrValues.AddRange(pcrValuesRetrieved);
                }
            }
            return pcrValues.ToArray();
        }

        public byte[] activateCredential(uint akHandleInt, uint ekHandleInt, byte[] integrityHMAC, byte[] encIdentity, byte[] encryptedSecret) {
            byte[] recoveredSecret = null;

            TpmHandle ekHandle = new TpmHandle(ekHandleInt);
            TpmHandle akHandle = new TpmHandle(akHandleInt);

            IdObject credentialBlob = new IdObject(integrityHMAC, encIdentity);

            TpmAlgId nameAlg = TpmAlgId.Sha256;
            var policyEK = new PolicyTree(nameAlg);
            policyEK.SetPolicyRoot(new TpmPolicySecret(TpmRh.Endorsement, false, 0, null, null));

            AuthSession sessEK = tpm.StartAuthSessionEx(TpmSe.Policy, nameAlg);
            sessEK.RunPolicy(tpm, policyEK);

            AuthSession sessAK = tpm.StartAuthSessionEx(TpmSe.None, nameAlg);
            recoveredSecret = tpm[sessAK, sessEK].ActivateCredential(akHandle, ekHandle, credentialBlob, encryptedSecret);
            Log.Debug("encryptedSecret: " + BitConverter.ToString(encryptedSecret));

            tpm.FlushContext(sessEK);
            tpm.FlushContext(sessAK);

            return recoveredSecret;
        }

        public byte[] GetEventLog() {
            byte[] eventLog = null;
            if (tpm._GetUnderlyingDevice().GetType() == typeof(TbsDevice)) { // if windows TPM
                if (!TbsWrapper.GetEventLog(out eventLog)) {
                    eventLog = null;
                    Log.Debug("Could not retrieve the event log from Tbsi");
                }
            }

            if (eventLog == null) {
                if (tpm._GetUnderlyingDevice().GetType() == typeof(TcpTpmDevice) && RuntimeInformation.IsOSPlatform(OSPlatform.Windows)) { // if TCP TPM on Windows
                                                                                                                                           // attempt to read from the measuredboot log folder
                    string windir = System.Environment.GetEnvironmentVariable("windir");
                    string path = windir + "\\Logs\\MeasuredBoot\\";
                    DirectoryInfo directory = new DirectoryInfo(path);
                    FileInfo mostRecent = directory.GetFiles()
                        .OrderByDescending(f => f.LastWriteTime)
                        .FirstOrDefault();
                    if (mostRecent != null && File.Exists(mostRecent.FullName)) {
                        eventLog = File.ReadAllBytes(mostRecent.FullName);
                    } else {
                        Log.Debug("Windows boot configuration log does not exist at expected location");
                    }
                } else if (tpm._GetUnderlyingDevice().GetType() == typeof(LinuxTpmDevice) || (tpm._GetUnderlyingDevice().GetType() == typeof(TcpTpmDevice) && RuntimeInformation.IsOSPlatform(OSPlatform.Linux))) { // if Linux TPM or TCP TPM on Linux
                    // attempt to read from the binary_bios_measurements file created by the kernel
                    string path = "/sys/kernel/security/tpm0/binary_bios_measurements";
                    if (File.Exists(path)) {
                        eventLog = File.ReadAllBytes(path);
                    } else {
                        Log.Debug("Linux bios measurements log does not exist at expected location");
                    }
                }
            }
            return eventLog;
        }

        private Tpm2 tpmSetupByType(Tpm2Device tpmDevice) {
            try {
                tpmDevice.Connect();
            } catch (AggregateException e) {
                Log.Error(e, "tpmSetupByType: Error connecting to tpmDevice");
                throw e;
            }
            
            Tpm2 tpm = new Tpm2(tpmDevice);
            if (tpmDevice is TcpTpmDevice) {
                //
                // If we are using the simulator, we have to do a few things the
                // firmware would usually do. These actions have to occur after
                // the connection has been established.
                // 
                if (simulator) {
                    uint rc = 0;
                    try {
                        rc = tpm.PcrRead(new PcrSelection[] { new PcrSelection(TpmAlgId.Sha1, new uint[] { 0 }) }, out _, out _);
                    } catch (TpmException e) {
                        if (e.RawResponse == TpmRc.Initialize) {
                            Log.Debug("TPM simulator not initialized. Running startup with clear.");
                            tpmDevice.PowerCycle();
                            tpm.Startup(Su.Clear);
                        } else {
                            Log.Debug("TPM simulator already initialized. Skipping TPM2_Startup.");
                        }
                    }
                }
            } else if (tpmDevice is TbsDevice) {
                /**
                 * For device TPMs on Windows, we have to use Windows Identity
                 */
                // ask windows for owner auth
                byte[] ownerAuth;
                if (TbsWrapper.GetOwnerAuthFromOS(out ownerAuth)) {
                    // if found, ownerauth will be delivered with the tpm object
                    tpm.OwnerAuth = ownerAuth;
                } else {
                    Log.Warning("Could not retrieve owner auth from registry. Trying empty auth.");
                }
            } else if (tpmDevice is LinuxTpmDevice) {

            }
            return tpm;
        }

        //TODO Fix ACA so that I don't have to re-format data in this way
        public static void formatPcrValuesForAca(Tpm2bDigest[] pcrValues, string algName, out string pcrValuesStr) {
            pcrValuesStr = "";
            if (pcrValues != null && pcrValues.Length > 0) {
                pcrValuesStr = algName + " :\n";
            }
            for (int i = 0; i < pcrValues.Length; i++) {
                Tpm2bDigest pcrValue = pcrValues[i];
                pcrValuesStr += "  " + i;
                pcrValuesStr += ((i > 9) ? " " : "  ") + ": ";
                pcrValuesStr += BitConverter.ToString(pcrValue.buffer).Replace("-", "").ToLower().Trim() + "\n";
            }
        }
    }
}

