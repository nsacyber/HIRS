package hirs.tpm.tss.command;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@see CommandTpm}.
 */
public class CommandTpmTest {

    /**
     * Tests retrieving the size given a valid output from tpm_nvinfo.
     */
    @Test
    public void parseSizeValid() {
        final int expectedSize = 1129;

        String nvInfoOutput =
                "\n"
                + "NVRAM index   : 0x1000f000 (268496896)\n"
                + "PCR read  selection:\n"
                + " Localities   : ALL\n"
                + "PCR write selection:\n"
                + " Localities   : ALL\n"
                + "Permissions   : 0x00020002 (OWNERREAD|OWNERWRITE)\n"
                + "bReadSTClear  : FALSE\n"
                + "bWriteSTClear : FALSE\n"
                + "bWriteDefine  : FALSE\n"
                + "Size          : 1129 (0x469)\n"
                + "\n";


        int readSize = CommandTpm.parseNvramSizeFromNvInfoOutput(nvInfoOutput);

        Assert.assertEquals(readSize, expectedSize);
    }

    /**
     * Tests failure to get the size given output from tpm_nvinfo with missing size value.
     */
    @Test(expectedExceptions = RuntimeException.class)
    public void parseSizeMissingSizeValue() {
        String nvInfoOutput =
                "\n"
                        + "NVRAM index   : 0x1000f000 (268496896)\n"
                        + "PCR read  selection:\n"
                        + " Localities   : ALL\n"
                        + "PCR write selection:\n"
                        + " Localities   : ALL\n"
                        + "Permissions   : 0x00020002 (OWNERREAD|OWNERWRITE)\n"
                        + "bReadSTClear  : FALSE\n"
                        + "bWriteSTClear : FALSE\n"
                        + "bWriteDefine  : FALSE\n"
                        + "Strength      : 1129 (0x469)\n"
                        + "\n";

        CommandTpm.parseNvramSizeFromNvInfoOutput(nvInfoOutput);
    }

    /**
     * Tests failure to get the EC size given a completely garbage tpm_nvifo output.
     */
    @Test(expectedExceptions = RuntimeException.class)
    public void parseSizeBadOutput() {
        String nvInfoOutput = "bad data here. FAILURE TO READ SOMETHING!";
        CommandTpm.parseNvramSizeFromNvInfoOutput(nvInfoOutput);
    }
}
