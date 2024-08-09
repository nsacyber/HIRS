using System;
using System.Collections.Generic;
using System.Text;
using Tpm2Lib;

namespace hirs {
    public interface IHirsAcaTpm {
        byte[] GetCertificateFromNvIndex(uint index);
        TpmPublic ReadPublicArea(uint handleInt, out byte[] name, out byte[] qualifiedName);
        void CreateEndorsementKey(uint ekHandleInt);
        void CreateAttestationKey(uint ekHandleInt, uint akHandleInt, bool replace);
        void CreateStorageRootKey(uint srkHandleInt);
        void CreateLDevIDKey(uint srkHandleInt, string pubPath, string privPath, bool replace);
        byte[] ConvertLDevIDPublic(string ldevidPubPath);
        Tpm2bDigest[] GetPcrList(TpmAlgId pcrBankDigestAlg, uint[] pcrs = null);
        void GetQuote(uint akHandleInt, TpmAlgId pcrBankDigestAlg, byte[] nonce, out CommandTpmQuoteResponse ctqr, uint[] pcrs = null);
        byte[] ActivateCredential(uint akHandleInt, uint ekHandleInt, byte[] integrityHMAC, byte[] encIdentity, byte[] encryptedSecret);
        byte[] GetEventLog();

    }
}
