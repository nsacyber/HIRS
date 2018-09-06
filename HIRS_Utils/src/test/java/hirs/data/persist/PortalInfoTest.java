package hirs.data.persist;

import java.net.InetAddress;
import org.testng.Assert;
import org.testng.annotations.Test;

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
        Assert.assertNull(info.getSchemeName());
        Assert.assertNull(info.getIpAddress());
        Assert.assertEquals(info.getPort(), 0);
    }

    /**
     * Test that the scheme can be set and retrieved.
     */
    @Test
    public void testScheme() {
        final PortalInfo.Scheme scheme = PortalInfo.Scheme.HTTPS;

        PortalInfo info = new PortalInfo();
        info.setSchemeName(scheme);

        Assert.assertEquals(info.getSchemeName(), scheme.name());
    }

    /**
     * Test that setSchemeName does not accept a null input.
     */
    @Test
    public void testSchemeNull() {
        final PortalInfo.Scheme scheme = null;

        PortalInfo info = new PortalInfo();

        try {
            info.setSchemeName(scheme);
            Assert.fail("The null scheme should have caused an error.");
        } catch (NullPointerException e) {
            Assert.assertNull(info.getSchemeName());
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

        Assert.assertEquals(info.getIpAddress(), address);
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

        Assert.assertEquals(info.getIpAddress().getHostName(), address);
    }

    /**
     * Test that the scheme can be set and retrieved.
     */
    @Test
    public void testPort() {
        final int port = 127;

        PortalInfo info = new PortalInfo();
        info.setPort(port);

        Assert.assertEquals(info.getPort(), port);
    }

    /**
     * Test that the context name can be set and retrieved.
     */
    @Test
    public void testContext() {
        final String context = "Portal";

        PortalInfo info = new PortalInfo();
        info.setContextName(context);

        Assert.assertEquals(info.getContextName(), context);
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
            Assert.fail("The null context should have caused an error.");
        } catch (NullPointerException e) {
            Assert.assertNull(info.getContextName());
        }
    }
}
