using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using hirs;
using NUnit.Framework;
using Tpm2Lib;

namespace hirsTest.tpm;

public class EkCertificateMatcherTests {
    private static byte[] SelfSigned(RSA key) {
        CertificateRequest req = new("CN=ek", key, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
        return req.CreateSelfSigned(DateTimeOffset.UtcNow.AddDays(-1), DateTimeOffset.UtcNow.AddDays(1)).RawData;
    }

    private static byte[] SelfSigned(ECDsa key) {
        CertificateRequest req = new("CN=ek", key, HashAlgorithmName.SHA256);
        return req.CreateSelfSigned(DateTimeOffset.UtcNow.AddDays(-1), DateTimeOffset.UtcNow.AddDays(1)).RawData;
    }

    private static TpmPublic RsaEk(RSA key) {
        TpmPublic pub = CommandTpm.GenerateEKTemplateL1();
        pub.unique = new Tpm2bPublicKeyRsa(key.ExportParameters(false).Modulus!);
        return pub;
    }

    private static TpmPublic EccEk(ECDsa key) {
        TpmPublic pub = CommandTpm.GenerateEKTemplateL2();
        ECParameters p = key.ExportParameters(false);
        pub.unique = new EccPoint(p.Q.X!, p.Q.Y!);
        return pub;
    }

    [Test]
    public void RsaCertificateMatchesOnlyItsOwnEk() {
        using RSA key = RSA.Create(2048);
        using RSA other = RSA.Create(2048);
        byte[] cert = SelfSigned(key);
        using (Assert.EnterMultipleScope()) {
            Assert.That(EkCertificateMatcher.Matches(cert, RsaEk(key)), Is.True);
            Assert.That(EkCertificateMatcher.Matches(cert, RsaEk(other)), Is.False);
        }
    }

    [Test]
    public void EccCertificateMatchesOnlyItsOwnEk() {
        using ECDsa key = ECDsa.Create(ECCurve.NamedCurves.nistP256);
        using ECDsa other = ECDsa.Create(ECCurve.NamedCurves.nistP256);
        byte[] cert = SelfSigned(key);
        using (Assert.EnterMultipleScope()) {
            Assert.That(EkCertificateMatcher.Matches(cert, EccEk(key)), Is.True);
            Assert.That(EkCertificateMatcher.Matches(cert, EccEk(other)), Is.False);
        }
    }

    [Test]
    public void RsaCertificateDoesNotMatchEccEk() {
        using RSA rsaKey = RSA.Create(2048);
        using ECDsa eccKey = ECDsa.Create(ECCurve.NamedCurves.nistP256);
        using (Assert.EnterMultipleScope()) {
            Assert.That(EkCertificateMatcher.Matches(SelfSigned(rsaKey), EccEk(eccKey)), Is.False);
            Assert.That(EkCertificateMatcher.Matches(SelfSigned(eccKey), RsaEk(rsaKey)), Is.False);
        }
    }

    [Test]
    public void GarbageOrMissingCertificateDoesNotMatch() {
        using RSA key = RSA.Create(2048);
        using (Assert.EnterMultipleScope()) {
            Assert.That(EkCertificateMatcher.Matches([.. "not a certificate"u8], RsaEk(key)), Is.False);
            Assert.That(EkCertificateMatcher.Matches([], RsaEk(key)), Is.False);
            Assert.That(EkCertificateMatcher.Matches(null, RsaEk(key)), Is.False);
        }
    }
}
