---
title: Example Output
---

# Event Log Example Output

<!-- 

[TPM Event Log (TpmLog.bin) :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/blob/main/tools/tcg_rim_tool/src/test/resources/TpmLog.bin){:target="_blank"}

[TPM Event Log (TPMLog_Altered.bin) :fontawesome-solid-external-link:](https://github.com/nsacyber/HIRS/blob/main/tools/tcg_rim_tool/src/test/resources/TPMLog_Altered.bin){:target="_blank"}

-->

``` shell

Event Log follows the "Crypto Agile" format and has 89 events:

Event# 0: Index PCR[0]
Event Type: 0x3 EV_NO_ACTION
Event Content:
   Signature = Spec ID Event03 :    Log format is Crypto Agile
   Platform Profile Specification version = 02.00 using errata version 00
   Algorithm list:
      0: TPM_ALG_SHA256
digest (SHA-1): 0000000000000000000000000000000000000000

Event# 1: Index PCR[0]
Event Type: 0x7 EV_S_CRTM_CONTENTS
Event Content:
   Boot Guard Measured S-CRTM
digest (TPM_ALG_SHA256): e8e7de94b0d9b8abe8993659e88ea755becd0e49ba0bcc6e69c8a98242225edd

Event# 2: Index PCR[0]
Event Type: 0x8 EV_S_CRTM_VERSION
Event Content:
   SCRM Version = 546bfb1e-1d0c-4055-a4ad-4ef4bf17b83a
digest (TPM_ALG_SHA256): d4720b4009438213b803568017f903093f6bea8ab47d283db32b6eabedbbf155

Event# 3: Index PCR[0]
Event Type: 0x1 EV_POST_CODE
Event Content:
   Platform Firmware Blob Address = ff191000 length = 6094848
digest (TPM_ALG_SHA256): 350c23c473ec8aa4d537cd7e1ea167aa022b082082489dad7feae2c0a9a7080d

Event# 4: Index PCR[7]
Event Type: 0x80000001 EV_EFI_VARIABLE_DRIVER_CONFIG
Event Content:
   UEFI Variable Name GUID: 8be4df61-93ca-11d2-aa0d-00e098032b8c : EFI_Global_Variable
   UEFI Unicode Name: SecureBoot
   UEFI Variable Data => 
      Secure Boot is NOT enabled   
digest (TPM_ALG_SHA256): 115aa827dbccfb44d216ad9ecfda56bdea620b860a94bed5b7a27bba1c4d02d8

Event# 5: Index PCR[7]
Event Type: 0x80000001 EV_EFI_VARIABLE_DRIVER_CONFIG
Event Content:
   UEFI Variable Name GUID: 8be4df61-93ca-11d2-aa0d-00e098032b8c : EFI_Global_Variable
   UEFI Unicode Name: PK
   UEFI Variable Data => 
Number of UEFI Signature Lists = 1
UEFI Signature List # 1 of 1: ------------------
   UEFI Signature List Type = a5c059a1-94e4-4aa7-87b5-ab155c2bf072 : EFI_CERT_X509_GUID
   Number of Certs or Hashes in UEFI Signature List = 1
   Cert or Hash # 1 of 1: ------------------
    UEFI Signature Owner = 70564dce-9afc-4ee3-85fc-949649d7e45c : Dell Inc.
      Certificate Serial Number = 50a1bd858ae7b6bc402dca78cdd268a1
      Subject DN = CN=Dell Inc. Platform Key,O=Dell Inc.,L=Round Rock,ST=Texas,C=US
      Issuer DN = CN=Dell Inc. Platform Key,O=Dell Inc.,L=Round Rock,ST=Texas,C=US
      Not Before Date = Wed Jun 01 16:20:07 EDT 2016
      Not After Date = Sun Jun 01 16:30:06 EDT 2031
      Signature Algorithm = SHA256withRSA
      SHA1 Fingerprint =  44:d6:41:ca:ca:08:09:00:23:98:b4:87:7b:8e:98:2e:d2:6f:7b:76

digest (TPM_ALG_SHA256): 2abfe9865a654102acb12f0fefe52dc4d01bce40901410eb3dadaf212700a2b7

Event# 6: Index PCR[7]
Event Type: 0x80000001 EV_EFI_VARIABLE_DRIVER_CONFIG
Event Content:
   UEFI Variable Name GUID: 8be4df61-93ca-11d2-aa0d-00e098032b8c : EFI_Global_Variable
   UEFI Unicode Name: KEK
   UEFI Variable Data => 
Number of UEFI Signature Lists = 2
UEFI Signature List # 1 of 2: ------------------
   UEFI Signature List Type = a5c059a1-94e4-4aa7-87b5-ab155c2bf072 : EFI_CERT_X509_GUID
   Number of Certs or Hashes in UEFI Signature List = 1
   Cert or Hash # 1 of 1: ------------------
    UEFI Signature Owner = 70564dce-9afc-4ee3-85fc-949649d7e45c : Dell Inc.
      Certificate Serial Number = 279bad52bf5dabb24c367742f4ebaccd
      Subject DN = CN=Dell Inc. Key Exchange Key,O=Dell Inc.,L=Round Rock,ST=Texas,C=US
      Issuer DN = CN=Dell Inc. Platform Key,O=Dell Inc.,L=Round Rock,ST=Texas,C=US
      Not Before Date = Wed Jun 01 16:22:48 EDT 2016
      Not After Date = Thu Jun 01 16:32:47 EDT 2023
      Signature Algorithm = SHA256withRSA
      SHA1 Fingerprint =  d4:88:46:08:ae:a4:42:43:3c:f0:6e:5a:21:64:bf:f8:d3:d3:10:d6
UEFI Signature List # 2 of 2: ------------------
   UEFI Signature List Type = a5c059a1-94e4-4aa7-87b5-ab155c2bf072 : EFI_CERT_X509_GUID
   Number of Certs or Hashes in UEFI Signature List = 1
   Cert or Hash # 1 of 1: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Certificate Serial Number = 610ad188000000000003
      Subject DN = CN=Microsoft Corporation KEK CA 2011,O=Microsoft Corporation,L=Redmond,ST=Washington,C=US
      Issuer DN = CN=Microsoft Corporation Third Party Marketplace Root,O=Microsoft Corporation,L=Redmond,ST=Washington,C=US
      Not Before Date = Fri Jun 24 16:41:29 EDT 2011
      Not After Date = Wed Jun 24 16:51:29 EDT 2026
      Signature Algorithm = SHA256withRSA
      SHA1 Fingerprint =  31:59:0b:fd:89:c9:d7:4e:d0:87:df:ac:66:33:4b:39:31:25:4b:30

digest (TPM_ALG_SHA256): 63a525134bfbc242058c0e6b42794f8b1d142d13029a9aa38a3272c5ca2390c5

Event# 7: Index PCR[7]
Event Type: 0x80000001 EV_EFI_VARIABLE_DRIVER_CONFIG
Event Content:
   UEFI Variable Name GUID: d719b2cb-3d3a-4596-a3bc-dad00e67656f : EFI_IMAGE_SECURITY_DATABASE_GUID
   UEFI Unicode Name: db
   UEFI Variable Data => 
Number of UEFI Signature Lists = 4
UEFI Signature List # 1 of 4: ------------------
   UEFI Signature List Type = a5c059a1-94e4-4aa7-87b5-ab155c2bf072 : EFI_CERT_X509_GUID
   Number of Certs or Hashes in UEFI Signature List = 1
   Cert or Hash # 1 of 1: ------------------
    UEFI Signature Owner = 70564dce-9afc-4ee3-85fc-949649d7e45c : Dell Inc.
      Certificate Serial Number = 1d6d9590d4808c8e4f82a0089a16fe39
      Subject DN = CN=Dell Bios DB Key,O=Dell Inc.,L=Round Rock,ST=TX,C=USA
      Issuer DN = CN=Dell Bios Key Exchange Key,O=Dell Inc.,L=Round Rock,ST=TX,C=USA
      Not Before Date = Thu Aug 09 19:04:36 EDT 2018
      Not After Date = Wed Aug 09 19:14:36 EDT 2028
      Signature Algorithm = SHA256withRSA
      SHA1 Fingerprint =  f1:18:70:35:32:b3:70:16:4b:6c:38:72:c0:18:dd:68:a9:fe:8a:2d
UEFI Signature List # 2 of 4: ------------------
   UEFI Signature List Type = a5c059a1-94e4-4aa7-87b5-ab155c2bf072 : EFI_CERT_X509_GUID
   Number of Certs or Hashes in UEFI Signature List = 1
   Cert or Hash # 1 of 1: ------------------
    UEFI Signature Owner = 70564dce-9afc-4ee3-85fc-949649d7e45c : Dell Inc.
      Certificate Serial Number = 22584d1f857b55bc4ce1e6f0bdf7be08
      Subject DN = CN=Dell Bios FW Aux Authority 2018,O=Dell Inc.,L=Round Rock,ST=TX,C=USA
      Issuer DN = CN=Dell Bios Key Exchange Key,O=Dell Inc.,L=Round Rock,ST=TX,C=USA
      Not Before Date = Tue Dec 11 16:59:23 EST 2018
      Not After Date = Mon Dec 11 17:09:23 EST 2028
      Signature Algorithm = SHA256withRSA
      SHA1 Fingerprint =  09:4c:ce:c8:5b:ba:a1:60:b8:ee:02:96:be:a2:4f:d7:c3:a3:eb:08
UEFI Signature List # 3 of 4: ------------------
   UEFI Signature List Type = a5c059a1-94e4-4aa7-87b5-ab155c2bf072 : EFI_CERT_X509_GUID
   Number of Certs or Hashes in UEFI Signature List = 1
   Cert or Hash # 1 of 1: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Certificate Serial Number = 61077656000000000008
      Subject DN = CN=Microsoft Windows Production PCA 2011,O=Microsoft Corporation,L=Redmond,ST=Washington,C=US
      Issuer DN = CN=Microsoft Root Certificate Authority 2010,O=Microsoft Corporation,L=Redmond,ST=Washington,C=US
      Not Before Date = Wed Oct 19 14:41:42 EDT 2011
      Not After Date = Mon Oct 19 14:51:42 EDT 2026
      Signature Algorithm = SHA256withRSA
      SHA1 Fingerprint =  58:0a:6f:4c:c4:e4:b6:69:b9:eb:dc:1b:2b:3e:08:7b:80:d0:67:8d
UEFI Signature List # 4 of 4: ------------------
   UEFI Signature List Type = a5c059a1-94e4-4aa7-87b5-ab155c2bf072 : EFI_CERT_X509_GUID
   Number of Certs or Hashes in UEFI Signature List = 1
   Cert or Hash # 1 of 1: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Certificate Serial Number = 6108d3c4000000000004
      Subject DN = CN=Microsoft Corporation UEFI CA 2011,O=Microsoft Corporation,L=Redmond,ST=Washington,C=US
      Issuer DN = CN=Microsoft Corporation Third Party Marketplace Root,O=Microsoft Corporation,L=Redmond,ST=Washington,C=US
      Not Before Date = Mon Jun 27 17:22:45 EDT 2011
      Not After Date = Sat Jun 27 17:32:45 EDT 2026
      Signature Algorithm = SHA256withRSA
      SHA1 Fingerprint =  46:de:f6:3b:5c:e6:1c:f8:ba:0d:e2:e6:63:9c:10:19:d0:ed:14:f3

digest (TPM_ALG_SHA256): 58a0c07f6f04c9d61afe2b70e99bc3caf569856119bd543cebae7dcd9ac2c7d1

Event# 8: Index PCR[7]
Event Type: 0x80000001 EV_EFI_VARIABLE_DRIVER_CONFIG
Event Content:
   UEFI Variable Name GUID: d719b2cb-3d3a-4596-a3bc-dad00e67656f : EFI_IMAGE_SECURITY_DATABASE_GUID
   UEFI Unicode Name: dbx
   UEFI Variable Data => 
Number of UEFI Signature Lists = 3
UEFI Signature List # 1 of 3: ------------------
   UEFI Signature List Type = a5c059a1-94e4-4aa7-87b5-ab155c2bf072 : EFI_CERT_X509_GUID
   Number of Certs or Hashes in UEFI Signature List = 1
   Cert or Hash # 1 of 1: ------------------
    UEFI Signature Owner = 00000000-0000-0000-0000-000000000000 : Empty UUID
      Certificate Serial Number = 610c6a19000000000004
      Subject DN = CN=Microsoft Windows PCA 2010,O=Microsoft Corporation,L=Redmond,ST=Washington,C=US
      Issuer DN = CN=Microsoft Root Certificate Authority 2010,O=Microsoft Corporation,L=Redmond,ST=Washington,C=US
      Not Before Date = Tue Jul 06 16:40:23 EDT 2010
      Not After Date = Sun Jul 06 16:50:23 EDT 2025
      Signature Algorithm = SHA256withRSA
      SHA1 Fingerprint =  c0:13:86:a9:07:49:64:04:f2:76:c3:c1:85:3a:bf:4a:52:74:af:88
UEFI Signature List # 2 of 3: ------------------
   UEFI Signature List Type = c1c41626-504c-4092-aca9-41f936934328 : EFI_CERT_SHA256_GUID
   Number of Certs or Hashes in UEFI Signature List = 77
   Cert or Hash # 1 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 80b4d96931bf0d02fd91a61e19d14f1da452e66db2408ca8604d411f92659f0a
   Cert or Hash # 2 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = f52f83a3fa9cfbd6920f722824dbe4034534d25b8507246b3b957dac6e1bce7a
   Cert or Hash # 3 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = c5d9d8a186e2c82d09afaa2a6f7f2e73870d3e64f72c4e08ef67796a840f0fbd
   Cert or Hash # 4 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 363384d14d1f2e0b7815626484c459ad57a318ef4396266048d058c5a19bbf76
   Cert or Hash # 5 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 1aec84b84b6c65a51220a9be7181965230210d62d6d33c48999c6b295a2b0a06
   Cert or Hash # 6 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = e6ca68e94146629af03f69c2f86e6bef62f930b37c6fbcc878b78df98c0334e5
   Cert or Hash # 7 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = c3a99a460da464a057c3586d83cef5f4ae08b7103979ed8932742df0ed530c66
   Cert or Hash # 8 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 58fb941aef95a25943b3fb5f2510a0df3fe44c58c95e0ab80487297568ab9771
   Cert or Hash # 9 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 5391c3a2fb112102a6aa1edc25ae77e19f5d6f09cd09eeb2509922bfcd5992ea
   Cert or Hash # 10 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = d626157e1d6a718bc124ab8da27cbb65072ca03a7b6b257dbdcbbd60f65ef3d1
   Cert or Hash # 11 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = d063ec28f67eba53f1642dbf7dff33c6a32add869f6013fe162e2c32f1cbe56d
   Cert or Hash # 12 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 29c6eb52b43c3aa18b2cd8ed6ea8607cef3cfae1bafe1165755cf2e614844a44
   Cert or Hash # 13 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 90fbe70e69d633408d3e170c6832dbb2d209e0272527dfb63d49d29572a6f44c
   Cert or Hash # 14 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 075eea060589548ba060b2feed10da3c20c7fe9b17cd026b94e8a683b8115238
   Cert or Hash # 15 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 07e6c6a858646fb1efc67903fe28b116011f2367fe92e6be2b36999eff39d09e
   Cert or Hash # 16 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 09df5f4e511208ec78b96d12d08125fdb603868de39f6f72927852599b659c26
   Cert or Hash # 17 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 0bbb4392daac7ab89b30a4ac657531b97bfaab04f90b0dafe5f9b6eb90a06374
   Cert or Hash # 18 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 0c189339762df336ab3dd006a463df715a39cfb0f492465c600e6c6bd7bd898c
   Cert or Hash # 19 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 0d0dbeca6f29eca06f331a7d72e4884b12097fb348983a2a14a0d73f4f10140f
   Cert or Hash # 20 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 0dc9f3fb99962148c3ca833632758d3ed4fc8d0b0007b95b31e6528f2acd5bfc
   Cert or Hash # 21 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 106faceacfecfd4e303b74f480a08098e2d0802b936f8ec774ce21f31686689c
   Cert or Hash # 22 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 174e3a0b5b43c6a607bbd3404f05341e3dcf396267ce94f8b50e2e23a9da920c
   Cert or Hash # 23 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 18333429ff0562ed9f97033e1148dceee52dbe2e496d5410b5cfd6c864d2d10f
   Cert or Hash # 24 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 2b99cf26422e92fe365fbf4bc30d27086c9ee14b7a6fff44fb2f6b9001699939
   Cert or Hash # 25 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 2bbf2ca7b8f1d91f27ee52b6fb2a5dd049b85a2b9b529c5d6662068104b055f8
   Cert or Hash # 26 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 2c73d93325ba6dcbe589d4a4c63c5b935559ef92fbf050ed50c4e2085206f17d
   Cert or Hash # 27 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 2e70916786a6f773511fa7181fab0f1d70b557c6322ea923b2a8d3b92b51af7d
   Cert or Hash # 28 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 306628fa5477305728ba4a467de7d0387a54f569d3769fce5e75ec89d28d1593
   Cert or Hash # 29 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 3608edbaf5ad0f41a414a1777abf2faf5e670334675ec3995e6935829e0caad2
   Cert or Hash # 30 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 3841d221368d1583d75c0a02e62160394d6c4e0a6760b6f607b90362bc855b02
   Cert or Hash # 31 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 3fce9b9fdf3ef09d5452b0f95ee481c2b7f06d743a737971558e70136ace3e73
   Cert or Hash # 32 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 4397daca839e7f63077cb50c92df43bc2d2fb2a8f59f26fc7a0e4bd4d9751692
   Cert or Hash # 33 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 47cc086127e2069a86e03a6bef2cd410f8c55a6d6bdb362168c31b2ce32a5adf
   Cert or Hash # 34 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 518831fe7382b514d03e15c621228b8ab65479bd0cbfa3c5c1d0f48d9c306135
   Cert or Hash # 35 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 5ae949ea8855eb93e439dbc65bda2e42852c2fdf6789fa146736e3c3410f2b5c
   Cert or Hash # 36 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 6b1d138078e4418aa68deb7bb35e066092cf479eeb8ce4cd12e7d072ccb42f66
   Cert or Hash # 37 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 6c8854478dd559e29351b826c06cb8bfef2b94ad3538358772d193f82ed1ca11
   Cert or Hash # 38 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 6f1428ff71c9db0ed5af1f2e7bbfcbab647cc265ddf5b293cdb626f50a3a785e
   Cert or Hash # 39 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 71f2906fd222497e54a34662ab2497fcc81020770ff51368e9e3d9bfcbfd6375
   Cert or Hash # 40 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 726b3eb654046a30f3f83d9b96ce03f670e9a806d1708a0371e62dc49d2c23c1
   Cert or Hash # 41 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 72e0bd1867cf5d9d56ab158adf3bddbc82bf32a8d8aa1d8c5e2f6df29428d6d8
   Cert or Hash # 42 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 7827af99362cfaf0717dade4b1bfe0438ad171c15addc248b75bf8caa44bb2c5
   Cert or Hash # 43 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 81a8b965bb84d3876b9429a95481cc955318cfaa1412d808c8a33bfd33fff0e4
   Cert or Hash # 44 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 82db3bceb4f60843ce9d97c3d187cd9b5941cd3de8100e586f2bda5637575f67
   Cert or Hash # 45 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 895a9785f617ca1d7ed44fc1a1470b71f3f1223862d9ff9dcc3ae2df92163daf
   Cert or Hash # 46 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 8ad64859f195b5f58dafaa940b6a6167acd67a886e8f469364177221c55945b9
   Cert or Hash # 47 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 8bf434b49e00ccf71502a2cd900865cb01ec3b3da03c35be505fdf7bd563f521
   Cert or Hash # 48 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 8d8ea289cfe70a1c07ab7365cb28ee51edd33cf2506de888fbadd60ebf80481c
   Cert or Hash # 49 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 9998d363c491be16bd74ba10b94d9291001611736fdca643a36664bc0f315a42
   Cert or Hash # 50 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 9e4a69173161682e55fde8fef560eb88ec1ffedcaf04001f66c0caf707b2b734
   Cert or Hash # 51 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = a6b5151f3655d3a2af0d472759796be4a4200e5495a7d869754c4848857408a7
   Cert or Hash # 52 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = a7f32f508d4eb0fead9a087ef94ed1ba0aec5de6f7ef6ff0a62b93bedf5d458d
   Cert or Hash # 53 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = ad6826e1946d26d3eaf3685c88d97d85de3b4dcb3d0ee2ae81c70560d13c5720
   Cert or Hash # 54 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = aeebae3151271273ed95aa2e671139ed31a98567303a332298f83709a9d55aa1
   Cert or Hash # 55 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = afe2030afb7d2cda13f9fa333a02e34f6751afec11b010dbcd441fdf4c4002b3
   Cert or Hash # 56 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = b54f1ee636631fad68058d3b0937031ac1b90ccb17062a391cca68afdbe40d55
   Cert or Hash # 57 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = b8f078d983a24ac433216393883514cd932c33af18e7dd70884c8235f4275736
   Cert or Hash # 58 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = b97a0889059c035ff1d54b6db53b11b9766668d9f955247c028b2837d7a04cd9
   Cert or Hash # 59 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = bc87a668e81966489cb508ee805183c19e6acd24cf17799ca062d2e384da0ea7
   Cert or Hash # 60 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = c409bdac4775add8db92aa22b5b718fb8c94a1462c1fe9a416b95d8a3388c2fc
   Cert or Hash # 61 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = c617c1a8b1ee2a811c28b5a81b4c83d7c98b5b0c27281d610207ebe692c2967f
   Cert or Hash # 62 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = c90f336617b8e7f983975413c997f10b73eb267fd8a10cb9e3bdbfc667abdb8b
   Cert or Hash # 63 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = cb6b858b40d3a098765815b592c1514a49604fafd60819da88d7a76e9778fef7
   Cert or Hash # 64 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = ce3bfabe59d67ce8ac8dfd4a16f7c43ef9c224513fbc655957d735fa29f540ce
   Cert or Hash # 65 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = d8cbeb9735f5672b367e4f96cdc74969615d17074ae96c724d42ce0216f8f3fa
   Cert or Hash # 66 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = e92c22eb3b5642d65c1ec2caf247d2594738eebb7fb3841a44956f59e2b0d1fa
   Cert or Hash # 67 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = fddd6e3d29ea84c7743dad4a1bdbc700b5fec1b391f932409086acc71dd6dbd8
   Cert or Hash # 68 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = fe63a84f782cc9d3fcf2ccf9fc11fbd03760878758d26285ed12669bdc6e6d01
   Cert or Hash # 69 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = fecfb232d12e994b6d485d2c7167728aa5525984ad5ca61e7516221f079a1436
   Cert or Hash # 70 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = ca171d614a8d7e121c93948cd0fe55d39981f9d11aa96e03450a415227c2c65b
   Cert or Hash # 71 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 55b99b0de53dbcfe485aa9c737cf3fb616ef3d91fab599aa7cab19eda763b5ba
   Cert or Hash # 72 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 77dd190fa30d88ff5e3b011a0ae61e6209780c130b535ecb87e6f0888a0b6b2f
   Cert or Hash # 73 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = c83cb13922ad99f560744675dd37cc94dcad5a1fcba6472fee341171d939e884
   Cert or Hash # 74 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 3b0287533e0cc3d0ec1aa823cbf0a941aad8721579d1c499802dd1c3a636b8a9
   Cert or Hash # 75 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 939aeef4f5fa51e23340c3f2e49048ce8872526afdf752c3a7f3a3f2bc9f6049
   Cert or Hash # 76 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 64575bd912789a2e14ad56f6341f52af6bf80cf94400785975e9f04e2d64d745
   Cert or Hash # 77 of 77: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 45c7c8ae750acfbb48fc37527d6412dd644daed8913ccd8a24c94d856967df8e
UEFI Signature List # 3 of 3: ------------------
   UEFI Signature List Type = c1c41626-504c-4092-aca9-41f936934328 : EFI_CERT_SHA256_GUID
   Number of Certs or Hashes in UEFI Signature List = 6
   Cert or Hash # 1 of 6: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 81d8fb4c9e2e7a8225656b4b8273b7cba4b03ef2e9eb20e0a0291624eca1ba86
   Cert or Hash # 2 of 6: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = b92af298dc08049b78c77492d6551b710cd72aada3d77be54609e43278ef6e4d
   Cert or Hash # 3 of 6: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = e19dae83c02e6f281358d4ebd11d7723b4f5ea0e357907d5443decc5f93c1e9d
   Cert or Hash # 4 of 6: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 39dbc2288ef44b5f95332cb777e31103e840dba680634aa806f5c9b100061802
   Cert or Hash # 5 of 6: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 32f5940ca29dd812a2c145e6fc89646628ffcc7c7a42cae512337d8d29c40bbd
   Cert or Hash # 6 of 6: ------------------
    UEFI Signature Owner = 77fa9abd-0359-4d32-bd60-28f4e78f784b : Microsoft Inc.
      Binary Hash = 10d45fcba396aef3153ee8f6ecae58afe8476a280a2026fc71f6217dcf49ba2f

digest (TPM_ALG_SHA256): b527b1547a0236117255d711663988af014fdda069ee2a2aa04198730087c0e3

Event# 9: Index PCR[7]
Event Type: 0x4 EV_SEPARATOR
digest (TPM_ALG_SHA256): df3f619804a92fdb4057192dc43dd748ea778adc52bc498ce80524c014b81119

Event# 10: Index PCR[6]
Event Type: 0xc EV_COMPACT_HASH
Event Content:
   Dell Configuration Information 1
digest (TPM_ALG_SHA256): a7d70b945cc5a84d9dfbe339cef92245e5fe514141704521cc30f780882fc592

Event# 11: Index PCR[6]
Event Type: 0xc EV_COMPACT_HASH
Event Content:
   Dell Configuration Information 1
digest (TPM_ALG_SHA256): 6d238432d473120f6a15be7bd9410517a562e94c06fbe52d0a5e2951d3fe45ee

Event# 12: Index PCR[6]
Event Type: 0xc EV_COMPACT_HASH
Event Content:
   Dell Configuration Information 2
digest (TPM_ALG_SHA256): b4e57c0255fdc4a6f0227449e3cc0066eda457bee40e672cc45f65b7834dc4c2

Event# 13: Index PCR[1]
Event Type: 0x80000009 EV_EFI_HANDOFF_TABLES
Event Content:
   Number of UEFI_CONFIGURATION_TABLEs = 1
      Table 0:
        GUID = 3ff916f2-6220-446f-8d98-bf08fe7ccb9f : Unknown GUID reference
        UEFI industry standard table type = Unknown GUID reference
        VendorTable 0 address: 188cfa6400000000

digest (TPM_ALG_SHA256): 0f57271e82d06cab06e58b458ad63bb406d375525694b1ee692fafb341bd7431

Event# 14: Index PCR[1]
Event Type: 0x80000009 EV_EFI_HANDOFF_TABLES
Event Content:
   Number of UEFI_CONFIGURATION_TABLEs = 1
      Table 0:
        GUID = 3ff916f2-6220-446f-8d98-bf08fe7ccb9f : Unknown GUID reference
        UEFI industry standard table type = Unknown GUID reference
        VendorTable 0 address: 18affa6400000000

digest (TPM_ALG_SHA256): 9c844a8fe22e6309ce1b358b0066a1c9cf0608b563b291563c6f388c3a3149ff

Event# 15: Index PCR[4]
Event Type: 0x80000003 EV_EFI_BOOT_SERVICES_APPLICATION
Event Content:
   Image info:
      Image physical address = 18d0d46400000000
      Image length = 72200
      Image link time address = 18d0d46400000000
      Device path length = 0
      No uefi device paths were specified

digest (TPM_ALG_SHA256): 522944324bb55c06f770c2757d962e562d9714d6041b6ba1a93041dd4cf9a8c7

Event# 16: Index PCR[1]
Event Type: 0xa EV_PLATFORM_CONFIG_FLAGS 

digest (TPM_ALG_SHA256): 5e8cb7cff06aa91fc8cdc348cd270ce541b298feb384b385e844ef3afbb75ab1

Event# 17: Index PCR[0]
Event Type: 0x4 EV_SEPARATOR
digest (TPM_ALG_SHA256): df3f619804a92fdb4057192dc43dd748ea778adc52bc498ce80524c014b81119

Event# 18: Index PCR[1]
Event Type: 0x4 EV_SEPARATOR
digest (TPM_ALG_SHA256): df3f619804a92fdb4057192dc43dd748ea778adc52bc498ce80524c014b81119

Event# 19: Index PCR[2]
Event Type: 0x4 EV_SEPARATOR
digest (TPM_ALG_SHA256): df3f619804a92fdb4057192dc43dd748ea778adc52bc498ce80524c014b81119

Event# 20: Index PCR[3]
Event Type: 0x4 EV_SEPARATOR
digest (TPM_ALG_SHA256): df3f619804a92fdb4057192dc43dd748ea778adc52bc498ce80524c014b81119

Event# 21: Index PCR[4]
Event Type: 0x4 EV_SEPARATOR
digest (TPM_ALG_SHA256): df3f619804a92fdb4057192dc43dd748ea778adc52bc498ce80524c014b81119

Event# 22: Index PCR[5]
Event Type: 0x4 EV_SEPARATOR
digest (TPM_ALG_SHA256): df3f619804a92fdb4057192dc43dd748ea778adc52bc498ce80524c014b81119

Event# 23: Index PCR[6]
Event Type: 0x4 EV_SEPARATOR
digest (TPM_ALG_SHA256): df3f619804a92fdb4057192dc43dd748ea778adc52bc498ce80524c014b81119

Event# 24: Index PCR[1]
Event Type: 0x80000002 EV_EFI_VARIABLE_BOOT
Event Content:
   UEFI Variable Name GUID: 8be4df61-93ca-11d2-aa0d-00e098032b8c : EFI_Global_Variable
   UEFI Unicode Name: BootOrder
   UEFI Variable Data => 
      BootOrder = Boot0001 Boot0000 Boot0002 Boot0003 
digest (TPM_ALG_SHA256): 6408b1ed41b7a7530f467d7cd0c908a95442c6f1228ea903f219a561395aebb3

Event# 25: Index PCR[1]
Event Type: 0x80000002 EV_EFI_VARIABLE_BOOT
Event Content:
   UEFI Variable Name GUID: 8be4df61-93ca-11d2-aa0d-00e098032b8c : EFI_Global_Variable
   UEFI Unicode Name: Boot0001
   UEFI Variable Data => 
      EFI Load Option = Rocky Linux
      Media Device Path:
        Sub Type = Hard Drive
        Partition Number = 01000000
        Partition Start = 0008000000000000
        Partition Size = 00c0120000000000
        Partition Signature = 3a81effe-11ea-440c-be4f-cb731ca60557 : Unknown GUID reference
        Partition Format = GUID Partition Table      Media Device Path:
        Sub Type = File Path
        File Path = \EFI\rocky\shimx64.efi
digest (TPM_ALG_SHA256): 276bfbdad2547855aba88ff08ab88b327e3a3129d32a0fd43fff7a8a9133da54

Event# 26: Index PCR[1]
Event Type: 0x80000002 EV_EFI_VARIABLE_BOOT
Event Content:
   UEFI Variable Name GUID: 8be4df61-93ca-11d2-aa0d-00e098032b8c : EFI_Global_Variable
   UEFI Unicode Name: Boot0000
   UEFI Variable Data => 
      EFI Load Option = Windows Boot Manager
      Media Device Path:
        Sub Type = Hard Drive
        Partition Number = 01000000
        Partition Start = 0008000000000000
        Partition Size = 0040060000000000
        Partition Signature = 3a81effe-11ea-440c-be4f-cb731ca60557 : Unknown GUID reference
        Partition Format = GUID Partition Table      Media Device Path:
        Sub Type = File Path
        File Path = \EFI\Microsoft\Boot\bootmgfw.efi
digest (TPM_ALG_SHA256): 896e0cd18cacd42fcc288963dbd0558364033318e22401cecef328e55e794ce6

Event# 27: Index PCR[1]
Event Type: 0x80000002 EV_EFI_VARIABLE_BOOT
Event Content:
   UEFI Variable Name GUID: 8be4df61-93ca-11d2-aa0d-00e098032b8c : EFI_Global_Variable
   UEFI Unicode Name: Boot0002
   UEFI Variable Data => 
      EFI Load Option = Onboard NICIPV
      ACPI Device Path:
        Sub Type = ACPI
        _HID = d041030a
        _UID = No _UID exists for this device
      Hardware Device Path:
        Sub Type = PCI
        PCI Function Number = 0x6
        PCI Device Number = 0x1f      UEFI Messaging Device Path Type 11
      UEFI Messaging Device Path Type 12

digest (TPM_ALG_SHA256): bb6785093713b3b5d4efb274945bbd40fab079f5ca832b2b7b19dfcaba670d9e

Event# 28: Index PCR[1]
Event Type: 0x80000002 EV_EFI_VARIABLE_BOOT
Event Content:
   UEFI Variable Name GUID: 8be4df61-93ca-11d2-aa0d-00e098032b8c : EFI_Global_Variable
   UEFI Unicode Name: Boot0003
   UEFI Variable Data => 
      EFI Load Option = Onboard NICIPV
      ACPI Device Path:
        Sub Type = ACPI
        _HID = d041030a
        _UID = No _UID exists for this device
      Hardware Device Path:
        Sub Type = PCI
        PCI Function Number = 0x6
        PCI Device Number = 0x1f      UEFI Messaging Device Path Type 11
      UEFI Messaging Device Path Type 13

digest (TPM_ALG_SHA256): 7ca34bc4c349fa59a87a3c612add5429e86cf1d6645685ba0cc6ee4cadb19c7e

Event# 29: Index PCR[5]
Event Type: 0x80000006 EV_EFI_GPT_EVENT
Event Content:
GPT Header Signature = 4546492050415254 : Number of Partitions = 3
  Partition 0 information
     Partition Name        : EFI System Partition
     Partition Type GUID   : c12a7328-f81f-11d2-ba4b-00a0c93ec93b : EFI System Partition
     Unique Partition GUID : 3a81effe-11ea-440c-be4f-cb731ca60557
     Attributes            : 0000000000000000
  Partition 1 information
     Partition Name        : 
     Partition Type GUID   : 0fc63daf-8483-4772-8e79-3d69d8477de4 : Linux filesystem data
     Unique Partition GUID : 998dd11a-e2aa-4559-9348-91a275ccc482
     Attributes            : 0000000000000000
  Partition 2 information
     Partition Name        : 
     Partition Type GUID   : e6d6d379-f507-44c2-a23c-238f2a3df928 : Logical Volume Manager (LVM) partition
     Unique Partition GUID : fef31fe2-8541-4c6f-a030-551cea150d96
     Attributes            : 0000000000000000
digest (TPM_ALG_SHA256): c5a582068dabc0c71eb01d30960ada4ec5bf58d572ea9a6d2bde68ff75fe5fe2

Event# 30: Index PCR[1]
Event Type: 0x80000009 EV_EFI_HANDOFF_TABLES
Event Content:
   Number of UEFI_CONFIGURATION_TABLEs = 1
      Table 0:
        GUID = eb9d2d31-2d88-11d3-9a16-0090273fc14d : SMBIOS_TABLE_GUID
        UEFI industry standard table type = SMBIOS_TABLE_GUID
        VendorTable 0 address: 30000f0000000000

digest (TPM_ALG_SHA256): cbf1aeb8468ff9053c8d85afadefe478b2b068f84125afc814f061241b000041

Event# 31: Index PCR[4]
Event Type: 0x80000003 EV_EFI_BOOT_SERVICES_APPLICATION
Event Content:
   Image info:
      Image physical address = 1880c76400000000
      Image length = 944024
      Image link time address = 1880c76400000000
      Device path length = 136
      ACPI Device Path:
        Sub Type = ACPI
        _HID = d041030a
        _UID = No _UID exists for this device
      Hardware Device Path:
        Sub Type = PCI
        PCI Function Number = 0x0
        PCI Device Number = 0x1d      Hardware Device Path:
        Sub Type = PCI
        PCI Function Number = 0x0
        PCI Device Number = 0x0      Messaging Device Path:
        Sub Type = NVM
        NVM Express Namespace = 010000008ce38e05002416e104012a00      Media Device Path:
        Sub Type = Hard Drive
        Partition Number = 01000000
        Partition Start = 0008000000000000
        Partition Size = 00c0120000000000
        Partition Signature = 3a81effe-11ea-440c-be4f-cb731ca60557 : Unknown GUID reference
        Partition Format = GUID Partition Table      Media Device Path:
        Sub Type = File Path
        File Path = \EFI\rocky\shimx64.efi

digest (TPM_ALG_SHA256): 08de9c6e8733af2ef9c36ebd59a760245925452343946a3b3e7f7aefff5cadbe

Event# 32: Index PCR[14]
Event Type: 0xd EV_IPL
Event Content:
  "MokList"
digest (TPM_ALG_SHA256): fb0322051fc45bb49736ababff1d77237d3ada5da9e3991889cce8c3afc0b115

Event# 33: Index PCR[14]
Event Type: 0xd EV_IPL
Event Content:
  "MokListX"
digest (TPM_ALG_SHA256): 8d8a3aae50d5d25838c95c034aadce7b548c9a952eb7925e366eda537c59c3b0

Event# 34: Index PCR[7]
Event Type: 0x800000e0 EV_EFI_VARIABLE_AUTHORITY
Event Content:
   UEFI Variable Name GUID: 605dab50-e046-4300-abb6-3dd810dd8b23 : RH_Shim
   UEFI Unicode Name: SbatLevel
   UEFI Variable Data => 
      Code does not yet process this Uefi Variable

digest (TPM_ALG_SHA256): 922e939a5565798a5ef12fe09d8b49bf951a8e7f89a0cca7a51636693d41a34d

Event# 35: Index PCR[7]
Event Type: 0x800000e0 EV_EFI_VARIABLE_AUTHORITY
Event Content:
   UEFI Variable Name GUID: 605dab50-e046-4300-abb6-3dd810dd8b23 : RH_Shim
   UEFI Unicode Name: MokListTrusted
   UEFI Variable Data => 
      Code does not yet process this Uefi Variable

digest (TPM_ALG_SHA256): 5f62a2107fa11ce0485fd252d2e6c603cb8ed075861f9513bfed0a26bf6ed62b

Event# 36: Index PCR[14]
Event Type: 0xd EV_IPL
Event Content:
  "MokListTrusted"
digest (TPM_ALG_SHA256): 4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a

Event# 37: Index PCR[4]
Event Type: 0x80000003 EV_EFI_BOOT_SERVICES_APPLICATION
Event Content:
   Image info:
      Image physical address = 18309e5900000000
      Image length = 2214728
      Image link time address = 18309e5900000000
      Device path length = 54
      Media Device Path:
        Sub Type = File Path
        File Path = \EFI\rocky\grubx64.efi

digest (TPM_ALG_SHA256): fc56055339d431114ba12d7c927ed57beaf3092dd702116b0416e8cf6f2c91df

Event# 38: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd set pager=1"
digest (TPM_ALG_SHA256): ba96cd80100b0df12232472c34bcbccc6ccfa1bc7e5701182f3d219041c33ac5

Event# 39: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ -f (hd0,gpt1)/EFI/rocky/grubenv ]"
digest (TPM_ALG_SHA256): 3ddcf98d713970c986993155419f9f40e5c4d0c4523c7ffaab1f8ab9d42054a1

Event# 40: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd load_env -f (hd0,gpt1)/EFI/rocky/grubenv"
digest (TPM_ALG_SHA256): b5a8c67a512d36546f44dc6fc8de4b8a6117f85404a8c8c9fe6a370be698f6ef

Event# 41: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [  ]"
digest (TPM_ALG_SHA256): bbdfda09475c0bb2518c485178ee77b507a8eb65365ba50232dbe1d1b73f8464

Event# 42: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd set default=745c882dbfb54171b5b5dca4d19564ab-4.18.0-513.9.1.el8_9.x86_64"
digest (TPM_ALG_SHA256): 7f354bcb96d9bb7af4ecaf74d0d25577bcfbaf7898abc69043bfca3fe7e620d6

Event# 43: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ xy = xy ]"
digest (TPM_ALG_SHA256): 1321dd86a5404e69bab0c496e5a3d49db01b95440422d8be2a07e1db6c4a802a

Event# 44: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd menuentry_id_option=--id"
digest (TPM_ALG_SHA256): d46b5d38520ab3a251440f3706a1f0ca433a64eadc2b0fee5578d7e8fd676072

Event# 45: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd export menuentry_id_option"
digest (TPM_ALG_SHA256): c1058a7a87f51dba73500b5a17e939a7ca82f398d549b03c26f6ef5eec2a1add

Event# 46: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [  ]"
digest (TPM_ALG_SHA256): bbdfda09475c0bb2518c485178ee77b507a8eb65365ba50232dbe1d1b73f8464

Event# 47: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd terminal_output console"
digest (TPM_ALG_SHA256): 73e60eaf5e1e14a313816ab4f3c5ee18ad67ff5a79fe04168c36d41c1574b1d7

Event# 48: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ xy = xy ]"
digest (TPM_ALG_SHA256): 1321dd86a5404e69bab0c496e5a3d49db01b95440422d8be2a07e1db6c4a802a

Event# 49: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd set timeout_style=menu"
digest (TPM_ALG_SHA256): d25ebe40c2d37067bbda2c284cc9c103652f12986e27035a632c0c075bed0e61

Event# 50: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd set timeout=5"
digest (TPM_ALG_SHA256): 8669c965de1e7cabd1ed2a8f119b7dca326b97223fd8f8bf2fc661973a41a13b

Event# 51: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd set tuned_params="
digest (TPM_ALG_SHA256): dc5375553219cbe42944a464f0b8c6849b909102c5b420130bcfbb83d93668a8

Event# 52: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd set tuned_initrd="
digest (TPM_ALG_SHA256): adeef081abbfd33b8904e9eefe6f2199d50d8c46e251a8b7163a3106e8f14bc5

Event# 53: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ -f (hd0,gpt1)/EFI/rocky/user.cfg ]"
digest (TPM_ALG_SHA256): 9c610c916838da455184390d11c2203842b20b07f4e3114d56a0270704f3f6d3

Event# 54: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd insmod increment"
digest (TPM_ALG_SHA256): d95f0f1d766da7bd04afd4024e2ca4be3971b7e5571ee777ecc643530989c52f

Event# 55: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ -n  -a 1 = 0 ]"
digest (TPM_ALG_SHA256): aeb9e7a0b468af0e24a88d30346a5d67f9e496ab7cbf94f5cbb1dfc0067b118a

Event# 56: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd insmod part_gpt"
digest (TPM_ALG_SHA256): 8e4f4629661d793c10d1cd45d700965163d59f9ee84ba1a2c95a2c2126169bcf

Event# 57: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd insmod xfs"
digest (TPM_ALG_SHA256): d4e5266039b3ea07839a59b59af65e2a5785ff122851e2f671924884504208f3

Event# 58: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ xy = xy ]"
digest (TPM_ALG_SHA256): 1321dd86a5404e69bab0c496e5a3d49db01b95440422d8be2a07e1db6c4a802a

Event# 59: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd search --no-floppy --fs-uuid --set=root 8366bdb4-b717-49da-a4c9-531cc458f292"
digest (TPM_ALG_SHA256): e9381418fbabe52ec5d6b930a8aedbb2a1251d65a78554bb0f143141fbffcef2

Event# 60: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd insmod part_gpt"
digest (TPM_ALG_SHA256): 8e4f4629661d793c10d1cd45d700965163d59f9ee84ba1a2c95a2c2126169bcf

Event# 61: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd insmod fat"
digest (TPM_ALG_SHA256): 835dd81d16830a8c3338d0c3a88733b0091104fce0dc39b07301066a6a285929

Event# 62: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ xy = xy ]"
digest (TPM_ALG_SHA256): 1321dd86a5404e69bab0c496e5a3d49db01b95440422d8be2a07e1db6c4a802a

Event# 63: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd search --no-floppy --fs-uuid --set=boot 2351-577A"
digest (TPM_ALG_SHA256): a17e276a3a3b08b8ef27b35bc942c5799938201fcdeaaf15ad550fd9fb32e42e

Event# 64: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ -z root=/dev/mapper/rl-root ro crashkernel=auto resume=/dev/mapper/rl-swap rd.lvm.lv=rl/root rd.lvm.lv=rl/swap  ]"
digest (TPM_ALG_SHA256): 01a9a90fd29271f9d068614e569bb7e385cd4a1e22183dfcf8b2f79e7047daef

Event# 65: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd insmod blscfg"
digest (TPM_ALG_SHA256): a6a34224f4a064d800082fd8131fabefc1cb29cea977eb30fffd7e3016b090b3

Event# 66: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd blscfg"
digest (TPM_ALG_SHA256): a647513778dea15569bf07c7dfa590f67dd8b7bcfd0d702f61afc370d9029c21

Event# 67: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ 1 = 1 -o 0 = 1 ]"
digest (TPM_ALG_SHA256): e29a641bd44f7f15fb4f6431289e97b1546dbd8ba4468ba493d4ec6256e94cb6

Event# 68: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd set menu_hide_ok=1"
digest (TPM_ALG_SHA256): bdcb5ad9be7a2dafc439312d8d7c35451d2f2f1de825e3bc4caa51e851e479c6

Event# 69: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ 1 = 1 ]"
digest (TPM_ALG_SHA256): b88e8f89a71fba7956b264b97213fc0a3846adb200425f991173947209080b85

Event# 70: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd set boot_indeterminate=0"
digest (TPM_ALG_SHA256): 9e7f2cdea610259b0b58473f021dd7972edba11e04b841c01080b2b0243e0fbb

Event# 71: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd set boot_success=0"
digest (TPM_ALG_SHA256): b87b314766971f7a3f97abf0a5fb6647f8c354a61131c21f040b29541f90e82d

Event# 72: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd save_env boot_success boot_indeterminate"
digest (TPM_ALG_SHA256): 111d42e310cb44bdf44d90bfe93a66a91be6ecd4355717f0df23d72c47f34425

Event# 73: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ xy = xy ]"
digest (TPM_ALG_SHA256): 1321dd86a5404e69bab0c496e5a3d49db01b95440422d8be2a07e1db6c4a802a

Event# 74: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [  ]"
digest (TPM_ALG_SHA256): bbdfda09475c0bb2518c485178ee77b507a8eb65365ba50232dbe1d1b73f8464

Event# 75: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [  -a 1 = 1 ]"
digest (TPM_ALG_SHA256): f4b9790a4b3512e6745d4e685d75eaabe14ae117622d7dfcc28167979ad375e7

Event# 76: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd menuentry System setup --id uefi-firmware {
	fwsetup
}"
digest (TPM_ALG_SHA256): 63198e3ef0865894761e004f119faa89e3c92157d88414401512298d2885d503

Event# 77: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ -f (hd0,gpt1)/EFI/rocky/custom.cfg ]"
digest (TPM_ALG_SHA256): a1a3e8bca3da8ceab1a739b7f415971761142269bde5dd869036b64a6316bc1b

Event# 78: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ -z (hd0,gpt1)/EFI/rocky -a -f (hd0,gpt1)/EFI/rocky/custom.cfg ]"
digest (TPM_ALG_SHA256): 9382c51ab2874845f65efef067004bbc0afd459092a49e6cf054b4d34b659741

Event# 79: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd load_video"
digest (TPM_ALG_SHA256): 57866bdb9afbc65defa4f3606983495fc3a136602d3f4ab8aaa7a8b74aa89574

Event# 80: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd [ xy = xy ]"
digest (TPM_ALG_SHA256): 1321dd86a5404e69bab0c496e5a3d49db01b95440422d8be2a07e1db6c4a802a

Event# 81: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd insmod all_video"
digest (TPM_ALG_SHA256): ebcb21d2985d234e5abf6b85418c511016633e58cefc08fbcbfd3ea6e2a48a4e

Event# 82: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd set gfx_payload=keep"
digest (TPM_ALG_SHA256): f490c9c15dcf0e19ae5d235de14927a587984c19310147ffb18eee03e057c546

Event# 83: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd insmod gzio"
digest (TPM_ALG_SHA256): c2550395dcbbec4d074f0a4576fd29ba1304ba71f5d3891c0d86248f9d99c5f4

Event# 84: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd linux (hd0,gpt2)/vmlinuz-4.18.0-513.9.1.el8_9.x86_64 root=/dev/mapper/rl-root ro crashkernel=auto resume=/dev/mapper/rl-swap rd.lvm.lv=rl/root rd.lvm.lv=rl/swap"
digest (TPM_ALG_SHA256): fd7570adc81006cc2d8eab55cd1c40f3d98f0af75d7bec1784bc12018587bbb6

Event# 85: Index PCR[9]
Event Type: 0xd EV_IPL
Event Content:
  "grub_linuxefi Kernel"
digest (TPM_ALG_SHA256): 6f8ade59c5d597354394784a5497628f9eddd44ab49b79eaae8e084d67130b1b

Event# 86: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_kernel_cmdline (hd0,gpt2)/vmlinuz-4.18.0-513.9.1.el8_9.x86_64 root=/dev/mapper/rl-root ro crashkernel=auto resume=/dev/mapper/rl-swap rd.lvm.lv=rl/root rd.lvm.lv=rl/swap"
digest (TPM_ALG_SHA256): 7973d9b69b84de43dabc9fc01148f059f1320215d10ec655ec8e6d9d18dd8593

Event# 87: Index PCR[8]
Event Type: 0xd EV_IPL
Event Content:
  "grub_cmd initrd (hd0,gpt2)/initramfs-4.18.0-513.9.1.el8_9.x86_64.img"
digest (TPM_ALG_SHA256): 926298278a102ff39eb63375b247072aa9eca99ece179df89520047d30e413af

Event# 88: Index PCR[9]
Event Type: 0xd EV_IPL
Event Content:
  "grub_linuxefi Initrd"
digest (TPM_ALG_SHA256): 629fe1e1ef7537eaded716fbd04daea37eb52aa55c0bfdbb2e0af8ee1340e781

```