using Hirs.Pb;
using Tpm2Lib;

namespace hirs {
    public interface IHirsAcaTpm : IDisposable {
        bool IsTpmPresent();
        byte[] GetCertificateFromNvIndex(uint index);
        TpmPublic ReadPublicArea(uint handleInt, out byte[] name, out byte[] qualifiedName);
        /// <summary>
        /// Returns a usable EK for the template, or null when the TPM cannot provide one.
        /// Templates with a conventional persistent handle (the Low Range EKs) are kept there when the
        /// handle is free or already holds the right key; otherwise the EK is regenerated from its
        /// template into a transient handle that is released when the result is disposed.
        /// </summary>
        EndorsementKey? AcquireEndorsementKey(EkTemplate template);
        /// <summary>
        /// True if the TPM already has this EK
        /// NV index defined. Does not create a key.
        /// </summary>
        bool HasEndorsementKey(EkTemplate template);
        int EvictLegacyAttestationKeys();
        /// <summary>
        /// Creates the AK under the SRK and saves it as files.
        /// </summary>
        void CreateAttestationKey(string pubPath, string privPath, AsymmetricKeyTemplate template, bool replace);
        void CreateLDevIDKey(string pubPath, string privPath, bool replace);
        /// <summary>
        /// Loads a key saved to files under the SRK.
        /// </summary>
        uint LoadKeyFromFiles(string pubPath, string privPath);
        void FlushHandle(uint handleInt);
        byte[] ReadPublicAreaFromFile(string pubPath);
        /// <summary>
        /// The SHA PCR banks the TPM has PCRs allocated in.
        /// </summary>
        IReadOnlyList<TpmAlgId> GetPcrBanks();
        Tpm2bDigest[] GetPcrList(TpmAlgId pcrBankDigestAlg, uint[] pcrs = null);
        void GetQuote(uint akHandleInt, TpmAlgId pcrBankDigestAlg, byte[] nonce, out CommandTpmQuoteResponse ctqr, uint[] pcrs = null);
        byte[] ActivateCredential(uint akHandleInt, uint ekHandleInt, byte[] integrityHMAC, byte[] encIdentity, byte[] encryptedSecret);
        byte[] ActivateCredential(uint akHandleInt, uint ekHandleInt, byte[] credentialBlob, byte[] encryptedSecret);
        byte[] GetEventLog();
        byte[] Sign(uint keyHandleInt, byte[] digest, TpmAlgId hashAlg);
        bool SupportsAlgorithm(TpmAlgId algId);
        bool SupportsTemplate(TpmPublic template);

    }
}
