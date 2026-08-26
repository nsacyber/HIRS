using FakeItEasy;
using hirs;
using Hirs.Pb;
using NUnit.Framework;
using Tpm2Lib;

namespace hirsTest.provisioner {
    public class ProvisionerTests {
        [Test]
        public async Task TestGoodAsync() {
            const string address = "https://127.0.0.1:8443/";
            byte[] ekCert = [.. "EK CERTIFICATE"u8];
            byte[] secret = [.. "AuthCredential Secret"u8];
            const string acaIssuedCert = "ACA ISSUED CERTIFICATE";
            byte[] integrityHMAC = Convert.FromBase64String("VAtedc1RlNA1w0XfrtwmhE0ILBlILP6163Tur5HRIo0=");
            byte[] encIdentity = Convert.FromBase64String("6e2oGBsK3H9Vzbj667ZsjnVOtvpSpQ==");
            byte[] encryptedSecret = Convert.FromBase64String("NekvnOX8RPRdyd0/cxBI4FTCuNkiu0KAnS28yT7yYJUL5Lwfcv5ctEK6zQA0fq0IsX5TlAYSidGKxrAilOSwALJmJ+m7sMiXwMKrZn1cd4gzXObZEQimQoWgSEQbPO7rfpUn1UfI8K5SzmUFUTxc5X3D8zFonaEBp6QCjtdLegKGgioCDcQFdz20Y0PFAa1Itug7YbZdCFpfit570eQQinmqdVryiNyn6CLQdMgIejuBxoEpoTSWszB5eFKEdn5g/+8wcvhp6RpNBQ0hikF+6688TOVK/j8n3JDwKVltJ/WNHjVO+lxa2aLIMJRgs5ZRuzuz6OSMf10KqJjSWZE04w==");
            byte[] encryptedSecretBlob = Convert.FromBase64String("AQA16S+c5fxE9F3J3T9zEEjgVMK42SK7QoCdLbzJPvJglQvkvB9y/ly0QrrNADR+rQixflOUBhKJ0YrGsCKU5LAAsmYn6buwyJfAwqtmfVx3iDNc5tkRCKZChaBIRBs87ut+lSfVR8jwrlLOZQVRPFzlfcPzMWidoQGnpAKO10t6AoaCKgINxAV3PbRjQ8UBrUi26Dthtl0IWl+K3nvR5BCKeap1WvKI3KfoItB0yAh6O4HGgSmhNJazMHl4UoR2fmD/7zBy+GnpGk0FDSGKQX7rrzxM5Ur+PyfckPApWW0n9Y0eNU76XFrZosgwlGCzllG7O7Po5Ix/XQqomNJZkTTj");
            byte[] credentialBlob = Convert.FromBase64String("ADgAIFQLXnXNUZTQNcNF367cJoRNCCwZSCz+tet07q+R0SKN6e2oGBsK3H9Vzbj667ZsjnVOtvpSpQ==");
            TpmPublic ekPublic = CommandTpm.GenerateEKTemplateL1();
            TpmPublic akPublic = new(TpmAlgId.Sha256, ObjectAttr.None, [.. "AK PUBLIC AUTH POLICY"u8], new RsaParms(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null), new SchemeRsassa(TpmAlgId.Sha256), 2048, 0), new Tpm2bPublicKeyRsa());
            TpmPublic srkPublic = CommandTpm.GenerateSRKTemplateL1();
            TpmPublic ldevidPublic = new(TpmAlgId.Sha256, ObjectAttr.None, [.. "LDEVID PUBLIC AUTH POLICY"u8], new RsaParms(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null), new SchemeRsassa(TpmAlgId.Sha256), 2048, 0), new Tpm2bPublicKeyRsa());
            //Tpm2bDigest[] sha1Values = [new Tpm2bDigest(System.Text.Encoding.UTF8.GetBytes("SHA1 DIGEST1"))];
            //Tpm2bDigest[] sha256Values = [new Tpm2bDigest(System.Text.Encoding.UTF8.GetBytes("SHA256 DIGEST1"))];
            DeviceInfo dv = new();
            const string paccorOutput = "paccor output";
            
