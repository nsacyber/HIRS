package hirs.attestationca.persist.tpm;

import hirs.attestationca.persist.entity.userdefined.record.TPMMeasurementRecord;
import hirs.utils.digest.Digest;
import hirs.utils.digest.DigestAlgorithm;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

/**
 * Java class for PcrInfoShort complex type, which was modified from code
 * auto-generated using the xjc command line tool. This code generates XML that
 * matches what is specified for the PcrInfoShortType ComplexType in the
 * IntegrityReportManifest_v1_0_1 schema file.
 * <p>
 * The PcrInfoShortType complex type is an XML representation of the TPM's
 * TPM_PCR_INFO_SHORT structure. PcrComposite is not part of the TPM
 * PCR_INFO_SHORT structure, however CompositeHash is a hash of PcrComposite,
 * thus PcrComposite is included to provide the data necessary to compute and
 * validate CompositeHash.
 */
@Log4j2
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PcrInfoShort",
        namespace = "http://www.trustedcomputinggroup.org/XML/SCHEMA/"
                + "Integrity_Report_v1_0#", propOrder = {"pcrSelection",
        "localityAtRelease", "compositeHash", "pcrComposite"})
@Embeddable
public class PcrInfoShort {

    @XmlElement(name = "PcrSelection", required = true)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "pcrSelect",
                    column = @Column(name = "selectionPcrSelect")
            )
    })
    private final PcrSelection pcrSelection;

    @XmlElement(name = "LocalityAtRelease")
    @XmlSchemaType(name = "unsignedByte")
    private final short localityAtRelease;

    @XmlElement(name = "CompositeHash", required = true)
    private final byte[] compositeHash;

    @XmlElement(name = "PcrComposite", required = true)
    @Embedded
    private final PcrComposite pcrComposite;

    /**
     * Default constructor necessary for marshalling/unmarshalling xml.
     */
    protected PcrInfoShort() {
        this.pcrSelection = new PcrSelection();
        this.pcrComposite = new PcrComposite();
        this.localityAtRelease = 0;
        this.compositeHash = new byte[0];
    }

    /**
     * Constructor used to create a PcrInfoShort object.
     *
     * @param pcrSelection      PcrSelection defines which TPM PCRs are used in the TPM Quote.
     * @param localityAtRelease short value includes locality information to provide the
     *                          requestor a more complete view of the current platform
     *                          configuration
     * @param compositeHash     A hash of PcrComposite
     * @param pcrComposite      A structure containing the actual values of the PCRs quoted.
     */
    public PcrInfoShort(final PcrSelection pcrSelection,
                        final short localityAtRelease, final byte[] compositeHash,
                        final PcrComposite pcrComposite) {
        if (pcrSelection == null) {
            log.error("null pcrSelection value");
            throw new NullPointerException("pcrSelection");
        }
        if (compositeHash == null) {
            log.error("null compositeHash value");
            throw new NullPointerException("compositeHash");
        }
        if (pcrComposite == null) {
            log.error("null pcrComposite value");
            throw new NullPointerException("pcrComposite");
        }

        this.pcrSelection = pcrSelection;
        this.localityAtRelease = localityAtRelease;
        this.compositeHash = compositeHash.clone();
        this.pcrComposite = pcrComposite;
    }

    /**
     * Gets the value of the pcrComposite property, a structure containing the
     * actual values of the PCRs quoted.
     *
     * @return possible object is {@link PcrComposite }
     */
    public final PcrComposite getPcrComposite() {
        return pcrComposite;
    }

    /**
     * Calculates the SHA-1 or SHA-256 digest of the PCR values the same way a TPM computes the
     * digest contained in the quote. Useful for TPM appraisal and for ensuring the digest of the
     * collected PCR values match the digest in the quote.
     *
     * @return byte array containing the digest
     * @throws NoSuchAlgorithmException if MessageDigest doesn't recognize "SHA-1" or "SHA-256"
     */
    public final byte[] getCalculatedDigest() throws NoSuchAlgorithmException {
        if (this.isTpm1()) {
            return getCalculatedDigestTpmV1p2(MessageDigest.getInstance("SHA-1"));
        } else {
            return getCalculatedDigestTpmV2p0(MessageDigest.getInstance("SHA-256"));
        }
    }

    /**
     * Calculates the SHA-1 digest of the PCR values the same way a TPM computes the digest
     * contained in the quote. Useful for TPM appraisal and for ensuring the digest of the
     * collected PCR values match the digest in the quote.
     *
     * @param messageDigest message digest algorithm to use
     * @return byte array containing the digest
     */
    private byte[] getCalculatedDigestTpmV1p2(final MessageDigest messageDigest) {
        byte[] computedDigest;

        final int sizeOfInt = 4;
        int sizeOfByteBuffer =
                this.pcrSelection.getLength() + sizeOfInt
                        + this.pcrComposite.getValueSize();

        ByteBuffer byteBuffer = ByteBuffer.allocate(sizeOfByteBuffer);
        log.debug("Size of the buffer allocated to hash: {}", sizeOfByteBuffer);

        byteBuffer.put(this.pcrSelection.getValue());
        byteBuffer.putInt(pcrComposite.getValueSize());

        for (TPMMeasurementRecord record : pcrComposite.getPcrValueList()) {
            byteBuffer.put(record.getHash().getDigest());
        }

        log.debug("PCR composite buffer to be hashed: {}",
                Hex.encodeHexString(byteBuffer.array()));
        computedDigest = messageDigest.digest(byteBuffer.array());
        log.debug("Calculated digest: {}", Hex.encodeHexString(computedDigest));

        return computedDigest;
    }

    /**
     * Calculates the digest of the PCR values the same way a TPM computes the digest contained in
     * the quote. Useful for TPM appraisal and for ensuring the digest of the collected PCR values
     * match the digest in the quote.
     *
     * @param messageDigest message digest algorithm to use
     * @return byte array containing the digest
     */
    private byte[] getCalculatedDigestTpmV2p0(final MessageDigest messageDigest) {
        int sizeOfByteBuffer = pcrComposite.getValueSize();
        ByteBuffer byteBuffer = ByteBuffer.allocate(sizeOfByteBuffer);
        log.debug("Size of the buffer allocated to hash: {}", sizeOfByteBuffer);
        Iterator iter = pcrComposite.getPcrValueList().iterator();

        while (iter.hasNext()) {
            TPMMeasurementRecord record = (TPMMeasurementRecord) iter.next();
            byteBuffer.put(record.getHash().getDigest());
        }

        log.debug("PCR composite buffer to be hashed: {}",
                Hex.encodeHexString(byteBuffer.array()));
        byte[] computedDigest = messageDigest.digest(byteBuffer.array());
        log.debug("Calculated digest: {}", Hex.encodeHexString(computedDigest));
        return computedDigest;
    }

    /**
     * Determines whether the TPM used to generate this pcr info is version 1.2 or not.
     *
     * @return whether the TPM used to generate this pcr info is version 1.2 or not
     */
    public boolean isTpm1() {
        // need to get an individual PCR and measure length to determine SHA1 v SHA 256
        List<TPMMeasurementRecord> pcrs = this.getPcrComposite().getPcrValueList();
        if (pcrs.size() == 0) {
            // it's the case of an empty pcrmask, so it doesn't matter
            return false;
        }

        Digest hash = pcrs.get(0).getHash();
        // check if the hash algorithm is SHA 1, if so it's TPM 1.2, if not it's TPM 2.0
        return hash.getAlgorithm() == DigestAlgorithm.SHA1;
    }
}
