using hirs;
using NUnit.Framework;
using Tpm2Lib;

namespace hirsTest.tpm;

public class CommandTpmTests {
    [Test]
    public void TestMarshalling() {
        byte[] integrityHMAC = Convert.FromBase64String("VAtedc1RlNA1w0XfrtwmhE0ILBlILP6163Tur5HRIo0=");
        byte[] encIdentity = Convert.FromBase64String("6e2oGBsK3H9Vzbj667ZsjnVOtvpSpQ==");
        byte[] encryptedSecret = Convert.FromBase64String("NekvnOX8RPRdyd0/cxBI4FTCuNkiu0KAnS28yT7yYJUL5Lwfcv5ctEK6zQA0fq0IsX5TlAYSidGKxrAilOSwALJmJ+m7sMiXwMKrZn1cd4gzXObZEQimQoWgSEQbPO7rfpUn1UfI8K5SzmUFUTxc5X3D8zFonaEBp6QCjtdLegKGgioCDcQFdz20Y0PFAa1Itug7YbZdCFpfit570eQQinmqdVryiNyn6CLQdMgIejuBxoEpoTSWszB5eFKEdn5g/+8wcvhp6RpNBQ0hikF+6688TOVK/j8n3JDwKVltJ/WNHjVO+lxa2aLIMJRgs5ZRuzuz6OSMf10KqJjSWZE04w==");
        byte[] encryptedSecretBlob = Convert.FromBase64String("AQA16S+c5fxE9F3J3T9zEEjgVMK42SK7QoCdLbzJPvJglQvkvB9y/ly0QrrNADR+rQixflOUBhKJ0YrGsCKU5LAAsmYn6buwyJfAwqtmfVx3iDNc5tkRCKZChaBIRBs87ut+lSfVR8jwrlLOZQVRPFzlfcPzMWidoQGnpAKO10t6AoaCKgINxAV3PbRjQ8UBrUi26Dthtl0IWl+K3nvR5BCKeap1WvKI3KfoItB0yAh6O4HGgSmhNJazMHl4UoR2fmD/7zBy+GnpGk0FDSGKQX7rrzxM5Ur+PyfckPApWW0n9Y0eNU76XFrZosgwlGCzllG7O7Po5Ix/XQqomNJZkTTj");
        byte[] credentialBlob = Convert.FromBase64String("ADgAIFQLXnXNUZTQNcNF367cJoRNCCwZSCz+tet07q+R0SKN6e2oGBsK3H9Vzbj667ZsjnVOtvpSpQ==");

        using (Assert.EnterMultipleScope()) {
            Assert.That(CommandTpm.CanMarshal<Tpm2bIdObject>(credentialBlob), Is.True);
            //Assert.That(CommandTpm.CanMarshal<IdObject>(credentialBlob), Is.True);
            Assert.That(CommandTpm.CanMarshal<Tpm2bEncryptedSecret>(encryptedSecretBlob), Is.True);
        }
        
        Tpm2bIdObject credential = CommandTpm.Marshal<Tpm2bIdObject>(credentialBlob);
        Tpm2bEncryptedSecret encrypted = CommandTpm.Marshal<Tpm2bEncryptedSecret>(encryptedSecretBlob);
        
        using (Assert.EnterMultipleScope()) {
            Assert.That(credential.credential.integrityHMAC, Is.EqualTo(integrityHMAC));
            Assert.That(credential.credential.encIdentity, Is.EqualTo(encIdentity));
            Assert.That(encrypted.secret, Is.EqualTo(encryptedSecret));
        }
    }
}