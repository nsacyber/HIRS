package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.CriteriaModifier;
import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.DBServiceException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.EndorsementCredentialRepository;
import hirs.attestationca.persist.entity.manager.IssuedCertificateRepository;
import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.ComponentIdentifier;
import hirs.attestationca.persist.util.CredentialHelper;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.CertificateStringMapBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.DecoderException;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// note uploading base64 certs, old or new having decode issues check ACA channel

/**
 * Controller for the Certificates list all pages.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request")
public class CertificatePageController extends PageController<NoPageParams> {
    @Autowired(required = false)
    private EntityManager entityManager;

    private CertificateAuthorityCredential certificateAuthorityCredential;
    private final CertificateRepository certificateRepository;
    private final PlatformCertificateRepository platformCertificateRepository;
    private final ComponentResultRepository componentResultRepository;
    private final EndorsementCredentialRepository endorsementCredentialRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;
    private final CACredentialRepository caCredentialRepository;

    private static final String TRUSTCHAIN = "trust-chain";
    private static final String PLATFORMCREDENTIAL = "platform-credentials";
    private static final String ENDORSEMENTCREDENTIAL = "endorsement-key-credentials";
    private static final String ISSUEDCERTIFICATES = "issued-certificates";

    /**
     * Model attribute name used by initPage for the aca cert info.
     */
    static final String ACA_CERT_DATA = "acaCertData";

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param certificateRepository           the general certificate manager
     * @param platformCertificateRepository   the platform credential manager
     * @param componentResultRepository       the component result repo
     * @param endorsementCredentialRepository the endorsement credential manager
     * @param issuedCertificateRepository     the issued certificate manager
     * @param caCredentialRepository          the ca credential manager
     * @param acaCertificate                  the ACA's X509 certificate
     */
    @Autowired
    public CertificatePageController(final CertificateRepository certificateRepository,
                                     final PlatformCertificateRepository platformCertificateRepository,
                                     final ComponentResultRepository componentResultRepository,
                                     final EndorsementCredentialRepository endorsementCredentialRepository,
                                     final IssuedCertificateRepository issuedCertificateRepository,
                                     final CACredentialRepository caCredentialRepository,
                                     final X509Certificate acaCertificate) {
        super(Page.TRUST_CHAIN);
        this.certificateRepository = certificateRepository;
        this.platformCertificateRepository = platformCertificateRepository;
        this.componentResultRepository = componentResultRepository;
        this.endorsementCredentialRepository = endorsementCredentialRepository;
        this.issuedCertificateRepository = issuedCertificateRepository;
        this.caCredentialRepository = caCredentialRepository;

        try {
            certificateAuthorityCredential
                    = new CertificateAuthorityCredential(acaCertificate.getEncoded());
        } catch (IOException ioEx) {
            log.error("Failed to read ACA certificate", ioEx);
        } catch (CertificateEncodingException ceEx) {
            log.error("Error getting encoded ACA certificate", ceEx);
        }
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from
     * redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView();
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param certificateType String containing the certificate type
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from
     * redirect.
     * @return the path for the view and data model for the page.
     */
    @RequestMapping("/{certificateType}")
    public ModelAndView initPage(@PathVariable("certificateType") final String certificateType,
                                 final NoPageParams params, final Model model) {

        ModelAndView mav = null;
        HashMap<String, String> data = new HashMap<>();
        // add page information
        switch (certificateType) {
            case PLATFORMCREDENTIAL:
                mav = getBaseModelAndView(Page.PLATFORM_CREDENTIALS);
                break;
            case ENDORSEMENTCREDENTIAL:
                mav = getBaseModelAndView(Page.ENDORSEMENT_KEY_CREDENTIALS);
                break;
            case ISSUEDCERTIFICATES:
                mav = getBaseModelAndView(Page.ISSUED_CERTIFICATES);
                break;
            case TRUSTCHAIN:
                mav = getBaseModelAndView(Page.TRUST_CHAIN);
                // Map with the ACA certificate information
                data.putAll(CertificateStringMapBuilder.getCertificateAuthorityInformation(
                        certificateAuthorityCredential, this.certificateRepository, this.caCredentialRepository));
                mav.addObject(ACA_CERT_DATA, data);
                break;
            default:
                // send to an error page
                break;
        }

        return mav;
    }


    /**
     * Queries for the list of Certificates and returns a data table response
     * with the records.
     *
     * @param certificateType String containing the certificate type
     * @param input the DataTables search/query parameters
     * @return the data table
     */
    @ResponseBody
    @RequestMapping(value = "/{certificateType}/list",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<? extends Certificate> getTableData(
            @PathVariable("certificateType") final String certificateType,
            final DataTableInput input) {
        log.debug("Handling list request: " + input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();
        log.debug("Ordering on column: " + orderColumnName);

        // check that the alert is not archived and that it is in the specified report
        CriteriaModifier criteriaModifier = new CriteriaModifier() {
            @Override
            public void modify(final CriteriaQuery criteriaQuery) {
                Session session = entityManager.unwrap(Session.class);
                CriteriaBuilder cb = session.getCriteriaBuilder();
                Root<Certificate> rimRoot = criteriaQuery.from(Reference.class);
                criteriaQuery.select(rimRoot).distinct(true).where(cb.isNull(rimRoot.get(Certificate.ARCHIVE_FIELD)));

                // add a device alias if this query includes the device table
                // for getting the device (e.g. device name).
                // use left join, since device may be null. Query will return all
                // Certs of this type, whether it has a Device or not (device field may be null)
                if (hasDeviceTableToJoin(certificateType)) {
//                    criteria.createAlias("device", "device", JoinType.LEFT_OUTER_JOIN);
                }
            }
        };

        int currentPage = input.getStart() / input.getLength();
        Pageable paging = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        // special parsing for platform credential
        // Add the EndorsementCredential for each PlatformCredential based on the
        // serial number. (pc.HolderSerialNumber = ec.SerialNumber)
        if (certificateType.equals(PLATFORMCREDENTIAL)) {
            FilteredRecordsList<PlatformCredential> records = new FilteredRecordsList<>();
            org.springframework.data.domain.Page<PlatformCredential> pagedResult = this.platformCertificateRepository.findByArchiveFlag(false, paging);

            if (pagedResult.hasContent()) {
                records.addAll(pagedResult.getContent());
                records.setRecordsTotal(pagedResult.getContent().size());
            } else {
                records.setRecordsTotal(input.getLength());
            }

            records.setRecordsFiltered(platformCertificateRepository.findByArchiveFlag(false).size());
            EndorsementCredential associatedEC;

            if (!records.isEmpty()) {
                // loop all the platform certificates
                for (int i = 0; i < records.size(); i++) {
                    PlatformCredential pc = (PlatformCredential) records.get(i);
                    // find the EC using the PC's "holder serial number"
                    associatedEC = this.endorsementCredentialRepository
                            .findBySerialNumber(pc.getHolderSerialNumber());

                    if (associatedEC != null) {
                        log.debug("EC ID for holder s/n " + pc
                                .getHolderSerialNumber() + " = " + associatedEC.getId());
                    }

                    pc.setEndorsementCredential(associatedEC);
                }
            }

            log.debug("Returning list of size: " + records.size());
            return new DataTableResponse<>(records, input);
        } else if (certificateType.equals(ENDORSEMENTCREDENTIAL)) {
            FilteredRecordsList<EndorsementCredential> records = new FilteredRecordsList<>();
            org.springframework.data.domain.Page<EndorsementCredential> pagedResult = this.endorsementCredentialRepository.findByArchiveFlag(false, paging);

            if (pagedResult.hasContent()) {
                records.addAll(pagedResult.getContent());
                records.setRecordsTotal(pagedResult.getContent().size());
            } else {
                records.setRecordsTotal(input.getLength());
            }

            records.setRecordsFiltered(endorsementCredentialRepository.findByArchiveFlag(false).size());

            log.debug("Returning list of size: " + records.size());
            return new DataTableResponse<>(records, input);
        } else if (certificateType.equals(TRUSTCHAIN)) {
            FilteredRecordsList<CertificateAuthorityCredential> records = new FilteredRecordsList<>();
            org.springframework.data.domain.Page<CertificateAuthorityCredential> pagedResult = this.caCredentialRepository.findByArchiveFlag(false, paging);

            if (pagedResult.hasContent()) {
                records.addAll(pagedResult.getContent());
                records.setRecordsTotal(pagedResult.getContent().size());
            } else {
                records.setRecordsTotal(input.getLength());
            }

            records.setRecordsFiltered(caCredentialRepository.findByArchiveFlag(false).size());

            log.debug("Returning list of size: " + records.size());
            return new DataTableResponse<>(records, input);
        } else if (certificateType.equals(ISSUEDCERTIFICATES)) {
            FilteredRecordsList<IssuedAttestationCertificate> records = new FilteredRecordsList<>();
            org.springframework.data.domain.Page<IssuedAttestationCertificate> pagedResult = this.issuedCertificateRepository.findByArchiveFlag(false, paging);

            if (pagedResult.hasContent()) {
                records.addAll(pagedResult.getContent());
                records.setRecordsTotal(pagedResult.getContent().size());
            } else {
                records.setRecordsTotal(input.getLength());
            }

            records.setRecordsFiltered(issuedCertificateRepository.findByArchiveFlag(false).size());

            log.debug("Returning list of size: " + records.size());
            return new DataTableResponse<>(records, input);
        }

        return new DataTableResponse<>(new FilteredRecordsList<>(), input);
    }

    /**
     * Upload and processes a credential.
     *
     * @param certificateType String containing the certificate type
     * @param files the files to process
     * @param attr the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "/{certificateType}/upload", method = RequestMethod.POST)
    protected RedirectView upload(
            @PathVariable("certificateType") final String certificateType,
            @RequestParam("file") final MultipartFile[] files,
            final RedirectAttributes attr) throws URISyntaxException {

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        for (MultipartFile file : files) {
            //Parse certificate
            Certificate certificate = parseCertificate(certificateType, file, messages);

            //Store only if it was parsed
            if (certificate != null) {
                storeCertificate(
                        certificateType,
                        file.getOriginalFilename(),
                        messages, certificate);
            }
        }

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);

        return redirectTo(getCertificatePage(certificateType), new NoPageParams(), model, attr);
    }

    /**
     * Archives (soft delete) the credential.
     *
     * @param certificateType String containing the certificate type
     * @param id the UUID of the cert to delete
     * @param attr RedirectAttributes used to forward data back to the original
     * page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "/{certificateType}/delete", method = RequestMethod.POST)
    public RedirectView delete(
            @PathVariable("certificateType") final String certificateType,
            @RequestParam final String id,
            final RedirectAttributes attr) throws URISyntaxException {
        log.info("Handling request to delete " + id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            UUID uuid = UUID.fromString(id);
            Certificate certificate = certificateRepository.getCertificate(uuid);

            if (certificate == null) {
                // Use the term "record" here to avoid user confusion b/t cert and cred
                String notFoundMessage = "Unable to locate record with ID: " + uuid;
                messages.addError(notFoundMessage);
                log.warn(notFoundMessage);
            } else {
                if (certificateType.equals(PLATFORMCREDENTIAL)) {
                    PlatformCredential platformCertificate = (PlatformCredential) certificate;
                    if (platformCertificate.isPlatformBase()) {
                        // only do this if the base is being deleted.
                        List<PlatformCredential> sharedCertificates = getCertificateByBoardSN(
                                certificateType,
                                platformCertificate.getPlatformSerial());

                        for (PlatformCredential pc : sharedCertificates) {
                            if (!pc.isPlatformBase()) {
                                pc.archive("User requested deletion via UI of the base certificate");
                                certificateRepository.save(pc);
                                deleteComponentResults(pc.getPlatformSerial());
                            }
                        }
                    }
                    deleteComponentResults(platformCertificate.getPlatformSerial());
                }

                certificate.archive("User requested deletion via UI");
                certificateRepository.save(certificate);

                String deleteCompletedMessage = "Certificate successfully deleted";
                messages.addInfo(deleteCompletedMessage);
                log.info(deleteCompletedMessage);
            }
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: " + id;
            messages.addError(uuidError);
            log.error(uuidError, ex);
        } catch (DBManagerException ex) {
            String dbError = "Failed to archive cert: " + id;
            messages.addError(dbError);
            log.error(dbError, ex);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(getCertificatePage(certificateType), new NoPageParams(), model, attr);
    }

    /**
     * Handles request to download the cert by writing it to the response stream
     * for download.
     *
     * @param certificateType String containing the certificate type
     * @param id the UUID of the cert to download
     * @param response the response object (needed to update the header with the
     * file name)
     * @throws java.io.IOException when writing to response output stream
     */
    @RequestMapping(value = "/{certificateType}/download", method = RequestMethod.GET)
    public void download(
            @PathVariable("certificateType") final String certificateType,
            @RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download " + id);

        try {
            UUID uuid = UUID.fromString(id);
            Certificate certificate = certificateRepository.getCertificate(uuid);
            if (certificate == null) {
                // Use the term "record" here to avoid user confusion b/t cert and cred
                String notFoundMessage = "Unable to locate record with ID: " + uuid;
                log.warn(notFoundMessage);
                // send a 404 error when invalid certificate
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                StringBuilder fileName = new StringBuilder("filename=\"");
                fileName.append(getCertificateClass(certificateType).getSimpleName());
                fileName.append("_");
                fileName.append(certificate.getSerialNumber());
                fileName.append(".cer\"");

                // Set filename for download.
                response.setHeader("Content-Disposition", "attachment;" + fileName);
                response.setContentType("application/octet-stream");

                // write cert to output stream
                response.getOutputStream().write(certificate.getRawBytes());
            }
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: " + id;
            log.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles request to download the ACA cert by writing it to the response
     * stream for download.
     *
     * @param response the response object (needed to update the header with the
     * file name)
     *
     * @throws java.io.IOException when writing to response output stream
     */
    @ResponseBody
    @RequestMapping(value = "/trust-chain/download-aca-cert", method = RequestMethod.GET)
    public void downloadAcaCertificate(final HttpServletResponse response)
            throws IOException {

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=\"hirs-aca-cert.cer\"");
        response.setContentType("application/octet-stream");

        // write cert to output stream
        response.getOutputStream().write(certificateAuthorityCredential.getRawBytes());
    }

    /**
     * Handles request to download the certs by writing it to the response stream
     * for download in bulk.
     *
     * @param response the response object (needed to update the header with the
     * file name)
     * @throws java.io.IOException when writing to response output stream
     */
    @RequestMapping(value = "/trust-chain/bulk", method = RequestMethod.GET)
    public void caBulkDownload(final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download all trust chain certificates");
        String fileName = "trust-chain.zip";
        final String singleFileName = "ca-certificates";

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            // get all files
            bulkDownload(zipOut, this.certificateRepository.findByType("CertificateAuthorityCredential"), singleFileName);
            // write cert to output stream
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: ";
            log.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles request to download the certs by writing it to the response stream
     * for download in bulk.
     *
     * @param response the response object (needed to update the header with the
     * file name)
     * @throws java.io.IOException when writing to response output stream
     */
    @RequestMapping(value = "/platform-credentials/bulk", method = RequestMethod.GET)
    public void pcBulkDownload(final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download all platform certificates");
        String fileName = "platform_certificates.zip";
        final String singleFileName = "Platform_Certificate";
        String zipFileName;

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            // get all files
            bulkDownload(zipOut, this.certificateRepository.findByType("PlatformCredential"), singleFileName);
            // write cert to output stream
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: ";
            log.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles request to download the certs by writing it to the response stream
     * for download in bulk.
     *
     * @param response the response object (needed to update the header with the
     * file name)
     * @throws java.io.IOException when writing to response output stream
     */
    @RequestMapping(value = "/issued-certificates/bulk", method = RequestMethod.GET)
    public void icBulkDownload(final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download all issued certificates");
        String fileName = "issued_certificates.zip";
        final String singleFileName = "Issued_Certificate";
        String zipFileName;

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            // get all files
            bulkDownload(zipOut, this.certificateRepository.findByType("IssuedAttestationCertificate"), singleFileName);
            // write cert to output stream
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: ";
            log.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles request to download the certs by writing it to the response stream
     * for download in bulk.
     *
     * @param response the response object (needed to update the header with the
     * file name)
     * @throws java.io.IOException when writing to response output stream
     */
    @RequestMapping(value = "/endorsement-key-credentials/bulk", method = RequestMethod.GET)
    public void ekBulkDownload(final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download all endorsement certificates");
        String fileName = "endorsement_certificates.zip";
        final String singleFileName = "Endorsement_Certificates";

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            // get all files
            bulkDownload(zipOut, this.certificateRepository.findByType("EndorsementCredential"), singleFileName);
            // write cert to output stream
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: ";
            log.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private ZipOutputStream bulkDownload(final ZipOutputStream zipOut,
                              final List<Certificate> certificates,
                              final String singleFileName) throws IOException {
        String zipFileName;
        // get all files
        for (Certificate certificate : certificates) {
            zipFileName = String.format("%s[%s].cer", singleFileName,
                    Integer.toHexString(certificate.getCertificateHash()));
            // configure the zip entry, the properties of the 'file'
            ZipEntry zipEntry = new ZipEntry(zipFileName);
            zipEntry.setSize((long) certificate.getRawBytes().length * Byte.SIZE);
            zipEntry.setTime(System.currentTimeMillis());
            zipOut.putNextEntry(zipEntry);
            // the content of the resource
            StreamUtils.copy(certificate.getRawBytes(), zipOut);
            zipOut.closeEntry();
        }
        zipOut.finish();
        return zipOut;
    }

    /**
     * Get flag indicating if a device-name join/alias is required for
     * displaying the table data. This will be true if displaying a cert that is
     * associated with a device.
     *
     * @param certificateType String containing the certificate type
     * @return true if the list criteria modifier requires aliasing the device
     * table, false otherwise.
     */
    private boolean hasDeviceTableToJoin(final String certificateType) {
        boolean hasDevice = true;
        // Trust_Chain Credential do not contain the device table to join.
        if (certificateType.equals(TRUSTCHAIN)) {
            hasDevice = false;
        }
        return hasDevice;
    }

    /**
     * Get the page based on the certificate type.
     *
     * @param certificateType String containing the certificate type
     * @return the page for the certificate type.
     */
    private static Page getCertificatePage(final String certificateType) {
        // get page information (default to TRUST_CHAIN)
        return switch (certificateType) {
            case PLATFORMCREDENTIAL -> Page.PLATFORM_CREDENTIALS;
            case ENDORSEMENTCREDENTIAL -> Page.ENDORSEMENT_KEY_CREDENTIALS;
            case ISSUEDCERTIFICATES -> Page.ISSUED_CERTIFICATES;
            default -> Page.TRUST_CHAIN;
        };
    }

    /**
     * Gets the concrete certificate class type to query for.
     *
     * @param certificateType String containing the certificate type
     * @return the certificate class type
     */
    private static Class<? extends Certificate> getCertificateClass(final String certificateType) {
        switch (certificateType) {
            case PLATFORMCREDENTIAL:
                return PlatformCredential.class;
            case ENDORSEMENTCREDENTIAL:
                return EndorsementCredential.class;
            case ISSUEDCERTIFICATES:
                return IssuedAttestationCertificate.class;
            case TRUSTCHAIN:
                return CertificateAuthorityCredential.class;
            default:
                throw new IllegalArgumentException(
                        String.format("Unknown certificate type: %s", certificateType));
        }
    }

    /**
     * Gets the certificate by the hash code of its bytes. Looks for both
     * archived and unarchived certificates.
     *
     * @param certificateType String containing the certificate type
     * @param certificateHash the hash of the certificate's bytes
     * @return the certificate or null if none is found
     */
    private Certificate getCertificateByHash(
            final String certificateType,
            final int certificateHash) {

        switch (certificateType) {
            case PLATFORMCREDENTIAL:
                return this.certificateRepository
                        .findByCertificateHash(certificateHash,
                                "PlatformCredential");
            case ENDORSEMENTCREDENTIAL:
                return this.certificateRepository
                        .findByCertificateHash(certificateHash,
                                "EndorsementCredential");
            case TRUSTCHAIN:
                return this.certificateRepository
                        .findByCertificateHash(certificateHash,
                                "CertificateAuthorityCredential");
            default:
                return null;
        }
    }

    /**
     * Gets the certificate by the platform serial number.
     *
     * @param certificateType String containing the certificate type
     * @param serialNumber the platform serial number
     * @return the certificate or null if none is found
     */
    private List<PlatformCredential> getCertificateByBoardSN(
            final String certificateType,
            final String serialNumber) {
        List<PlatformCredential> associatedCertificates = new LinkedList<>();

        if (serialNumber != null) {
            switch (certificateType) {
                case PLATFORMCREDENTIAL:
                    associatedCertificates.addAll(this.certificateRepository
                            .byBoardSerialNumber(serialNumber));
                default:
            }
        }

        return associatedCertificates;
    }

    /**
     * Parses an uploaded file into a certificate and populates the given model
     * with error messages if parsing fails.
     *
     * @param certificateType String containing the certificate type
     * @param file the file being uploaded from the portal
     * @param messages contains any messages that will be display on the page
     * @return the parsed certificate or null if parsing failed.
     */
    private Certificate parseCertificate(
            final String certificateType,
            final MultipartFile file,
            final PageMessages messages) {
        log.info("Received File of Size: " + file.getSize());

        byte[] fileBytes;
        String fileName = file.getOriginalFilename();

        // build the certificate from the uploaded bytes
        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to read uploaded file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
            return null;
        }
        try {
            switch (certificateType) {
                case PLATFORMCREDENTIAL:
                    return new PlatformCredential(fileBytes);
                case ENDORSEMENTCREDENTIAL:
                    return new EndorsementCredential(fileBytes);
                case TRUSTCHAIN:
                    if (CredentialHelper.isMultiPEM(new String(fileBytes, StandardCharsets.UTF_8))) {
                        try (ByteArrayInputStream certInputStream = new ByteArrayInputStream(fileBytes)) {
                            CertificateFactory cf = CertificateFactory.getInstance("X.509");
                            Collection c = cf.generateCertificates(certInputStream);
                            Iterator i = c.iterator();
                            while (i.hasNext()) {
                                storeCertificate(
                                        certificateType,
                                        file.getOriginalFilename(),
                                        messages, new CertificateAuthorityCredential(((java.security.cert.Certificate) i.next()).getEncoded()));
                            }

                            // stop the main thread from saving/storing
                            return null;
                        } catch (CertificateException e) {
                            throw new IOException("Cannot construct X509Certificate from the input stream", e);
                        }
                    }
                    return new CertificateAuthorityCredential(fileBytes);
                default:
                    final String failMessage = String.format("Failed to parse uploaded file "
                            + "(%s). Invalid certificate type: %s", fileName, certificateType);
                    log.error(failMessage);
                    messages.addError(failMessage);
                    return null;
            }
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            messages.addError(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format(
                    "Certificate format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            messages.addError(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage = String.format(
                    "Unexpected object while parsing %s ", fileName);
            log.error(failMessage, isEx);
            messages.addError(failMessage + isEx.getMessage());
            return null;
        }
    }

    /**
     * Store the given certificate in the database.
     *
     * @param certificateType String containing the certificate type
     * @param fileName        contain the name of the file of the certificate to
     *                        be stored
     * @param messages        contains any messages that will be display on the page
     * @param certificate     the certificate to store
     */
    private void storeCertificate(
            final String certificateType,
            final String fileName,
            final PageMessages messages,
            final Certificate certificate) {

        Certificate existingCertificate;

        // look for an identical certificate in the database
        try {
            existingCertificate = getCertificateByHash(
                    certificateType,
                    certificate.getCertificateHash());
        } catch (DBServiceException dbsEx) {
            final String failMessage = "Querying for existing certificate failed ("
                    + fileName + "): ";
            messages.addError(failMessage + dbsEx.getMessage());
            log.error(failMessage, dbsEx);
            return;
        }

        try {
            // save the new certificate if no match is found
            if (existingCertificate == null) {
                if (certificateType.equals(PLATFORMCREDENTIAL)) {
                    PlatformCredential platformCertificate = (PlatformCredential) certificate;
                    if (platformCertificate.isPlatformBase()) {
                        List<PlatformCredential> sharedCertificates = getCertificateByBoardSN(
                                certificateType,
                                platformCertificate.getPlatformSerial());
                        for (PlatformCredential pc : sharedCertificates) {
                            if (pc.isPlatformBase()) {
                                final String failMessage = "Storing certificate failed: "
                                        + "platform credential "
                                        + "chain (" + pc.getPlatformSerial()
                                        + ") base already exists in this chain ("
                                        + fileName + ")";
                                messages.addError(failMessage);
                                log.error(failMessage);
                                return;
                            }
                        }
                    } /**else {
                     // this is a delta, check if the holder exists.
                     PlatformCredential holderPC = PlatformCredential
                     .select(certificateManager)
                     .bySerialNumber(platformCertificate.getHolderSerialNumber())
                     .getCertificate();
                     if (holderPC == null)  {
                     final String failMessage = "Storing certificate failed: "
                     + "delta credential"
                     + " must have an existing holder stored.  "
                     + "Credential serial "
                     + platformCertificate.getHolderSerialNumber()
                     + " doesn't exist.";
                     messages.addError(failMessage);
                     LOGGER.error(failMessage);
                     return;
                     }
                     }**/
                }

                this.certificateRepository.save(certificate);
                handlePlatformComponents(certificate);

                final String successMsg
                        = String.format("New certificate successfully uploaded (%s): ", fileName);
                messages.addSuccess(successMsg);
                log.info(successMsg);
                return;
            }
        } catch (DBServiceException dbsEx) {
            final String failMessage = String.format("Storing new certificate failed (%s): ",
                    fileName);
            messages.addError(failMessage + dbsEx.getMessage());
            log.error(failMessage, dbsEx);
            return;
        }

        try {
            // if an identical certificate is archived, update the existing certificate to
            // unarchive it and change the creation date
            if (existingCertificate.isArchived()) {
                existingCertificate.restore();
                existingCertificate.resetCreateTime();
                this.certificateRepository.save(existingCertificate);

                List<ComponentResult> componentResults = componentResultRepository
                        .findByBoardSerialNumber(((PlatformCredential) existingCertificate)
                                .getPlatformSerial());
                for (ComponentResult componentResult : componentResults) {
                    componentResult.restore();
                    componentResult.resetCreateTime();
                    this.componentResultRepository.save(componentResult);
                }

                final String successMsg = String.format("Pre-existing certificate "
                        + "found and unarchived (%s): ", fileName);
                messages.addSuccess(successMsg);
                log.info(successMsg);
                return;
            }
        } catch (DBServiceException dbsEx) {
            final String failMessage = String.format("Found an identical"
                    + " pre-existing certificate in the "
                    + "archive, but failed to unarchive it (%s): ", fileName);
            messages.addError(failMessage + dbsEx.getMessage());
            log.error(failMessage, dbsEx);
            return;
        }

        // if an identical certificate is already unarchived, do nothing and show a fail message
        final String failMessage
                = String.format("Storing certificate failed: an identical"
                + " certificate already exists (%s): ", fileName);
        messages.addError(failMessage);
        log.error(failMessage);
    }

    private void handlePlatformComponents(final Certificate certificate) {
        PlatformCredential platformCredential;

        if (certificate instanceof PlatformCredential) {
            platformCredential = (PlatformCredential) certificate;
            ComponentResult componentResult;
            for (ComponentIdentifier componentIdentifier : platformCredential
                    .getComponentIdentifiers()) {

                componentResult = new ComponentResult(platformCredential.getPlatformSerial(),
                        platformCredential.getSerialNumber().toString(),
                        platformCredential.getPlatformChainType(),
                        componentIdentifier);
                componentResultRepository.save(componentResult);
            }
        }
    }

    private void deleteComponentResults(final String platformSerial) {
        List<ComponentResult> componentResults = componentResultRepository
                .findByBoardSerialNumber(platformSerial);

        for (ComponentResult componentResult : componentResults) {
            componentResult.archive();
            componentResultRepository.save(componentResult);
        }
    }
}
