using Hirs.Pb;
using hirs;
using FakeItEasy;
using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Tpm2Lib;
using Xunit;

namespace hirsTest {
    public class ProvisionerTests {
        [Fact]
        public async void TestGood() {
            const string address = "https://127.0.0.1:8443/";
            byte[] ekCert = Encoding.UTF8.GetBytes("EK CERTIFICATE");
            byte[] secret = Encoding.UTF8.GetBytes("AuthCredential Secret");
            byte[] acaIssuedCert = Encoding.UTF8.GetBytes("ACA ISSUED CERTIFICATE");
            byte[] integrityHMAC = Convert.FromBase64String("VAtedc1RlNA1w0XfrtwmhE0ILBlILP6163Tur5HRIo0=");
            byte[] encIdentity = Convert.FromBase64String("6e2oGBsK3H9Vzbj667ZsjnVOtvpSpQ==");
            byte[] encryptedSecret = Convert.FromBase64String("NekvnOX8RPRdyd0/cxBI4FTCuNkiu0KAnS28yT7yYJUL5Lwfcv5ctEK6zQA0fq0IsX5TlAYSidGKxrAilOSwALJmJ+m7sMiXwMKrZn1cd4gzXObZEQimQoWgSEQbPO7rfpUn1UfI8K5SzmUFUTxc5X3D8zFonaEBp6QCjtdLegKGgioCDcQFdz20Y0PFAa1Itug7YbZdCFpfit570eQQinmqdVryiNyn6CLQdMgIejuBxoEpoTSWszB5eFKEdn5g/+8wcvhp6RpNBQ0hikF+6688TOVK/j8n3JDwKVltJ/WNHjVO+lxa2aLIMJRgs5ZRuzuz6OSMf10KqJjSWZE04w==");
            byte[] credentialBlob = Convert.FromBase64String("OAAAIFQLXnXNUZTQNcNF367cJoRNCCwZSCz+tet07q+R0SKN6e2oGBsK3H9Vzbj667ZsjnVOtvpSpQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAATXpL5zl/ET0XcndP3MQSOBUwrjZIrtCgJ0tvMk+8mCVC+S8H3L+XLRCus0ANH6tCLF+U5QGEonRisawIpTksACyZifpu7DIl8DCq2Z9XHeIM1zm2REIpkKFoEhEGzzu636VJ9VHyPCuUs5lBVE8XOV9w/MxaJ2hAaekAo7XS3oChoIqAg3EBXc9tGNDxQGtSLboO2G2XQhaX4ree9HkEIp5qnVa8ojcp+gi0HTICHo7gcaBKaE0lrMweXhShHZ+YP/vMHL4aekaTQUNIYpBfuuvPEzlSv4/J9yQ8ClZbSf1jR41TvpcWtmiyDCUYLOWUbs7s+jkjH9dCqiY0lmRNOM=");
            TpmPublic ekPublic = CommandTpm.generateEKTemplateL1();
            TpmPublic akPublic = new TpmPublic(TpmAlgId.Sha256, ObjectAttr.None, System.Text.Encoding.UTF8.GetBytes("AK PUBLIC AUTH POLICY"), new RsaParms(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null), new SchemeRsassa(TpmAlgId.Sha256), 2048, 0), new Tpm2bPublicKeyRsa());
            Tpm2bDigest[] sha1Values = new Tpm2bDigest[] { new Tpm2bDigest(System.Text.Encoding.UTF8.GetBytes("SHA1 DIGEST1")) };
            Tpm2bDigest[] sha256Values = new Tpm2bDigest[] { new Tpm2bDigest(System.Text.Encoding.UTF8.GetBytes("SHA256 DIGEST1")) };
            DeviceInfo dv = new DeviceInfo();
            string paccorOutput = "paccor output";
            
            CommandTpmQuoteResponse ctqr = null;
            IdentityClaimResponse idClaimResp = new IdentityClaimResponse();
            idClaimResp.CredentialBlob = Google.Protobuf.ByteString.CopyFrom(credentialBlob);
            CertificateResponse certResp = new CertificateResponse();
            certResp.Certificate = Google.Protobuf.ByteString.CopyFrom(acaIssuedCert);

