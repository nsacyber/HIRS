package hirs.attestationca.persist.provision.helper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for TpmObjectAttributes
 */
public class TpmObjectAttributesTest {
    private static final String AK_PUBLIC_KEY_PATH_RSA = "/tpm2/ak_rsa.pub";
    private static final String AK_PUBLIC_KEY_PATH_ECC = "/tpm2/ak_ecc.pub";
    private static final String EK_PUBLIC_KEY_PATH_RSA = "/tpm2/ek_rsa.pub";
    private static final String AK_PUBLIC_KEY_PATH_ML_DSA = "/tpm2/ak_mldsa.pub";
    private static final String EK_PUBLIC_KEY_PATH_ML_KEM = "/tpm2/ek_mlkem.pub";

    @Test
    public void testAcceptsAttestationKeys() throws Exception {
        Assertions.assertEquals(Optional.empty(), TpmObjectAttributes.checkAttestationKey(parse(AK_PUBLIC_KEY_PATH_RSA)));
        Assertions.assertEquals(Optional.empty(), TpmObjectAttributes.checkAttestationKey(parse(AK_PUBLIC_KEY_PATH_ECC)));
        Assertions.assertEquals(Optional.empty(), TpmObjectAttributes.checkAttestationKey(parse(AK_PUBLIC_KEY_PATH_ML_DSA)));
    }

    @Test
    public void testRefusesEndorsementKeyAsAttestationKey() throws Exception {
        Optional<String> refusal = TpmObjectAttributes.checkAttestationKey(parse(EK_PUBLIC_KEY_PATH_RSA));

        Assertions.assertTrue(refusal.isPresent());
        Assertions.assertTrue(refusal.get().contains("sign"), refusal.get());
        Assertions.assertTrue(refusal.get().contains("decrypt"), refusal.get());
    }

    @Test
    public void testRefusesMlKemEndorsementKeyAsAttestationKey() throws Exception {
        Optional<String> refusal = TpmObjectAttributes.checkAttestationKey(parse(EK_PUBLIC_KEY_PATH_ML_KEM));

        Assertions.assertTrue(refusal.isPresent());
        Assertions.assertTrue(refusal.get().contains("sign"), refusal.get());
        Assertions.assertTrue(refusal.get().contains("decrypt"), refusal.get());
    }

    private ParsedTpmPublic parse(final String path) throws IOException, URISyntaxException {
        return TpmPublicHelper.parseTpmPublicArea(read(path));
    }

    private byte[] read(final String path) throws IOException, URISyntaxException {
        return Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource(path)).toURI()));
    }
}
