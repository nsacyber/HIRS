package hirs.utils.signature.cose;

import lombok.Getter;
import lombok.Setter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import com.authlete.cbor.CBORByteArray;
import com.authlete.cbor.CBORInteger;
import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORItemList;
import com.authlete.cbor.CBORDecoder;
import com.authlete.cbor.CBORTaggedItem;
import com.authlete.cbor.CBORNull;
import com.authlete.cose.COSESign1;
import com.authlete.cose.COSEUnprotectedHeader;
import com.authlete.cose.COSEProtectedHeader;
import com.authlete.cose.COSEProtectedHeaderBuilder;
import com.authlete.cose.COSEUnprotectedHeaderBuilder;
import com.authlete.cose.COSEException;
import com.authlete.cose.COSESign1Builder;
import com.authlete.cose.SigStructure;
import com.authlete.cose.SigStructureBuilder;
import hirs.utils.signature.SignatureFormat;
import hirs.utils.signature.SignatureHelper;
import hirs.utils.signature.cose.Cbor.CborContentTypes;
import hirs.utils.signature.cose.Cbor.CborTagProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Class for implementing rfc rfc9052 CBOR Object Signing and Encryption (COSE)
 * Refer to https://datatracker.ietf.org/doc/html/rfc9053
 *
 * COSE_Sign = [
 *        Headers,
 *        payload : bstr / nil,
 *        signatures : [+ COSE_Signature]
 *    ]
 *    From section 4.4 of rfc 9052 "How to compute a signature:
 *    1.  Create a Sig_structure and populate it with the appropriate fields.
 *    2.  Create the value ToBeSigned by encoding the Sig_structure to a
 *        byte string, using the encoding described in Section 9.
 *    3.  Call the signature creation algorithm, passing in K (the key to
 *        sign with), alg (the algorithm to sign with), and ToBeSigned (the value to sign).
 *    4.  Strip off the DER encoding from the Signature field placed on by
 *        Java.Security. Even though RFC 9052 does not specify a format,
 *        The COSE Working Groups test patterns use a "Raw" (IEEE P1363) format.
 *    5.  Place the resulting signature value in the correct location.
 *        This is the "signature" field of the COSE_Signature or COSE_Sign1 structure.
 */
public class CoseSignature implements SignatureFormat {
    // COSE Generic Header
    @Setter
    @Getter
    private int algId = 0;
    private byte[] toBeSigned = null;
    private byte[] payload = null;
    private byte[] signature = null;
    private byte[] keyId = null;
    private byte[] protectedHeaders = null;
    private COSESign1Builder coseBuilder = null;
    private static final Logger LOGGER = LogManager.getLogger(CoseSignature.class);

    /**
     * Default CoseSignature constructor for a COSE (rfc 9052) object.
     */
    public CoseSignature() {
    }

    /**
     * Create toBeSigned using supplied kid and algorithm for testing only.
     * Kid will be assigned to the unprotected header for tests.
     * @param algId IANA registered COSE Algorithm String
     * @param kid Key Identifier
     * @param payload  data to be placed in the payload
     * @param signingCert a signing certificate used if the embedded parameter is true
     * @param embedded if true, embeds the signing certificate and thumbprint per RFC 9360
     * @param rimType the type of RIM, for use with the protected header content-type
     * @param useUnprotectedKid will place kid in unprotected header if true
     * @return the COSE_Sign1 toBeSigned data
     * @throws CertificateEncodingException
     * @throws NoSuchAlgorithmException
     */
    public byte[] createToBeSigned(final int algId, final byte[] kid, final byte[] payload,
                                   final X509Certificate signingCert, final boolean useUnprotectedKid,
                                   final boolean embedded, final String rimType)
            throws CertificateEncodingException, NoSuchAlgorithmException {
        final int certhash = 34;
        final int certchain = 33;
        byte[] newKid = new byte[kid.length];
        System.arraycopy(kid, 0, newKid, 0, kid.length);
        COSEProtectedHeader pHeader = null;
        COSEUnprotectedHeader uHeader = null;
        coseBuilder = new COSESign1Builder();
        // Create protected header (with alg)
        COSEProtectedHeaderBuilder pHeaderBuilder = new COSEProtectedHeaderBuilder().alg(algId);
        // Add content-type based on RIM type
        CborContentTypes contentType = CborContentTypes.getContentTypeFromRimType(rimType);
        pHeaderBuilder = pHeaderBuilder.contentType(contentType.getContentType());
        // Only add kid if flag is false
        if ((newKid == null) | (kid.length == 0)) {
            newKid = SignatureHelper.getKidFromCert(signingCert);
            if (newKid == null) {
                throw new RuntimeException("kid is required but not supplied");
            }
        }
        if (useUnprotectedKid) {
            uHeader = new COSEUnprotectedHeaderBuilder().kid(newKid).build();
            coseBuilder.unprotectedHeader(uHeader);
        } else {
            pHeaderBuilder = pHeaderBuilder.kid(newKid);
        }
        // Embed per RFC 9360
        if (embedded) {
            // Add x5chain
            pHeaderBuilder.put(new CBORInteger(certchain), signingCert.getEncoded());
            // Add x5t (thumbprint)
            List<CBORItem> x5tList = new ArrayList<>();
            x5tList.add(new CBORInteger(CoseAlgorithm.COSE_SHA_256)); // hashAlg
            x5tList.add(new CBORByteArray(getThumbprint(signingCert))); // hashValue
            CBORItemList x5tItemList = new CBORItemList(x5tList);
            pHeaderBuilder.put(new CBORInteger(certhash), x5tItemList);
        }
        pHeader = pHeaderBuilder.build();
        this.setKeyId(newKid);
        return finalizeToBeSigned(payload, pHeader);
    }

