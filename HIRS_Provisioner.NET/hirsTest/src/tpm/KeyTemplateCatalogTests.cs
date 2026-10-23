using System.Reflection;
using System.Text.RegularExpressions;
using Google.Protobuf.Reflection;
using hirs;
using Hirs.Pb;
using NUnit.Framework;
using Tpm2Lib;

namespace hirsTest.tpm;

public class KeyTemplateCatalogTests {
    private const ObjectAttr StorageBase = ObjectAttr.FixedTPM | ObjectAttr.FixedParent
        | ObjectAttr.SensitiveDataOrigin | ObjectAttr.AdminWithPolicy | ObjectAttr.Restricted;

    private static TpmPublic HighRangeRsaStorage() {
        RsaParms rsa = new(new SymDefObject(TpmAlgId.Aes, 128, TpmAlgId.Cfb), new NullAsymScheme(), 2048, 0);
        // High Range: userWithAuth = 1, PolicyB (any digest other than PolicyA).
        return new TpmPublic(TpmAlgId.Sha256, StorageBase | ObjectAttr.UserWithAuth | ObjectAttr.Decrypt,
            new byte[32], rsa, new Tpm2bPublicKeyRsa());
    }

    private static TpmPublic HighRangeEcc(ObjectAttr extra, uint rawExtraBits = 0) {
        EccParms ecc = new(new SymDefObject(TpmAlgId.Aes, 128, TpmAlgId.Cfb), new NullAsymScheme(),
            EccCurve.TpmEccNistP256, new NullKdfScheme());
        ObjectAttr attrs = (StorageBase | ObjectAttr.UserWithAuth | extra) | (ObjectAttr)rawExtraBits;
        return new TpmPublic(TpmAlgId.Sha256, attrs, new byte[32], ecc, new EccPoint());
    }

    [Test]
    public void EkTemplateValuesFollowNamingConvention() {
        Regex name = new(@"^EK_(?<fam>[HL])(?<n>\d+)$");
        int checkedCount = 0;
        foreach (FieldInfo field in typeof(EkTemplate).GetFields(BindingFlags.Public | BindingFlags.Static)) {
            string original = field.GetCustomAttribute<OriginalNameAttribute>()!.Name;
            if (original == "EK_TEMPLATE_UNSPECIFIED") {
                continue;
            }
            Match m = name.Match(original);
            Assert.That(m.Success, Is.True, $"{original} does not match EK_H<n>/EK_L<n>");
            int n = int.Parse(m.Groups["n"].Value);
            int expected = m.Groups["fam"].Value == "H" ? n : 1000 + n;
            Assert.That((int)field.GetValue(null)!, Is.EqualTo(expected), original);
            checkedCount++;
        }
        Assert.That(checkedCount, Is.EqualTo(39));
    }

    [Test]
    public void EveryEkTemplateHasCertificateNvIndex() {
        foreach (EkTemplate template in Enum.GetValues<EkTemplate>()) {
            if (template == EkTemplate.Unspecified) {
                continue;
            }
            Assert.That(KeyTemplateCatalog.GetEkCertificateNvIndex(template), Is.Not.Null, template.ToString());
        }
        Assert.That(KeyTemplateCatalog.GetEkCertificateNvIndex(EkTemplate.Unspecified), Is.Null);
    }

    [Test]
    public void LowRangeTemplatesClassifyAsLAndUseLowRangeNvIndices() {
        using (Assert.EnterMultipleScope()) {
            Assert.That(KeyTemplateCatalog.ClassifyEk(CommandTpm.GenerateEKTemplateL1()), Is.EqualTo(EkTemplate.EkL1));
            Assert.That(KeyTemplateCatalog.ClassifyEk(CommandTpm.GenerateEKTemplateL2()), Is.EqualTo(EkTemplate.EkL2));
            Assert.That(KeyTemplateCatalog.GetEkCertificateNvIndex(EkTemplate.EkL1), Is.EqualTo(0x01c00002u));
            Assert.That(KeyTemplateCatalog.GetEkCertificateNvIndex(EkTemplate.EkL2), Is.EqualTo(0x01c0000au));
        }
    }

