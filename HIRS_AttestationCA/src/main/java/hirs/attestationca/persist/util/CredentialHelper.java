package hirs.attestationca.persist.util;

import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateVariables;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.Base64;

import java.io.IOException;
import java.nio.ByteBuffer;
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
@Log4j2
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
     * Method to remove header footer information from PEM.
     *
     * @param pemFile string representation of the file
     * @return a cleaned up raw byte object
     */
    public static byte[] stripPemHeaderFooter(final String pemFile) {
        String strippedFile;
        strippedFile = pemFile.replace(CertificateVariables.PEM_HEADER, "");
        int keyFooterPos = strippedFile.indexOf(CertificateVariables.PEM_FOOTER);
        if (keyFooterPos >= 0) {
            strippedFile = strippedFile.substring(0, keyFooterPos);
        }
        strippedFile = strippedFile.replace(CertificateVariables.PEM_ATTRIBUTE_HEADER, "");
        int attrFooterPos = strippedFile.indexOf(CertificateVariables.PEM_ATTRIBUTE_FOOTER);
        if (attrFooterPos >= 0) {
            strippedFile = strippedFile.substring(0, attrFooterPos);
        }
        return Base64.decode(strippedFile);
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
     * that will be used to lookup issuer certificates.  This will not be stored in
     * the certificate, just the DB for lookup.
     *
     * @param distinguishedName the original DN string.
     * @return a modified string of sorted DNs
     */
    public static String parseSortDNs(final String distinguishedName) {
        StringBuilder sb = new StringBuilder();
        String dnsString;

        if (distinguishedName == null || distinguishedName.isEmpty()) {
            sb.append("BLANK");
        } else {
            dnsString = distinguishedName.trim();
            dnsString = dnsString.toLowerCase();
            List<String> dnValArray = Arrays.asList(dnsString.split(","));
            Collections.sort(dnValArray);
            ListIterator<String> dnListIter = dnValArray.listIterator();
            while (dnListIter.hasNext()) {
                sb.append(dnListIter.next());
                if (dnListIter.hasNext()) {
                    sb.append(",");
                }
            }
        }

        return sb.toString();
    }
}
