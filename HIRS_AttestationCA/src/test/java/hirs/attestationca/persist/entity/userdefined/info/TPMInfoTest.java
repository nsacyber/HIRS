package hirs.attestationca.persist.entity.userdefined.info;

import static hirs.utils.enums.DeviceInfoEnums.NOT_SPECIFIED;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * TPMInfoTest is a unit test class for TPMInfo.
 */
public class TPMInfoTest {

    private static final String TPM_MAKE = "test tpmMake";
    private static final String LONG_TPM_MAKE = StringUtils.rightPad("test tpmMake", 65);
    private static final String TEST_IDENTITY_CERT =
            "/tpm/sample_identity_cert.cer";
    private static final short VERSION_MAJOR = 1;
    private static final short VERSION_MINOR = 2;
    private static final short VERSION_REV_MAJOR = 3;
    private static final short VERSION_REV_MINOR = 4;
    private static final Logger LOGGER = LogManager
            .getLogger(TPMInfoTest.class);

    /**
     * Tests instantiation and getters of a TPMInfo object.
     */
    @Test
    public final void tpmInfo() {
        TPMInfo tpmInfo =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        assertEquals(tpmInfo.getTpmMake(), TPM_MAKE);
        assertEquals(tpmInfo.getTpmVersionMajor(), VERSION_MAJOR);
        assertEquals(tpmInfo.getTpmVersionMinor(), VERSION_MINOR);
        assertEquals(tpmInfo.getTpmVersionRevMajor(), VERSION_REV_MAJOR);
        assertEquals(tpmInfo.getTpmVersionRevMinor(), VERSION_REV_MINOR);
    }

    /**
     * Tests that the no-parameter constructor for TPMInfo contains expected values.
     */
    @Test
    public final void tpmInfoNoParams() {
        TPMInfo tpmInfo = new TPMInfo();
        assertEquals(tpmInfo.getTpmMake(), NOT_SPECIFIED);
        assertEquals(tpmInfo.getTpmVersionMajor(), (short) 0);
        assertEquals(tpmInfo.getTpmVersionMinor(), (short) 0);
        assertEquals(tpmInfo.getTpmVersionRevMajor(), (short) 0);
        assertEquals(tpmInfo.getTpmVersionRevMinor(), (short) 0);
        assertEquals(tpmInfo.getIdentityCertificate(), null);
    }

