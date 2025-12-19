const iconPath = "/icons";

/**
 * Converts a byte to HEX.
 * @param arr byte array
 * @returns a hex string
 */
function byteToHexString(arr) {
  let str = "";
  $.each(arr, function (index, value) {
    str += ("0" + (value & 0xff).toString(16)).slice(-2) + ":​";
  });
  return str.substring(0, str.length - 2).toUpperCase();
}

/**
 * Parses hex string for display.
 * @param hexString hex string
 * @returns a parsed hex string
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
 * Parses the HEX string value to display as byte hex string.
 * @param hexString hex string
 * @returns a parsed byte hex string
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
 * Sends a POST request to the backend every time the user decides to change the
 * selected logger's log level to a different log level.
 * @param loggerName logger name
 * @param logLevel new log level
 */
function handleLogLevelChange(loggerName, newLogLevel) {
  if (
    confirm(
      `Are you sure you want to change the log level for ${loggerName} to ${newLogLevel}?`
    )
  ) {
    // Construct the URL with the query parameters
    const url =
      "help/setLogLevel?loggerName=" +
      encodeURIComponent(loggerName) +
      "&logLevel=" +
      encodeURIComponent(newLogLevel);

    // Make the POST request to change the log level
    $.ajax({
      url: url, // Use the constructed URL with query parameters
      type: "POST",
      success: function (response) {
        // show a success message
        alert(
          `Logger ${loggerName}'s level changed to ${newLogLevel} successfully!`
        );
        $("#loggersTable").DataTable().ajax.reload(); // Reload DataTable to reflect the change
      },
      error: function (xhr, status, error) {
        alert("Error changing log level: " + error);
      },
    });
  }
}

/**
 * Initializes a data table with the specified configuration options, including columns, AJAX URL, and table-specific settings.
 *
 * @param {string} id - The ID of the data table element.
 * @param {string} url - The URL for the AJAX request to fetch data for the table.
 * @param {Array} columns - An array of column definitions for the data table (e.g., column names, data properties).
 * @param {Object} [options] - Optional configuration object to override default DataTable settings (e.g., disabling search, pagination, or ordering).
 * @returns {DataTable} A DataTable instance with the provided configurations (both default and custom options).
 */
function setDataTables(id, url, columns, options = {}) {
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
                exportOptions: {
                  columns: ":visible",
                },
              },
              {
                extend: "csv",
                exportOptions: {
                  columns: ":visible",
                },
              },
              {
                extend: "excel",
                exportOptions: {
                  columns: ":visible",
                },
              },
              {
                extend: "pdfHtml5",
                exportOptions: {
                  columns: ":visible",
                },
                orientation: "landscape",
                pageSize: "LEGAL",
              },
              {
                extend: "print",
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

              // Reset the ordering to default
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
  const config = { ...defaultConfig, ...options };

  return new DataTable(id, config);
}

/**
 * Create a certificate details like for the specified certificate
 * type and ID with the corresponding icon.
 * @param type of certificate
 * @param id of the certificate
 * @param sameType boolean indicating if the details is the same
 *       certificate type.
 * @returns a HTML string of a certificate detail link
 */
function certificateDetailsLink(type, id, sameType) {
  const href = "certificate-details?id=" + id + "&type=" + type;
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
  const html =
    "<a href=" +
    href +
    ">" +
    '<img src="' +
    fullIconPath +
    '" title="' +
    title +
    '"></a>';
  return html;
}

/**
 * Create a RIM details like for the specified rim.
 * type and ID with the corresponding icon.
 * @param pagePath path to the link
 * @param id of the rim
 * @returns a HTML string of a RIM detail link
 */
function rimDetailsLink(pagePath, id) {
  const href = pagePath + "/rim-details?id=" + id;
  const fullIconPath = iconPath + "/ic_assignment_black_24dp.png";
  const title = "Details";

  const html =
    "<a href=" +
    href +
    ">" +
    '<img src="' +
    fullIconPath +
    '" title="' +
    title +
    '"></a>';
  return html;
}

/**
 * Generates an HTML string for a certificate delete link based on the given certificate ID.
 *
 * @param {string} certificateId - The ID of the certificate to delete. This should be a string that uniquely identifies the certificate.
 * @returns {string} A string of HTML representing a delete link for the certificate.
 */
function generateCertificateDeleteLink(certificateId) {
  const fullIconPath = iconPath + "/svg/trash.svg";
  const modalTargetId = `#deleteCertificateConfirmationModal`;

  const html =
    `<a href="${modalTargetId}" ` +
    'data-bs-toggle="modal" ' +
    `data-bs-target="${modalTargetId}" ` +
    `data-id="${certificateId}" ` +
    'aria-label="Delete Certificate Link">' +
    `<img src="${fullIconPath}" alt="Delete Certificate Link" title="Delete Certificate">` +
    "</a>";

  return html;
}

/**
 * Generates an HTML string for a RIM delete link based on the given RIM ID.
 *
 * @param {string} rimId - The ID of the RIM entry to delete. This should be a string that uniquely identifies the RIM.
 * @returns {string} A string of HTML representing a delete link for the RIM.
 */
function generateRIMDeleteLink(rimId) {
  const fullIconPath = iconPath + "/svg/trash.svg";
  const modalTargetId = `#deleteRIMConfirmationModal`;

  const html =
    `<a href="${modalTargetId}" ` +
    'data-bs-toggle="modal" ' +
    `data-bs-target="${modalTargetId}" ` +
    `data-id="${rimId}" ` +
    'aria-label="Delete RIM Link">' +
    `<img src="${fullIconPath}" alt="Delete RIM Link" title="Delete RIM">` +
    "</a>";

  return html;
}

/**
 * Create a certificate download link for the specified ID
 * @param pagePath path to the link
 * @param id of the certificate
 */
function certificateDownloadLink(pagePath, id) {
  const href = pagePath + "/download?id=" + id;
  const fullIconPath = iconPath + "/ic_file_download_black_24dp.png";

  const html =
    '<a href="' +
    href +
    '">' +
    '<img src="' +
    fullIconPath +
    '" title="Download Certificate"></a>';

  return html;
}

/**
 * Create a rim download link for the specified ID
 * @param pagePath path to the link
 * @param id of the rim
 */
function rimDownloadLink(pagePath, id) {
  const icon = iconPath + "/ic_file_download_black_24dp.png";
  const href = pagePath + "/download?id=" + id;

  const html =
    '<a href="' +
    href +
    '">' +
    '<img src="' +
    icon +
    '" title="Download Reference Integrity Manifest"></a>';

  return html;
}

/**
 * Formats a given date to a UTC string, or returns Indefinite (802.1AR)
 * @param date to format
 * @returns a string representing a date in RFC 7231 format, or "Indefinite"
 */
function formatCertificateDate(dateText) {
  const timestamp = Date.parse(dateText); // Convert to numeric

  if (timestamp == 253402300799000) {
    // Handle special case: Indefinite per 802.1AR
    return "Indefinite";
  }

  return new Date(timestamp).toUTCString();
}
