package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.DBServiceException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.IDevIDCertificateRepository;
import hirs.attestationca.persist.entity.manager.IssuedCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentResult;
import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.service.CertificateService;
import hirs.attestationca.persist.util.CredentialHelper;
import hirs.attestationca.portal.datatables.Column;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.CertificateStringMapBuilder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.DecoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    /**
     * Model attribute name used by initPage for the aca cert info.
     */
    static final String ACA_CERT_DATA = "acaCertData";
    private static final String TRUSTCHAIN = "trust-chain";
    private static final String IDEVIDCERTIFICATE = "idevid-certificates";
    private static final String ISSUEDCERTIFICATES = "issued-certificates";
    private final CertificateRepository certificateRepository;
    private final ComponentResultRepository componentResultRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;
    private final CACredentialRepository caCredentialRepository;
    private final IDevIDCertificateRepository iDevIDCertificateRepository;
    private final CertificateService certificateService;
    private CertificateAuthorityCredential certificateAuthorityCredential;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param certificateRepository       the general certificate manager
     * @param componentResultRepository   the component result repo
     * @param issuedCertificateRepository the issued certificate manager
     * @param caCredentialRepository      the ca credential manager
     * @param iDevIDCertificateRepository the IDevID certificate repository
     * @param acaCertificate              the ACA's X509 certificate
     */
    @Autowired
    public CertificatePageController(final CertificateRepository certificateRepository,
                                     final ComponentResultRepository componentResultRepository,
                                     final IssuedCertificateRepository issuedCertificateRepository,
                                     final CACredentialRepository caCredentialRepository,
                                     final IDevIDCertificateRepository iDevIDCertificateRepository,
                                     final CertificateService certificateService,
                                     final X509Certificate acaCertificate) {
        super(Page.TRUST_CHAIN);
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
        this.issuedCertificateRepository = issuedCertificateRepository;
        this.caCredentialRepository = caCredentialRepository;
        this.iDevIDCertificateRepository = iDevIDCertificateRepository;
        this.certificateService = certificateService;

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
     * Get the page based on the certificate type.
     *
     * @param certificateType String containing the certificate type
     * @return the page for the certificate type.
     */
    private static Page getCertificatePage(final String certificateType) {
        // get page information (default to TRUST_CHAIN)
        return switch (certificateType) {
            case ISSUEDCERTIFICATES -> Page.ISSUED_CERTIFICATES;
            case IDEVIDCERTIFICATE -> Page.IDEVID_CERTIFICATES;
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
        return switch (certificateType) {
            case ISSUEDCERTIFICATES -> IssuedAttestationCertificate.class;
            case IDEVIDCERTIFICATE -> IDevIDCertificate.class;
            case TRUSTCHAIN -> CertificateAuthorityCredential.class;
            default -> throw new IllegalArgumentException(
                    String.format("Unknown certificate type: %s", certificateType));
        };
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
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
     * @param params          The object to map url parameters into.
     * @param model           The data model for the request. Can contain data from
     *                        redirect.
     * @return the path for the view and data model for the page.
     */
    @RequestMapping("/{certificateType}")
    public ModelAndView initPage(@PathVariable("certificateType") final String certificateType,
                                 final NoPageParams params, final Model model) {

        ModelAndView mav = null;
        HashMap<String, String> data = new HashMap<>();
        // add page information
        switch (certificateType) {
            case IDEVIDCERTIFICATE:
                mav = getBaseModelAndView(Page.IDEVID_CERTIFICATES);
                break;
            case ISSUEDCERTIFICATES:
                mav = getBaseModelAndView(Page.ISSUED_CERTIFICATES);
                break;
            case TRUSTCHAIN:
                mav = getBaseModelAndView(Page.TRUST_CHAIN);
                // Map with the ACA certificate information
                data.putAll(CertificateStringMapBuilder.getCertificateAuthorityInformation(
                        certificateAuthorityCredential, this.certificateRepository,
                        this.caCredentialRepository));
                mav.addObject(ACA_CERT_DATA, data);
                break;
            default:
                // send to an error page
                break;
        }

        return mav;
    }

    /**
     * Helper method that returns a list of column names that are searchable.
     *
     * @return searchable column names
     */
    private List<String> findSearchableColumnsNames(List<Column> columns) {

        // Retrieve all searchable columns and collect their names into a list of strings.
        return columns.stream().filter(Column::isSearchable).map(Column::getName)
                .collect(Collectors.toList());
    }

    @ResponseBody
    @GetMapping(value = "/trust-chain/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<? extends Certificate> getTrustChainTableData(
            final DataTableInput input) {
        log.debug("Handling list request: {}", input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();

        log.debug("Ordering on column: {}", orderColumnName);

        String searchText = input.getSearch().getValue();
        List<String> searchableColumns = findSearchableColumnsNames(input.getColumns());

        int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        FilteredRecordsList<CertificateAuthorityCredential> records = new FilteredRecordsList<>();

        org.springframework.data.domain.Page<CertificateAuthorityCredential> pagedResult;

        if (StringUtils.isBlank(searchText)) {
            pagedResult =
                    this.caCredentialRepository.findByArchiveFlag(false, pageable);
        } else {
            pagedResult =
                    this.certificateService.findBySearchableColumnsAndArchiveFlag(
                            CertificateAuthorityCredential.class,
                            searchableColumns,
                            searchText,
                            false, pageable);
        }


        if (pagedResult.hasContent()) {
            records.addAll(pagedResult.getContent());
            records.setRecordsTotal(pagedResult.getContent().size());
        } else {
            records.setRecordsTotal(input.getLength());
        }

        records.setRecordsFiltered(caCredentialRepository.findByArchiveFlag(false).size());

        log.debug("Returning the size of the list of trust chain certificates: {}", records.size());
        return new DataTableResponse<>(records, input);
    }


    @ResponseBody
    @GetMapping(value = "/idevid-certificates/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<? extends Certificate> getIDevIdCertificatesTableData(
            final DataTableInput input) {

        log.debug("Handling list request: {}", input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();

        log.debug("Ordering on column: {}", orderColumnName);

        String searchText = input.getSearch().getValue();
        List<String> searchableColumns = findSearchableColumnsNames(input.getColumns());

        int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        FilteredRecordsList<IDevIDCertificate> records = new FilteredRecordsList<>();
        org.springframework.data.domain.Page<IDevIDCertificate> pagedResult;

        if (StringUtils.isBlank(searchText)) {
            pagedResult =
                    this.iDevIDCertificateRepository.findByArchiveFlag(false, pageable);
        } else {
            pagedResult =
                    this.certificateService.findBySearchableColumnsAndArchiveFlag(
                            IDevIDCertificate.class,
                            searchableColumns,
                            searchText,
                            false, pageable);
        }

        if (pagedResult.hasContent()) {
            records.addAll(pagedResult.getContent());
            records.setRecordsTotal(pagedResult.getContent().size());
        } else {
            records.setRecordsTotal(input.getLength());
        }

        records.setRecordsFiltered(iDevIDCertificateRepository.findByArchiveFlag(false).size());

        log.debug("Returning the size of the list of IDEVID certificates: {}", records.size());
        return new DataTableResponse<>(records, input);
    }

    @ResponseBody
    @GetMapping(value = "/issued-certificates/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<? extends Certificate> getIssuedCertificatesTableData(
            final DataTableInput input) {
        log.debug("Handling list request: {}", input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();

        log.debug("Ordering on column: {}", orderColumnName);

        String searchText = input.getSearch().getValue();
        List<String> searchableColumns = findSearchableColumnsNames(input.getColumns());

        int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        FilteredRecordsList<IssuedAttestationCertificate> records = new FilteredRecordsList<>();
        org.springframework.data.domain.Page<IssuedAttestationCertificate> pagedResult;

        if (StringUtils.isBlank(searchText)) {
            pagedResult =
                    this.issuedCertificateRepository.findByArchiveFlag(false, pageable);
        } else {
            pagedResult =
                    this.certificateService.findBySearchableColumnsAndArchiveFlag(
                            IssuedAttestationCertificate.class,
                            searchableColumns,
                            searchText,
                            false, pageable);
        }

        if (pagedResult.hasContent()) {
            records.addAll(pagedResult.getContent());
            records.setRecordsTotal(pagedResult.getContent().size());
        } else {
            records.setRecordsTotal(input.getLength());
        }

        records.setRecordsFiltered(issuedCertificateRepository.findByArchiveFlag(false).size());

        log.debug("Returning the size of the list of issued certificates: {}", records.size());
        return new DataTableResponse<>(records, input);

    }


    /**
     * Upload and processes a credential.
     *
     * @param certificateType String containing the certificate type
     * @param files           the files to process
     * @param attr            the redirection attributes
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
     * @param id              the UUID of the cert to delete
     * @param attr            RedirectAttributes used to forward data back to the original
     *                        page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "/{certificateType}/delete", method = RequestMethod.POST)
    public RedirectView delete(
            @PathVariable("certificateType") final String certificateType,
            @RequestParam final String id,
            final RedirectAttributes attr) throws URISyntaxException {
        log.info("Handling request to delete {}", id);

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
     * @param id              the UUID of the cert to download
     * @param response        the response object (needed to update the header with the
     *                        file name)
     * @throws IOException when writing to response output stream
     */
    @RequestMapping(value = "/{certificateType}/download", method = RequestMethod.GET)
    public void download(
            @PathVariable("certificateType") final String certificateType,
            @RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download {}", id);

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
                String fileName = "filename=\"" + getCertificateClass(certificateType).getSimpleName()
                        + "_"
                        + certificate.getSerialNumber()
                        + ".cer\"";

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
     *                 file name)
     * @throws IOException when writing to response output stream
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
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @RequestMapping(value = "/trust-chain/bulk-download", method = RequestMethod.GET)
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
            bulkDownload(zipOut, this.certificateRepository.findByType("CertificateAuthorityCredential"),
                    singleFileName);
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
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @RequestMapping(value = "/issued-certificates/bulk-download", method = RequestMethod.GET)
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
            bulkDownload(zipOut, this.certificateRepository.findByType("IssuedAttestationCertificate"),
                    singleFileName);
            // write cert to output stream
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: ";
            log.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Helper method that packages a collection of certificates into a zip file.
     *
     * @param zipOut         zip outputs stream
     * @param certificates   collection of certificates
     * @param singleFileName zip file name
     * @return zip outputs stream
     * @throws IOException if there are any issues packaging or downloading the zip file
     */
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
        // Trust_Chain Credential do not contain the device table to join.
        return !certificateType.equals(TRUSTCHAIN);
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

        return switch (certificateType) {
            case TRUSTCHAIN -> this.certificateRepository
                    .findByCertificateHash(certificateHash,
                            "CertificateAuthorityCredential");
            case IDEVIDCERTIFICATE -> this.certificateRepository
                    .findByCertificateHash(certificateHash,
                            "IDevIDCertificate");
            default -> null;
        };
    }

    /**
     * Parses an uploaded file into a certificate and populates the given model
     * with error messages if parsing fails.
     *
     * @param certificateType String containing the certificate type
     * @param file            the file being uploaded from the portal
     * @param messages        contains any messages that will be display on the page
     * @return the parsed certificate or null if parsing failed.
     */
    private Certificate parseCertificate(
            final String certificateType,
            final MultipartFile file,
            final PageMessages messages) {
        log.info("Received File of Size: {}", file.getSize());

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
                case IDEVIDCERTIFICATE:
                    return new IDevIDCertificate(fileBytes);
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
                                        messages, new CertificateAuthorityCredential(
                                                ((java.security.cert.Certificate) i.next()).getEncoded()));
                            }

                            // stop the main thread from saving/storing
                            return null;
                        } catch (CertificateException e) {
                            throw new IOException("Cannot construct X509Certificate from the input stream",
                                    e);
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
                    } /*else {
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
                     }*/
                }

                this.certificateRepository.save(certificate);
                parseAndSaveComponentResults(certificate);

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
        } catch (IOException ioException) {
            final String ioExceptionMessage = "Failed to save component results in the database";
            messages.addError(ioExceptionMessage + ioException.getMessage());
            log.error(ioExceptionMessage, ioException);
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
}
