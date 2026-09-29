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

        [SetUp]
        public void SetUp() {
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

        [Test]
        public void TestSupportsAlgorithmReportsKnownAlgorithm() {
            Assert.That(tpm.SupportsAlgorithm(TpmAlgId.Rsa), Is.True);
        }
    }
}
