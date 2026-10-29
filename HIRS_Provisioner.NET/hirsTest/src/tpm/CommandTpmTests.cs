using hirs;
using NUnit.Framework;
using System.Security.Cryptography;
using Tpm2Lib;

namespace hirsTest.tpm;

public class CommandTpmTests {
    private const ObjectAttr AkAttributes = ObjectAttr.Restricted | ObjectAttr.Sign | ObjectAttr.FixedParent
        | ObjectAttr.FixedTPM | ObjectAttr.SensitiveDataOrigin | ObjectAttr.UserWithAuth;

    private static TpmPublic Ak(ObjectAttr attributes) {
        RsaParms rsa = new(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null), new SchemeRsassa(TpmAlgId.Sha256), 2048, 0);
        return new TpmPublic(TpmAlgId.Sha256, attributes, [], rsa, new Tpm2bPublicKeyRsa(RandomNumberGenerator.GetBytes(256)));
    }

    [Test]
    public void TestExtractFirstCertificateReturnsEmptyForZeroFill() {
        Assert.That(CommandTpm.ExtractFirstCertificate(new byte[1024]), Is.Empty);
    }

    [Test]
    public void TestExtractFirstCertificateSkipsFill() {
        byte[] certificate = [0x30, 0x82, 0x00, 0x03, 0x01, 0x02, 0x03];
        byte[] data = [0xFF, 0x00, .. certificate, 0xFF, 0xFF];

        Assert.That(CommandTpm.ExtractFirstCertificate(data), Is.EqualTo(certificate));
    }

    // The standard SRK with an empty unique
    [Test]
    public void TestSrkTemplateHasEmptyUnique() {
        TpmPublic rsa = KeyTemplateCatalog.GenerateSrkTemplate(TpmAlgId.Rsa);
        TpmPublic ecc = KeyTemplateCatalog.GenerateSrkTemplate(TpmAlgId.Ecc);
        const ObjectAttr storage = ObjectAttr.FixedTPM | ObjectAttr.FixedParent | ObjectAttr.SensitiveDataOrigin
            | ObjectAttr.UserWithAuth | ObjectAttr.Restricted | ObjectAttr.Decrypt | ObjectAttr.NoDA;

        Assert.Multiple(() => {
            // TSS.NET leaves an empty buffer null; either marshals as a zero-length unique
            Assert.That(((Tpm2bPublicKeyRsa)rsa.unique).buffer ?? [], Is.Empty);
            Assert.That(((EccPoint)ecc.unique).x ?? [], Is.Empty);
            Assert.That(((EccPoint)ecc.unique).y ?? [], Is.Empty);
            Assert.That(rsa.objectAttributes, Is.EqualTo(storage));
            Assert.That(rsa.nameAlg, Is.EqualTo(TpmAlgId.Sha256));
            Assert.That(((RsaParms)rsa.parameters).keyBits, Is.EqualTo(2048));
            Assert.That(((EccParms)ecc.parameters).curveID, Is.EqualTo(EccCurve.TpmEccNistP256));
        });
    }

    [Test]
    public void TestEkTemplateL2UniqueIsTwo32ByteZeroBuffers() {
        EccPoint unique = (EccPoint)KeyTemplateCatalog.GenerateEkTemplateL2().unique;

        Assert.That(unique.x, Is.EqualTo(new byte[32]));
        Assert.That(unique.y, Is.EqualTo(new byte[32]));
    }

    [Test]
    public void TestIsHirsAttestationKeyMatchesAkTemplate() {
        Assert.That(CommandTpm.IsHirsAttestationKey(Ak(AkAttributes)), Is.True);
    }

    [Test]
    public void TestIsHirsAttestationKeyRejectsOtherKeys() {
        Assert.That(CommandTpm.IsHirsAttestationKey(KeyTemplateCatalog.GenerateEkTemplateL1()), Is.False);
        Assert.That(CommandTpm.IsHirsAttestationKey(KeyTemplateCatalog.GenerateEkTemplateL2()), Is.False);
        Assert.That(CommandTpm.IsHirsAttestationKey(Ak(AkAttributes & ~ObjectAttr.Restricted)), Is.False);
    }

    [Test]
    public void TestMarshalling() {
        byte[] integrityHMAC = Convert.FromBase64String("VAtedc1RlNA1w0XfrtwmhE0ILBlILP6163Tur5HRIo0=");
        byte[] encIdentity = Convert.FromBase64String("6e2oGBsK3H9Vzbj667ZsjnVOtvpSpQ==");
        byte[] encryptedSecret = Convert.FromBase64String("NekvnOX8RPRdyd0/cxBI4FTCuNkiu0KAnS28yT7yYJUL5Lwfcv5ctEK6zQA0fq0IsX5TlAYSidGKxrAilOSwALJmJ+m7sMiXwMKrZn1cd4gzXObZEQimQoWgSEQbPO7rfpUn1UfI8K5SzmUFUTxc5X3D8zFonaEBp6QCjtdLegKGgioCDcQFdz20Y0PFAa1Itug7YbZdCFpfit570eQQinmqdVryiNyn6CLQdMgIejuBxoEpoTSWszB5eFKEdn5g/+8wcvhp6RpNBQ0hikF+6688TOVK/j8n3JDwKVltJ/WNHjVO+lxa2aLIMJRgs5ZRuzuz6OSMf10KqJjSWZE04w==");
        byte[] encryptedSecretBlob = Convert.FromBase64String("AQA16S+c5fxE9F3J3T9zEEjgVMK42SK7QoCdLbzJPvJglQvkvB9y/ly0QrrNADR+rQixflOUBhKJ0YrGsCKU5LAAsmYn6buwyJfAwqtmfVx3iDNc5tkRCKZChaBIRBs87ut+lSfVR8jwrlLOZQVRPFzlfcPzMWidoQGnpAKO10t6AoaCKgINxAV3PbRjQ8UBrUi26Dthtl0IWl+K3nvR5BCKeap1WvKI3KfoItB0yAh6O4HGgSmhNJazMHl4UoR2fmD/7zBy+GnpGk0FDSGKQX7rrzxM5Ur+PyfckPApWW0n9Y0eNU76XFrZosgwlGCzllG7O7Po5Ix/XQqomNJZkTTj");
        byte[] credentialBlob = Convert.FromBase64String("ADgAIFQLXnXNUZTQNcNF367cJoRNCCwZSCz+tet07q+R0SKN6e2oGBsK3H9Vzbj667ZsjnVOtvpSpQ==");

        using (Assert.EnterMultipleScope()) {
            Assert.That(CommandTpm.CanMarshal<Tpm2bIdObject>(credentialBlob), Is.True);
            //Assert.That(CommandTpm.CanMarshal<IdObject>(credentialBlob), Is.True);
            Assert.That(CommandTpm.CanMarshal<Tpm2bEncryptedSecret>(encryptedSecretBlob), Is.True);
        }
        
        Tpm2bIdObject credential = CommandTpm.Marshal<Tpm2bIdObject>(credentialBlob);
        Tpm2bEncryptedSecret encrypted = CommandTpm.Marshal<Tpm2bEncryptedSecret>(encryptedSecretBlob);
        
        using (Assert.EnterMultipleScope()) {
            Assert.That(credential.credential.integrityHMAC, Is.EqualTo(integrityHMAC));
            Assert.That(credential.credential.encIdentity, Is.EqualTo(encIdentity));
            Assert.That(encrypted.secret, Is.EqualTo(encryptedSecret));
        }
    }
}