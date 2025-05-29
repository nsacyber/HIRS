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
    <div class="download-header">
      <h3>Download HIRS Attestation Log File</h3>
      <a href="${portal}/help/hirs-log/download">
        <img src="${icons}/ic_file_download_black_24dp.png" title="Download HIRS Log">
      </a>
    </div>
    <div class="aca-data-table">
      <h2>Logger Management</h2>
      <table id="loggersTable" class="display" style="width: 100%">
        <thead>
          <tr>
            <th>Logger Name</th>
            <th>Configured Level</th>
          </tr>
        </thead>
      </table>
    </div>
    <div>
      <h3 class="content-subhead" id="alerttype">Documentation</h3>
      <p>
        For more documentation on the project, you may visit the wiki section of
        our <a href="https://github.com/nsacyber/HIRS/wiki">code repository</a>.
      </p>
    </div>
  </jsp:body>
</my:page>

<script>
  $(document).ready(function () {
    const levels = ["ERROR", "WARN", "INFO", "DEBUG", "TRACE"];

    let url = pagePath + "/hirs-log/loggers-list";
    let columns = [
      {
        name: "loggerName",
        data: "loggerName",
        orderable: true,
        searchable: true
      },
      {
        name: "logLevel",
        data: "logLevel",
        orderable: true,
        searchable: true
      }
    ];

    //Set data tables
    let dataTable = setDataTables("#loggersTable", url, columns);
  });
</script>
