package hirs.attestationca.persist.entity.userdefined.info;

import hirs.utils.enums.PortalScheme;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 * Provides tests for PortalInfo.
 */
public class PortalInfoTest {

    /**
     * Test the default state of the object, once constructed.
     */
    @Test
    public void testPortalInfoDefaults() {
        PortalInfo info = new PortalInfo();
        assertNull(info.getName());
        assertNull(info.getIpAddress());
        assertEquals(info.getPort(), 0);
    }

    /**
     * Test that the scheme can be set and retrieved.
     */
    @Test
    public void testScheme() {
        final PortalScheme scheme = PortalScheme.HTTPS;

        PortalInfo info = new PortalInfo();
        info.setSchemeName(scheme);

        assertEquals(info.getName(), scheme.name());
    }

    /**
     * Test that setSchemeName does not accept a null input.
     */
    @Test
    public void testSchemeNull() {
        final PortalScheme scheme = null;

        PortalInfo info = new PortalInfo();

        try {
            info.setSchemeName(scheme);
            fail("The null scheme should have caused an error.");
        } catch (Exception e) {
            assertNull(info.getName());
        }
    }

    /**
     * Test that the ip address can be set and retrieved via an InetAddress.
     * @throws Exception If there is a problem with InetAddress.
     */
    @Test
    public void testIpAddressInetAddress() throws Exception {
        final InetAddress address = InetAddress.getLocalHost();

        PortalInfo info = new PortalInfo();
        info.setIpAddress(address);

        assertEquals(info.getIpAddress(), address);
    }

    /**
     * Test that the ip address can be set and retrieved via a String.
     * @throws Exception If there is a problem with InetAddress.
     */
    @Test
    public void testIpAddressString() throws Exception {
        final String address = "localhost";

        PortalInfo info = new PortalInfo();
        info.setIpAddress(address);

        assertEquals(info.getIpAddress().getHostName(), address);
    }

    /**
     * Test that the scheme can be set and retrieved.
     */
    @Test
    public void testPort() {
        final int port = 127;

        PortalInfo info = new PortalInfo();
        info.setPort(port);

        assertEquals(info.getPort(), port);
    }

    /**
     * Test that the context name can be set and retrieved.
     */
    @Test
    public void testContext() {
        final String context = "Portal";

        PortalInfo info = new PortalInfo();
        info.setContextName(context);

        assertEquals(info.getContext(), context);
    }

    /**
     * Test that setContextName does not accept a null input.
     */
    @Test
    public void testContextNull() {
        final String context = null;

        PortalInfo info = new PortalInfo();

        try {
            info.setContextName(context);
            fail("The null context should have caused an error.");
        } catch (Exception e) {
            assertNull(info.getContext());
        }
    }
}