            CommandTpmQuoteResponse ctqr = null!;
            IdentityClaimResponse idClaimResp = new() {
                Status = ResponseStatus.Pass,
                CredentialBlob = Google.Protobuf.ByteString.CopyFrom(credentialBlob),
                EncryptedSecret = Google.Protobuf.ByteString.CopyFrom(encryptedSecretBlob)
            };
            CertificateResponse certResp = new() {
                Status = ResponseStatus.Pass,
                Certificate = acaIssuedCert
            };

            IHirsAcaTpm tpm = A.Fake<IHirsAcaTpm>();
            byte[] name = null!, qualifiedName = null!;
            A.CallTo(() => tpm.GetCertificateFromNvIndex(CommandTpm.DefaultEkcNvIndex)).Returns(ekCert);
            A.CallTo(() => tpm.CreateEndorsementKey(CommandTpm.DefaultEkHandle)).DoesNothing();
            A.CallTo(() => tpm.ReadPublicArea(CommandTpm.DefaultEkHandle, out name, out qualifiedName)).Returns(ekPublic);
            A.CallTo(() => tpm.CreateAttestationKey(CommandTpm.DefaultEkHandle, CommandTpm.DefaultAkHandle, false)).DoesNothing();
            A.CallTo(() => tpm.ReadPublicArea(CommandTpm.DefaultAkHandle, out name, out qualifiedName)).Returns(akPublic);
            A.CallTo(() => tpm.CreateStorageRootKey(CommandTpm.DefaultSrkHandle)).DoesNothing();
            A.CallTo(() => tpm.ReadPublicArea(CommandTpm.DefaultSrkHandle, out name, out qualifiedName)).Returns(srkPublic);
            A.CallTo(() => tpm.CreateLDevIDKey(CommandTpm.DefaultSrkHandle, "", "", false)).DoesNothing();
            //A.CallTo(() => tpm.getPcrList(TpmAlgId.Sha1, A<uint[]>.Ignored)).Returns(sha1Values);
            //A.CallTo(() => tpm.getPcrList(TpmAlgId.Sha256, A<uint[]>.Ignored)).Returns(sha256Values);
            A.CallTo(() => tpm.GetQuote(CommandTpm.DefaultAkHandle, TpmAlgId.Sha256, secret, out ctqr, A<uint[]>.Ignored)).DoesNothing();

            IHirsDeviceInfoCollector collector = A.Fake<IHirsDeviceInfoCollector>();
            A.CallTo(() => collector.CollectDeviceInfo(address)).Returns(dv);

            IHirsAcaClient client = A.Fake<IHirsAcaClient>();
            IdentityClaim idClaim = client.CreateIdentityClaim(dv, akPublic, ekPublic, ekCert, null!, paccorOutput, ldevidPublic);
            CertificateRequest certReq = client.CreateAkCertificateRequest(secret, ctqr);
            A.CallTo(() => client.PostIdentityClaim(idClaim)).Returns(Task.FromResult(idClaimResp));
            A.CallTo(() => client.PostCertificateRequest(certReq)).Returns(Task.FromResult(certResp));

            Settings settings = Settings.LoadSettingsFromFile("./Resources/test/settings_test/appsettings.json");
            settings.SetUpLog();
            settings.CompleteSetUp();

            CLI cli = A.Fake<CLI>();

            IHirsProvisioner p = A.Fake<Provisioner>();
            p.SetSettings(settings);
            p.SetCLI(cli);
            p.SetClient(client);

            p.SetDeviceInfoCollector(collector); // Give the provisioner the mocked collector
            int result = await p.Provision(tpm);

