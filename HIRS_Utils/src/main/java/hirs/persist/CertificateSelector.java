package hirs.persist;

import com.google.common.base.Preconditions;
import hirs.data.persist.certificate.Certificate;
import hirs.persist.service.CertificateService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * This class is used to select one or many certificates in conjunction
 * with a {@link hirs.persist.CertificateManager}.  To make use of this object,
 * use (some CertificateImpl).select(CertificateManager).
 *
 * This class loosely follows the builder pattern.  It is instantiated with
 * the type of certificate that should be retrieved.  It is possible to
 * further specify which certificate(s) should be retrieved by using an
 * instance's by* methods; each call to a by* method will further
 * restrict the result set.  At any time, the results may be retrieved
 * by using one of the get* methods according to the form the
 * results should be in.
 *
 * If no matching certificates were found for the query, the returned
 * value may empty or null, depending on the return type.
 *
 * For example, to retrieve all platform certificates:
 *
 * <pre>
 * {@code
 * Set<Certificate> certificates =
 *      certificateManager.select(Certificate.Type.PLATFORM)
 *      .getCertificates();
 * }
 * </pre>
 *
 * To retrieve all CA certificates in a KeyStore:
 *
 * <pre>
 * {@code
 * KeyStore trustStore =
 *      certificateManager.select(Certificate.Type.CERTIFICATE_AUTHORITY)
 *      .getKeyStore();
 * }
 * </pre>
 *
 * To retrieve all CA certificates matching a certain issuer in X509 format:
 *
 * <pre>
 * {@code
 * Set<X509Certificate> certificates =
 *      certificateManager.select(Certificate.Type.CERTIFICATE_AUTHORITY)
 *      .byIssuer("CN=Some certain issuer")
 *      .getX509Certificates();
 * }
 * </pre>
 *
 * @param <T> the type of certificate that will be retrieved
 */
public abstract class CertificateSelector<T extends Certificate> {
    private final CertificateService certificateService;
    private final Class<T> certificateClass;

    private final Map<String, Object> fieldValueSelections;
    private boolean excludeArchivedCertificates;

    /**
     * Construct a new CertificateSelector that will use the given {@link CertificateManager} to
     * retrieve certificates of the given type.
     *
     * @param certificateService the certificate manager to be used to retrieve certificates
     * @param certificateClass the class of certificate to be retrieved
     */
    public CertificateSelector(
            final CertificateService certificateService,
            final Class<T> certificateClass) {
        this(certificateService, certificateClass, true);
    }

    /**
     * Construct a new CertificateSelector that will use the given {@link CertificateManager} to
     * retrieve certificates of the given type.
     *
     * @param certificateService the certificate manager to be used to retrieve certificates
     * @param certificateClass the class of certificate to be retrieved
     * @param excludeArchivedCertificates true if excluding archived certificates
     */
    public CertificateSelector(
            final CertificateService certificateService,
            final Class<T> certificateClass, final boolean excludeArchivedCertificates) {
        Preconditions.checkArgument(
                certificateService != null,
                "certificate manager cannot be null"
        );

        Preconditions.checkArgument(
                certificateClass != null,
                "type cannot be null"
        );

        this.certificateService = certificateService;
        this.certificateClass = certificateClass;
        this.fieldValueSelections = new HashMap<>();
        this.excludeArchivedCertificates = excludeArchivedCertificates;
    }
    /**
     * Specify the entity id that certificates must have to be considered
     * as matching.
     *
     * @param uuid the UUID to query
     * @return this instance (for chaining further calls)
     */
    public CertificateSelector<T> byEntityId(final UUID uuid) {
        setFieldValue(Certificate.ID_FIELD, uuid);
        return this;
    }

    /**
     * Specify the hash code of the bytes that certificates must match.
     *
     * @param certificateHash the hash code of the bytes to query for
     * @return this instance (for chaining further calls)
     */
    public CertificateSelector<T> byHashCode(final int certificateHash) {
        setFieldValue(Certificate.CERTIFICATE_HASH_FIELD, certificateHash);
        return this;
    }

    /**
     * Specify a serial number that certificates must have to be considered
     * as matching.
     *
     * @param serialNumber the serial number to query
     * @return this instance (for chaining further calls)
     */
    public CertificateSelector<T> bySerialNumber(final BigInteger serialNumber) {
        setFieldValue(Certificate.SERIAL_NUMBER_FIELD, serialNumber);
        return this;
    }

    /**
     * Specify a holder serial number that certificates must have to be considered
     * as matching.
     *
     * @param holderSerialNumber the holder serial number to query
     * @return this instance (for chaining further calls)
     */
    public CertificateSelector<T> byHolderSerialNumber(final BigInteger holderSerialNumber) {
        setFieldValue(Certificate.HOLDER_SERIAL_NUMBER_FIELD, holderSerialNumber);
        return this;
    }

