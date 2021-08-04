package hirs.data.persist.certificate.attributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DLSet;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;

import java.io.IOException;

/**
 * A class that will be specific to parsing a PKC attribute certificate.
 */
public class AttributeCertificatePkc extends ASN1Object {

    private static final Logger LOGGER = LogManager.getLogger(AttributeCertificatePkc.class);
    private static final int X509_CREDENTIAL_INDEX = 0;
    private static final int CREDENTIAL_TYPE_INDEX = 1;
    private static final int ATTRIBUTES_INDEX = 3;
    private static final int EXTENSIONS_INDEX = 7;
    private static final String PLATFORM_CREDENTIAL_TYPE = "2.23.133.2.25";
    private static final String TBB_SECURITY_ASSERTION = "2.23.133.2.19";

    private ASN1Integer x509CredentialVersion;
    private ASN1Sequence extensionSequence;
    private ASN1Encodable[] attributeArray;
    private ASN1Sequence credentialOid;

    /**
     * Default constructor that takes in an attributeCertificateInfoPkc.
     * @param obj an asn1Encodable object
     * @throws IOException start it up
     */
    public AttributeCertificatePkc(final Object obj) throws IOException {
        this(ASN1Sequence.getInstance(obj));
    }

    private AttributeCertificatePkc(final ASN1Sequence asn1Sequence) {
        ASN1Sequence subSequence = null;
        try {
            ASN1TaggedObject taggedObj = ASN1TaggedObject.getInstance(ASN1Sequence.getInstance(
                    asn1Sequence.getObjectAt(0)).getObjectAt(X509_CREDENTIAL_INDEX));
            this.x509CredentialVersion = ASN1Integer.getInstance(
                    ASN1TaggedObject.getInstance(taggedObj), true);

            taggedObj = ASN1TaggedObject.getInstance(
                    ASN1Sequence.getInstance(asn1Sequence.getObjectAt(0))
                            .getObjectAt(EXTENSIONS_INDEX));
            this.extensionSequence = ASN1Sequence.getInstance(taggedObj.getObjectParser(
                    taggedObj.getTagNo(), true));

            // basic constraints
            // keyUsage
            // subjectAltName
            ASN1Sequence tbbSequence = ASN1Sequence.getInstance(Extensions
                    .getInstance(this.extensionSequence)
                    .getExtensionParsedValue(new ASN1ObjectIdentifier(TBB_SECURITY_ASSERTION)));
            subSequence = ASN1Sequence.getInstance(tbbSequence.getObjectAt(0));
        } catch (IOException ioEx) {
            return;
        }
        // PKC OID
        this.credentialOid = ASN1Sequence.getInstance(((DLSet) subSequence
                .getObjectAt(CREDENTIAL_TYPE_INDEX)).getObjectAt(0));
        // sequence with attributes
        this.attributeArray = ((DLSet) subSequence.getObjectAt(ATTRIBUTES_INDEX)).toArray();
    }

    /**
     * This is a thing.
     * @param obj base object
     * @return An instance of the class object.
     * @throws IOException start it up
     */
    public static AttributeCertificatePkc getInstance(final Object obj) throws IOException {
        if (obj instanceof AttributeCertificatePkc) {
            return (AttributeCertificatePkc) obj;
        } else {
            if (obj != null) {
                ASN1Sequence seq = ASN1Sequence.getInstance(obj);
                return new AttributeCertificatePkc(seq);
            } else {
                return null;
            }
        }
    }

    /**
     * Getter for version number for this certificate type.
     * @return int of the value
     */
    public ASN1Integer getX509CredentialVersion() {
        return x509CredentialVersion;
    }

    /**
     * Getter for the Extension associated with the string oid.
     * @param oid string object of the oid
     * @return the extension associated
     */
    public Extension getExtension(final String oid) {
        return getExtension(new ASN1ObjectIdentifier(oid));
    }

    /**
     * Getter for the Extension associated with the oid.
     * @param oid ASN1ObjectIdentifier of the oid
     * @return the extension associated
     */
    public Extension getExtension(final ASN1ObjectIdentifier oid) {
        return Extensions.getInstance(this.extensionSequence).getExtension(oid);
    }

    /**
     * Getter the component attributes for the certificate.
     * @return an ASN1Sequence object
     *
     * @throws IOException when reading the certificate.
     */
    public ASN1Sequence getAttributes() throws IOException {
        ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(DERTaggedObject
                .getInstance(attributeArray[0]).getObjectParser(DERTaggedObject
                        .getInstance(attributeArray[0]).getTagNo(), true));



        return asn1Sequence;
    }

    /**
     * Getter for the OID that identifies if this is a PKC.
     * @return an asn1objectidentifier object
     */
    public ASN1ObjectIdentifier getCredentialType() {
        return ASN1ObjectIdentifier.getInstance(credentialOid.getObjectAt(0));
    }

    /**
     * Interface class method that has to be implemented.
     * @return an ASN1Primitive.
     */
    @Override
    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector vector = new ASN1EncodableVector();

        if (x509CredentialVersion.getValue().intValue() != 0) {
            vector.add(x509CredentialVersion);
        }
        vector.add(extensionSequence);

        return new DERSequence(vector);
    }
}
