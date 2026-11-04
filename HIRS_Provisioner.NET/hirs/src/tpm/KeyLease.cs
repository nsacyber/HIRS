using Hirs.Pb;
using Tpm2Lib;

namespace hirs {
    /**
     * A key loaded in the TPM for one operation. The handle is valid only until the lease is disposed.
     * Disposing flushes a key that was loaded just for the lease.
     * Each lease checks that the TPM has the key that was sent to the ACA.
     * A handle is never trusted on its own.
     */
    public sealed class KeyLease : IDisposable {
        private Action? release;

        public KeyLease(uint handle, Action? release) {
            Handle = handle;
            this.release = release;
        }

        public uint Handle { get; }

        public void Dispose() {
            Action? action = release;
            release = null;
            action?.Invoke();
        }

        // An EK is derived from its template, so acquiring it again gives the same key
        public static KeyLease ForEndorsementKey(IHirsAcaTpm tpm, EkTemplate template, byte[] expectedPublicArea) {
            EndorsementKey ek = tpm.AcquireEndorsementKey(template)
                ?? throw new ProvisioningFailureException(ClientExitCodes.TPM_ERROR, $"The TPM did not provide EK {template}.");
            return Verified(new KeyLease(ek.Handle, ek.Dispose), ek.PublicArea, expectedPublicArea, $"EK {template}");
        }

        // The persistent handle is only where the key is expected to be
        public static KeyLease ForPersistentKey(IHirsAcaTpm tpm, uint handle, byte[] expectedPublicArea) {
            TpmPublic? publicArea = tpm.ReadPublicArea(handle, out byte[] _, out byte[] _);
            return Verified(new KeyLease(handle, null), publicArea == null ? [] : publicArea, expectedPublicArea,
                $"the key at 0x{handle:X}");
        }

        // A key saved to files under the SRK, such as the AK or the LDevID. It is flushed when the lease ends
        public static KeyLease ForKeyFiles(IHirsAcaTpm tpm, string pubPath, string privPath, byte[] expectedPublicArea,
            string description) {
            uint handle = tpm.LoadKeyFromFiles(pubPath, privPath);
            KeyLease lease = new(handle, () => tpm.FlushHandle(handle));
            TpmPublic? publicArea = tpm.ReadPublicArea(handle, out byte[] _, out byte[] _);
            return Verified(lease, publicArea == null ? [] : publicArea, expectedPublicArea, description);
        }

        private static KeyLease Verified(KeyLease lease, byte[] publicArea, byte[] expectedPublicArea, string description) {
            if (publicArea.AsSpan().SequenceEqual(expectedPublicArea)) {
                return lease;
            }
            lease.Dispose();
            throw new ProvisioningFailureException(ClientExitCodes.TPM_ERROR,
                $"The TPM no longer has the key that was sent to the ACA as {description}.");
        }
    }
}
