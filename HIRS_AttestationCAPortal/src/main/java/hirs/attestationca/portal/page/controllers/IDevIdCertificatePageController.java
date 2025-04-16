package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.IDevIDCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
import hirs.attestationca.persist.service.CertificateService;
import hirs.attestationca.persist.service.CertificateType;
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
 * Controller for the IDevID Certificates page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/idevid-certificates")
public class IDevIdCertificatePageController extends PageController<NoPageParams> {
    private final IDevIDCertificateRepository iDevIDCertificateRepository;
    private final CertificateService certificateService;

    /**
     * Constructor for the IDevID Certificate page.
     *
     * @param iDevIDCertificateRepository iDevIDCertificateRepository
     * @param certificateService          certificateService
     */
    @Autowired
    public IDevIdCertificatePageController(final IDevIDCertificateRepository iDevIDCertificateRepository,
                                           final CertificateService certificateService) {
        super(Page.IDEVID_CERTIFICATES);
        this.iDevIDCertificateRepository = iDevIDCertificateRepository;
        this.certificateService = certificateService;
    }

    /**
     * Returns the path for the view and the data model for the IDevId Certificate page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the IDevId Certificate page.
     */
    @RequestMapping
    public ModelAndView initPage(
            final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.IDEVID_CERTIFICATES);
    }

    /**
     * Processes request to retrieve the collection of idevid certificates that will
     * be displayed on the idevid certificates page.
     *
     * @param input data table input received from the front-end
     * @return data table of idevid certificates
     */
    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<IDevIDCertificate> getIDevIdCertificatesTableData(
            final DataTableInput input) {

        log.info("Received request to display list of idevid certificates");
        log.debug("Request received a datatable input object for the idevid certificates page: {}", input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();

        log.debug("Ordering on column: {}", orderColumnName);

        final String searchText = input.getSearch().getValue();
        final List<String> searchableColumns = findSearchableColumnsNames(input.getColumns());

        int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        FilteredRecordsList<IDevIDCertificate> records = new FilteredRecordsList<>();
        org.springframework.data.domain.Page<IDevIDCertificate> pagedResult;

        if (StringUtils.isBlank(searchText)) {
            pagedResult =
                    this.iDevIDCertificateRepository.findByArchiveFlag(false, pageable);
        } else {
            pagedResult =
                    this.certificateService.findCertificatesBySearchableColumnsAndArchiveFlag(
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

        log.info("Returning the size of the list of IDEVID certificates: {}", records.size());
        return new DataTableResponse<>(records, input);
    }

    /**
     * Processes request to download the IDevId certificate by writing it to the response stream
     * for download.
     *
     * @param id       the UUID of the idevid certificate to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadSingleIDevIdCertificate(
            @RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download idevid certificate id {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Certificate certificate = this.certificateService.findCertificate(uuid);

            if (certificate == null) {
                final String errorMessage = "Unable to locate idevid certificate record with ID " + uuid;
                log.warn(errorMessage);
                throw new EntityNotFoundException(errorMessage);
            } else if (!(certificate instanceof IDevIDCertificate)) {
                final String errorMessage =
                        "Unable to cast the found certificate to a idevid certificate object";
                log.warn(errorMessage);
                throw new ClassCastException(errorMessage);

            }
            final IDevIDCertificate iDevIDCertificate = (IDevIDCertificate) certificate;

            final String fileName = "filename=\"" + IDevIDCertificate.class.getSimpleName()
                    + "_"
                    + iDevIDCertificate.getSerialNumber()
                    + ".cer\"";

            // Set filename for download.
            response.setHeader("Content-Disposition", "attachment;" + fileName);
            response.setContentType("application/octet-stream");

            // write idevid certificate to output stream
            response.getOutputStream().write(certificate.getRawBytes());
        } catch (Exception ex) {
            log.error("An exception was thrown while attempting to download the"
                    + " specified idevid certificate", ex);

            // send a 404 error when an exception is thrown while attempting to download the
            // specified idevid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes request to bulk download all the IDevID Certificates by writing it to the response stream
     * for download in bulk.
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadIDevIdCertificates(final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download all idevid certificates");

        final String fileName = "idevid_certificates.zip";
        final String singleFileName = "IDevID_Certificates";

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            //  write idevid certificates to output stream and bulk download them
            this.certificateService.bulkDownloadCertificates(zipOut, CertificateType.IDEVID_CERTIFICATES,
                    singleFileName);
        } catch (Exception ex) {
            log.error("An exception was thrown while attempting to bulk download all the"
                    + "idevid certificates", ex);

            // send a 404 error when an exception is thrown while attempting to download the
            // specified platform credential
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes request to upload one or more idevid certificates to the ACA.
     *
     * @param files the files to process
     * @param attr  the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView uploadIDevIdCertificate(
            @RequestParam("file") final MultipartFile[] files,
            final RedirectAttributes attr) throws URISyntaxException {

        log.info("Received request to upload one or more idevid certificates");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        for (MultipartFile file : files) {
            List<String> errorMessages = new ArrayList<>();
            List<String> successMessages = new ArrayList<>();

            //Parse IDevId Certificate
            IDevIDCertificate parsedIDevIDCertificate =
                    parseIDevIDCertificate(file, messages);

            //Store only if it was parsed
            if (parsedIDevIDCertificate != null) {
                certificateService.storeCertificate(
                        CertificateType.IDEVID_CERTIFICATES,
                        file.getOriginalFilename(),
                        successMessages, errorMessages, parsedIDevIDCertificate);

                messages.addSuccessMessages(successMessages);
                messages.addErrorMessages(errorMessages);
            }
        }

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);

        return redirectTo(Page.IDEVID_CERTIFICATES, new NoPageParams(), model, attr);
    }

    /**
     * Processes request to archive/soft delete the provided idevid certificate.
     *
     * @param id   the UUID of the idevid certificate to delete
     * @param attr RedirectAttributes used to forward data back to the original
     *             page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteIdevIdCertificate(
            @RequestParam final String id,
            final RedirectAttributes attr) throws URISyntaxException {
        log.info("Received request to delete idevid certificate id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            UUID uuid = UUID.fromString(id);

            this.certificateService.deleteCertificate(uuid, CertificateType.IDEVID_CERTIFICATES,
                    successMessages, errorMessages);

            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete"
                    + " the specified idevid certificate";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.IDEVID_CERTIFICATES, new NoPageParams(), model, attr);
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
     * Attempts to parse the provided file in order to create an IDevId Certificate.
     *
     * @param file     file
     * @param messages page messages
     * @return IDevId certificate
     */
    private IDevIDCertificate parseIDevIDCertificate(final MultipartFile file,
                                                     final PageMessages messages) {
        log.info("Received IDevId certificate file of size: {}", file.getSize());

        byte[] fileBytes;
        String fileName = file.getOriginalFilename();

        // attempt to retrieve file bytes from the provided file
        try {
            fileBytes = file.getBytes();
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to read uploaded IDevId certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addErrorMessage(failMessage + ioEx.getMessage());
            return null;
        }

        // attempt to build the IDevId certificate from the uploaded bytes
        try {
            return new IDevIDCertificate(fileBytes);
        } catch (IOException ioEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded IDevId certificate file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addErrorMessage(failMessage + ioEx.getMessage());
            return null;
        } catch (DecoderException dEx) {
            final String failMessage = String.format(
                    "Failed to parse uploaded IDevId certificate pem file (%s): ", fileName);
            log.error(failMessage, dEx);
            messages.addErrorMessage(failMessage + dEx.getMessage());
            return null;
        } catch (IllegalArgumentException iaEx) {
            final String failMessage = String.format(
                    "IDevId certificate format not recognized(%s): ", fileName);
            log.error(failMessage, iaEx);
            messages.addErrorMessage(failMessage + iaEx.getMessage());
            return null;
        } catch (IllegalStateException isEx) {
            final String failMessage = String.format(
                    "Unexpected object while parsing IDevId certificate %s ", fileName);
            log.error(failMessage, isEx);
            messages.addErrorMessage(failMessage + isEx.getMessage());
            return null;
        }
    }
}
