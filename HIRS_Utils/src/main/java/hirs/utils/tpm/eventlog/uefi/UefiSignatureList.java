package hirs.utils.tpm.eventlog.uefi;

import hirs.utils.HexUtils;
import lombok.Getter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Class for processing either
 * <ol>
 * <li>The contents of a Secure Boot PK, KEK, DB or DBX contents,
 * used for EFIVariables associated with Secure Boot,
 * as defined by Section 32.4.1 Signature Database from the UEFI 2.8 specification
 * </li>
 * <li>The contents of an SPDM devdb,
 * used for SPDM Device Policy, whose data is an EFIVariable
 * as defined by PFP v1.06 Rev52, Section 10.4
 * </ol>
 * <p>
 * An EFI Signature List is actually a list of Certificates used to verify a Signature.
 * This is mainly found in PCR[7] UEFI variables for either the
 * <ul>
 * <li>Secure Boot PK, KEK, Db and DBx variables</li>
 * <li>or the SPDM devdb variable (under EV_EFI_SPDM_DEVICE_POLICY)</li>
 * </ul>
 * <p>
 * typedef struct _EFI_SIGNATURE_LIST {
 * <pre>    EFI_GUID            SignatureType;</pre>
 * <pre>    UINT32              SignatureListSize;</pre>
 * <pre>    UINT32              SignatureHeaderSize;</pre>
 * <pre>    UINT32              SignatureSize;</pre>
 * <pre>    // UINT8               SignatureHeader[SignatureHeaderSize];</pre>
 * <pre>    // EFI_SIGNATURE_DATA  Signatures[...][SignatureSize];</pre>
 * } EFI_SIGNATURE_LIST;
 * </p>
 * <p>
 * SignatureListHeader (contents common to any Signature Type)
 * <ul>
 * <li>SignatureType (SHA256, X509)</li>
 * <li>SignatureListSize</li>
 * <li>SignatureHeaderSize</li>
 * <li>SignatureSize</li>
 * </ul></p>
 * SignatureHeader (contents depend on the SignatureType)
 * <ul>
 * <li>The format of this header is specified by the SignatureType (SHA256, X509).</li>
 * </ul>
 * Signatures[][] is an array of signatures.
 * <ul>
 * <li>Each signature is SignatureSize bytes in length.</li>
 * <li>The format of the signature is defined by SignatureType (SHA256, X509).</li>
 * </ul>
 * <pre>
 * /                             / |-------------------------| ------- SignatureType
 * /                            /  | Signature List Header   |         SignatureListSize
 * |---------------------|     /   |-------------------------|\        SignatureHeaderSize
 * | Signature List #0   |    /    |    Signature Header     | \ _____ SignatureSize
 * |                     |   /     |-------------------------|
 * |---------------------|  /      |      Signature #0       |
 * | Signature List #1   | /       |-------------------------|
 * |---------------------|/        |      Signature #1       |  --> each Signature is
 * | Signature List #2   |         |-------------------------|      1 UefiSignatureData
 * |                     |         |      Signature #2       |      (1 cert or hash)
 * |                     |         |-------------------------|
 * |---------------------|         |           ...           |
 * |                     | \       |                         |
 * |                     |   \     |-------------------------|
 * |                     |     \   |      Signature #n       |
 * |                     |       \ |-------------------------|
 * </pre>
 */
public class UefiSignatureList {
    /**
     * Array List of Signature found in the list.
     */
    private final ArrayList<UefiSignatureData> sigList = new ArrayList<>();
    /**
     * Size of the signature list.
     */
    private int listSize = 0;
    /**
     * Size of a signature.
     */
    private int signatureSize = 0;
    /**
     * Signature data.
     */
    private byte[] sigData = null;
    /**
     * Number of Items in the list.
     */
    @Getter
    private int numberOfCerts = 0;
    /**
     * Signature validity.
     */
    @Getter
    private boolean signatureTypeValid = false;
    /**
     * Data validity.
     */
    private boolean dataValid = true;
    /**
     * Current status of Signature List data.
     */
    private String dataInvalidStatus = "Signature List data validity is undetermined yet";
    /**
     * Input Stream for processing.
     */
    private ByteArrayInputStream efiSigDataIS = null;

