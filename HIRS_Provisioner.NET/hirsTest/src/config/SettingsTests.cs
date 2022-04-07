using hirs;
using System;
using System.Collections.Generic;
using System.Text;
using Xunit;

namespace hirsTest {
    public class SettingsTests {
        [Fact]
        public void TestConstructorWithAppsettings() {
            Settings settings = new Settings("./Resources/test/settings_test/appsettings.json");

            Assert.False(settings.IsAutoDetectTpmEnabled());
            Assert.True(settings.HasAcaAddress());
            Assert.Equal("https://127.0.0.1:8443/", settings.getAcaAddress().ToString());
            Assert.True(settings.HasEfiPrefix());
            Assert.True(settings.HasPaccorOutputFile());
            Assert.Equal("./Resources/test/settings_test/component_list", settings.getPaccorOutputFile());
            Assert.True(settings.hasEventLogPath());

            const string baseSerial = "Base Platform Cert Serial\n";
            const string deltaSerial = "Delta Platform Certificate Serial\n";
            const string deltaModel = "Delta Platform Certificiate Model\n";
            const string swidtagModel = "RIM Swidtag Model 1\n";
            const string rimelModel = "RIM EL Model 1\n";
            const string rimpcrModel = "RIM PCR Model 1\n";
            const string eventLogModel = "event log\n";
            const string componentList = "component list\n";

            string paccorOutput = settings.getPaccorOutput();
            IEnumerable<byte[]> platformCerts = settings.gatherPlatformCertificatesFromEFI();
            IEnumerable<byte[]> rims = settings.gatherRIMBasesFromEFI();
            IEnumerable<byte[]> rimELs = settings.gatherSupportRIMELsFromEFI();
            IEnumerable<byte[]> rimPCRs = settings.gatherSupportRIMPCRsFromEFI();
            byte[] eventLog = settings.gatherEventLogFromAppsettingsPath();

            Assert.Equal(componentList, paccorOutput);
            Assert.Collection<byte[]>(platformCerts,
                elem1 => Assert.Equal(Encoding.ASCII.GetBytes(baseSerial), elem1),
                elem2 => Assert.Equal(Encoding.ASCII.GetBytes(deltaSerial), elem2),
                elem3 => Assert.Equal(Encoding.ASCII.GetBytes(deltaModel), elem3));

            Assert.Collection<byte[]>(rims,
                elem1 => Assert.Equal(Encoding.ASCII.GetBytes(swidtagModel), elem1));

            Assert.Collection<byte[]>(rimELs,
                elem1 => Assert.Equal(Encoding.ASCII.GetBytes(rimelModel), elem1));

            Assert.Collection<byte[]>(rimPCRs,
                elem1 => Assert.Equal(Encoding.ASCII.GetBytes(rimpcrModel), elem1));

            Assert.Equal(eventLogModel, Encoding.ASCII.GetString(eventLog));
        }
    }
}
