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
 * Initializes a DataTable with the specified configuration, including columns, AJAX URL, and additional settings.
 *
 * @param {string} id - The ID of the DataTable element.
 * @param {string} url - The URL for the AJAX request to fetch table data.
 * @param {Array} columns - An array of column definitions (e.g., column names and data properties).
 * @param {Object} [customConfig] - Optional configuration object to customize the DataTable settings (e.g., disable search, pagination, or ordering).
 * @returns {DataTable} A DataTable instance with the provided configurations.
 */
function setDataTables(id, url, columns, customConfig = {}) {
  let defaultConfig = {
    processing: true,
    serverSide: true,
    colReorder: true,
    select: true,
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
        buttons: [
          "pageLength",
          "colvis",
          {
            extend: "collection",
            text: "Export",
            buttons: [
              {
                extend: "copy",
                text: '<img src="/icons/svg/copy-20dp.svg" alt="CSV"> Copy',
                exportOptions: {
                  columns: ":visible",
                },
              },
              {
                extend: "csv",
                text: '<img src="/icons/svg/filetype-csv-20dp.svg" alt="CSV"> CSV',
                exportOptions: {
                  columns: ":visible",
                },
              },
              {
                extend: "excel",
                text: '<img src="/icons/svg/file-earmark-spreadsheet-20dp.svg" alt="XLS"> Excel',
                exportOptions: {
                  columns: ":visible",
                },
              },
              {
                extend: "pdfHtml5",
                text: '<img src="/icons/svg/filetype-pdf-20dp.svg" alt="PDF"> PDF',
                exportOptions: {
                  columns: ":visible",
                },
                orientation: "landscape",
                pageSize: "LEGAL",
              },
              {
                extend: "print",
                text: '<img src="/icons/svg/printer-20dp.svg" alt="Print"> Print',
                exportOptions: {
                  columns: ":visible",
                },
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
              // Clear search and reset column controls
              dt.search("").columns().columnControl.searchClear().draw();

              // Reset ordering
              dt.order([]).draw();
            },
          },
        ],
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
  };

  // Merge user options over default config
  const finalConfiguration = { ...defaultConfig, ...customConfig };

  return new DataTable(id, finalConfiguration);
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
    `class="btn ${logLevelColor} btn-sm" ` + // Use dynamic color
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
function generateCertificateDetailsLink(type, certificateId, sameType) {
  const href = "certificate-details?id=" + certificateId + "&type=" + type;
  let fullIconPath = iconPath;
  let title = "";

  //If the details is the same certificate type use assignment icon,
  //otherwise use the icon for the certificate type.
  if (sameType) {
    title = "Details";
    fullIconPath += "/ic_assignment_black_24dp.png";
  } else {
    switch (type) {
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
    <img src="${fullIconPath}" class="options-icons" title="${title}">
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
function generateRimDetailsLink(pagePath, rimId) {
  const href = pagePath + "/rim-details?id=" + rimId;
  const fullIconPath = iconPath + "/ic_assignment_black_24dp.png";
  const title = "Details";

  const html = `
  <a href="${href}">
    <img src="${fullIconPath}" class="options-icons" title="${title}">
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
    `<img src="${fullIconPath}" class="options-icons" alt="Delete Certificate Link" title="Delete Certificate">` +
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
    `<img src="${fullIconPath}" class="options-icons" alt="Delete RIM Link" title="Delete RIM">` +
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
    <img src="${fullIconPath}" class="options-icons" title="Download Certificate">
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
    <img src="${icon}" class="options-icons" title="Download Reference Integrity Manifest">
  </a>
`;

  return html;
}

/**
 * Formats a given date to a UTC string, or returns "Indefinite" (802.1AR).
 * @param {string | Date} dateText - The date to format.
 * @returns {string} The formatted date in RFC 7231 format, or "Indefinite".
 */
function formatCertificateDate(dateText) {
  const timestamp = Date.parse(dateText); // Convert to numeric

  if (timestamp == 253402300799000) {
    // Handle special case: Indefinite per 802.1AR
    return "Indefinite";
  }

  return new Date(timestamp).toUTCString();
}
