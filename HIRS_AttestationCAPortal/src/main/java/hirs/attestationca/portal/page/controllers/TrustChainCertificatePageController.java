package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain")
public class TrustChainCertificatePageController extends PageController<NoPageParams> {

    /**
     * Model attribute name used by initPage for the aca cert info.
     */
    static final String ACA_CERT_DATA = "acaCertData";

    private static final String TRUST_CHAIN = "trust-chain";

    private final CertificateRepository certificateRepository;
    private final CACredentialRepository caCredentialRepository;
    private final CertificateService certificateService;
    private CertificateAuthorityCredential certificateAuthorityCredential;

    @Autowired
    public TrustChainCertificatePageController(final CertificateRepository certificateRepository,
                                               final CACredentialRepository caCredentialRepository,
                                               final CertificateService certificateService,
                                               final X509Certificate acaCertificate) {
        super(Page.TRUST_CHAIN);
        this.certificateRepository = certificateRepository;
        this.caCredentialRepository = caCredentialRepository;
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
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the page.
     */
    @RequestMapping
    public ModelAndView initPage(
            final NoPageParams params, final Model model) {

        ModelAndView mav = getBaseModelAndView(Page.TRUST_CHAIN);

        mav.addObject(ACA_CERT_DATA,
                new HashMap<>(CertificateStringMapBuilder.getCertificateAuthorityInformation(
                        certificateAuthorityCredential, this.certificateRepository,
                        this.caCredentialRepository)));

        return mav;
    }

    /**
     * @param input
     * @return
     */
    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<CertificateAuthorityCredential> getTrustChainCertificatesTableData(
            final DataTableInput input) {
        log.debug("Handling list request for trust chain certificates: {}", input);

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

    /**
     * Handles request to download the trust chain certificate by writing it to the response stream
     * for download.
     *
     * @param id       the UUID of the trust chain certificate to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadSingleTrustChainCertificate(
            @RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Certificate certificate = this.certificateService.findCertificate(uuid);

            if (certificate == null) {
                // Use the term "record" here to avoid user confusion b/t cert and cred
                String notFoundMessage = "Unable to locate record with ID: " + uuid;
                log.warn(notFoundMessage);
                // send a 404 error when invalid certificate
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                String fileName = "filename=\"" + CertificateAuthorityCredential.class.getSimpleName()
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
    @GetMapping("/download-aca-cert")
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
    @GetMapping("/bulk-download")
    public void bulkDownloadTrustChainCertificates(final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download all trust chain certificates");
        final String fileName = "trust-chain.zip";
        final String singleFileName = "ca-certificates";

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            //  write trust chain certificates to output stream and bulk download them
            this.certificateService.bulkDownloadCertificates(zipOut, TRUST_CHAIN, singleFileName);
        } catch (Exception ex) {
            log.error("Failed to bulk download trust chain certificates: ", ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Uploads and processes a trust chain certificate.
     *
     * @param files the files to process
     * @param attr  the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView uploadTrustChainCertificate(
            @RequestParam("file") final MultipartFile[] files,
            final RedirectAttributes attr) throws URISyntaxException {

        log.info("Handling request to upload one or more trust chain certificates");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> errorMessages = new ArrayList<>();
        List<String> successMessages = new ArrayList<>();

        for (MultipartFile file : files) {
            //Parse trust chain certificate
            CertificateAuthorityCredential parsedTrustChainCertificate =
                    parseTrustChainCertificate(file, messages);

            //Store only if it was parsed
            if (parsedTrustChainCertificate != null) {
                certificateService.storeCertificate(
                        TRUST_CHAIN,
                        file.getOriginalFilename(),
                        successMessages, errorMessages, parsedTrustChainCertificate);
            }

            var a = successMessages;
            var b = errorMessages;
        }

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);

        return redirectTo(Page.TRUST_CHAIN, new NoPageParams(), model, attr);
    }

    /**
     * Archives (soft deletes) the trust chain certificate.
     *
     * @param id   the UUID of the trust chain certificate to delete
     * @param attr RedirectAttributes used to forward data back to the original
     *             page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteTrustChainCertificates(
            @RequestParam final String id,
            final RedirectAttributes attr) throws URISyntaxException {
        log.info("Handling request to delete trust chain certificate id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            List<String> successMessages = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();

            UUID uuid = UUID.fromString(id);

            this.certificateService.deleteCertificate(uuid, TRUST_CHAIN,
                    successMessages, errorMessages);

            var a = successMessages;

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
        return redirectTo(Page.TRUST_CHAIN, new NoPageParams(), model, attr);
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

    /**
     * Attempts to parse the provided file in order to create a trust chain certificate.
     *
     * @param file     file
     * @param messages page messages
     * @return trust chain certificate
     */
    private CertificateAuthorityCredential parseTrustChainCertificate(MultipartFile file,
                                                                      PageMessages messages) {
        log.info("Received trust chain certificate file of size: {}", file.getSize());

        byte[] fileBytes;
        String fileName = file.getOriginalFilename();

        // attempt to retrieve file bytes from the provided file
        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to read uploaded trust chain certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
            return null;
        }

        // attempt to build the trust chain certificates from the uploaded bytes
        try {
            if (CredentialHelper.isMultiPEM(new String(fileBytes, StandardCharsets.UTF_8))) {
                try (ByteArrayInputStream certInputStream = new ByteArrayInputStream(fileBytes)) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    Collection<? extends java.security.cert.Certificate> c =
                            cf.generateCertificates(certInputStream);

                    for (java.security.cert.Certificate certificate : c) {
                        List<String> successMessages = new ArrayList<>();
                        List<String> errorMessages = new ArrayList<>();
                        this.certificateService.storeCertificate(
                                TRUST_CHAIN,
                                file.getOriginalFilename(),
                                successMessages,
                                errorMessages,
                                new CertificateAuthorityCredential(
                                        certificate.getEncoded()));
                    }

                    // stop the main thread from saving/storing
                    return null;
                } catch (CertificateException e) {
                    throw new IOException("Cannot construct X509Certificate from the input stream",
                            e);
                }
            }
            return new CertificateAuthorityCredential(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded trust chain certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded trust chain certificate pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            messages.addError(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format(
                    "Trust chain certificate format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            messages.addError(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage = String.format(
                    "Unexpected object while parsing trust chain certificate %s ", fileName);
            log.error(failMessage, isEx);
            messages.addError(failMessage + isEx.getMessage());
            return null;
        }
    }

}
