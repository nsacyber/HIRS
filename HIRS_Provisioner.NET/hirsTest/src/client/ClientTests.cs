using hirs;
using Hirs.Pb;
using FakeItEasy;
using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Text;
using Xunit;
using System.Threading.Tasks;
using Tpm2Lib;

namespace hirsTest {
    public class ClientTests {
        public static readonly string localhost = "https://127.0.0.1:8443/";

        [Fact]
        public void TestCreateIdentityClaim() {
            IHirsAcaClient client = new Client(ClientTests.localhost);

            DeviceInfo dv = new DeviceInfo();
            byte[] akPub = new byte[] { };
            byte[] ekPub = new byte[] { };
            byte[] ekc = new byte[] { };
            List<byte[]> pcs = new List<byte[]>();
            pcs.Add(new byte[] { });
            string componentList= "";

            IdentityClaim obj = client.createIdentityClaim(dv, akPub, ekPub, ekc, pcs, componentList);

            Assert.NotNull(obj.Dv);
            Assert.True(obj.HasAkPublicArea);
            Assert.True(obj.HasEkPublicArea);
            Assert.True(obj.HasEndorsementCredential);
            Assert.Single(obj.PlatformCredential);
            Assert.True(obj.HasPaccorOutput);
        }

        [Fact]
        public void TestCreateAkCertificateRequest() {
            IHirsAcaClient client = new Client(ClientTests.localhost);

            byte[] secret = Encoding.UTF8.GetBytes("secret");
            Attest quoted = new Attest(Generated.None, Encoding.UTF8.GetBytes("QUALIFIED SIGNER"), Encoding.UTF8.GetBytes("EXTRA DATA"), new ClockInfo(0, 0, 0, 0), 0, new QuoteInfo());
            ISignatureUnion signature = new SignatureRsassa();
            Tpm2bDigest[] pcrValues = new Tpm2bDigest[] { new Tpm2bDigest(Encoding.UTF8.GetBytes("SHA1 DIGEST1")) };

            CommandTpmQuoteResponse ctqr = new CommandTpmQuoteResponse(quoted, signature, pcrValues);

            CertificateRequest obj = client.createAkCertificateRequest(secret, ctqr);

            Assert.True(obj.HasNonce);
            Assert.True(obj.HasQuote);
        }
    }
}
