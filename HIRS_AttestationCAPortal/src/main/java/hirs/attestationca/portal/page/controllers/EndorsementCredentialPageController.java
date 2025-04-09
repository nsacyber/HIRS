package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.EndorsementCredentialRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
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
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/endorsement-key-credentials")
public class EndorsementCredentialPageController extends PageController<NoPageParams> {

    private static final String ENDORSEMENT_CREDENTIALS = "endorsement-key-credentials";

    private final CertificateRepository certificateRepository;
    private final EndorsementCredentialRepository endorsementCredentialRepository;
    private final CertificateService certificateService;

    @Autowired
    public EndorsementCredentialPageController(
            final CertificateRepository certificateRepository,
            final EndorsementCredentialRepository endorsementCredentialRepository,
            final CertificateService certificateService) {
        super(Page.TRUST_CHAIN);
        this.certificateRepository = certificateRepository;
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
        return getBaseModelAndView(Page.ENDORSEMENT_KEY_CREDENTIALS);
    }

    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<EndorsementCredential> getEndorsementCredentialsTableData(
            final DataTableInput input) {

        log.debug("Handling list request: {}", input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();

        log.debug("Ordering on column: {}", orderColumnName);

        String searchText = input.getSearch().getValue();
        List<String> searchableColumns = findSearchableColumnsNames(input.getColumns());

        int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        FilteredRecordsList<EndorsementCredential> records = new FilteredRecordsList<>();

        org.springframework.data.domain.Page<EndorsementCredential> pagedResult;

        if (StringUtils.isBlank(searchText)) {
            pagedResult = this.endorsementCredentialRepository.findByArchiveFlag(false, pageable);
        } else {
            pagedResult =
                    this.certificateService.findBySearchableColumnsAndArchiveFlag(
                            EndorsementCredential.class,
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

        records.setRecordsFiltered(endorsementCredentialRepository.findByArchiveFlag(false).size());

        log.debug("Returning the size of the list of endorsement credentials: {}", records.size());
        return new DataTableResponse<>(records, input);
    }

    /**
     * Handles request to download the endorsement credential by writing it to the response stream
     * for download.
     *
     * @param id       the UUID of the cert to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadSingleEndorsementCredential(
            @RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download endorsement credential id {}", id);

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
                if (certificate instanceof EndorsementCredential uploadedEndorsementCredential) {
                    String fileName = "filename=\"" + EndorsementCredential.class.getSimpleName()
                            + "_"
                            + uploadedEndorsementCredential.getSerialNumber()
                            + ".cer\"";

                    // Set filename for download.
                    response.setHeader("Content-Disposition", "attachment;" + fileName);
                    response.setContentType("application/octet-stream");

                    // write cert to output stream
                    response.getOutputStream().write(certificate.getRawBytes());
                }
            }
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: " + id;
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
    @GetMapping("/bulk-download")
    public void endorsementCredentialBulkDownload(final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download all endorsement credentials");

        final String fileName = "endorsement_certificates.zip";

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {

            // find all the uploaded endorsement credentials
            List<Certificate> certificates = this.certificateRepository.findByType("EndorsementCredential");

            // convert the list of certificates to a list of endorsement credentials
            List<EndorsementCredential> uploadedEKs = certificates.stream()
                    .filter(eachPC -> eachPC instanceof EndorsementCredential)
                    .map(eachPC -> (EndorsementCredential) eachPC).toList();

            // get all files
            bulkDownloadEndorsementCredentials(zipOut, uploadedEKs);
            // write cert to output stream
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: ";
            log.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Upload and processes an endorsement credential.
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

        log.info("Handling request to upload one or more endorsement credentials");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> errorMessages = new ArrayList<>();
        List<String> successMessages = new ArrayList<>();

        for (MultipartFile file : files) {
            //Parse endorsement credential
            EndorsementCredential parseEndorsementCredential = parseEndorsementCredential(file, messages);

            //Store only if it was parsed
            if (parseEndorsementCredential != null) {
                certificateService.storeCertificate(
                        ENDORSEMENT_CREDENTIALS,
                        file.getOriginalFilename(),
                        successMessages, errorMessages, parseEndorsementCredential);
            }
        }

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);

        return redirectTo(Page.ENDORSEMENT_KEY_CREDENTIALS, new NoPageParams(), model, attr);
    }

    /**
     * Archives (soft delete) the endorsement credential.
     *
     * @param id   the UUID of the endorsement cert to delete
     * @param attr RedirectAttributes used to forward data back to the original
     *             page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView delete(
            @RequestParam final String id,
            final RedirectAttributes attr) throws URISyntaxException {
        log.info("Handling request to delete endorsement credential id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            List<String> successMessages = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();

            UUID uuid = UUID.fromString(id);

            this.certificateService.deleteCertificate(uuid, ENDORSEMENT_CREDENTIALS,
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
        return redirectTo(Page.ENDORSEMENT_KEY_CREDENTIALS, new NoPageParams(), model, attr);
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
     * Attempts to parse the provided file in order to create an Endorsement Credential.
     *
     * @param file     file
     * @param messages page messages
     * @return endorsement credential
     */
    private EndorsementCredential parseEndorsementCredential(MultipartFile file, PageMessages messages) {
        log.info("Received endorsement credential file of size: {}", file.getSize());

        byte[] fileBytes;
        String fileName = file.getOriginalFilename();

        // attempt to retrieve file bytes from the provided file
        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to read uploaded endorsement credential file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
            return null;
        }

        // attempt to build the endorsement credential from the uploaded bytes
        try {
            return new EndorsementCredential(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded endorsement credential file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded endorsement credential pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            messages.addError(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format(
                    "Endorsement credential format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            messages.addError(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage = String.format(
                    "Unexpected object while parsing endorsement credential %s ", fileName);
            log.error(failMessage, isEx);
            messages.addError(failMessage + isEx.getMessage());
            return null;
        }
    }

    /**
     * Helper method that packages a collection of endorsement credentials into a zip file.
     *
     * @param zipOut                 zip outputs stream
     * @param endorsementCredentials collection of endorsement credentials
     * @throws IOException if there are any issues packaging or downloading the zip file
     */
    private void bulkDownloadEndorsementCredentials(final ZipOutputStream zipOut,
                                                    final List<EndorsementCredential> endorsementCredentials)
            throws IOException {
        String zipFileName;
        final String singleFileName = "Endorsement_Certificates";

        // get all endorsement credentials
        for (EndorsementCredential endorsementCredential : endorsementCredentials) {
            zipFileName = String.format("%s[%s].cer", singleFileName,
                    Integer.toHexString(endorsementCredential.getCertificateHash()));
            // configure the zip entry, the properties of the 'file'
            ZipEntry zipEntry = new ZipEntry(zipFileName);
            zipEntry.setSize((long) endorsementCredential.getRawBytes().length * Byte.SIZE);
            zipEntry.setTime(System.currentTimeMillis());
            zipOut.putNextEntry(zipEntry);
            // the content of the resource
            StreamUtils.copy(endorsementCredential.getRawBytes(), zipOut);
            zipOut.closeEntry();
        }
        zipOut.finish();
    }
}
