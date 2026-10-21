using FakeItEasy;
using Google.Protobuf;
using HardwareManifestPlugin;
using HardwareManifestProto;
using hirs;
using Hirs.Pb;
using NUnit.Framework;

namespace hirsTest.provisioner {
    public class ComponentEvidenceKeysTests {
        private static readonly byte[] CertIdentifier = [.. Enumerable.Range(0, 48).Select(i => (byte)i)];
        private static readonly byte[] Nonce = [.. Enumerable.Range(100, 32).Select(i => (byte)i)];

        [Test]
        public void TestOffersComponentWithCertificateIdentifier() {
            ComponentEvidenceKeys keys = ComponentEvidenceKeys.FromPlugins([FakeEpscPlugin()]);

            Assert.That(keys.KeyCandidates, Has.Count.EqualTo(1));
            KeyCandidate candidate = keys.KeyCandidates[0];
            Assert.Multiple(() => {
                Assert.That(candidate.Role, Is.EqualTo(new[] { KeyRole.DeviceIdentity }));
                Assert.That(candidate.External.EvidenceFormat, Is.EqualTo(EvidenceFormat.HpEpscBootlog));
                Assert.That(candidate.External.CertDigest.ToByteArray(), Is.EqualTo(CertIdentifier));
                Assert.That(candidate.KeyId.ToByteArray(), Is.EqualTo(
                    KeyTemplateCatalog.DeriveKeyId(KeyRole.DeviceIdentity, ProvisioningOrigin.OemExternal, CertIdentifier)));
            });
        }

        [Test]
        public void TestIgnoresPluginsWithoutEvidence() {
            IHardwareManifestPlugin plugin = FakeEpscPlugin();
            A.CallTo(() => plugin.SupportsComponentEvidence).Returns(false);

            Assert.That(ComponentEvidenceKeys.FromPlugins([plugin]).KeyCandidates, Is.Empty);
        }

        [Test]
        public void TestAnswersChallengeWithPluginEvidence() {
            IHardwareManifestPlugin plugin = FakeEpscPlugin();
            byte[] evidence = [1, 2, 3];
            A.CallTo(() => plugin.GatherComponentEvidence(A<byte[]>.That.IsSameSequenceAs(Nonce))).Returns([
                new ComponentEvidence {
                    ComponentIndex = 1,
                    EvidenceFormat = "HP_EPSC_BOOTLOG",
                    EvidenceVersion = "1",
                    Evidence = evidence,
                    CertificateChain = [[10], [20]]
                }
            ]);
            ComponentEvidenceKeys keys = ComponentEvidenceKeys.FromPlugins([plugin]);
            VerificationChallenge challenge = new() {
                KeyId = keys.KeyCandidates[0].KeyId,
                ExternalEvidence = new ExternalEvidenceChallenge { Nonce = ByteString.CopyFrom(Nonce), EvidenceFormat = EvidenceFormat.HpEpscBootlog }
            };

            List<VerificationResult> results = keys.Answer([challenge]);

            Assert.That(results, Has.Count.EqualTo(1));
            ExternalEvidenceResult answer = results[0].ExternalEvidence;
            Assert.Multiple(() => {
                Assert.That(results[0].KeyId, Is.EqualTo(challenge.KeyId));
                Assert.That(answer.EvidenceFormat, Is.EqualTo(EvidenceFormat.HpEpscBootlog));
                Assert.That(answer.EvidenceVersion, Is.EqualTo("1"));
                Assert.That(answer.Evidence.ToByteArray(), Is.EqualTo(evidence));
                Assert.That(answer.CertificateChain.Select(c => c.ToByteArray()), Is.EqualTo(new[] { new byte[] { 10 }, new byte[] { 20 } }));
            });
        }

        [Test]
        public void TestIgnoresChallengesForOtherKeys() {
            ComponentEvidenceKeys keys = ComponentEvidenceKeys.FromPlugins([FakeEpscPlugin()]);
            VerificationChallenge challenge = new() {
                KeyId = ByteString.CopyFrom([9, 9, 9]),
                ExternalEvidence = new ExternalEvidenceChallenge { Nonce = ByteString.CopyFrom(Nonce) }
            };

            Assert.That(keys.Answer([challenge]), Is.Empty);
        }

        /**
         * An EpSC plugin whose manifest has another component first, so the EpSC is at index 1.
         */
        private static IHardwareManifestPlugin FakeEpscPlugin() {
            ManifestV2 manifest = new();
            manifest.COMPONENTS.Add(new ComponentIdentifier { MANUFACTURER = "Other", MODEL = "No identifier" });
            manifest.COMPONENTS.Add(new ComponentIdentifier {
                MANUFACTURER = "HP Inc.",
                MODEL = "HP EpSC 24",
                PLATFORMCERT = new CertificateIdentifier {
                    HASHEDCERTIDENTIFIER = new HashedCertificateIdentifier {
                        HASHALG = "2.16.840.1.101.3.4.2.2",
                        HASHVALUE = Convert.ToBase64String(CertIdentifier)
                    }
                }
            });
            IHardwareManifestPlugin plugin = A.Fake<IHardwareManifestPlugin>();
            A.CallTo(() => plugin.Name).Returns("paccor.hp.epsc");
            A.CallTo(() => plugin.SupportsComponentEvidence).Returns(true);
            A.CallTo(() => plugin.ManifestV2).Returns(manifest);
            return plugin;
        }
    }
}
