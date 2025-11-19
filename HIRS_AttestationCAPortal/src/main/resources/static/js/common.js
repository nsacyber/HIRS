const iconPath = "/icons";

/**
 * Converts a byte array to a HEX string.
 * @param {Uint8Array | number[]} arr - The byte array to convert.
 * @returns {string} The HEX string representation of the byte array.
 */
function byteToHexString(arr) {
  let str = "";
  $.each(arr, function (index, value) {
    str += ("0" + (value & 0xff).toString(16)).slice(-2) + ":​";
  });
  return str.substring(0, str.length - 2).toUpperCase();
}

/**
 * Parses a hex string for display.
 * @param {string} hexString - The hex string to parse.
 * @returns {string} The parsed hex string.
 */
function parseHexString(hexString) {
  let str = hexString.toUpperCase();
  //Do not parse if there is 2 characters
  if (str.length === 2) {
    return str;
  }
  return str.match(/.{2}/g).join(":​");
}

/**
 * Parses a HEX string to display as a byte hex string.
 * @param {string} hexString - The HEX string to parse.
 * @returns {string} The parsed byte hex string.
 */
function parseSerialNumber(hexString) {
  let str = hexString.toUpperCase();
  if (str.length % 2 !== 0) {
    str = "0" + hexString;
  }
  //Do not parse if there is 2 characters
  if (str.length === 2) {
    return str;
  }
  //Parse and return
  return (newString = str.match(/.{2}/g).join(":​"));
}

/**
 * Configures and returns the button setup for the DataTable based on the page context.
 *
 * This function defines the button configuration for the DataTable, including options
 * for page length, column visibility, export buttons (CSV, Excel, PDF), and custom buttons
 * like "Clear All" and "Delete Selected". The buttons array is dynamically updated based
 * on the current page.
 *
 * @param {string} pageName - The name or identifier of the current page, used to adjust
 *                            the buttons shown (e.g., enabling "Delete Selected").
 *
 * @returns {Array} The array of button configurations for the DataTable.
 */
function initializeDataTableButtonSetup(pageName) {
  const pageNames = [
    "endorsement-key-credentials",
    "trust-chain",
    "platform-credentials",
    "idevid-certificates",
    "issued-certificates",
    "reference-manifests",
  ];

  // this configuration contains the default buttons: "Page Length", "Clear All", "Export", "Column Visibility"
  // for all the DataTables
  let dataTableButtons = [
    "pageLength",
    "colvis",
    {
      extend: "collection",
      text: "Export",
      buttons: [
        {
          extend: "copy",
          text: '<img src="/icons/svg/copy-20dp.svg" alt="CSV"> Copy',
          exportOptions: { columns: ":visible" },
        },
        {
          extend: "csv",
          text: '<img src="/icons/svg/filetype-csv-20dp.svg" alt="CSV"> CSV',
          exportOptions: { columns: ":visible" },
        },
        {
          extend: "excel",
          text: '<img src="/icons/svg/file-earmark-spreadsheet-20dp.svg" alt="XLS"> Excel',
          exportOptions: { columns: ":visible" },
        },
        {
          extend: "pdfHtml5",
          text: '<img src="/icons/svg/filetype-pdf-20dp.svg" alt="PDF"> PDF',
          exportOptions: { columns: ":visible" },
          orientation: "landscape",
          pageSize: "LEGAL",
        },
        {
          extend: "print",
          text: '<img src="/icons/svg/printer-20dp.svg" alt="Print"> Print',
          exportOptions: { columns: ":visible" },
          customize: function (win) {
            $(win.document.body).find("table").css({
              "font-size": "10pt", // Scale down the font size
            });
          },
        },
      ],
    },
    {
      text: "Clear All",
      action: function (e, dt, node, config) {
        dt.search("").columns().columnControl.searchClear().draw();
        dt.order([]).draw();
      },
    },
  ];

  // If the page is one of the specified pages, add the "Delete Selected" button with appropriate configuration
  if (pageNames.includes(pageName)) {
    dataTableButtons.push({
      text: "Delete Selected",
      action: function (e, dt, node, config) {
        const selectedRows = dt.rows({ selected: true });
        const modalTargetId = "#deleteMultipleConfirmationModal";
        if (selectedRows.count() > 0) {
          // Get an array of IDs of the selected rows
          const selectedIds = selectedRows
            .data()
            .toArray()
            .map((row) => row.id);

          // Store the selected IDs in the hidden input field inside the modal as a comma-separated list
          $("#selectedRecordsToDeleteIds").val(selectedIds.join(","));

          // Update the modal text with the number of records to delete
          $("#numRecordsToDelete").text(selectedIds.length);

          // If there are selected rows, show the modal
          $(modalTargetId).modal("show");
        }
      },
      attr: {
        id: "deleteSelectedButton",
        disabled: true,
        class: "btn btn-danger",
      },
    });
  }

  return dataTableButtons;
}

