package hirs.data.persist;

import java.util.Arrays;

import org.apache.commons.codec.digest.DigestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the <code>Digest</code> class.
 */
public class DigestTest {
    private static final int DIGEST_LENGTH_BYTES = 20;

    /**
     * Tests that constructor throws a <code>NullPointerException</code> when a
     * null <code>DigestAlgorithm</code> is passed into constructor.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void nullAlgorithm() {
        final byte[] digest = getTestDigest(16);
        new Digest(null, digest);
        Assert.fail("null digest algorithm not detected");
    }

    /**
     * Tests that constructor throws a <code>IllegalArgumentException</code> when a
     * null digest is passed into constructor.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void nullDigest() {
        new Digest(DigestAlgorithm.MD2, null);
    }

    /**
     * Tests that constructor throws a <code>IllegalArgumentException</code> when an
     * digest that is an empty array is passed into constructor.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void emptyArrayDigest() {
        new Digest(DigestAlgorithm.MD2, new byte[0]);
    }

    /**
     * Tests that MD2 digest can be created.
     */
    @Test
    public final void md2() {
        final byte[] digest = getTestDigest(16);
        final Digest d = new Digest(DigestAlgorithm.MD2, digest);
        Assert.assertNotNull(d);
    }

    /**
     * Tests that an MD2 digest can be recreated from a string.
     */
    @Test
    public final void testFromStringMD2() {
        final byte[] digestBytes = getTestDigest(16);
        Digest digest = new Digest(DigestAlgorithm.MD2, digestBytes);
        String digestString = digest.toString();
        Digest digestFromString = Digest.fromString(digestString);
        Assert.assertEquals(digestFromString, digest);
    }

    /**
     * Tests that MD2 digest cannot be created with a digest that has extra
     * bytes.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void md2IllegalDigest() {
        final byte[] digest = getTestDigest(17);
        new Digest(DigestAlgorithm.MD2, digest);
        Assert.fail("digest not validated");
    }

    /**
     * Tests that MD5 digest can be created.
     */
    @Test
    public final void md5() {
        final byte[] digest = getTestDigest(16);
        final Digest d = new Digest(DigestAlgorithm.MD5, digest);
        Assert.assertNotNull(d);
    }

    /**
     * Tests that an MD5 digest can be recreated from a string.
     */
    @Test
    public final void testFromStringMD5() {
        final byte[] digestBytes = getTestDigest(16);
        Digest digest = new Digest(DigestAlgorithm.MD5, digestBytes);
        String digestString = digest.toString();
        Digest digestFromString = Digest.fromString(digestString);
        Assert.assertEquals(digestFromString, digest);
    }

    /**
     * Tests that MD5 digest cannot be created with a digest that has extra
     * bytes.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void md5IllegalDigest() {
        final byte[] digest = getTestDigest(17);
        new Digest(DigestAlgorithm.MD5, digest);
        Assert.fail("digest not validated");
    }

    /**
     * Tests that SHA1 digest can be created.
     */
    @Test
    public final void sha1() {
        final byte[] digest = getTestDigest(20);
        final Digest d = new Digest(DigestAlgorithm.SHA1, digest);
        Assert.assertNotNull(d);
    }

    /**
     * Tests that SHA1 digest can be recreated from a string.
     */
    @Test
    public final void testFromStringSHA1() {
        final byte[] digestBytes = getTestDigest(20);
        Digest digest = new Digest(DigestAlgorithm.SHA1, digestBytes);
        String digestString = digest.toString();
        Digest digestFromString = Digest.fromString(digestString);
        Assert.assertEquals(digestFromString, digest);
    }

    /**
     * Tests that SHA1 digest cannot be created with a digest that has extra
     * bytes.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void sha1IllegalDigest() {
        final byte[] digest = getTestDigest(21);
        new Digest(DigestAlgorithm.SHA1, digest);
        Assert.fail("digest not validated");
    }

    /**
     * Tests that SHA256 digest can be created.
     */
    @Test
    public final void sha256() {
        final byte[] digest = getTestDigest(32);
        final Digest d = new Digest(DigestAlgorithm.SHA256, digest);
        Assert.assertNotNull(d);
    }

