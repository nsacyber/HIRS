package hirs.attestationca.persist.util;

import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateVariables;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Helper class that provides various utility methods for handling credential-related tasks.
 */
@Slf4j
public final class CredentialHelper {

    /**
     * Private constructor was created to silence checkstyle error.
     */
    private CredentialHelper() {
    }

    /**
     * Converts a set of {@link CertificateAuthorityCredential} certificates to a list of
     * {@link X509Certificate} Certificates.
     *
     * @param certificateAuthorityCredentials Set of {@link CertificateAuthorityCredential} certificates
     *                                        to convert
     * @return list of {@link X509Certificate} certificates
     * @throws IOException if any issues arise attempting to convert the list of certificate
     *                     authority credentials to X509 certificates
     */
    public static List<X509Certificate> convertCACsToX509Certificates(
            final Set<CertificateAuthorityCredential> certificateAuthorityCredentials)
            throws IOException {
        List<X509Certificate> certs = new ArrayList<>(certificateAuthorityCredentials.size());
        for (CertificateAuthorityCredential cac : certificateAuthorityCredentials) {
            certs.add(cac.getX509Certificate());
        }
        return certs;
    }

    /**
     * Small method to check if the certificate is a PEM.
     *
     * @param possiblePEM header information
     * @return true if the provided string is a PEM.
     */
    public static boolean isPEM(final String possiblePEM) {
        return possiblePEM.contains(CertificateVariables.PEM_HEADER)
                || possiblePEM.contains(CertificateVariables.PEM_ATTRIBUTE_HEADER);
    }

    /**
     * Small method to check if there are multi pem files.
     *
     * @param possiblePEM header information
     * @return true if the provided string is a Multi-PEM.
     */
    public static boolean isMultiPEM(final String possiblePEM) {
        boolean multiPem = false;
        int iniIndex = possiblePEM.indexOf(CertificateVariables.PEM_HEADER);
        if (iniIndex >= 0) {
            iniIndex = possiblePEM.indexOf(CertificateVariables.PEM_HEADER,
                    iniIndex + CertificateVariables.PEM_HEADER.length());
            if (iniIndex > 1) {
                multiPem = true;
            }
        }
        return multiPem;
    }

    /**
     * Method to remove header footer information from PEM. Only the first complete PEM block is
     * decoded; any text before or after it, and any additional PEM blocks, are ignored. If the
     * content contains an X.509 certificate block, the first one is used. Otherwise the first
     * attribute certificate block is used.
     *
     * @param pemFile string representation of the file
     * @return a cleaned up raw byte object
     */
    public static byte[] stripPemHeaderFooter(final String pemFile) {
        String pemBody = extractFirstPemBody(pemFile,
                CertificateVariables.PEM_HEADER, CertificateVariables.PEM_FOOTER);
        if (pemBody == null) {
            pemBody = extractFirstPemBody(pemFile,
                    CertificateVariables.PEM_ATTRIBUTE_HEADER, CertificateVariables.PEM_ATTRIBUTE_FOOTER);
        }
        if (pemBody == null) {
            // no complete PEM block was found; remove any markers and decode what remains
            pemBody = pemFile.replace(CertificateVariables.PEM_HEADER, "")
                    .replace(CertificateVariables.PEM_FOOTER, "")
                    .replace(CertificateVariables.PEM_ATTRIBUTE_HEADER, "")
                    .replace(CertificateVariables.PEM_ATTRIBUTE_FOOTER, "");
        }
        return Base64.decode(pemBody.replaceAll("\\s", ""));
    }

    /**
     * Returns the base64 body (between the header and its matching footer) of the first PEM
     * block of the given type.
     *
     * @param pemFile string representation of the file
     * @param header  PEM header marking the start of the block
     * @param footer  PEM footer marking the end of the block
     * @return the body of the first complete block, or null if there is no complete block
     */
    private static String extractFirstPemBody(final String pemFile, final String header,
                                              final String footer) {
        int beginIndex = pemFile.indexOf(header);
        if (beginIndex < 0) {
            return null;
        }
        beginIndex += header.length();
        int endIndex = pemFile.indexOf(footer, beginIndex);
        if (endIndex < 0) {
            return null;
        }
        return pemFile.substring(beginIndex, endIndex);
    }

