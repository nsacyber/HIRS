package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.data.persist.certificate.Certificate;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import static hirs.attestationca.portal.page.Page.VALIDATION_REPORTS;
import hirs.FilteredRecordsList;
import hirs.data.persist.SupplyChainValidationSummary;
import hirs.persist.CriteriaModifier;
import hirs.persist.CrudManager;

/**
 * Controller for the Validation Reports page.
 */
@Controller
@RequestMapping("/validation-reports")
public class ValidationReportsPageController extends PageController<NoPageParams> {

    private final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager;

    private static final Logger LOGGER = getLogger(ValidationReportsPageController.class);

    /**
     * Constructor providing the Page's display and routing specification.
     * @param supplyChainValidatorSummaryManager the manager
     */
    @Autowired
    public ValidationReportsPageController(
            final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager) {
        super(VALIDATION_REPORTS);
        this.supplyChainValidatorSummaryManager = supplyChainValidatorSummaryManager;
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
                OrderedListQueryDataTableAdapter.getOrderedList(SupplyChainValidationSummary.class,
                        supplyChainValidatorSummaryManager, input, orderColumnName, criteriaModifier);

        return new DataTableResponse<>(records, input);
    }
}
