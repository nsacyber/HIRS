package hirs.utils.rim.unsignedRim.cbor.ietfCorim;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.bouncycastle.asn1.x509.Extension;

import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORPair;
import com.authlete.cbor.CBORPairList;
import com.authlete.cbor.CBORString;
import com.authlete.cbor.CBORTaggedItem;
import com.authlete.cose.COSEProtectedHeader;
import com.authlete.cose.COSEUnprotectedHeaderBuilder;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.module.SimpleModule;

import hirs.utils.crypto.DefaultCrypto;
import hirs.utils.signature.SignatureHelper;
import hirs.utils.signature.cose.CoseAlgorithm;
import hirs.utils.signature.cose.CoseSignature;
import hirs.utils.rim.unsignedRim.GenericRim;

/**
 * Class containing the logic used to build out a CoRIM from user input.
 */
public final class CoRimBuilder {
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm";
    /** Tag number for unsigned CoRIM. */
    public static final int TAGGED_UNSIGNED_CORIM_MAP = 501;

    /**
     * Builds a CoRIM object from a given configuration file.
     *
     * @param configFile The input file for the CoRIM.
     * @return The byte array containing the output CoRIM, in unsigned CBOR
     * format.
     */
    public static byte[] build(final String configFile) {
        final ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        // Add date formatting
        final DateFormat df = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        objectMapper.setDateFormat(df);
        // Add serializer for hexadecimal byte[] deserialization
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(byte[].class, new HexByteArrayDeserializer());
        objectMapper.registerModule(module);
        try {
            final byte[] data = Files.readAllBytes(Paths.get(configFile));
            final CoRimConfig corimConfig = objectMapper.readValue(data, CoRimConfig.class);
            return createCborFromCorim(corimConfig);
        } catch (final UnrecognizedPropertyException upe) {
            throw new RuntimeException(
                    "Invalid field contained in CoRIM configuration file: " + upe.getPropertyName());
        } catch (final Exception e) {
            throw new RuntimeException(
                    "Error building CoRIM from input file " + configFile + ": " + e.getMessage());
        }
    }

    /**
     * Create signed CoRIM from existing unsigned CoRIM object.
     *
     * @param unsignedCorim The original, unsigned CoRIM object.
     * @param keyPath The path of the private key used to sign the CoRIM.
     * @param certPath The path of the public certificate used for the CoRIM.
     * @param algName The IANA algorithm used for signing the CoRIM.
     * @param isEmbedded Set to true if embedding a signing certificate per RFC
     * 9360.
     *
     * @return A signed CoRIM object, given the original CoRIM.
     */
    public static byte[] createSignedCorim(final byte[] unsignedCorim, final String keyPath,
                                           final String certPath, final String algName,
                                           final boolean isEmbedded) {
        // Read certificate from file
        X509Certificate cert = null;
        try (FileInputStream is = new FileInputStream(certPath)) {
            final CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) certFactory.generateCertificate(is);
        } catch (final Exception e) {
            throw new RuntimeException(
                    "Error reading public certificate from input file " + certPath + ": " + e.getMessage());
        }

        // Load private key
        final DefaultCrypto cryptoProvider = new DefaultCrypto();
        try {
            cryptoProvider.loadPrivateKey(keyPath, cert, algName);
        } catch (final Exception e) {
            throw new RuntimeException(
                    "Error reading private key from input file " + keyPath + ": " + e.getMessage());
        }
        final String finalAlgName = cryptoProvider.getAlgorithm(); // Reassign algName such that
        // matches crypto provider

        // Create COSE-Sign1-CoRim structure
        final ArrayList<CBORItem> coseSign1Items = new ArrayList<>();
        COSEProtectedHeader protectedHeader = null;
        // Add protected header
        try {
            protectedHeader = createProtectedCorimHeader(CoseAlgorithm.getAlgId(finalAlgName),
                    cert, isEmbedded);
        } catch (final Exception e) {
            throw new RuntimeException("Error creating protected COSE header for CORIM. " + e.getMessage());
        }
        coseSign1Items.add(protectedHeader);
        // Add unprotected header
        coseSign1Items.add(new COSEUnprotectedHeaderBuilder().build());
        // Add payload
        coseSign1Items.add(new CBORByteArray(unsignedCorim));
        // Add signature
        // Create signature block (Sig_structure) and ToBeSigned
        try {
            // byte[] toBeSigned = new CoseSignature().createToBeSigned(cert,
            // unsignedCorim, protectedHeader);
            final byte[] toBeSigned = new CoseSignature().createToBeSigned(
                    SignatureHelper.getCoseAlgFromCert(cert), SignatureHelper.getKidFromCert(cert),
                    unsignedCorim, cert, false, false,
                    GenericRim.RIMTYPE_CORIM_COMID); // need protectedHeader
            final byte[] signature = cryptoProvider.sign(toBeSigned);
            coseSign1Items.add(new CBORByteArray(signature));
        } catch (final Exception e) {
            throw new RuntimeException("Failed to sign CoRIM" + e.getMessage());
        }

