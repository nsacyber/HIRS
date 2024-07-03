package hirs.utils.tpm.eventlog.uefi;

import hirs.utils.HexUtils;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import static hirs.utils.tpm.eventlog.uefi.UefiConstants.FILESTATUS_FROM_FILESYSTEM;
import static hirs.utils.tpm.eventlog.uefi.UefiConstants.FILESTATUS_NOT_ACCESSIBLE;

/**
 * Class to process a UEFI variable within a TPM Event.
 * typedef struct tdUEFI_VARIABLE_DATA{
 * UEFI_GUID VariableName;     (16 bytes)
 * UINT64 UnicodeNameLength;   (8 bytes)
 * UINT64 VariableDataLength;  (8 bytes)
 * CHAR16 UnicodeName[];
 * INT8 VariableData[];
 * } UEFI_VARIABLE_DATA
 */
public class UefiVariable {

    /**
     * UEFI defined variable identifier GUID.
     */
    @Getter
    private UefiGuid uefiVarGuid = null;
    /**
     * List of Signature lists.
     */
    private List<UefiSignatureList> certSuperList;
    /**
     * Name of the UEFI variable.
     */
    @Getter
    private String efiVarName = "";
    /**
     * Encountered invalid UEFI Signature List
     */
    private boolean invalidSignatureListEncountered = false;
    /**
     * Invalid UEFI Signature List
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
     * UEFI variable data.
     */
    private byte[] uefiVariableData = null;

    /**
     * Track status of vendor-table.json.
     * The default here is that each list correctly grabbed the file from file system.
     * If any one list has issues, this overall status will change to reflect the
     * problematic list's status.
     */
    @Getter
    private String vendorTableFileStatus = FILESTATUS_FROM_FILESYSTEM;

    /**
     * EFIVariable constructor.
     * The UEFI_VARIABLE_DATA contains a "VariableName" field which is used to determine
     * the class used to parse the data within the "VariableData".
     *
     * @param variableData byte array holding the UEFI Variable.
     * @throws java.security.cert.CertificateException If there a problem
     *              parsing the X509 certificate.
     * @throws java.security.NoSuchAlgorithmException  if there's a problem
     *              hashing the certificate.
     * @throws java.io.IOException                     If there's a problem
     *              parsing the signature data.
     */
    public UefiVariable(final byte[] variableData)
            throws CertificateException, NoSuchAlgorithmException, IOException {
        certSuperList = new ArrayList<>();
        byte[] guid = new byte[UefiConstants.SIZE_16];
        byte[] nameLength = new byte[UefiConstants.SIZE_8];
        byte[] nameTemp = null;
        byte[] dataLength = new byte[UefiConstants.SIZE_8];
        byte[] name = null;
        int variableLength = 0;

        System.arraycopy(variableData, 0, guid, 0, UefiConstants.SIZE_16);
        uefiVarGuid = new UefiGuid(guid);
        System.arraycopy(variableData, UefiConstants.SIZE_16, nameLength,
                0, UefiConstants.SIZE_8);
        int nlength = HexUtils.leReverseInt(nameLength);
        System.arraycopy(variableData, UefiConstants.OFFSET_24, dataLength,
                0, UefiConstants.SIZE_8);
        nameTemp = new byte[nlength * UefiConstants.SIZE_2];

        System.arraycopy(variableData, UefiConstants.OFFSET_32,
                nameTemp, 0, nlength * UefiConstants.SIZE_2);
        byte[] name1 = UefiDevicePath.convertChar16tobyteArray(nameTemp);
        name = new byte[nlength];
        System.arraycopy(name1, 0, name, 0, nlength);
        variableLength = HexUtils.leReverseInt(dataLength);
        uefiVariableData = new byte[variableLength];
        System.arraycopy(variableData, UefiConstants.OFFSET_32
                + nlength * UefiConstants.SIZE_2, uefiVariableData, 0, variableLength);
        efiVarName = new String(name, StandardCharsets.UTF_8);
        String tmpName = efiVarName;
        if (efiVarName.contains("Boot00")) {
            tmpName = "Boot00";
        }
        switch (tmpName) {
            case "PK":
            case "KEK":
            case "db":
            case "dbx":
                processSigList(uefiVariableData);
                break;
            case "devdb":   // Update when test patterns exist
                break;      // PFP v1.06 Rev 52, Sec 3.3.4.8
                            // EV_EFI_SPDM_DEVICE_POLICY: EFI_SIGNATURE_LIST
                            // EV_EFI_SPDM_DEVICE_AUTHORITY: EFI_SIGNATURE_DATA
            case "Boot00":
                bootv = new UefiBootVariable(uefiVariableData);
                break;
            case "BootOrder":
                booto = new UefiBootOrder(uefiVariableData);
                break;
            case "SecureBoot":
                sb = new UefiSecureBoot(uefiVariableData);
                break;
            default:
        }
    }

