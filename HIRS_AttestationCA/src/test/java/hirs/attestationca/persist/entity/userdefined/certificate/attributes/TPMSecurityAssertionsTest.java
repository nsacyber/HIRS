package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

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
        assertEquals(TPMSecurityAssertions.EkGenerationType.values()[0],
                TPMSecurityAssertions.EkGenerationType.INTERNAL);
        assertEquals(TPMSecurityAssertions.EkGenerationType.values()[1],
                TPMSecurityAssertions.EkGenerationType.INJECTED);
        assertEquals(TPMSecurityAssertions.EkGenerationType.values()[2],
                TPMSecurityAssertions.EkGenerationType.INTERNAL_REVOCABLE);
        assertEquals(TPMSecurityAssertions.EkGenerationType.values()[3],
                TPMSecurityAssertions.EkGenerationType.INJECTED_REVOCABLE);
        try {
            assertNull(TPMSecurityAssertions.EkGenerationType.values()[4]);
            fail();
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
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.values()[0],
                TPMSecurityAssertions.EkGenerationLocation.TPM_MANUFACTURER);
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.values()[1],
                TPMSecurityAssertions.EkGenerationLocation.PLATFORM_MANUFACTURER);
        assertEquals(TPMSecurityAssertions.EkGenerationLocation.values()[2],
                TPMSecurityAssertions.EkGenerationLocation.EK_CERT_SIGNER);
        try {
            assertNull(TPMSecurityAssertions.EkGenerationLocation.values()[3]);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        }
    }
}