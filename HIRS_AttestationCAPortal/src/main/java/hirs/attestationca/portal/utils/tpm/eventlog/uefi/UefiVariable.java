package hirs.attestationca.portal.utils.tpm.eventlog.uefi;

import hirs.attestationca.utils.HexUtils;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

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
    @Getter
    private ArrayList<UefiSignatureList> certSuperList = new ArrayList<>();
    /**
     * Name of the UEFI variable.
     */
    @Getter
    private String efiVarName = "";
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
     * EFIVariable constructor.
     * The UEFI_VARIABLE_DATA contains a "VariableName" field which is used to determine
     * the class used to parse the data within the "VariableData".
     *
     * @param variableData byte array holding the UEFI Variable.
     * @throws java.security.cert.CertificateException If there a problem parsing the X509 certificate.
     * @throws java.security.NoSuchAlgorithmException  if there's a problem hashing the certificate.
     * @throws java.io.IOException                     If there's a problem parsing the signature data.
     */
    public UefiVariable(final byte[] variableData)
            throws CertificateException, NoSuchAlgorithmException, IOException {
        byte[] guid = new byte[UefiConstants.SIZE_16];
        byte[] nameLength = new byte[UefiConstants.SIZE_8];
        byte[] nameTemp = null;
        byte[] dataLength = new byte[UefiConstants.SIZE_8];
        byte[] name = null;
        int variableLength = 0;

        System.arraycopy(variableData, 0, guid, 0, UefiConstants.SIZE_16);
        uefiVarGuid = new UefiGuid(guid);
        System.arraycopy(variableData, UefiConstants.SIZE_16, nameLength, 0, UefiConstants.SIZE_8);
        int nlength = HexUtils.leReverseInt(nameLength);
        System.arraycopy(variableData, UefiConstants.OFFSET_24, dataLength, 0, UefiConstants.SIZE_8);
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
     * Processes the data as a UEFI defined Signature List.
     *
     * @param data the bye array holding the Signature List.
     * @throws java.security.cert.CertificateException If there a problem parsing the X509 certificate.
     * @throws java.security.NoSuchAlgorithmException  if there's a problem hashing the certificate.
     * @throws java.io.IOException                     If there's a problem parsing the signature data.
     */
    private void processSigList(final byte[] data)
            throws CertificateException, NoSuchAlgorithmException, IOException {
        ByteArrayInputStream certData = new ByteArrayInputStream(data);
        while (certData.available() > 0) {
            UefiSignatureList list;
            list = new UefiSignatureList(certData);
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
        efiVariable.append("UEFI Variable Name:" + efiVarName + "\n");
        efiVariable.append("UEFI_GUID = " + uefiVarGuid.toString() + "\n ");
        if (efiVarName != "") {
            efiVariable.append("UEFI Variable Contents => " + "\n  ");
        }
        String tmpName = efiVarName;
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
                    efiVariable.append(String.format("Data not provided for UEFI variable named %s   ",
                            tmpName));
                } else {
                    efiVariable.append("Data not provided   ");
                }
        }
        for (int i = 0; i < certSuperList.size(); i++) {
            efiVariable.append(certSuperList.get(i).toString());
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
        System.arraycopy(data, offset + UefiConstants.OFFSET_2, certLength, 0, UefiConstants.SIZE_2);
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
