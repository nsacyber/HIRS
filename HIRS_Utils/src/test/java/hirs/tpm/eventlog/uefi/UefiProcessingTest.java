package hirs.tpm.eventlog.uefi;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import com.eclipsesource.json.JsonObject;
import hirs.utils.JsonUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import hirs.utils.HexUtils;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Class for testing TCG Event Log processing of UEFI defined Data.
 */
public class UefiProcessingTest {
    // Variable files collected using an Event Parsing tool
    private static final String JSON_FILE = "/tcgeventlog/uefi/vendor-table.json";
    private static final String UEFI_VARIABLE_BOOT = "/tcgeventlog/uefi/EV_EFI_VARIABLE_BOOT.txt";
    private static final String UEFI_VARIABLE_BOOT_SECURE_BOOT
            = "/tcgeventlog/uefi/EV_EFI_VAR_SECURE_BOOT.txt";
    private static final String UEFI_VARIABLE_BOOT_DRIVER_CONFIG_KEK
            = "/tcgeventlog/uefi/EV_EFI_VARIABLE_DRIVER_CONFIG_KEK.txt";
    private static final String UEFI_GPT_EVENT = "/tcgeventlog/uefi/EV_EFI_GPT_EVENT.txt";
    private static final String UEFI_FW_BLOB = "/tcgeventlog/uefi/EFI_PLATFORM_FIRMWARE_BLOB.txt";
    private static final String UEFI_DEVICE_PATH = "/tcgeventlog/uefi/EFI_DEVICE_PATH.txt";

    private static final Logger LOGGER
            = LogManager.getLogger(UefiProcessingTest.class);

    /**
     * Initializes a <code>SessionFactory</code>.
     * The factory is used for an in-memory database that is used for testing.
     */
    @BeforeClass
    public static final void setup() {
        LOGGER.debug("retrieving session factory");
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterClass
    public static final void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * Tests the processing of UEFI Variables.
     *
     * @throws IOException              when processing the test fails.
     * @throws NoSuchAlgorithmException if non TCG Algorithm is encountered.
     * @throws CertificateException     if parsing issue for X509 cert is encountered.
     * @throws URISyntaxException File location exception
     */
    @Test
    public final void testUefiVariables() throws IOException,
            CertificateException, NoSuchAlgorithmException, URISyntaxException {
        LOGGER.debug("Testing the parsing of UEFI Variables");
        Path jsonPath = Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI());
        String uefiTxt = IOUtils.toString(this.getClass().getResourceAsStream(UEFI_VARIABLE_BOOT),
                "UTF-8");
        byte[] uefiVariableBytes = HexUtils.hexStringToByteArray(uefiTxt);
        UefiVariable uefiVariable = new UefiVariable(uefiVariableBytes);
        UefiGuid guid = uefiVariable.getEfiVarGuid();
        String varName = uefiVariable.getEfiVarName();
        JsonObject jsonObject = JsonUtils.getSpecificJsonObject(jsonPath, "VendorTable");
        String guidStr = jsonObject.getString(
                guid.toStringNoLookup().toLowerCase(), "Unknown GUID reference");
        Assert.assertEquals(guidStr,
                "EFI_Global_Variable");
        Assert.assertEquals(varName, "BootOrder");

        uefiTxt = IOUtils.toString(this.getClass()
                        .getResourceAsStream(UEFI_VARIABLE_BOOT_SECURE_BOOT),
                "UTF-8");
        uefiVariableBytes = HexUtils.hexStringToByteArray(uefiTxt);
        uefiVariable = new UefiVariable(uefiVariableBytes);
        guid = uefiVariable.getEfiVarGuid();
        varName = uefiVariable.getEfiVarName();
        guidStr = jsonObject.getString(
                guid.toStringNoLookup().toLowerCase(), "Unknown GUID reference");
        Assert.assertEquals(guidStr,
                "EFI_Global_Variable");
        Assert.assertEquals(varName, "SecureBoot");

        uefiTxt = IOUtils.toString(this.getClass().getResourceAsStream(
                UEFI_VARIABLE_BOOT_DRIVER_CONFIG_KEK), "UTF-8");
        uefiVariableBytes = HexUtils.hexStringToByteArray(uefiTxt);
        uefiVariable = new UefiVariable(uefiVariableBytes);
        varName = uefiVariable.getEfiVarName();
        Assert.assertEquals(varName, "KEK");
    }