    /**
     * Specify an issuer string that certificates must have to be considered
     * as matching.
     *
     * @param issuer certificate issuer string to query, not empty or null
     * @return this instance (for chaining further calls)
     */
    public CertificateSelector<T> byIssuer(final String issuer) {
        Preconditions.checkArgument(
                StringUtils.isNotEmpty(issuer),
                String.format("%s: issuer cannot be null or empty.",
                        this.certificateClass.toString())
        );

        setFieldValue(Certificate.ISSUER_FIELD, issuer);
        return this;
    }

        /**
     * Specify a subject string that certificates must have to be considered
     * as matching.
     *
     * @param subject certificate subject string to query, not empty or null
     * @return this instance (for chaining further calls)
     */
    public CertificateSelector<T> bySubject(final String subject) {
        Preconditions.checkArgument(
                StringUtils.isNotEmpty(subject),
                String.format("%s: subject cannot be null or empty.",
                        this.certificateClass.toString())
        );

        setFieldValue(Certificate.SUBJECT_FIELD, subject);
        return this;
    }

    /**
     * Specify the sorted issuer string that certificates must have to be considered
     * as matching.
     *
     * @param issuerSorted certificate issuer organization string to query, not empty or null
     * @return this instance (for chaining further calls)
     */
    public CertificateSelector<T> byIssuerSorted(final String issuerSorted) {
        Preconditions.checkArgument(
                StringUtils.isNotEmpty(issuerSorted),
                String.format("%s: issuerSorted cannot be null or empty.",
                        this.certificateClass.toString())
        );

        setFieldValue(Certificate.ISSUER_SORTED_FIELD, issuerSorted);
        return this;
    }

    /**
     * Specify the sorted subject string that certificates must have to be considered
     * as matching.
     *
     * @param subjectSorted certificate subject organization string to query, not empty or null
     * @return this instance (for chaining further calls)
     */
    public CertificateSelector<T> bySubjectSorted(final String subjectSorted) {
        Preconditions.checkArgument(
                StringUtils.isNotEmpty(subjectSorted),
                String.format("%s: subjectSorted cannot be null or empty.",
                        this.certificateClass.toString())
        );

        setFieldValue(Certificate.SUBJECT_SORTED_FIELD, subjectSorted);
        return this;
    }

    /**
     * Specify a public key that certificates must have to be considered
     * as matching.
     *
     * @param encodedPublicKey the binary-encoded public key to query, not empty or null
     * @return this instance (for chaining further calls)
     */
    public CertificateSelector<T> byEncodedPublicKey(final byte[] encodedPublicKey) {
        Preconditions.checkArgument(
                ArrayUtils.isNotEmpty(encodedPublicKey),
                String.format("%s: publicKey cannot be null or empty.",
                        this.certificateClass.toString())
        );

        setFieldValue(
                Certificate.ENCODED_PUBLIC_KEY_FIELD,
                Arrays.copyOf(encodedPublicKey, encodedPublicKey.length)
        );

        return this;
    }

    /**
     * Specify the authority key identifier to find certificate(s).
     * @param authorityKeyIdentifier the string of the AKI associated with the certificate.
     * @return this instance
     */
    public CertificateSelector<T> byAuthorityKeyIdentifier(final String authorityKeyIdentifier) {
        Preconditions.checkArgument(
                StringUtils.isNotEmpty(authorityKeyIdentifier),
                String.format("%s: authorityKeyIdentifier cannot be null or empty.",
                        this.certificateClass.toString())
        );

        setFieldValue(Certificate.AUTHORITY_KEY_ID_FIELD, authorityKeyIdentifier);

        return this;
    }

    /**
     * Specify a public key modulus that certificates must have to be considered
     * as matching.
     *
     * @param publicKeyModulus a BigInteger representing a public key's modulus to query not null
     * @return this instance (for chaining further calls)
     */
    public CertificateSelector<T> byPublicKeyModulus(final BigInteger publicKeyModulus) {
        Preconditions.checkArgument(
                publicKeyModulus != null,
                String.format("%s: Public key modulus cannot be null",
                        this.certificateClass.toString())
        );

        setFieldValue(
                Certificate.PUBLIC_KEY_MODULUS_FIELD,
                publicKeyModulus.toString(Certificate.HEX_BASE)
        );

        return this;
    }

