package hirs.data.persist.certificate.attributes;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the TPMSecurityAssertions class.
 */
public class TPMSecurityAssertionsTest {

    /**
     * Tests that enum integer association matches the TCG spec for the EK Generation
     * Type enum.
     */
    @Test
    public void testEkGenTypeEnum() {
        Assert.assertEquals(TPMSecurityAssertions.EkGenerationType.values()[0],
                TPMSecurityAssertions.EkGenerationType.INTERNAL);
        Assert.assertEquals(TPMSecurityAssertions.EkGenerationType.values()[1],
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        Assert.assertEquals(TPMSecurityAssertions.EkGenerationType.values()[2],
                TPMSecurityAssertions.EkGenerationType.INTERNAL_REVOCABLE);
        Assert.assertEquals(TPMSecurityAssertions.EkGenerationType.values()[3],
                TPMSecurityAssertions.EkGenerationType.INJECTED_REVOCABLE);
        try {
            Assert.assertNull(TPMSecurityAssertions.EkGenerationType.values()[4]);
            Assert.fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        }
    }

    /**
     * Tests that enum integer association matches the TCG spec for the Generation
     * Location enum.
     */
    @Test
    public void testGenLocationEnum() {
        Assert.assertEquals(TPMSecurityAssertions.EkGenerationLocation.values()[0],
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        Assert.assertEquals(TPMSecurityAssertions.EkGenerationLocation.values()[1],
                TPMSecurityAssertions.EkGenerationLocation.PLATFORM_MANUFACTURER);
        Assert.assertEquals(TPMSecurityAssertions.EkGenerationLocation.values()[2],
                TPMSecurityAssertions.EkGenerationLocation.EK_CERT_SIGNER);
        try {
            Assert.assertNull(TPMSecurityAssertions.EkGenerationLocation.values()[3]);
            Assert.fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        }
    }
}