    /**
     * Follows the "The steps for verifying a signature are" of section 4.4. of rfc9052 Signing
     * and Verification Process.
     *  https://datatracker.ietf.org/doc/html/rfc9052#section-4.4
     *  Steps 1 and 2.
     *  Note that step 3 (verify, the final step) is handled by a Cryptographic Engine
     *
     * @param coseData byte array holding the data to be verified
     * @return toBeVerified data
     */
    public byte[] getToBeVerified(final byte[] coseData) throws IOException {
        // COSEProtectedHeader pheader = processCose(coseData, true);
        return toBeSigned.clone();
    }

    /**
     * Takes a Cose signed object and a detached signature object and creates the toBeVerified data
     * used for signature verification.
     * Uses the protected header from the signed structure and the supplied payload
     * to create the toBeVerified data.
     * @param coseData
     * @param detachedPayload a detached signature (Cose detached content) which is actually just the payload
     * @return toBeVerfied data to be used with the java signature verification
     * @throws IOException
     */
    public byte[] getToBeVerified(final byte[] coseData, final byte[] detachedPayload) throws IOException {
        COSEProtectedHeader pheader = processCose(coseData, false);
        return finalizeToBeSigned(detachedPayload, pheader);
    }

    /**
     * Parses a cose object and populated this classes member variables.
     * @param coseData signed cose object
     * @param genToBeSinged if true the toBeSigned variable will be populated.
     *                      Should be set to false when processing a detached signature
     * @return a protected header object
     * @throws IOException
     */
    private COSEProtectedHeader processCose(final byte[] coseData, final boolean genToBeSinged)
            throws IOException {
        COSEProtectedHeader pheader = null;
        CBORDecoder cborDecoder = new CBORDecoder(coseData);
        if (coseBuilder == null) {
            coseBuilder = new COSESign1Builder();
        }
        COSESign1 sign1 = null;
        // String process = "Processing toBeSigned for verification: ";
        String status = "Parsing Cose Tag, expecting tag 18 (cose-sign1):";
        CborTagProcessor ctp = new CborTagProcessor(coseData);
        int coseTag = ctp.getTagId();
        if (coseTag != CoseType.COSE_SIGN_1) {
            throw new RuntimeException("Error parsing COSE signature: COSE tag of "
                    + coseTag + " found but only cose-sign1 (18) is supported");
        }
        try {
            status = "Decoding COSE object";
            CBORItem coseObject = cborDecoder.next();
            ArrayList<Object> parsedata = (ArrayList) coseObject.parse();
            COSESign1 signOne = COSESign1.build(parsedata);
            status = "Decoding COSE Protected Header";
            pheader = signOne.getProtectedHeader();
            status = "Decoding COSE Unprotected Header";
            COSEUnprotectedHeader uheader = signOne.getUnprotectedHeader();
            status = "Checking Cose headers for required Algorithm Identifier";
            if (pheader.getAlg() != null) {
                Object algObject = (Object) pheader.getAlg();
                if (algObject instanceof String) {  // library will return a String if algorithm is unknown
                    String sAlg = (String) pheader.getAlg();
                    if (sAlg.compareToIgnoreCase("unknown") == 0) {
                        throw new RuntimeException("Unknown Algorithm Identifier found in COSE header");
                    }
                } else {
                    algId = (int) pheader.getAlg();
                }
            } else if (uheader.getAlg() != null) {
                algId = (int) uheader.getAlg();
            } else {
                throw new RuntimeException("Algorithm ID required but not found in COSE header");
            }
            status = "Checking Cose headers for required Key ID (kid)";
            if (pheader.getKid() != null) {
                keyId = pheader.getKid();
            } else if (uheader.getKid() != null) {
                keyId = uheader.getKid();
            } else {
                LOGGER.warn("Key ID not found in COSE header");
            }
            status = "retrieving signature from COSE object";
            signature = signOne.getSignature().getValue();
            status = "Retrieving payload from COSE object";
            payload = signOne.getPayload().encode();
            if (genToBeSinged) {
                status = "Retrieving protected and unprotected header from COSE object";
                SigStructure ssb = new SigStructureBuilder().sign1(signOne).build();
                toBeSigned = ssb.encode();
            }
            return pheader;
        } catch (COSEException e) {
            throw new RuntimeException("Error processing a Cose Signature: " + status
                    + " /nException details:" + e.getMessage());
        }
    }

