using hirs;
using NUnit.Framework;
using System.Net.Sockets;
using System.Reflection;
using System.Security.Cryptography;
using Tpm2Lib;

namespace hirsTest.tpm {
    /*
     * Unit tests with a TPM simulator
     */
    public class CommandTpmSimulatorTests {
        private CommandTpm tpm;
        private string tempDir;

        // One connection for all tests:
        //   the simulator serves one client at a time
        [OneTimeSetUp]
        public void ConnectSimulator() {
            string[] hostPort = CommandTpm.DefaultSimulatorNamePort.Split(':');
            string host = hostPort[0];
            int port = int.Parse(hostPort[1]);

            using (TcpClient probe = new()) {
                IAsyncResult ar = probe.BeginConnect(host, port, null, null);
                if (!ar.AsyncWaitHandle.WaitOne(TimeSpan.FromSeconds(2)) || !probe.Connected) {
                    Assert.Ignore("No TCP listener reachable at " + CommandTpm.DefaultSimulatorNamePort + ".");
                }
            }

            // Run it in a background thread
            Task<CommandTpm> connectTask = Task.Run(() => new CommandTpm(true, host, port));
            if (!connectTask.Wait(TimeSpan.FromSeconds(5))) {
                Assert.Ignore("TPM simulator did not start at " + CommandTpm.DefaultSimulatorNamePort);
            }
            try {
                tpm = connectTask.Result;
            } catch (Exception e) {
                Assert.Ignore("Could not complete the TPM simulator handshake at " +
                    CommandTpm.DefaultSimulatorNamePort + ": " + e.Message);
            }
        }

        [OneTimeTearDown]
        public void DisconnectSimulator() {
            tpm?.Dispose();
        }

        [SetUp]
        public void SetUp() {
            tempDir = Path.Combine(Path.GetTempPath(), "hirsTest-" + Guid.NewGuid());
            Directory.CreateDirectory(tempDir);
        }

        [TearDown]
        public void TearDown() {
            if (Directory.Exists(tempDir)) {
                Directory.Delete(tempDir, true);
            }
        }

        private uint CreateAndLoadAk(Hirs.Pb.AsymmetricKeyTemplate template, out byte[] akName) {
            string pubPath = Path.Combine(tempDir, "ak.pub");
            string privPath = Path.Combine(tempDir, "ak.priv");
            tpm.CreateAttestationKey(pubPath, privPath, template, replace: true);
            uint handle = tpm.LoadKeyFromFiles(pubPath, privPath);
            tpm.ReadPublicArea(handle, out akName, out byte[] _);
            return handle;
        }

        private void PersistAk(uint loadedHandle, uint persistentHandle) {
            Tpm2 raw = (Tpm2)typeof(CommandTpm).GetField("tpm", BindingFlags.NonPublic | BindingFlags.Instance)!.GetValue(tpm)!;
            if (tpm.ReadPublicArea(persistentHandle, out byte[] _, out byte[] _) != null) {
                raw.EvictControl(TpmRh.Owner, new TpmHandle(persistentHandle), new TpmHandle(persistentHandle));
            }
            raw.EvictControl(TpmRh.Owner, new TpmHandle(loadedHandle), new TpmHandle(persistentHandle));
        }

        [Test]
        public void TestSignRoundTripWithNonRestrictedKey() {

            string pubPath = Path.Combine(tempDir, "ldevid.pub");
            string privPath = Path.Combine(tempDir, "ldevid.priv");
            tpm.CreateLDevIDKey(pubPath, privPath, replace: true);

            uint keyHandle = tpm.LoadKeyFromFiles(pubPath, privPath);
            try {
                byte[] message = [.. "proof-of-possession nonce"u8];
                byte[] digest = SHA256.HashData(message);

                byte[] signature = tpm.Sign(keyHandle, digest, TpmAlgId.Sha256);
                Assert.That(signature, Is.Not.Null.And.Not.Empty);

                TpmPublic ldevidPub = tpm.ReadPublicArea(keyHandle, out byte[] _, out byte[] _);
                bool verified = ldevidPub.VerifySignatureOverHash(digest, new SignatureRsassa(TpmAlgId.Sha256, signature));
                Assert.That(verified, Is.True, "LDevID signature did not verify against its own public area.");
            } finally {
                tpm.FlushHandle(keyHandle);
            }
        }

        [Test]
        public void TestSignFailsForRestrictedKey() {
            uint akHandle = CreateAndLoadAk(Hirs.Pb.AsymmetricKeyTemplate.Rsa2048Quote, out byte[] _);
            try {
                byte[] digest = SHA256.HashData([.. "should not be signable"u8]);

                Assert.Throws<TpmException>(() => tpm.Sign(akHandle, digest, TpmAlgId.Sha256));
            } finally {
                tpm.FlushHandle(akHandle);
            }
        }

