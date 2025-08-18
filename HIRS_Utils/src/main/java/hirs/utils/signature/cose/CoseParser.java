package hirs.utils.signature.cose;

import com.authlete.cbor.CBORItem;
import com.authlete.cbor.CBORDecoder;
import com.authlete.cose.COSESign1;
import com.authlete.cose.COSEUnprotectedHeader;
import com.authlete.cose.COSEProtectedHeader;
import com.authlete.cose.COSEException;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.CoRim;
import hirs.utils.rim.unsignedRim.cbor.ietfCorim.CoRimParser;
import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.Coswid;
import hirs.utils.signature.cose.Cbor.CborTagProcessor;
import hirs.utils.rim.unsignedRim.cbor.tcgCompRimCoswid.TcgCompRimCoswidParser;
import hirs.utils.signature.cose.Cbor.CborBstr;
import lombok.Getter;
import lombok.Setter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Parser for COSE Formatted data per RFC 9052.
 */
public class CoseParser {
    @Setter
    @Getter
    private int coseTag = 0;
    private byte[] toBeSigned = null;
    private byte[] payload = null;
    private byte[] signature = null;
    @Setter
    @Getter
    private String algIdentifier = "";
    private byte[] keyIdBytes = null;
    @Setter
    @Getter
    private String keyIdentifier = "";
    private CoseHeaderProtected coseHeaderP = null;
    private CoseHeaderUnprotected coseHeaderU = null;
    @Getter
    private String contentType = "";
    private byte[] protectedHeaders = null;
    @Setter
    @Getter
    private String toStringCborDiag = "";
    private CborTagProcessor cborTag = null;

    /**
     * Parser constructor to fill class variables.
     * Algorithm should only be in protected header or from externally supplied data.
     * However, if alg is in both unprotected and protected headers, the protected header will be used.
     * If alg is not found, an error will be thrown.
     * Key Identifier (kid) should be in the protected header but can be in the unprotected header,
     * or not provided.
     * @param coseData Byte array holding the COSE data
     */
    public CoseParser(final byte[] coseData) {

        CBORDecoder cborDecoder = new CBORDecoder(coseData);
        COSESign1 sign1 = null;
        String process = "Processing Cose toBeSigned for verification: ";
        String status = "Parsing Cose Tag, expecting tag 18 (cose-sign1):";
        CborTagProcessor ctp = new CborTagProcessor(coseData);
        coseTag = ctp.getTagId();
        if ((!ctp.isCose()) && (!ctp.isCorim())) {
            throw new RuntimeException("Error parsing COSE signature: COSE tag of " + coseTag
                    + " found but only cose-sign1 (18) is supported");
        }
        try {
            status = "Decoding COSE object";
            CBORItem coseObjectData = cborDecoder.next();
            toStringCborDiag = coseObjectData.toString();

            ArrayList<Object> parsedata = (ArrayList) coseObjectData.parse();
            COSESign1 signOne = COSESign1.build(parsedata);
            status = "Decoding COSE Protected Header";
            COSEProtectedHeader pheader = signOne.getProtectedHeader();
            status = "Decoding COSE Unprotected Header";
            COSEUnprotectedHeader uheader = signOne.getUnprotectedHeader();
            status = "Checking COSE headers for required attributes";

            // parse COSE protected and unprotected headers
            coseHeaderP = new CoseHeaderProtected(pheader);
            coseHeaderU = new CoseHeaderUnprotected(uheader);

            if (!coseHeaderP.getAlgIdentifier().isEmpty()) {
                algIdentifier = coseHeaderP.getAlgIdentifier();
            } else if (!coseHeaderU.getAlgIdentifier().isEmpty()) {
                algIdentifier = coseHeaderU.getAlgIdentifier();
            } else {
                throw new RuntimeException("Algorithm ID required but not found in COSE header");
            }

            // kid can be in protected or unprotected header; if in both, protected one will be used
            if (!coseHeaderP.getKeyIdentifier().isEmpty()) {
                keyIdentifier = coseHeaderP.getKeyIdentifier();
            } else if (!coseHeaderU.getKeyIdentifier().isEmpty()) {
                keyIdentifier = coseHeaderU.getKeyIdentifier();
            }
            // content type can be in protected or unprotected header; if in both, protected one will be used
            if (!coseHeaderP.getContentType().isEmpty()) {
                contentType = coseHeaderP.getContentType();
            } else if (!coseHeaderU.getContentType().isEmpty()) {
                contentType = coseHeaderU.getContentType();
            }
            status = "Retrieving signature from COSE object";
            signature = signOne.getSignature().getValue();
            status = "Retrieving payload from COSE object";
            byte[] encodedPayload = signOne.getPayload().encode();
            payload = CborBstr.removeByteStringIfPresent(encodedPayload);
            checkForTag(payload);
        } catch (IOException | COSEException e) {
            throw new RuntimeException(process + status + " :" + e.getMessage());
        }
    }

    /**
     * Checks the payload for a valid tag.
     * by parsing the first byte of the payload as a tag
     * and checking for one of the supported tags by this application
     * If a supported tag is found the payload and coswid tag references are adjusted
     * @param payloadData
     * @return true if a valid tag is found
     */
    private boolean checkForTag(final byte[] payloadData) {
        boolean tagFound = false;
        CborTagProcessor tmpTag = new CborTagProcessor(payloadData);
        if (tmpTag.isTagged()) {
            cborTag = tmpTag;
            tagFound = true;
            payload = tmpTag.getContent();
        } else {
            cborTag = new CborTagProcessor();
        }
        return tagFound;
    }

