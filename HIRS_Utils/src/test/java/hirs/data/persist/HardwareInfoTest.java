package hirs.data.persist;

import static hirs.data.persist.DeviceInfoReport.NOT_SPECIFIED;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * HardwareInfoTest is a unit test class for HardwareInfo.
 */
public class HardwareInfoTest {

    private static final String MANUFACTURER = "test manufacturer";
    private static final String PRODUCT_NAME = "test product name";
    private static final String VERSION = "test version";
    private static final String SERIAL_NUMBER = "test serial number";
    private static final String CHASSIS_SERIAL_NUMBER = "test chassis serial number";
    private static final String BASEBOARD_SERIAL_NUMBER = "test baseboard serial number";

    private static final String LONG_MANUFACTURER = StringUtils.rightPad(
            "test manufacturer",
            257
    );
    private static final String LONG_PRODUCT_NAME = StringUtils.rightPad(
            "test product name",
            257
    );
    private static final String LONG_VERSION = StringUtils.rightPad(
            "test version",
            65
    );
    private static final String LONG_SERIAL_NUMBER = StringUtils.rightPad(
            "test serial number",
            257
    );
    private static final String LONG_CHASSIS_SERIAL_NUMBER = StringUtils.rightPad(
            "test chassis serial number",
            257
    );
    private static final String LONG_BASEBOARD_SERIAL_NUMBER = StringUtils.rightPad(
            "test baseboard serial number",
            257
    );

    /**
     * Tests instantiation of a HardwareInfo object.
     */
    @Test
    public final void hardwareInfo() {
        new HardwareInfo(
                MANUFACTURER,
                PRODUCT_NAME,
                VERSION,
                SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        );
    }

    /**
     * Tests that the no-parameter constructor for HardwareInfo contains expected values.
     */
    @Test
    public final void hardwareInfoNoParams() {
        HardwareInfo hardwareInfo = new HardwareInfo();
        Assert.assertEquals(hardwareInfo.getManufacturer(), NOT_SPECIFIED);
        Assert.assertEquals(hardwareInfo.getProductName(), NOT_SPECIFIED);
        Assert.assertEquals(hardwareInfo.getSystemSerialNumber(), NOT_SPECIFIED);
        Assert.assertEquals(hardwareInfo.getVersion(), NOT_SPECIFIED);
        Assert.assertEquals(hardwareInfo.getBaseboardSerialNumber(), NOT_SPECIFIED);
    }

    /**
     * Tests that the getters for HardwareInfo return the expected values.
     */
    @Test
    public final void hardwareInfoGetters() {
        HardwareInfo hardwareInfo =
                new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                        CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        Assert.assertEquals(hardwareInfo.getManufacturer(), MANUFACTURER);
        Assert.assertEquals(hardwareInfo.getProductName(), PRODUCT_NAME);
        Assert.assertEquals(hardwareInfo.getVersion(), VERSION);
        Assert.assertEquals(hardwareInfo.getSystemSerialNumber(), SERIAL_NUMBER);
        Assert.assertEquals(hardwareInfo.getBaseboardSerialNumber(), BASEBOARD_SERIAL_NUMBER);
    }

    /**
     * Tests that the default value is present if manufacturer is null.
     */
    @Test
    public final void manufacturerNullTest() {
        HardwareInfo hardwareInfo = new HardwareInfo(null,
                PRODUCT_NAME,
                VERSION,
                SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        );
        Assert.assertEquals(hardwareInfo.getManufacturer(), DeviceInfoReport.NOT_SPECIFIED);
    }

    /**
     * Tests that the default value is present if product name is null.
     */
    @Test
    public final void productNameNullTest() {
        HardwareInfo hardwareInfo = new HardwareInfo(MANUFACTURER,
                null,
                VERSION,
                SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        );
        Assert.assertEquals(hardwareInfo.getProductName(), DeviceInfoReport.NOT_SPECIFIED);
    }

    /**
     * Tests that the default value is present if version is null.
     */
    @Test
    public final void versionNullTest() {
        HardwareInfo hardwareInfo = new HardwareInfo(MANUFACTURER,
                PRODUCT_NAME,
                null,
                SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        );
        Assert.assertEquals(hardwareInfo.getVersion(), DeviceInfoReport.NOT_SPECIFIED);
    }

    /**
     * Tests that the default value is present if serial number is null.
     */
    @Test
    public final void serialNumberNullTest() {
        HardwareInfo hardwareInfo = new HardwareInfo(MANUFACTURER,
                PRODUCT_NAME,
                VERSION,
                null,
                CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        );
        Assert.assertEquals(hardwareInfo.getSystemSerialNumber(), DeviceInfoReport.NOT_SPECIFIED);
    }

    /**
     * Tests that the default value is present if chassis serial number is null.
     */
    @Test
    public final void chassisSerialNumberNullTest() {
        HardwareInfo hardwareInfo = new HardwareInfo(MANUFACTURER,
                PRODUCT_NAME,
                VERSION,
                SERIAL_NUMBER,
                null,
                BASEBOARD_SERIAL_NUMBER
        );
        Assert.assertEquals(hardwareInfo.getChassisSerialNumber(),
                DeviceInfoReport.NOT_SPECIFIED);
    }

