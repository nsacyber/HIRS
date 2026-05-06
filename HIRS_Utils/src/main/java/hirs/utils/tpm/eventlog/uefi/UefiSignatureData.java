package hirs.utils.tpm.eventlog.uefi;

import hirs.utils.HexUtils;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Class for processing either
 * <ol>
 * <li>The contents of a Secure Boot PK, KEK, DB or DBX contents,
 * used for EFIVariables associated with Secure Boot,
 * as defined by Section 32.4.1 Signature Database from the UEFI 2.8 specification
 * <ul>
 * <li>EV_EFI_VARIABLE_DRIVER_CONFIG</li>
 * <li>EV_EFI_VARIABLE_AUTHORITY</li>
 * </ul></li>
 * <li>The contents of an SPDM devdb,
 * used for SPDM Device Policy or Device Authority, whose data is an EFIVariable
 * <ul>
 * <li>EFIVariable data for EV_EFI_SPDM_DEVICE_POLICY: UefiSignatureList</li>
 * <li>EFIVariable data for EV_EFI_SPDM_DEVICE_AUTHORITY : UefiSignatureData only</li>
 * </ul>
 * as defined by PFP v1.06 Rev52, Section 10.4
 * </li>
 * </ol>
 * <p>
 * typedef struct _EFI_SIGNATURE_DATA {
 * <pre>    EFI_GUID    SignatureOwner;</pre>
 * <pre>    UINT8       SignatureData[...];</pre>
 * } EFI_SIGNATURE_DATA;
 * <p>
 * However page 1729 0f UEFI 2.8 implies that SignatureListType of EFI_CERT_SHA256_GUID
 * will contain the "the SHA-256 hash of the binary".
 * So the Signature Data depends upon the Signature Type from the EFI Signature List.
 * </p>
 */
@Log4j2
public class UefiSignatureData {
    /**
     * UEFI Certificate GUID.
     */
    private final byte[] guid = new byte[UefiConstants.SIZE_16];
    /**
     * UEFI Signature data.
     */
    private byte[] sigData = null;
    /**
     * UEFI Certificate object .
     */
    @Getter
    private UefiX509Cert cert = null;
    /**
     * UEFI Certificate GUID.
     */
    @Getter
    private UefiGuid efiVarGuid = null;
    /**
     * UEFI Signature type.
     */
    @Getter
    private UefiGuid signatureType = null;
    /**
     * UEFI Signature validity.
     */
    @Getter
    private boolean valid = false;
    /**
     * UEFI Certificate SHA256 hash.
     */
    private final byte[] binaryHash = new byte[UefiConstants.SIZE_32];
    /**
     * UEFI Signature data error status.
     */
    @Getter
    private String errorStatus = "";

    /**
     * UefiSignatureData constructor.
     *
     * @param inputStream The Signature data.
     * @param sigType     UEFI defined signature type.
     * @throws java.io.IOException                     if there's a problem reading the input stream.
     * @throws java.security.cert.CertificateException If there's a problem parsing the X509 certificate.
     * @throws java.security.NoSuchAlgorithmException  if there's a problem hashing the certificate.
     */
    UefiSignatureData(final ByteArrayInputStream inputStream, final UefiGuid sigType)
            throws IOException {
        signatureType = sigType;
        // UEFI spec section 32.5.3.3 states that SignatureListType of EFI_CERT_SHA256_GUID
        // only contains a hash, not a cert
        if (sigType.getVendorTableReference().equals("EFI_CERT_SHA256_GUID")) {
            inputStream.read(guid);
            efiVarGuid = new UefiGuid(guid);
            // Should be a SHA256 hash of the "binary"
            inputStream.read(binaryHash);
        } else if (sigType.getVendorTableReference().equals("EFI_CERT_X509_GUID")) {
            inputStream.read(guid);
            efiVarGuid = new UefiGuid(guid);
            processC509Cert(inputStream);
        } else {
            errorStatus = "Signature List Type has either an unknown GUID or a type that hasn't been implemented yet";
            return;
        }

        valid = true;
    }

    /**
     * Default EFISignatureData Constructor.
     *
     * @param data byte array of the EFISignatureData to process
     */
    UefiSignatureData(final byte[] data) {
        System.arraycopy(data, 0, guid, 0, UefiConstants.SIZE_16);
        sigData = new byte[data.length - UefiConstants.SIZE_16];
        System.arraycopy(data, UefiConstants.OFFSET_16, sigData, 0,
                data.length - UefiConstants.SIZE_16);
        try {
            cert = new UefiX509Cert(sigData);
        } catch (CertificateException | NoSuchAlgorithmException e) {
            errorStatus = "\n   **** UefiSignatureData Certificate Issue ****: " + e.getMessage();
            log.warn("UefiSignatureData Certificate Issue: {}", e.getMessage());
        }
        efiVarGuid = new UefiGuid(guid);
    }

    /**
     * Processes an x509 Cert used by secure DB or DBx.
     *
     * @param inputStream x509 certificate data.
     * @throws java.io.IOException if there's a problem reading the data.
     */
    private void processC509Cert(final ByteArrayInputStream inputStream)
            throws IOException {

        // Read in Type and Length separately so we calculate the rest of the cert size
        byte[] certType = new byte[UefiConstants.SIZE_2];
        inputStream.read(certType);
        byte[] certLength = new byte[UefiConstants.SIZE_2];
        inputStream.read(certLength);
        int certDataLength = new BigInteger(certLength).intValue();
        byte[] certData = new byte[certDataLength];
        inputStream.read(certData);
        byte[] certBlob = new byte[certDataLength + UefiConstants.SIZE_4];
        System.arraycopy(certType, 0, certBlob, 0, 2);
        System.arraycopy(certLength, 0, certBlob, 2, 2);
        System.arraycopy(certData, 0, certBlob, UefiConstants.OFFSET_4, certDataLength);
        try {
            cert = new UefiX509Cert(certBlob);
        } catch (CertificateException | NoSuchAlgorithmException e) {
            errorStatus = "\n   **** UefiSignatureData Certificate Issue ****: " + e.getMessage();
            log.warn("UefiSignatureData Certificate Issue: {}", e.getMessage());
        }
    }

    /**
     * Provides a description of the fields within the EFI Signature Data.
     *
     * @return X509Cert human readable description.
     */
    public String toString() {
        String sigInfo = "";
        if (!valid) {
            sigInfo = errorStatus;
        } else {
            if (signatureType.getVendorTableReference().equals("EFI_CERT_SHA256_GUID")) {
                sigInfo += "    UEFI Signature Owner = " + efiVarGuid.toString() + "\n";
                sigInfo += "      Binary Hash = " + HexUtils.byteArrayToHexString(binaryHash) + "\n";
            } else {
                sigInfo += "    UEFI Signature Owner = " + efiVarGuid.toString() + "\n";
                if (errorStatus.isEmpty()) {
                    sigInfo += cert.toString();
                } else {
                    sigInfo += errorStatus;
                }
            }
        }
        return sigInfo;
    }
}
