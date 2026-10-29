using Google.Protobuf;
using hirs;
using Hirs.Pb;
using NUnit.Framework;

namespace hirsTest.provisioner {
    public class ProvisioningKeysTests {
        private static KeyCandidate Candidate(params byte[] keyId) {
            return new KeyCandidate { KeyId = ByteString.CopyFrom(keyId) };
        }

        private static Func<KeyLease> At(uint handle) {
            return () => new KeyLease(handle, null);
        }

        [Test]
        public void TestCandidatesKeepAddedOrder() {
            ProvisioningKeys keys = new();
            keys.Add(Candidate(3), At(0x81010002));
            keys.Add(Candidate(1), At(0x81010003));
            keys.Add(Candidate(2), At(0x80000001));

            Assert.That(keys.Candidates.Select(candidate => candidate.KeyId.ToByteArray()[0]), Is.EqualTo(new byte[] { 3, 1, 2 }));
        }

        [Test]
        public void TestTryGetFindsKeyByEqualKeyId() {
            ProvisioningKeys keys = new();
            keys.Add(Candidate(0xAA, 0xBB), At(0x81010002));

            Assert.That(keys.TryGet(ByteString.CopyFrom(0xAA, 0xBB), out ProvisioningKey key), Is.True);
            using (KeyLease lease = key.Acquire()) {
                Assert.That(lease.Handle, Is.EqualTo(0x81010002));
            }
            Assert.That(keys.TryGet(ByteString.CopyFrom(0xAA), out _), Is.False);
        }

        [Test]
        public void TestAddRejectsDuplicateKeyId() {
            ProvisioningKeys keys = new();
            keys.Add(Candidate(1), At(0x81010002));

            Assert.Throws<ArgumentException>(() => keys.Add(Candidate(1), At(0x81010003)));
            Assert.That(keys.Candidates, Has.Count.EqualTo(1));
        }

        [Test]
        public void TestAnswerGathersEvidenceForExternalKeys() {
            byte[] nonce = [1, 2, 3];
            ProvisioningKeys keys = new();
            keys.Add(Candidate(1), At(0x81010001));
            keys.AddExternal(Candidate(2), challenge => new ExternalEvidenceResult {
                EvidenceFormat = EvidenceFormat.HpEpscBootlog,
                Evidence = challenge.Nonce
            });

            List<VerificationResult> results = keys.Answer([
                new VerificationChallenge { KeyId = ByteString.CopyFrom(2), ExternalEvidence = new() { Nonce = ByteString.CopyFrom(nonce) } },
                // Not offered
                new VerificationChallenge { KeyId = ByteString.CopyFrom(9), ExternalEvidence = new() },
                // A TPM key cannot answer with external evidence
                new VerificationChallenge { KeyId = ByteString.CopyFrom(1), ExternalEvidence = new() },
                // Not answered yet
                new VerificationChallenge { KeyId = ByteString.CopyFrom(1), Quote = new() }
            ]);

            Assert.That(results, Has.Count.EqualTo(1));
            Assert.That(results[0].KeyId, Is.EqualTo(ByteString.CopyFrom(2)));
            Assert.That(results[0].ExternalEvidence.Evidence.ToByteArray(), Is.EqualTo(nonce));
        }

        [Test]
        public void TestExternalKeyCannotBeLoaded() {
            ProvisioningKeys keys = new();
            ProvisioningKey external = keys.AddExternal(Candidate(2), _ => null);
            ProvisioningKey tpmKey = keys.Add(Candidate(1), At(0x81010001));

            Assert.Multiple(() => {
                Assert.That(external.IsExternal, Is.True);
                Assert.Throws<InvalidOperationException>(() => external.Acquire());
                Assert.Throws<InvalidOperationException>(() => tpmKey.GatherEvidence(new ExternalEvidenceChallenge()));
            });
        }

        [Test]
        public void TestAcquireLoadsTheKeyEachTime() {
            int loads = 0;
            ProvisioningKeys keys = new();
            ProvisioningKey key = keys.Add(Candidate(1), () => new KeyLease(0x80000000u + (uint)++loads, null));

            using (KeyLease first = key.Acquire()) {
                Assert.That(first.Handle, Is.EqualTo(0x80000001));
            }
            using (KeyLease second = key.Acquire()) {
                Assert.That(second.Handle, Is.EqualTo(0x80000002));
            }
        }
    }
}
