<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>
<%-- CONTENT --%>

<my:page>
  <jsp:attribute name="pageHeaderTitle">Help</jsp:attribute>

  <jsp:attribute name="script">
    <script
      type="text/javascript"
      src="${lib}/jquery.spring-friendly/jquery.spring-friendly.js"
    ></script>
  </jsp:attribute>

  <jsp:body>
    <div>
      <h2>Documentation</h2>
      <p>
        For more documentation on the project, you may visit the wiki section of
        our <a href="https://github.com/nsacyber/HIRS/wiki">code repository</a>.
      </p>
    </div>
    <hr />
    <div class="aca-data-table">
      <h2>Main HIRS Logger Management</h2>
      <div class="download-header">
        <h3>Download HIRS Attestation Log File</h3>
        <a href="${portal}/help/hirs-log-download">
          <img src="${icons}/ic_file_download_black_24dp.png" title="Download HIRS Log">
        </a>
      </div>
      <table id="loggersTable" class="display" style="width: 100%">
        <thead>
          <tr>
            <th>Logger Name</th>
            <th>Current Log Level</th>
            <th>Change Log Level</th>
          </tr>
        </thead>
      </table>
    </div>
  </jsp:body>
</my:page>

<script>
  $(document).ready(function () {
    const levels = ["ERROR", "WARN", "INFO", "DEBUG"];

    const mainLoggerName = "hirs.attestationca";
    const mainLoggerPath = "var/log/hirs/HIRS_AttestationCA_Portal.log";

    // Mapping log levels to button colors
    const logLevelColors = {
      ERROR: "btn-danger", // Red for ERROR
      WARN: "btn-warning", // Yellow for WARN
      INFO: "btn-info", // Light Blue for INFO
      DEBUG: "btn-primary", // Blue for DEBUG
    };

    let url = pagePath + "/list-main-logger";
    let columns = [
      {
        name: "loggerName",
        data: "loggerName",
        render: function (data, type, row) {
          // if the row's logger name is the same as the main logger name
          if (row.loggerName.toLowerCase() === mainLoggerName.toLowerCase()) {
            // display the main logger's path
            return mainLoggerPath;
          }

          // otherwise just return the row's logger name as is
          return row.loggerName;
        },
      },
      {
        name: "logLevel",
        data: "logLevel",
      },
      {
        name: "changeLogLevel",
        data: null,
        render: function (data, type, row) {
          // Start the btn-group div to wrap all the buttons
          let buttonsHtml = '<div class="btn-group" role="group">';

          // Loop through the log levels and generate buttons
          levels.forEach(function (logLevel) {
            let logLevelColor = logLevelColors[logLevel] || "btn-secondary"; // Default to gray if color not found

            // Append button HTML for the current log level
            buttonsHtml +=
              '<button type="button" class="btn ' +
              logLevelColor +
              ' btn-sm" onclick="handleLogLevelChange(\'' +
              row.loggerName +
              "', '" +
              logLevel +
              "')\">" +
              logLevel +
              "</button>";
          });

          // Close the btn-group div
          buttonsHtml += "</div>";

          return buttonsHtml;
        }
      },
    ];

    //Set data tables
    let dataTable = setDataTables("#loggersTable", url, columns, { searching: false, ordering : false, paging: false, info: false });
  });
</script>