            IHirsAcaTpm tpm = A.Fake<IHirsAcaTpm>();
            byte[] name = null, qualifiedName = null;
            A.CallTo(() => tpm.getCertificateFromNvIndex(CommandTpm.DefaultEkcNvIndex)).Returns(ekCert);
            A.CallTo(() => tpm.createEndorsementKey(CommandTpm.DefaultEkHandle)).DoesNothing();
            A.CallTo(() => tpm.readPublicArea(CommandTpm.DefaultEkHandle, out name, out qualifiedName)).Returns(ekPublic);
            A.CallTo(() => tpm.createAttestationKey(CommandTpm.DefaultEkHandle, CommandTpm.DefaultAkHandle, false)).DoesNothing();
            A.CallTo(() => tpm.readPublicArea(CommandTpm.DefaultAkHandle, out name, out qualifiedName)).Returns(akPublic);
            //A.CallTo(() => tpm.getPcrList(TpmAlgId.Sha1, A<uint[]>.Ignored)).Returns(sha1Values);
            //A.CallTo(() => tpm.getPcrList(TpmAlgId.Sha256, A<uint[]>.Ignored)).Returns(sha256Values);
            A.CallTo(() => tpm.getQuote(CommandTpm.DefaultAkHandle, TpmAlgId.Sha256, secret, out ctqr, A<uint[]>.Ignored)).DoesNothing();

            IHirsDeviceInfoCollector collector = A.Fake<IHirsDeviceInfoCollector>();
            A.CallTo(() => collector.collectDeviceInfo(address)).Returns(dv);

            IHirsAcaClient client = A.Fake<IHirsAcaClient>();
            IdentityClaim idClaim = client.createIdentityClaim(dv, akPublic, ekPublic, ekCert, null, paccorOutput);
            CertificateRequest certReq = client.createAkCertificateRequest(secret, ctqr);
            A.CallTo(() => client.postIdentityClaim(idClaim)).Returns(Task.FromResult<IdentityClaimResponse>(idClaimResp));
            A.CallTo(() => client.postCertificateRequest(certReq)).Returns(Task.FromResult<CertificateResponse>(certResp));

            Settings settings = A.Fake<Settings>();
            A.CallTo(() => settings.getAcaAddress()).Returns(new Uri(address));
            A.CallTo(() => settings.getPaccorOutput()).Returns(paccorOutput);
            A.CallTo(() => settings.gatherPlatformCertificatesFromEFI()).Returns(null);
            A.CallTo(() => settings.gatherRIMBasesFromEFI()).Returns(null);
            A.CallTo(() => settings.gatherSupportRIMELsFromEFI()).Returns(null);
            A.CallTo(() => settings.gatherSupportRIMPCRsFromEFI()).Returns(null);
            A.CallTo(() => settings.gatherEventLogFromAppsettingsPath()).Returns(null);

            CLI cli = A.Fake<CLI>();

            IHirsProvisioner p = A.Fake<Provisioner>();
            p.setSettings(settings);
            p.setCLI(cli);
            p.setClient(client);

            p.setDeviceInfoCollector(collector); // Give the provisioner the mocked collector
            int result = await p.provision(tpm);

            A.CallTo(() => tpm.activateCredential(CommandTpm.DefaultAkHandle, CommandTpm.DefaultEkHandle, A<byte[]>.That.IsSameSequenceAs(integrityHMAC), A<byte[]>.That.IsSameSequenceAs(encIdentity), A<byte[]>.That.IsSameSequenceAs(encryptedSecret))).MustHaveHappenedOnceExactly();
            Assert.Equal(0, result);
        }

