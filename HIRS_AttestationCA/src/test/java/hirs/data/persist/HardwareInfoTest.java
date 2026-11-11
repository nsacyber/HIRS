package hirs.data.persist;

import hirs.attestationca.persist.entity.userdefined.info.HardwareInfo;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static hirs.utils.enums.DeviceInfoEnums.NOT_SPECIFIED;

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
    private static final int PRIMARY_SIZE = 257;
    private static final int SECONDARY_SIZE = 65;

    private static final String LONG_MANUFACTURER = StringUtils.rightPad(
            MANUFACTURER,
            PRIMARY_SIZE
    );
    private static final String LONG_PRODUCT_NAME = StringUtils.rightPad(
            PRODUCT_NAME,
            PRIMARY_SIZE
    );
    private static final String LONG_VERSION = StringUtils.rightPad(
            VERSION,
            SECONDARY_SIZE
    );
    private static final String LONG_SERIAL_NUMBER = StringUtils.rightPad(
            SERIAL_NUMBER,
            PRIMARY_SIZE
    );
    private static final String LONG_CHASSIS_SERIAL_NUMBER = StringUtils.rightPad(
            CHASSIS_SERIAL_NUMBER,
            PRIMARY_SIZE
    );
    private static final String LONG_BASEBOARD_SERIAL_NUMBER = StringUtils.rightPad(
            BASEBOARD_SERIAL_NUMBER,
            PRIMARY_SIZE
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
        Assertions.assertEquals(NOT_SPECIFIED, hardwareInfo.getManufacturer());
        Assertions.assertEquals(NOT_SPECIFIED, hardwareInfo.getProductName());
        Assertions.assertEquals(NOT_SPECIFIED, hardwareInfo.getSystemSerialNumber());
        Assertions.assertEquals(NOT_SPECIFIED, hardwareInfo.getVersion());
        Assertions.assertEquals(NOT_SPECIFIED, hardwareInfo.getBaseboardSerialNumber());
    }

    /**
     * Tests that the getters for HardwareInfo return the expected values.
     */
    @Test
    public final void hardwareInfoGetters() {
        HardwareInfo hardwareInfo =
                new HardwareInfo(MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER,
                        CHASSIS_SERIAL_NUMBER, BASEBOARD_SERIAL_NUMBER);
        Assertions.assertEquals(MANUFACTURER, hardwareInfo.getManufacturer());
        Assertions.assertEquals(PRODUCT_NAME, hardwareInfo.getProductName());
        Assertions.assertEquals(VERSION, hardwareInfo.getVersion());
        Assertions.assertEquals(SERIAL_NUMBER, hardwareInfo.getSystemSerialNumber());
        Assertions.assertEquals(BASEBOARD_SERIAL_NUMBER, hardwareInfo.getBaseboardSerialNumber());
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
        Assertions.assertEquals(NOT_SPECIFIED, hardwareInfo.getManufacturer());
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
        Assertions.assertEquals(hardwareInfo.getProductName(), NOT_SPECIFIED);
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
        Assertions.assertEquals(hardwareInfo.getVersion(), NOT_SPECIFIED);
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
        Assertions.assertEquals(hardwareInfo.getSystemSerialNumber(), NOT_SPECIFIED);
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
        Assertions.assertEquals(NOT_SPECIFIED, hardwareInfo.getChassisSerialNumber());
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
        Assertions.assertEquals(NOT_SPECIFIED, hardwareInfo.getBaseboardSerialNumber());
    }

    /**
     * Tests that an IllegalArgumentException is thrown if manufacturer is too long.
     */
    @Test
    public final void manufacturerLongTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HardwareInfo(
                LONG_MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER, CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        ));
    }

    /**
     * Tests that an IllegalArgumentException is thrown if product name is too long.
     */
    @Test
    public final void productNameLongTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HardwareInfo(
                MANUFACTURER, LONG_PRODUCT_NAME, VERSION, SERIAL_NUMBER, CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        ));
    }

    /**
     * Tests that an IllegalArgumentException is thrown if version is too long.
     */
    @Test
    public final void versionLongTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HardwareInfo(
                MANUFACTURER, PRODUCT_NAME, LONG_VERSION, SERIAL_NUMBER, CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        ));
    }

    /**
     * Tests that an IllegalArgumentException is thrown if serial number is too long.
     */
    @Test
    public final void serialNumberLongTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HardwareInfo(
                MANUFACTURER, PRODUCT_NAME, VERSION, LONG_SERIAL_NUMBER, CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        ));
    }

    /**
     * Tests that an IllegalArgumentException is thrown if chassis serial number is too long.
     */
    @Test
    public final void chassisSerialNumberLongTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HardwareInfo(
                MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER, LONG_CHASSIS_SERIAL_NUMBER,
                BASEBOARD_SERIAL_NUMBER
        ));
    }

    /**
     * Tests that an IllegalArgumentException is thrown if chassis serial number is too long.
     */
    @Test
    public final void baseboardSerialNumberLongTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HardwareInfo(
                MANUFACTURER, PRODUCT_NAME, VERSION, SERIAL_NUMBER, LONG_CHASSIS_SERIAL_NUMBER,
                LONG_BASEBOARD_SERIAL_NUMBER
        ));
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
        Assertions.assertEquals(hi2, hi1);
        Assertions.assertEquals(hi2.hashCode(), hi1.hashCode());

        HardwareInfo hi3 = new HardwareInfo();
        Assertions.assertNotEquals(hi3, hi1);
        Assertions.assertNotEquals(hi3.hashCode(), hi1.hashCode());
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
        Assertions.assertNotEquals(hi2, hi1);
        Assertions.assertNotEquals(hi2.hashCode(), hi1.hashCode());
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
        Assertions.assertNotEquals(hi2, hi1);
        Assertions.assertNotEquals(hi2.hashCode(), hi1.hashCode());
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
        Assertions.assertNotEquals(hi2, hi1);
        Assertions.assertNotEquals(hi2.hashCode(), hi1.hashCode());
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
        Assertions.assertNotEquals(hi2, hi1);
        Assertions.assertNotEquals(hi2.hashCode(), hi1.hashCode());
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
        Assertions.assertNotEquals(hi2, hi1);
        Assertions.assertNotEquals(hi2.hashCode(), hi1.hashCode());
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
        Assertions.assertNotEquals(hi2, hi1);
        Assertions.assertNotEquals(hi2.hashCode(), hi1.hashCode());
    }
}