    /**
     * Tests that SHA256 digest can be recreated from a string.
     */
    @Test
    public final void testFromStringSHA256() {
        final byte[] digestBytes = getTestDigest(32);
        Digest digest = new Digest(DigestAlgorithm.SHA256, digestBytes);
        String digestString = digest.toString();
        Digest digestFromString = Digest.fromString(digestString);
        Assert.assertEquals(digestFromString, digest);
    }

    /**
     * Tests that SHA256 digest cannot be created with a digest that has extra
     * bytes.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void sha256IllegalDigest() {
        final byte[] digest = getTestDigest(33);
        new Digest(DigestAlgorithm.SHA256, digest);
        Assert.fail("digest not validated");
    }

    /**
     * Tests that SHA384 digest can be created.
     */
    @Test
    public final void sha384() {
        final byte[] digest = getTestDigest(48);
        final Digest d = new Digest(DigestAlgorithm.SHA384, digest);
        Assert.assertNotNull(d);
    }

    /**
     * Tests that SHA384 digest can be recreated from a string.
     */
    @Test
    public final void testFromStringSHA384() {
        final byte[] digestBytes = getTestDigest(48);
        Digest digest = new Digest(DigestAlgorithm.SHA384, digestBytes);
        String digestString = digest.toString();
        Digest digestFromString = Digest.fromString(digestString);
        Assert.assertEquals(digestFromString, digest);
    }

    /**
     * Tests that SHA384 digest cannot be created with a digest that has extra
     * bytes.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void sha384IllegalDigest() {
        final byte[] digest = getTestDigest(49);
        new Digest(DigestAlgorithm.SHA384, digest);
        Assert.fail("digest not validated");
    }

    /**
     * Tests that SHA512 digest can be created.
     */
    @Test
    public final void sha512() {
        final byte[] digest = getTestDigest(64);
        final Digest d = new Digest(DigestAlgorithm.SHA512, digest);
        Assert.assertNotNull(d);
    }

    /**
     * Tests that SHA512 digest can be recreated from a string.
     */
    @Test
    public final void testFromStringSHA512() {
        final byte[] digestBytes = getTestDigest(64);
        Digest digest = new Digest(DigestAlgorithm.SHA512, digestBytes);
        String digestString = digest.toString();
        Digest digestFromString = Digest.fromString(digestString);
        Assert.assertEquals(digestFromString, digest);
    }

    /**
     * Tests that SHA512 digest cannot be created with a digest that has extra
     * bytes.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void sha512IllegalDigest() {
        final byte[] digest = getTestDigest(65);
        new Digest(DigestAlgorithm.SHA512, digest);
        Assert.fail("digest not validated");
    }

    /**
     * Tests that the correct <code>DigestAlgorithm</code> is returned by
     * {@link Digest#getAlgorithm()}.
     */
    @Test
    public final void testGetAlgorithm() {
        final Digest d = new Digest(DigestAlgorithm.SHA1, getTestDigest(20));
        Assert.assertEquals(DigestAlgorithm.SHA1, d.getAlgorithm());
    }

    /**
     * Tests that the bytes of the digest are created and do not affect the
     * underlying state of the <code>Digest</code> instance.
     */
    @Test
    public final void testGetDigest() {
        final Digest d = new Digest(DigestAlgorithm.SHA1, getTestDigest(20));
        final byte[] digestBytes = d.getDigest();
        final byte[] testBytes = getTestDigest(20);
        Assert.assertTrue(Arrays.equals(testBytes, digestBytes));
        digestBytes[0] = (byte) (digestBytes[0] + 1);
        Assert.assertTrue(Arrays.equals(testBytes, d.getDigest()));
        Assert.assertFalse(Arrays.equals(digestBytes, d.getDigest()));
    }

