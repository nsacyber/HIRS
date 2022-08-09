package hirs.data.service;

import hirs.data.persist.Device;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.info.FirmwareInfo;
import hirs.data.persist.info.HardwareInfo;
import hirs.data.persist.info.NetworkInfo;
import hirs.data.persist.info.OSInfo;
import hirs.data.persist.info.TPMInfo;
import hirs.persist.DeviceManager;

import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@see DeviceRegisterImpl}.
 */
public class DeviceRegisterImplTest {

    private static final String HOSTNAME = "test-host";
    private static final byte[] MAC_ADDRESS = new byte[] {11, 22, 33, 44, 55, 66};

    /**
     * Registers a device that is not stored yet by report.
     */
    @Test
    public void registerNonStoredDeviceByReport() {
        final DeviceManager deviceManager = mock(DeviceManager.class);
        final InetAddress ipAddress = getTestIpAddress();
        final NetworkInfo networkInfo = new NetworkInfo(HOSTNAME, ipAddress, MAC_ADDRESS);

        final DeviceInfoReport report = new DeviceInfoReport(networkInfo, new OSInfo(),
                new FirmwareInfo(), new HardwareInfo(), new TPMInfo());

        verify(deviceManager).saveDevice(any(Device.class));
    }

    /**
     * Registers a device that is not stored yet by name.
     */
    @Test
    public void registerNonStoredDeviceByName() {
        final DeviceManager deviceManager = mock(DeviceManager.class);

        verify(deviceManager).saveDevice(any(Device.class));
    }

    /**
     * Registers an already-stored device by report.
     */
    @Test
    public void registerExistingDeviceByReport() {
        final DeviceManager deviceManager = mock(DeviceManager.class);
        final InetAddress ipAddress = getTestIpAddress();
        final NetworkInfo networkInfo = new NetworkInfo(HOSTNAME, ipAddress, MAC_ADDRESS);
        final DeviceInfoReport report = new DeviceInfoReport(networkInfo, new OSInfo(),
                new FirmwareInfo(), new HardwareInfo(), new TPMInfo());
        final Device device = new Device(HOSTNAME);

        when(deviceManager.getDevice(HOSTNAME)).thenReturn(device);
        verify(deviceManager).updateDevice(any(Device.class));
    }

    /**
     * Registers an already-stored device by name.
     */
    @Test
    public void registerExistingDeviceByName() {
        final DeviceManager deviceManager = mock(DeviceManager.class);
        final Device device = new Device(HOSTNAME);
        when(deviceManager.getDevice(HOSTNAME)).thenReturn(device);

        DeviceRegisterImpl register = new DeviceRegisterImpl(deviceManager);
        register.saveOrUpdateDevice(HOSTNAME);

        verify(deviceManager).updateDevice(any(Device.class));
    }

    private static InetAddress getTestIpAddress() {
        try {
            return InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
