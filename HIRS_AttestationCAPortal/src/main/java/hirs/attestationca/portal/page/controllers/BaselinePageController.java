package hirs.portal.controller;

import hirs.FilteredRecordsList;
import hirs.data.bean.SimpleBaselineBean;
import hirs.data.persist.Alert;
import hirs.data.persist.Baseline;
import hirs.data.persist.BroadRepoImaBaseline;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.Digest;
import hirs.data.persist.DigestAlgorithm;
import hirs.data.persist.FirmwareInfo;
import hirs.data.persist.HardwareInfo;
import hirs.data.persist.IMABaselineRecord;
import hirs.data.persist.IMAPolicy;
import hirs.data.persist.ImaAcceptableRecordBaseline;
import hirs.data.persist.ImaBaseline;
import hirs.data.persist.ImaIgnoreSetBaseline;
import hirs.data.persist.ImaIgnoreSetRecord;
import hirs.data.persist.ImaBlacklistBaseline;
import hirs.data.persist.ImaBlacklistRecord;
import hirs.data.persist.IntegrityReport;
import hirs.data.persist.OSInfo;
import hirs.data.persist.Policy;
import hirs.data.persist.SimpleImaBaseline;
import hirs.data.persist.TPMBaseline;
import hirs.data.persist.TPMInfo;
import hirs.data.persist.TPMMeasurementRecord;
import hirs.data.persist.TPMPolicy;
import hirs.data.persist.TPMReport;
import hirs.data.persist.TargetedRepoImaBaseline;
import hirs.data.persist.TpmWhiteListBaseline;
import hirs.data.persist.TpmBlackListBaseline;
import hirs.ima.CSVGenerator;
import hirs.ima.IMABaselineGeneratorException;
import hirs.ima.SimpleImaBaselineGenerator;
import hirs.ima.ImaBlacklistBaselineGenerator;
import hirs.persist.BaselineManager;
import hirs.persist.BaselineManagerException;
import hirs.persist.ImaBaselineRecordManager;
import hirs.persist.ImaBlacklistBaselineRecordManager;
import hirs.persist.ImaBaselineRecordManagerException;
import hirs.persist.ImportBaselineCSV;
import hirs.persist.PolicyManager;
import hirs.persist.ReportManager;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.tpm.TPMBaselineGenerator;

import org.apache.http.client.utils.URIBuilder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

import static org.apache.logging.log4j.LogManager.getLogger;
import org.springframework.ui.Model;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides Baseline CRUD operations as REST end points.
 */
@Controller
@RequestMapping("/reference-manifests")
public class BaselinePageController extends PageController<NoPageParams> {

    private static final Logger LOGGER = getLogger(BaselinePageController.class);

    private static final String PAGE_LIST = "list";

    private static final String DESC = "desc";
    private static final int TIMESTAMP = 0;
    private static final int NAME = 1;

    private static final int REPOSITORY = 0;
    private static final int PACKAGE = 1;
    private static final int VERSION = 2;
    private static final int RELEASE = 3;

    //Used for listing ima baseline records
    private static final int PATH = 0;
    private static final int HASH = 1;

    //Redirect URLS
    private static final String REDIRECT_BASELINES_JSP = "/jsp/baselines.jsp";
    private static final String REDIRECT_IMA_BASELINE_JSP = "/jsp/editimabaseline.jsp";
    private static final String REDIRECT_IMA_BLACKLIST_BASELINE_JSP
            = "/jsp/editimablacklistbaseline.jsp";

    private static final String SRCH = "search[value]";

    private static final String DIR = "order[0][dir]";

    private static final String COLUMN = "order[0][column]";

    @Autowired
    private BaselineManager baselineManager;

    @Autowired
    private PolicyManager policyManager;

    @Autowired
    private ImaBaselineRecordManager imaBaselineRecordManager;

    @Autowired
    private ImaBlacklistBaselineRecordManager imaBlacklistBaselineRecordManager;

    @Autowired
    private ReportManager reportManager;

    @Autowired
    public BaselinePageController() {
        super(Page.REFERENCE_MANIFESTS);
    }

    @Override
    public ModelAndView initPage(NoPageParams params, Model model) {
        return getBaseModelAndView();
    }

    /**
     * Maps /baselines/ to the page listing page.
     *
     * @return {@link #PAGE_LIST}
     */
    @RequestMapping(method = RequestMethod.GET)
    public String index() {
        return PAGE_LIST;
    }

