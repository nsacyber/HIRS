using FakeItEasy;
using hirs;
using Hirs.Pb;
using NUnit.Framework;
using Tpm2Lib;

namespace hirsTest.tpm {
    public class KeyLeaseTests {
        private const uint TransientHandle = 0x80000001;

        [Test]
        public void TestEndorsementKeyLeaseReleasesTheEkOnce() {
            int releases = 0;
            TpmPublic l1 = KeyTemplateCatalog.GenerateEkTemplateL1();
            IHirsAcaTpm tpm = A.Fake<IHirsAcaTpm>();
            A.CallTo(() => tpm.AcquireEndorsementKey(EkTemplate.EkL1))
                .Returns(new EndorsementKey(EkTemplate.EkL1, l1, TransientHandle, false, () => releases++));

            KeyLease lease = KeyLease.ForEndorsementKey(tpm, EkTemplate.EkL1, l1);
            Assert.That(lease.Handle, Is.EqualTo(TransientHandle));
            lease.Dispose();
            lease.Dispose();

            Assert.That(releases, Is.EqualTo(1));
        }

        [Test]
        public void TestEndorsementKeyLeaseRefusesADifferentKey() {
            int releases = 0;
            IHirsAcaTpm tpm = A.Fake<IHirsAcaTpm>();
            A.CallTo(() => tpm.AcquireEndorsementKey(EkTemplate.EkL1)).Returns(new EndorsementKey(EkTemplate.EkL1,
                KeyTemplateCatalog.GenerateEkTemplateL2(), TransientHandle, false, () => releases++));

            Assert.Throws<ProvisioningFailureException>(() =>
                KeyLease.ForEndorsementKey(tpm, EkTemplate.EkL1, KeyTemplateCatalog.GenerateEkTemplateL1()));
            Assert.That(releases, Is.EqualTo(1));
        }

        [Test]
        public void TestEndorsementKeyLeaseFailsWhenTheTpmHasNoEk() {
            IHirsAcaTpm tpm = A.Fake<IHirsAcaTpm>();
            A.CallTo(() => tpm.AcquireEndorsementKey(EkTemplate.EkL1)).Returns(null);

            Assert.Throws<ProvisioningFailureException>(() =>
                KeyLease.ForEndorsementKey(tpm, EkTemplate.EkL1, KeyTemplateCatalog.GenerateEkTemplateL1()));
        }

        [Test]
        public void TestPersistentKeyLeaseRefusesAnEmptyHandle() {
            byte[] name = null!, qualifiedName = null!;
            IHirsAcaTpm tpm = A.Fake<IHirsAcaTpm>();
            A.CallTo(() => tpm.ReadPublicArea(CommandTpm.PersistentAkHandle, out name, out qualifiedName)).Returns(null!);

            Assert.Throws<ProvisioningFailureException>(() =>
                KeyLease.ForPersistentKey(tpm, CommandTpm.PersistentAkHandle, KeyTemplateCatalog.GenerateSrkTemplate()));
        }

        [Test]
        public void TestLDevIDLeaseFlushesTheLoadedKey() {
            TpmPublic ldevid = KeyTemplateCatalog.GenerateSrkTemplate();
            byte[] name = null!, qualifiedName = null!;
            IHirsAcaTpm tpm = A.Fake<IHirsAcaTpm>();
            A.CallTo(() => tpm.LoadKeyFromFiles("ldevid.pub", "ldevid.priv")).Returns(TransientHandle);
            A.CallTo(() => tpm.ReadPublicArea(TransientHandle, out name, out qualifiedName)).Returns(ldevid);

            using (KeyLease lease = KeyLease.ForKeyFiles(tpm, "ldevid.pub", "ldevid.priv", ldevid, "the LDevID")) {
                A.CallTo(() => tpm.FlushHandle(TransientHandle)).MustNotHaveHappened();
            }

            A.CallTo(() => tpm.FlushHandle(TransientHandle)).MustHaveHappenedOnceExactly();
        }

        [Test]
        public void TestLDevIDLeaseFlushesARefusedKey() {
            byte[] name = null!, qualifiedName = null!;
            IHirsAcaTpm tpm = A.Fake<IHirsAcaTpm>();
            A.CallTo(() => tpm.LoadKeyFromFiles(A<string>.Ignored, A<string>.Ignored)).Returns(TransientHandle);
            A.CallTo(() => tpm.ReadPublicArea(TransientHandle, out name, out qualifiedName)).Returns(KeyTemplateCatalog.GenerateEkTemplateL1());

            Assert.Throws<ProvisioningFailureException>(() => KeyLease.ForKeyFiles(tpm,
                "ldevid.pub", "ldevid.priv", KeyTemplateCatalog.GenerateSrkTemplate(), "the LDevID"));
            A.CallTo(() => tpm.FlushHandle(TransientHandle)).MustHaveHappenedOnceExactly();
        }
    }
}
