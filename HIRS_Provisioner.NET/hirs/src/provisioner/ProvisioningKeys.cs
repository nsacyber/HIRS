using Google.Protobuf;
using Hirs.Pb;
using Serilog;

namespace hirs {
    /**
     * The keys offered to the ACA in one provisioning run, findable by key_id.
     * A VerificationChallenge names the key it is for by key_id; this resolves it to the key and answers it.
     * Candidates keep the order they were added in, which is the order they are sent in the identity claim.
     */
    public class ProvisioningKeys {
        private readonly List<ProvisioningKey> keys = new();
        private readonly Dictionary<ByteString, ProvisioningKey> keysById = new();

        public IReadOnlyList<KeyCandidate> Candidates => keys.Select(key => key.Candidate).ToList();

        /**
         * A key in this TPM.
         * @param acquire loads the key for one operation
         */
        public ProvisioningKey Add(KeyCandidate candidate, Func<KeyLease> acquire) {
            return Add(new ProvisioningKey(candidate, acquire, null));
        }

        /**
         * A key outside the TPM, such as a component's Device ID, that proves possession with evidence.
         * @param gatherEvidence produces evidence for a challenge, or null if the component cannot
         */
        public ProvisioningKey AddExternal(KeyCandidate candidate,
            Func<ExternalEvidenceChallenge, ExternalEvidenceResult?> gatherEvidence) {
            return Add(new ProvisioningKey(candidate, null, gatherEvidence));
        }

        private ProvisioningKey Add(ProvisioningKey key) {
            if (!keysById.TryAdd(key.Candidate.KeyId, key)) {
                throw new ArgumentException("A key candidate with this key_id was already added.");
            }
            keys.Add(key);
            return key;
        }

        public bool TryGet(ByteString keyId, out ProvisioningKey key) {
            return keysById.TryGetValue(keyId, out key!);
        }

        /**
         * Answers the challenges this provisioner can answer. A challenge for an unknown key_id, or of a kind
         * not answered yet, is logged and left out of the results.
         */
        public List<VerificationResult> Answer(IEnumerable<VerificationChallenge> challenges) {
            List<VerificationResult> results = new();
            foreach (VerificationChallenge challenge in challenges) {
                if (!TryGet(challenge.KeyId, out ProvisioningKey key)) {
                    Log.Warning("The ACA sent a {Kind} challenge for a key that was not offered.", challenge.ChallengeCase);
                    continue;
                }
                VerificationResult? result = challenge.ChallengeCase switch {
                    VerificationChallenge.ChallengeOneofCase.ExternalEvidence => AnswerExternalEvidence(key, challenge),
                    _ => null
                };
                if (result == null) {
                    Log.Warning("The {Kind} challenge was not answered.", challenge.ChallengeCase);
                    continue;
                }
                results.Add(result);
            }
            return results;
        }

        private static VerificationResult? AnswerExternalEvidence(ProvisioningKey key, VerificationChallenge challenge) {
            if (!key.IsExternal) {
                return null;
            }
            ExternalEvidenceResult? evidence = key.GatherEvidence(challenge.ExternalEvidence);
            return evidence == null ? null : new VerificationResult { KeyId = challenge.KeyId, ExternalEvidence = evidence };
        }
    }

    /**
     * A key offered to the ACA: its candidate as sent, and how it proves possession.
     * A TPM key is loaded again for each operation; no handle is kept between operations.
     * An external key gathers evidence from its component.
     */
    public sealed class ProvisioningKey {
        private readonly Func<KeyLease>? acquire;
        private readonly Func<ExternalEvidenceChallenge, ExternalEvidenceResult?>? gatherEvidence;

        internal ProvisioningKey(KeyCandidate candidate, Func<KeyLease>? acquire,
            Func<ExternalEvidenceChallenge, ExternalEvidenceResult?>? gatherEvidence) {
            Candidate = candidate;
            this.acquire = acquire;
            this.gatherEvidence = gatherEvidence;
        }

        public KeyCandidate Candidate { get; }

        public bool IsExternal => gatherEvidence != null;

        // Loads the key for one operation. Dispose the lease when the operation is done
        public KeyLease Acquire() {
            return acquire != null ? acquire()
                : throw new InvalidOperationException("This key is not in the TPM, so it cannot be loaded.");
        }

        public ExternalEvidenceResult? GatherEvidence(ExternalEvidenceChallenge challenge) {
            return gatherEvidence != null ? gatherEvidence(challenge)
                : throw new InvalidOperationException("This key is in the TPM; it does not produce external evidence.");
        }
    }
}
