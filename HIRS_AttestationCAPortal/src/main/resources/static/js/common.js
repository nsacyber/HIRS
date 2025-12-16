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
 * Handles user request to delete a cert. Prompts user to confirm.
 * Upon confirmation, submits the delete form which is required to make
 * a POST call to delete the credential.
 * @param id certificate id
 */
function handleDeleteRequest(id) {
  if (confirm("Delete certificate?")) {
    $("#deleteForm" + id).submit();
  }
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
 * Handles user request to delete a cert. Prompts user to confirm.
 * Upon confirmation, submits the delete form which is required to make
 * a POST call to delete the reference integrity manifest.
 * @param id RIM ID
 */
function handleRimDeleteRequest(id) {
  if (confirm("Delete RIM?")) {
    $("#deleteForm" + id).submit();
  }
}

/**
 * Set the data tables using the columns definition, the ajax URL,
 * the table ID
 * @param id data table ID
 * @param url url for the AJAX call
 * @param columns table columns definition
 * @param options if configured will override the default configs set by datatable (options include the ability to disable the global search bar, the
 * global paginator, the ordering ability, etc.)
 * @returns a datatable with a combination of the default and customized configurations
 */
function setDataTables(id, url, columns, options = {}) {
  let defaultConfig = {
    processing: true,
    serverSide: true,
    colReorder: true,
    select: true,
    responsive: true,
    pageLength: 10, // Default number of rows per page
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
              dt.search("").columns().columnControl.searchClear().draw();
            },
          },
        ],
      },
    },
    columnDefs: [{ className: "dt-head-center", targets: "_all" }],
    ajax: {
      url: url,
      dataSrc: function (json) {
        formatElementDates(".date");
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
  let icon = iconPath;
  let title = "";

  //If the details is the same certificate type use assignment icon,
  //otherwise use the icon for the certificate type.
  if (sameType) {
    title = "Details";
    icon += "/ic_assignment_black_24dp.png";
  } else {
    switch (type) {
      case "issued":
        icon += "/ic_library_books_black_24dp.png";
        title = "View Issued Attestation Certificate Details";
        break;
      case "platform":
        icon += "/ic_important_devices_black_24dp.png";
        title = "View Platform Certificate Details";
        break;
      case "endorsement":
        icon += "/ic_vpn_key_black_24dp.png";
        title = "View Endorsement Certificate Details";
        break;
      case "idevid":
        icon += "/ic_vpn_key_black_24dp.png";
        title = "View IDevID Certificate Details";
        break;
    }
  }
  const html =
    "<a href=" +
    href +
    ">" +
    '<img src="' +
    icon +
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
  const icon = iconPath + "/ic_assignment_black_24dp.png";
  const title = "Details";

  const html =
    "<a href=" +
    href +
    ">" +
    '<img src="' +
    icon +
    '" title="' +
    title +
    '"></a>';
  return html;
}

/**
 * Create a certificate delete link for the specified ID
 * @param pagePath path to the link
 * @param id of the certificate
 * @returns a HTML string of the certificate delete link
 */
function certificateDeleteLink(pagePath, id) {
  const icon = iconPath + "/ic_delete_black_24dp.png";
  const formURL = pagePath + "/delete";

  const html =
    '<a href="#!" onclick="handleDeleteRequest(\'' +
    id +
    "')\">" +
    '<img src="' +
    icon +
    '" title="Delete"></a>' +
    '<form id="deleteForm' +
    id +
    '" action="' +
    formURL +
    '" method="post">' +
    '<input name="id" type="hidden" value="' +
    id +
    '"></form>';
  return html;
}

/**
 * Create a RIM delete link for the specified ID
 * @param pagePath path to the link
 * @param id of the RIM
 * @returns a HTML string of the RIM delete link
 */
function rimDeleteLink(pagePath, id) {
  const icon = iconPath + "/ic_delete_black_24dp.png";
  const formURL = pagePath + "/delete";

  const html =
    '<a href="#!" onclick="handleRimDeleteRequest(\'' +
    id +
    "')\">" +
    '<img src="' +
    icon +
    '" title="Delete"></a>' +
    '<form id="deleteForm' +
    id +
    '" action="' +
    formURL +
    '" method="post">' +
    '<input name="id" type="hidden" value="' +
    id +
    '"></form>';
  return html;
}

/**
 * Create a certificate download link for the specified ID
 * @param pagePath path to the link
 * @param id of the certificate
 */
function certificateDownloadLink(pagePath, id) {
  const href = pagePath + "/download?id=" + id;
  const icon = iconPath + "/ic_file_download_black_24dp.png";

  const html =
    '<a href="' +
    href +
    '">' +
    '<img src="' +
    icon +
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
 * Formats a given Java date string to a UTC date string, or returns an indefinite value.
 * @param dateText - The Java Date string (ISO 8601 format)
 * @returns Formatted date or "Indefinite" if invalid.
 */
function formatCertificateDate(dateText) {
  // Check if the dateText is "Indefinite" (handle special case)
  if (dateText === "Indefinite") {
    return dateText;
  }

  // Use Moment.js to parse the ISO 8601 date string
  const momentDate = moment(dateText); // `dateText` should be an ISO 8601 string

  // If the date is invalid, return "Indefinite"
  if (!momentDate.isValid()) {
    return "Indefinite";
  }

  // Format the date (you can adjust the format as needed)
  return momentDate.utc().format("ddd, DD MMM YYYY HH:mm:ss [GMT]");
}
