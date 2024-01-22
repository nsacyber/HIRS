using hirs;
using NUnit.Framework;
using System.Collections.Generic;
using System.Text;

namespace hirsTest {
    public class SettingsTests {
        [Test]
        public void TestConstructorWithAppsettings() {
            Settings settings = Settings.LoadSettingsFromFile("./Resources/test/settings_test/appsettings.json");
            settings.SetUpLog();
            settings.CompleteSetUp();

            Assert.Multiple(() => {
                Assert.That(settings.IsAutoDetectTpmEnabled(), Is.False);
                Assert.That(settings.HasAcaAddress(), Is.True);
                Assert.That(settings.aca_address_port.ToString(), Is.EqualTo("https://127.0.0.1:8443/"));
                Assert.That(settings.HasEfiPrefix(), Is.True);
                Assert.That(settings.HasPaccorOutputFromFile(), Is.True);
                Assert.That(settings.HasEventLogFromFile(), Is.True);
            });

            const string baseSerial = "Base Platform Cert Serial\n";
            const string deltaSerial = "Delta Platform Certificate Serial\n";
            const string deltaModel = "Delta Platform Certificiate Model\n";
            const string swidtagModel = "RIM Swidtag Model 1\n";
            const string rimelModel = "RIM EL Model 1\n";
            const string rimpcrModel = "RIM PCR Model 1\n";
            const string eventLogModel = "event log\n";
            const string componentList = "component list\n";

            string paccorOutput = settings.paccor_output;
            List<byte[]> platformCerts = settings.gatherPlatformCertificatesFromEFI();
            List<byte[]> rims = settings.gatherRIMBasesFromEFI();
            List<byte[]> rimELs = settings.gatherSupportRIMELsFromEFI();
            List<byte[]> rimPCRs = settings.gatherSupportRIMPCRsFromEFI();
            byte[] eventLog = settings.event_log;
            //Assert.Multiple(() => {
                Assert.That(paccorOutput, Is.EqualTo(componentList));
                Assert.That(platformCerts, Has.Count.EqualTo(3));
            
                Assert.That(platformCerts[0], Is.EqualTo(Encoding.ASCII.GetBytes(baseSerial)));
                Assert.That(platformCerts[1], Is.EqualTo(Encoding.ASCII.GetBytes(deltaSerial)));
                Assert.That(platformCerts[2], Is.EqualTo(Encoding.ASCII.GetBytes(deltaModel)));

                Assert.That(rims, Has.Count.EqualTo(1));
                Assert.That(rims[0], Is.EqualTo(Encoding.ASCII.GetBytes(swidtagModel)));

                Assert.That(rimELs, Has.Count.EqualTo(1));
                Assert.That(rimELs[0], Is.EqualTo(Encoding.ASCII.GetBytes(rimelModel)));

                Assert.That(rimPCRs, Has.Count.EqualTo(1));
                Assert.That(rimPCRs[0], Is.EqualTo(Encoding.ASCII.GetBytes(rimpcrModel)));

                Assert.That(Encoding.ASCII.GetString(eventLog), Is.EqualTo(eventLogModel));
            //});
        }
    }
}
