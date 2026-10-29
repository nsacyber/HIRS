using hirs;
using NUnit.Framework;

namespace hirsTest.provisioner {
    public class LegacyCredentialBlobTests {
        // Combined credential blob and its parts
        private static readonly byte[] IntegrityHmac = Convert.FromBase64String("VAtedc1RlNA1w0XfrtwmhE0ILBlILP6163Tur5HRIo0=");
        private static readonly byte[] EncIdentity = Convert.FromBase64String("6e2oGBsK3H9Vzbj667ZsjnVOtvpSpQ==");
        private static readonly byte[] EncryptedSecret = Convert.FromBase64String("NekvnOX8RPRdyd0/cxBI4FTCuNkiu0KAnS28yT7yYJUL5Lwfcv5ctEK6zQA0fq0IsX5TlAYSidGKxrAilOSwALJmJ+m7sMiXwMKrZn1cd4gzXObZEQimQoWgSEQbPO7rfpUn1UfI8K5SzmUFUTxc5X3D8zFonaEBp6QCjtdLegKGgioCDcQFdz20Y0PFAa1Itug7YbZdCFpfit570eQQinmqdVryiNyn6CLQdMgIejuBxoEpoTSWszB5eFKEdn5g/+8wcvhp6RpNBQ0hikF+6688TOVK/j8n3JDwKVltJ/WNHjVO+lxa2aLIMJRgs5ZRuzuz6OSMf10KqJjSWZE04w==");
        private static readonly byte[] CombinedBlob = Convert.FromBase64String("OAAAIFQLXnXNUZTQNcNF367cJoRNCCwZSCz+tet07q+R0SKN6e2oGBsK3H9Vzbj667ZsjnVOtvpSpQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAATXpL5zl/ET0XcndP3MQSOBUwrjZIrtCgJ0tvMk+8mCVC+S8H3L+XLRCus0ANH6tCLF+U5QGEonRisawIpTksACyZifpu7DIl8DCq2Z9XHeIM1zm2REIpkKFoEhEGzzu636VJ9VHyPCuUs5lBVE8XOV9w/MxaJ2hAaekAo7XS3oChoIqAg3EBXc9tGNDxQGtSLboO2G2XQhaX4ree9HkEIp5qnVa8ojcp+gi0HTICHo7gcaBKaE0lrMweXhShHZ+YP/vMHL4aekaTQUNIYpBfuuvPEzlSv4/J9yQ8ClZbSf1jR41TvpcWtmiyDCUYLOWUbs7s+jkjH9dCqiY0lmRNOM=");

        [Test]
        public void TestSplitsMainCombinedBlob() {
            Assert.That(LegacyCredentialBlob.TrySplit(CombinedBlob, out byte[] integrityHmac, out byte[] encIdentity,
                out byte[] encryptedSecret, out string error), Is.True, error);
            Assert.Multiple(() => {
                Assert.That(integrityHmac, Is.EqualTo(IntegrityHmac));
                Assert.That(encIdentity, Is.EqualTo(EncIdentity));
                Assert.That(encryptedSecret, Is.EqualTo(EncryptedSecret));
            });
        }

        [Test]
        public void TestRejectsShortBlob() {
            Assert.That(LegacyCredentialBlob.TrySplit(CombinedBlob[..100], out _, out _, out _, out string error), Is.False);
            Assert.That(error, Does.Contain("too short"));
        }

        [Test]
        public void TestRejectsSecretPastEnd() {
            byte[] truncated = CombinedBlob[..200];
            Assert.That(LegacyCredentialBlob.TrySplit(truncated, out _, out _, out _, out string error), Is.False);
            Assert.That(error, Does.Contain("encrypted-secret length"));
        }
    }
}
