package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.EndorsementCredentialRepository;
import hirs.attestationca.persist.entity.manager.PlatformCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.service.CertificateService;
import hirs.attestationca.portal.datatables.Column;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/platform-credentials")
public class PlatformCredentialPageController extends PageController<NoPageParams> {

    private static final String PLATFORMCREDENTIAL = "platform-credentials";

    private final CertificateRepository certificateRepository;
    private final PlatformCertificateRepository platformCertificateRepository;
    private final EndorsementCredentialRepository endorsementCredentialRepository;
    private final CertificateService certificateService;

    @Autowired
    public PlatformCredentialPageController(final CertificateRepository certificateRepository,
                                            final PlatformCertificateRepository platformCertificateRepository,
                                            final EndorsementCredentialRepository endorsementCredentialRepository,
                                            final CertificateService certificateService) {
        super(Page.TRUST_CHAIN);
        this.certificateRepository = certificateRepository;
        this.platformCertificateRepository = platformCertificateRepository;
        this.endorsementCredentialRepository = endorsementCredentialRepository;
        this.certificateService = certificateService;
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
        return getBaseModelAndView(Page.PLATFORM_CREDENTIALS);
    }

    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<PlatformCredential> getPlatformCredentialsTableData(
            final DataTableInput input) {

        log.debug("Handling list request for platform credentials: {}", input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();

        log.debug("Ordering on column: {}", orderColumnName);

        String searchText = input.getSearch().getValue();
        List<String> searchableColumns = findSearchableColumnsNames(input.getColumns());

        int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        FilteredRecordsList<PlatformCredential> records = new FilteredRecordsList<>();

        org.springframework.data.domain.Page<PlatformCredential> pagedResult;

        if (StringUtils.isBlank(searchText)) {
            pagedResult =
                    this.platformCertificateRepository.findByArchiveFlag(false, pageable);
        } else {
            pagedResult =
                    this.certificateService.findBySearchableColumnsAndArchiveFlag(
                            PlatformCredential.class,
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

        records.setRecordsFiltered(platformCertificateRepository.findByArchiveFlag(false).size());

        EndorsementCredential associatedEC;

        if (!records.isEmpty()) {
            // loop all the platform certificates
            for (PlatformCredential pc : records) {
                // find the EC using the PC's "holder serial number"
                associatedEC = this.endorsementCredentialRepository
                        .findBySerialNumber(pc.getHolderSerialNumber());

                if (associatedEC != null) {
                    log.debug("EC ID for holder s/n {} = {}", pc
                            .getHolderSerialNumber(), associatedEC.getId());
                }

                pc.setEndorsementCredential(associatedEC);
            }
        }

        log.debug("Returning the size of the list of platform credentials: {}", records.size());
        return new DataTableResponse<>(records, input);
    }

    /**
     * Handles request to download the cert by writing it to the response stream
     * for download.
     *
     * @param id       the UUID of the cert to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void platformCredentialSingleDownload(
            @RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download platform certificate id {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Certificate certificate = certificateRepository.getCertificate(uuid);

            if (certificate == null) {
                log.warn("Unable to locate platform certificate record with ID: {}", uuid);
                // send a 404 error when invalid certificate
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                if (certificate instanceof PlatformCredential uploadedPlatformCredential) {

                    String fileName = "filename=\"" + PlatformCredential.class.getSimpleName()
                            + "_"
                            + uploadedPlatformCredential.getSerialNumber()
                            + ".cer\"";

                    // Set filename for download.
                    response.setHeader("Content-Disposition", "attachment;" + fileName);
                    response.setContentType("application/octet-stream");

                    // write cert to output stream
                    response.getOutputStream().write(certificate.getRawBytes());
                }
            }
        } catch (IllegalArgumentException ex) {
            log.error("Failed to parse platform certificate ID from: " + id, ex);
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
    @GetMapping("/bulk-download")
    public void platformCredentialsBulkDownload(final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download all platform certificates");

        final String fileName = "platform_certificates.zip";

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {

            // find all the uploaded platform certificates
            List<Certificate> certificates = this.certificateRepository.findByType("PlatformCredential");

            // convert the list of certificates to a list of platform certificates
            List<PlatformCredential> uploadedPCs = certificates.stream()
                    .filter(eachPC -> eachPC instanceof PlatformCredential)
                    .map(eachPC -> (PlatformCredential) eachPC).toList();

            // get all files and write certificates to output stream
            bulkDownloadPlatformCertificates(zipOut, uploadedPCs);
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse platform certificate ID from: ";
            log.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Upload and processes a credential.
     *
     * @param files the files to process
     * @param attr  the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView upload(
            @RequestParam("file") final MultipartFile[] files,
            final RedirectAttributes attr) throws URISyntaxException {

        log.info("Handling request to upload one or more platform certificates");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> errorMessages = new ArrayList<>();
        List<String> successMessages = new ArrayList<>();

        for (MultipartFile file : files) {
            //Parse certificate
            PlatformCredential parsedPlatformCredential = parsePlatformCredential(file, messages);

            //Store only if it was parsed
            if (parsedPlatformCredential != null) {
                certificateService.storeCertificate(
                        PLATFORMCREDENTIAL,
                        file.getOriginalFilename(),
                        successMessages, errorMessages, parsedPlatformCredential);
            }
        }

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);

        return redirectTo(Page.PLATFORM_CREDENTIALS, new NoPageParams(), model, attr);
    }


    /**
     * Archives (soft delete) the credential.
     *
     * @param id   the UUID of the cert to delete
     * @param attr RedirectAttributes used to forward data back to the original
     *             page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView delete(
            @RequestParam final String id,
            final RedirectAttributes attr) throws URISyntaxException {
        log.info("Handling request to delete platform certificate id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            List<String> successMessages = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();

            UUID uuid = UUID.fromString(id);

            this.certificateService.deleteCertificate(uuid, PLATFORMCREDENTIAL,
                    successMessages, errorMessages);

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
        return redirectTo(Page.PLATFORM_CREDENTIALS, new NoPageParams(), model, attr);
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
     * Attempts to parse the provided file in order to create a PLatform Credential.
     *
     * @param file     file
     * @param messages page messages
     * @return platform credential
     */
    private PlatformCredential parsePlatformCredential(MultipartFile file, PageMessages messages) {
        log.info("Received platform certificate file of size: {}", file.getSize());

        byte[] fileBytes;
        String fileName = file.getOriginalFilename();

        // attempt to retrieve file bytes from the provided file
        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to read uploaded platform certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
            return null;
        }

        // attempt to build the platform certificate from the uploaded bytes
        try {
            return new PlatformCredential(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded platform certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded platform certificate pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            messages.addError(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format(
                    "platform certificate format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            messages.addError(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage = String.format(
                    "Unexpected object while parsing platform certificate %s ", fileName);
            log.error(failMessage, isEx);
            messages.addError(failMessage + isEx.getMessage());
            return null;
        }
    }

    /**
     * Helper method that packages a collection of platform credentials into a zip file.
     *
     * @param zipOut              zip outputs stream
     * @param platformCredentials collection of  platform certificates
     * @throws IOException if there are any issues packaging or downloading the zip file
     */
    private void bulkDownloadPlatformCertificates(final ZipOutputStream zipOut,
                                                  final List<PlatformCredential> platformCredentials)
            throws IOException {
        String zipFileName;
        final String singleFileName = "Platform_Certificate";

        // get all files
        for (PlatformCredential platformCredential : platformCredentials) {
            zipFileName = String.format("%s[%s].cer", singleFileName,
                    Integer.toHexString(platformCredential.getCertificateHash()));
            // configure the zip entry, the properties of the 'file'
            ZipEntry zipEntry = new ZipEntry(zipFileName);
            zipEntry.setSize((long) platformCredential.getRawBytes().length * Byte.SIZE);
            zipEntry.setTime(System.currentTimeMillis());
            zipOut.putNextEntry(zipEntry);
            // the content of the resource
            StreamUtils.copy(platformCredential.getRawBytes(), zipOut);
            zipOut.closeEntry();
        }
        zipOut.finish();
    }
}
