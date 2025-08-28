package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.service.CertificateService;
import hirs.attestationca.persist.service.CertificateType;
import hirs.attestationca.persist.service.IssuedAttestationCertificatePageService;
import hirs.attestationca.persist.util.DownloadFile;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

/**
 * Controller for the Issued Certificates page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/issued-certificates")
public class IssuedCertificatePageController extends PageController<NoPageParams> {
    private final IssuedAttestationCertificatePageService issuedAttestationCertificateService;
    private final CertificateService certificateService;

    /**
     * Constructor for the Issued Attestation Certificate page.
     *
     * @param issuedAttestationCertificateService issued certificate service
     * @param certificateService                  certificate service
     */
    @Autowired
    public IssuedCertificatePageController(
            final IssuedAttestationCertificatePageService issuedAttestationCertificateService,
            final CertificateService certificateService) {
        super(Page.ISSUED_CERTIFICATES);
        this.issuedAttestationCertificateService = issuedAttestationCertificateService;
        this.certificateService = certificateService;
    }

    /**
     * Returns the path for the view and the data model for the Issued Attestation Certificate page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the Issued Attestation Certificate page.
     */
    @RequestMapping
    public ModelAndView initPage(
            final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.ISSUED_CERTIFICATES);
    }

    /**
     * Processes the request to retrieve a list of issued attestation certificates
     * for display on the issued certificates page.
     *
     * @param input data table input received from the front-end
     * @return data table of issued certificates
     */
    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<IssuedAttestationCertificate> getIssuedCertificatesTableData(
            final DataTableInput input) {
        log.info("Received request to display list of issued attestation certificates");
        log.debug("Request received a datatable input object for the issued attestation"
                + " certificate page: {}", input);


        String orderColumnName = input.getOrderColumnName();

        log.debug("Ordering on column: {}", orderColumnName);

        final String searchTerm = input.getSearch().getValue();
        final Set<String> searchableColumns =
                ControllerPagesUtils.findSearchableColumnsNames(IssuedAttestationCertificate.class,
                        input.getColumns());

        final int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        FilteredRecordsList<IssuedAttestationCertificate> issuedCertificateFilteredRecordsList =
                new FilteredRecordsList<>();
        org.springframework.data.domain.Page<IssuedAttestationCertificate> pagedResult;

        if (StringUtils.isBlank(searchTerm)) {
            pagedResult =
                    this.issuedAttestationCertificateService.findByArchiveFlag(false, pageable);
        } else {
            pagedResult =
                    this.certificateService.findCertificatesBySearchableColumnsAndArchiveFlag(
                            IssuedAttestationCertificate.class,
                            searchableColumns,
                            searchTerm,
                            false, pageable);
        }

        if (pagedResult.hasContent()) {
            issuedCertificateFilteredRecordsList.addAll(pagedResult.getContent());
        }

        issuedCertificateFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        issuedCertificateFilteredRecordsList.setRecordsTotal(
                this.issuedAttestationCertificateService.findIssuedCertificateRepoCount());

        log.info("Returning the size of the list of issued certificates: "
                + "{}", issuedCertificateFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(issuedCertificateFilteredRecordsList, input);
    }

    /**
     * Processes the request to download the specified issued attestation certificate.
     *
     * @param id       the UUID of the issued attestation certificate to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadIssuedCertificate(
            @RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download issued certificate id {}", id);

        try {
            final DownloadFile downloadFile =
                    this.certificateService.downloadCertificate(IssuedAttestationCertificate.class,
                            UUID.fromString(id));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;"
                    + downloadFile.getFileName());
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.getOutputStream().write(downloadFile.getFileBytes());
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the"
                    + " specified issued attestation certificate", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to bulk download all the issued attestation certificates.
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadIssuedCertificates(final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download all issued certificates");

        final String singleFileName = "Issued_Certificate";
        final String fileName = "issued_certificates.zip";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            this.certificateService.bulkDownloadCertificates(zipOut, CertificateType.ISSUED_CERTIFICATES,
                    singleFileName);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the"
                    + "issued attestation certificates", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to archive/soft delete the specified issued attestation certificate.
     *
     * @param id   the UUID of the issued attestation certificate to delete
     * @param attr RedirectAttributes used to forward data back to the original
     *             page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteIssuedCertificate(
            @RequestParam final String id,
            final RedirectAttributes attr) throws URISyntaxException {
        log.info("Received request to delete issued attestation certificate id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            this.certificateService.deleteCertificate(UUID.fromString(id),
                    successMessages, errorMessages);
            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete"
                    + " the specified issued attestation certificate";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ISSUED_CERTIFICATES, new NoPageParams(), model, attr);
    }
}
