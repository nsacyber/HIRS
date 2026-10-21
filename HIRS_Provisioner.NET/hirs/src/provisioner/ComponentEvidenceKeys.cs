using Google.Protobuf;
using Google.Protobuf.Reflection;
using HardwareManifestPlugin;
using HardwareManifestProto;
using Hirs.Pb;
using Serilog;

namespace hirs {
    /**
     * Offers component keys from hardware manifest plugins to the ACA, and answers the ACA's evidence challenges for them.
     * A component qualifies when its plugin supports component evidence and the component carries a hashed certificate
     * identifier (componentPlatformCert). The ACA finds the same identifier in the platform certificate.
     */
    public class ComponentEvidenceKeys {
        private const int Sha384AlgId = 0x000C; // TPM_ALG_SHA384

        /**
         * Plugin interface 2.2 does not report a component's evidence format before a nonce arrives.
         * Until it does, the format is known per plugin.
         */
        private static readonly Dictionary<string, EvidenceFormat> EvidenceFormatByPlugin = new() {
            ["paccor.hp.epsc"] = EvidenceFormat.HpEpscBootlog
        };

        private readonly Dictionary<ByteString, ComponentKey> keysById = new();

        public IReadOnlyList<KeyCandidate> KeyCandidates => keysById.Values.Select(key => key.Candidate).ToList();

        public static ComponentEvidenceKeys FromPlugins(IEnumerable<IHardwareManifestPlugin> plugins) {
            ComponentEvidenceKeys keys = new();
            foreach (IHardwareManifestPlugin plugin in plugins.Where(plugin => plugin.SupportsComponentEvidence)) {
                if (!EvidenceFormatByPlugin.TryGetValue(plugin.Name, out EvidenceFormat format)) {
                    Log.Debug("{Plugin} supports component evidence, but its evidence format is unknown.", plugin.Name);
                    continue;
                }
                for (int index = 0; index < plugin.ManifestV2.COMPONENTS.Count; index++) {
                    keys.Add(plugin, index, format);
                }
            }
            return keys;
        }

        /**
         * Asks the plugins to sign the ACA's nonces. Challenges for other keys are ignored.
         */
        public List<VerificationResult> Answer(IEnumerable<VerificationChallenge> challenges) {
            List<VerificationResult> results = new();
            foreach (VerificationChallenge challenge in challenges.Where(c => c.ChallengeCase == VerificationChallenge.ChallengeOneofCase.ExternalEvidence)) {
                if (!keysById.TryGetValue(challenge.KeyId, out ComponentKey? key)) {
                    Log.Warning("The ACA sent an evidence challenge for a key this provisioner did not offer.");
                    continue;
                }
                Log.Information("----> Asking {Plugin} for component evidence.", key.Plugin.Name);
                ComponentEvidence? evidence = key.Plugin.GatherComponentEvidence(challenge.ExternalEvidence.Nonce.ToByteArray())
                    .FirstOrDefault(e => e.ComponentIndex == key.ComponentIndex);
                if (evidence == null) {
                    Log.Warning("{Plugin} did not return component evidence. The ACA will reject the component.", key.Plugin.Name);
                }
                results.Add(ToResult(challenge.KeyId, evidence));
            }
            return results;
        }

        private void Add(IHardwareManifestPlugin plugin, int index, EvidenceFormat format) {
            HashedCertificateIdentifier? identifier = plugin.ManifestV2.COMPONENTS[index].PLATFORMCERT?.HASHEDCERTIDENTIFIER;
            if (identifier == null || string.IsNullOrEmpty(identifier.HASHVALUE)) {
                return;
            }
            byte[] certIdentifier = Convert.FromBase64String(identifier.HASHVALUE);
            KeyCandidate candidate = new() {
                KeyId = ByteString.CopyFrom(KeyTemplateCatalog.DeriveKeyId(KeyRole.DeviceIdentity, ProvisioningOrigin.OemExternal, certIdentifier)),
                // The public key and certificate chain arrive with the evidence. The identifier is the one in the platform certificate.
                External = new ExternalKeyMaterial {
                    CertDigest = ByteString.CopyFrom(certIdentifier),
                    DigestAlgorithm = new AlgorithmId { Registry = AlgorithmRegistry.TpmAlgId, Value = Sha384AlgId },
                    EvidenceFormat = format
                }
            };
            candidate.Role.Add(KeyRole.DeviceIdentity);
            keysById[candidate.KeyId] = new ComponentKey(plugin, index, candidate);
            Log.Debug("Offering a {Format} component key from {Plugin}.", format, plugin.Name);
        }

        private static VerificationResult ToResult(ByteString keyId, ComponentEvidence? evidence) {
            VerificationResult result = new() { KeyId = keyId, ExternalEvidence = new ExternalEvidenceResult() };
            if (evidence == null) {
                return result;
            }
            EnumValueDescriptor? format = ProvisionerTpm2Reflection.Descriptor.FindTypeByName<EnumDescriptor>(nameof(EvidenceFormat))
                .FindValueByName(evidence.EvidenceFormat);
            result.ExternalEvidence.EvidenceFormat = format == null ? EvidenceFormat.Unspecified : (EvidenceFormat)format.Number;
            result.ExternalEvidence.EvidenceVersion = evidence.EvidenceVersion;
            result.ExternalEvidence.Evidence = ByteString.CopyFrom(evidence.Evidence);
            result.ExternalEvidence.CertificateChain.AddRange(evidence.CertificateChain.Select(ByteString.CopyFrom));
            return result;
        }

        private sealed record ComponentKey(IHardwareManifestPlugin Plugin, int ComponentIndex, KeyCandidate Candidate);
    }
}