    /**
     * Tests that the default value is present if baseboard serial number is null.
     */
    @Test
    public final void baseboardSerialNumberNullTest() {
        HardwareInfo hardwareInfo = new HardwareInfo(MANUFACTURER,
                PRODUCT_NAME,
                VERSION,
                SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER,
                null
        );
        Assert.assertEquals(hardwareInfo.getBaseboardSerialNumber(),
                DeviceInfoReport.NOT_SPECIFIED);
    }

    /**
     * Tests that an IllegalArgumentException is thrown if manufacturer is too long.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void manufacturerLongTest() {
        new HardwareInfo(
                LONG_MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER, CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        );
    }

    /**
     * Tests that an IllegalArgumentException is thrown if product name is too long.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void productNameLongTest() {
        new HardwareInfo(
                MANUFACTURER, LONG_PRODUCT_NAME, VERSION, SERIAL_NUMBER, CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        );
    }

    /**
     * Tests that an IllegalArgumentException is thrown if version is too long.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void versionLongTest() {
        new HardwareInfo(
                MANUFACTURER, PRODUCT_NAME, LONG_VERSION, SERIAL_NUMBER, CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        );
    }

    /**
     * Tests that an IllegalArgumentException is thrown if serial number is too long.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void serialNumberLongTest() {
        new HardwareInfo(
                MANUFACTURER, PRODUCT_NAME, VERSION, LONG_SERIAL_NUMBER, CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        );
    }

    /**
     * Tests that an IllegalArgumentException is thrown if chassis serial number is too long.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void chassisSerialNumberLongTest() {
        new HardwareInfo(
                MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER, LONG_CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        );
    }

    /**
     * Tests that an IllegalArgumentException is thrown if chassis serial number is too long.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void baseboardSerialNumberLongTest() {
        new HardwareInfo(
                MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER, LONG_CHASSIS_SERIAL_NUMBER,
                LONG_BASEBOARD_SERIAL_NUMBER
        );
    }

    /**
     * Tests that two HardwareInfo objects with the same manufacturer, product name, version, and
     * serial number create hash codes that are equal.
     */
    @Test
    public final void testEqualsAndHashCode() {
        HardwareInfo hi1 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        HardwareInfo hi2 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        Assert.assertEquals(hi1, hi2);
        Assert.assertEquals(hi1.hashCode(), hi2.hashCode());

        HardwareInfo hi3 = new HardwareInfo();
        Assert.assertNotEquals(hi1, hi3);
        Assert.assertNotEquals(hi1.hashCode(), hi3.hashCode());
    }

    /**
     * Tests that two HardwareInfo objects with different manufacturer information will generate
     * different hash codes.
     */
    @Test
    public final void testEqualsAndHashCodeManufacturer() {
        String manufacturer2 = "test manufacturer 2";
        HardwareInfo hi1 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        HardwareInfo hi2 = new HardwareInfo(manufacturer2, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        Assert.assertNotEquals(hi1, hi2);
        Assert.assertNotEquals(hi1.hashCode(), hi2.hashCode());
    }

    /**
     * Tests that two HardwareInfo objects with different product name information will
     * generate different hash codes.
     */
    @Test
    public final void testEqualsAndHashCodeProductName() {
        String productName2 = "test product name 2";
        HardwareInfo hi1 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        HardwareInfo hi2 = new HardwareInfo(MANUFACTURER, productName2, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        Assert.assertNotEquals(hi1, hi2);
        Assert.assertNotEquals(hi1.hashCode(), hi2.hashCode());
    }

    /**
     * Tests that two HardwareInfo objects with different version information will generate
     * different hash codes.
     */
    @Test
    public final void testEqualsAndHashCodeVersion() {
        String version2 = "test version 2";
        HardwareInfo hi1 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        HardwareInfo hi2 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, version2, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        Assert.assertNotEquals(hi1, hi2);
        Assert.assertNotEquals(hi1.hashCode(), hi2.hashCode());
    }

    /**
     * Tests that two HardwareInfo objects with different serial number information will generate
     * different hash codes.
     */
    @Test
    public final void testEqualsAndHashCodeSerialNumber() {
        String serialNumber2 = "test serialNumber 2";
        HardwareInfo hi1 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        HardwareInfo hi2 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, serialNumber2,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        Assert.assertNotEquals(hi1, hi2);
        Assert.assertNotEquals(hi1.hashCode(), hi2.hashCode());
    }

    /**
     * Tests that two HardwareInfo objects with different chassis serial number information will
     * generate different hash codes.
     */
    @Test
    public final void testEqualsAndHashCodeChassisSerialNumber() {
        String chassisSerialNumber2 = "test chassisSerialNumber 2";
        HardwareInfo hi1 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        HardwareInfo hi2 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                chassisSerialNumber2, BASEBOARD_SERIAL_NUMBER);
        Assert.assertNotEquals(hi1, hi2);
        Assert.assertNotEquals(hi1.hashCode(), hi2.hashCode());
    }

    /**
     * Tests that two HardwareInfo objects with different baseboard serial number information will
     * generate different hash codes.
     */
    @Test
    public final void testEqualsAndHashCodeBaseboardSerialNumber() {
        String baseboardSerialNumber2 = "test baseboardSerialNumber 2";
        HardwareInfo hi1 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        HardwareInfo hi2 = new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                CHASSIS_SERIAL_NUMBER, baseboardSerialNumber2);
        Assert.assertNotEquals(hi1, hi2);
        Assert.assertNotEquals(hi1.hashCode(), hi2.hashCode());
    }
}