    /**
     * Creates the toBeSigned structure from a pre-processed header and payload data.
     * @param data byte array holding to be signed data
     * @param pHeader cose header to be included in final cose object
     * @return the COSE_Sign1 toBeSigned data
     */
    private byte[] finalizeToBeSigned(final byte[] data, final COSEProtectedHeader pHeader) {
        CBORByteArray encodedPayload = new CBORByteArray(data);
        SigStructure structure = new SigStructureBuilder()
                .signature1()
                .bodyAttributes(pHeader)
                .payload(encodedPayload)
                .build();
        toBeSigned = structure.encode();
        coseBuilder.payload(encodedPayload);
        coseBuilder.protectedHeader(pHeader);
        payload = data;
        return toBeSigned;
    }

    /**
     *   Performs step 4 of  the  "How to compute a signature" section.
     *      from https://datatracker.ietf.org/doc/html/rfc9052#section-4.4
     *
     *   4. Place the resulting signature value in the correct location.
     *      This is the "signature" field of the COSE_Signature or COSE_Sign1 structure.
     *
     * @param  signatureBytes data generated from step 3. Note step 3 is performed by a Cryptographic Engine
     */
    @Override
    public void addSignature(final byte[] signatureBytes) throws IOException {
        signature = signatureBytes.clone();
        coseBuilder.signature(signatureBytes);
    }

    /**
     * Encodes the signature data an updates class variables.
     * @return byte array holding the singed data
     */
    @Override
    public byte[] getSignedData() throws IOException {
        COSESign1 sigData = coseBuilder.build();
        // Set local variables for future use
        // byte[] rawSignature = sigData.getSignature().getValue();
        protectedHeaders = sigData.getProtectedHeader().getValue();
        CBORTaggedItem taggedCose = new CBORTaggedItem(CoseType.COSE_SIGN_1, sigData);
        return taggedCose.encode().clone();
    }

