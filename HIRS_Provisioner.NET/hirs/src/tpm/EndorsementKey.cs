using Hirs.Pb;
using Tpm2Lib;

namespace hirs {
    /**
     * An Endorsement Key that is loaded in the TPM and usable through <see cref="Handle"/>.
     * The EK is identified by its public area (and the public key in its certificate), never
     * by the handle: the handle is either a persistent one by convention (the Low Range EKs)
     * or a transient one for an EK that was regenerated from its template on demand.
     * Disposing releases a transient handle; persistent EKs are left in place.
     */
    public sealed class EndorsementKey : IDisposable {
        private Action? release;

        public EndorsementKey(EkTemplate template, TpmPublic pub, uint handle, bool isPersistent, Action? release) {
            Template = template;
            Public = pub;
            Handle = handle;
            IsPersistent = isPersistent;
            this.release = release;
        }

        public EkTemplate Template { get; }

        public TpmPublic Public { get; }

        // TPMT_PUBLIC
        public byte[] PublicArea => Public;

        // TPM handle
        public uint Handle { get; }

        // True if it is persistent
        public bool IsPersistent { get; }

        // Clean up
        public void Dispose() {
            Action? action = release;
            release = null;
            action?.Invoke();
        }
    }
}
