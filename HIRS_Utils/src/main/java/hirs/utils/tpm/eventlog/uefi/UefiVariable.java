package hirs.utils.tpm.eventlog.uefi;

import hirs.utils.HexUtils;
import hirs.utils.tpm.eventlog.events.EvConstants;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to process a UEFI variable within a TPM Event.
 * <pre>
 * typedef struct tdUEFI_VARIABLE_DATA{
 * &emsp;UEFI_GUID VariableName;     (16 bytes)
 * &emsp;UINT64 UnicodeNameLength;   (8 bytes)
 * &emsp;UINT64 VariableDataLength;  (8 bytes)
 * &emsp;CHAR16 UnicodeName[];
 * &emsp;INT8 VariableData[];
 * } UEFI_VARIABLE_DATA<br>
 *
 * Example:
 *
 * UEFI_VARIABLE_DATA example{
 * &emsp;"8be4df61-93ca-11d2-aa0d-00e098032b8c : EFI_Global_Variable"
 * &emsp;2
 * &emsp;973
 * &emsp;PK
 * &emsp;< UefiSignatureList >
 * }
 * </pre><br>
 * PFP 1.06 Revision 52:<br>
 * <ul>
 * <li>For Event Type EV_EFI_VARIABLE_DRIVER_CONFIG: The event field MUST contain a
 * UEFI_VARIABLE_DATA structure, including
 * the variable data, the GUID and the Unicode
 * string.</li>
 * <li>For Event Type EV_EFI_VARIABLE_AUTHORITY: The event field MUST contain a
 * UEFI_VARIABLE_DATA structure where the
 * VariableData field contains the
 * EFI_SIGNATURE_DATA value from the
 * EFI_SIGNATURE_LIST used to validate the
 * loaded image</li>
 * </ul>
 */
public class UefiVariable {

    /**
     * Event Type.
     */
    @Getter
    private int eventType = 0;
    /**
     * UEFI defined variable identifier GUID.
     */
    @Getter
    private UefiGuid variableNameGuid = null;
    /**
     * Name of the UEFI variable.
     */
    @Getter
    private String unicodeName = "";
    /**
     * UEFI variable data.
     */
    private byte[] uefiVariableData = null;
    /**
     * List of Signature lists.
     */
    private final List<UefiSignatureList> certSuperList;
    /**
     * Was able to process the Variable Data.
     */
    private boolean uefiVariableDataProcessed = false;
    /**
     * Encountered invalid UEFI Signature List.
     */
    private boolean invalidSignatureListEncountered = false;
    /**
     * Invalid UEFI Signature List.
     */
    private String invalidSignatureListStatus = "";
    /**
     * UEFI defined Boot Variable.
     */
    private UefiBootVariable bootv = null;
    /**
     * UEFI Defined boot order.
     */
    private UefiBootOrder booto = null;
    /**
     * UEFI defined secure boot.
     */
    private UefiSecureBoot sb = null;
    /**
     * Human-readable description of the data within the SPDM devdc (to be updated with more test data).
     */
//    private String spdmDevdcInfo = "";
    private String sigDataInfo = "";

