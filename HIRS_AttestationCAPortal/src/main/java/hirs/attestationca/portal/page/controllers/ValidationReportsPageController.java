package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.data.persist.certificate.attributes.ComponentIdentifier;
import hirs.persist.CertificateManager;
import org.apache.logging.log4j.Logger;
import static org.apache.logging.log4j.LogManager.getLogger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import static hirs.attestationca.portal.page.Page.VALIDATION_REPORTS;
import hirs.FilteredRecordsList;
import hirs.data.persist.SupplyChainValidationSummary;
import hirs.persist.CriteriaModifier;
import hirs.persist.CrudManager;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Controller for the Validation Reports page.
 */
@Controller
@RequestMapping("/validation-reports")
public class ValidationReportsPageController extends PageController<NoPageParams> {

    private final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager;
    private final CertificateManager certificateManager;

    private static final Logger LOGGER = getLogger(ValidationReportsPageController.class);

    /**
     * Constructor providing the Page's display and routing specification.
     * @param supplyChainValidatorSummaryManager the manager
     */
    @Autowired
    public ValidationReportsPageController(
            final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager,
            final CertificateManager certificateManager) {
        super(VALIDATION_REPORTS);
        this.supplyChainValidatorSummaryManager = supplyChainValidatorSummaryManager;
        this.certificateManager = certificateManager;
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView();
    }

    /**
     * Gets the list of validation summaries per the data table input query.
     * @param input the data table query.
     * @return the data table response containing the supply chain summary records
     */
    @ResponseBody
    @RequestMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<SupplyChainValidationSummary> getTableData(
            final DataTableInput input) {

        LOGGER.debug("Handling request for summary list: " + input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();

        LOGGER.debug("Ordering on column: " + orderColumnName);

        // define an alias so the composite object, device, can be used by the
        // datatables / query. This is necessary so the device.name property can
        // be used.
        CriteriaModifier criteriaModifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                criteria.add(Restrictions.isNull(Certificate.ARCHIVE_FIELD));
                criteria.createAlias("device", "device");
            }
        };

        FilteredRecordsList<SupplyChainValidationSummary> records =
                OrderedListQueryDataTableAdapter.getOrderedList(
                        SupplyChainValidationSummary.class,
                        supplyChainValidatorSummaryManager, input, orderColumnName,
                        criteriaModifier);

        return new DataTableResponse<>(records, input);
    }

    @RequestMapping(value = "download", method = RequestMethod.GET)
    public void download(@RequestParam final String id,
                         final HttpServletResponse response) {
        LOGGER.info("Downloading validation report for " + id);
        UUID uuid = UUID.fromString(id);
        PlatformCredential pc = PlatformCredential.select(certificateManager).byDeviceId(uuid).getCertificate();
        LOGGER.info("Verified manufacturer: " + pc.getManufacturer());
        LOGGER.info("Model: " + pc.getModel());
        LOGGER.info("SN: " + pc.getChassisSerialNumber());
        LOGGER.info("Verification date: " + pc.getBeginValidity());
        if (pc.getComponentIdentifiers() != null &&
                pc.getComponentIdentifiers().size() > 0) {
            for (ComponentIdentifier ci : pc.getComponentIdentifiers()) {
                LOGGER.info("Manufacturer ID: " + ci.getComponentManufacturerId().toString() +
                        "\nModel: " + ci.getComponentModel().getString() +
                        "\nRevision: " + ci.getComponentRevision().getString());
            }
        }
    }
}
