package hirs.tpm.eventlog.uefi;

import com.eclipsesource.json.JsonObject;
import hirs.utils.HexUtils;
import hirs.utils.JsonUtils;
import hirs.utils.tpm.eventlog.uefi.UefiDevicePath;
import hirs.utils.tpm.eventlog.uefi.UefiFirmware;
import hirs.utils.tpm.eventlog.uefi.UefiGuid;
import hirs.utils.tpm.eventlog.uefi.UefiPartition;
import hirs.utils.tpm.eventlog.uefi.UefiVariable;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

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
    @BeforeAll
    public static final void setup() {
        LOGGER.debug("retrieving session factory");
    }

    /**
     * Closes the <code>SessionFactory</code> from setup.
     */
    @AfterAll
    public static final void tearDown() {
        LOGGER.debug("closing session factory");
    }

    /**
     * Tests the processing of UEFI Variables.
     *
     * @throws IOException              when processing the test fails.
     * @throws NoSuchAlgorithmException if non TCG Algorithm is encountered.
     * @throws CertificateException     if parsing issue for X509 cert is encountered.
     * @throws URISyntaxException       File location exception
     */
    @Test
    public final void testUefiVariables() throws IOException,
            CertificateException, NoSuchAlgorithmException, URISyntaxException {
        LOGGER.debug("Testing the parsing of UEFI Variables");
        Path jsonPath = Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI());
        String uefiTxt = IOUtils.toString(this.getClass().getResourceAsStream(UEFI_VARIABLE_BOOT),
                StandardCharsets.UTF_8);
        byte[] uefiVariableBytes = HexUtils.hexStringToByteArray(uefiTxt);
        UefiVariable uefiVariable = new UefiVariable(uefiVariableBytes);
        UefiGuid guid = uefiVariable.getUefiVarGuid();
        String varName = uefiVariable.getEfiVarName();
        JsonObject jsonObject = JsonUtils.getSpecificJsonObject(jsonPath, "VendorTable");
        String guidStr = jsonObject.getString(
                guid.toStringNoLookup().toLowerCase(), "Unknown GUID reference");
        Assertions.assertEquals("EFI_Global_Variable", guidStr);
        Assertions.assertEquals("BootOrder", varName);

        uefiTxt = IOUtils.toString(this.getClass()
                        .getResourceAsStream(UEFI_VARIABLE_BOOT_SECURE_BOOT),
                StandardCharsets.UTF_8);
        uefiVariableBytes = HexUtils.hexStringToByteArray(uefiTxt);
        uefiVariable = new UefiVariable(uefiVariableBytes);
        guid = uefiVariable.getUefiVarGuid();
        varName = uefiVariable.getEfiVarName();
        guidStr = jsonObject.getString(
                guid.toStringNoLookup().toLowerCase(), "Unknown GUID reference");
        Assertions.assertEquals("EFI_Global_Variable", guidStr);
        Assertions.assertEquals("SecureBoot", varName);

        uefiTxt = IOUtils.toString(this.getClass().getResourceAsStream(
                UEFI_VARIABLE_BOOT_DRIVER_CONFIG_KEK), StandardCharsets.UTF_8);
        uefiVariableBytes = HexUtils.hexStringToByteArray(uefiTxt);
        uefiVariable = new UefiVariable(uefiVariableBytes);
        varName = uefiVariable.getEfiVarName();
        Assertions.assertEquals("KEK", varName);
    }

    /**
     * Tests the processing of a UEFI defined GPT Partition event.
     *
     * @throws IOException              when processing the test fails.
     * @throws NoSuchAlgorithmException if non TCG Algorithm is encountered.
     * @throws CertificateException     if parsing issue for X509 cert is encountered.
     * @throws URISyntaxException       File location exception
     */
    @Test
    public final void testUefiPartiton() throws IOException,
            CertificateException, NoSuchAlgorithmException, URISyntaxException {
        LOGGER.debug("Testing the parsing of GPT Data");
        Path jsonPath = Paths.get(this.getClass()
                .getResource(JSON_FILE).toURI());
        String uefiTxt = IOUtils.toString(this.getClass().getResourceAsStream(UEFI_GPT_EVENT),
                StandardCharsets.UTF_8);
        byte[] uefiPartitionBytes = HexUtils.hexStringToByteArray(uefiTxt);
        UefiPartition gptPart = new UefiPartition(uefiPartitionBytes);
        String gptPartName = gptPart.getPartitionName();
        UefiGuid gptTypeuid = gptPart.getPartitionTypeGUID();
        UefiGuid gptUniqueGuid = gptPart.getUniquePartitionGUID();
        JsonObject jsonObject = JsonUtils.getSpecificJsonObject(jsonPath, "VendorTable");
        String guidStr = jsonObject.getString(
                gptTypeuid.toStringNoLookup().toLowerCase(), "Unknown GUID reference");
        Assertions.assertEquals("EFI System Partition", guidStr);
        Assertions.assertEquals("8ca7623c-041e-4fab-8c12-f49a86b85d73 : Unknown GUID reference",
                gptUniqueGuid.toString());
        Assertions.assertEquals("EFI system partition", gptPartName);
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
                .getResourceAsStream(UEFI_FW_BLOB), StandardCharsets.UTF_8);
        byte[] uefiFwBlobBytes = HexUtils.hexStringToByteArray(uefiTxt);
        UefiFirmware uefiFWBlob = new UefiFirmware(uefiFwBlobBytes);
        int fwAddress = uefiFWBlob.getPhysicalBlobAddress();
        int fwLength = uefiFWBlob.getBlobLength();

        final int expectedFwAddress = 1797287936;
        Assertions.assertEquals(expectedFwAddress, fwAddress);

        final int expectedFwLength = 851968;
        Assertions.assertEquals(expectedFwLength, fwLength);
    }

    /**
     * Tests the processing of a UEFI defined Device Path.
     *
     * @throws IOException        when processing the test fails.
     * @throws URISyntaxException File location exception
     */
    @Test
    public final void testUefiDevicePath() throws IOException, URISyntaxException {
        LOGGER.debug("Testing the parsing of Uefi Device Path");
        String uefiTxt = IOUtils.toString(this.getClass().getResourceAsStream(UEFI_DEVICE_PATH),
                StandardCharsets.UTF_8);
        byte[] uefiFwBlobBytes = HexUtils.hexStringToByteArray(uefiTxt);
        UefiDevicePath uefiDevPath = new UefiDevicePath(uefiFwBlobBytes);
        String devPathType = uefiDevPath.getType();
        Assertions.assertEquals("Media Device Path", devPathType);
    }
}
