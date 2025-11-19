const iconPath = "/icons";

/**
 * Converts a byte to HEX.
 */
function byteToHexString(arr) {
  let str = "";
  $.each(arr, function (index, value) {
    str += ("0" + (value & 0xff).toString(16)).slice(-2) + ":​";
  });
  return str.substring(0, str.length - 2).toUpperCase();
}

/**
 Parses hex string for display.
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
 */
function setDataTables(id, url, columns, options = {}) {
  let defaultConfig = {
    processing: true,
    serverSide: true,
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
 */
function certificateDetailsLink(type, id, sameType) {

  const href = "/HIRS_AttestationCAPortal/portal/certificate-details?id=" + id + "&type=" + type;
//  const href = "certificate-details?id=" + id + "&type=" + type;
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
 * @param id of the rim
 */
function rimDetailsLink(id) {
  const href = "rim-details?id=" + id;
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
 * Formats a given date to a UTC string, or returns an indefinite icon
 * @param date to format
 */
function formatCertificateDate(dateText) {
  const timestamp = Date.parse(dateText); // Convert to numeric

  if (timestamp == 253402300799000) {
    return "Indefinite";
  }

  return new Date(timestamp).toUTCString();
}