    /**
     * Tests that two <code>Digest</code>s have equal hash code for same
     * algorithm and digest.
     */
    @Test
    public final void testHashCodeEqual() {
        final Digest d1 = new Digest(DigestAlgorithm.SHA1, getTestDigest(20));
        final Digest d2 = new Digest(DigestAlgorithm.SHA1, getTestDigest(20));
        Assert.assertEquals(d1.hashCode(), d2.hashCode());
    }

    /**
     * Tests that two <code>Digest</code>s indicate MATCH when compared.
     */
    @Test
    public final void testMatchedComparison() {
        final Digest d1 = new Digest(DigestAlgorithm.SHA1, getTestDigest(20));
        final Digest d2 = new Digest(DigestAlgorithm.SHA1, getTestDigest(20));
        Assert.assertEquals(d1.compare(d2), DigestComparisonResultType.MATCH);
        Assert.assertEquals(d2.compare(d1), DigestComparisonResultType.MATCH);
    }

    /**
     * Tests that two <code>Digest</code>s have unequal hash code for same
     * digest but different algorithm.
     */
    @Test
    public final void testHashCodeNotEqualAlgorithm() {
        final Digest d1 = new Digest(DigestAlgorithm.MD2, getTestDigest(16));
        final Digest d2 = new Digest(DigestAlgorithm.MD5, getTestDigest(16));
        Assert.assertNotEquals(d1.hashCode(), d2.hashCode());
    }

    /**
     * Tests that two <code>Digest</code>s indicate MISMATCH when compared.
     */
    @Test
    public final void testMismatchAlgorithm() {
        final Digest d1 = new Digest(DigestAlgorithm.MD2, getTestDigest(16));
        final Digest d2 = new Digest(DigestAlgorithm.MD5, getTestDigest(16));
        Assert.assertEquals(d1.compare(d2), DigestComparisonResultType.MISMATCH);
        Assert.assertEquals(d2.compare(d1), DigestComparisonResultType.MISMATCH);
    }

    /**
     * Tests that two <code>Digest</code>s have unequal hash code for same
     * algorithm but different digest.
     */
    @Test
    public final void testHashCodeNotEqualDigest() {
        final byte[] digest = getTestDigest(20);
        final Digest d1 = new Digest(DigestAlgorithm.SHA1, digest);
        digest[0] += 1;
        final Digest d2 = new Digest(DigestAlgorithm.SHA1, digest);
        Assert.assertNotEquals(d1.hashCode(), d2.hashCode());
        Assert.assertEquals(d1.compare(d2), DigestComparisonResultType.MISMATCH);
    }

    /**
     * Tests that two <code>Digest</code>s are equal for same algorithm and
     * digest.
     */
    @Test
    public final void testEqual() {
        final Digest d1 = new Digest(DigestAlgorithm.SHA1, getTestDigest(20));
        final Digest d2 = new Digest(DigestAlgorithm.SHA1, getTestDigest(20));
        Assert.assertEquals(d1, d2);
    }

    /**
     * Tests that two <code>Digest</code>s are unequal for same digest but
     * different algorithm.
     */
    @Test
    public final void testNotEqualAlgorithm() {
        final Digest d1 = new Digest(DigestAlgorithm.MD2, getTestDigest(16));
        final Digest d2 = new Digest(DigestAlgorithm.MD5, getTestDigest(16));
        Assert.assertNotEquals(d1, d2);
    }

    /**
     * Tests that two <code>Digest</code>s are unequal for same algorithm but
     * different digest.
     */
    @Test
    public final void testNotEqualDigest() {
        final byte[] digest = getTestDigest(20);
        final Digest d1 = new Digest(DigestAlgorithm.SHA1, digest);
        digest[0] += 1;
        final Digest d2 = new Digest(DigestAlgorithm.SHA1, digest);
        Assert.assertNotEquals(d1, d2);
    }

    /**
     * Tests that comparing a null Digest to a Digest indicates an UNKNOWN
     * comparison type.
     */
    @Test
    public final void testCompareToNull() {
        final Digest d1 = new Digest(DigestAlgorithm.MD2, getTestDigest(16));
        Assert.assertEquals(d1.compare(null), DigestComparisonResultType.UNKNOWN);
    }

