package hirs.tpm.eventlog.uefi;

import java.math.BigInteger;
import java.util.UUID;

import hirs.utils.HexUtils;

/**
 * Class to process GUID per the UEFI specification
 * GUIDs are essentially UUID as defined by RFC-1422, however Microsoft refers to GUIDS.
 */
public class UefiGuid {
    /** number of 100ns intervals since UUID Epoch. */
    private static final long UUID_EPOCH_INTERVALS =  0x01b21dd213814000L;
    /** used for conversion to uuid time. */
    private static final int UUID_EPOCH_DIVISOR = 10000;
    /** guid byte array. */
    private byte[] guid = new byte[UefiConstants.SIZE_16 ];
    /** UUID object. */
    private UUID uuid;

    /**
     * UefiGUID constructor.
     * @param guidBytes byte array holding a valid guid.
     */
    public UefiGuid(final byte[] guidBytes) {
      System.arraycopy(guidBytes, 0, guid, 0, UefiConstants.SIZE_16);
      uuid = processGuid(guidBytes);
    }

 /** Converts a GUID with a byte array to a RFC-1422 UUID object.
  * Assumes a MS format and converts to Big Endian format used by most others , including Linux
  * Matched uuids found in /sys/firmware/efi/efivars on Centos 7.
  */
private static UUID processGuid(final byte[] guid) {
   byte[] msb1 = new byte[UefiConstants.SIZE_4];
   System.arraycopy(guid, 0, msb1, 0, UefiConstants.SIZE_4);
   byte[] msb1r = HexUtils.leReverseByte(msb1);
   byte[] msb2 = new byte[UefiConstants.SIZE_4];
   System.arraycopy(guid, UefiConstants.OFFSET_4, msb2, 0, UefiConstants.SIZE_4);
   byte[] msb2r = HexUtils.leReverseByte(msb2);
   byte[] msb2rs = new byte[UefiConstants.SIZE_4];
   System.arraycopy(msb2r, 0, msb2rs, UefiConstants.OFFSET_2, UefiConstants.SIZE_2);
   System.arraycopy(msb2r, UefiConstants.OFFSET_2, msb2rs, 0, UefiConstants.SIZE_2);
   byte[] msbt = new byte[UefiConstants.SIZE_8];
   System.arraycopy(msb1r, 0, msbt, 0, UefiConstants.SIZE_4);
   System.arraycopy(msb2rs, 0, msbt, UefiConstants.OFFSET_4, UefiConstants.SIZE_4);
   long msbl = new BigInteger(msbt).longValue();
   byte[] lsb = new byte[UefiConstants.SIZE_8];
   System.arraycopy(guid, UefiConstants.OFFSET_8, lsb, 0, UefiConstants.SIZE_8);
   long lsbl = new BigInteger(lsb).longValue();
   UUID tmpUuid = new UUID(msbl, lsbl);
   return tmpUuid;
 }
/**
 * Returns the standard GUID length.
 * @return guid length
 */
public static int getGuidLength() {
    return UefiConstants.SIZE_16;
}
/**
 * Returns a String that represents a specification name referenced by the EFI_CONFIGURATION_TABLE
 * VendorGUID field.  For structure of EFI_CONFIGURATION_TABLE type, the UEFI specification
 * has set of GUIDs published that represent standards that one can find further information on
 * the configuration table being referenced.
 * Refer to section 4.6 of UEFI spec v 2.8, page 101.
 *
 * @return A String of major UUID parameters
 */
@SuppressWarnings("checkstyle:methodlength")
public String getVendorTableReference() {

 String vendorRef = uuid.toString().toLowerCase();
 String reference = "";

 switch (vendorRef) {
 // UUIDS listed in the UEFI Specification
 case "eb9d2d30-2d88-11d3-9a16-0090273fc14d": reference = "ACPI_TABLE_GUID"; break;
 case "eb9d2d32-2d88-11d3-9a16-0090273fc14d": reference = "SAL_SYSTEM_TABLE_GUID"; break;
 case "eb9d2d31-2d88-11d3-9a16-0090273fc14d": reference = "SMBIOS_TABLE_GUID"; break;
 case "f2fd1544-9794-4a2c-992e-e5bbcf20e394": reference = "SMBIOS3_TABLE_GUID"; break;
 case "eb9d2d2f-2d88-11d3-9a16-0090273fc14d": reference = "MPS_TABLE_GUID"; break;
 case "8868e871-e4f1-11d3-bc22-0080c73c8881": reference = "EFI_ACPI_TABLE_GUID"; break;
 case "87367f87-1119-41ce-aaec-8be01101f558":
                reference = "EFI_JSON_CONFIG_DATA_TABLE_GUID "; break;
 case "35e7a725-8dd2-4cac-8011-33cda8109056":
                reference = "EFI_JSON_CAPSULE_DATA_TABLE_GUID"; break;
 case "dbc461c3-b3de-422a-b9b4-9886fd49a1e5":
                reference = "EFI_JSON_CAPSULE_RESULT_TABLE_GUID"; break;
 case "77ab535a-45fc-624b-5560-f7b281d1f96e": reference = "EFI_VIRTUAL_DISK_GUID"; break;
 case "3d5abd30-4175-87Ce-6d64-d2ADe523C4bb": reference = "EFI_VIRTUAL_CD_GUID"; break;
 case "5Cea02c9-4d07-69d3-269f-4496Fbe096f9":
                reference = "EFI_PERSISTENT_VIRTUAL_DISK_GUID"; break;
 case "08018188-42cd-bb48-100f-5387D53ded3d": reference = "EFI_PERSISTENT_VIRTUAL_CD_GUID"; break;

 // DXE GUIds from https://github.com/linuxboot/linuxboot/blob/master/boards/qemu/image-files.txt
 case "fc510ee7-ffdc-11d4-bd41-0080c73c8881": reference = "DXE Apriori-FVRECOVERY"; break;
 case "1b45cc0a-156a-428a-62af-49864da0e6e6": reference = "PEI Apriori file name"; break;
 case "80cf7257-87ab-47f9-a3fe-d50b76d89541": reference = "PcdDxe"; break;
 case "b601f8c4-43b7-4784-95b1-f4226cb40cee": reference = "RuntimeDxe"; break;
 case "f80697e9-7fd6-4665-8646-88e33ef71dfc": reference = "SecurityStubDxe"; break;
 case "1a1e4886-9517-440e-9fde-3be44cee2136": reference = "CpuDxe"; break;
 case "11a6edf6-a9be-426d-a6cc-b22fe51d9224": reference = "PciHotPlugInitDxe"; break;
 case "128fb770-5e79-4176-9e51-9bb268a17dd1": reference = "PciHostBridgeDxe"; break;
 case "93b80004-9fb3-11d4-9a3a-0090273fc14d": reference = "PCI Bus Driver - PciBusDxe"; break;
 case "9b680fce-ad6b-4f3a-b60b-f59899003443": reference = "DevicePathDxe"; break;
 case "f9d88642-0737-49bc-81b5-6889cd57d9ea": reference = "SmbiosDxe"; break;
 case "4110465d-5ff3-4f4b-b580-24ed0d06747a": reference = "SmbiosPlatformDxe"; break;
 case "9622e42c-8e38-4a08-9e8f-54f784652f6b": reference = "AcpiTableDxe"; break;
 case "49970331-e3fa-4637-9abc-3b7868676970": reference = "AcpiPlatform"; break;
 case "7e374e25-8e01-4fee-87f2-390c23c606cd": reference = "ACPI data"; break;
 case "bdce85bb-fbaa-4f4e-9264-501a2c249581": reference = "S3SaveStateDxe"; break;
 case "d9dcc5df-4007-435e-9098-8970935504b2": reference = "PlatformDxe"; break;
 case "8657015b-ea43-440d-949a-af3be365c0fc": reference = "IoMmuDxe"; break;
 case "cbd2e4d5-7068-4ff5-b462-9822b4ad8d60": reference = "VariableRuntimeDxe"; break;

 //PIWG Dxe driver Files (FvFile)
 // from https://bugs.launchpad.net/ubuntu/+source/edk2/+bug/1272444
 case "70d57d67-7f05-494d-a014-b75d7345b700": reference = "Storage Security Command Driver"; break;
 case "3acc966d-8e33-45c6-b4fe-62724bcd15a9": reference = "AHCI Bus Driver"; break;
 case "67bbc344-84bc-4e5c-b4df-f5e4a00e1f3a": reference = "Host Controller Driver"; break;
 case "86edaae5-073c-4c89-b949-8984ac8a55f3": reference = "MMC/SD Media Device Driver"; break;
 case "9e863906-a40f-4875-977F-5b93ff237fc6": reference = "Serial Terminal Driver"; break;
 case "a6cc6bc8-2ada-46C3-bba4-e99672CC9530": reference = "PCI Serial Driver"; break;
 case "69fd8e47-a161-4550-b01a-5594ceb2b2b2": reference = "PCI IDE/ATAPI Bus Driver"; break;
 case "51ccf399-4fdf-4e55-a45b-e123f84d456a":
               reference = "Platform Console Management Driver"; break;
 case "6b38f7b4-ad98-40e9-9093-aca2b5a253c4": reference = "Generic Disk I/O Driver"; break;
 case "2d2e62cf-9ecf-43b7-8219-94e7fC713dfe": reference = "Usb Keyboard Driver"; break;
 case "9fb4b4a7-42C0-4bcd-8540-9bcc6711f83e": reference = "Usb Mass Storage Driver"; break;
 case "e3752948-b9a1-4770-90c4-df41c38986be": reference = "QEMU Video Driver"; break;
 case "240612B7-a063-11d4-9a3a-0090273fc14d": reference = "Usb Bus Driver"; break;
 case "bdfe430e-8F2a-4db0-9991-6f856594777e": reference = "Usb Ehci Driver"; break;
 case "2fb92efa-2ee0-4bae-9eB6-7464125E1EF7": reference = "Usb Ehci Driver"; break;
 case "a92cdb4b-82f1-4e0b-a516-8a655d371524": reference = "Virtio Network Driver"; break;
 case "4579b72d-7ec4-4dd4-8486-083c86b182a7": reference = "iSCSI Driver"; break;
 case "3b1deaB5-c75d-442e-9238-8e2ffb62b0bb": reference = "UEFI PXE Base Code Driver"; break;
 case "6b6963ab-906d-4a65-a7ca-bd40e5d6af2b": reference = "UDP Network Service Driver"; break;
 case "6d6963ab-906d-4a65-a7ca-bd40e5d6af4d": reference = "Tcp Network Service Driver"; break;
 case "dc3641b8-2fa8-4ed3-bc1f-f9962a03454b": reference = "MTFTP4 Network Service Driver"; break;
 case "9fb1a1f3-3b71-4324-b39a-745cbb015fff": reference = "IP4 Network Service Driver"; break;
 case "26841bde-920a-4e7a-9Fbe-637f477143a6":
                reference = "IP4 CONFIG Network Service Driver"; break;
 case "94734718-0bbc-47fb-96a5-ee7a5ae6a2ad": reference = "DHCP Protocol Driver"; break;
 case "529d3f93-e8e9-4e73-b1e1-bdf6a9d50113": reference = "ARP Network Service Driver "; break;
 case "e4f61863-fe2c-4b56-a8d4-08519bc439df": reference = "VLAN Configuration Driver"; break;
 case "a2f436ea-a127-4ef8-957c-8048606ff670": reference = "Simple Network Protocol Driver"; break;
 case "961578fe-b6b7-44c3-af35-6bc705cd2b1f": reference = "FAT File System Driver"; break;
 case "0abd8284-6da3-4616-971a-83a5148067ba": reference = "ISA Floppy Driver"; break;
 case "3dc82376-637b-40a6-a8fc-a565417f2c38": reference = "PS/2 Keyboard Driver"; break;
 case "93b80003-9fb3-11d4-9a3a-0090273fc14d": reference = "ISA Serial Driver"; break;
 case "240612b5-a063-11d4-9a3a-0090273fc14a": reference = "ISA Bus Driver"; break;
 case "99549f44-49bb-4820-b9d2-901329412d67": reference = "IDE Controller Init Driver"; break;
 case "0a66e322-3740-4cce-ad62-bd172cecca35": reference = "Scsi Disk Driver"; break;
 case "1fa1f39e-feff-4aae-bd7b-38a070a3b609": reference = "Partition Driver"; break;
 case "9e863906-a40f-4875-977f-5b93ff237fc6": reference = "Serial Terminal Driver"; break;
 case "cccb0c28-4b24-11d5-9a5a-0090273fc14d": reference = "Graphics Console Driver"; break;
 case "408edcec-cf6d-477c-a5a8-b4844e3de281": reference = "Console Splitter Driver"; break;
 case "fab5d4f4-83c0-4aaf-8480-442d11df6cea": reference = "Virtio SCSI Host Driver"; break;
 case "11d92dfb-3Ca9-4f93-ba2e-4780ed3e03b5": reference = "Virtio Block Driver"; break;
 case "33cb97af-6c33-4c42-986b-07581fa366d4": reference = "Block MMIO to Block IO Driver"; break;
 // PIWG Volumes (Fv)
 case "a881d567-6cb0-4eee-8435-2e72d33e45B5": reference = "PIWG Default Volume"; break;

 // UEFI UUIDS for Certificates
 case "3c5766e8-269c-4e34-aa14-ed776e85b3b6":reference = "EFI_CERT_RSA2048_GUID"; break;
 case "e2b36190-879b-4a3d-ad8d-f2e7bba32784":reference = "EFI_CERT_RSA2048_SHA256_GUID"; break;
 case "c1c41626-504c-4092-aca9-41f936934328":reference = "EFI_CERT_SHA256_GUID"; break;
 case "826ca512-cf10-4ac9-b187-be01496631bd":reference = "EFI_CERT_SHA1_GUID"; break;
 case "67f8444f-8743-48f1-a328-1eaab8736080":reference = "EFI_CERT_RSA2048_SHA1_GUID"; break;
 case "a5c059a1-94e4-4aa7-87b5-ab155c2bf072":reference = "EFI_CERT_X509_GUID"; break;
 case "0b6e5233-a65c-44c9-9407-d9ab83bfc8bd":reference = "EFI_CERT_SHA224_GUID"; break;
 case "ff3e5307-9fd0-48c9-85f1-8ad56c701e01":reference = "EFI_CERT_SHA384_GUID"; break;
 case "093e0fae-a6c4-4f50-9f1b-d41e2b89c19a":reference = "EFI_CERT_SHA512_GUID"; break;
 case "3bd2a492-96c0-4079-b420-fcf98ef103ed":reference = "EFI_CERT_X509_SHA256_GUID"; break;
 case "7076876e-80c2-4ee6-aad2-28b349a6865b":reference = "EFI_CERT_X509_SHA384_GUID"; break;
 case "446dbf63-2502-4cda-bcfa-2465d2b0fe9d":reference = "EFI_CERT_X509_SHA512_GUID"; break;
 case "a7717414-c616-4977-9420-844712a735bf":
                reference = "EFI_CERT_TYPE_RSA2048_SHA256_GUID"; break;
 // UEFI defined variables
 case "452e8ced-dfff-4b8c-ae01-5118862e682c":
                reference = "EFI_CERT_EXTERNAL_MANAGEMENT_GUID"; break;
 case "d719b2cb-3d3a-4596-a3bc-dad00e67656f":reference = "EFI_IMAGE_SECURITY_DATABASE_GUID"; break;
 case "4aafd29d-68df-49ee-8aa9-347d375665a7":reference = "EFI_CERT_TYPE_PKCS7_GUID"; break;
 case "c12a7328-f81f-11d2-ba4b-00a0c93ec93b" :reference = "EFI System Partition"; break;
 case "024DEE41-33E7-11D3-9D69-0008C781F39F" :
                reference = "Partition containing a legacy MBR"; break;
 // RHBoot UEFI Application UUIDs
 // From listed in RHBoot (RHShim) https://github.com/rhboot/efivar/blob/master/src/guids.txt
 case "0abba7dc-e516-4167-bbf5-4d9d1c739416":reference = "fwupdate:"; break;
 case "3b8c8162-188c-46a4-aec9-be43f1d65697":reference = "ux_capsule"; break;
 case "605dab50-e046-4300-abb6-3dd810dd8b23":reference = "RH_Shim"; break;
 case "8be4df61-93ca-11d2-aa0d-00e098032b8c":reference = "EFI_Global_Variable"; break;
 case "91376aff-cba6-42be-949d-06fde81128e8":reference = "GRUB"; break;

 // Partition Table GUIDs
 case "0fc63daf-8483-4772-8e79-3d69d8477de4":reference = "Linux filesystem data"; break;
 case "e6d6d379-f507-44c2-a23c-238f2a3df928":
                reference = "Logical Volume Manager (LVM) partition"; break;
 case "4f68bce3-e8cd-4db1-96e7-fbcaf984b709":reference = "Root partition (x86-64)"; break;
 case "a19d880f-05fc-4d3b-a006-743f0f84911e":reference = "RAID partition "; break;
 case "933ac7e1-2eb4-4f13-b844-0e14e2aef915":reference = "/home partition[ (x86-64)"; break;
 case "ebd0a0a2-b9e5-4433-87c0-68b6b72699c7":reference = "GPT Basic data partition"; break;

 // RHBoot Lenovo specific UUIDS
 case "3cc24e96-22c7-41d8-8863-8e39dcdcc2cf":reference = "lenovo"; break;
 case "82988420-7467-4490-9059-feb448dd1963":reference = "lenovo_me_config"; break;
 case "f7e615b-0d45-4f80-88dc-26b234958560":reference = "lenovo_diag"; break;
 case "665d3f60-ad3e-4cad-8e26-db46eee9f1b5":reference = "lenovo_rescue"; break;
 case "721c8b66-426c-4e86-8e99-3457c46ab0b9":reference = "lenovo_setup"; break;
 case "f46ee6f4-4785-43a3-923d-7f786c3c8479":reference = "lenovo_startup_interrupt"; break;
 case "126a762d-5758-4fca-8531-201a7f57f850":reference = "lenovo_boot_menu"; break;
 case "a7d8d9a6-6ab0-4aeb-ad9d-163e59a7a380":reference = "lenovo_diag_splash"; break;
 // Company UUIDs (From Internet searches)
 case "77fa9abd-0359-4d32-bd60-28f4e78f784b":reference = "Microsoft Inc."; break;
 case "f5a96b31-dba0-4faa-a42a-7a0c9832768e":reference = "HPE Inc."; break;
 case "2879c886-57ee-45cc-b126-f92f24f906b9":reference = "SUSE Certificate"; break;
 case "70564dce-9afc-4ee3-85fc-949649d7e45c":reference = "Dell Inc."; break;

 // Intel GUIDS
 case "bfcc0833-2125-42d1-8c6d-13821e23c078":reference = "Intel(R) Desktop Boards"; break;
 case "80b3ad5b-9880-4af9-a645-e56a68be89de":reference = "Intel(R) CISD FW Update"; break;

// Microsoft GUIDS
 case "e3c9e316-0b5c-4db8-817d-f92df00215ae":
                reference = "Microsoft Reserved Partition (MSR)"; break;
 case "5808c8aa-7e8f-42e0-85d2-e1e90434cfb3":
                reference = "Logical Disk Manager (LDM) metadata partition "; break;
 case "af9b60a0-1431-4f62-bc68-3311714a69ad":
                reference = "Logical Disk Manager data partition"; break;
 case "de94bba4-06d1-4d40-a16a-bfd50179d6ac":reference = "Windows Recovery Environment"; break;
 case "9f25ee7a-e7b7-11db-94b5-f7e662935912":reference = "Windows Boot Loader"; break;

 // Linux specific GUIDS
 case "0fc63daf-8483-4772-8e79-3d69d8477de":reference = "Linux filesystem data"; break;
 case "44479540-f297-41b2-9af7-d131d5f0458a4":reference = "Root partition (x86)"; break;
 case "69dad710-2ce4-4e3c-b16c-21a1d49abed3":reference = "Root partition (32-bit ARM)"; break;
 case "b921b045-1df0-41c3-af44-4c6f280d3fae":
                reference = "Root partition (64-bit ARM/AArch64)"; break;
 case "0657fd6d-a4ab-43c4-84e5-0933c84b4f4f":reference = "Swap partition"; break;
 case "3b8f8425-20e0-4f3b-907f-1a25a76f98e8":reference = "/srv (server data) partition"; break;
 case "7ffec5c9-2d00-49b7-8941-3ea10a5586b7":reference = "Plain dm-crypt partitiont"; break;
 case "ca7d7ccb-63ed-4c53-861c-1742536059cc":reference = "LUKS partition"; break;

 // Linux Boot GUIDS
 // https://github.com/linuxboot/linuxboot/blob/master/boards/s2600wf/vendor-files.txt
 case "9cfd802c-09a1-43d6-8217-aa49c1f90d2c":
                reference = "Intel Management Engine BIOS Extension (Mebx)"; break;
 case "b62efbbb-3923-4cb9-a6e8-db818e828a80":
                reference = "Intel Management Engine BIOS Extension (Mebx) Setup Browser"; break;
 case "9ce4325e-003e-11e3-b582-b8ac6f199a57":
                reference = "Non-Volatile Dual In-line Memory Module (NVDIMM) Driver"; break;
 case "ea9de6d5-7839-46f7-9e63-4de8b00e2e5d":
                reference = "NVM DIMM Human Interface Infrastructure (HII)"; break;
 case "56a1b86f-0d4a-485d-87de-ad0eba1c8c2a":reference = "IBM C Video Gop"; break;
 case "a1f436ea-a127-4ef8-957c-8048606ff670":reference = "SnpDxe"; break;
 case "a210f973-229d-4f4d-aa37-9895e6c9eaba":reference = "DpcDxe"; break;
 case "025bbfc7-e6a9-4b8b-82ad-6815a1aeaf4a":
                reference = "MNP Network Service Driver - MnpDxe"; break;
 case "b44b2005-42bc-41c9-80af-abd7dc7d6923":reference = "RSTesSATAEFI"; break;
 case "15e1e31a-9f9d-4c84-82fb-1a707fc0f63b":reference = "RSTeSATAEFI"; break;
 case "2cc25173-bd9f-4c89-89cc-29256a3fd9c3":reference = "RSTesSATALegacy"; break;
 case "bd5d4ca5-674f-4584-8cf9-ce4ea1f54dd1":reference = "RSTeSATALegacy"; break;

 //  WinNt GUIDs, add if they are still found in use
 //https://sourceforge.net/p/uefinotes/wiki/FV%20Sources/?version=3
 case "fc5c7020-1a48-4198-9be2-ead5abc8cf2f":reference = "BdsDxe"; break;
 case "d0893f05-b06d-4161-b947-9be9b85ac3a1":reference = "SnpNt32Dxe"; break;
 case "9b3ada4f-ae56-4c24-8Dea-f03b7558ae50":reference = "PcdPeim"; break;
 case "34c8c28F-b61c-45a2-8f2e-89e46becc63b":reference = "PeiVariable"; break;
 case "fe5cea76-4f72-49e8-986f-2cd899dffe5d":reference = "FaultTolerantWriteDxe"; break;

 // Linux Boot Image files
 // UEFI Platform Initialization (PI) specifications
 // Driver Execution Environment (DXE) Architectural protocols and platform modules
 //https://github.com/linuxboot/linuxboot/blob/master/boards/winterfell/image-files.txt
 case "5ae3f37e-4eae-41ae-8240-35465b5e81eb":reference = "CORE_DXE"; break;
 case "cbc59c4a-383a-41eb-a8ee-4498aea567e4":reference = "DXE Runtime"; break;
 case "3c1de39f-d207-408a-aacc-731cfb7f1dd7":reference = "DXE PciBus"; break;
 case "80e66e0a-ccd1-43fa-a7b1-2d5ee0f13910":reference = "DXE PciRootBridge"; break;
 case "9f3a0016-ae55-4288-829d-d22fd344c347":reference = "DXE AmiBoardInfo"; break;
 case "13ac6dd0-73d0-11d4-b06b-00aa00bd6de7":reference = "DXE EBC"; break;
 case "e03abadf-e536-4e88-b3a0-b77f78eb34fe":reference = "CPU DXE"; break;
 case "b7d19491-e55a-470d-8508-85a5dfa41974":reference = "SBDXE"; break;
 case "e23f86e1-056e-4888-b685-cfcd67c179d4":reference = "DXE SBRun"; break;
 case "e4ecd0b2-e277-4f2b-becb-e4d75c9a812e":reference = "NBDXE"; break;
 case "5ad34ba6-f024-2149-52e4-da0398e2bb9" :reference = "DXE Services Table"; break;
 // ACPI configuration and tables
 case "750890a6-7acf-4f4f-81bd-b400c2bea95a":reference = "AcpiModeEnable"; break;
 case "d4c05cd1-5eae-431d-a095-13a9e5822045":reference = "MPST"; break;
 case "db93cb2c-bf1c-431a-abc8-8737bc2afc1f":reference = "PRAD-ACPI-table"; break;
 case "3bc5b795-a4e0-4d56-9321-316d18a7aefe":reference = "PRAD"; break;
 case "16d0a23e-c09c-407d-a14a-ad058fdd0ca1":reference = "ACPI"; break;
 case "26a2481e-4424-46a2-9943-cc4039ead8f8":reference = "S3Save"; break;
 case "efd652cc-0e99-40f0-96c0-e08c089070fc":reference = "S3Restore"; break;
 case "8c783970-f02a-4a4d-af09-8797a51eec8d":reference = "PowerManagement"; break;
 case "299141bb-211a-48a5-92c0-6f9a0a3a006e0":reference = "PowerManagement-ACPI-table"; break;
 case "2df10014-cf21-4280-8c3f-e539b8ee5150":reference = "PpmPolicyInitDxe"; break;
 case "4b680e2d-0d63-4f62-b930-7ae995b9b3a3":reference = "SmBusDxe"; break;
 // SMM handlers
 case "4a37320b-3fb3-4365-9730-9e89c600395d":reference = "SmmDispatcher"; break;
 case "753630c9-fae5-47a9-bbbf-88d621cd7282":reference = "SmmChildDispatcher"; break;
 case "be216ba8-38c4-4535-a6ca-5dca5b43addf":reference = "SmiVariable"; break;
 case "a56897a1-a77f-4600-84db-22b0a801fa9a":reference = "SmmRuntime"; break;
 case "d2596f82-f0e1-49fa-95bc-62012c795728":reference = "SmmBase Data"; break;
 case "69009842-63f2-43db-964b-efad1c39ec85":reference = "SmmBase Data"; break;
 case "d0632c90-afd7-4492-b186-257c63143c61":reference = "SmmBase"; break;
 case "7e2d983f-f703-4a29-9761-77b51f5354ed":reference = "SmmCommunicate"; break;
 // CMOS and NVRAM handlers
 case "6869c5b3-ac8d-4973-8b37-e354dbf34add":reference = "CmosManagerSmm"; break;
 case "842a454a-75e5-408b-8b1c-36420e4e3f21":reference = "NvramSmi"; break;
 case "5446c293-339b-47cd-b719-585de39408cc":reference = "PostReport"; break;
 case "71ca9ca1-325d-4bfe-afa3-2ec5c94a8680":reference = "DmAcpi"; break;
 case "cef68c66-06ab-4fb3-a3ed-5ffa885b5725":reference = "SMBiosBoard"; break;
 case "b13edd38-684c-41ed-a305-d7b7e32497df":reference = "SMBios64"; break;
 case "ded7956d-7e20-4f20-91a1-190439b04d5b":reference = "SmbiosGetFlashData64"; break;
 case "daf4bf89-ce71-4917-b522-c89d32fbc59f":reference = "SmbiosStaticData"; break;
 // Apple GUIDS
 case "48465300-0000-11aa-aa11-00306543ecac":
                reference = "Apple Hierarchical File System Plus (HFS+) partition "; break;
 case "7c3457ef-0000-11aa-aa11-00306543ecac":reference = "Apple APFS container"; break;
 case "55465300-0000-11aa-aa11-00306543ecac":reference = "Apple UFS container"; break;
 case "52414944-0000-11aa-aa11-00306543ecac":reference = "Apple RAID partition"; break;
 case "4c616265-6c00-11aa-aa11-00306543ecac":reference = "Apple Label"; break;
 case "53746f72-6167-11aa-aa11-00306543ecac":reference = "Apple Core Storage Container"; break;
 case "6a898cc3-1dd2-11b2-99a6-080020736631":reference = "ZFS Partition"; break;

 // Chrome OS GUIDS
 case "2568845d-2332-4675-bc39-8fa5a4748d15":reference = "Chrome OS kernel "; break;
 case "3cb8e202-3b7e-47dd-8a3c-7ff2a13cfcec":reference = "Chrome OS rootfs "; break;
 case "2e0a753d-9e48-43b0-8337-b15192cb1b5e":reference = "Chrome OS future use "; break;

 // Android GUIDS
 case "fe3a2a5d-4f32-41a7-b725-accc3285a309":reference = "Android Bootloader"; break;
 case "114eaffe-1552-4022-b26e-9b053604cf84":reference = "Android Bootloader 2"; break;
 case "49a4d17f-93a3-45c1-a0de-f50b2ebe2599":reference = "Android Boot"; break;
 case "4177c722-9e92-4aab-8644-43502bfd5506":reference = "Android Recovery"; break;
 case "38f428e6-d326-425d-9140-6e0ea133647c":reference = "Android System"; break;
 case "bd59408b-4514-490d-bf12-9878d963f378":reference = "Android Config"; break;
 case "8f68cc74-c5e5-48da-be91-a0c8c15e9c80":reference = "Android Factory"; break;
 case "ac6d7924-eb71-4df8-b48d-e267b27148ff":reference = "Android OEM"; break;

 // MISC GUIDs
 case "5023b95c-db26-429b-a648-bd47664c8012":reference = "Built-in EFI Shell"; break;
 case "610a0202-d308-00c4-0000-000004300d06":reference = "Mystery UUID"; break;
 case "00000000-0000-0000-0000-000000000000":reference = "Empty UUID"; break;

 default: reference = "Unknown GUID reference";
  }
 return reference;
 }

/**
 * Returns a string of the entity that the UUID represents.
 * Contains a Vendor String lookup on the UUID.
 * @return UUID description.
 */
public String toString() {
   String guidinfo = "";
   guidinfo = uuid.toString() + " : " + getVendorTableReference();
   return guidinfo;
 }

/**
 * Returns a string of the entity that the UUID represents.
 * Does not contain a vendor lookup on the UUID.
 * @return UUID description.
 */
public String toStringNoLookup() {
 return  uuid.toString();
}
/**
 * Returns a string of the entity that the UUID represents.
 * Does not contain a vendor lookup on the UUID.
 * @param guid byte array holding the guid data.
 * @return true if the UUID has a valid structure.
 */
public static boolean isValidUUID(final byte[] guid) {
 boolean valid = false;
 UUID tmpUuid = processGuid(guid);
 if (tmpUuid.toString().length() != 0)  {
       valid = true;
      }
    return (valid);
 }

/**
 * Checks to see if the uuid is the test or Empty UUID ("00000000-0000-0000-0000-000000000000").
 * @return true if the uuid is the Empty UUID, false if not
 */
public boolean isEmptyUUID() {
 return uuid.toString().equals("00000000-0000-0000-0000-000000000000");
 }

/**
 * Checks to see if the uuid is the Empty UUID or an unknown.
 * @return true if the uuid is the Empty UUID, false if not
 */
public boolean isUnknownUUID() {
   if (getVendorTableReference().equals("Unknown GUID reference")) {
       return true;
      }
       return false;
 }

/**
 * Retrieves the timestamp within a time based GUID.
 * @param uuid  uuid object
 * @return long representing the time stamp from the GUID
 */
public long getTimeFromUUID(final UUID uuid) {
        return (uuid.timestamp() - UUID_EPOCH_INTERVALS) / UUID_EPOCH_DIVISOR;
   }
}