    /**
     * EFIVariable constructor.
     * The UEFI_VARIABLE_DATA contains a "VariableName" field which is used to determine
     * the class used to parse the data within the "VariableData".
     *
     * @param eventTypeIn the event type
     * @param variableData byte array holding the UEFI Variable.
     * @throws java.security.NoSuchAlgorithmException if there's a problem
     *                                                hashing the certificate.
     * @throws java.io.IOException                    If there's a problem
     *                                                parsing the signature data.
     */
    public UefiVariable(final int eventTypeIn, final byte[] variableData)
            throws NoSuchAlgorithmException, IOException {

        eventType = eventTypeIn;
        certSuperList = new ArrayList<>();

        byte[] variableNameGuidBytes = new byte[UefiConstants.SIZE_16];
        byte[] unicodeNameLengthBytes = new byte[UefiConstants.SIZE_8];
        byte[] variableDataLengthBytes = new byte[UefiConstants.SIZE_8];
        int variableDataLength = 0;
        byte[] unicodeNameCharBytes = null;
        byte[] unicodeNameWithZerosBytes = null;
        byte[] unicodeNameBytes = null;

        // VariableName (GUID)
        System.arraycopy(variableData, 0, variableNameGuidBytes, 0, UefiConstants.SIZE_16);
        variableNameGuid = new UefiGuid(variableNameGuidBytes);

        // UnicodeNameLength
        System.arraycopy(variableData, UefiConstants.SIZE_16, unicodeNameLengthBytes,
                0, UefiConstants.SIZE_8);
        int unicodeNameLength = HexUtils.leReverseInt(unicodeNameLengthBytes);

        // VariableDataLength
        System.arraycopy(variableData, UefiConstants.OFFSET_24, variableDataLengthBytes,
                0, UefiConstants.SIZE_8);
        variableDataLength = HexUtils.leReverseInt(variableDataLengthBytes);

        // UnicodeName
        unicodeNameCharBytes = new byte[unicodeNameLength * UefiConstants.SIZE_2];
        System.arraycopy(variableData, UefiConstants.OFFSET_32,
                unicodeNameCharBytes, 0, unicodeNameLength * UefiConstants.SIZE_2);
        unicodeNameWithZerosBytes = UefiDevicePath.convertChar16tobyteArray(unicodeNameCharBytes);
        unicodeNameBytes = new byte[unicodeNameLength];
        System.arraycopy(unicodeNameWithZerosBytes, 0, unicodeNameBytes, 0, unicodeNameLength);
        unicodeName = new String(unicodeNameBytes, StandardCharsets.UTF_8);
        String unicodeNameAdjusted = unicodeName;
        if (unicodeName.contains("Boot00")) {
            unicodeNameAdjusted = "Boot00";
        }

        // VariableData
        uefiVariableData = new byte[variableDataLength];
        System.arraycopy(variableData, UefiConstants.OFFSET_32
                + unicodeNameLength * UefiConstants.SIZE_2, uefiVariableData, 0, variableDataLength);

        switch (eventType) {
            case EvConstants.EV_EFI_VARIABLE_DRIVER_CONFIG:
                switch (unicodeName) {
                    case "SecureBoot":
                        sb = new UefiSecureBoot(uefiVariableData);
                        uefiVariableDataProcessed = true;
                        break;
                    case "PK":
                    case "KEK":
                    case "db":
                    case "dbx":
                        processSigList(uefiVariableData);
                        uefiVariableDataProcessed = true;
                        break;
                    default:
                }
                break;
            case EvConstants.EV_EFI_VARIABLE_BOOT:
                if (unicodeName.contains("Boot00")) {
                    bootv = new UefiBootVariable(uefiVariableData);
                    uefiVariableDataProcessed = true;
                } else if (unicodeName.equals("BootOrder")) {
                    booto = new UefiBootOrder(uefiVariableData);
                    uefiVariableDataProcessed = true;
                }
                break;
            case EvConstants.EV_EFI_VARIABLE_AUTHORITY:
                if (variableNameGuid.getVendorTableReference().equals("EFI_IMAGE_SECURITY_DATABASE_GUID")) {
                    processSigDataX509(uefiVariableData);
                    uefiVariableDataProcessed = true;
                }
                break;
            case EvConstants.EV_EFI_SPDM_DEVICE_POLICY:
                processSigList(uefiVariableData);
                uefiVariableDataProcessed = true;
                break;
            case EvConstants.EV_EFI_SPDM_DEVICE_AUTHORITY:
                processSigDataX509(uefiVariableData);
                uefiVariableDataProcessed = true;
                break;
            default:
        }
    }

    /**
     * Processes the data as a list of UEFI defined Signature Lists.
     *
     * @param data the bye array holding one or more Signature Lists.
     * @throws java.security.cert.CertificateException If there's a problem
     *                                                 parsing the X509 certificate.
     * @throws java.security.NoSuchAlgorithmException  if there's a problem
     *                                                 hashing the certificate.
     * @throws java.io.IOException                     If there's a problem
     *                                                 parsing the signature data.
     */
    private void processSigList(final byte[] data)
            throws NoSuchAlgorithmException, IOException {
        ByteArrayInputStream certData = new ByteArrayInputStream(data);
        while (certData.available() > 0) {
            UefiSignatureList list;
            list = new UefiSignatureList(certData);

//            // first check if any previous list has not been able to access vendor-table.json,
//            // and if that is the case, the first comparison in the if returns false and
//            // the if statement is not executed
//            // [previous event file status = guidTableFileStatus]
//            // (ie. keep the file status to reflect that file was not accessible at some point)
//            // next, check if the new list has any status other than the default 'filesystem',
//            // and if that is the case, the 2nd comparison in the if returns true and
//            // the if statement is executed
//            // [new event file status = list.getGuidTableFileStatus()]
//            // (ie. if the new file status is not-accessible or from-code, then want to update)
//            if ((guidTableFileStatus != FILESTATUS_NOT_ACCESSIBLE)
//                    && (list.getGuidTableFileStatus() != FILESTATUS_FROM_FILESYSTEM)) {
//                guidTableFileStatus = list.getGuidTableFileStatus();
//            }

            if (!list.isSignatureTypeValid()) {
                invalidSignatureListEncountered = true;
                invalidSignatureListStatus = list.toString();
                break;
            }
            certSuperList.add(list);
        }
    }

