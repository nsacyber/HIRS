using hirs;
using Hirs.Pb;
using NUnit.Framework;
using System.Collections.Generic;
using System.Text;
using Tpm2Lib;

namespace hirsTest {
    public class ClientTests {
        public static readonly string localhost = "https://127.0.0.1:8443/";

        [Test]
        public void TestCreateIdentityClaim() {
            IHirsAcaClient client = new Client(ClientTests.localhost);

            DeviceInfo dv = new DeviceInfo();
            byte[] akPub = new byte[] { };
            byte[] ekPub = new byte[] { };
            byte[] ldevidPub = new byte[] { };
            byte[] ekc = new byte[] { };
            List<byte[]> pcs = new List<byte[]>();
            pcs.Add(new byte[] { });
            string componentList= "";

            IdentityClaim obj = client.CreateIdentityClaim(dv, akPub, ekPub, ekc, pcs, componentList, ldevidPub);
            Assert.Multiple(() => {
                Assert.That(obj.Dv, Is.Not.Null);
                Assert.That(obj.HasAkPublicArea, Is.True);
                Assert.That(obj.HasEkPublicArea, Is.True);
                Assert.That(obj.HasEndorsementCredential, Is.True);
                Assert.That(obj.PlatformCredential, Has.Count.EqualTo(1));
                Assert.That(obj.HasPaccorOutput, Is.True);
                Assert.That(obj.HasLdevidPublicArea, Is.True);
            });
        }

        [Test]
        public void TestCreateIdentityClaimWithKeyCandidates() {
            IHirsAcaClient client = new Client(ClientTests.localhost);

            DeviceInfo dv = new DeviceInfo();
            TpmPublic ekPub = CommandTpm.GenerateEKTemplateL1();
            TpmPublic akPub = new(TpmAlgId.Sha256, ObjectAttr.Restricted | ObjectAttr.Sign,
                null, new RsaParms(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null),
                    new SchemeRsassa(TpmAlgId.Sha256), 2048, 0), new Tpm2bPublicKeyRsa());
            byte[] akPubBytes = akPub;
            byte[] ekPubBytes = ekPub;

            List<KeyCandidate> candidates = new() {
                KeyTemplateCatalog.BuildEkCandidate(ekPubBytes, ekPub, CommandTpm.DefaultL1EkHandle),
                KeyTemplateCatalog.BuildAsymmetricCandidate(akPubBytes, akPub, CommandTpm.DefaultAkHandle,
                    KeyRole.Attestation, ProvisioningOrigin.Local),
            };

            IdentityClaim obj = client.CreateIdentityClaim(dv, akPubBytes, ekPubBytes, new byte[] { },
                new List<byte[]>(), "", null, candidates);

            Assert.Multiple(() => {
                Assert.That(obj.KeyCandidates, Has.Count.EqualTo(2));
                Assert.That(obj.KeyCandidates[0].Role, Does.Contain(KeyRole.Endorsement));
                Assert.That(obj.KeyCandidates[0].Tpm.EkTemplate, Is.EqualTo(EkTemplate.EkL1));
                Assert.That(obj.KeyCandidates[1].Role, Does.Contain(KeyRole.Attestation));
                Assert.That(obj.KeyCandidates[1].Tpm.AsymmetricTemplate, Is.EqualTo(AsymmetricKeyTemplate.Rsa2048Quote));
                Assert.That(obj.KeyCandidates[0].KeyId, Is.Not.EqualTo(obj.KeyCandidates[1].KeyId));
            });
        }

        [Test]
        public void TestCreateAkCertificateRequest() {
            IHirsAcaClient client = new Client(ClientTests.localhost);

            byte[] secret = Encoding.UTF8.GetBytes("secret");
            Attest quoted = new Attest(Generated.None, Encoding.UTF8.GetBytes("QUALIFIED SIGNER"), Encoding.UTF8.GetBytes("EXTRA DATA"), new ClockInfo(0, 0, 0, 0), 0, new QuoteInfo());
            ISignatureUnion signature = new SignatureRsassa();
            Tpm2bDigest[] pcrValues = new Tpm2bDigest[] { new Tpm2bDigest(Encoding.UTF8.GetBytes("SHA1 DIGEST1")) };

            CommandTpmQuoteResponse ctqr = new CommandTpmQuoteResponse(quoted, signature, pcrValues);

            CertificateRequest obj = client.CreateAkCertificateRequest(secret, ctqr);
            Assert.Multiple(() => {
                Assert.That(obj.HasNonce, Is.True);
                Assert.That(obj.HasQuote, Is.True);
            });
        }
    }
}
