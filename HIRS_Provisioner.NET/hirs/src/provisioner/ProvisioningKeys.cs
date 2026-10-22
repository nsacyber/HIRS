using Google.Protobuf;
using Hirs.Pb;

namespace hirs {
    /**
     * The keys offered to the ACA in one provisioning run, findable by key_id.
     * A VerificationChallenge names the key it is for by key_id; this resolves it to the key's TPM handle.
     * Candidates keep the order they were added in, which is the order they are sent in the identity claim.
     */
    public class ProvisioningKeys {
        private readonly List<ProvisioningKey> keys = new();
        private readonly Dictionary<ByteString, ProvisioningKey> keysById = new();

        public IReadOnlyList<KeyCandidate> Candidates => keys.Select(key => key.Candidate).ToList();

        /**
         * @param handle the key's TPM handle. Null if it is not loaded
         * @param endorsementKey the EK
         */
        public void Add(KeyCandidate candidate, uint? handle, EndorsementKey? endorsementKey = null) {
            ProvisioningKey key = new(candidate, handle, endorsementKey);
            if (!keysById.TryAdd(candidate.KeyId, key)) {
                throw new ArgumentException("A key candidate with this key_id was already added.");
            }
            keys.Add(key);
        }

        public bool TryGet(ByteString keyId, out ProvisioningKey key) {
            return keysById.TryGetValue(keyId, out key!);
        }
    }

    /**
     * A key offered to the ACA
     * Links the key candidate with where the key is in the TPM.
     */
    public sealed record ProvisioningKey(KeyCandidate Candidate, uint? Handle, EndorsementKey? EndorsementKey);
}