    /**
     * Method for processing the data in an EFI Signature Data, where the data is known to be an X509 cert.
     *
     * @param efiSigData Byte array holding the SignatureData data
     * @throws java.security.cert.CertificateException If there's a problem parsing the X509 certificate.
     * @throws java.security.NoSuchAlgorithmException  if there's a problem hashing the certificate.
     * @throws java.io.IOException                     If there's a problem parsing the signature data.
     */
    private void processSigDataX509(final byte[] efiSigData)
            throws NoSuchAlgorithmException, IOException {

        ByteArrayInputStream efiSigDataIS = new ByteArrayInputStream(efiSigData);
        ArrayList<UefiSignatureData> sigList = new ArrayList<UefiSignatureData>();
        sigDataInfo += "";

        // for now, hard-code the signature type for X509
        // in future with more test data, update this (for SPDM potentially need to look at previous SPDM event)
        byte[] guid = HexUtils.hexStringToByteArray("A159C0A5E494A74A87B5AB155C2BF072");
        UefiGuid signatureType = new UefiGuid(guid);

        int numberOfCerts = 0;
        boolean dataValid = true;
        String dataInvalidStatus = "Signature data validity is undetermined yet";
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
        sigDataInfo += "   Number of X509 Certs in UEFI Signature Data = " + numberOfCerts + "\n";
        int certCnt = 0;
        for (int i = 0; i < sigList.size(); i++) {
            certCnt++;
            sigDataInfo += "   Cert # " + certCnt + " of " + numberOfCerts + ": ------------------\n";
            UefiSignatureData certData = sigList.get(i);
            sigDataInfo += certData.toString();
        }
        if (!dataValid) {
            sigDataInfo += "   *** Invalid UEFI Signature data encountered: " + dataInvalidStatus + "\n";
        }
    }

    /**
     * Print out all the interesting characteristics available on this UEFI Variable.
     *
     * @return human-readable description of the UEFi variable.
     */
    public String toString() {
        StringBuilder efiVariable = new StringBuilder();

        efiVariable.append("   UEFI Variable Name GUID: " + variableNameGuid.toString() + "\n");
        efiVariable.append(String.format("   %s: %s%n", UefiConstants.UEFI_VARIABLE_UNICODE_NAME, unicodeName));
        if (unicodeName != "") {
            efiVariable.append("   UEFI Variable Data => " + "\n");
        }

        // fix shim & moklist once come across an example:
        if (unicodeName.equals("Shim") || unicodeName.equals("MokList")) {
            efiVariable.append(printCert(uefiVariableData, 0));
        } else {
            switch (eventType) {
                case EvConstants.EV_EFI_VARIABLE_DRIVER_CONFIG:
                    if (unicodeName.equals("SecureBoot")) {
                        efiVariable.append(sb.toString());
                    }
                    break;
                case EvConstants.EV_EFI_VARIABLE_BOOT:
                    if (unicodeName.contains("Boot00")) {
                        efiVariable.append(bootv.toString());
                    } else if (unicodeName.equals("BootOrder")) {
                        efiVariable.append(booto.toString());
                    }
                    break;
                case EvConstants.EV_EFI_VARIABLE_AUTHORITY:
                case EvConstants.EV_EFI_SPDM_DEVICE_POLICY:
                case EvConstants.EV_EFI_SPDM_DEVICE_AUTHORITY:
                    break;
                default:
            }
        }

        if(!uefiVariableDataProcessed) {
            efiVariable.append("      Code does not yet process this Uefi Variable\n");
        }

        // Signature List output (if there are any Signature Lists)
        if (certSuperList.size() > 0) {
            efiVariable.append("Number of UEFI Signature Lists = " + certSuperList.size() + "\n");
            int certSuperListCnt = 1;
            for (UefiSignatureList uefiSigList : certSuperList) {
                efiVariable.append("UEFI Signature List # " + certSuperListCnt++ + " of "
                        + certSuperList.size() + ": ------------------\n");
                efiVariable.append(uefiSigList.toString());
            }
        }
        if (invalidSignatureListEncountered) {
            efiVariable.append(invalidSignatureListStatus);
            efiVariable.append("*** Encountered invalid Signature Type - "
                    + "Stopped processing of this event data\n");
        }

        // Signature Data output (if there is a Signature Data)
        if (!sigDataInfo.isEmpty()) {
            efiVariable.append(sigDataInfo);
        }

        return efiVariable.toString();
    }

    /**
     * Retrieves human-readable description from a Certificate.
     *
     * @param data   byte[] holding the certificate.
     * @param offset offset to start of the certificate within the byte array.
     * @return human-readable description of a certificate.
     */
    public String printCert(final byte[] data, final int offset) {
        String certInfo = "";
        byte[] certLength = new byte[UefiConstants.SIZE_2];
        System.arraycopy(data, offset + UefiConstants.OFFSET_2, certLength,
                0, UefiConstants.SIZE_2);
        int cLength = new BigInteger(certLength).intValue() + UefiConstants.SIZE_4;
        byte[] certData = new byte[cLength];
        System.arraycopy(data, offset, certData, 0, cLength);
        try {
            UefiX509Cert cert = new UefiX509Cert(certData);
            certInfo = cert.toString();
        } catch (Exception e) {
            certInfo = "Error Processing Certificate : " + e.getMessage();
        }
        return (certInfo);
    }
}