            A.CallTo(() => tpm.ActivateCredential(CommandTpm.DefaultAkHandle, CommandTpm.DefaultEkHandle, A<byte[]>.That.IsSameSequenceAs(integrityHMAC), A<byte[]>.That.IsSameSequenceAs(encIdentity), A<byte[]>.That.IsSameSequenceAs(encryptedSecret))).MustHaveHappenedOnceExactly();
            Assert.That(result, Is.EqualTo(0));
        }

        [Test]
        public async Task TestIssueWithIdentityClaimResponse() {
            const string address = "https://127.0.0.1:8443/";
            byte[] ekCert = [.. "EK CERTIFICATE"u8];
            //byte[] acaIssuedCert = [.. "ACA ISSUED CERTIFICATE"u8];
            TpmPublic ekPublic = CommandTpm.GenerateEKTemplateL1();
            TpmPublic akPublic = new(TpmAlgId.Sha256, ObjectAttr.None, [.. "AK PUBLIC AUTH POLICY"u8], new RsaParms(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null), new SchemeRsassa(TpmAlgId.Sha256), 2048, 0), new Tpm2bPublicKeyRsa());
            TpmPublic srkPublic = CommandTpm.GenerateSRKTemplateL1();
            TpmPublic ldevidPublic = new(TpmAlgId.Sha256, ObjectAttr.None, [.. "LDEVID PUBLIC AUTH POLICY"u8], new RsaParms(new SymDefObject(TpmAlgId.Null, 0, TpmAlgId.Null), new SchemeRsassa(TpmAlgId.Sha256), 2048, 0), new Tpm2bPublicKeyRsa());
            Tpm2bDigest[] sha1Values = [new([.. "SHA1 DIGEST1"u8])];
            Tpm2bDigest[] sha256Values = [new([.. "SHA256 DIGEST1"u8])];
            DeviceInfo dv = new();
            const string paccorOutput = "paccor output";
            IdentityClaimResponse idClaimResp = new();
            idClaimResp.ClearCredentialBlob();

            IHirsAcaTpm tpm = A.Fake<IHirsAcaTpm>();
            byte[] name = null!, qualifiedName = null!;
            A.CallTo(() => tpm.GetCertificateFromNvIndex(CommandTpm.DefaultEkcNvIndex)).Returns(ekCert);
            A.CallTo(() => tpm.CreateEndorsementKey(CommandTpm.DefaultEkHandle)).DoesNothing();
            A.CallTo(() => tpm.ReadPublicArea(CommandTpm.DefaultEkHandle, out name, out qualifiedName)).Returns(ekPublic);
            A.CallTo(() => tpm.CreateAttestationKey(CommandTpm.DefaultEkHandle, CommandTpm.DefaultAkHandle, false)).DoesNothing();
            A.CallTo(() => tpm.ReadPublicArea(CommandTpm.DefaultAkHandle, out name, out qualifiedName)).Returns(ekPublic);
            A.CallTo(() => tpm.CreateStorageRootKey(CommandTpm.DefaultSrkHandle)).DoesNothing();
            A.CallTo(() => tpm.ReadPublicArea(CommandTpm.DefaultSrkHandle, out name, out qualifiedName)).Returns(srkPublic);
            A.CallTo(() => tpm.CreateLDevIDKey(CommandTpm.DefaultSrkHandle, "", "", false)).DoesNothing();
            A.CallTo(() => tpm.GetPcrList(TpmAlgId.Sha1, A<uint[]>.Ignored)).Returns(sha1Values);
            A.CallTo(() => tpm.GetPcrList(TpmAlgId.Sha256, A<uint[]>.Ignored)).Returns(sha256Values);

            IHirsDeviceInfoCollector collector = A.Fake<IHirsDeviceInfoCollector>();
            A.CallTo(() => collector.CollectDeviceInfo(address)).Returns(dv);

            IHirsAcaClient client = A.Fake<IHirsAcaClient>();
            IdentityClaim idClaim = client.CreateIdentityClaim(dv, akPublic, ekPublic, ekCert, null!, paccorOutput, ldevidPublic);
            A.CallTo(() => client.PostIdentityClaim(idClaim)).WithAnyArguments().Returns(Task.FromResult(idClaimResp));

            Settings settings = Settings.LoadSettingsFromFile("./Resources/test/settings_test/appsettings.json");
            settings.SetUpLog();
            settings.CompleteSetUp();

            CLI cli = A.Fake<CLI>();

            IHirsProvisioner p = A.Fake<Provisioner>();
            p.SetSettings(settings);
            p.SetCLI(cli);
            p.SetClient(client);
            
            p.SetDeviceInfoCollector(collector); // Give the provisioner the mocked collector
            int result = await p.Provision(tpm);

            Assert.That(result, Is.EqualTo((int)ClientExitCodes.MAKE_CREDENTIAL_BLOB_MALFORMED));
        }
    }
}
