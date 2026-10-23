using hirs;
using NUnit.Framework;
using System.Net.Sockets;
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
        //   the simulator serves one client
        //   CommandTpm only closes it when finalized
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

        [Test]
        public void TestSignRoundTripWithNonRestrictedKey() {
            tpm.CreateStorageRootKey(CommandTpm.DefaultSrkHandle);

            string pubPath = Path.Combine(tempDir, "ldevid.pub");
            string privPath = Path.Combine(tempDir, "ldevid.priv");
            tpm.CreateLDevIDKey(CommandTpm.DefaultSrkHandle, pubPath, privPath, replace: true);

            uint keyHandle = tpm.LoadLDevIDKey(CommandTpm.DefaultSrkHandle, pubPath, privPath);
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
            using EndorsementKey ek = tpm.AcquireEndorsementKey(Hirs.Pb.EkTemplate.EkL1)!;
            tpm.CreateAttestationKey(ek.Handle, CommandTpm.DefaultAkHandle, replace: true);

            byte[] digest = SHA256.HashData([.. "should not be signable"u8]);

            Assert.Throws<TpmException>(() => tpm.Sign(CommandTpm.DefaultAkHandle, digest, TpmAlgId.Sha256));
        }

        [TestCase(Hirs.Pb.EkTemplate.EkL1)]
        [TestCase(Hirs.Pb.EkTemplate.EkL2)]
        public void TestActivateCredentialRecoversSecret(Hirs.Pb.EkTemplate template) {
            using EndorsementKey akParent = tpm.AcquireEndorsementKey(Hirs.Pb.EkTemplate.EkL1)!;
            tpm.CreateAttestationKey(akParent.Handle, CommandTpm.DefaultAkHandle, replace: true);
            tpm.ReadPublicArea(CommandTpm.DefaultAkHandle, out byte[] akName, out byte[] _);

            using EndorsementKey ek = tpm.AcquireEndorsementKey(template)!;
            byte[] secret = RandomNumberGenerator.GetBytes(32);
            IdObject idObject = ek.Public.CreateActivationCredentials(secret, akName, out byte[] encryptedSecret);
            byte[] credentialBlob = new Tpm2bIdObject(idObject).GetTpmRepresentation();
            byte[] encryptedSecretBlob = new Tpm2bEncryptedSecret(encryptedSecret).GetTpmRepresentation();

            byte[] recovered = tpm.ActivateCredential(CommandTpm.DefaultAkHandle, ek.Handle, credentialBlob, encryptedSecretBlob);

            Assert.That(recovered, Is.EqualTo(secret));
        }

        [Test]
        public void TestEvictLegacyAttestationKeyRemovesOldAk() {
            using EndorsementKey l1Ek = tpm.AcquireEndorsementKey(Hirs.Pb.EkTemplate.EkL1)!;
            tpm.CreateAttestationKey(l1Ek.Handle, CommandTpm.LegacyAkHandle, replace: true);

            Assert.That(tpm.EvictLegacyAttestationKey(), Is.True);
            Assert.That(tpm.ReadPublicArea(CommandTpm.LegacyAkHandle, out byte[] _, out byte[] _), Is.Null);
        }

        [Test]
        public void TestEvictLegacyAttestationKeyKeepsL2Ek() {
            using (EndorsementKey _ = tpm.AcquireEndorsementKey(Hirs.Pb.EkTemplate.EkL2)!) { }
            Assume.That(tpm.ReadPublicArea(CommandTpm.DefaultL2EkHandle, out byte[] _, out byte[] _), Is.Not.Null,
                "The EK was not persisted. Nothing to keep.");

            Assert.That(tpm.EvictLegacyAttestationKey(), Is.False);
            Assert.That(tpm.ReadPublicArea(CommandTpm.DefaultL2EkHandle, out byte[] _, out byte[] _), Is.Not.Null);
        }

        [Test]
        public void TestSupportsAlgorithmReportsKnownAlgorithm() {
            Assert.That(tpm.SupportsAlgorithm(TpmAlgId.Rsa), Is.True);
        }
    }
}