        [TestCase(Hirs.Pb.EkTemplate.EkL1)]
        [TestCase(Hirs.Pb.EkTemplate.EkL2)]
        [TestCase(Hirs.Pb.EkTemplate.EkH1)]
        [TestCase(Hirs.Pb.EkTemplate.EkH2)]
        [TestCase(Hirs.Pb.EkTemplate.EkH3)]
        public void TestActivateCredentialRecoversSecret(Hirs.Pb.EkTemplate template) {
            using EndorsementKey? ek = tpm.AcquireEndorsementKey(template);
            Assume.That(ek, Is.Not.Null, $"The simulator cannot create EK {template}.");
            uint akHandle = CreateAndLoadAk(Hirs.Pb.AsymmetricKeyTemplate.Rsa2048Quote, out byte[] akName);
            try {
                byte[] secret = RandomNumberGenerator.GetBytes(32);
                IdObject idObject = ek!.Public.CreateActivationCredentials(secret, akName, out byte[] encryptedSecret);
                byte[] credentialBlob = new Tpm2bIdObject(idObject).GetTpmRepresentation();
                byte[] encryptedSecretBlob = new Tpm2bEncryptedSecret(encryptedSecret).GetTpmRepresentation();

                byte[] recovered = tpm.ActivateCredential(akHandle, ek.Handle, credentialBlob, encryptedSecretBlob);

                Assert.That(recovered, Is.EqualTo(secret));
            } finally {
                tpm.FlushHandle(akHandle);
            }
        }

        [TestCase(Hirs.Pb.AsymmetricKeyTemplate.Rsa2048Quote)]
        [TestCase(Hirs.Pb.AsymmetricKeyTemplate.Rsa3072Quote)]
        [TestCase(Hirs.Pb.AsymmetricKeyTemplate.EccP256Quote)]
        [TestCase(Hirs.Pb.AsymmetricKeyTemplate.EccP384Quote)]
        [TestCase(Hirs.Pb.AsymmetricKeyTemplate.EccP521Quote)]
        public void TestAkTemplateQuotesAndActivates(Hirs.Pb.AsymmetricKeyTemplate akTemplate) {
            Assume.That(tpm.SupportsTemplate(KeyTemplateCatalog.GenerateAkTemplate(akTemplate)), Is.True,
                $"The simulator does not support {akTemplate}.");
            using EndorsementKey ek = tpm.AcquireEndorsementKey(Hirs.Pb.EkTemplate.EkL1)!;
            uint akHandle = CreateAndLoadAk(akTemplate, out byte[] akName);
            try {
                TpmPublic ak = tpm.ReadPublicArea(akHandle, out byte[] _, out byte[] _);
                Assert.That(CommandTpm.IsAttestationKey(ak, akTemplate), Is.True);

                byte[] nonce = RandomNumberGenerator.GetBytes(20);
                tpm.GetQuote(akHandle, TpmAlgId.Sha256, nonce, out CommandTpmQuoteResponse ctqr);
                Assert.That(ctqr, Is.Not.Null, "The quote did not verify.");
                CommandTpmQuoteResponse.formatQuoteInfoSigForAca(ctqr.quoted, ctqr.signature, out string quoteForAca);
                Assert.That(quoteForAca, Does.Contain("signature:"));

                byte[] secret = RandomNumberGenerator.GetBytes(32);
                IdObject idObject = ek.Public.CreateActivationCredentials(secret, akName, out byte[] encryptedSecret);
                Assert.That(tpm.ActivateCredential(akHandle, ek.Handle, idObject.integrityHMAC,
                    idObject.encIdentity, encryptedSecret), Is.EqualTo(secret));
            } finally {
                tpm.FlushHandle(akHandle);
            }
        }

        [Test]
        public void TestAkFilesAreKeptUnlessReplaced() {
            string pubPath = Path.Combine(tempDir, "ak.pub");
            string privPath = Path.Combine(tempDir, "ak.priv");
            Hirs.Pb.AsymmetricKeyTemplate template = Hirs.Pb.AsymmetricKeyTemplate.EccP256Quote;

            tpm.CreateAttestationKey(pubPath, privPath, template, replace: true);
            byte[] first = tpm.ReadPublicAreaFromFile(pubPath);
            tpm.CreateAttestationKey(pubPath, privPath, template, replace: false);
            byte[] kept = tpm.ReadPublicAreaFromFile(pubPath);
            tpm.CreateAttestationKey(pubPath, privPath, template, replace: true);
            byte[] replaced = tpm.ReadPublicAreaFromFile(pubPath);

            Assert.That(kept, Is.EqualTo(first));
            Assert.That(replaced, Is.Not.EqualTo(first));
        }

        [Test]
        public void TestStorageRootKeyIsRegeneratedAndSrkHandleIsLeftAlone() {
            const uint tcgSrkHandle = 0x81000001;
            string before = Describe(tpm.ReadPublicArea(tcgSrkHandle, out byte[] _, out byte[] _));
            string pubPath = Path.Combine(tempDir, "ldevid.pub");
            string privPath = Path.Combine(tempDir, "ldevid.priv");

            tpm.CreateLDevIDKey(pubPath, privPath, replace: true);
            foreach (int _ in Enumerable.Range(0, 2)) {
                uint handle = tpm.LoadKeyFromFiles(pubPath, privPath);
                tpm.FlushHandle(handle);
            }

            string after = Describe(tpm.ReadPublicArea(tcgSrkHandle, out byte[] _, out byte[] _));
            Assert.That(after, Is.EqualTo(before));
        }

