using System;
using System.Collections.Generic;
using System.Text;
using Tpm2Lib;

namespace hirs {
    public class CommandTpmQuoteResponse : Tpm2QuoteResponse {

        private readonly Attest quoted1;
        private readonly ISignatureUnion signature1;
        private readonly Tpm2bDigest[] pcrValues1;

        public CommandTpmQuoteResponse(Attest quoted, ISignatureUnion signature, Tpm2bDigest[] pcrValues) {
            quoted1 = quoted;
            signature1 = signature;
            pcrValues1 = pcrValues != null ? (Tpm2bDigest[])pcrValues.Clone() : null;
        }
        public new Attest quoted => quoted1;
        public new ISignatureUnion signature => signature1;
        public Tpm2bDigest[] pcrValues => pcrValues1;

        //TODO Fix ACA so that I don't have to re-format data in this way
        public static void formatQuoteInfoSigForAca(Attest quoteInfo, ISignatureUnion quoteSig, out string quoteInfoSigStr) {
            quoteInfoSigStr = "quoted:";
            quoteInfoSigStr += BitConverter.ToString(quoteInfo.GetTpm2BRepresentation()).Replace("-", "").ToLower().Trim();
            quoteInfoSigStr += "signature:";
            quoteInfoSigStr += BitConverter.ToString(((SignatureRsassa)quoteSig).GetTpm2BRepresentation()).Replace("-", "").ToLower().Trim();
        }
    }
}
