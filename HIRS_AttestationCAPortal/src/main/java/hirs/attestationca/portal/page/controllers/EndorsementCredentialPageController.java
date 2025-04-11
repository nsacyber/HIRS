package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
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
import jakarta.persistence.EntityNotFoundException;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

/**
 * Controller for the Endorsement Key Credentials page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/endorsement-key-credentials")
public class EndorsementCredentialPageController extends PageController<NoPageParams> {

    private static final String ENDORSEMENT_CREDENTIALS = "endorsement-key-credentials";

    private final EndorsementCredentialRepository endorsementCredentialRepository;
    private final CertificateService certificateService;

    /**
     * Constructor for the Endorsement Credential page.
     *
     * @param endorsementCredentialRepository endorsementCredentialRepository
     * @param certificateService              certificateService
     */
    @Autowired
    public EndorsementCredentialPageController(
            final EndorsementCredentialRepository endorsementCredentialRepository,
            final CertificateService certificateService) {
        super(Page.ENDORSEMENT_KEY_CREDENTIALS);
        this.endorsementCredentialRepository = endorsementCredentialRepository;
        this.certificateService = certificateService;
    }

    /**
     * Returns the path for the view and the data model for the Endorsement Key Credentials page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the Endorsement Key Credentials page.
     */
    @RequestMapping
    public ModelAndView initPage(
            final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.ENDORSEMENT_KEY_CREDENTIALS);
    }

    /**
     * Processes request to retrieve the collection of endorsement credentials that will be
     * displayed on the endorsement credentials page.
     *
     * @param input data table input received from the front-end
     * @return data table of endorsement credentials
     */
    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<EndorsementCredential> getEndorsementCredentialsTableData(
            final DataTableInput input) {
        log.info("Received request to display list of endorsement credentials");
        log.debug("Request received a datatable input object for the endorsement credentials page: {}",
                input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();

        log.debug("Ordering on column: {}", orderColumnName);

        final String searchText = input.getSearch().getValue();
        final List<String> searchableColumns = findSearchableColumnsNames(input.getColumns());

        int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        FilteredRecordsList<EndorsementCredential> records = new FilteredRecordsList<>();

        org.springframework.data.domain.Page<EndorsementCredential> pagedResult;

        if (StringUtils.isBlank(searchText)) {
            pagedResult = this.endorsementCredentialRepository.findByArchiveFlag(false, pageable);
        } else {
            pagedResult =
                    this.certificateService.findCertificatesBySearchableColumnsAndArchiveFlag(
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
     * Processes request to download the endorsement credential by writing it to the response stream
     * for download.
     *
     * @param id       the UUID of the endorsement credential to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadSingleEndorsementCredential(
            @RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download endorsement credential id {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Certificate certificate = this.certificateService.findCertificate(uuid);

            if (certificate == null) {
                final String errorMessage = "Unable to locate endorsement credential record with ID " + uuid;
                log.warn(errorMessage);
                throw new EntityNotFoundException(errorMessage);
            } else if (!(certificate instanceof EndorsementCredential)) {
                final String errorMessage =
                        "Unable to cast the found certificate to a endorsement credential object";
                log.warn(errorMessage);
                throw new ClassCastException(errorMessage);

            }

            final EndorsementCredential endorsementCredential = (EndorsementCredential) certificate;

            final String fileName = "filename=\"" + EndorsementCredential.class.getSimpleName()
                    + "_"
                    + endorsementCredential.getSerialNumber()
                    + ".cer\"";

            // Set filename for download.
            response.setHeader("Content-Disposition", "attachment;" + fileName);
            response.setContentType("application/octet-stream");

            // write endorsement credential to output stream
            response.getOutputStream().write(certificate.getRawBytes());

        } catch (Exception ex) {
            log.error("An exception was thrown while attempting to download the"
                    + " specified endorsement credential", ex);

            // send a 404 error when an exception is thrown while attempting to download the
            // specified endorsement credential
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes request to bulk download all the endorsement credentials by writing it to the response stream
     * for download in bulk.
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadEndorsementCredentials(final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download all endorsement credentials");

        final String fileName = "endorsement_certificates.zip";
        final String singleFileName = "Endorsement_Certificates";

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            //  write endorsement credentials to output stream and bulk download them
            this.certificateService.bulkDownloadCertificates(zipOut, ENDORSEMENT_CREDENTIALS, singleFileName);
        } catch (Exception ex) {
            log.error("An exception was thrown while attempting to bulk download all the"
                    + "endorsement credentials", ex);

            // send a 404 error when an exception is thrown while attempting to download the
            // endorsement credentials
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes request to upload one or more endorsement credentials to the ACA.
     *
     * @param files the files to process
     * @param attr  the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView uploadEndorsementCredential(
            @RequestParam("file") final MultipartFile[] files,
            final RedirectAttributes attr) throws URISyntaxException {

        log.info("Received request to upload one or more endorsement credentials");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        for (MultipartFile file : files) {
            List<String> errorMessages = new ArrayList<>();
            List<String> successMessages = new ArrayList<>();

            //Parse endorsement credential
            EndorsementCredential parsedEndorsementCredential = parseEndorsementCredential(file, messages);

            //Store only if it was parsed
            if (parsedEndorsementCredential != null) {
                certificateService.storeCertificate(
                        ENDORSEMENT_CREDENTIALS,
                        file.getOriginalFilename(),
                        successMessages, errorMessages, parsedEndorsementCredential);

                messages.addSuccessMessages(successMessages);
                messages.addErrorMessages(errorMessages);
            }
        }

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);

        return redirectTo(Page.ENDORSEMENT_KEY_CREDENTIALS, new NoPageParams(), model, attr);
    }

    /**
     * Processes request to archive/soft delete the provided endorsement credential.
     *
     * @param id   the UUID of the endorsement certificate to delete
     * @param attr RedirectAttributes used to forward data back to the original
     *             page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteEndorsementCredential(
            @RequestParam final String id,
            final RedirectAttributes attr) throws URISyntaxException {
        log.info("Received request to delete endorsement credential id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        try {
            UUID uuid = UUID.fromString(id);

            this.certificateService.deleteCertificate(uuid, ENDORSEMENT_CREDENTIALS,
                    successMessages, errorMessages);

            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete"
                    + " endorsement credential";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ENDORSEMENT_KEY_CREDENTIALS, new NoPageParams(), model, attr);
    }

    /**
     * Helper method that returns a list of column names that are searchable.
     *
     * @param columns columns
     * @return searchable column names
     */
    private List<String> findSearchableColumnsNames(final List<Column> columns) {
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
    private EndorsementCredential parseEndorsementCredential(final MultipartFile file,
                                                             final PageMessages messages) {
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
            messages.addErrorMessage(failMessage + ioEx.getMessage());
            return null;
        }

        // attempt to build the endorsement credential from the uploaded bytes
        try {
            return new EndorsementCredential(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded endorsement credential file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addErrorMessage(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded endorsement credential pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            messages.addErrorMessage(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format(
                    "Endorsement credential format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            messages.addErrorMessage(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage = String.format(
                    "Unexpected object while parsing endorsement credential %s ", fileName);
            log.error(failMessage, isEx);
            messages.addErrorMessage(failMessage + isEx.getMessage());
            return null;
        }
    }
}
