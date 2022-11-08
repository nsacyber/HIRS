package hirs.client.collector;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.when;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import hirs.DeviceInfoReportRequest;
import hirs.collector.CollectorException;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.enums.OSName;

/**
 * Unit tests for <code>DeviceInfoCollector</code>.
 */
@PrepareForTest(DeviceInfoCollector.class)
public class DeviceInfoCollectorTest extends PowerMockTestCase {

    private DeviceInfoCollector collector;

    /**
     * Prepares a test environment for each individual test.
     *
     * @throws CollectorException should not be thrown here as all collection is mocked
     */
    @BeforeMethod
    public void beforeMethod() throws CollectorException {
        collector = spy(new DeviceInfoCollector());
        PowerMockito.mockStatic(DeviceInfoCollector.class);

        // mock out serial number collection as it requires root
        when(DeviceInfoCollector.collectDmiDecodeValue(
                any(OSName.class), anyString())).thenReturn("some string");
        doReturn("Linux").when(collector).getSystemProperty(eq("os.name"));
    }

    /**
     * Tests that a DeviceInfoCollector will generate a DeviceInfoReport.
     *
     * @throws Exception if an error occurs creating mocked output
     */
    @Test
    public final void testCollect() throws Exception {
        final DeviceInfoReportRequest request = new DeviceInfoReportRequest();
        final DeviceInfoReport report =
                (DeviceInfoReport) collector.collect(request);
        final int numberOfDmiDecodeCalls = 9;

        // the following two lines assert that collectDmiDecodeValue was called 9 times
        PowerMockito.verifyStatic(DeviceInfoCollector.class, times(numberOfDmiDecodeCalls));
        DeviceInfoCollector.collectDmiDecodeValue(any(OSName.class), anyString());

        Assert.assertNotNull(report.getNetworkInfo());
        Assert.assertNotNull(report.getOSInfo());

        // Test the IP address in the report against the system that created it
        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();
        boolean equivalenceFound = false;
        while (interfaces.hasMoreElements()) {
            NetworkInterface netInt = interfaces.nextElement();
            Enumeration<InetAddress> addresses = netInt.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address.getHostAddress().equals(
                        report.getNetworkInfo().getIpAddress().
                                getHostAddress())) {
                    equivalenceFound = true;
                }
            }
        }
        Assert.assertTrue(equivalenceFound);
    }


    /**
     * Tests that hardware and firmware info is set correctly.
     *
     * @throws CollectorException should not be thrown here as all collection is mocked
     */
    @Test
    public final void testCollectProperInfo() throws CollectorException {
        DeviceInfoCollector mockedCollector = spy(DeviceInfoCollector.class);
        PowerMockito.mockStatic(DeviceInfoCollector.class);

        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-manufacturer")).thenReturn("Manufacturer");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-product-name")).thenReturn("Product name");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-serial-number")).thenReturn("Serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "chassis-serial-number")).thenReturn("Chassis serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "baseboard-serial-number")).thenReturn("Baseboard serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-version")).thenReturn("Version");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-vendor")).thenReturn("Bios vendor");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-version")).thenReturn("Bios version");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-release-date")).thenReturn("Bios release date");

        doReturn("Linux").when(mockedCollector).getSystemProperty("os.name");

        final DeviceInfoReportRequest request = new DeviceInfoReportRequest();
        DeviceInfoReport report = (DeviceInfoReport) mockedCollector.doCollect(request);
        Assert.assertEquals(report.getHardwareInfo().getManufacturer(),
                "Manufacturer");
        Assert.assertEquals(report.getHardwareInfo().getProductName(),
                "Product name");
        Assert.assertEquals(report.getHardwareInfo().getSystemSerialNumber(),
                "Serial number");
        Assert.assertEquals(report.getHardwareInfo().getChassisSerialNumber(),
                "Chassis serial number");
        Assert.assertEquals(report.getHardwareInfo().getBaseboardSerialNumber(),
                "Baseboard serial number");
        Assert.assertEquals(report.getHardwareInfo().getVersion(),
                "Version");
        Assert.assertEquals(report.getFirmwareInfo().getBiosVendor(),
                "Bios vendor");
        Assert.assertEquals(report.getFirmwareInfo().getBiosVersion(),
                "Bios version");
        Assert.assertEquals(report.getFirmwareInfo().getBiosReleaseDate(),
                "Bios release date");

    }

    /**
     * Tests that null hardware info is set to "Not specified".
     *
     * @throws CollectorException should not be thrown here as all collection is mocked
     */
    @Test
    public final void testCollectNullHardwareInfo() throws CollectorException {
        DeviceInfoCollector mockedCollector = spy(DeviceInfoCollector.class);
        PowerMockito.mockStatic(DeviceInfoCollector.class);

        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-manufacturer")).thenReturn(null);
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-product-name")).thenReturn(null);
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-serial-number")).thenReturn(null);
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "chassis-serial-number")).thenReturn(null);
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "baseboard-serial-number")).thenReturn(null);
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-version")).thenReturn(null);
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-vendor")).thenReturn("Bios vendor");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-version")).thenReturn("Bios version");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-release-date")).thenReturn("Bios release date");

        doReturn("Linux").when(mockedCollector).getSystemProperty("os.name");

        final DeviceInfoReportRequest request = new DeviceInfoReportRequest();
        DeviceInfoReport report = (DeviceInfoReport) mockedCollector.doCollect(request);

        Assert.assertEquals(report.getHardwareInfo().getManufacturer(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getHardwareInfo().getProductName(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getHardwareInfo().getSystemSerialNumber(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getHardwareInfo().getChassisSerialNumber(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getHardwareInfo().getBaseboardSerialNumber(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getHardwareInfo().getVersion(),
                DeviceInfoCollector.NOT_SPECIFIED);

    }

    /**
     * Tests that empty hardware info is set to "Not specified".
     *
     * @throws CollectorException should not be thrown here as all collection is mocked
     */
    @Test
    public final void testCollectEmptyHardwareInfo() throws CollectorException {
        DeviceInfoCollector mockedCollector = spy(DeviceInfoCollector.class);
        PowerMockito.mockStatic(DeviceInfoCollector.class);

        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-manufacturer")).thenReturn("");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-product-name")).thenReturn("");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-serial-number")).thenReturn("");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "chassis-serial-number")).thenReturn("");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "baseboard-serial-number")).thenReturn("");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-version")).thenReturn("");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-vendor")).thenReturn("Bios vendor");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-version")).thenReturn("Bios version");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-release-date")).thenReturn("Bios release date");

        doReturn("Linux").when(mockedCollector).getSystemProperty("os.name");

        final DeviceInfoReportRequest request = new DeviceInfoReportRequest();
        DeviceInfoReport report = (DeviceInfoReport) mockedCollector.doCollect(request);

        Assert.assertEquals(report.getHardwareInfo().getManufacturer(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getHardwareInfo().getProductName(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getHardwareInfo().getSystemSerialNumber(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getHardwareInfo().getChassisSerialNumber(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getHardwareInfo().getBaseboardSerialNumber(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getHardwareInfo().getVersion(),
                DeviceInfoCollector.NOT_SPECIFIED);

    }

    /**
     * Tests that null firmware info is set to "Not specified".
     *
     * @throws CollectorException should not be thrown here as all collection is mocked
     */
    @Test
    public final void testCollectNullFirmwareInfo() throws CollectorException {
        DeviceInfoCollector mockedCollector = spy(DeviceInfoCollector.class);
        PowerMockito.mockStatic(DeviceInfoCollector.class);

        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-manufacturer")).thenReturn("Manufacturer");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-product-name")).thenReturn("Product name");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-serial-number")).thenReturn("Serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "chassis-serial-number")).thenReturn("Chassis serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "baseboard-serial-number")).thenReturn("Baseboard serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-version")).thenReturn("Version");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-vendor")).thenReturn(null);
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-version")).thenReturn(null);
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-release-date")).thenReturn(null);

        doReturn("Linux").when(mockedCollector).getSystemProperty("os.name");

        final DeviceInfoReportRequest request = new DeviceInfoReportRequest();
        DeviceInfoReport report = (DeviceInfoReport) mockedCollector.doCollect(request);

        Assert.assertEquals(report.getFirmwareInfo().getBiosVendor(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getFirmwareInfo().getBiosVersion(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getFirmwareInfo().getBiosReleaseDate(),
                DeviceInfoCollector.NOT_SPECIFIED);

    }

    /**
     * Tests that empty firmware info is set to "Not specified".
     *
     * @throws CollectorException should not be thrown here as all collection is mocked
     */
    @Test
    public final void testCollectEmptyFirmwareInfo() throws CollectorException {
        DeviceInfoCollector mockedCollector = spy(DeviceInfoCollector.class);
        PowerMockito.mockStatic(DeviceInfoCollector.class);

        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-manufacturer")).thenReturn("Manufacturer");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-product-name")).thenReturn("Product name");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-serial-number")).thenReturn("Serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "chassis-serial-number")).thenReturn("Chassis serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "baseboard-serial-number")).thenReturn("Baseboard serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-version")).thenReturn("Version");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-vendor")).thenReturn("");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-version")).thenReturn("");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-release-date")).thenReturn("");

        doReturn("Linux").when(mockedCollector).getSystemProperty("os.name");

        final DeviceInfoReportRequest request = new DeviceInfoReportRequest();
        DeviceInfoReport report = (DeviceInfoReport) mockedCollector.doCollect(request);

        Assert.assertEquals(report.getFirmwareInfo().getBiosVendor(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getFirmwareInfo().getBiosVersion(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getFirmwareInfo().getBiosReleaseDate(),
                DeviceInfoCollector.NOT_SPECIFIED);

    }

    /**
     * Tests that null OS info is set to "Not specified".
     *
     * @throws CollectorException should not be thrown here as all collection is mocked
     */
    @Test
    public final void testCollectNullOSInfo() throws CollectorException {
        DeviceInfoCollector mockedCollector = spy(DeviceInfoCollector.class);
        PowerMockito.mockStatic(DeviceInfoCollector.class);

        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-manufacturer")).thenReturn("Manufacturer");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-product-name")).thenReturn("Product name");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-serial-number")).thenReturn("Serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "chassis-serial-number")).thenReturn("Chassis serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "baseboard-serial-number")).thenReturn("Baseboard serial number");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "system-version")).thenReturn("Version");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-vendor")).thenReturn("Bios vendor");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-version")).thenReturn("Bios version");
        when(DeviceInfoCollector.collectDmiDecodeValue(
                OSName.LINUX, "bios-release-date")).thenReturn("Bios release date");

        doReturn("Unknown").when(mockedCollector).getSystemProperty("os.name");

        final DeviceInfoReportRequest request = new DeviceInfoReportRequest();
        DeviceInfoReport report = (DeviceInfoReport) mockedCollector.doCollect(request);

        Assert.assertEquals(report.getOSInfo().getDistribution(),
                DeviceInfoCollector.NOT_SPECIFIED);
        Assert.assertEquals(report.getOSInfo().getDistributionRelease(),
                DeviceInfoCollector.NOT_SPECIFIED);

    }

    /**
     * Tests that isReportRequestSupported() will return DeviceInfoReportRequest.
     */
    @Test
    public final void testReportRequestSupported() {
        Assert.assertEquals(collector.reportRequestTypeSupported(), DeviceInfoReportRequest.class);
    }

}