    /**
     * Obtain the SHA-256 thumbprint of an X.509 certificate (used for embedding).
     *
     * @param cert The input X.509 certificate.
     * @return The SHA-256 thumbprint corresponding to the certificate.
     * @throws NoSuchAlgorithmException if the SHA-256 algorithm is unsupported
     * @throws CertificateEncodingException if the certificate cannot be encoded to DER
     */
    public static byte[] getThumbprint(final X509Certificate cert) throws NoSuchAlgorithmException,
            CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(cert.getEncoded());
        return md.digest();
    }

    /**
     * Provides a nil CBOR object as defined for supporting "Detached signatures"
     * (referred to as "detached content" in rfc 9052).
     */
    public void setNilPayload() {
        coseBuilder.payload(CBORNull.INSTANCE);
    }

    /**
     * Validates the thumbprint of a given protected header and certificate contents.
     * @param cert The embedded cert to validate.
     * @param pHeader The protected header contents (containing thumbprint) to validate against.
     * @return True if the contents are validated; false otherwise.
     */
    private boolean validateThumbprint(final X509Certificate cert, final COSEProtectedHeader pHeader) {
        Integer thumbprintAlg;
        final int certhash = 34; // per rfc 9360
        byte[] thumbprint;
        boolean validated = false;
        final Integer[] thumbprintAlgHolder = new Integer[1];
        final byte[][] thumbprintHolder = new byte[1][];
        pHeader.getPairs().stream()
                .filter(pair -> Objects.equals(((CBORInteger) pair.getKey()).getValue(), certhash))
                .findFirst()
                .ifPresent(pair -> {
                    CBORItemList x5tList = (CBORItemList) pair.getValue();
                    var items = x5tList.getItems();
                    thumbprintAlgHolder[0] = ((CBORInteger) items.get(0)).getValue();
                    thumbprintHolder[0] = ((CBORByteArray) items.get(1)).getValue();
                });
        thumbprint = thumbprintHolder[0];
        thumbprintAlg = thumbprintAlgHolder[0];
        if (thumbprint != null && thumbprintAlg.equals(CoseAlgorithm.COSE_SHA_256)) {
            // Per RFC 9360, SHA-256 is supported at minimum; further algorithms may be added later
            try {
                if (Arrays.equals(CoseSignature.getThumbprint(cert), thumbprint)) {
                    validated = true;
                }
            } catch (Exception e) {
                LOGGER.warn("Embedded thumbprint failed to validate");
            }
        }
        return validated;
    }

    /**
     * Retrieves the embedded cert fom the ToBeVerified object (see RFC 9360).
     * Also validates the cert against the included thumbprint.
     *
     * @param toBeVerified the ToBeVerified object
     * @return Certificate containing the embedded signing cert, or null if invalid.
     */
    public X509Certificate getEmbeddedCert(final byte[] toBeVerified) {
        CBORItemList coseSignature;
        try {
            coseSignature = (CBORItemList) new CBORDecoder(toBeVerified, 0, toBeVerified.length).next();
        } catch (Exception e) {
            return null;
        }
        COSEProtectedHeader pHeader = null;
        try {
            pHeader = COSEProtectedHeader.build(coseSignature.getItems().get(1));
        } catch (Exception e) {
            return null;
        }
        // Retrieve cert
        if (pHeader.getX5Chain() == null) {
            return null;
        }
        X509Certificate cert = pHeader.getX5Chain().get(0);
        // Retrieve thumbprint and validate
        boolean validated = validateThumbprint(cert, pHeader);

        if (validated) {
            return cert;
        }
        return null;
    }

    /**
     * Returns a copy of the toBeSigned byte array.
     *
     * @return a defensive copy of toBeSigned
     */
    public byte[] getToBeSigned() {
        return toBeSigned.clone();
    }

    /**
     * Sets the toBeSigned byte array using a defensive copy.
     *
     * @param toBeSigned the byte array to set
     */
    public void setToBeSigned(final byte[] toBeSigned) {
        this.toBeSigned = toBeSigned.clone();
    }

    /**
     * Returns a copy of the payload byte array.
     *
     * @return a defensive copy of payload
     */
    public byte[] getPayload() {
        return payload.clone();
    }

    /**
     * Sets the payload byte array using a defensive copy.
     *
     * @param payload the byte array to set
     */
    public void setPayload(final byte[] payload) {
        this.payload = payload.clone();
    }

    /**
     * Returns a copy of the signature byte array.
     *
     * @return a defensive copy of signature
     */
    public byte[] getSignature() {
        return signature.clone();
    }

    /**
     * Sets the signature byte array using a defensive copy.
     *
     * @param signature the byte array to set
     */
    public void setSignature(final byte[] signature) {
        this.signature = signature.clone();
    }

    /**
     * Returns a copy of the keyId byte array.
     *
     * @return a defensive copy of keyId
     */
    public byte[] getKeyId() {
        return keyId.clone();
    }

    /**
     * Sets the keyId byte array using a defensive copy.
     *
     * @param keyId the byte array to set
     */
    public void setKeyId(final byte[] keyId) {
        this.keyId = keyId.clone();
    }

    /**
     * Returns a copy of the protectedHeaders byte array.
     *
     * @return a defensive copy of protectedHeaders
     */
    public byte[] getProtectedHeaders() {
        return protectedHeaders.clone();
    }

    /**
     * Sets the protectedHeaders byte array using a defensive copy.
     *
     * @param protectedHeaders the byte array to set
     */
    public void setProtectedHeaders(final byte[] protectedHeaders) {
        this.protectedHeaders = protectedHeaders.clone();
    }
}
