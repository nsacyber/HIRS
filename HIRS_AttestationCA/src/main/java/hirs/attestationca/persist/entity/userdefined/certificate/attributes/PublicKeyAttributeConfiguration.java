package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;

import java.util.ArrayList;

/**
 * Basic class that handle Platform Configuration for the Platform Certificate
 * Attribute.
 */
@Log4j2
public class PublicKeyAttributeConfiguration  extends PlatformConfiguration {

    private static final int COMPONENT_CLASS_INDEX = 1;
    private static final int COMPONENT_DATA_INDEX = 2;

    /**
     * Constructor given the SEQUENCE that contains Platform Configuration.
     *
     * @param sequence containing the the Platform Configuration.
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public PublicKeyAttributeConfiguration(final ASN1Sequence sequence) throws IllegalArgumentException {

        //Default values
        setComponentIdentifier(new ArrayList<>());
        setComponentIdentifierUri(null);
        setPlatformProperties(new ArrayList<>());
        setPlatformPropertiesUri(null);

        for (int i = 0; i < sequence.size(); i++) {
            ASN1TaggedObject taggedSequence
                    = ASN1TaggedObject.getInstance(sequence.getObjectAt(i));

            ComponentClass cc = new ComponentClass("TCG", ASN1Sequence
                    .getInstance(taggedSequence.getLoadedObject())
                    .getObjectAt(COMPONENT_CLASS_INDEX).toString());
            ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(ASN1Sequence
                    .getInstance(taggedSequence.getLoadedObject()).getObjectAt(COMPONENT_DATA_INDEX));

            ComponentIdentifierV2 cV2 = new ComponentIdentifierV2(asn1Sequence, true);
            cV2.setComponentClass(cc);
            add(cV2);
        }
    }
}
