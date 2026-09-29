using Hirs.Pb;
using Tpm2Lib;

namespace hirs {
    public interface IHirsAcaTpm {
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
        void CreateAttestationKey(uint ekHandleInt, uint akHandleInt, bool replace);
        void CreateStorageRootKey(uint srkHandleInt);
        void CreateLDevIDKey(uint srkHandleInt, string pubPath, string privPath, bool replace);
        uint LoadLDevIDKey(uint srkHandleInt, string pubPath, string privPath);
        byte[] ConvertLDevIDPublic(string ldevidPubPath);
        Tpm2bDigest[] GetPcrList(TpmAlgId pcrBankDigestAlg, uint[] pcrs = null);
        void GetQuote(uint akHandleInt, TpmAlgId pcrBankDigestAlg, byte[] nonce, out CommandTpmQuoteResponse ctqr, uint[] pcrs = null);
        byte[] ActivateCredential(uint akHandleInt, uint ekHandleInt, byte[] integrityHMAC, byte[] encIdentity, byte[] encryptedSecret);
        byte[] ActivateCredential(uint akHandleInt, uint ekHandleInt, byte[] credentialBlob, byte[] encryptedSecret);
        byte[] GetEventLog();
        byte[] Sign(uint keyHandleInt, byte[] digest, TpmAlgId hashAlg);
        bool SupportsAlgorithm(TpmAlgId algId);

    }
}