        return new CBORTaggedItem(18, new CBORItemList(coseSign1Items)).encode();
    }

    /**
     * Constructs a {@link COSEProtectedHeader} for a CoRIM.
     *
     * @param alg the COSE algorithm
     * @param publicCert the X.509 certificate whose key will be used for the
     * header
     * @param isEmbedded whether to embed the full certificate and its
     * thumbprint in the header
     * @return a populated {@link COSEProtectedHeader} representing the
     * protected CoRIM header
     * @throws CertificateEncodingException if encoding the certificate fails
     * @throws NoSuchAlgorithmException if the thumbprint algorithm is not found
     */
    public static COSEProtectedHeader createProtectedCorimHeader(final int alg,
                                                                 final X509Certificate publicCert,
                                                                 final boolean isEmbedded)
            throws CertificateEncodingException, NoSuchAlgorithmException {
        // Create protected-corim-header-map
        final ArrayList<CBORPair> pchMapItems = new ArrayList<>();
        pchMapItems.add(new CBORPair(new CBORInteger(1), new CBORInteger(alg))); // alg
        pchMapItems.add(new CBORPair(new CBORInteger(3),
                new CBORString("application/rim+cbor"))); // content-type
        pchMapItems.add(new CBORPair(new CBORInteger(4),
                new CBORByteArray(publicCert.getExtensionValue(Extension.subjectKeyIdentifier
                        .getId())))); // kid
        pchMapItems.add(new CBORPair(new CBORInteger(8),
                new CBORByteArray(createCorimMetaMap(publicCert).encode()))); // corim-meta
        // Embed per RFC 9360
        if (isEmbedded) {
            // Add x5chain
            pchMapItems.add(new CBORPair(new CBORInteger(33), new CBORByteArray(publicCert.getEncoded())));
            // Add x5t (thumbprint)
            final List<CBORItem> x5tList = new ArrayList<>();
            x5tList.add(new CBORInteger(CoseAlgorithm.COSE_SHA_256)); // hashAlg
            x5tList.add(new CBORByteArray(CoseSignature.getThumbprint(publicCert))); // hashValue
            final CBORItemList x5tItemList = new CBORItemList(x5tList);
            pchMapItems.add(new CBORPair(new CBORInteger(34), x5tItemList));
        }
        final CBORPairList pchMapList = new CBORPairList(pchMapItems);
        return new COSEProtectedHeader(pchMapList.encode(), pchMapItems);
    }

    /**
     * Creates a CoRIM metadata map (corim-meta-map) containing signer
     * information.
     *
     * @param publicCert the X.509 certificate used to derive signer information
     * @return a {@link CBORPairList} representing the CoRIM metadata map
     */
    protected static CBORPairList createCorimMetaMap(final X509Certificate publicCert) {
        // Create corim-signer-map first
        final ArrayList<CBORPair> csmItems = new ArrayList<>();
        csmItems.add(new CBORPair(new CBORInteger(0), new CBORString("HIRS"))); // signer-name
        final CBORPairList signerMap = new CBORPairList(csmItems);

        final ArrayList<CBORPair> cmmItems = new ArrayList<>();
        cmmItems.add(new CBORPair(new CBORInteger(0), signerMap)); // corim-signer-map
        return new CBORPairList(cmmItems);
    }

    /**
     * Encodes a CoRIM configuration into a CBOR byte array with the appropriate
     * CoRIM tag.
     *
     * @param corimConfig the CoRIM configuration to encode
     * @return the encoded CBOR byte array representing the tagged unsigned
     * CoRIM map
     */
    protected static byte[] createCborFromCorim(final CoRimConfig corimConfig) {
        final CBORItem unsignedCorimMap = corimConfig.build();
        // Wrap item for tagged-unsigned-corim-map
        final CBORTaggedItem taggedUnsignedCorimMap = new CBORTaggedItem(TAGGED_UNSIGNED_CORIM_MAP,
                unsignedCorimMap);
        return taggedUnsignedCorimMap.encode();
    }

    /**
     * Default constructor.
     */
    private CoRimBuilder() {
    }

}
