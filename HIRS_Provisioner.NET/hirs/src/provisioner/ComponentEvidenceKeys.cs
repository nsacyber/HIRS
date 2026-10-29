using Google.Protobuf;
using Google.Protobuf.Reflection;
using HardwareManifestPlugin;
using HardwareManifestProto;
using Hirs.Pb;
using Serilog;

namespace hirs {
    /**
     * Offers component keys from hardware manifest plugins to the ACA as external keys in the ProvisioningKeys registry.
     * The registry answers the ACA's evidence challenges for them by asking the plugin.
     * A component qualifies when its plugin supports component evidence and the component carries a hashed certificate
     * identifier (componentPlatformCert). The ACA finds the same identifier in the platform certificate.
     */
    public static class ComponentEvidenceKeys {
        private const int Sha384AlgId = 0x000C; // TPM_ALG_SHA384

        /**
         * Plugin interface 2.2 does not report a component's evidence format before a nonce arrives.
         * Until it does, the format is known per plugin.
         */
        private static readonly Dictionary<string, EvidenceFormat> EvidenceFormatByPlugin = new() {
            ["paccor.hp.epsc"] = EvidenceFormat.HpEpscBootlog
        };

        /**
         * Adds a key for each qualifying component.
         * @return how many component keys were added
         */
        public static int AddTo(ProvisioningKeys keys, IEnumerable<IHardwareManifestPlugin> plugins) {
            int added = 0;
            foreach (IHardwareManifestPlugin plugin in plugins.Where(plugin => plugin.SupportsComponentEvidence)) {
                if (!EvidenceFormatByPlugin.TryGetValue(plugin.Name, out EvidenceFormat format)) {
                    Log.Debug("{Plugin} supports component evidence, but its evidence format is unknown.", plugin.Name);
                    continue;
                }
                for (int index = 0; index < plugin.ManifestV2.COMPONENTS.Count; index++) {
                    if (Add(keys, plugin, index, format)) {
                        added++;
                    }
                }
            }
            return added;
        }

        private static bool Add(ProvisioningKeys keys, IHardwareManifestPlugin plugin, int index, EvidenceFormat format) {
            HashedCertificateIdentifier? identifier = plugin.ManifestV2.COMPONENTS[index].PLATFORMCERT?.HASHEDCERTIDENTIFIER;
            if (identifier == null || string.IsNullOrEmpty(identifier.HASHVALUE)) {
                return false;
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
            if (keys.TryGet(candidate.KeyId, out ProvisioningKey _)) {
                Log.Debug("{Plugin} reported the same component key twice; offering it once.", plugin.Name);
                return false;
            }
            keys.AddExternal(candidate, challenge => GatherEvidence(plugin, index, challenge));
            Log.Debug("Offering a {Format} component key from {Plugin}.", format, plugin.Name);
            return true;
        }

        /**
         * Asks the plugin to sign the ACA's nonce. A component that returns no evidence still gets an empty result,
         * so the ACA records it as failed rather than unanswered.
         */
        private static ExternalEvidenceResult GatherEvidence(IHardwareManifestPlugin plugin, int index,
            ExternalEvidenceChallenge challenge) {
            Log.Information("----> Asking {Plugin} for component evidence.", plugin.Name);
            ComponentEvidence? evidence = plugin.GatherComponentEvidence(challenge.Nonce.ToByteArray())
                .FirstOrDefault(e => e.ComponentIndex == index);
            if (evidence == null) {
                Log.Warning("{Plugin} did not return component evidence. The ACA will reject the component.", plugin.Name);
                return new ExternalEvidenceResult();
            }
            return ToResult(evidence);
        }

        private static ExternalEvidenceResult ToResult(ComponentEvidence evidence) {
            EnumValueDescriptor? format = ProvisionerTpm2Reflection.Descriptor.FindTypeByName<EnumDescriptor>(nameof(EvidenceFormat))
                .FindValueByName(evidence.EvidenceFormat);
            ExternalEvidenceResult result = new() {
                EvidenceFormat = format == null ? EvidenceFormat.Unspecified : (EvidenceFormat)format.Number,
                EvidenceVersion = evidence.EvidenceVersion,
                Evidence = ByteString.CopyFrom(evidence.Evidence)
            };
            result.CertificateChain.AddRange(evidence.CertificateChain.Select(ByteString.CopyFrom));
            return result;
        }
    }
}
