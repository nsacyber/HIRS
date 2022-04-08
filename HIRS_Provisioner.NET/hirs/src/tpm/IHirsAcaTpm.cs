using System;
using System.Collections.Generic;
using System.Text;
using Tpm2Lib;

namespace hirs {
    public interface IHirsAcaTpm {
        byte[] getCertificateFromNvIndex(uint index);
        TpmPublic readPublicArea(uint handleInt, out byte[] name, out byte[] qualifiedName);
        void createEndorsementKey(uint ekHandleInt);
        void createAttestationKey(uint ekHandleInt, uint akHandleInt, bool replace);
        Tpm2bDigest[] getPcrList(TpmAlgId pcrBankDigestAlg, uint[] pcrs = null);
        void getQuote(uint akHandleInt, TpmAlgId pcrBankDigestAlg, byte[] nonce, out CommandTpmQuoteResponse ctqr, uint[] pcrs = null);
        byte[] activateCredential(uint akHandleInt, uint ekHandleInt, byte[] integrityHMAC, byte[] encIdentity, byte[] encryptedSecret);
        byte[] GetEventLog();

    }
}