    /**
     * Tests the processing of a UEFI defined GPT Partition event.
     *
     * @throws IOException              when processing the test fails.
     * @throws NoSuchAlgorithmException if non TCG Algorithm is encountered.
     * @throws CertificateException     if parsing issue for X509 cert is encountered.
     * @throws URISyntaxException File location exception
     */
    @Test
    public final void testUefiPartiton() throws IOException,
            CertificateException, NoSuchAlgorithmException, URISyntaxException {
        LOGGER.debug("Testing the parsing of GPT Data");
        Path jsonPath = Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI());
        String uefiTxt = IOUtils.toString(this.getClass().getResourceAsStream(UEFI_GPT_EVENT),
                "UTF-8");
        byte[] uefiPartitionBytes = HexUtils.hexStringToByteArray(uefiTxt);
        UefiPartition gptPart = new UefiPartition(uefiPartitionBytes);
        String gptPartName = gptPart.getName();
        UefiGuid gptTypeuid = gptPart.getPartitionTypeGUID();
        UefiGuid gptUniqueGuid = gptPart.getUniquePartitionGUID();
        JsonObject jsonObject = JsonUtils.getSpecificJsonObject(jsonPath, "VendorTable");
        String guidStr = jsonObject.getString(
                gptTypeuid.toStringNoLookup().toLowerCase(), "Unknown GUID reference");
        Assert.assertEquals(guidStr,
                "EFI System Partition");
        Assert.assertEquals(gptUniqueGuid.toString(),
                "8ca7623c-041e-4fab-8c12-f49a86b85d73 : Unknown GUID reference");
        Assert.assertEquals(gptPartName, "EFI system partition");
    }

    /**
     * Tests the processing of a UEFI defined GPT Partition event.
     *
     * @throws IOException              when processing the test fails.
     * @throws NoSuchAlgorithmException if non TCG Algorithm is encountered.
     * @throws CertificateException     if parsing issue for X509 cert is encountered.
     */
    @Test
    public final void testUefiFirmwareBlob() throws IOException,
            CertificateException, NoSuchAlgorithmException {
        LOGGER.debug("Testing the parsing of Uefi Firmware Blob");
        String uefiTxt = IOUtils.toString(this.getClass()
                .getResourceAsStream(UEFI_FW_BLOB), "UTF-8");
        byte[] uefiFwBlobBytes = HexUtils.hexStringToByteArray(uefiTxt);
        UefiFirmware uefiFWBlob = new UefiFirmware(uefiFwBlobBytes);
        int fwAddress = uefiFWBlob.getPhysicalAddress();
        int fwLength = uefiFWBlob.getBlobLength();
        Assert.assertEquals(fwAddress, 1797287936);
        Assert.assertEquals(fwLength, 851968);
    }

    /**
     * Tests the processing of a UEFI defined Device Path.
     *
     * @throws IOException when processing the test fails.
     * @throws URISyntaxException File location exception
     */
    @Test
    public final void testUefiDevicePath() throws IOException, URISyntaxException {
        LOGGER.debug("Testing the parsing of Uefi Device Path");
        String uefiTxt = IOUtils.toString(this.getClass().getResourceAsStream(UEFI_DEVICE_PATH),
                "UTF-8");
        byte[] uefiFwBlobBytes = HexUtils.hexStringToByteArray(uefiTxt);
        UefiDevicePath uefiDevPath = new UefiDevicePath(uefiFwBlobBytes);
        String devPathType = uefiDevPath.getType();
        Assert.assertEquals(devPathType, "Media Device Path");
    }
}
