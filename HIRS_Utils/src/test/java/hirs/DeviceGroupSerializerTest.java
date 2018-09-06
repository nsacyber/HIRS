package hirs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hirs.data.persist.AppraisalStatus;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceGroup;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.FirmwareInfo;
import hirs.data.persist.HardwareInfo;
import hirs.data.persist.NetworkInfo;
import hirs.data.persist.OSInfo;
import hirs.data.persist.TPMInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unit tests for the {@link hirs.DeviceGroupSerializer}.
 */
public class DeviceGroupSerializerTest {

    private static final int COUNT_OF_ID_FIELD_OUTSIDE_DEVICE_GROUP = 2;
    private static final int COUNT_OF_CREATION_DATE_FIELD_OUTSIDE_DEVICE_GROUP = 1;
    private static final int COUNT_OF_NAME_FIELD_OUTSIDE_DEVICE_GROUP = 45;
    private static final int COUNT_OF_HEALTH_STATUS_FIELD_OUTSIDE_DEVICE_GROUP = 1;

    /**
     * Tests that Jackson correctly serializes the inner {@link DeviceGroup} on a single Device
     * using the custom {@link hirs.DeviceGroupSerializer}.
     */
    @Test
    public void serializeDeviceGroupOnDevice() {
        Device testDevice = getTestDevice("Device1");
        assertDeviceSerializedCorrectly(testDevice);
    }

    /**
     * Tests that Jackson correctly serializes the same inner {@link DeviceGroup} on multiple
     * Devices using the custom {@link hirs.DeviceGroupSerializer}.
     */
    @Test
    public void serializeSameDeviceGroupOnDevices() {
        List<Device> testDevices = new ArrayList<>();
        Device testDevice1 = getTestDevice("Device1");
        Device testDevice2 = getTestDevice("Device2");
        Assert.assertTrue(testDevice1.getDeviceGroup().equals(testDevice2.getDeviceGroup()));
        testDevices.add(testDevice1);
        testDevices.add(testDevice2);
        assertDevicesSerializedCorrectly(testDevices);
    }

    /**
     * Tests that Jackson correctly serializes different inner {@link DeviceGroup DeviceGroups} on
     * multiple Devices using the custom {@link hirs.DeviceGroupSerializer}.
     */
    @Test
    public void serializeDifferentDeviceGroupOnDevices() {
        List<Device> testDevices = new ArrayList<>();
        Device testDevice1 = getTestDevice("Device1");
        Device testDevice2 = getTestDevice("Device2");
        testDevice2.setDeviceGroup(new DeviceGroup("Non-Default Device Group"));
        Assert.assertTrue(!testDevice1.getDeviceGroup().equals(testDevice2.getDeviceGroup()));
        testDevices.add(testDevice1);
        testDevices.add(testDevice2);
        assertDevicesSerializedCorrectly(testDevices);
    }

    private DeviceInfoReport getTestDeviceInfoReport() throws UnknownHostException {
        NetworkInfo testNetworkInfo = new NetworkInfo("TestHostname",
                InetAddress.getLocalHost(), "FFFFFF".getBytes(StandardCharsets.UTF_8));
        OSInfo osInfo = new OSInfo();
        FirmwareInfo firmwareInfo = new FirmwareInfo();
        HardwareInfo hardwareInfo = new HardwareInfo();
        TPMInfo tpmInfo = new TPMInfo();
        return new DeviceInfoReport(testNetworkInfo, osInfo, firmwareInfo, hardwareInfo, tpmInfo);
    }

    private Device getTestDevice(final String testDeviceName) {
        Device testDevice = new Device(testDeviceName);
        testDevice.setSupplyChainStatus(AppraisalStatus.Status.PASS);
        testDevice.setLastReportTimestamp(new Timestamp(System.currentTimeMillis()));
        try {
            testDevice.setDeviceInfo(getTestDeviceInfoReport());
        } catch (UnknownHostException uhe) {
            Assert.fail("Failed to Create Test DeviceInfoReport");
        }
        testDevice.setDeviceGroup(new DeviceGroup("Default Device Group"));
        return testDevice;
    }

    private int countExactSubstringOccurrences(final String testString, final String substring) {
        Pattern p = Pattern.compile("\\b" + substring + "\\b");
        Matcher m = p.matcher(testString);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }

    private void assertDeviceSerializedCorrectly(final Device testDevice) {
        List<Device> testDevices = new ArrayList<>();
        testDevices.add(testDevice);
        assertDevicesSerializedCorrectly(testDevices);
    }

    private void assertDevicesSerializedCorrectly(final List<Device> testDevices) {
        String serializedDevices = null;
        try {
            serializedDevices = new ObjectMapper().writeValueAsString(testDevices);
        } catch (JsonProcessingException jpe) {
            Assert.fail("Failed to successfully serialize the Test Devices");
        }
        int numSerializedDeviceGroups = testDevices.size();
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "id") - (numSerializedDeviceGroups * COUNT_OF_ID_FIELD_OUTSIDE_DEVICE_GROUP)
                == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "createTime")
                - (numSerializedDeviceGroups * COUNT_OF_CREATION_DATE_FIELD_OUTSIDE_DEVICE_GROUP)
                == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "archivedTime") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "archivedDescription") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "name")
                - (numSerializedDeviceGroups * COUNT_OF_NAME_FIELD_OUTSIDE_DEVICE_GROUP)
                == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "description") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "periodicReportDelayThreshold") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "enablePeriodicReportDelayAlert") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "onDemandReportDelayThreshold") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "enableOnDemandReportDelayAlert") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "waitForAppraisalCompletionEnabled") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "scheduledJobInfo") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "numberOfDevices") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "numberOfTrustedDevices") == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "healthStatus")
                - (numSerializedDeviceGroups * COUNT_OF_HEALTH_STATUS_FIELD_OUTSIDE_DEVICE_GROUP)
                == numSerializedDeviceGroups);
        Assert.assertTrue(countExactSubstringOccurrences(serializedDevices,
                "archived") == numSerializedDeviceGroups);
        Assert.assertTrue(!serializedDevices.contains("devices"));
    }

}
