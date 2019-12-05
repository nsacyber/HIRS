package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.util.CertificateStringMapBuilder;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import static org.apache.logging.log4j.LogManager.getLogger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import hirs.FilteredRecordsList;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.IssuedAttestationCertificate;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.persist.CertificateManager;
import hirs.persist.CriteriaModifier;
import hirs.persist.CrudManager;
import hirs.persist.DBManagerException;
import hirs.persist.OrderedListQuerier;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the Device page.
 */
@Controller
@RequestMapping("/certificate-request")
public class CertificateRequestPageController extends PageController<NoPageParams> {

    private final CertificateManager certificateManager;
    private final OrderedListQuerier<Certificate> dataTableQuerier;

    private CertificateAuthorityCredential certificateAuthorityCredential;

    private static final Logger LOGGER = getLogger(CertificateRequestPageController.class);

    private static final String TRUSTCHAIN = "trust-chain";
    private static final String PLATFORMCREDENTIAL = "platform-credentials";
    private static final String ENDORSEMENTCREDENTIAL = "endorsement-key-credentials";
    private static final String ISSUEDCERTIFICATES = "issued-certificates";

    /**
     * Model attribute name used by initPage for the aca cert info.
     */
    static final String ACA_CERT_DATA = "acaCertData";

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param certificateManager the certificate manager
     * @param crudManager the CRUD manager for certificates
     * @param acaCertificate the ACA's X509 certificate
     */
    @Autowired
    public CertificateRequestPageController(
            final CertificateManager certificateManager,
            final CrudManager<Certificate> crudManager,
            final X509Certificate acaCertificate) {
        super(Page.TRUST_CHAIN);
        this.certificateManager = certificateManager;
        this.dataTableQuerier = crudManager;

        try {
            certificateAuthorityCredential
                    = new CertificateAuthorityCredential(acaCertificate.getEncoded());
        } catch (IOException e) {
            LOGGER.error("Failed to read ACA certificate", e);
        } catch (CertificateEncodingException e) {
            LOGGER.error("Error getting encoded ACA certificate", e);
        }
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from
     * redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView();
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param certificateType String containing the certificate type
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from
     * redirect.
     * @return the path for the view and data model for the page.
     */
    @RequestMapping("/{certificateType}")
    public ModelAndView initPage(@PathVariable("certificateType") final String certificateType,
            final NoPageParams params, final Model model) {

        ModelAndView mav = null;
        HashMap<String, String> data = new HashMap<>();
        // add page information
        switch (certificateType) {
            case PLATFORMCREDENTIAL:
                mav = getBaseModelAndView(Page.PLATFORM_CREDENTIALS);
                break;
            case ENDORSEMENTCREDENTIAL:
                mav = getBaseModelAndView(Page.ENDORSEMENT_KEY_CREDENTIALS);
                break;
            case ISSUEDCERTIFICATES:
                mav = getBaseModelAndView(Page.ISSUED_CERTIFICATES);
                break;
            case TRUSTCHAIN:
                 mav = getBaseModelAndView(Page.TRUST_CHAIN);
                // Map with the ACA certificate information
                data.putAll(CertificateStringMapBuilder.getCertificateAuthorityInformation(
                        certificateAuthorityCredential, this.certificateManager));
                mav.addObject(ACA_CERT_DATA, data);
                break;
            default:
                // send to an error page
                break;
        }

        return mav;
    }

    /**
     * Queries for the list of Certificates and returns a data table response
     * with the records.
     *
     * @param certificateType String containing the certificate type
     * @param input the DataTables search/query parameters
     * @return the data table
     */
    @ResponseBody
    @RequestMapping(value = "/{certificateType}/list",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    @SuppressWarnings("unchecked")
    public DataTableResponse<? extends Certificate> getTableData(
            @PathVariable("certificateType") final String certificateType,
            final DataTableInput input) {

        LOGGER.debug("Handling list request: " + input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();

        LOGGER.debug("Ordering on column: " + orderColumnName);

        // check that the alert is not archived and that it is in the specified report
        CriteriaModifier criteriaModifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                criteria.add(Restrictions.isNull("archivedTime"));

                // add a device alias if this query includes the device table
                // for getting the device (e.g. device name).
                // use left join, since device may be null. Query will return all
                // Certs of this type, whether it has a Device or not (device field may be null)
                if (hasDeviceTableToJoin(certificateType)) {
                    criteria.createAlias("device", "device", JoinType.LEFT_OUTER_JOIN);
                }

            }
        };

        FilteredRecordsList records
                = OrderedListQueryDataTableAdapter.getOrderedList(
                        getCertificateClass(certificateType), dataTableQuerier,
                        input, orderColumnName, criteriaModifier);

        // special parsing for platform credential
        // Add the EndorsementCredential for each PlatformCredential based on the
        // serial number. (pc.HolderSerialNumber = ec.SerialNumber)
        if (certificateType.equals(PLATFORMCREDENTIAL)) {
            EndorsementCredential associatedEC;

            if (!records.isEmpty()) {
                // loop all the platform certificates
                for (int i = 0; i < records.size(); i++) {
                    PlatformCredential pc = (PlatformCredential) records.get(i);
                    // find the EC using the PC's "holder serial number"
                    associatedEC = EndorsementCredential
                            .select(certificateManager)
                            .bySerialNumber(pc.getHolderSerialNumber())
                            .getCertificate();

                    if (associatedEC != null) {
                        LOGGER.debug("EC ID for holder s/n " + pc
                                .getHolderSerialNumber() + " = " + associatedEC.getId());
                    }

                    pc.setEndorsementCredential(associatedEC);
                }
            }
        }

        LOGGER.debug("Returning list of size: " + records.size());
        return new DataTableResponse<>(records, input);
    }

    /**
     * Archives (soft delete) the credential.
     *
     * @param certificateType String containing the certificate type
     * @param id the UUID of the cert to delete
     * @param attr RedirectAttributes used to forward data back to the original
     * page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "/{certificateType}/delete", method = RequestMethod.POST)
    public RedirectView delete(
            @PathVariable("certificateType") final String certificateType,
            @RequestParam final String id,
            final RedirectAttributes attr) throws URISyntaxException {
        LOGGER.info("Handling request to delete " + id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            UUID uuid = UUID.fromString(id);
            Certificate certificate = getCertificateById(certificateType, uuid, certificateManager);
            if (certificate == null) {
                // Use the term "record" here to avoid user confusion b/t cert and cred
                String notFoundMessage = "Unable to locate record with ID: " + uuid;
                messages.addError(notFoundMessage);
                LOGGER.warn(notFoundMessage);
            } else {
                if (certificateType.equals(PLATFORMCREDENTIAL)) {
                    PlatformCredential platformCertificate = (PlatformCredential) certificate;
                    if (platformCertificate.isBase()) {
                        // only do this if the base is being deleted.
                        List<PlatformCredential> sharedCertificates = getCertificateByBoardSN(
                                certificateType,
                                platformCertificate.getPlatformSerial(),
                                certificateManager);

                        if (sharedCertificates != null) {
                            for (PlatformCredential pc : sharedCertificates) {
                                if (!pc.isBase()) {
                                    pc.archive();
                                    certificateManager.update(pc);
                                }
                            }
                        }
                    }
                }

                certificate.archive();
                certificateManager.update(certificate);

                String deleteCompletedMessage = "Certificate successfully deleted";
                messages.addInfo(deleteCompletedMessage);
                LOGGER.info(deleteCompletedMessage);
            }
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: " + id;
            messages.addError(uuidError);
            LOGGER.error(uuidError, ex);
        } catch (DBManagerException ex) {
            String dbError = "Failed to archive cert: " + id;
            messages.addError(dbError);
            LOGGER.error(dbError, ex);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(getCertificatePage(certificateType), new NoPageParams(), model, attr);
    }

    /**
     * Handles request to download the cert by writing it to the response stream
     * for download.
     *
     * @param certificateType String containing the certificate type
     * @param id the UUID of the cert to download
     * @param response the response object (needed to update the header with the
     * file name)
     * @throws java.io.IOException when writing to response output stream
     */
    @RequestMapping(value = "/{certificateType}/download", method = RequestMethod.GET)
    public void download(
            @PathVariable("certificateType") final String certificateType,
            @RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        LOGGER.info("Handling request to download " + id);

        try {
            UUID uuid = UUID.fromString(id);
            Certificate certificate = getCertificateById(certificateType, uuid, certificateManager);
            if (null == certificate) {
                // Use the term "record" here to avoid user confusion b/t cert and cred
                String notFoundMessage = "Unable to locate record with ID: " + uuid;
                LOGGER.warn(notFoundMessage);
                // send a 404 error when invalid certificate
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                StringBuilder fileName = new StringBuilder("filename=\"");
                fileName.append(getCertificateClass(certificateType).getSimpleName());
                fileName.append("_");
                fileName.append(certificate.getSerialNumber());
                fileName.append(".cer\"");

                // Set filename for download.
                response.setHeader("Content-Disposition", "attachment;" + fileName);
                response.setContentType("application/octet-stream");

                // write cert to output stream
                response.getOutputStream().write(certificate.getRawBytes());
            }
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: " + id;
            LOGGER.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    /**
     * Handles request to download the ACA cert by writing it to the response
     * stream for download.
     *
     * @param response the response object (needed to update the header with the
     * file name)
     *
     * @throws java.io.IOException when writing to response output stream
     */
    @ResponseBody
    @RequestMapping(value = "/trust-chain/download-aca-cert", method = RequestMethod.GET)
    public void downloadAcaCertificate(final HttpServletResponse response)
            throws IOException {

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=\"hirs-aca-cert.cer\"");
        response.setContentType("application/octet-stream");

        // write cert to output stream
        response.getOutputStream().write(certificateAuthorityCredential.getRawBytes());
    }

    /**
     * Upload and processes a credential.
     *
     * @param certificateType String containing the certificate type
     * @param files the files to process
     * @param attr the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "/{certificateType}/upload", method = RequestMethod.POST)
    protected RedirectView upload(
            @PathVariable("certificateType") final String certificateType,
            @RequestParam("file") final MultipartFile[] files,
            final RedirectAttributes attr) throws URISyntaxException {

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        for (MultipartFile file : files) {
            //Parse certificate
            Certificate certificate = parseCertificate(certificateType, file, messages);

            //Store only if it was parsed
            if (certificate != null) {
                storeCertificate(
                        certificateType,
                        file.getOriginalFilename(),
                        messages, certificate,
                        certificateManager);
            }
        }

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);

        return redirectTo(getCertificatePage(certificateType), new NoPageParams(), model, attr);
    }

    /**
     * Get the page based on the certificate type.
     *
     * @param certificateType String containing the certificate type
     * @return the page for the certificate type.
     */
    private static Page getCertificatePage(final String certificateType) {
        // get page information (default to TRUST_CHAIN)
        switch (certificateType) {
            case PLATFORMCREDENTIAL:
                return Page.PLATFORM_CREDENTIALS;
            case ENDORSEMENTCREDENTIAL:
                return Page.ENDORSEMENT_KEY_CREDENTIALS;
            case ISSUEDCERTIFICATES:
                return Page.ISSUED_CERTIFICATES;
            case TRUSTCHAIN:
            default:
                 return Page.TRUST_CHAIN;
        }
    }

    /**
     * Gets the concrete certificate class type to query for.
     *
     * @param certificateType String containing the certificate type
     * @return the certificate class type
     */
    private static Class<? extends Certificate> getCertificateClass(final String certificateType) {
        switch (certificateType) {
            case PLATFORMCREDENTIAL:
                return PlatformCredential.class;
            case ENDORSEMENTCREDENTIAL:
                return EndorsementCredential.class;
            case ISSUEDCERTIFICATES:
                return IssuedAttestationCertificate.class;
            case TRUSTCHAIN:
                return CertificateAuthorityCredential.class;
            default:
                throw new IllegalArgumentException(
                        String.format("Unknown certificate type: %s", certificateType));
        }
    }

    /**
     * Get flag indicating if a device-name join/alias is required for
     * displaying the table data. This will be true if displaying a cert that is
     * associated with a device.
     *
     * @param certificateType String containing the certificate type
     * @return true if the list criteria modifier requires aliasing the device
     * table, false otherwise.
     */
    private boolean hasDeviceTableToJoin(final String certificateType) {

        boolean hasDevice = true;
        // Trust_Chain Credential do not contain the device table to join.
        if (certificateType.equals(TRUSTCHAIN)) {
            hasDevice = false;
        }
        return hasDevice;
    }

    /**
     * Gets the certificate by ID.
     *
     * @param certificateType String containing the certificate type
     * @param uuid the ID of the cert
     * @param certificateManager the certificate manager to query
     * @return the certificate or null if none is found
     */
    private Certificate getCertificateById(
            final String certificateType,
            final UUID uuid,
            final CertificateManager certificateManager) {

        switch (certificateType) {
            case PLATFORMCREDENTIAL:
                return PlatformCredential
                        .select(certificateManager)
                        .byEntityId(uuid)
                        .getCertificate();
            case ENDORSEMENTCREDENTIAL:
                return EndorsementCredential
                        .select(certificateManager)
                        .byEntityId(uuid)
                        .getCertificate();
            case ISSUEDCERTIFICATES:
                return IssuedAttestationCertificate
                        .select(certificateManager)
                        .byEntityId(uuid)
                        .getCertificate();
            case TRUSTCHAIN:
                return CertificateAuthorityCredential
                        .select(certificateManager)
                        .byEntityId(uuid)
                        .getCertificate();
            default:
                return null;
        }
    }

    /**
     * Gets the certificate by the hash code of its bytes. Looks for both
     * archived and unarchived certificates.
     *
     * @param certificateType String containing the certificate type
     * @param certificateHash the hash of the certificate's bytes
     * @param certificateManager the certificate manager to query
     * @return the certificate or null if none is found
     */
    private Certificate getCertificateByHash(
            final String certificateType,
            final int certificateHash,
            final CertificateManager certificateManager) {

        switch (certificateType) {
            case PLATFORMCREDENTIAL:
                return PlatformCredential
                        .select(certificateManager)
                        .includeArchived()
                        .byHashCode(certificateHash)
                        .getCertificate();
            case ENDORSEMENTCREDENTIAL:
                return EndorsementCredential
                        .select(certificateManager)
                        .includeArchived()
                        .byHashCode(certificateHash)
                        .getCertificate();
            case TRUSTCHAIN:
                return CertificateAuthorityCredential
                        .select(certificateManager)
                        .includeArchived()
                        .byHashCode(certificateHash)
                        .getCertificate();
            default:
                return null;
        }
    }

    /**
     * Gets the certificate by the platform serial number.
     *
     * @param certificateType String containing the certificate type
     * @param serialNumber the platform serial number
     * @param certificateManager the certificate manager to query
     * @return the certificate or null if none is found
     */
    private List<PlatformCredential> getCertificateByBoardSN(
            final String certificateType,
            final String serialNumber,
            final CertificateManager certificateManager) {

        if (serialNumber == null) {
            return null;
        }

        switch (certificateType) {
            case PLATFORMCREDENTIAL:
                return PlatformCredential
                        .select(certificateManager)
                        .byBoardSerialNumber(serialNumber)
                        .getCertificates().stream().collect(Collectors.toList());
            default:
                return null;
        }
    }

    /**
     * Parses an uploaded file into a certificate and populates the given model
     * with error messages if parsing fails.
     *
     * @param certificateType String containing the certificate type
     * @param file the file being uploaded from the portal
     * @param model the map of page elements to populate with error messages
     * upon failure
     * @param messages contains any messages that will be display on the page
     * @return the parsed certificate or null if parsing failed.
     */
    private Certificate parseCertificate(
            final String certificateType,
            final MultipartFile file,
            final PageMessages messages) {

        LOGGER.info("Received File of Size: " + file.getSize());

        byte[] fileBytes;
        String fileName = file.getOriginalFilename();

        // build the certificate from the uploaded bytes
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            final String failMessage = "Failed to read uploaded file ("
                    + fileName + "): ";
            LOGGER.error(failMessage, e);
            messages.addError(failMessage + e.getMessage());
            return null;
        }
        try {
            switch (certificateType) {
                case PLATFORMCREDENTIAL:
                    return new PlatformCredential(fileBytes);
                case ENDORSEMENTCREDENTIAL:
                    return new EndorsementCredential(fileBytes);
                case TRUSTCHAIN:
                    return new CertificateAuthorityCredential(fileBytes);
                default:
                    final String failMessage = "Failed to parse uploaded file ("
                            + fileName + "). Invalid certificate type: "
                            + certificateType;
                    LOGGER.error(failMessage);
                    messages.addError(failMessage);
                    return null;
            }
        } catch (IOException e) {
            final String failMessage = "Failed to parse uploaded file ("
                    + fileName + "): ";
            LOGGER.error(failMessage, e);
            messages.addError(failMessage + e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            final String failMessage = "Certificate format not recognized("
                    + fileName + "): ";
            LOGGER.error(failMessage, e);
            messages.addError(failMessage + e.getMessage());
            return null;
        }
    }

    /**
     * Store the given certificate in the database.
     *
     * @param certificateType String containing the certificate type
     * @param fileName contain the name of the file of the certificate to
     * be stored
     * @param messages contains any messages that will be display on the page
     * @param certificate the certificate to store
     * @param certificateManager the DB manager to use
     * @return the messages for the page
     */
    private void storeCertificate(
            final String certificateType,
            final String fileName,
            final PageMessages messages,
            final Certificate certificate,
            final CertificateManager certificateManager) {

        Certificate existingCertificate;

        // look for an identical certificate in the database
        try {
            existingCertificate = getCertificateByHash(
                    certificateType,
                    certificate.getCertificateHash(),
                    certificateManager);
        } catch (DBManagerException e) {
            final String failMessage = "Querying for existing certificate failed ("
                    + fileName + "): ";
            messages.addError(failMessage + e.getMessage());
            LOGGER.error(failMessage, e);
            return;
        }

        try {
            // save the new certificate if no match is found
            if (existingCertificate == null) {
                if (certificateType.equals(PLATFORMCREDENTIAL)) {
                    PlatformCredential platformCertificate = (PlatformCredential) certificate;
                    if (platformCertificate.isBase()) {
                        List<PlatformCredential> sharedCertificates = getCertificateByBoardSN(
                                certificateType,
                                platformCertificate.getPlatformSerial(),
                                certificateManager);

                        if (sharedCertificates != null) {
                            for (PlatformCredential pc : sharedCertificates) {
                                if (pc.isBase()) {
                                    final String failMessage = "Storing certificate failed: "
                                            + "platform credential "
                                            + "chain (" + pc.getPlatformSerial()
                                            + ") base already exists in this chain ("
                                            + fileName + ")";
                                    messages.addError(failMessage);
                                    LOGGER.error(failMessage);
                                    return;
                                }
                            }
                        }
                    } /**else {
                        // this is a delta, check if the holder exists.
                       PlatformCredential holderPC = PlatformCredential
                                .select(certificateManager)
                                .bySerialNumber(platformCertificate.getHolderSerialNumber())
                                .getCertificate();

                        if (holderPC == null)  {
                            final String failMessage = "Storing certificate failed: "
                                    + "delta credential"
                                    + " must have an existing holder stored.  "
                                    + "Credential serial "
                                    + platformCertificate.getHolderSerialNumber()
                                    + " doesn't exist.";
                            messages.addError(failMessage);
                            LOGGER.error(failMessage);
                            return;
                        }
                    }**/
                }

                certificateManager.save(certificate);

                final String successMsg = "New certificate successfully uploaded ("
                    + fileName + ")";
                messages.addSuccess(successMsg);
                LOGGER.info(successMsg);
                return;
            }
        } catch (DBManagerException e) {
            final String failMessage = "Storing new certificate failed ("
                    + fileName + "): ";
            messages.addError(failMessage + e.getMessage());
            LOGGER.error(failMessage, e);
            return;
        }

        try {
            // if an identical certificate is archived, update the existing certificate to
            // unarchive it and change the creation date
            if (existingCertificate.isArchived()) {
                existingCertificate.restore();
                existingCertificate.resetCreateTime();
                certificateManager.update(existingCertificate);

                final String successMsg = "Pre-existing certificate found and unarchived ("
                    + fileName + ")";
                messages.addSuccess(successMsg);
                LOGGER.info(successMsg);
                return;
            }
        } catch (DBManagerException e) {
            final String failMessage = "Found an identical pre-existing certificate in the "
                    + "archive, but failed to unarchive it (" + fileName + "): ";
            messages.addError(failMessage + e.getMessage());
            LOGGER.error(failMessage, e);
            return;
        }

        // if an identical certificate is already unarchived, do nothing and show a fail message
        final String failMessage
                = "Storing certificate failed: an identical certificate already exists ("
                    + fileName + ")";
        messages.addError(failMessage);
        LOGGER.error(failMessage);
    }
}