    /**
     * Maps /baselines/imaignore/list to get the IMA Ignore baseline list.
     *
     * @param name of the IMA Baseline
     * @return list of IMA ignore records
     */
    @ResponseBody
    @RequestMapping(value = "imaignore/list", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public HashMap<String, Object> imaIgnoreList(
            @RequestParam final String name) {

        // obtain and return the IMA Ignore list
        Baseline baseline = baselineManager.getBaseline(name);
        ImaIgnoreSetBaseline ignoreSet = (ImaIgnoreSetBaseline) baseline;
        Set<ImaIgnoreSetRecord> records = ignoreSet.getImaIgnoreRecords();

        HashMap<String, Object> data = new HashMap<>();
        data.put("data", records);

        return data;
    }

    /**
     * Maps /baselines/targeted/list to get the IMA targeted baseline list.
     *
     * @param name of the IMA Targeted Baseline
     * @return list of IMA targeted repository packages
     */
    @ResponseBody
    @RequestMapping(value = "targeted/list", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public HashMap<String, Object> targtedList(
            @RequestParam final String name) {

        // obtain and return the IMA Targeted packages list
        TargetedRepoImaBaseline baseline
                = (TargetedRepoImaBaseline) baselineManager.getBaseline(name);

        HashMap<String, Object> data = new HashMap<>();
        data.put("data", baseline.getRepositoryPackages());

        return data;
    }

    /**
     * Maps /baselines/tpm/list to get the TPM baseline list.
     *
     * @param name of the TPM Baseline
     * @return list of PCR records
     */
    @ResponseBody
    @RequestMapping(value = "tpm/list", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public HashMap<String, Object> tpmList(
            @RequestParam final String name) {

        List<HashMap<String, Object>> records = new ArrayList<>();

        // obtain and return the IMA Ignore list
        Baseline baseline = baselineManager.getBaseline(name);
        TPMBaseline tpmBaseline = (TPMBaseline) baseline;
        for (TPMMeasurementRecord record : tpmBaseline.getPcrRecords()) {
            HashMap<String, Object> temp = new HashMap<>();
            int pcr = record.getPcrId();
            String hex = record.getHash().getDigestString();
            String id = String.valueOf(pcr) + hex;

            temp.put("id", id);
            temp.put("pcr", pcr);
            temp.put("hex", hex);

            records.add(temp);
        }

        HashMap<String, Object> data = new HashMap<>();
        data.put("data", records);

        return data;
    }

    /**
     * Creates a new Baseline with the specified name and type. The CSV file is
     * used by the {@link
     * ImportBaselineCSV} utility to construct the actual baseline.
     *
     * @param name of the baseline to create
     * @param baselineType "IMA", "TPM", or "Ignore"
     * @param file CSV baseline content
     * @return redirect to baseline page
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public String createBaselineFromCSV(@RequestParam("name") final String name,
            @RequestParam("type") final String baselineType,
            @RequestParam("file") final MultipartFile file) {

        String result = "", error = "";
        Baseline baseline = null;

//        try {
//            if (baselineType.equalsIgnoreCase("SwidTag")) {
//                File temp = new File(file.getOriginalFilename());
//                file.transferTo(temp);
//                baseline = ImportBaselineCSV.createBaseline(name,
//                        (new SwidTagGateway()).parsePayload(temp.getAbsolutePath()),
//                        "TPMWhite");
//            } else {
//                baseline = ImportBaselineCSV.createBaseline(name, file.getInputStream(), baselineType);
//            }
//            baselineManager.saveBaseline(baseline);
//            result = "Baseline imported successfully";
//        } catch (IOException ex) {
//            error = "Baseline import failed due to: " + ExceptionUtils.getRootCauseMessage(ex);
//        } catch (RuntimeException ex) {
//            error = "Error importing CSV: " + ex.getMessage();
//        }
        return "redirect:/jsp/baselines.jsp?result=" + result + "&error=" + error;
    }

    /**
     * Updates a Baseline with the specified name. The CSV file is used by the {@link
     * SimpleImaBaselineGenerator} utility to update the actual baseline.
     *
     * @param name of the baseline to update
     * @param file CSV baseline content
     * @return redirect to baseline's page
     */
    @RequestMapping(value = "/upload/update", method = RequestMethod.POST)
    public String updateBaselineFromCSV(@RequestParam("name") final String name,
            @RequestParam("file") final MultipartFile file) {

        String redirect = "redirect:";
        String result = "", error = "";

        try {
            if (baselineManager.getCompleteBaseline(name) instanceof SimpleImaBaseline) {
                redirect += REDIRECT_IMA_BASELINE_JSP + "?name=" + name;
                SimpleImaBaseline baseline
                        = (SimpleImaBaseline) baselineManager.getCompleteBaseline(name);
                final SimpleImaBaselineGenerator imaGenerator = new SimpleImaBaselineGenerator();
                imaGenerator.updateBaselineFromCSVFile(baseline, file.getInputStream());
                baselineManager.updateBaseline(baseline);
            } else if (baselineManager.getCompleteBaseline(name) instanceof ImaBlacklistBaseline) {
                redirect += REDIRECT_IMA_BLACKLIST_BASELINE_JSP + "?name=" + name;
                ImaBlacklistBaseline baseline
                        = (ImaBlacklistBaseline) baselineManager.getCompleteBaseline(name);
                ImaBlacklistBaselineGenerator.updateBaselineFromCSVFile(baseline,
                        file.getInputStream());
                baselineManager.updateBaseline(baseline);
            } else if (baselineManager.getCompleteBaseline(name) == null) {
                return "redirect:/jsp/baselines.jsp?name=" + name
                        + "&error=Baseline not found!";
            }
            result = "Baseline updated successfully";
        } catch (IOException | ParseException | IMABaselineGeneratorException ex) {
            error = "Baseline update failed due to: " + ExceptionUtils.getRootCauseMessage(ex);
        }

        return redirect + "&result=" + result + "&error=" + error;
    }

    /**
     * Deletes the Baseline with the specified name.
     *
     * @param name of the baseline to delete
     * @return redirect to baseline page
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public String deleteBaseline(@RequestParam(required = true) final String name) {
        final String url = "redirect:/jsp/baselines.jsp?error=";

        LOGGER.info("Attempting to delete baseline {}", name);
        String error;

        Set<String> policyNames = new HashSet<>();
        Baseline baseline = baselineManager.getBaseline(name);
        if (baseline == null) {
            error = "Baseline not found!";
            LOGGER.error(error);
            return url + error;
        }
        LOGGER.info("Checking if the baseline is part of any policies");
        if (baseline instanceof ImaBaseline) {
            List<Policy> policies = policyManager.getPolicyList(IMAPolicy.class);
            for (Policy policy : policies) {
                if (references(policy, baseline)) {
                    policyNames.add("<a href=\"editimapolicy.jsp?name="
                            + policy.getName() + "\">" + policy.getName() + "</a>");
                }
            }
        } else if (baseline instanceof TPMBaseline) {
            List<Policy> policies = policyManager.getPolicyList(TPMPolicy.class);
            for (Policy policy : policies) {
                TPMPolicy tpmPolicy = (TPMPolicy) policy;
                if (tpmPolicy.getTpmWhiteListBaselines().contains(baseline)
                        || tpmPolicy.getTpmBlackListBaselines().contains(baseline)) {
                    policyNames.add("<a href=\"edittpmpolicy.jsp?name="
                            + policy.getName() + "\">" + policy.getName() + "</a>");
                }
            }
        }

        if (!policyNames.isEmpty()) {
            error = "Baseline '" + name + "' is a part of the following policies and must be "
                    + "removed before it can be deleted:<br><ul>";
            for (String policyName : policyNames) {
                error += "<li>" + policyName + "</li>";
            }
            error += "</ul>";
            LOGGER.error(error);
            return url + error;
        }

        return "redirect:/jsp/baselines.jsp?result=Baseline was successfully deleted";
    }

    private static boolean references(final Policy policy, final Baseline baseline) {
        IMAPolicy imaPolicy = (IMAPolicy) policy;
        return imaPolicy.getWhitelists().contains(baseline)
                || imaPolicy.getRequiredSets().contains(baseline)
                || imaPolicy.getIgnoreSets().contains(baseline);
    }

    /**
     * Creates a Baseline with the specified name and type.
     *
     * @param name of the baseline to create
     * @param type of the baseline to create
     * @return redirect to baseline page
     */
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String createBaseline(
            @RequestParam(required = true) final String name,
            @RequestParam(required = true) final String type) {

        LOGGER.debug("Attempting to Create a baseline.");

        final Baseline baseline;
        final String redirect;
        switch (type) {
            case "broad":
                baseline = new BroadRepoImaBaseline(name);
                redirect = "editbroadbaseline.jsp";
                break;
            case "targeted":
                baseline = new TargetedRepoImaBaseline(name);
                redirect = "edittargetedbaseline.jsp";
                break;
            case "simple":
                baseline = new SimpleImaBaseline(name);
                redirect = "editimabaseline.jsp";
                break;
            case "ignore":
                baseline = new ImaIgnoreSetBaseline(name);
                redirect = "editimaignorebaseline.jsp";
                break;
            case "imaBlacklist":
                baseline = new ImaBlacklistBaseline(name);
                redirect = "editimablacklistbaseline.jsp";
                break;
            case "tpmWhitelist":
                baseline = new TpmWhiteListBaseline(name);
                redirect = "edittpmbaseline.jsp";
                break;
            case "tpmBlacklist":
                baseline = new TpmBlackListBaseline(name);
                redirect = "edittpmbaseline.jsp";
                break;
            default:
                String error = "Unrecognized Baseline Type: " + type;
                LOGGER.error(error);
                return "redirect:/jsp/baselines.jsp?error=" + error;
        }

        // Make sure the baseline doesn't already exist
        if (baselineManager.getBaseline(name) != null) {
            final String error = "Name chosen is already being used.";
            return "redirect:/jsp/baselines.jsp?error=" + error;
        }

        try {
            baselineManager.saveBaseline(baseline);
        } catch (Exception e) {
            String error = "Error occurred trying to save Baseline";
            LOGGER.error(e);
            return "redirect:/jsp/baselines.jsp?error=" + error;
        }

        String result = "Baseline '" + name + "' was successfully created";
        //The user is redirected to the baseline's page.
        return "redirect:/jsp/" + redirect + "?name=" + name + "&result=" + result;
    }

    /**
     * Maps /baseline/update/name. Update the baseline's name.
     *
     * @param name the baseline name
     * @param newName the new baseline name
     * @param redirect the path to the jsp page to redirect to (different
     * baseline types have different pages)
     * @return redirect to the baseline editor
     */
    @RequestMapping(value = "update/name", method = RequestMethod.POST)
    public String updateName(
            @RequestParam(required = true) final String name,
            @RequestParam(required = true) final String newName,
            @RequestParam(defaultValue = "jsp/baselines.jsp") final String redirect) {

        Baseline baseline = baselineManager.getBaseline(name);
        if (baseline == null) {
            return "redirect:/" + redirect + "?name=" + name
                    + "&error=Baseline not found!";
        }

        // Make sure the baseline doesn't already exist
        if (baselineManager.getBaseline(newName) != null) {
            final String error = "Name chosen is already being used.";
            return "redirect:/" + redirect + "?name=" + name + "&error=" + error;
        }

        baseline.setName(newName);
        baselineManager.updateBaseline(baseline);

        return "redirect:/" + redirect + "?name=" + newName
                + "&result=Baseline successfully updated";
    }

    /**
     * Maps /baseline/update/description. Updates the baseline's description.
     *
     * @param name the baseline name
     * @param description the baseline description
     * @param redirect the path to the jsp page to redirect to (different
     * baseline types have different pages)
     * @return redirect to the baseline editor
     */
    @RequestMapping(value = "update/description", method = RequestMethod.POST)
    public String updateDescription(
            @RequestParam(required = true) final String name,
            @RequestParam(required = true) final String description,
            @RequestParam(defaultValue = "jsp/baselines.jsp") final String redirect) {

        Baseline baseline = baselineManager.getBaseline(name);
        if (baseline == null) {
            return "redirect:/" + redirect + "?name=" + name
                    + "&error=Baseline not found!";
        }
        baseline.setDescription(description);
        baselineManager.updateBaseline(baseline);

        return "redirect:/" + redirect + "?name=" + name
                + "&result=Baseline successfully updated";
    }

    /**
     * Maps /baseline/update/severity. Updates the baseline's severity.
     *
     * @param name the baseline name
     * @param severity the baseline severity
     * @param redirect the path to the jsp page to redirect to (different
     * baseline types have different pages)
     * @return redirect to the baseline editor
     */
    @RequestMapping(value = "update/severity", method = RequestMethod.POST)
    public String updateSeverity(
            @RequestParam(required = true) final String name,
            @RequestParam(required = true) final Alert.Severity severity,
            @RequestParam(defaultValue = "jsp/baselines.jsp") final String redirect) {

        Baseline baseline = baselineManager.getBaseline(name);
        if (baseline == null) {
            return "redirect:/" + redirect + "?name=" + name
                    + "&error=Baseline not found!";
        }
        baseline.setSeverity(severity);
        baselineManager.updateBaseline(baseline);

        return "redirect:/" + redirect + "?name=" + name
                + "&result=Baseline successfully updated";
    }

    /**
     * Maps /baselines/list to a set of Baselines.
     *
     * @param draw id
     * @param start index of search
     * @param length of results to return (max)
     * @param column to sort by
     * @param orderDirection ascending or descending ordering
     * @param search parameters
     * @return list filtered and sorted based on request parameters
     */
    @ResponseBody
    @RequestMapping(value = "list", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<FilteredRecordsList<Baseline>> list(
            @RequestParam(required = false, defaultValue = "0") final int draw,
            @RequestParam(required = false, defaultValue = "0") final int start,
            @RequestParam(required = false, defaultValue = "0") final int length,
            @RequestParam(value = COLUMN, required = false, defaultValue = "0") final int column,
            @RequestParam(value = DIR, required = false, defaultValue = DESC)
            final String orderDirection,
            @RequestParam(value = SRCH, required = false, defaultValue = "") final String search) {

        String orderColumn = "name";

        // determine the sort ordering
        boolean isAscendingSort = !DESC.equals(orderDirection);

        //Chooses which column is to be ordered
        if (column == TIMESTAMP) {
            orderColumn = "createTime";
        } else if (column == NAME) {
            orderColumn = "name";
        }

        //Maps object types and their ability to be searched by Hibernate
        //without modification
        Map<String, Boolean> searchableColumns = new HashMap<>();
        searchableColumns.put("name", true);
        searchableColumns.put("create_time", false);
        searchableColumns.put("severity", false);

        FilteredRecordsList<Baseline> baselines = baselineManager
                .getOrderedBaselineList(orderColumn, isAscendingSort,
                        start, length, search,
                        searchableColumns);

        return new DataTableResponse<>(baselines, draw, baselines.getRecordsTotal(),
                baselines.getRecordsFiltered());
    }

    /**
     * Maps /baselines/listWithoutRecords to a set of Baselines.
     *
     * @param draw id
     * @param start index of search
     * @param length of results to return (max)
     * @param column to sort by
     * @param orderDirection ascending or descending ordering
     * @param search parameters
     * @return list filtered and sorted based on request parameters
     */
    @ResponseBody
    @RequestMapping(value = "listWithoutRecords", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<FilteredRecordsList<SimpleBaselineBean>> listWithoutRecords(
            @RequestParam(required = false, defaultValue = "0") final int draw,
            @RequestParam(required = false, defaultValue = "0") final int start,
            @RequestParam(required = false, defaultValue = "0") final int length,
            @RequestParam(value = COLUMN, required = false, defaultValue = "0") final int column,
            @RequestParam(value = DIR, required = false, defaultValue = DESC)
            final String orderDirection,
            @RequestParam(value = SRCH, required = false, defaultValue = "") final String search) {

        String orderColumn = "name";

        // determine the sort ordering
        boolean isAscendingSort = !DESC.equals(orderDirection);

        //Chooses which column is to be ordered
        if (column == TIMESTAMP) {
            orderColumn = "createTime";
        }

        //Maps object types and their ability to be searched by Hibernate
        //without modification
        Map<String, Boolean> searchableColumns = new HashMap<>();
        searchableColumns.put("name", true);
        searchableColumns.put("create_time", false);
        searchableColumns.put("severity", false);
        searchableColumns.put("type", true);

        FilteredRecordsList<SimpleBaselineBean> baselines = baselineManager
                .getOrderedBaselineListWithoutRecords(orderColumn, isAscendingSort,
                        start, length, search,
                        searchableColumns);

        return new DataTableResponse<>(baselines, draw, baselines.getRecordsTotal(),
                baselines.getRecordsFiltered());
    }

    /**
     * Maps /baselines/list/ima to a set of IMA Baseline Records.
     *
     * @param id id of the selected baseline
     * @param draw request number provided by DataTables
     * @param start index of search
     * @param length of results to return (max)
     * @param column to sort by
     * @param orderDirection ascending or descending ordering
     * @param search parameters
     * @return list filtered and sorted based on request parameters
     */
    @ResponseBody
    @RequestMapping(value = "list/ima", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<FilteredRecordsList<IMABaselineRecord>> listImaBaseline(
            @RequestParam(required = true) final String id,
            @RequestParam(required = false, defaultValue = "0") final int draw,
            @RequestParam(required = false, defaultValue = "0") final int start,
            @RequestParam(required = false, defaultValue = "0") final int length,
            @RequestParam(value = COLUMN, required = false, defaultValue = "0") final int column,
            @RequestParam(value = DIR, required = false, defaultValue = DESC)
            final String orderDirection,
            @RequestParam(value = SRCH, required = false, defaultValue = "") final String search) {

        String orderColumn = "id";

        // determine the sort ordering
        boolean isAscendingSort = !DESC.equals(orderDirection);

        //Chooses which column is to be ordered
        if (column == PATH) {
            orderColumn = "path";
        } else if (column == HASH) {
            orderColumn = "hash";
        }

        FilteredRecordsList<IMABaselineRecord> baselines = baselineManager
                .getOrderedRecordList(UUID.fromString(id), orderColumn, isAscendingSort,
                        start, length, search);

        return new DataTableResponse<>(baselines, draw, baselines.getRecordsTotal(),
                baselines.getRecordsFiltered());
    }

    /**
     * Maps /baselines/list/imablack to a set of IMA Blacklist Baseline Records.
     *
     * @param id id of the selected baseline
     * @param draw request number provided by DataTables
     * @param start index of search
     * @param length of results to return (max)
     * @param column to sort by
     * @param orderDirection ascending or descending ordering
     * @param search parameters
     * @return list filtered and sorted based on request parameters
     */
    @ResponseBody
    @RequestMapping(value = "list/imablack", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<FilteredRecordsList<ImaBlacklistRecord>> listImaBlacklistBaseline(
            @RequestParam(required = true) final String id,
            @RequestParam(required = false, defaultValue = "0") final int draw,
            @RequestParam(required = false, defaultValue = "0") final int start,
            @RequestParam(required = false, defaultValue = "0") final int length,
            @RequestParam(value = COLUMN, required = false, defaultValue = "0") final int column,
            @RequestParam(value = DIR, required = false, defaultValue = DESC)
            final String orderDirection,
            @RequestParam(value = SRCH, required = false, defaultValue = "") final String search) {

        String orderColumn = "id";

        // determine the sort ordering
        boolean isAscendingSort = !DESC.equals(orderDirection);

        //Chooses which column is to be ordered
        if (column == PATH) {
            orderColumn = "path";
        } else if (column == HASH) {
            orderColumn = "hash";
        }

        FilteredRecordsList<ImaBlacklistRecord> baselines = baselineManager
                .getOrderedBlacklistRecordList(UUID.fromString(id), orderColumn, isAscendingSort,
                        start, length, search);

        return new DataTableResponse<>(baselines, draw, baselines.getRecordsTotal(),
                baselines.getRecordsFiltered());
    }

    /**
     * Updates a record in an {@link IMABaselineRecord}. Adds an
     * {@link IMABaselineRecord} to an {@link ImaBaseline}.
     *
     * @param name Name of Baseline to update the records for
     * @param path Path of the record being added
     * @param hash Hash of the record being added
     * @param description Description of the record being added (blacklist only)
     * @return redirect to editimabaseline.jsp redirect to
     * editimablacklistbaseline.jsp
     */
    @RequestMapping(value = "/record/ima/add", method = RequestMethod.POST)
    public String addImaRecords(@RequestParam(required = true) final String name,
            @RequestParam(required = true) final String path,
            @RequestParam(required = true) final String hash,
            @RequestParam(required = false) final String description) {
        String redirect = "redirect:";
        String result = "", error = "";

        LOGGER.info("Attempting to update IMA Baseline Record for baseline '" + name + "'");

        try {

            if (baselineManager.getBaseline(name) instanceof SimpleImaBaseline) {
                redirect += REDIRECT_IMA_BASELINE_JSP + "?name=" + name;
                if (StringUtils.isBlank(path)) {
                    return redirect + "&error=Path cannot be empty";
                }
                SimpleImaBaseline simpleImaBaseline
                        = (SimpleImaBaseline) baselineManager.getBaseline(name);
                IMABaselineRecord record
                        = new IMABaselineRecord(path, stringToDigest(hash), simpleImaBaseline);
                imaBaselineRecordManager.saveRecord(record);
            } else if (baselineManager.getBaseline(name) instanceof ImaBlacklistBaseline) {
                redirect += REDIRECT_IMA_BLACKLIST_BASELINE_JSP + "?name=" + name;
                if (StringUtils.isBlank(path) && StringUtils.isBlank(hash)) {
                    return redirect + "&error=Path and hash cannot both be empty";
                }
                //Check if there is a path (default to null)
                String filePath = null;
                if (!StringUtils.isBlank(path)) {
                    filePath = path;
                }
                ImaBlacklistBaseline imaBlacklistBaseline
                        = (ImaBlacklistBaseline) baselineManager.getBaseline(name);
                ImaBlacklistRecord record
                        = new ImaBlacklistRecord(filePath, stringToDigest(hash), description,
                                imaBlacklistBaseline);
                imaBlacklistBaselineRecordManager.saveRecord(record);
            } else if (baselineManager.getBaseline(name) == null) {
                return redirect + REDIRECT_BASELINES_JSP + "?name=" + name
                        + "&error=Baseline not found!";
            }

            result = "Record for \"" + path + "\" was successfully added to baseline";
            LOGGER.info(result);

            if (StringUtils.containsWhitespace(path)) {
                error = "Warning: \"" + path + "\" contains whitespace. Was this intentional?";
                LOGGER.info(error);
            }

        } catch (DecoderException de) {
            error = "Could not digest the hash provided";
            LOGGER.error(error, de);

        } catch (BaselineManagerException | ImaBaselineRecordManagerException | NullPointerException ex) {
            error = "Could not update the selected Baseline record";
            LOGGER.error(error, ex);

        } catch (IllegalArgumentException iae) {
            error = "Could not update the record, " + iae.getMessage();
            LOGGER.error(error);

        }

        return redirect + "&result=" + result + "&error=" + error;
    }

    /**
     * Updates a {@link TPMMeasurementRecord} in a {@link TPMBaseline}.
     *
     * @param name Name of the baseline to add the record to
     * @param pcrId PCR ID of the record being added
     * @param hash Hex representation of the new record's hash if static,
     * otherwise null
     *
     * @return redirect to edittpmbaseline.jsp
     */
    @RequestMapping(value = "/record/tpm/add", method = RequestMethod.POST)
    public String addTpmRecords(
            @RequestParam final String name,
            @RequestParam final String pcrId,
            @RequestParam final String hash) {

        String redirect = "redirect:/jsp/edittpmbaseline.jsp?name=" + name;
        String result = "", error = "";

        LOGGER.info("Attempting to add TPM Measurement record for baseline '" + name + "'");
        try {

            TPMBaseline tpmBaseline = (TPMBaseline) baselineManager.getBaseline(name);

            if (tpmBaseline == null) {
                return redirect + "&error=Baseline not found!";
            }

            if (hash == null) {
                return redirect + "&error=New TPM record must have a hash!";
            }
            tpmBaseline.addToBaseline(new TPMMeasurementRecord(
                    Integer.parseInt(pcrId), stringToDigest(hash)));
            baselineManager.updateBaseline(tpmBaseline);

            result = "Record was successfully added to baseline";
            LOGGER.info(result);

        } catch (DecoderException de) {
            error = "Could not digest the hash provided";
            LOGGER.error(error, de);

        } catch (NumberFormatException nfe) {
            error = "PCR value was not a valid integer";
            LOGGER.error(error, nfe);

        } catch (BaselineManagerException | NullPointerException ex) {
            error = "Could not add the selected Baseline record";
            LOGGER.error(error, ex);

        } catch (IllegalArgumentException iae) {
            error = "Could not add the selected Baseline record, " + iae.getMessage();
            LOGGER.error(error, iae);
        }
        return redirect + "&result=" + result + "&error=" + error;
    }

    /**
     * Updates a {@link ImaIgnoreSetRecord} in an {@link ImaIgnoreSetBaseline}.
     *
     * @param name Name of Baseline to update the records for
     * @param path Path of the record being add
     * @param description Description of the record being add
     * @return redirect to editimaignorebaseline.jsp
     */
    @RequestMapping(value = "/record/ignore/add", method = RequestMethod.POST)
    public String addIgnoreRecords(@RequestParam(required = true) final String name,
            @RequestParam(required = true) final String path,
            @RequestParam(required = true, defaultValue = "") final String description) {
        String redirect = "redirect:/jsp/editimaignorebaseline.jsp?name=" + name;
        String result = "", error = "";

        LOGGER.info("Attempting to add Ima Ignore Set Record for baseline '" + name + "'");

        if (StringUtils.isBlank(path)) {
            return redirect + "&error=Path cannot be empty";
        }

        try {
            ImaIgnoreSetBaseline ignoreBaseline
                    = (ImaIgnoreSetBaseline) baselineManager.getBaseline(name);

            if (ignoreBaseline == null) {
                return redirect + "&error=Baseline not found!";
            }

            ImaIgnoreSetRecord record = new ImaIgnoreSetRecord(path, description);

            if (ignoreBaseline.addToBaseline(record)) {
                baselineManager.updateBaseline(ignoreBaseline);

                result = "Record for \"" + path + "\" was successfully added to baseline";
                LOGGER.info(result);

                if (StringUtils.containsWhitespace(path)) {
                    error = "Warning: \"" + path + "\" contains whitespace. "
                            + "Was this intentional?";
                    LOGGER.info(error);
                }

            } else {
                throw new IllegalArgumentException("record already exists");
            }

        } catch (BaselineManagerException | NullPointerException ex) {
            error = "Could not update the selected Baseline record";
            LOGGER.error(error, ex);

        } catch (IllegalArgumentException iae) {
            error = "Could not update the selected Baseline record, " + iae.getMessage();
            LOGGER.error(error, iae);

        }

        return redirect + "&result=" + result + "&error=" + error;
    }

    /**
     * Updates a record in an {@link ImaBaseline}.
     *
     * @param name Name of Baseline to update the records for
     * @param id id of the Baseline record to be deleted
     * @param oldPath Path of the old record being updated
     * @param oldHash Hash of the old Record being updated
     * @param newPath new path for the IMA baseline record
     * @param newHash new hash for the IMA baseline record
     * @param newDescription new description for IMA blacklist record
     * @return redirect to editimabaseline.jsp redirect to
     * editimablacklistbaseline.jsp
     */
    @RequestMapping(value = "/record/ima/update", method = RequestMethod.POST)
    public String updateImaRecords(@RequestParam(required = true) final String name,
            @RequestParam(required = false) final String id,
            @RequestParam(required = true) final String oldPath,
            @RequestParam(required = true) final String oldHash,
            @RequestParam(required = true) final String newPath,
            @RequestParam(required = true) final String newHash,
            @RequestParam(required = false) final String newDescription) {

        String redirect = "redirect:";
        String result = "", error = "";

        LOGGER.info("Attempting to update IMA Baseline Record for baseline '" + name + "'");
        try {
            if (baselineManager.getBaseline(name) instanceof SimpleImaBaseline) {
                redirect += REDIRECT_IMA_BASELINE_JSP + "?name=" + name;
                SimpleImaBaseline simpleImaBaseline
                        = (SimpleImaBaseline) baselineManager.getBaseline(name);

                //Get old record and create new one
                IMABaselineRecord record = imaBaselineRecordManager.getRecord(oldPath,
                        stringToDigest(oldHash), simpleImaBaseline);
                IMABaselineRecord newRecord = new IMABaselineRecord(newPath,
                        stringToDigest(newHash), simpleImaBaseline);

                //Remove old record and save new one
                imaBaselineRecordManager.deleteRecord(record);
                imaBaselineRecordManager.saveRecord(newRecord);
            } else if (baselineManager.getBaseline(name) instanceof ImaBlacklistBaseline) {
                redirect += REDIRECT_IMA_BLACKLIST_BASELINE_JSP + "?name=" + name;
                if (StringUtils.isBlank(newPath) && StringUtils.isBlank(newHash)) {
                    return redirect + "&error=Path and hash cannot both be empty";
                }
                Long recordId = Long.valueOf(id);
                //Check if there is a path (default to null)
                String filePath = null;
                if (!StringUtils.isBlank(newPath)) {
                    filePath = newPath;
                }
                //Get old record and create new one
                ImaBlacklistRecord record = imaBlacklistBaselineRecordManager.getRecord(recordId);
                ImaBlacklistBaseline imaBlacklistBaseline
                        = (ImaBlacklistBaseline) baselineManager.getBaseline(name);
                ImaBlacklistRecord newRecord = new ImaBlacklistRecord(filePath,
                        stringToDigest(newHash), newDescription, imaBlacklistBaseline);

                //Delete old record and save new one
                imaBlacklistBaselineRecordManager.deleteRecord(record);
                imaBlacklistBaselineRecordManager.saveRecord(newRecord);
            } else if (baselineManager.getBaseline(name) == null) {
                return redirect + REDIRECT_BASELINES_JSP + "?name=" + name
                        + "&error=Baseline not found!";
            }

            result = "Record for \"" + newPath + "\" was successfully updated for baseline";
            LOGGER.info(result);

            if (StringUtils.containsWhitespace(newPath)) {
                error = "Warning: \"" + newPath + "\" contains whitespace. Was this intentional?";
                LOGGER.info(error);
            }

        } catch (DecoderException de) {
            error = "Could not digest the hash provided";
            LOGGER.error(error, de);

        } catch (IllegalArgumentException iae) {
            error = "Could not update the record, " + iae.getMessage();
            LOGGER.error(error);

        } catch (BaselineManagerException | ImaBaselineRecordManagerException | NullPointerException ex) {
            error = "Could not update the selected Baseline record";
            LOGGER.error(error, ex);

        }

        return redirect + "&result=" + result + "&error=" + error;
    }

    /**
     * Updates a record in a {@link TPMBaseline}.
     *
     * @param name Name of the baseline to update the record for
     * @param oldPcrId PCR ID of the record before the update
     * @param oldHash Hash of the record before the update if static, otherwise
     * null
     * @param newPcrId PCR ID of the record after the update
     * @param newHash Hash of the record after the update if static, otherwise
     * null
     * @return redirect to edittpmbaseline.jsp
     */
    @RequestMapping(value = "/record/tpm/update", method = RequestMethod.POST)
    public String updateTpmRecords(
            @RequestParam final String name,
            @RequestParam final String oldPcrId,
            @RequestParam final String oldHash,
            @RequestParam final String newPcrId,
            @RequestParam final String newHash) {

        String redirect = "redirect:/jsp/edittpmbaseline.jsp?name=" + name;
        String result = "", error = "";

        LOGGER.info("Attempting to update TPM Baseline Record for baseline '" + name + "'");
        try {

            TPMBaseline tpmBaseline = (TPMBaseline) baselineManager.getBaseline(name);

            if (tpmBaseline == null) {
                return redirect + "&error=Baseline not found!";
            }

            boolean recordExists = false;

            if (oldHash == null) {
                return redirect + "&error=Old TPM record must have a hash!";
            }

            recordExists = tpmBaseline.removeFromBaseline(new TPMMeasurementRecord(
                    Integer.parseInt(oldPcrId), stringToDigest(oldHash)));

            if (recordExists) {
                if (newHash == null) {
                    return redirect + "&error=New TPM record must have a hash!";
                }
                tpmBaseline.addToBaseline(new TPMMeasurementRecord(
                        Integer.parseInt(newPcrId), stringToDigest(newHash)));

                baselineManager.updateBaseline(tpmBaseline);

                result = "Record was successfully updated for baseline";
                LOGGER.info(result);

            } else {
                error = "Record does not exist";
                LOGGER.error(error);
            }

        } catch (DecoderException de) {
            error = "Could not digest the hash provided";
            LOGGER.error(error, de);

        } catch (NumberFormatException nfe) {
            error = "PCR value was not a valid integer";
            LOGGER.error(error, nfe);

        } catch (IllegalArgumentException iae) {
            error = "Could not update the record, " + iae.getMessage();
            LOGGER.error(error);

        } catch (BaselineManagerException | NullPointerException ex) {
            error = "Could not update the selected Baseline record";
            LOGGER.error(error, ex);

        }
        return redirect + "&result=" + result + "&error=" + error;
    }

    /**
     * Updates a record in an {@link ImaIgnoreSetBaseline}.
     *
     * @param name Name of Baseline to update the records for
     * @param oldPath Path of the old record being updated
     * @param oldDescription Description of the old record being updated
     * @param newPath new path for the IMA Ignore Set baseline record
     * @param newDescription new description for the IMA Ignore Set baseline
     * record
     * @return redirect to editimaignorebaseline.jsp
     */
    @RequestMapping(value = "/record/ignore/update", method = RequestMethod.POST)
    public String updateIgnoreRecords(@RequestParam(required = true) final String name,
            @RequestParam(required = true) final String oldPath,
            @RequestParam(required = true) final String oldDescription,
            @RequestParam(required = true) final String newPath,
            @RequestParam(required = true, defaultValue = "") final String newDescription) {
        String url = "redirect:/jsp/editimaignorebaseline.jsp?name=" + name;
        String result = "", error = "";

        LOGGER.info("Attempting to update Ima Ignore Set Record for baseline '" + name + "'");

        try {
            ImaIgnoreSetBaseline ignoreBaseline
                    = (ImaIgnoreSetBaseline) baselineManager.getBaseline(name);

            if (ignoreBaseline == null) {
                return url + "&error=Baseline not found!";
            }

            ImaIgnoreSetRecord oldRecord = new ImaIgnoreSetRecord(oldPath, oldDescription);
            ImaIgnoreSetRecord newRecord = new ImaIgnoreSetRecord(newPath, newDescription);

            //This is done to match equals() and hashcode()
            oldRecord.setOnlyBaseline(ignoreBaseline);

            //Checks that old record could be removed and new one could be added before updating
            if (ignoreBaseline.removeFromBaseline(oldRecord)
                    && ignoreBaseline.addToBaseline(newRecord)) {

                baselineManager.updateBaseline(ignoreBaseline);

                result = "Record for \"" + newPath + "\" was successfully updated for baseline";
                LOGGER.info(result);

                if (StringUtils.containsWhitespace(newPath)) {
                    error = "Warning: \"" + newPath + "\" contains whitespace. Was this "
                            + "intentional?";
                    LOGGER.info(error);
                }

            } else {
                throw new IllegalArgumentException("record list could not be modified");
            }

        } catch (BaselineManagerException | NullPointerException | IllegalArgumentException ex) {
            error = "Could not update the selected Baseline record";
            LOGGER.error(error, ex);

        }

        return url + "&result=" + result + "&error=" + error;
    }

    /**
     * Deletes an {@link IMABaselineRecord} from an {@link ImaBaseline}.
     *
     * @param name name of the baseline to have the record removed
     * @param id id of the Baseline record to be deleted
     * @param path path of the IMA Baseline Record to be deleted
     * @param hash hash of the IMA Baseline Record to be deleted
     * @return redirects to editimabaseline.jsp redirects to
     * editimablacklistbaseline.jsp
     */
    @RequestMapping(value = "/record/ima/delete", method = RequestMethod.POST)
    public String deleteImaRecord(@RequestParam(required = true) final String name,
            @RequestParam(required = false) final String id,
            @RequestParam(required = true) final String path,
            @RequestParam(required = true) final String hash) {

        String redirect = "redirect:";
        String result = "", error = "";

        LOGGER.info("Deleting the IMA record from the Baseline " + name);

        try {
            if (baselineManager.getBaseline(name) instanceof SimpleImaBaseline) {
                redirect += REDIRECT_IMA_BASELINE_JSP + "?name=" + name;
                SimpleImaBaseline imaBaseline
                        = (SimpleImaBaseline) baselineManager.getBaseline(name);

                IMABaselineRecord record = imaBaselineRecordManager
                        .getRecord(path, stringToDigest(hash), imaBaseline);

                imaBaselineRecordManager.deleteRecord(record);
            } else if (baselineManager.getBaseline(name) instanceof ImaBlacklistBaseline) {
                redirect += REDIRECT_IMA_BLACKLIST_BASELINE_JSP + "?name=" + name;
                Long recordId = Long.valueOf(id);
                ImaBlacklistRecord record = imaBlacklistBaselineRecordManager.getRecord(recordId);

                imaBlacklistBaselineRecordManager.deleteRecord(record);
            } else if (baselineManager.getBaseline(name) == null) {
                return redirect + REDIRECT_BASELINES_JSP + "?name=" + name
                        + "&error=Baseline not found!";
            }
            result = "Record was successfully removed from baseline";
            LOGGER.info(result);

        } catch (DecoderException de) {
            error = "Could not digest the hash provided";
            LOGGER.error(error, de);

        } catch (BaselineManagerException | NullPointerException | ImaBaselineRecordManagerException ex) {
            error = "Could not delete the selected Baseline record";
            LOGGER.error(error, ex);

        }
        return redirect + "&result=" + result + "&error=" + error;
    }

    /**
     * Deletes a {@link TPMMeasurementRecord} from a {@link TPMBaseline}.
     *
     * @param name Name of the baseline from which to remove the record
     * @param pcrId PCR ID of the record to be deleted
     * @param hash Hex representation of the hash of the record to be deleted,
     * or the special string: "Device-Specific"
     * @return redirects to edittpmbaseline.jsp
     */
    @RequestMapping(value = "record/tpm/delete", method = RequestMethod.POST)
    public String deleteTpmRecord(
            @RequestParam(required = true) final String name,
            @RequestParam(required = true) final String pcrId,
            @RequestParam(required = true) final String hash) {

        String redirect = "redirect:/jsp/edittpmbaseline.jsp?name=" + name;
        String result = "", error = "";

        LOGGER.info("Deleting the TPM record from the Baseline " + name);

        try {
            TPMBaseline tpmBaseline = (TPMBaseline) baselineManager.getBaseline(name);

            if (tpmBaseline == null) {
                LOGGER.error("Baseline '" + name + "' not found!");
                return redirect + "&error=Baseline not found!";
            }

            boolean deletedRecord = false;

            TPMMeasurementRecord record = new TPMMeasurementRecord(Integer.parseInt(pcrId),
                    stringToDigest(hash));
            deletedRecord = tpmBaseline.removeFromBaseline(record);

            if (deletedRecord) {
                baselineManager.updateBaseline(tpmBaseline);
                result = "Record was successfully removed from baseline";
                LOGGER.info(result);

            } else {
                error = "Record could not be removed from baseline";
                LOGGER.error(error);
            }

        } catch (DecoderException de) {
            error = "Could not digest the hash provided";
            LOGGER.error(error, de);

        } catch (NumberFormatException nfe) {
            error = "PCR value was not a valid integer";
            LOGGER.error(error, nfe);

        } catch (BaselineManagerException | NullPointerException ex) {
            error = "Could not delete the selected Baseline record";
            LOGGER.error(error, ex);
        }

        return redirect + "&result=" + result + "&error=" + error;
    }

    /**
     * Deletes an {@link ImaIgnoreSetRecord} from an
     * {@link ImaIgnoreSetBaseline}.
     *
     * @param name name of the baseline to have the record removed
     * @param path path of the IMA Ignore Set Baseline Record to be deleted
     * @return redirects to editimaignorebaseline.jsp
     */
    @RequestMapping(value = "record/ignore/delete", method = RequestMethod.POST)
    public String deleteIgnoreRecord(@RequestParam(required = true) final String name,
            @RequestParam(required = true) final String path) {

        String redirect = "redirect:/jsp/editimaignorebaseline.jsp?name=" + name;
        String result = "", error = "";

        LOGGER.info("Deleting the ignore set record from the Baseline " + name);

        try {
            ImaIgnoreSetBaseline ignoreBaseline
                    = (ImaIgnoreSetBaseline) baselineManager.getBaseline(name);

            if (ignoreBaseline == null) {
                LOGGER.error("Baseline '" + name + "' not found!");
                return redirect + "&error=Baseline not found!";
            }

            ImaIgnoreSetRecord record = new ImaIgnoreSetRecord(path);

            //This is done to match up with equals() and hashcode()
            record.setOnlyBaseline(ignoreBaseline);
            if (ignoreBaseline.removeFromBaseline(record)) {
                baselineManager.updateBaseline(ignoreBaseline);
                result = "Record was successfully removed from baseline";
                LOGGER.info(result);

            } else {
                error = "Record does not exist in baseline";
                LOGGER.error(error);

            }

        } catch (BaselineManagerException bme) {
            error = "Could not delete the selected Baseline record";
            LOGGER.error(error, bme);

        }

        return redirect + "&result=" + result + "&error=" + error;
    }

    /**
     * Converts provided String into a Digest.
     */
    private Digest stringToDigest(final String hash) throws DecoderException {
        //Return null if it's empty
        if (StringUtils.isBlank(hash)) {
            return null;
        }

        byte[] digestBytes = Hex.decodeHex(hash.toCharArray());
        return new Digest(DigestAlgorithm.SHA1, digestBytes);
    }

    /**
     * Maps /baselines/export. Exports the baseline records as CSV.
     *
     * @param name the baseline name
     * @param response the response object (needed to update the header with the
     * baseline name in the file name
     * @return redirect to the baseline editor
     */
    @ResponseBody
    @RequestMapping(value = "export", method = RequestMethod.GET,
            produces = "application/octet-stream")
    public String exportBaseline(@RequestParam(required = true) final String name,
            final HttpServletResponse response) {
        LOGGER.info("Attempting to export baseline " + name);

        Baseline baseline = baselineManager.getCompleteBaseline(name);

        //Sets filename for download.  All spaces in name are replaced by
        //underscores to make it more appropriate for Linux based systems
        response.setHeader("Content-Disposition", "attachment; filename=\""
                + baseline.getName().replaceAll(" ", "_") + ".csv\"");

        if (baseline instanceof ImaAcceptableRecordBaseline) {
            LOGGER.info("Baseline is an ImaAcceptableRecordBaseline");
            ImaAcceptableRecordBaseline ima = (ImaAcceptableRecordBaseline) baseline;
            return CSVGenerator.imaRecordsToCsv(ima);
        } else if (baseline instanceof ImaIgnoreSetBaseline) {
            LOGGER.info("Baseline is an ImaIgnoreSetBaseline");
            ImaIgnoreSetBaseline ignore = (ImaIgnoreSetBaseline) baseline;
            return CSVGenerator.ignoreSetToCsv(ignore);
        } else if (baseline instanceof ImaBlacklistBaseline) {
            LOGGER.info("Baseline is an ImaBlacklistBaseline");
            ImaBlacklistBaseline blacklist = (ImaBlacklistBaseline) baseline;
            return CSVGenerator.blacklistToCsv(blacklist);
        } else if (baseline instanceof TPMBaseline) {
            LOGGER.info("Baseline is an TPMBaseline");
            TPMBaseline tpm = (TPMBaseline) baseline;
            return CSVGenerator.tpmRecordsToCsv(tpm);
        }

        return "";
    }

    /**
     * Creates a {@link Baseline} from the specified {@link IntegrityReport}.
     *
     * @param uuid of the IntegrityReport
     * @param name of the Baseline
     * @param type of the Baseline
     * @return redirects the user to the edit page of the Baseline
     */
    @RequestMapping(value = "create/from/integrityReport", method = RequestMethod.POST)
    public String createFromIntegrityReport(@RequestParam final String uuid,
            @RequestParam final String name, @RequestParam final String type) {
        LOGGER.info("Creating " + type + " Baseline [" + name + "] from IntegrityReport ["
                + uuid + "]");
        final String errorRedirect
                = "redirect:/jsp/rawtpmreport.jsp?reportID=" + uuid + "&error=";
        try {

            // check if Baseline with specified name already exists
            Baseline existingBaseline = baselineManager.getBaseline(name);
            if (existingBaseline != null) {
                return errorRedirect + "Baseline named '" + name + "' already exists.";
            }

            // get IntegrityReport
            IntegrityReport report = (IntegrityReport) reportManager.getReport(UUID.fromString(uuid));
            if (report == null) {
                return errorRedirect + "Report was not found.";
            }

            /*
             * NOTE: this could be better genericized for IMA Baselines in the future
             * if both TPMBaselineGenerator and SimpleImaBaselineGenerator implemented
             * a common interface.
             */
            // check type
            if ("TPM".equalsIgnoreCase(type)) {

                // check for TPMReport
                if (!report.contains(TPMReport.class)) {
                    return errorRedirect + "Report does not contain a TPM Report.";
                }

                // generate baseline
                final TPMBaselineGenerator generator = new TPMBaselineGenerator();
                TPMBaseline baseline = generator.generateBaselineFromIntegrityReport(name, report);

                // save baseline
                baselineManager.saveBaseline(baseline);
                return "redirect:/jsp/edittpmbaseline.jsp?name=" + name
                        + "&result=TPM Baseline created successfully.";

            } else {
                return errorRedirect + "Unsupported report type: '" + type + "'.";
            }

        } catch (Exception ex) {
            final String msg
                    = "Error creating Baseline from IntegrityReport: " + ex.getMessage();
            LOGGER.error(msg, ex);
            return errorRedirect + msg;
        }
    }

    private static String redirectToTPMBaseline(
            final String name,
            final String result,
            final String error) throws URISyntaxException {
        final URIBuilder uri = new URIBuilder("redirect:/jsp/");

        // append page and parameters
//        if (name != null) {
//            uri.path("edittpmbaseline.jsp");
//            uri.queryParam("name", name);
//        } else {
//            uri.path("baselines.jsp");
//        }
//
//        // append messages
//        if (result != null) {
//            uri.queryParam("result", result);
//        }
//        if (error != null) {
//            uri.queryParam("error", error);
//        }
//
//        // expand device info
//        uri.queryParam("deviceInfoExpanded", true);
        return uri.toString();

    }

    private static String redirectToTPMBaselineError(final String name,
            final Exception ex) throws URISyntaxException {
        return redirectToTPMBaseline(name, null, ex.getMessage());
    }

    private static String redirectToTPMBaselineSuccess(final String name)
            throws URISyntaxException {
        return redirectToTPMBaseline(name, "TPMBaseline '" + name + "' was successfully updated.",
                null);
    }

    @SuppressWarnings("checkstyle:avoidinlineconditionals")
    private static String stringOrNotSpecified(final String s) {
        return StringUtils.isBlank(s) ? DeviceInfoReport.NOT_SPECIFIED : s;
    }

    private TPMBaseline getTPMBaseline(final String name) {
        Baseline baseline = baselineManager.getBaseline(name);
        if (baseline == null) {
            throw new IllegalArgumentException("Baseline '" + name + "' was not found.");
        }
        if (!(baseline instanceof TPMBaseline)) {
            throw new IllegalArgumentException("Baseline '" + name + "' is not a"
                    + " TPM baseline.");
        }
        return (TPMBaseline) baseline;
    }

    /**
     * Updates the HardwareInfo for a TPMBaseline.
     *
     * @param name of the TPMBaseline
     * @param manufacturer of the hardware
     * @param productName of the hardware
     * @param version of the hardware
     * @return url redirect to error or success page
     */
    @RequestMapping(value = "update/device/hardware", method = RequestMethod.POST)
    public String updateDeviceHardwareInfo(
            @RequestParam final String name,
            @RequestParam final String manufacturer,
            @RequestParam final String productName,
            @RequestParam final String version) {

        TPMBaseline baseline;

        try {
            try {
                baseline = getTPMBaseline(name);
            } catch (IllegalArgumentException ex) {
                return redirectToTPMBaselineError(null, ex);
            }

            HardwareInfo info = new HardwareInfo(stringOrNotSpecified(manufacturer),
                    stringOrNotSpecified(productName), stringOrNotSpecified(version),
                    DeviceInfoReport.NOT_SPECIFIED, DeviceInfoReport.NOT_SPECIFIED,
                    DeviceInfoReport.NOT_SPECIFIED);
            baseline.setHardwareInfo(info);
            baselineManager.updateBaseline(baseline);

            return redirectToTPMBaselineSuccess(name);
        } catch (URISyntaxException uriEx) {

        }

        return "";
    }

    private static final DateFormat BIOS_RELEASE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Updates the FirmwareInfo for a TPMBaseline.
     *
     * @param name of the TPMBaseline
     * @param biosVendor of the BIOS
     * @param biosVersion of the BIOS
     * @param biosReleaseDate of the BIOS
     * @return url redirect to error or success page
     */
    @RequestMapping(value = "update/device/firmware", method = RequestMethod.POST)
    public String updateDeviceFirmwareInfo(
            @RequestParam final String name,
            @RequestParam final String biosVendor,
            @RequestParam final String biosVersion,
            @RequestParam final String biosReleaseDate) {

        TPMBaseline baseline;

        try {
            try {
                baseline = getTPMBaseline(name);
            } catch (IllegalArgumentException ex) {
                return redirectToTPMBaselineError(null, ex);
            }

            if (!StringUtils.isBlank(biosReleaseDate)) {
                try {
                    BIOS_RELEASE_DATE_FORMAT.parse(biosReleaseDate);
                } catch (ParseException ex) {
                    return redirectToTPMBaseline(name, null, "Error parsing BIOS release date '"
                            + biosReleaseDate + "'.");
                }
            }

            FirmwareInfo info = new FirmwareInfo(stringOrNotSpecified(biosVendor),
                    stringOrNotSpecified(biosVersion), stringOrNotSpecified(biosReleaseDate));
            baseline.setFirmwareInfo(info);
            baselineManager.updateBaseline(baseline);

            return redirectToTPMBaselineSuccess(name);
        } catch (URISyntaxException uriEx) {

        }

        return "";
    }

    /**
     * Updates the TPMInfo for a TPMBaseline.
     *
     * @param name of thetpmBaseline
     * @param tpmMake of the TPM
     * @param tpmVersionMajor of the TPM
     * @param tpmVersionMinor of the TPM
     * @param tpmVersionRevMajor of the TPM
     * @param tpmVersionRevMinor of the TPM
     * @return url redirect to error or success page
     */
    @RequestMapping(value = "update/device/tpm", method = RequestMethod.POST)
    public String updateDeviceTPMInfo(
            @RequestParam final String name,
            @RequestParam final String tpmMake,
            @RequestParam final short tpmVersionMajor,
            @RequestParam final short tpmVersionMinor,
            @RequestParam final short tpmVersionRevMajor,
            @RequestParam final short tpmVersionRevMinor) {

        TPMBaseline baseline;

        try {
            try {
                baseline = getTPMBaseline(name);
            } catch (IllegalArgumentException ex) {
                return redirectToTPMBaselineError(null, ex);
            }

            TPMInfo info;
            try {
                info = new TPMInfo(stringOrNotSpecified(tpmMake), tpmVersionMajor,
                        tpmVersionMinor, tpmVersionRevMajor, tpmVersionRevMinor);
            } catch (IllegalArgumentException ex) {
                return redirectToTPMBaselineError(name, ex);
            }
            baseline.setTPMInfo(info);
            baselineManager.updateBaseline(baseline);

            return redirectToTPMBaselineSuccess(name);
        } catch (URISyntaxException uriEx) {

        }

        return "";
    }

    /**
     * Updates the OSInfo for a TPMBaseline.
     *
     * @param name of the TPMBaseline
     * @param osName of the OS
     * @param osVersion of the OS
     * @param osArch of the OS
     * @param distribution of the OS
     * @param distributionRelease of the OS
     * @return url redirect to error or success page
     */
    @RequestMapping(value = "update/device/os", method = RequestMethod.POST)
    public String updateDeviceOSInfo(
            @RequestParam final String name,
            @RequestParam final String osName,
            @RequestParam final String osVersion,
            @RequestParam final String osArch,
            @RequestParam final String distribution,
            @RequestParam final String distributionRelease) {

        TPMBaseline baseline;

        try {
            try {
                baseline = getTPMBaseline(name);
            } catch (IllegalArgumentException ex) {
                return redirectToTPMBaselineError(null, ex);
            }

            OSInfo info;
            info = new OSInfo(stringOrNotSpecified(osName), stringOrNotSpecified(osVersion),
                    stringOrNotSpecified(osArch), stringOrNotSpecified(distribution),
                    stringOrNotSpecified(distributionRelease));
            baseline.setOSInfo(info);
            baselineManager.updateBaseline(baseline);

            return redirectToTPMBaselineSuccess(name);
        } catch (URISyntaxException uriEx) {

        }

        return "";
    }
}