    /**
     * Type of signature.
     */
    private UefiGuid signatureType = null;

//    /**
//     * Track status of vendor-table.json file.
//     */
//    @Getter
//    private String guidTableFileStatus = FILESTATUS_NOT_ACCESSIBLE;

//    /**
//     * UefiSignatureList constructor.
//     *
//     * @param list byte array holding the signature list.
//     * @throws java.security.cert.CertificateException If there a problem parsing the X509 certificate.
//     * @throws java.security.NoSuchAlgorithmException  if there's a problem hashing the certificate.
//     * @throws java.io.IOException                     If there's a problem parsing the signature data.
//     */
//    UefiSignatureList(final byte[] list)
//            throws CertificateException, NoSuchAlgorithmException, IOException {
//
//        byte[] guid = new byte[UefiConstants.SIZE_16];
//        System.arraycopy(list, 0, guid, 0, UefiConstants.SIZE_16);
//        signatureType = new UefiGuid(guid);
//        guidTableFileStatus = signatureType.getGuidTableFileStatus();
//
//        byte[] lSize = new byte[UefiConstants.SIZE_4];
//        System.arraycopy(list, UefiConstants.OFFSET_16, lSize, 0, UefiConstants.SIZE_4);
//        listSize = HexUtils.leReverseInt(lSize);
//
//        byte[] hSize = new byte[UefiConstants.SIZE_4];
//        System.arraycopy(list, UefiConstants.OFFSET_20, hSize, 0, UefiConstants.SIZE_4);
//
//        byte[] sSize = new byte[UefiConstants.SIZE_4];
//        System.arraycopy(list, UefiConstants.OFFSET_24, sSize, 0, UefiConstants.SIZE_4);
//        signatureSize = HexUtils.leReverseInt(sSize);
//
//        sigData = new byte[signatureSize];
//        System.arraycopy(list, UefiConstants.OFFSET_28, sigData, 0, signatureSize);
//        processSignatureList(sigData);
//    }

    /**
     * EFI Signature list constructor.
     *
     * @param lists ByteArrayInputStream containing an EFI Signature list.
     * @throws IOException              If there's a problem in reading he input stream.
     * @throws NoSuchAlgorithmException if there's a problem hashing the certificate.
     */
    UefiSignatureList(final ByteArrayInputStream lists) throws IOException, NoSuchAlgorithmException {
        byte[] guid = new byte[UefiConstants.SIZE_16];
        lists.read(guid);
        signatureType = new UefiGuid(guid);
//        guidTableFileStatus = signatureType.getGuidTableFileStatus();

        // if signatureType is invalid, don't even process any of the data
        // however, if signatureType is valid, but some of the data later on is invalid, that will
        // be caught when UefiSignatureData is processed
        if (!isValidSigListGUID(signatureType)) {
            signatureTypeValid = false;
        } else { // valid SigData Processing
            signatureTypeValid = true;

            byte[] lSize = new byte[UefiConstants.SIZE_4];      // signature list size
            lists.read(lSize);
            listSize = HexUtils.leReverseInt(lSize);

            byte[] hSize = new byte[UefiConstants.SIZE_4];      // signature header size
            lists.read(hSize);

            byte[] sSize = new byte[UefiConstants.SIZE_4];      // signature size
            lists.read(sSize);
            signatureSize = listSize - UefiConstants.SIZE_28;
            sigData = new byte[signatureSize];
            lists.read(sigData);
            processSignatureList(sigData);
        }
    }

    /**
     * Method for processing the data in an EFI SignatureList (ex. can be one or more X509 certs)
     *
     * @param efiSigData Byte array holding the SignatureList data
     * @throws NoSuchAlgorithmException if there's a problem hashing the certificate.
     * @throws IOException              If there's a problem parsing the signature data.
     */
    private void processSignatureList(final byte[] efiSigData) throws NoSuchAlgorithmException, IOException {
        efiSigDataIS = new ByteArrayInputStream(efiSigData);
        while (efiSigDataIS.available() > 0) {
            UefiSignatureData tmpSigData = new UefiSignatureData(efiSigDataIS, signatureType);
            if (!tmpSigData.isValid()) {
                dataValid = false;
                dataInvalidStatus = tmpSigData.getErrorStatus();
                break;
            }
            sigList.add(tmpSigData);
            numberOfCerts++;
        }
    }

    /**
     * Checks to see if GUID is listed on page 1729 of UEFI spec version 2.8.
     *
     * @param guid GUID of the has algorithm.
     * @return true if the GUID is a valid GUID for Signature List Type, false if not.
     */
    public boolean isValidSigListGUID(final UefiGuid guid) {
        return switch (guid.getVendorTableReference()) {
            case "EFI_CERT_SHA256_GUID", "EFI_CERT_X509_SHA256", "EFI_CERT_X509_SHA384",
                 "EFI_CERT_X509_SHA512", "EFI_CERT_X509_GUID" -> true;
            default -> false;
        };
    }

    /**
     * Provides a description of the fields within the EFI Signature Data field.
     * Which is essentially a list of X509 certificates.
     *
     * @return human readable description.
     */
    public String toString() {
        StringBuilder sigInfo = new StringBuilder();

        if (!signatureTypeValid) {
            sigInfo.append("   *** Unknown UEFI Signature Type encountered:\n"
                    + "       " + signatureType.toString() + "\n");
        } else {
            sigInfo.append("   UEFI Signature List Type = " + signatureType.toString() + "\n");
            sigInfo.append("   Number of Certs or Hashes in UEFI Signature List = " + numberOfCerts + "\n");

            int certOrHashCnt = 1;
            for (int i = 0; i < sigList.size(); i++) {
                sigInfo.append("   Cert or Hash # " + certOrHashCnt++ + " of "
                        + numberOfCerts + ": ------------------\n");
                UefiSignatureData certData = sigList.get(i);
                sigInfo.append(certData.toString());
            }
            if (!dataValid) {
                sigInfo.append("   *** Invalid UEFI Signature data encountered: " + dataInvalidStatus + "\n");
            }
        }
        return sigInfo.toString();
    }
}