    /**
     * Processes the data as a list of UEFI defined Signature Lists.
     *
     * @param data the bye array holding one or more Signature Lists.
     * @throws java.security.cert.CertificateException If there's a problem
     *          parsing the X509 certificate.
     * @throws java.security.NoSuchAlgorithmException  if there's a problem
     *          hashing the certificate.
     * @throws java.io.IOException                     If there's a problem
     *          parsing the signature data.
     */
    private void processSigList(final byte[] data)
            throws CertificateException, NoSuchAlgorithmException, IOException {
        ByteArrayInputStream certData = new ByteArrayInputStream(data);
        while (certData.available() > 0) {
            UefiSignatureList list;
            list = new UefiSignatureList(certData);

            // first check if any previous list has not been able to access vendor-table.json,
            // and if that is the case, the first comparison in the if returns false and
            // the if statement is not executed
            // [previous event file status = vendorTableFileStatus]
            // (ie. keep the file status to reflect that file was not accessible at some point)
            // next, check if the new list has any status other than the default 'filesystem',
            // and if that is the case, the 2nd comparison in the if returns true and
            // the if statement is executed
            // [new event file status = list.getVendorTableFileStatus()]
            // (ie. if the new file status is not-accessible or from-code, then want to update)
            if((vendorTableFileStatus != FILESTATUS_NOT_ACCESSIBLE) &&
                    (list.getVendorTableFileStatus() != FILESTATUS_FROM_FILESYSTEM)) {
                        vendorTableFileStatus = list.getVendorTableFileStatus();
            }

//            efiVariableSigListContents += list.toString();
            if(!list.isSignatureTypeValid()) {
                invalidSignatureListEncountered = true;
                invalidSignatureListStatus = list.toString();
                break;
            }
            certSuperList.add(list);
        }
    }

    /**
     * Print out all the interesting characteristics available on this UEFI Variable.
     *
     * @return human readable description of the UEFi variable.
     */
    public String toString() {
        StringBuilder efiVariable = new StringBuilder();

        efiVariable.append("UEFI Variable Name: " + efiVarName + "\n");
        efiVariable.append("UEFI Variable GUID: " + uefiVarGuid.toString() + "\n");
        if (efiVarName != "") {
            efiVariable.append("UEFI Variable Contents => " + "\n");
        }
        String tmpName = "";
        if (efiVarName.contains("Boot00")) {
            tmpName = "Boot00";
        } else {
            tmpName = efiVarName;
        }
        switch (tmpName) {
            case "Shim":
            case "MokList":
                efiVariable.append(printCert(uefiVariableData, 0));
                break;
            case "PK":
            case "KEK":
            case "db":
            case "dbx":
                break;
            case "devdb":           // SPDM_DEVICE_POLICY and SPDM_DEVICE_AUTHORITY
                                    // (update when test patterns exist)
                efiVariable.append("   EV_EFI_SPDM_DEVICE_POLICY and EV_EFI_SPDM_DEVICE_AUTHORITY: " +
                        "To be processed once more test patterns exist");
                break;
            case "Boot00":
                efiVariable.append(bootv.toString());
                break;
            case "BootOrder":
                efiVariable.append(booto.toString());
                break;
            case "SecureBoot":
                efiVariable.append(sb.toString());
                break;
            default:
                if (!tmpName.isEmpty()) {
                    efiVariable.append(String.format("Data not provided for "
                                    + "UEFI variable named %s   ", tmpName));
                } else {
                    efiVariable.append("Data not provided   ");
                }
        }

        // Signature List output (if there are any Signature Lists)
        if (certSuperList.size() > 0){
            efiVariable.append("Number of UEFI Signature Lists = " + certSuperList.size() + "\n");
        }
        int certSuperListCnt = 1;
        for (UefiSignatureList uefiSigList : certSuperList) {
            efiVariable.append("UEFI Signature List # " + certSuperListCnt++ + " of " +
                    certSuperList.size() + ":\n");
            efiVariable.append(uefiSigList.toString());
        }
        if(invalidSignatureListEncountered) {
            efiVariable.append(invalidSignatureListStatus);
            efiVariable.append("*** Encountered invalid Signature Type - " +
                    "Stopped processing of this event data\n");
        }

        return efiVariable.toString();
    }

    /**
     * Retrieves human readable description from a Certificate.
     *
     * @param data   byte[] holding the certificate.
     * @param offset offset to start of the certificate within the byte array.
     * @return human readable description of a certificate.
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
