package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

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

        final int thirdPosition = 3;
        assertEquals(TPMSecurityAssertions.EkGenerationType.values()[thirdPosition],
                TPMSecurityAssertions.EkGenerationType.INJECTED_REVOCABLE);
        try {
            final int positionOutOfBounds = 4;
            assertNull(TPMSecurityAssertions.EkGenerationType.values()[positionOutOfBounds]);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
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
            final int positionOutOfBounds = 3;
            assertNull(TPMSecurityAssertions.EkGenerationLocation.values()[positionOutOfBounds]);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }
}