    /**
     * Set a field name and value to match.
     *
     * @param name the field name to query
     * @param value the value to query
     */
    protected void setFieldValue(final String name, final Object value) {
        Object valueToAssign = value;

        Preconditions.checkArgument(
                value != null,
                String.format("field value (%s) cannot be null.", name)
        );

        if (value instanceof String) {
            Preconditions.checkArgument(
                    StringUtils.isNotEmpty((String) value),
                    "field value cannot be empty."
            );
        }

        if (value instanceof byte[]) {
            byte[] valueBytes = (byte[]) value;

            Preconditions.checkArgument(
                    ArrayUtils.isNotEmpty(valueBytes),
                    String.format("field value (%s) cannot be empty.", name)
            );

            valueToAssign = Arrays.copyOf(valueBytes, valueBytes.length);
        }

        fieldValueSelections.put(name, valueToAssign);
    }

    /**
     * Retrieve the result set as a single {@link Certificate}.
     * This method is best used when selecting on a unique attribute.
     * If the result set contains more than one certificate, one is chosen
     * arbitrarily and returned.  If no matching certificates are found,
     * this method returns null.
     *
     * @return a matching certificate or null if none is found
     */
    public T getCertificate() {
        Set<T> certs = execute();
        if (certs.size() == 0) {
            return null;
        }
        return certs.iterator().next();
    }

    /**
     * Retrieve the result set as a set of {@link Certificate}s.
     * This method is best used when selecting on non-unique attributes.
     * Certificates are populated into the set in no specific order.
     * If no matching certificates are found, the returned Set will be empty.
     *
     * @return a Set of matching Certificates, possibly empty
     */
    public Set<T> getCertificates() {
        return Collections.unmodifiableSet(new HashSet<>(execute()));
    }

    /**
     * Retrieve the result set as a single {@link X509Certificate}.
     * This method is best used when selecting on a unique attribute.
     * If the result set contains more than one certificate, one is chosen
     * arbitrarily and returned.  If no matching certificates are found,
     * this method returns null.
     *
     * @return a matching certificate or null if none is found
     * @throws IOException if there is a problem reconstructing the X509Certificate
     */
    public X509Certificate getX509Certificate() throws IOException {
        Certificate cert = getCertificate();
        if (cert == null) {
            return null;
        }
        return cert.getX509Certificate();
    }

    /**
     * Retrieve the result set as a set of {@link X509Certificate}s.
     * This method is best used when selecting on non-unique attributes.
     * Certificates are populated into the set in no specific order.
     * If no matching certificates are found, the returned Set will be empty.
     *
     * @return a Set of matching Certificates, possibly empty
     * @throws IOException if there is a problem reconstructing the X509Certificates
     */
    public Set<X509Certificate> getX509Certificates() throws IOException {
        Set<X509Certificate> certs = new HashSet<>();
        for (Certificate cert : getCertificates()) {
            certs.add(cert.getX509Certificate());
        }
        return Collections.unmodifiableSet(certs);
    }

    /**
     * Retrieve the result set populated into a {@link KeyStore}.
     * Certificates are populated into a JKS-formatted KeyStore, with their aliases
     * set to their unique identifiers.
     * If no matching certificates are found, the returned KeyStore will be empty.
     *
     * @return a KeyStore populated with the matching certificates, if any
     * @throws KeyStoreException if there is a problem instantiating a JKS-formatted KeyStore
     * @throws IOException if there is a problem populating the keystore
     */
    public KeyStore getKeyStore() throws KeyStoreException, IOException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try {
            keyStore.load(null, "".toCharArray());
            for (Certificate cert : getCertificates()) {
                keyStore.setCertificateEntry(cert.getId().toString(), cert.getX509Certificate());
            }
        } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
            throw new IOException("Could not create and populate keystore", e);
        }

        return keyStore;
    }

    /**
     * Construct the criterion that can be used to query for certificates matching the configuration
     * of this {@link CertificateSelector}.
     *
     * @return a Criterion that can be used to query for certificates matching the configuration of
     *         this instance
     */
    Criterion getCriterion() {
        Conjunction conj = new Conjunction();

        for (Map.Entry<String, Object> fieldValueEntry : fieldValueSelections.entrySet()) {
            conj.add(Restrictions.eq(fieldValueEntry.getKey(), fieldValueEntry.getValue()));
        }

        if (this.excludeArchivedCertificates) {
            conj.add(Restrictions.isNull(Certificate.ARCHIVE_FIELD));
        }

        return conj;
    }

    /**
     * @return the certificate class that this instance will query
     */
    public Class<T> getCertificateClass() {
        return certificateClass;
    }

    // construct and execute query
    private Set<T> execute() {
        return certificateService.getCertificate(this);
    }

    /**
     * Configures the selector to query for archived and unarchived certificates.
     * @return the selector
     */
    public CertificateSelector<T> includeArchived() {
        excludeArchivedCertificates = false;
        return this;
    }
}