    [Test]
    public void HighRangeSameShapeAsLowRangeClassifiesAsH() {
        using (Assert.EnterMultipleScope()) {
            Assert.That(KeyTemplateCatalog.ClassifyEk(HighRangeRsaStorage()), Is.EqualTo(EkTemplate.EkH1));
            Assert.That(KeyTemplateCatalog.ClassifyEk(HighRangeEcc(ObjectAttr.Decrypt)), Is.EqualTo(EkTemplate.EkH2));
        }
    }

    [Test]
    public void SigningAndFirmwareLimitedEccClassify() {
        using (Assert.EnterMultipleScope()) {
            Assert.That(KeyTemplateCatalog.ClassifyEk(HighRangeEcc(ObjectAttr.Sign)), Is.EqualTo(EkTemplate.EkH10));
            // firmwareLimited (bit 8) replaces fixedTPM.
            TpmPublic fwStorage = HighRangeEcc(ObjectAttr.Decrypt, 1u << 8);
            fwStorage.objectAttributes &= ~ObjectAttr.FixedTPM;
            Assert.That(KeyTemplateCatalog.ClassifyEk(fwStorage), Is.EqualTo(EkTemplate.EkH18));
            TpmPublic fwSigning = HighRangeEcc(ObjectAttr.Sign, 1u << 8);
            fwSigning.objectAttributes &= ~ObjectAttr.FixedTPM;
            Assert.That(KeyTemplateCatalog.ClassifyEk(fwSigning), Is.EqualTo(EkTemplate.EkH22));
        }
    }

    [Test]
    public void BuildEkCandidateSetsCertificateNvIndex() {
        TpmPublic l1 = CommandTpm.GenerateEKTemplateL1();
        KeyCandidate candidate = KeyTemplateCatalog.BuildEkCandidate(l1, l1, CommandTpm.DefaultL1EkHandle);
        using (Assert.EnterMultipleScope()) {
            Assert.That(candidate.Tpm.EkTemplate, Is.EqualTo(EkTemplate.EkL1));
            Assert.That(candidate.Tpm.CertificateNvIndex, Is.EqualTo(0x01c00002u));
        }
    }

    [Test]
    public void EverySupportedEkTemplateIsGeneratedAndRoundTrips() {
        foreach (EkTemplate template in KeyTemplateCatalog.SupportedEkTemplates) {
            TpmPublic? generated = CommandTpm.GenerateEkTemplate(template);
            Assert.That(generated, Is.Not.Null, template.ToString());
            Assert.That(KeyTemplateCatalog.ClassifyEk(generated), Is.EqualTo(template), template.ToString());
            Assert.That(KeyTemplateCatalog.GetEkCertificateNvIndex(template), Is.Not.Null, template.ToString());
        }
    }

    [Test]
    public void ConventionalEkHandlesAreOnlyForLowRange() {
        using (Assert.EnterMultipleScope()) {
            Assert.That(KeyTemplateCatalog.GetConventionalEkHandle(EkTemplate.EkL1), Is.EqualTo(0x81010001u));
            Assert.That(KeyTemplateCatalog.GetConventionalEkHandle(EkTemplate.EkL2), Is.EqualTo(0x81010002u));
            Assert.That(KeyTemplateCatalog.GetConventionalEkHandle(EkTemplate.EkH1), Is.Null);
        }
    }

    [Test]
    public void BuildEkCandidateCarriesCertificateAndOmitsHandleForTransientEk() {
        TpmPublic h1 = HighRangeRsaStorage();
        byte[] cert = [1, 2, 3];
        KeyCandidate candidate = KeyTemplateCatalog.BuildEkCandidate(h1, h1, null, cert);
        using (Assert.EnterMultipleScope()) {
            Assert.That(candidate.Tpm.EkTemplate, Is.EqualTo(EkTemplate.EkH1));
            Assert.That(candidate.Tpm.HasPersistentHandle, Is.False);
            Assert.That(candidate.Tpm.Certificate.ToByteArray(), Is.EqualTo(cert));
            Assert.That(candidate.Tpm.CertificateNvIndex, Is.EqualTo(0x01c00012u));
        }
    }
}
