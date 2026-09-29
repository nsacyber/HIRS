using Hirs.Pb;
using Serilog;
using System.Runtime.InteropServices;
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

        public const uint DefaultL1EkHandle = 0x81010001;
        // Was previously (incorrectly) set equal to DefaultL1EkHandle. Per the TCG
        // "Registry of Reserved TPM 2.0 Handles and Localities", 0x81010002 is the
        // conventional persistent handle for the ECC NIST P256 (L-2) EK.
        public const uint DefaultL2EkHandle = 0x81010002;
        public const uint DefaultAkHandle = 0x81000002;
        public const uint DefaultSrkHandle = 0x81000001;

        private readonly Tpm2 tpm;

        private readonly bool simulator;

        private List<AuthSession> sessionTracking = new List<AuthSession>();

        /**
         * For TCP TpmDevices
         */
        public CommandTpm(bool sim, string ip, int port) {
            simulator = sim;
            Tpm2Device tpmDevice = new TcpTpmDevice(ip, port);
            tpm = TpmSetupByType(tpmDevice);
        }

        /**
         * For a TPM device on Linux and Windows
         */
        public CommandTpm(Devices dev) {
            Tpm2Device tpmDevice = null;
            switch (dev) {
                case Devices.NIX:
                    // LinuxTpmDevice will first try to connect tpm2-abrmd and second try to connect directly to device
                    StringWriter writer = new();
                    Console.SetOut(writer);
                    // The LinuxTpmDevice will print to Console/Stdout an error when it cannot find a resource manager
                    // This will redirect the Console messages
                    tpmDevice = new LinuxTpmDevice();
                    // Reset the Console messages
                    Console.SetOut(new StreamWriter(Console.OpenStandardOutput()));
                    break;
                case Devices.WIN:
                    tpmDevice = new TbsDevice();
                    break;
                default:
                    Log.Error("Unknown option selected in CommandTpm(Devices) constructor.");
                    break;
            }
            tpm = TpmSetupByType(tpmDevice);
        }

        public CommandTpm(Tpm2 tpm) {
            this.tpm = tpm;
        }

        ~CommandTpm() {
            tpm.Dispose();
        }
        
        public bool IsTpmPresent() {
            if (tpm._GetUnderlyingDevice() == null) {
                return false;
            }
            try {
                tpm.GetCapability(Cap.TpmProperties, (uint)Pt.FamilyIndicator, 1, out ICapabilitiesUnion _);
                return true;
            } catch (TpmException) {
                return false;
            }
        }

        public byte[] GetCertificateFromNvIndex(uint index) {
            Log.Debug("GetCertificateFromNvIndex 0x" + index.ToString("X"));
            byte[] certificate = [];
            
            TpmHandle nvHandle = new(index);
            try {
                NvPublic obj = tpm.NvReadPublic(nvHandle, out byte[] _); // out param not used for this function. have to collect from NvReadPublic. 
                if (obj != null) {
                    byte[] indexData = NvBufferedRead(TpmHandle.RhOwner, nvHandle, obj.dataSize, 0);
                    if (indexData is not null and not []) {
                        // the nvIndex could contain random fill around the certificate
                        certificate = ExtractFirstCertificate(indexData);
                        if (certificate is []) {
                            Log.Debug("GetCertificateFromNvIndex: No certificate found within data at index.");
                        } else {
                            Log.Debug("GetCertificateFromNvIndex: Read: " + BitConverter.ToString(certificate));
                        }
                    } else {
                        Log.Debug("GetCertificateFromNvIndex: Could not read any data.");
                    }
                } else {
                    Log.Debug("GetCertificateFromNvIndex: Nothing found at index: " + index);
                }
            } catch (TpmException e) {
                Log.Debug(e, "GetCertificateFromNvIndex TPM error");
            }
            return certificate;
        }

        private byte[] NvBufferedRead(TpmHandle authHandle, TpmHandle nvIndex, ushort size, ushort offset) {
            ushort maxReadSize = 256;
            byte[] buffer = new byte[size];

            ushort ptr = 0;
            while (offset < size) {
                int q = Math.DivRem(size - offset, maxReadSize, out int r);
                ushort sizeToRead = q > 0 ? maxReadSize : (ushort)r;
                byte[] block = tpm.NvRead(authHandle, nvIndex, sizeToRead, offset);
                Array.Copy(block, 0, buffer, ptr, sizeToRead);
                offset += sizeToRead;
                ptr += sizeToRead;
            }
            return buffer;
        }

        private static byte[] ExtractFirstCertificate(byte[] data) {
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
                if (size > 0 && pos + size <= data.Length) {
                    extracted = new byte[size];
                    Array.Copy(data, pos, extracted, 0, size);
                }
            }

            return extracted;
        }

        // allows client to access the readpublic function
        public TpmPublic ReadPublicArea(uint handleInt, out byte[] name, out byte[] qualifiedName) {
            TpmHandle handle = new(handleInt);
            TpmPublic obj = null;
            name = null;
            qualifiedName = null;
            try {
                obj = tpm.ReadPublic(handle, out byte[] localName, out byte[] localQualifiedName);
                name = localName;
                qualifiedName = localQualifiedName;
            } catch {
                // Don't think I need an exception here. Let the calling method throw if needed.
            }
            return obj;
        }

        public static TpmPublic GenerateEKTemplateL1() {
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
            RsaParms rsa = new(new SymDefObject(TpmAlgId.Aes, 128, TpmAlgId.Cfb), new NullAsymScheme(), 2048, 0);
            // unique buffer must be filled with 0 for the EK Template L-1.
            byte[] zero256 = new byte[256];
            Array.Fill<byte>(zero256, 0x00);
            Tpm2bPublicKeyRsa unique = new(zero256);
            TpmPublic inPublic = new(nameAlg, attributes, auth_policy, rsa, unique);
            return inPublic;
        }
        
        public static TpmPublic GenerateEKTemplateL2() {
            TpmAlgId nameAlg = TpmAlgId.Sha256;
            ObjectAttr attributes = ObjectAttr.FixedTPM | ObjectAttr.FixedParent | ObjectAttr.SensitiveDataOrigin | ObjectAttr.AdminWithPolicy | ObjectAttr.Restricted | ObjectAttr.Decrypt;
            byte[] auth_policy = { // Template L-2
                0x83, 0x71, 0x97, 0x67, 0x44, 0x84,
                0xB3, 0xF8, 0x1A, 0x90, 0xCC, 0x8D,
                0x46, 0xA5, 0xD7, 0x24, 0xFD, 0x52,
                0xD7, 0x6E, 0x06, 0x52, 0x0B, 0x64,
                0xF2, 0xA1, 0xDA, 0x1B, 0x33, 0x14,
                0x69, 0xAA
            };
            // ASYM: ECC NIST P256 with NULL scheme, SYM: AES-128 with CFB mode
            EccParms ecc = new(new SymDefObject(TpmAlgId.Aes, 128, TpmAlgId.Cfb), new NullAsymScheme(), EccCurve.TpmEccNistP256, new NullKdfScheme());
            // unique buffer must be filled with 0 for the EK Template L-1.
            byte[] zero16 = new byte[16];
            Array.Fill<byte>(zero16, 0x00);
            EccPoint point = new() {
                x = zero16,
                y = zero16
            };
            TpmPublic inPublic = new(nameAlg, attributes, auth_policy, ecc, point);
            return inPublic;
        }

        public static TpmPublic GenerateSRKTemplateL1() {
            TpmAlgId nameAlg = TpmAlgId.Sha256;
            ObjectAttr attributes = ObjectAttr.FixedTPM | ObjectAttr.FixedParent | ObjectAttr.SensitiveDataOrigin | ObjectAttr.UserWithAuth | ObjectAttr.Restricted | ObjectAttr.Decrypt | ObjectAttr.NoDA;
            byte[] auth_policy = null;
            // ASYM: RSA 2048 with NULL scheme, SYM: AES-128 with CFB mode
            RsaParms rsa = new(new SymDefObject(TpmAlgId.Aes, 128, TpmAlgId.Cfb), new NullAsymScheme(), 2048, 0);
            // unique buffer must be filled with 0 for the EK Template L-1.
            byte[] zero256 = new byte[256];
            Array.Fill<byte>(zero256, 0x00);
            Tpm2bPublicKeyRsa unique = new(zero256);
            TpmPublic inPublic = new(nameAlg, attributes, auth_policy, rsa, unique);
            return inPublic;
        }

        /**
         * The TPM object template for an EK template this provisioner can generate, or null.
         * See KeyTemplateCatalog.SupportedEkTemplates
         */
        public static TpmPublic? GenerateEkTemplate(EkTemplate template) {
            return template switch {
                EkTemplate.EkL1 => GenerateEKTemplateL1(),
                EkTemplate.EkL2 => GenerateEKTemplateL2(),
                _ => null
            };
        }

        public EndorsementKey? AcquireEndorsementKey(EkTemplate template) {
            TpmPublic? inPublic = GenerateEkTemplate(template);
            if (inPublic == null) {
                Log.Debug("No local template for EK template {Template}.", template);
                return null;
            }
            if (!SupportsAlgorithm(inPublic.type)) {
                Log.Debug("The TPM does not support {Alg}; skipping EK template {Template}.", inPublic.type, template);
                return null;
            }

            // Verify what is at the handle
            uint? conventionalHandle = KeyTemplateCatalog.GetConventionalEkHandle(template);
            bool conventionalHandleFree = false;
            if (conventionalHandle is { } handleInt) {
                TpmPublic? existing = ReadPublicArea(handleInt, out byte[] _, out byte[] _);
                if (existing == null) {
                    conventionalHandleFree = true;
                } else if (KeyTemplateCatalog.ClassifyEk(existing) == template) {
                    Log.Debug("EK {Template} already exists at 0x{Handle:X}.", template, handleInt);
                    return new EndorsementKey(template, existing, handleInt, true, null);
                } else {
                    Log.Warning("Object at 0x{Handle:X}, the conventional handle for EK {Template}, is not that EK " +
                                "(looks like EK {Found}, or key type {Asym}). It will be left alone and the EK will " +
                                "be regenerated into a transient handle instead.", handleInt, template,
                        KeyTemplateCatalog.ClassifyEk(existing), KeyTemplateCatalog.ClassifyAsymmetric(existing));
                }
            }

            TpmHandle transientHandle;
            TpmPublic outPublic;
            try {
                transientHandle = tpm.CreatePrimary(TpmRh.Endorsement, new SensitiveCreate(), inPublic, [], [],
                    out outPublic, out CreationData _, out byte[] _, out TkCreation _);
            } catch (TpmException e) {
                Log.Debug(e, "The TPM could not create EK {Template}.", template);
                return null;
            }
            Log.Debug("New EK {Template} PUB Name: {Name}", template, BitConverter.ToString(outPublic.GetName()));

            if (conventionalHandleFree && conventionalHandle is { } persistentHandle) {
                try {
                    tpm.EvictControl(TpmRh.Owner, transientHandle, new TpmHandle(persistentHandle));
                    tpm.FlushContext(transientHandle);
                    Log.Debug("Made EK {Template} persistent at 0x{Handle:X}.", template, persistentHandle);
                    return new EndorsementKey(template, outPublic, persistentHandle, true, null);
                } catch (TpmException e) {
                    Log.Warning(e, "Could not persist EK {Template} at 0x{Handle:X}; using a transient EK.",
                        template, persistentHandle);
                }
            }

            TpmHandle toRelease = transientHandle;
            return new EndorsementKey(template, outPublic, transientHandle.handle, false,
                () => tpm.FlushContext(toRelease));
        }

        private static RsaParms AkRsaParms() {
            TpmAlgId digestAlg = TpmAlgId.Sha256;
            RsaParms parms = new(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null), new SchemeRsassa(digestAlg), 2048, 0);
            return parms;
        }

        private static ObjectAttr AkAttributes() {
            ObjectAttr attrib = ObjectAttr.Restricted | ObjectAttr.Sign | ObjectAttr.FixedParent | ObjectAttr.FixedTPM
                    | ObjectAttr.SensitiveDataOrigin | ObjectAttr.UserWithAuth;
            return attrib;
        }   

        private static TpmPublic GenerateAKTemplate(TpmAlgId nameAlg) {
            RsaParms rsa = AkRsaParms();
            ObjectAttr attributes = AkAttributes();
            TpmPublic inPublic = new(nameAlg, attributes, null, rsa, new Tpm2bPublicKeyRsa());
            return inPublic;
        }

        private static RsaParms LDevIDRSAParms() {
            TpmAlgId digestAlg = TpmAlgId.Sha256;
            RsaParms parms = new(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null), null, 2048, 0);
            return parms;
        }

        private static ObjectAttr LDevIDAttributes() {
            ObjectAttr attrib =  ObjectAttr.Sign | ObjectAttr.FixedParent | ObjectAttr.FixedTPM
                    | ObjectAttr.SensitiveDataOrigin | ObjectAttr.UserWithAuth;
            return attrib;
        }

        private static TpmPublic GenerateLDevIDTemplate(TpmAlgId nameAlg) {
            RsaParms rsa = LDevIDRSAParms();
            ObjectAttr attributes = LDevIDAttributes();
            TpmPublic inPublic = new(nameAlg, attributes, null, rsa, new Tpm2bPublicKeyRsa());
            return inPublic;
        }

        public void CreateAttestationKey(uint ekHandleInt, uint akHandleInt, bool replace) {
            TpmHandle ekHandle = new(ekHandleInt);
            TpmHandle akHandle = new(akHandleInt);

            TpmPublic existingObject = null;
            try {
                existingObject = tpm.ReadPublic(akHandle, out byte[] name, out byte[] qualifiedName);
            } catch { }

            if (!replace && existingObject != null) {
                // Do Nothing
                Log.Debug("AK exists at expected handle. Flag to not replace the AK is set in the settings file.");
                return;
            } else if (replace && existingObject != null) {
                // Clear the object and continue
                tpm.EvictControl(TpmRh.Owner, akHandle, akHandle);
                Log.Debug("Removed previous AK.");
            }  

            // Create a new key and make it persistent at akHandle
            TpmAlgId nameAlg = TpmAlgId.Sha256;

            SensitiveCreate inSens = new();
            TpmPublic inPublic = GenerateAKTemplate(nameAlg);

            var policyEK = new PolicyTree(nameAlg);
            policyEK.SetPolicyRoot(new TpmPolicySecret(TpmRh.Endorsement, false, 0, null, null));

            AuthSession sessEK = tpm.StartAuthSessionEx(TpmSe.Policy, nameAlg);
            sessEK.RunPolicy(tpm, policyEK);

            TpmPrivate kAK = tpm[sessEK].Create(ekHandle, inSens, inPublic, null, null, out TpmPublic outPublic,
                                            out CreationData creationData, out byte[] creationHash, out TkCreation ticket);

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

        public void CreateStorageRootKey(uint srkHandleInt) {
            TpmHandle srkHandle = new(srkHandleInt);

            TpmPublic existingObject;
            try {
                existingObject = tpm.ReadPublic(srkHandle, out byte[] name, out byte[] qualifiedName);
                Log.Debug("SRK already exists.");
                return;
            } catch (TpmException) {
                Log.Debug("Verified SRK does not exist at expected handle. Creating SRK.");
            }

            SensitiveCreate inSens = new(); // key password (no params = no key password)
            TpmPublic inPublic = CommandTpm.GenerateSRKTemplateL1(); 

            TpmHandle newTransientSrkHandle = tpm.CreatePrimary(TpmRh.Owner, inSens, inPublic,
                                                new byte[] { }, new PcrSelection[] { }, out TpmPublic outPublic,
                                                out CreationData creationData, out byte[] creationHash, out TkCreation ticket);

            Log.Debug("New SRK Handle: " + BitConverter.ToString(newTransientSrkHandle));
            Log.Debug("New SRK PUB Name: " + BitConverter.ToString(outPublic.GetName()));
            Log.Debug("New SRK PUB 2BREP: " + BitConverter.ToString(outPublic.GetTpm2BRepresentation()));

            // Make the object persistent
            tpm.EvictControl(TpmRh.Owner, newTransientSrkHandle, srkHandle);
            Log.Debug("Successfully made the new SRK persistent at handle " + BitConverter.ToString(srkHandle) + ".");

            tpm.FlushContext(newTransientSrkHandle);
            Log.Debug("Flushed the context for the transient SRK.");
        }

        public void CreateLDevIDKey(uint srkHandleInt, string pubPath, string privPath, bool replace) {
            TpmHandle srkHandle = new(srkHandleInt);

            if (!replace && File.Exists(privPath) && File.Exists(pubPath)) {
                // Do Nothing
                Log.Debug("LDevID exists at local file system path. Flag to not replace the LDevID is set in the settings file.");
                return;
            }

            // Create a new transient key
            TpmAlgId nameAlg = TpmAlgId.Sha256;

            SensitiveCreate inSens = new();
            TpmPublic inPublic = GenerateLDevIDTemplate(nameAlg);

            TpmPrivate kLDevID = tpm.Create(srkHandle, inSens, inPublic, null, null, out TpmPublic outPublic,
                                            out CreationData creationData, out byte[] creationHash, out TkCreation ticket);

            Log.Debug("New LDevID PUB Name: " + BitConverter.ToString(outPublic.GetName()));
            Log.Debug("New LDevID PUB 2BREP: " + BitConverter.ToString(outPublic.GetTpm2BRepresentation()));
            Log.Debug("New LDevID PUB unique: " + BitConverter.ToString((Tpm2bPublicKeyRsa)(outPublic.unique)));

            Tpm2bPublic ldevidPublic = new Tpm2bPublic(outPublic);

            File.WriteAllBytes(pubPath, ldevidPublic);
            File.WriteAllBytes(privPath, kLDevID);
            Log.Debug("Created new LDevID at local file system paths.");
            Log.Debug("    LDevID Pub Path: {0}", pubPath);
            Log.Debug("    LDevID Priv Path: {0}", privPath);
        }

        /*
         * Loads LDevID previously saved to file by CreateLDevIDKey
         */
        public uint LoadLDevIDKey(uint srkHandleInt, string pubPath, string privPath) {
            TpmHandle srkHandle = new(srkHandleInt);

            Tpm2bPublic pub2b = Marshal<Tpm2bPublic>(File.ReadAllBytes(pubPath));
            TpmPrivate priv = Marshal<TpmPrivate>(File.ReadAllBytes(privPath));

            TpmHandle loaded = tpm.Load(srkHandle, priv, pub2b.publicArea);
            return loaded.handle;
        }

        public void FlushHandle(uint handleInt) {
            tpm.FlushContext(new TpmHandle(handleInt));
        }

        public byte[] ConvertLDevIDPublic(string ldevidPubPath) {
            byte[] ldevidPubBytes = File.ReadAllBytes(ldevidPubPath);
            var marshaller = new Marshaller(ldevidPubBytes, DataRepresentation.Tpm);
            Tpm2bPublic ldevidPublic = marshaller.Get<Tpm2bPublic>();
            return ldevidPublic.publicArea;
        }

        public Tpm2bDigest[] GetPcrList(TpmAlgId pcrBankDigestAlg, uint[] pcrs = null) {
            if (pcrs == null) {
                pcrs = new uint[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 };
            }
            Log.Debug("Retrieving PCR LIST for pcrs: " + string.Join(",", pcrs));

            PcrSelection[] pcrSelection = new PcrSelection[] {
                new PcrSelection(pcrBankDigestAlg, pcrs, (uint)pcrs.Length)
            };
            Tpm2bDigest[] pcrValues = MultiplePcrRead(pcrSelection[0]);
            return pcrValues;
        }

        // qualifying data hashed with SHA256, quote set to use RSASSA scheme with SHA256-- TODO: enable usage of ECC and other digest alg
        // if no pcrs are requested (parameter pcrs == null), all pcrs wil be returned. The function does not check if any pcr is available before asking for the quote
        public void GetQuote(uint akHandleInt, TpmAlgId pcrBankDigestAlg, byte[] nonce, out CommandTpmQuoteResponse ctqr, uint[] pcrs = null) {
            TpmHandle akHandle = new(akHandleInt);

            if (pcrs == null) {
                pcrs = new uint[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 };
            }
            Log.Debug("Retrieving TPM quote for pcrs: " + string.Join(",", pcrs));


            PcrSelection[] pcrSelection = new PcrSelection[] {
                new PcrSelection(pcrBankDigestAlg, pcrs, (uint)pcrs.Length)
            };

            // for test only
            TpmHash qualifyingData = TpmHash.FromData(TpmAlgId.Sha256, nonce);

            Attest localQuotedInfo = tpm.Quote(akHandle, qualifyingData, new SchemeRsassa(TpmAlgId.Sha256), pcrSelection, out ISignatureUnion localQuoteSig);
            Tpm2bDigest[] localPcrValues = MultiplePcrRead(pcrSelection[0]);

            TpmPublic pub = tpm.ReadPublic(akHandle, out byte[] name, out byte[] qualifiedName);

            bool verified = pub.VerifyQuote(TpmAlgId.Sha256, pcrSelection, localPcrValues, qualifyingData, localQuotedInfo, localQuoteSig, qualifiedName);
            Log.Debug("Quote " + (verified ? "was" : "was not") + " verified.");
            ctqr = null;
            if (verified) {
                ctqr = new CommandTpmQuoteResponse(localQuotedInfo, localQuoteSig, localPcrValues);
            }
        }

        /*
         * Raw TPM2_Sign proof-of-possession signature. Only valid for a
         * non-restricted signing key (e.g. LDevID) — a restricted key (e.g. AK)
         * cannot sign an externally-supplied digest without a same-TPM hash
         * validation ticket (TPM2_Hash), which this method does not attempt.
         * Callers pass an unrestricted key's handle; a restricted key will
         * fail with TPM_RC_TICKET/TPM_RC_ATTRIBUTES from the TPM itself.
         */
        public byte[] Sign(uint keyHandleInt, byte[] digest, TpmAlgId hashAlg) {
            TpmHandle keyHandle = new(keyHandleInt);

            // TkHashcheck.Null() marks the digest as not TPM-generated, which is
            // only accepted by a non-restricted signing key.
            TkHashcheck nullTicket = new(TpmRh.Null, Array.Empty<byte>());

            ISignatureUnion signature = tpm.Sign(keyHandle, digest, new SchemeRsassa(hashAlg), nullTicket);
            if (signature is SignatureRsassa rsaSig) {
                return rsaSig.sig;
            }
            if (signature is SignatureEcdsa eccSig) {
                Marshaller m = new();
                m.Put(eccSig.signatureR, "signatureR");
                m.Put(eccSig.signatureS, "signatureS");
                return m.GetBytes();
            }

            Log.Debug("Sign: unrecognized signature union type " + signature?.GetType());
            return Array.Empty<byte>();
        }

        /*
         * Queries TPM_CAP_ALGS via TPM2_GetCapability to determine whether the TPM supports the given algorithm.
         */
        public bool SupportsAlgorithm(TpmAlgId algId) {
            try {
                tpm.GetCapability(Cap.Algs, (uint)algId, 1, out ICapabilitiesUnion capData);
                if (capData is AlgPropertyArray algProperties) {
                    foreach (AlgProperty prop in algProperties.algProperties) {
                        if (prop.alg == algId) {
                            return true;
                        }
                    }
                }
            } catch (TpmException e) {
                Log.Debug(e, "SupportsAlgorithm: TPM2_GetCapability failed for algorithm 0x" + ((ushort)algId).ToString("X"));
            }
            return false;
        }

        public Tpm2bDigest[] MultiplePcrRead(PcrSelection pcrs) {
            if (pcrs == null) {
                return Array.Empty<Tpm2bDigest>();
            }

            List<Tpm2bDigest> pcrValues = new();

            const int MAX_NUM_PCRS_PER_READ = 8;  // anticipate TPM has a limit on the number of PCRs read at a time
            Queue<uint> selectedPcrs = new(pcrs.GetSelectedPcrs());

            while (selectedPcrs.Count() > 0) {
                int numPcrsToRead = (selectedPcrs.Count > MAX_NUM_PCRS_PER_READ) ? MAX_NUM_PCRS_PER_READ : selectedPcrs.Count;

                List<uint> subset = new();
                for (int i = 0; i < numPcrsToRead; i++) {
                    subset.Add(selectedPcrs.Dequeue());
                }
                PcrSelection selection = new(pcrs.hash, subset.ToArray());
                PcrSelection[] pcrsIn = { // Need to wrap into an array
                    selection
                };
                uint count = tpm.PcrRead(pcrsIn, out PcrSelection[] pcrsOut, out Tpm2bDigest[] pcrValuesRetrieved); // TODO incorporate check on count to handle race condition
                if (pcrValuesRetrieved != null && pcrValuesRetrieved.Length > 0) {
                    pcrValues.AddRange(pcrValuesRetrieved);
                }
            }
            return pcrValues.ToArray();
        }

        public byte[] ActivateCredential(uint akHandleInt, uint ekHandleInt, byte[] integrityHMAC, byte[] encIdentity, byte[] encryptedSecret) {
            byte[] recoveredSecret;

            TpmHandle ekHandle = new(ekHandleInt);
            TpmHandle akHandle = new(akHandleInt);

            IdObject credentialBlob = new(integrityHMAC, encIdentity);

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
        
        public byte[] ActivateCredential(uint akHandleInt, uint ekHandleInt, byte[] credentialBlob, byte[] encryptedSecret) {
            if (!CanMarshal<Tpm2bIdObject>(credentialBlob)) {
                Log.Debug("Credential ID elements could not be extracted from the ACA's response.");
                return [];
            }
            if (!CanMarshal<Tpm2bEncryptedSecret>(encryptedSecret)) {
                Log.Debug("Encrypted secret elements could not be extracted from the ACA's response.");
                return [];
            }

            Tpm2bIdObject credentialBlobObj = Marshal<Tpm2bIdObject>(credentialBlob);
            Tpm2bEncryptedSecret encryptedSecretObj = Marshal<Tpm2bEncryptedSecret>(encryptedSecret);
            Log.Debug("Prepared values to give to activateCredential.");
            Log.Debug("    integrityHMAC: " + BitConverter.ToString(credentialBlobObj.credential.integrityHMAC));
            Log.Debug("    encIdentity: " + BitConverter.ToString(credentialBlobObj.credential.encIdentity));
            Log.Debug("    encryptedSecret: " + BitConverter.ToString(encryptedSecretObj.secret));

            return ActivateCredential(akHandleInt, ekHandleInt, credentialBlobObj.credential.integrityHMAC, credentialBlobObj.credential.encIdentity, encryptedSecretObj.secret);
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
                    DirectoryInfo directory = new(path);
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

        private Tpm2 TpmSetupByType(Tpm2Device tpmDevice) {
            try {
                tpmDevice.Connect();
            } catch (AggregateException e) {
                Log.Error(e, "tpmSetupByType: Error connecting to tpmDevice");
                throw e;
            }
            
            Tpm2 tpm = new(tpmDevice);
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
        public static void FormatPcrValuesForAca(Tpm2bDigest[] pcrValues, string algName, out string pcrValuesStr) {
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

        public static bool CanMarshal<T>(byte[] data) {
            bool result = false;
            try {
                Marshaller m = new(data, DataRepresentation.Tpm);
                m.Get<T>();
                result = true;
            } catch (Exception e) {
                Log.Error("Error marshalling data: " + e.Message);
            }
            return result;
        }
        
        /**
         * Run CanMarshal first to test if marshalling will work
         */
        public static T Marshal<T>(byte[] data) {
            Marshaller m = new(data, DataRepresentation.Tpm);
            return m.Get<T>();
        }
    }
}