    /**
     * Method to print hex data.
     * @param data byte containing hex data to be print
     * @return String containing hex representation of the data
     */
    public static String hexToString(final byte[] data) {
        StringBuilder sb2 = new StringBuilder();
        for (byte b : data) {
            sb2.append(String.format("%02X", b));
        }
        return sb2.toString();
    }

    /**
     * Looks up the COSE types defined in Table 1 of RFC 9052.
     * Also processes CoRim options for COSE.
     * @param tag the CBOR Tag (int) defined in Table 1
     * @return a String defined in Table 1 that corresponds to the tag
     */
    public String coseTagLookup(final int tag) {
        final int coseSign = 98;
        final int coseSignOne = 18;
        final int coseEncrypt = 96;
        final int coseEncrypt0 = 16;
        final int coseMac = 97;
        final int coseMac0 = 17;

        switch (tag) {
            case coseSign: return "cose-sign";
            case coseSignOne: return "cose-sign1";
            case coseEncrypt: return "cose-encrypt";
            case coseEncrypt0: return "cose-encrypt0";
            case coseMac: return "cose-mac";
            case coseMac0: return "cose-mac0";
            default: return CoRim.getTagLabel(tag);
        }
    }

    /**
     * Default toString.
     * @return default "pretty" version
     */
    public String toString()   {
        try {
            return toString("pretty");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates human-readable text from a Cose Object.
     * @param format empty (default String) or "pretty"
     * @return a formated string representation of the data in the COSE object
     */
    public String toString(final String format) throws IOException {
        String returnString = "";
        final int lineLength  = 100;
        if (format.compareToIgnoreCase("pretty") == 0) {
            returnString = "  COSE Signed object:\n";
            returnString += "  tag = " + coseTagLookup(coseTag) + "\n";
            returnString += coseHeaderP.toString("pretty");
            returnString += coseHeaderU.toString("pretty");
            returnString += "COSE Payload: " + "\n";
            if (contentType.compareToIgnoreCase("application/rim+cbor") == 0) {
                returnString += "  Processing payload as CoRim:"  + "\n";
                CoRimParser cparser = new CoRimParser(payload);
                returnString += cparser.toString();
            } else if (!cborTag.isTagged()) {
                returnString += "  Untagged Payload of length " + payload.length + " bytes found\n";
                String pdata = hexToString(payload);
                String formattedPdata = pdata.replaceAll("(.{100})", "$1\n");
                returnString += "  Payload data: \n  " + formattedPdata + "\n";
            } else {
                returnString += "  Payload tag of type " + cborTag.getTagId() + " found: \n";
                // Process tags of type we know
                if (cborTag.isCoswid()) {
                    Coswid cswid = new Coswid();
                    TcgCompRimCoswidParser cswidParser = new TcgCompRimCoswidParser(payload);
                    returnString += " " + cswidParser.toString(format);
                } else {   // Else just dump the raw data
                    returnString += "  Data found is:\n";
                    String pdata = hexToString(payload);
                    String formattedPdata = pdata.replaceAll("(.{100})", "$1\n");
                    returnString += "  Payload data: \n  " + formattedPdata + "\n";
                }
            }
            String sig = hexToString(signature);
            String formattedSig = sig.replaceAll("(.{100})", "$1\n");
            returnString += "  Signature = \n" + formattedSig;
        } else if (format.compareToIgnoreCase("cbor-diag") == 0) {
            // return Authelete defined representation of COSE protected header
            returnString = toStringCborDiag;
        }
        return returnString;
    }

    /**
     * Returns a copy of the toBeSigned bytes.
     * @return copy of toBeSigned
     */
    public byte[] getToBeSigned() {
        return toBeSigned.clone();
    }

    /**
     * Sets a copy of the toBeSigned bytes.
     * @param toBeSigned byte array to set
     */
    public void setToBeSigned(final byte[] toBeSigned) {
        this.toBeSigned = toBeSigned.clone();
    }

    /**
     * Returns a copy of the payload bytes.
     * @return copy of payload
     */
    public byte[] getPayload() {
        return payload.clone();
    }

    /**
     * Sets a copy of the payload bytes.
     * @param payload byte array to set
     */
    public void setPayload(final byte[] payload) {
        this.payload = payload.clone();
    }

    /**
     * Returns a copy of the signature bytes.
     * @return copy of signature
     */
    public byte[] getSignature() {
        return signature.clone();
    }

    /**
     * Sets a copy of the signature bytes.
     * @param signature byte array to set
     */
    public void setSignature(final byte[] signature) {
        this.signature = signature.clone();
    }

    /**
     * Returns a copy of the keyIdBytes.
     * @return copy of keyIdBytes
     */
    public byte[] getKeyIdBytes() {
        return keyIdBytes.clone();
    }

    /**
     * Sets a copy of the keyIdBytes.
     * @param keyIdBytes byte array to set
     */
    public void setKeyIdBytes(final byte[] keyIdBytes) {
        this.keyIdBytes = keyIdBytes.clone();
    }

    /**
     * Returns a copy of the protected headers.
     * @return copy of protected headers
     */
    public byte[] getProtectedHeaders() {
        return protectedHeaders.clone();
    }

    /**
     * Sets a copy of the protected headers.
     * @param protectedHeaders byte array to set
     */
    public void setProtectedHeaders(final byte[] protectedHeaders) {
        this.protectedHeaders = protectedHeaders.clone();
    }

}