    /**
     * Tests that the TPM make information cannot be null.
     */
    @Test
    public final void tpmMakeNullTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new TPMInfo(null, VERSION_MAJOR, VERSION_MINOR, VERSION_REV_MAJOR,
                    VERSION_REV_MINOR, getTestIdentityCertificate()));
    }

    /**
     * Tests that the TPM make information cannot be longer than 64 characters.
     */
    @Test
    public final void tpmMakeLongTest() {
        assertThrows(IllegalArgumentException.class, () ->
                new TPMInfo(LONG_TPM_MAKE, VERSION_MAJOR, VERSION_MINOR, VERSION_REV_MAJOR,
                    VERSION_REV_MINOR, getTestIdentityCertificate()));
    }

    /**
     * Tests that the version major number info cannot be set to negative
     * values.
     */
    @Test
    public final void testTPMInfoInvalidVersionMajor() {
        assertThrows(IllegalArgumentException.class, () ->
                new TPMInfo(TPM_MAKE, (short) -1, VERSION_MINOR, VERSION_REV_MAJOR,
                    VERSION_REV_MINOR, getTestIdentityCertificate()));
    }

    /**
     * Tests that the version minor number info cannot be set to negative
     * values.
     */
    @Test
    public final void testTPMInfoInvalidVersionMinor() {
        assertThrows(IllegalArgumentException.class, () ->
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, (short) -1, VERSION_REV_MAJOR,
                    VERSION_REV_MINOR, getTestIdentityCertificate()));
    }

    /**
     * Tests that the version revision major numbers cannot be set to negative
     * values.
     */
    @Test
    public final void testTPMInfoInvalidVersionRevMajor() {
        assertThrows(IllegalArgumentException.class, () ->
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR, (short) -1,
                    VERSION_REV_MINOR, getTestIdentityCertificate()));
    }

    /**
     * Tests that the version revision minor numbers cannot be set to negative
     * values.
     */
    @Test
    public final void testTPMInfoInvalidVersionRevMinor() {
        assertThrows(IllegalArgumentException.class, () ->
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR, VERSION_REV_MAJOR,
                    (short) -1, getTestIdentityCertificate()));
    }

    /**
     * Tests that two TPMInfo objects with the same TPM make, major, minor,
     * major revision, and minor revision information have equal hash codes.
     */
    @Test
    public final void testEqualHashCode() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        assertEquals(ti1.hashCode(), ti2.hashCode());
    }

    /**
     * Tests that two TPMInfo objects with different TPM make information have
     * different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeTPMMake() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo("test tpmMake 2", VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        assertNotEquals(ti1.hashCode(), ti2.hashCode());
    }

    /**
     * Tests that two TPMInfo objects with different TPM major version number
     * information have different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeTPMVersionMajor() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo(TPM_MAKE, (short) 0, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        assertNotEquals(ti1.hashCode(), ti2.hashCode());
    }

    /**
     * Tests that two TPMInfo objects with different TPM minor version number
     * information have different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeTPMVersionMinor() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, (short) 0,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        assertNotEquals(ti1.hashCode(), ti2.hashCode());
    }

    /**
     * Tests that two TPMInfo objects with different TPM major revision version
     * number information have different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeTPMVersionRevMajor() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR, (short) 0,
                        VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        assertNotEquals(ti1.hashCode(), ti2.hashCode());
    }

    /**
     * Tests that two TPMInfo objects with different TPM minor revision version
     * number information have different hash codes.
     */
    @Test
    public final void testNotEqualHashCodeTPMVersionRevMinor() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, (short) 0,
                        getTestIdentityCertificate());
        assertNotEquals(ti1.hashCode(), ti2.hashCode());
    }

    /**
     * Tests that two TPMInfo objects with the same TPM make, major, minor,
     * major revision, and minor revision version number information are equal.
     */
    @Test
    public final void testEqual() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        assertEquals(ti1, ti2);
    }

    /**
     * Tests that two TPMInfo objects with different TPM make information are
     * not equal.
     */
    @Test
    public final void testNotEqualTPMMake() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo("test tpmMake 2", VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        assertNotEquals(ti1, ti2);
    }

    /**
     * Tests that two TPMInfo objects with different TPM major version number
     * information are not equal.
     */
    @Test
    public final void testNotEqualTPMVersionMajor() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo(TPM_MAKE, (short) 0, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        assertNotEquals(ti1, ti2);
    }

    /**
     * Tests that two TPMInfo objects with different TPM minor version number
     * information are not equal.
     */
    @Test
    public final void testNotEqualTPMVersionMinor() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, (short) 0,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        assertNotEquals(ti1, ti2);
    }

    /**
     * Tests that two TPMInfo objects with different TPM major revision version
     * number information are not equal.
     */
    @Test
    public final void testNotEqualTPMVersionRevMajor() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR, (short) 0,
                        VERSION_REV_MINOR, getTestIdentityCertificate());
        assertNotEquals(ti1, ti2);
    }

    /**
     * Tests that two TPMInfo objects with different TPM minor revision version
     * number information are not equal.
     */
    @Test
    public final void testNotEqualTPMVersionRevMinor() {
        final TPMInfo ti1 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, VERSION_REV_MINOR,
                        getTestIdentityCertificate());
        final TPMInfo ti2 =
                new TPMInfo(TPM_MAKE, VERSION_MAJOR, VERSION_MINOR,
                        VERSION_REV_MAJOR, (short) 0,
                        getTestIdentityCertificate());
        assertNotEquals(ti1, ti2);
    }

    private X509Certificate getTestIdentityCertificate() {
        X509Certificate certificateValue = null;
        InputStream istream = null;
        istream = getClass().getResourceAsStream(TEST_IDENTITY_CERT);
        try {
            if (istream == null) {
                throw new FileNotFoundException(TEST_IDENTITY_CERT);
            }
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certificateValue = (X509Certificate) cf.generateCertificate(
                    istream);

        } catch (Exception e) {
            return null;
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException e) {
                    LOGGER.error("test certificate file could not be closed");
                }
            }
        }
        return certificateValue;
    }
}
