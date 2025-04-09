package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.IssuedCertificateRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
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

@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/issued-certificates")
public class IssuedCertificateController extends PageController<NoPageParams> {

    private static final String ISSUED_CERTIFICATES = "issued-certificates";

    private final IssuedCertificateRepository issuedCertificateRepository;
    private final CertificateService certificateService;

    @Autowired
    public IssuedCertificateController(
            final IssuedCertificateRepository issuedCertificateRepository,
            final CertificateService certificateService) {
        super(Page.TRUST_CHAIN);
        this.issuedCertificateRepository = issuedCertificateRepository;
        this.certificateService = certificateService;
    }

    /**
     * Returns the path for the view and the data model for the Issued Attestation Certificate page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the page.
     */
    @RequestMapping
    public ModelAndView initPage(
            final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.ISSUED_CERTIFICATES);
    }

    /**
     * @param input
     * @return
     */
    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<IssuedAttestationCertificate> getIssuedCertificatesTableData(
            final DataTableInput input) {
        log.debug("Handling list request for issued certificates: {}", input);

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
     * Handles request to download the issued attestation certificate by writing it to the response stream
     * for download.
     *
     * @param id       the UUID of the issued attestation certificate to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadSingleIssuedCertificate(
            @RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download issued certificate id {}", id);

        try {
            UUID uuid = UUID.fromString(id);
            Certificate certificate = this.certificateService.findCertificate(uuid);

            if (certificate == null) {
                // Use the term "record" here to avoid user confusion b/t cert and cred
                String notFoundMessage = "Unable to locate record with ID: " + uuid;
                log.warn(notFoundMessage);
                // send a 404 error when invalid certificate
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else if (certificate instanceof IssuedAttestationCertificate uploadedIssuedCertificate) {
                String fileName = "filename=\"" + IssuedAttestationCertificate.class.getSimpleName()
                        + "_"
                        + uploadedIssuedCertificate.getSerialNumber()
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
     * Handles request to download the issued attestation certificates by writing it to the response stream
     * for download in bulk.
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadIssuedCertificates(final HttpServletResponse response)
            throws IOException {
        log.info("Handling request to download all issued certificates");

        final String singleFileName = "Issued_Certificate";
        final String fileName = "issued_certificates.zip";

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            //  write issued attestation certificates to output stream and bulk download them
            this.certificateService.bulkDownloadCertificates(zipOut, ISSUED_CERTIFICATES, singleFileName);
        } catch (Exception ex) {
            log.error("Failed to bulk download issued certificates:", ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Archives (soft deletes) the issued attestation certificate.
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
        log.info("Handling request to delete issued attestation certificate id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            List<String> successMessages = new ArrayList<>();
            List<String> errorMessages = new ArrayList<>();

            UUID uuid = UUID.fromString(id);

            this.certificateService.deleteCertificate(uuid, ISSUED_CERTIFICATES,
                    successMessages, errorMessages);

        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from issued attestation certificate: " + id;
            messages.addError(uuidError);
            log.error(uuidError, ex);
        } catch (DBManagerException ex) {
            String dbError = "Failed to archive issued attestation certificate: " + id;
            messages.addError(dbError);
            log.error(dbError, ex);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ISSUED_CERTIFICATES, new NoPageParams(), model, attr);
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
}
