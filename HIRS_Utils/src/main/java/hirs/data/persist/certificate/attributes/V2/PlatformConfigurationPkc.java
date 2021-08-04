package hirs.data.persist.certificate.attributes.V2;

import hirs.data.persist.certificate.attributes.ComponentClass;
import hirs.data.persist.certificate.attributes.PlatformConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;

import java.util.ArrayList;

/**
 * Basic class that handle Platform Configuration for the Platform Certificate
 * Attribute.
 */
public class PlatformConfigurationPkc extends PlatformConfiguration {

    private static final Logger LOGGER = LogManager.getLogger(PlatformConfigurationPkc.class);

    private static final int COMPONENT_CLASS_INDEX = 1;
    private static final int COMPONENT_DATA_INDEX = 2;

    /**
     * Constructor given the SEQUENCE that contains Platform Configuration.
     * @param sequence containing the the Platform Configuration.
     * @throws IllegalArgumentException if there was an error on the parsing
     */
    public PlatformConfigurationPkc(final ASN1Sequence sequence) throws IllegalArgumentException {

        //Default values
        setComponentIdentifier(new ArrayList<>());
        setComponentIdentifierUri(null);
        setPlatformProperties(new ArrayList<>());
        setPlatformPropertiesUri(null);

        for (int i = 0; i < sequence.size(); i++) {
            ASN1TaggedObject taggedSequence
                    = ASN1TaggedObject.getInstance(sequence.getObjectAt(i));

            ComponentClass cc = new ComponentClass("TCG", ASN1Sequence
                    .getInstance(taggedSequence.getObject())
                    .getObjectAt(COMPONENT_CLASS_INDEX).toString());
            ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(ASN1Sequence
                    .getInstance(taggedSequence.getObject()).getObjectAt(COMPONENT_DATA_INDEX));

            ComponentIdentifierV2 cV2 = new ComponentIdentifierV2(asn1Sequence, true);
            cV2.setComponentClass(cc);
            add(cV2);
        }
    }
}
