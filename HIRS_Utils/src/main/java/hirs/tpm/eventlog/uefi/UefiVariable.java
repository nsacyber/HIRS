package hirs.tpm.eventlog.uefi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;

import hirs.utils.HexUtils;

/**
 *  Class to process a UEFI variable within a TPM Event.
 *  typedef struct tdUEFI_VARIABLE_DATA{
 *      UEFI_GUID VariableName;     (16 bytes)
 *      UINT64 UnicodeNameLength;   (8 bytes)
 *      UINT64 VariableDataLength;  (8 bytes)
 *      CHAR16 UnicodeName[];
 *      INT8 VariableData[];
 *  } UEFI_VARIABLE_DATA
 */
public class UefiVariable {

  /** UEFI defined variable identifier GUID. */
  private UefiGuid uefiGuid = null;
  /** List of Signature lists. */
  private ArrayList<UefiSignatureList> certSuperList = new ArrayList<UefiSignatureList>();
  /** Name of the UEFI variable. */
  private String varName = "";
  /** UEFI defined Boot Variable. */
  private UefiBootVariable bootv = null;
  /** UEFI Defined boot order. */
  private UefiBootOrder booto = null;
  /** UEFI defined secure boot. */
  private UefiSecureBoot sb = null;
  /** UEFI variable data. */
  private byte[] uefiVaribelData = null;

/**
 * EFIVariable constructor.
 * The UEFI_VARIABLE_DATA contains a "VariableName" field which is used to determine
 * the class used to parse the data within the "VariableData".
 * @param varibaleData byte array holding the UEFI Variable.
 * @throws CertificateException If there a problem parsing the X509 certificate.
 * @throws NoSuchAlgorithmException if there's a problem hashing the certificate.
 * @throws IOException If there's a problem parsing the signature data.
 */
public UefiVariable(final byte[] varibaleData)
        throws CertificateException, NoSuchAlgorithmException, IOException {
  byte[] guid = new byte[UefiConstants.SIZE_16];
  byte[] nameLength = new byte[UefiConstants.SIZE_8];
  byte[] nameTemp = null;
  byte[] dataLength = new byte[UefiConstants.SIZE_8];
  byte[] name = null;
  int variableLength = 0;

  System.arraycopy(varibaleData, 0, guid, 0, UefiConstants.SIZE_16);
  uefiGuid = new UefiGuid(guid);
  System.arraycopy(varibaleData, UefiConstants.SIZE_16, nameLength, 0, UefiConstants.SIZE_8);
  int nlength = HexUtils.leReverseInt(nameLength);
  System.arraycopy(varibaleData, UefiConstants.OFFSET_24, dataLength, 0, UefiConstants.SIZE_8);
  nameTemp = new byte[nlength * UefiConstants.SIZE_2];

  System.arraycopy(varibaleData, UefiConstants.OFFSET_32,
                   nameTemp, 0, nlength * UefiConstants.SIZE_2);
  byte[] name1 = UefiDevicePath.convertChar16tobyteArray(nameTemp);
  name = new byte[nlength];
  System.arraycopy(name1, 0, name, 0, nlength);
  variableLength = HexUtils.leReverseInt(dataLength);
  uefiVaribelData = new byte[variableLength];
  System.arraycopy(varibaleData, UefiConstants.OFFSET_32
                   + nlength * UefiConstants.SIZE_2, uefiVaribelData, 0, variableLength);
  varName = new String(name, "UTF-8");
  String tmpName = varName;
  if (varName.contains("Boot00")) {
      tmpName = "Boot00";
  }
  switch (tmpName) {
      case "PK":          processSigList(uefiVaribelData); break;
      case "KEK":         processSigList(uefiVaribelData); break;
      case "db":          processSigList(uefiVaribelData); break;
      case "dbx":         processSigList(uefiVaribelData); break;
      case "Boot00":      bootv = new UefiBootVariable(uefiVaribelData); break;
      case "BootOrder":   booto = new UefiBootOrder(uefiVaribelData); break;
      case "SecureBoot":  sb = new UefiSecureBoot(uefiVaribelData); break;
      default:
      }
}

/**
 * The GUID is a globally unique identifier assigned to this UEFI variable.
 * UEFI variable specific GUIDs are specified in the UEFI specification.
 * @return UEFI Variable GUID.
 */
public UefiGuid getEfiVarGuid() {
    return uefiGuid;
}

/**
 * Returns a human readable Name for this UEFI Variable (e.g. SecureBoot).
 * @return the UEFI Variable name assigned to this variable.
 */
public String getEfiVarName() {
   return varName;
}

/**
 * Returns a arrayList of UefiSignatureList (Certificates or hashes) held in the Event Log.
 * @return and array list of UEFI defined Signature Lists.
 */
public ArrayList<UefiSignatureList> getEFISignatureList() {
  return certSuperList;
}

/**
 * Processes the data as a UEFI defined Signature List.
 * @param data the bye array holding the Signature List.
 * @throws CertificateException If there a problem parsing the X509 certificate.
 * @throws NoSuchAlgorithmException if there's a problem hashing the certificate.
 * @throws IOException If there's a problem parsing the signature data.
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
 *  Print out all the interesting characteristics available on this UEFI Variable.
 *  @return human readable description of the UEFi variable.
 */
public String toString() {
  StringBuffer efiVariable = new StringBuffer();
  efiVariable.append("UEFI Variable Name:" + varName + "\n");
  efiVariable.append("UEFI_GUID = " + getEfiVarGuid().toString() + "\n");
  efiVariable.append("UEFI Variable Contents => " + "\n");
  String tmpName = varName;
  if (varName.contains("Boot00")) {
      tmpName = "Boot00";
  }
  switch (tmpName) {
     case "Shim":       efiVariable.append(printCert(uefiVaribelData, 0)); break;
     case "MokList":    efiVariable.append(printCert(uefiVaribelData, 0)); break;
     case "Boot00":     efiVariable.append(bootv.toString()); break;
     case "BootOrder":  efiVariable.append(booto.toString()); break;
     case "SecureBoot": efiVariable.append(sb.toString()); break;
     default:
     }
  for (int i = 0; i < certSuperList.size(); i++) {
        efiVariable.append(certSuperList.get(i).toString());
  }
  return efiVariable.toString();
}

/**
 * Retrieves human readable description from a Certificate.
 * @param data byte[] holding the certificate.
 * @param offset offset to start of the certificate within the byte array.
 * @return  human readable description of a certificate.
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
         certInfo = "Error Processing Certificate : " + e.getMessage() + "\n";
      }
  return (certInfo);
 }
}
