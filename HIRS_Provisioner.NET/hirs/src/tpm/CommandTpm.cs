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
        public const uint PersistentAkHandle = 0x81000002;
        public const uint LegacyAkHandle = 0x81010002; // before v3. Now the L-2 EK handle

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

        public void Dispose() {
            tpm?.Dispose();
            GC.SuppressFinalize(this);
        }

        ~CommandTpm() {
            tpm?.Dispose();
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
                if (block == null || block.Length < sizeToRead) {
                    Log.Warning("NV read of 0x{Index:X} at offset {Offset} failed with TPM response {Rc}.",
                        nvIndex.handle, offset, tpm._GetLastResponseCode());
                    return null;
                }
                Array.Copy(block, 0, buffer, ptr, sizeToRead);
                offset += sizeToRead;
                ptr += sizeToRead;
            }
            return buffer;
        }

        /**
         * Returns the first DER certificate in the data, or [] if there is none.
         */
        public static byte[] ExtractFirstCertificate(byte[] data) {
            byte[] extracted = [];

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

        public EndorsementKey? AcquireEndorsementKey(EkTemplate template) {
            TpmPublic? inPublic = KeyTemplateCatalog.GenerateEkTemplate(template);
            if (inPublic == null) {
                Log.Debug("No local template for EK template {Template}.", template);
                return null;
            }
            if (!SupportsTemplate(inPublic)) {
                Log.Debug("The TPM does not support the parameters of EK template {Template}; skipping it.", template);
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

        /**
         * True if the key has an old HIRS AK template.
         */
        public static bool IsHirsAttestationKey(TpmPublic pub) {
            return IsAttestationKey(pub, AsymmetricKeyTemplate.Rsa2048Quote);
        }

        /**
         * True if the key has the AK template.
         */
        public static bool IsAttestationKey(TpmPublic pub, AsymmetricKeyTemplate template) {
            TpmPublic expected = KeyTemplateCatalog.GenerateAkTemplate(template);
            TpmPublic withoutKey = new(pub.nameAlg, pub.objectAttributes, pub.authPolicy, pub.parameters, expected.unique);
            return pub.type == expected.type
                && withoutKey.GetTpmRepresentation().SequenceEqual(expected.GetTpmRepresentation());
        }

        /**
         * True if the TPM already has this EK.
         */
        public bool HasEndorsementKey(EkTemplate template) {
            if (KeyTemplateCatalog.GetConventionalEkHandle(template) is { } handle) {
                TpmPublic? existing = ReadPublicArea(handle, out byte[] _, out byte[] _);
                if (existing != null && KeyTemplateCatalog.ClassifyEk(existing) == template) {
                    return true;
                }
            }
            if (KeyTemplateCatalog.GetEkCertificateNvIndex(template) is not { } nvIndex) {
                return false;
            }
            try {
                return tpm.NvReadPublic(new TpmHandle(nvIndex), out byte[] _) != null;
            } catch (TpmException) {
                return false;
            }
        }

        /**
         * Removes old AKs persisted at LegacyAkHandle and PersistentAkHandle.
         * Anything at those handles that is not an AK is left alone.
         * Returns how many were removed.
         */
        public int EvictLegacyAttestationKeys() {
            int removed = 0;
            foreach (uint handle in (uint[])[LegacyAkHandle, PersistentAkHandle]) {
                TpmPublic? existing = ReadPublicArea(handle, out byte[] _, out byte[] _);
                if (existing == null || !KeyTemplateCatalog.SupportedAkTemplates.Any(template => IsAttestationKey(existing, template))) {
                    continue;
                }
                TpmHandle akHandle = new(handle);
                tpm.EvictControl(TpmRh.Owner, akHandle, akHandle);
                Log.Debug("Removed the AK found at 0x" + handle.ToString("X") + ".");
                removed++;
            }
            return removed;
        }

        /**
         * Creates the AK under the SRK and saves it as files.
         */
        public void CreateAttestationKey(string pubPath, string privPath, AsymmetricKeyTemplate template, bool replace) {
            CreateKeyFiles(KeyTemplateCatalog.GenerateAkTemplate(template), pubPath, privPath, replace, "AK", "--replaceAK");
        }

        /**
         * Creates a key under the SRK and saves it as two files: its public area (TPM2B_PUBLIC), and its private area
         * encrypted by the SRK (TPM2B_PRIVATE), which only this TPM can load.
         * Existing files are kept unless replace is set.
         */
        private void CreateKeyFiles(TpmPublic template, string pubPath, string privPath, bool replace, string description,
            string replaceOption) {
            if (!replace && File.Exists(privPath) && File.Exists(pubPath)) {
                Log.Debug("{Description} exists at local file system path. Flag to not replace it is not set.", description);
                if (!CanLoadKeyFiles(pubPath, privPath)) {
                    throw new ProvisioningFailureException(ClientExitCodes.TPM_ERROR,
                        $"This TPM cannot load the saved {description} at {privPath}. The TPM may have been cleared. " +
                        $"Run again with {replaceOption} to create a new {description}.");
                }
                return;
            }

            (TpmPrivate privateArea, TpmPublic outPublic) = WithStorageRootKey(srk => {
                TpmPrivate created = tpm.Create(srk, new SensitiveCreate(), template, null, null,
                    out TpmPublic createdPublic, out CreationData _, out byte[] _, out TkCreation _);
                return (created, createdPublic);
            });
            Log.Debug("New {Description} PUB Name: {Name}", description, BitConverter.ToString(outPublic.GetName()));
            Log.Debug("New {Description} PUB 2BREP: {Public}", description,
                BitConverter.ToString(outPublic.GetTpm2BRepresentation()));

            File.WriteAllBytes(pubPath, new Tpm2bPublic(outPublic));
            File.WriteAllBytes(privPath, privateArea);
            Log.Debug("Created new {Description} at local file system paths.", description);
            Log.Debug("    Pub Path: {0}", pubPath);
            Log.Debug("    Priv Path: {0}", privPath);
        }

        /**
         * Start an auth session for using an EK (ActivateCredential).
         */
        private AuthSession? StartEndorsementKeySession(TpmHandle ekHandle) {
            TpmPublic ek = tpm.ReadPublic(ekHandle, out byte[] _, out byte[] _);
            if (ek.objectAttributes.HasFlag(ObjectAttr.UserWithAuth)) {
                return null;
            }
            return StartEndorsementPolicySession(ek.nameAlg);
        }

        private static SessionBase EndorsementKeyAuth(AuthSession? policySession) {
            return policySession != null ? policySession : Auth.Pw;
        }

        /**
         * A policy session satisfying the EK policy, PolicySecret(TPM_RH_ENDORSEMENT). The caller should clear it.
         */
        private AuthSession StartEndorsementPolicySession(TpmAlgId nameAlg) {
            PolicyTree policyEK = new(nameAlg);
            policyEK.SetPolicyRoot(new TpmPolicySecret(TpmRh.Endorsement, false, 0, null, null));

            AuthSession session = tpm.StartAuthSessionEx(TpmSe.Policy, nameAlg);
            try {
                session.RunPolicy(tpm, policyEK);
            } catch {
                FlushQuietly(session);
                throw;
            }
            return session;
        }

        private void FlushQuietly(TpmHandle handle) {
            tpm.CancelSafeFlushContext(handle);
        }

        private void FlushQuietly(AuthSession? session) {
            if (session != null) {
                tpm.CancelSafeFlushContext(session);
            }
        }

        /**
         * Regenerates the SRK, runs the operation with it, and flushes it.
         * The SRK is not persisted, so whatever the operating system keeps at the TCG SRK handle (0x81000001) is left alone.
         * TPM2_Clear will replace the storage seed. Otherwise, the SRK can be regenerated.
         */
        private T WithStorageRootKey<T>(Func<TpmHandle, T> operation) {
            TpmHandle srk = tpm.CreatePrimary(TpmRh.Owner, new SensitiveCreate(), KeyTemplateCatalog.GenerateSrkTemplate(),
                [], [], out TpmPublic _, out CreationData _, out byte[] _, out TkCreation _);
            try {
                return operation(srk);
            } finally {
                FlushQuietly(srk);
            }
        }

        /**
         * Creates the LDevID under the SRK and saves it as files.
         */
        public void CreateLDevIDKey(string pubPath, string privPath, bool replace) {
            CreateKeyFiles(KeyTemplateCatalog.GenerateLDevIdTemplate(TpmAlgId.Sha256), pubPath, privPath, replace, "LDevID",
                "--replaceLDevID");
        }

        private bool CanLoadKeyFiles(string pubPath, string privPath) {
            try {
                FlushHandle(LoadKeyFromFiles(pubPath, privPath));
                return true;
            } catch (TpmException e) {
                Log.Debug(e, "The key saved at {Path} could not be loaded.", privPath);
                return false;
            }
        }

        /*
         * Loads a key saved to files by CreateAttestationKey or CreateLDevIDKey.
         * It stays loaded after the SRK is flushed.  The caller should clear it.
         */
        public uint LoadKeyFromFiles(string pubPath, string privPath) {
            Tpm2bPublic pub2b = Marshal<Tpm2bPublic>(File.ReadAllBytes(pubPath));
            TpmPrivate priv = Marshal<TpmPrivate>(File.ReadAllBytes(privPath));

            return WithStorageRootKey(srk => tpm.Load(srk, priv, pub2b.publicArea).handle);
        }

        public void FlushHandle(uint handleInt) {
            tpm.FlushContext(new TpmHandle(handleInt));
        }

        // The public area (TPMT_PUBLIC) of a key saved to files
        public byte[] ReadPublicAreaFromFile(string pubPath) {
            Marshaller marshaller = new(File.ReadAllBytes(pubPath), DataRepresentation.Tpm);
            return marshaller.Get<Tpm2bPublic>().publicArea;
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

        // qualifying data hashed with SHA256. The quote is signed with the AK's own scheme: (RSASSA or ECDSA, with its hash)
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

            Attest localQuotedInfo = tpm.Quote(akHandle, qualifyingData, new NullSigScheme(), pcrSelection, out ISignatureUnion localQuoteSig);
            Tpm2bDigest[] localPcrValues = MultiplePcrRead(pcrSelection[0]);

            TpmPublic pub = tpm.ReadPublic(akHandle, out byte[] name, out byte[] qualifiedName);

            // The quote's PCR digest uses the AK's name algorithm
            bool verified = pub.VerifyQuote(pub.nameAlg, pcrSelection, localPcrValues, qualifyingData, localQuotedInfo, localQuoteSig, qualifiedName);
            Log.Debug("Quote " + (verified ? "was" : "was not") + " verified.");
            ctqr = null;
            if (verified) {
                ctqr = new CommandTpmQuoteResponse(localQuotedInfo, localQuoteSig, localPcrValues);
            }
        }

        // For non-restricted signing keys
        // Will need to create a (not null) ticket for restricted keys
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

        /*
         * Check whether the TPM supports algorithms and parameters.
         */
        public bool SupportsTemplate(TpmPublic template) {
            try {
                tpm.TestParms(template.parameters);
                return true;
            } catch (TpmException e) {
                Log.Debug(e, "SupportsTemplate: the TPM does not support these {Type} parameters.", template.type);
                return false;
            }
        }

        private static readonly TpmAlgId[] ShaPcrBanks = [TpmAlgId.Sha1, TpmAlgId.Sha256, TpmAlgId.Sha384, TpmAlgId.Sha512];

        /*
         * The PCR banks the TPM has PCRs allocated in (TPM_CAP_PCRS).
         */
        public IReadOnlyList<TpmAlgId> GetPcrBanks() {
            try {
                tpm.GetCapability(Cap.Pcrs, 0, 1, out ICapabilitiesUnion capData);
                if (capData is PcrSelectionArray allocated) {
                    return allocated.pcrSelections
                        .Where(bank => ShaPcrBanks.Contains(bank.hash) && bank.NumPcrsSelected() > 0)
                        .Select(bank => bank.hash)
                        .ToList();
                }
            } catch (TpmException e) {
                Log.Debug(e, "GetPcrBanks: TPM2_GetCapability failed for TPM_CAP_PCRS.");
            }
            return [];
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
            AuthSession? sessEK = StartEndorsementKeySession(ekHandle);
            AuthSession? sessAK = null;
            try {
                sessAK = tpm.StartAuthSessionEx(TpmSe.None, nameAlg);
                recoveredSecret = tpm[sessAK, EndorsementKeyAuth(sessEK)].ActivateCredential(akHandle, ekHandle, credentialBlob, encryptedSecret);
                Log.Debug("encryptedSecret: " + BitConverter.ToString(encryptedSecret));
            } finally {
                FlushQuietly(sessEK);
                if (sessAK != null) {
                    FlushQuietly(sessAK);
                }
            }

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