        [Fact]
        public async void TestIssueWithIdentityClaimResponse() {
            const string address = "https://127.0.0.1:8443/";
            byte[] ekCert = Encoding.UTF8.GetBytes("EK CERTIFICATE");
            byte[] acaIssuedCert = Encoding.UTF8.GetBytes("ACA ISSUED CERTIFICATE");
            TpmPublic ekPublic = CommandTpm.generateEKTemplateL1();
            TpmPublic akPublic = new TpmPublic(TpmAlgId.Sha256, ObjectAttr.None, System.Text.Encoding.UTF8.GetBytes("AK PUBLIC AUTH POLICY"), new RsaParms(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null), new SchemeRsassa(TpmAlgId.Sha256), 2048, 0), new Tpm2bPublicKeyRsa());
            Tpm2bDigest[] sha1Values = new Tpm2bDigest[] { new Tpm2bDigest(System.Text.Encoding.UTF8.GetBytes("SHA1 DIGEST1")) };
            Tpm2bDigest[] sha256Values = new Tpm2bDigest[] { new Tpm2bDigest(System.Text.Encoding.UTF8.GetBytes("SHA256 DIGEST1")) };
            DeviceInfo dv = new DeviceInfo();
            string paccorOutput = "paccor output";
            IdentityClaimResponse idClaimResp = new IdentityClaimResponse();
            idClaimResp.ClearCredentialBlob();

            IHirsAcaTpm tpm = A.Fake<IHirsAcaTpm>();
            byte[] name = null, qualifiedName = null;
            A.CallTo(() => tpm.getCertificateFromNvIndex(CommandTpm.DefaultEkcNvIndex)).Returns(ekCert);
            A.CallTo(() => tpm.createEndorsementKey(CommandTpm.DefaultEkHandle)).DoesNothing();
            A.CallTo(() => tpm.readPublicArea(CommandTpm.DefaultEkHandle, out name, out qualifiedName)).Returns(ekPublic);
            A.CallTo(() => tpm.createAttestationKey(CommandTpm.DefaultEkHandle, CommandTpm.DefaultAkHandle, false)).DoesNothing();
            A.CallTo(() => tpm.readPublicArea(CommandTpm.DefaultAkHandle, out name, out qualifiedName)).Returns(ekPublic);
            A.CallTo(() => tpm.getPcrList(TpmAlgId.Sha1, A<uint[]>.Ignored)).Returns(sha1Values);
            A.CallTo(() => tpm.getPcrList(TpmAlgId.Sha256, A<uint[]>.Ignored)).Returns(sha256Values);

            IHirsDeviceInfoCollector collector = A.Fake<IHirsDeviceInfoCollector>();
            A.CallTo(() => collector.collectDeviceInfo(address)).Returns(dv);

            IHirsAcaClient client = A.Fake<IHirsAcaClient>();
            IdentityClaim idClaim = client.createIdentityClaim(dv, akPublic, ekPublic, ekCert, null, paccorOutput);
            A.CallTo(() => client.postIdentityClaim(idClaim)).WithAnyArguments().Returns(Task.FromResult<IdentityClaimResponse>(idClaimResp));

            Settings settings = A.Fake<Settings>();
            A.CallTo(() => settings.getAcaAddress()).Returns(new Uri(address));
            A.CallTo(() => settings.getPaccorOutput()).Returns(paccorOutput);
            A.CallTo(() => settings.gatherPlatformCertificatesFromEFI()).Returns(null);
            A.CallTo(() => settings.gatherRIMBasesFromEFI()).Returns(null);
            A.CallTo(() => settings.gatherSupportRIMELsFromEFI()).Returns(null);
            A.CallTo(() => settings.gatherSupportRIMPCRsFromEFI()).Returns(null);
            A.CallTo(() => settings.gatherEventLogFromAppsettingsPath()).Returns(null);

            CLI cli = A.Fake<CLI>();

            IHirsProvisioner p = A.Fake<Provisioner>();
            p.setSettings(settings);
            p.setCLI(cli);
            p.setClient(client);
            
            p.setDeviceInfoCollector(collector); // Give the provisioner the mocked collector
            int result = await p.provision(tpm);

            Assert.Equal(102, result);
        }
    }
}
