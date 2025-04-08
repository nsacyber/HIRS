//package hirs.attestationca.portal.page.controllers;
//
//import hirs.attestationca.persist.FilteredRecordsList;
//import hirs.attestationca.persist.entity.manager.IDevIDCertificateRepository;
//import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
//import hirs.attestationca.persist.service.CertificateService;
//import hirs.attestationca.portal.datatables.Column;
//import hirs.attestationca.portal.datatables.DataTableInput;
//import hirs.attestationca.portal.datatables.DataTableResponse;
//import hirs.attestationca.portal.page.Page;
//import hirs.attestationca.portal.page.PageController;
//import hirs.attestationca.portal.page.params.NoPageParams;
//import lombok.extern.log4j.Log4j2;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//import org.springframework.web.servlet.ModelAndView;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Log4j2
//@Controller
//@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/idevid-certificates")
//public class IDevIdCertificatePageController extends PageController<NoPageParams> {
//
//    private final IDevIDCertificateRepository iDevIDCertificateRepository;
//    private final CertificateService certificateService;
//
//    @Autowired
//    public IDevIdCertificatePageController(final IDevIDCertificateRepository iDevIDCertificateRepository,
//                                           final CertificateService certificateService) {
//        super(Page.TRUST_CHAIN);
//        this.iDevIDCertificateRepository = iDevIDCertificateRepository;
//        this.certificateService = certificateService;
//    }
//
//    /**
//     * Returns the path for the view and the data model for the page.
//     *
//     * @param params The object to map url parameters into.
//     * @param model  The data model for the request. Can contain data from
//     *               redirect.
//     * @return the path for the view and data model for the page.
//     */
//    @RequestMapping
//    public ModelAndView initPage(
//            final NoPageParams params, final Model model) {
//        return getBaseModelAndView(Page.IDEVID_CERTIFICATES);
//    }
//
//
//    @ResponseBody
//    @GetMapping(value = "/list",
//            produces = MediaType.APPLICATION_JSON_VALUE)
//    public DataTableResponse<IDevIDCertificate> getIDevIdCertificatesTableData(
//            final DataTableInput input) {
//
//        log.debug("Handling list request: {}", input);
//
//        // attempt to get the column property based on the order index.
//        String orderColumnName = input.getOrderColumnName();
//
//        log.debug("Ordering on column: {}", orderColumnName);
//
//        String searchText = input.getSearch().getValue();
//        List<String> searchableColumns = findSearchableColumnsNames(input.getColumns());
//
//        int currentPage = input.getStart() / input.getLength();
//        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));
//
//        FilteredRecordsList<IDevIDCertificate> records = new FilteredRecordsList<>();
//        org.springframework.data.domain.Page<IDevIDCertificate> pagedResult;
//
//        if (StringUtils.isBlank(searchText)) {
//            pagedResult =
//                    this.iDevIDCertificateRepository.findByArchiveFlag(false, pageable);
//        } else {
//            pagedResult =
//                    this.certificateService.findBySearchableColumnsAndArchiveFlag(
//                            IDevIDCertificate.class,
//                            searchableColumns,
//                            searchText,
//                            false, pageable);
//        }
//
//        if (pagedResult.hasContent()) {
//            records.addAll(pagedResult.getContent());
//            records.setRecordsTotal(pagedResult.getContent().size());
//        } else {
//            records.setRecordsTotal(input.getLength());
//        }
//
//        records.setRecordsFiltered(iDevIDCertificateRepository.findByArchiveFlag(false).size());
//
//        log.debug("Returning the size of the list of IDEVID certificates: {}", records.size());
//        return new DataTableResponse<>(records, input);
//    }
//
//    /**
//     * Helper method that returns a list of column names that are searchable.
//     *
//     * @return searchable column names
//     */
//    private List<String> findSearchableColumnsNames(List<Column> columns) {
//
//        // Retrieve all searchable columns and collect their names into a list of strings.
//        return columns.stream().filter(Column::isSearchable).map(Column::getName)
//                .collect(Collectors.toList());
//    }
//}
