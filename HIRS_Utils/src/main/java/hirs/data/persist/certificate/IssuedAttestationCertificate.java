package hirs.data.persist.certificate;

import hirs.persist.CertificateManager;
import hirs.persist.CertificateSelector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

/**
 * Represents an issued attestation certificate to a HIRS Client.
 */
@Entity
public class IssuedAttestationCertificate extends DeviceAssociatedCertificate {

    private static final Logger LOGGER = LogManager.getLogger(IssuedAttestationCertificate.class);

    private static final int MAX_CERT_LENGTH_BYTES = 1024;
    private static final String CATALINA_HOME = System.getProperty("catalina.base");
    private static final String TOMCAT_UPLOAD_DIRECTORY
            = "/webapps/HIRS_AttestationCAPortal/upload/device_pcrs/";
    private static final String PCR_UPLOAD_FOLDER
            = CATALINA_HOME + TOMCAT_UPLOAD_DIRECTORY;

    /**
     * AIC label that must be used.
     */
    public static final String AIC_TYPE_LABEL = "TCPA Trusted Platform Identity";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ek_id")
    private EndorsementCredential endorsementCredential;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "pc_id")
    private Set<PlatformCredential> platformCredentials;

    @Column(nullable = true, length = MAX_CERT_LENGTH_BYTES)
    private String pcrValues;

    /**
     * This class enables the retrieval of IssuedAttestationCertificate by their attributes.
     */
    public static class Selector extends CertificateSelector<IssuedAttestationCertificate> {
        /**
         * Construct a new CertificateSelector that will use the given {@link CertificateManager} to
         * retrieve one or many IssuedAttestationCertificate.
         *
         * @param certificateManager the certificate manager to be used to retrieve certificates
         */
        public Selector(final CertificateManager certificateManager) {
            super(certificateManager, IssuedAttestationCertificate.class);
        }

        /**
         * Specify a device id that certificates must have to be considered
         * as matching.
         *
         * @param device the device id to query
         * @return this instance (for chaining further calls)
         */
        public Selector byDeviceId(final UUID device) {
            setFieldValue(DEVICE_ID_FIELD, device);
            return this;
        }
    }

    /**
     * Get a Selector for use in retrieving IssuedAttestationCertificate.
     *
     * @param certMan the CertificateManager to be used to retrieve persisted certificates
     * @return a IssuedAttestationCertificate.Selector instance to use for retrieving certificates
     */
    public static IssuedAttestationCertificate.Selector select(final CertificateManager certMan) {
        return new IssuedAttestationCertificate.Selector(certMan);
    }


    /**
     * Default constructor for Hibernate.
     */
    protected IssuedAttestationCertificate() {

    }

    /**
     * Constructor.
     * @param certificateBytes the issued certificate bytes
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials the platform credentials
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public IssuedAttestationCertificate(final byte[] certificateBytes,
                                        final EndorsementCredential endorsementCredential,
                                        final Set<PlatformCredential> platformCredentials)
            throws IOException {
        super(certificateBytes);
        this.endorsementCredential = endorsementCredential;
        this.platformCredentials = platformCredentials;
    }

    /**
     * Constructor.
     * @param certificatePath path to certificate
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials the platform credentials
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public IssuedAttestationCertificate(final Path certificatePath,
                                        final EndorsementCredential endorsementCredential,
                                        final Set<PlatformCredential> platformCredentials)
            throws IOException {
        this(readBytes(certificatePath), endorsementCredential, platformCredentials);
    }

    /**
     *
     * @return the Endorsement Credential
     */
    public EndorsementCredential getEndorsementCredential() {
        return endorsementCredential;
    }

    /**
     *
     * @return the platform credential
     */
    public Set<PlatformCredential> getPlatformCredentials() {
        return Collections.unmodifiableSet(platformCredentials);
    }

    /**
     * Getter for the pcrValues passed up by the client.
     * @return a string blob of pcrs
     */
    public String getPcrValues() {
        return pcrValues;
    }

    /**
     * Setter for the pcrValues passed up by the client.
     * @param pcrValues to be stored.
     */
    public void setPcrValues(final String pcrValues) {
        this.pcrValues = savePcrValues(pcrValues);
    }

    private String savePcrValues(final String pcrValues) {
        try {
            if (Files.notExists(Paths.get(PCR_UPLOAD_FOLDER))) {
                Files.createDirectory(Paths.get(PCR_UPLOAD_FOLDER));
            }
            Path pcrPath = Paths.get(String.format("%s/%s",
                    PCR_UPLOAD_FOLDER, this.getDevice().getName()));
            if (Files.notExists(pcrPath)) {
                Files.createFile(pcrPath);
            }
            Files.write(pcrPath, pcrValues.getBytes("UTF8"));
            return pcrPath.toString();
        } catch (NoSuchFileException nsfEx) {
            LOGGER.error(String.format("File Not found!: %s",
                    this.getDevice().getName()));
            LOGGER.error(nsfEx);
        } catch (IOException ioEx) {
            LOGGER.error(ioEx);
        }

        return "";
    }
}