        private static string Describe(TpmPublic? pub) {
            return pub == null ? "nothing" : Convert.ToHexString(pub.GetTpmRepresentation());
        }

        [Test]
        public void TestGetPcrBanksListsAllocatedShaBanks() {
            IReadOnlyList<TpmAlgId> banks = tpm.GetPcrBanks();

            Assert.That(banks, Does.Contain(TpmAlgId.Sha256));
            foreach (TpmAlgId bank in banks) {
                Assert.That(tpm.GetPcrList(bank), Has.Length.EqualTo(24), $"{bank} bank");
            }
        }

        [Test]
        public void TestFailedActivateCredentialReleasesSessions() {
            using EndorsementKey ek = tpm.AcquireEndorsementKey(Hirs.Pb.EkTemplate.EkL1)!;
            uint akHandle = CreateAndLoadAk(Hirs.Pb.AsymmetricKeyTemplate.Rsa2048Quote, out byte[] akName);
            try {
                byte[] secret = RandomNumberGenerator.GetBytes(32);
                IdObject idObject = ek.Public.CreateActivationCredentials(secret, akName, out byte[] encryptedSecret);
                byte[] wrongSecret = (byte[])encryptedSecret.Clone();
                wrongSecret[0] ^= 0xFF;

                for (int i = 0; i < 3; i++) {
                    Assert.Throws<TpmException>(() => tpm.ActivateCredential(akHandle, ek.Handle,
                        idObject.integrityHMAC, idObject.encIdentity, wrongSecret));
                }

                Assert.That(tpm.ActivateCredential(akHandle, ek.Handle, idObject.integrityHMAC,
                    idObject.encIdentity, encryptedSecret), Is.EqualTo(secret));
            } finally {
                tpm.FlushHandle(akHandle);
            }
        }

        [Test]
        public void TestLeasesAcceptTheRecordedKeys() {
            foreach (Hirs.Pb.EkTemplate template in KeyTemplateCatalog.SupportedEkTemplates) {
                byte[] ekPublicArea;
                using (EndorsementKey? ek = tpm.AcquireEndorsementKey(template)) {
                    if (ek == null) {
                        continue; // not supported by the simulator
                    }
                    ekPublicArea = ek.PublicArea;
                }
                using KeyLease ekLease = KeyLease.ForEndorsementKey(tpm, template, ekPublicArea);
            }

            foreach (string key in (string[])["ak", "ldevid"]) {
                string pubPath = Path.Combine(tempDir, key + ".pub");
                string privPath = Path.Combine(tempDir, key + ".priv");
                if (key == "ak") {
                    tpm.CreateAttestationKey(pubPath, privPath,
                        Hirs.Pb.AsymmetricKeyTemplate.Rsa2048Quote, replace: true);
                } else {
                    tpm.CreateLDevIDKey(pubPath, privPath, replace: true);
                }
                byte[] publicArea = tpm.ReadPublicAreaFromFile(pubPath);
                uint handle;
                using (KeyLease lease = KeyLease.ForKeyFiles(tpm,pubPath, privPath,
                           publicArea, key)) {
                    handle = lease.Handle;
                }
                // Flushed when the lease ended
                Assert.That(tpm.ReadPublicArea(handle, out byte[] _, out byte[] _), Is.Null, key);
            }
        }

        [TestCase(CommandTpm.LegacyAkHandle)]
        [TestCase(CommandTpm.PersistentAkHandle)]
        public void TestEvictLegacyAttestationKeysRemovesOldAk(uint handle) {
            uint akHandle = CreateAndLoadAk(Hirs.Pb.AsymmetricKeyTemplate.Rsa2048Quote, out byte[] _);
            try {
                PersistAk(akHandle, handle);
            } finally {
                tpm.FlushHandle(akHandle);
            }

            Assert.That(tpm.EvictLegacyAttestationKeys(), Is.EqualTo(1));
            Assert.That(tpm.ReadPublicArea(handle, out byte[] _, out byte[] _), Is.Null);
        }

        [Test]
        public void TestEvictLegacyAttestationKeysKeepsL2Ek() {
            using (EndorsementKey _ = tpm.AcquireEndorsementKey(Hirs.Pb.EkTemplate.EkL2)!) { }
            Assume.That(tpm.ReadPublicArea(CommandTpm.DefaultL2EkHandle, out byte[] _, out byte[] _), Is.Not.Null,
                "The EK was not persisted. Nothing to keep.");

            tpm.EvictLegacyAttestationKeys();

            Assert.That(tpm.ReadPublicArea(CommandTpm.DefaultL2EkHandle, out byte[] _, out byte[] _), Is.Not.Null);
        }

        [Test]
        public void TestSupportsAlgorithmReportsKnownAlgorithm() {
            Assert.That(tpm.SupportsAlgorithm(TpmAlgId.Rsa), Is.True);
        }
    }
}