    /**
     * Isolates the attribute certificate block from PEM content.
     *
     * @param certificateBytes the raw contents of a certificate file
     * @return only the first attribute certificate PEM block (header through footer) if the
     * content is PEM containing one; otherwise the original bytes unchanged
     */
    public static byte[] isolateAttributeCertificatePem(final byte[] certificateBytes) {
        if (certificateBytes == null) {
            return null;
        }
        String possiblePem = new String(certificateBytes, StandardCharsets.UTF_8);
        int beginIndex = possiblePem.indexOf(CertificateVariables.PEM_ATTRIBUTE_HEADER);
        if (beginIndex < 0) {
            return certificateBytes;
        }
        int endIndex = possiblePem.indexOf(CertificateVariables.PEM_ATTRIBUTE_FOOTER, beginIndex);
        if (endIndex < 0) {
            return certificateBytes;
        }
        endIndex += CertificateVariables.PEM_ATTRIBUTE_FOOTER.length();
        return possiblePem.substring(beginIndex, endIndex).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * The method is used to remove unwanted spaces and other artifacts from the certificate.
     *
     * @param certificateBytes raw byte form
     * @return a cleaned up byte form
     */
    public static byte[] trimCertificate(final byte[] certificateBytes) {
        int certificateStart = 0;
        int certificateLength = 0;
        ByteBuffer certificateByteBuffer = ByteBuffer.wrap(certificateBytes);

        StringBuilder malformedCertStringBuilder = new StringBuilder(
                CertificateVariables.MALFORMED_CERT_MESSAGE);
        while (certificateByteBuffer.hasRemaining()) {
            // Check if there isn't an ASN.1 structure in the provided bytes
            if (certificateByteBuffer.remaining() <= 2) {
                throw new IllegalArgumentException(malformedCertStringBuilder
                        .append(" No certificate length field could be found.").toString());
            }

            // Look for first ASN.1 Sequence marked by the two bytes (0x30) and (0x82)
            // The check advances our position in the ByteBuffer by one byte
            int currentPosition = certificateByteBuffer.position();
            final byte byte1 = (byte) 0x30;
            final byte byte2 = (byte) 0x82;
            if (certificateByteBuffer.get() == byte1
                    && certificateByteBuffer.get(currentPosition + 1) == byte2) {
                // Check if we have anything more in the buffer than an ASN.1 Sequence header
                final int minByteBufferRemaining = 3;
                if (certificateByteBuffer.remaining() <= minByteBufferRemaining) {
                    throw new IllegalArgumentException(malformedCertStringBuilder
                            .append(" Certificate is nothing more than ASN.1 Sequence.")
                            .toString());
                }
                // Mark the start of the first ASN.1 Sequence / Certificate Body
                certificateStart = currentPosition;

                // Parse the length as the 2-bytes following the start of the ASN.1 Sequence
                certificateLength = Short.toUnsignedInt(
                        certificateByteBuffer.getShort(currentPosition + 2));
                // Add the 4 bytes that comprise the start of the ASN.1 Sequence and the length
                final int startOfASN1Bytes = 4;
                certificateLength += startOfASN1Bytes;
                break;
            }
        }

        if (certificateStart + certificateLength > certificateBytes.length) {
            throw new IllegalArgumentException(malformedCertStringBuilder
                    .append(" Value of certificate length field extends beyond length")
                    .append(" of provided certificate.").toString());
        }
        // Return bytes representing the main certificate body
        return Arrays.copyOfRange(certificateBytes, certificateStart,
                certificateStart + certificateLength);
    }

    /**
     * Return the string associated with the boolean slot.
     *
     * @param bit associated with the location in the array.
     * @return string value of the bit set.
     */
    public static String getKeyUsageString(final int bit) {
        String tempStr = "";

        switch (bit) {
            case CertificateVariables.KEY_USAGE_BIT0:
                tempStr = String.format("%s%n", CertificateVariables.KEY_USAGE_DS);
                break;
            case CertificateVariables.KEY_USAGE_BIT1:
                tempStr = String.format("%s%n", CertificateVariables.KEY_USAGE_NR);
                break;
            case CertificateVariables.KEY_USAGE_BIT2:
                tempStr = String.format("%s%n", CertificateVariables.KEY_USAGE_KE);
                break;
            case CertificateVariables.KEY_USAGE_BIT3:
                tempStr = String.format("%s%n", CertificateVariables.KEY_USAGE_DE);
                break;
            case CertificateVariables.KEY_USAGE_BIT4:
                tempStr = String.format("%s%n", CertificateVariables.KEY_USAGE_KA);
                break;
            case CertificateVariables.KEY_USAGE_BIT5:
                tempStr = String.format("%s%n", CertificateVariables.KEY_USAGE_KC);
                break;
            case CertificateVariables.KEY_USAGE_BIT6:
                tempStr = String.format("%s%n", CertificateVariables.KEY_USAGE_CS);
                break;
            case CertificateVariables.KEY_USAGE_BIT7:
                tempStr = String.format("%s%n", CertificateVariables.KEY_USAGE_EO);
                break;
            case CertificateVariables.KEY_USAGE_BIT8:
                tempStr = String.format("%s%n", CertificateVariables.KEY_USAGE_DO);
                break;
            default:
                break;
        }

        return tempStr;
    }

    /**
     * This method is to take the DNs from certificates and sort them in an order
     * that will be used to lookup issuer certificates.
     *
     * @param distinguishedName the original DN string.
     * @return a modified string of sorted DNs
     */
    public static String parseSortDNs(final String distinguishedName) {
        StringBuilder sb = new StringBuilder();

        if (distinguishedName == null || distinguishedName.isEmpty()) {
            sb.append("BLANK");
        } else {
            sb.append(sortParsedDistinguishedName(distinguishedName));
        }

        return sb.toString();
    }

    private static String sortParsedDistinguishedName(final String distinguishedName) {
        String dnsString;
        List<String> dnValArray = new ArrayList<>();

        try {
            X500Name x500Name = new X500Name(distinguishedName.trim());
            for (RDN rdn : x500Name.getRDNs()) {
                dnValArray.add(new X500Name(new RDN[]{rdn}).toString().toLowerCase());
            }
        } catch (IllegalArgumentException iaEx) {
            log.error("Unable to parse distinguished name for sorting: {}", distinguishedName, iaEx);
            dnsString = distinguishedName.trim();
            dnsString = dnsString.toLowerCase();
            dnValArray = Arrays.asList(dnsString.split(","));
        }

        Collections.sort(dnValArray);
        ListIterator<String> dnListIter = dnValArray.listIterator();
        StringBuilder sb = new StringBuilder();
        while (dnListIter.hasNext()) {
            sb.append(dnListIter.next());
            if (dnListIter.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