/**
 * Initializes a DataTable with the specified configuration, including columns, AJAX URL, and additional settings.
 *
 * @param {string} viewName - The name or identifier of the current page, used to adjust the DataTable configuration (e.g., enabling certain buttons).
 * @param {string} id - The ID of the DataTable element.
 * @param {string} url - The URL for the AJAX request to fetch table data.
 * @param {Array} columns - An array of column definitions (e.g., column names and data properties).
 * @param {Object} [customConfig] - Optional configuration object to customize the DataTable settings (e.g., disable search, pagination, or ordering).
 * @returns {DataTable} A DataTable instance with the provided configurations.
 */
function setDataTables(viewName, id, url, columns, customConfig = {}) {
  let defaultConfig = {
    processing: true,
    serverSide: true,
    colReorder: true,
    select: {
      style: "multi",
      selector: "td:first-child",
      headerCheckbox: "select-page",
    },
    responsive: true,
    lengthMenu: [
      [10, 25, 50, 75, 100, 250, -1],
      [
        "10 rows",
        "25 rows",
        "50 rows",
        "75 rows",
        "100 rows",
        "250 rows",
        "Show All Rows",
      ],
    ],
    layout: {
      topStart: {
        buttons: initializeDataTableButtonSetup(viewName),
      },
    },
    columnDefs: [{ className: "dt-head-center", targets: "_all" }],
    ajax: {
      url: url,
      dataSrc: function (json) {
        return json.data;
      },
    },
    columns: columns,
    order: [], // Ensure no initial ordering
  };

  // Merge custom user configuration over default configuration
  const finalConfiguration = { ...defaultConfig, ...customConfig };

  const dataTable = new DataTable(id, finalConfiguration);

  handleDeleteMultipleButtonState(dataTable);

  return dataTable;
}

/**
 * Attaches event listeners to a DataTable to handle row selection and toggle the "Delete Selected" button's
 * enabled/disabled state based on the number of selected rows.
 *
 * This function listens for the `select` and `deselect` events on the DataTable and enables the "Delete Selected"
 * button if at least one row is selected, or disables it when no rows are selected.
 *
 * @param {DataTable} dataTable - The DataTable instance to which the event listeners will be attached.
 */
function handleDeleteMultipleButtonState(dataTable) {
  // Add event listener to handle row selection and button enabling/disabling
  dataTable.on("select deselect", function () {
    const selectedRowsCount = dataTable.rows({ selected: true }).count();
    const deleteButton = $("#deleteSelectedButton");

    // Only attempt to enable/disable the delete button if it exists
    if (deleteButton) {
      // Enable/Disable the delete button based on selected row count
      if (selectedRowsCount > 0) {
        deleteButton.prop("disabled", false);
      } else {
        deleteButton.prop("disabled", true);
      }
    }
  });
}

/**
 * Generates an HTML button that triggers a modal to change the log level for a specific logger.
 * The button is styled according to the provided log level color and displays the new log level.
 *
 * @param {string} loggerName - The name of the logger whose log level is being changed.
 * @param {string} currentLogLevel - The current log level of the logger.
 * @param {string} newLogLevel - The new log level that the button represents (this will be displayed on the button).
 * @param {string} logLevelColor - The CSS class that determines the button's color (e.g., `btn-primary`, `btn-danger`).
 * @returns {string} An HTML string representing the button that triggers the log level change modal.
 */
function generateLogLevelChangeButton(
  loggerName,
  currentLogLevel,
  newLogLevel,
  logLevelColor
) {
  const modalTargetId = `#logLevelChangeConfirmationModal`;

  // Generate the modal trigger button for the specified log level
  const html =
    `<button type="button" ` +
    `class="btn ${logLevelColor} btn-sm action-icons" ` + // Use dynamic color
    `data-bs-toggle="modal" ` +
    `data-bs-target="${modalTargetId}" ` +
    `data-currentLogLevel="${currentLogLevel}" ` +
    `data-loggerName="${loggerName}" ` +
    `data-newLogLevel="${newLogLevel}" ` +
    `aria-label="Change Log Level Link">` +
    newLogLevel + // Display the log level text inside the button
    `</button>`;
  return html;
}

/**
 * Generates an HTML string for a certificate detail link based on the
 * specified certificate type and certificate ID.
 *
 * @param {string} type - The type of the certificate.
 * @param {string} certificateId - The unique identifier for the certificate.
 * @param {boolean} sameType - Indicates whether the details belong to the same certificate type.
 * @returns {string} An HTML string representing the certificate detail link.
 */
