package hirs.repository.spacewalk;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import hirs.repository.TestRepository;

/**
 * Unit tests for <code>SpacewalkPackage</code>.
 */
public class SpacewalkPackageTest {

    private static final int PACKAGE_ID = 777;
    private static final String PACKAGE_NAME = "test-chan";
    private static final String PACKAGE_VERSION = "1.2.3";
    private static final String PACKAGE_RELEASE = "test-rel1";
    private static final String PACAKAGE_ARCH = "x128";
    private Map<String, Object> validPackageMap = null;

    /**
     * Sets up test resources.
     *
     * @throws MalformedURLException
     *             if the URL is malformed
     */
    @BeforeClass
    public final void setup() throws MalformedURLException {
        validPackageMap = new HashMap<>();

        validPackageMap.put("id", PACKAGE_ID);
        validPackageMap.put("name", PACKAGE_NAME);
        validPackageMap.put("version", PACKAGE_VERSION);
        validPackageMap.put("release", PACKAGE_RELEASE);
        validPackageMap.put("arch_label", PACAKAGE_ARCH);
    }

    /**
     * Verifies exception when no package map is provided.
     */
    @Test(
          expectedExceptions = NullPointerException.class,
          expectedExceptionsMessageRegExp = ".*packageMap.*null*")
    public void exceptionWithNullMap() {
        SpacewalkPackage.buildSpacewalkPackage(null, new TestRepository("zzz"));
    }

    /**
     * Verifies exception when no repository is provided.
     */
    @Test(
          expectedExceptions = NullPointerException.class,
          expectedExceptionsMessageRegExp = ".*sourceRepository.*null*")
    public void exceptionWithNullRepository() {
        SpacewalkPackage.buildSpacewalkPackage(validPackageMap, null);
    }

    /**
     * Tests that the SpacewalkPackagel throws an exception when the provided map is missing an
     * expected key/value pair.
     */
    @Test(
          expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = ".*version*")
    public void parseInvalidPackageMap() {
        Map<String, Object> insufficientMap = new HashMap<String, Object>(validPackageMap);
        insufficientMap.remove("version");

        SpacewalkPackage.buildSpacewalkPackage(insufficientMap, new TestRepository("zzz"));
    }

    /**
     * Tests that the SpacewalkChannel can parse a valid map.
     */
    @Test
    public void parseValidPackageMap() {
        SpacewalkPackage channel =
                SpacewalkPackage
                        .buildSpacewalkPackage(validPackageMap, new TestRepository("zzz"));

        Assert.assertEquals(channel.getSpacewalkPackageId(), PACKAGE_ID);
        Assert.assertEquals(channel.getName(), PACKAGE_NAME);
        Assert.assertEquals(channel.getVersion(), PACKAGE_VERSION);
        Assert.assertEquals(channel.getRelease(), PACKAGE_RELEASE);
        Assert.assertEquals(channel.getArchitecture(), PACAKAGE_ARCH);
    }
}
