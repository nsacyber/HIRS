using Google.Protobuf;
using hirs;
using Hirs.Pb;
using NUnit.Framework;

namespace hirsTest.provisioner {
    public class ProvisioningKeysTests {
        private static KeyCandidate Candidate(params byte[] keyId) {
            return new KeyCandidate { KeyId = ByteString.CopyFrom(keyId) };
        }

        [Test]
        public void TestCandidatesKeepAddedOrder() {
            ProvisioningKeys keys = new();
            keys.Add(Candidate(3), 0x81010002);
            keys.Add(Candidate(1), 0x81010003);
            keys.Add(Candidate(2), null);

            Assert.That(keys.Candidates.Select(candidate => candidate.KeyId.ToByteArray()[0]), Is.EqualTo(new byte[] { 3, 1, 2 }));
        }

        [Test]
        public void TestTryGetFindsKeyByEqualKeyId() {
            ProvisioningKeys keys = new();
            keys.Add(Candidate(0xAA, 0xBB), 0x81010002);

            Assert.That(keys.TryGet(ByteString.CopyFrom(0xAA, 0xBB), out ProvisioningKey key), Is.True);
            Assert.That(key.Handle, Is.EqualTo(0x81010002));
            Assert.That(keys.TryGet(ByteString.CopyFrom(0xAA), out _), Is.False);
        }

        [Test]
        public void TestAddRejectsDuplicateKeyId() {
            ProvisioningKeys keys = new();
            keys.Add(Candidate(1), 0x81010002);

            Assert.Throws<ArgumentException>(() => keys.Add(Candidate(1), 0x81010003));
            Assert.That(keys.Candidates, Has.Count.EqualTo(1));
        }
    }
}