function generateCertificateDetailsLink(
  certificateType,
  certificateId,
  sameType
) {
  const href =
    "/HIRS_AttestationCAPortal/portal/certificate-details?id=" +
    certificateId +
    "&type=" +
    certificateType;
  let fullIconPath = iconPath;
  let title = "";

  //If the details is the same certificate type use assignment icon,
  //otherwise use the icon for the certificate type.
  if (sameType) {
    title = "Details";
    fullIconPath += "/ic_assignment_black_24dp.png";
  } else {
    switch (certificateType) {
      case "issued":
        fullIconPath += "/ic_library_books_black_24dp.png";
        title = "View Issued Attestation Certificate Details";
        break;
      case "platform":
        fullIconPath += "/ic_important_devices_black_24dp.png";
        title = "View Platform Certificate Details";
        break;
      case "endorsement":
        fullIconPath += "/ic_vpn_key_black_24dp.png";
        title = "View Endorsement Certificate Details";
        break;
      case "idevid":
        fullIconPath += "/ic_vpn_key_black_24dp.png";
        title = "View IDevID Certificate Details";
        break;
    }
  }

  const html = `
  <a href="${href}">
    <img src="${fullIconPath}" class="action-icons" title="${title}">
  </a>
`;

  return html;
}

/**
 * Generates an HTML string for a RIM detail link based on the specified
 * page path and the RIM ID.
 *
 * @param {string} pagePath - The prefix needed to find path to the details REST endpoint for RIMS.
 * @param {string} rimId - The unique identifier for the RIM.
 * @returns {string} An HTML string representing the RIM detail link.
 */
function generateRimDetailsLink(rimId) {
  const href = "rim-details?id=" + rimId;
  const fullIconPath = iconPath + "/ic_assignment_black_24dp.png";
  const title = "Details";

  const html = `
  <a href="${href}">
    <img src="${fullIconPath}" class="action-icons" title="${title}">
  </a>
`;

  return html;
}

/**
 * Generates an HTML string for a certificate delete link based on the given certificate ID.
 *
 * @param {string} certificateId - The ID of the certificate to delete.
 * @returns {string} An HTML string representing a delete link for the certificate.
 */
function generateCertificateDeleteLink(certificateId) {
  const fullIconPath = iconPath + "/svg/trash-24dp.svg";
  const modalTargetId = `#deleteCertificateConfirmationModal`;

  const html =
    `<a href="${modalTargetId}" ` +
    'data-bs-toggle="modal" ' +
    `data-bs-target="${modalTargetId}" ` +
    `data-id="${certificateId}" ` +
    'aria-label="Delete Certificate Link">' +
    `<img src="${fullIconPath}" class="action-icons" alt="Delete Certificate Link" title="Delete Certificate">` +
    "</a>";

  return html;
}

/**
 * Generates an HTML string for a RIM delete link based on the given RIM ID.
 *
 * @param {string} rimId - The ID of the RIM entry to delete.
 * @returns {string} An HTML string representing a delete link for the RIM.
 */
function generateRIMDeleteLink(rimId) {
  const fullIconPath = iconPath + "/svg/trash-24dp.svg";
  const modalTargetId = `#deleteRIMConfirmationModal`;

  const html =
    `<a href="${modalTargetId}" ` +
    'data-bs-toggle="modal" ' +
    `data-bs-target="${modalTargetId}" ` +
    `data-id="${rimId}" ` +
    'aria-label="Delete RIM Link">' +
    `<img src="${fullIconPath}" class="action-icons" alt="Delete RIM Link" title="Delete RIM">` +
    "</a>";

  return html;
}

/**
 * Generates a download link for the specified certificate ID.
 *
 * @param {string} pagePath - The prefix needed to find path to the download REST endpoint for certificates.
 * @param {string} certificateId - The unique identifier for the certificate.
 * @returns {string} An HTML string representing a download link for the certificate.
 */
function generateCertificateDownloadLink(pagePath, certificateId) {
  const href = pagePath + "/download?id=" + certificateId;
  const fullIconPath = iconPath + "/ic_file_download_black_24dp.png";

  const html = `
  <a href="${href}">
    <img src="${fullIconPath}" class="action-icons" title="Download Certificate">
  </a>
`;

  return html;
}

/**
 * Generates a download link for the specified RIM ID.
 *
 * @param {string} pagePath - The prefix needed to find path to the download REST endpoint for RIMS.
 * @param {string} rimId - The unique identifier for the RIM.
 * @returns {string} An HTML string representing a download link for the RIM.
 */
function generateRimDownloadLink(pagePath, rimId) {
  const icon = iconPath + "/ic_file_download_black_24dp.png";
  const href = pagePath + "/download?id=" + rimId;

  const html = `
  <a href="${href}">
    <img src="${icon}" class="action-icons" title="Download Reference Integrity Manifest">
  </a>
`;

  return html;
}

/**
 * Formats a given date to a UTC string, or returns Indefinite (802.1AR)
 * @param dateText expected to be an ISO 8601 date-time format
 * @returns a string representing a date in RFC 7231 format, or "Indefinite"
 * example 2018-01-01T05:00:00.000Z -> Mon, 01 Jan 2018 05:00:00 GMT
 * example 2017-06-19T09:45:41.000+00:00 -> Mon, 19 Jun 2017 09:45:41 GMT
 */
function formatCertificateDate(dateText) {
  const timestamp = Date.parse(dateText); // Convert to numeric

  if (timestamp == 253402300799000) {
    // Handle special case: Indefinite per 802.1AR
    return "Indefinite";
  }

  return new Date(timestamp).toUTCString();
}
