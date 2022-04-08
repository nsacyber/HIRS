using Hirs.Pb;
using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;

namespace hirs {
    public interface IHirsAcaClient {
        /// <summary>
        /// Send the <see cref="IdentityClaim"/> to the ACA. The claim is delivered
        /// asynchronously to the ACA. However, the client will wait for the response.
        /// </summary>
        /// <param name="identityClaim">Evidence about the client.</param>
        /// <returns>The <see cref="Task"/>&lt;<see cref="IdentityClaimResponse"/>&gt; from the
        /// ACA. The response is wrapped in a Task.</returns>
        Task<IdentityClaimResponse> postIdentityClaim(IdentityClaim identityClaim);
        /// <summary>
        /// Send the <see cref="CertificateRequest"/> to the ACA. The request is delivered
        /// asynchronously to the ACA. However, the client will wait for the response.
        /// </summary>
        /// <param name="certReq">The request for a certificate. Should contain evidence from
        /// client to enable nonce verification.</param>
        /// <returns>The <see cref="Task"/>&lt;<see cref="CertificateResponse"/>&gt; from the ACA.
        /// The response is wrapped in a Task. It will contain a certificate or the reason why
        /// the certificate request was rejected.</returns>
        Task<CertificateResponse> postCertificateRequest(CertificateRequest certReq);
        /// <summary>
        /// Collect client evidence regarding a Device into an object that can be interpreted by
        /// the ACA.
        /// </summary>
        /// <param name="dv">Facts about the Device.</param>
        /// <param name="akPublicArea">The public AK retrieved as a TPM2B_PUBLIC.</param>
        /// <param name="ekPublicArea">The public EK retrieved as a TPM2B_PUBLIC.</param>
        /// <param name="endorsementCredential">The public EK certificate, encoded in DER or
        /// PEM.</param>
        /// <param name="platformCredentials">Any platform certificates relevant to the Device,
        /// encoded in DER or PEM.</param>
        /// <param name="paccoroutput">Platform Manifest in a JSON format.</param>
        /// <returns>An <see cref="IdentityClaim"/> object that can be sent to the ACA.</returns>
        IdentityClaim createIdentityClaim(DeviceInfo dv, byte[] akPublicArea, byte[] ekPublicArea,
                                       byte[] endorsementCredential,
                                       List<byte[]> platformCredentials, string paccoroutput);
        /// <summary>
        /// Collect answers to verification requirements regarding a Device into an object that
        /// can be interpreted by the ACA.
        /// </summary>
        /// <param name="secret">Verification data.</param>
        /// <param name="ctqr">TPM Quote data from the client Device.</param>
        /// <returns>A <see cref="CertificateRequest"/> object that can be sent to the
        /// ACA.</returns>
        CertificateRequest createAkCertificateRequest(byte[] secret, CommandTpmQuoteResponse ctqr);
    }
}