    /**
     * Tests that comparing two Digests with hashes with values of zero gives a MATCH
     * comparison result.
     */
    @Test
    public final void testCompareToDigestWithBothZeroizedHash() {
        final Digest d1 = new Digest(DigestAlgorithm.SHA1, getZeroValueDigest(20));
        final Digest d2 = new Digest(DigestAlgorithm.SHA1, getZeroValueDigest(20));
        Assert.assertEquals(d1.compare(d2), DigestComparisonResultType.MATCH);
        Assert.assertEquals(d2.compare(d1), DigestComparisonResultType.MATCH);
    }

    /**
     * Tests that comparing two Digests, one with a hash of value zero, gives a MISMATCH
     * comparison result.
     */
    @Test
    public final void testCompareToDigestWithOneZeroizedHash() {
        final Digest d1 = new Digest(DigestAlgorithm.SHA1, getTestDigest(20));
        final Digest d2 = new Digest(DigestAlgorithm.SHA1, getZeroValueDigest(20));
        Assert.assertEquals(d1.compare(d2), DigestComparisonResultType.MISMATCH);
        Assert.assertEquals(d2.compare(d1), DigestComparisonResultType.MISMATCH);
    }

    /**
     * Tests that comparing two Digests with a hash of no data gives a MATCH
     * comparison result.
     */
    @Test
    public final void testCompareToDigestWithBothEmptyHash() {
        final Digest d1 = new Digest(DigestAlgorithm.SHA1, getEmptySHA1Digest());
        final Digest d2 = new Digest(DigestAlgorithm.SHA1, getEmptySHA1Digest());
        Assert.assertEquals(d1.compare(d2), DigestComparisonResultType.MATCH);
        Assert.assertEquals(d2.compare(d1), DigestComparisonResultType.MATCH);
    }

    /**
     * Tests that comparing two Digests, one with a hash of no data, gives a MISMATCH
     * comparison result.
     */
    @Test
    public final void testCompareToDigestWithOneEmptyHash() {
        final Digest d1 = new Digest(DigestAlgorithm.SHA1, getTestDigest(20));
        final Digest d2 = new Digest(DigestAlgorithm.SHA1, getEmptySHA1Digest());
        Assert.assertEquals(d1.compare(d2), DigestComparisonResultType.MISMATCH);
        Assert.assertEquals(d2.compare(d1), DigestComparisonResultType.MISMATCH);
    }

    /**
     * Tests that if someone tries to recreate a Digest using an invalid String, an error is thrown.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public final void testFromStringInvalid() {
        String invalidDigestString = "SHA1 00000000000000000000";
        Digest.fromString(invalidDigestString);
    }

    /**
     * Get a test SHA1 digest.
     *
     * @return a test SHA1 digest
     */
    public static Digest getTestSHA1Digest() {
        return new Digest(DigestAlgorithm.SHA1, getTestDigest(DIGEST_LENGTH_BYTES));
    }

    /**
     * Get a test SHA1 digest filled with the given byte.
     *
     * @param fill the byte that will be used to fill the digest
     * @return a test SHA1 digest with repeated entries of the given byte
     */
    public static Digest getTestSHA1Digest(final byte fill) {
        return new Digest(DigestAlgorithm.SHA1, getTestDigest(DIGEST_LENGTH_BYTES, fill));
    }

    private static byte[] getTestDigest(final int count) {
        return getTestDigest(count, (byte) 1);
    }

    private static byte[] getTestDigest(final int count, final byte fill) {
        final byte[] ret = new byte[count];
        for (int i = 0; i < count; ++i) {
            ret[i] = fill;
        }
        return ret;
    }

    private static byte[] getZeroValueDigest(final int count) {
        return new byte[count];
    }

    private byte[] getEmptySHA1Digest() {
        return DigestUtils.sha1(new byte[]{});
    }
}
