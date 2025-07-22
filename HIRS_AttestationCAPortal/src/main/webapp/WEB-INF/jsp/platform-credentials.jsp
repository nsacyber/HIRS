<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>
<my:page>
  <jsp:attribute name="script">
    <script
      type="text/javascript"
      src="${lib}/jquery.spring-friendly/jquery.spring-friendly.js"
    ></script>
  </jsp:attribute>
  <jsp:attribute name="pageHeaderTitle">Platform Certificates</jsp:attribute>

  <jsp:body>
    <!-- text and icon resource variables -->
    <c:set var="endorsementIcon" value="${icons}/ic_vpn_key_black_24dp.png" />
    <div class="aca-input-box-header">
      <form:form
        method="POST"
        action="${portal}/certificate-request/platform-credentials/upload"
        enctype="multipart/form-data"
      >
        Platform Credentials
        <my:file-chooser
          id="platformCredentialEditor"
          label="Import Platform Credentials"
        >
          <input id="importFile" type="file" name="file" multiple="multiple" />
        </my:file-chooser>
        <a href="${portal}/certificate-request/platform-credentials/bulk-download">
          <img src="${icons}/ic_file_download_black_24dp.png" title="Download All Platform Certificates">  
        </a>
      </form:form>
    </div>
    <br />
    <div class="aca-data-table">
      <table id="platformTable" class="display" width="100%">
        <thead>
          <tr>
            <th>Device</th>
            <th>Issuer</th>
            <th>Type</th>
            <th>Manufacturer</th>
            <th>Model</th>
            <th>Version</th>
            <th>Board SN</th>
            <th>Valid (begin)</th>
            <th>Valid (end)</th>
            <th>Endorsement</th>
            <th>Options</th>
          </tr>
        </thead>
      </table>
    </div>

    <script>
      $(document).ready(function () {
        let url = pagePath + "/list";
        let columns = [
          {
            name: "deviceName",
            data: "deviceName",
            orderable: true,
            searchable: true,
            render: function (data, type, full, meta) {
              // if there is a device, display its name, otherwise
              return full.deviceName;
            },
          },
          {
            name: "issuer",
            data: "issuer",
            orderable: true,
            searchable: true,
          },
          {
            name: "credentialType",
            data: "credentialType",
            render: function (data, type, full, meta) {
              if (full.platformChainType !== "") {
                return full.platformChainType;
              } else {
                return full.credentialType;
              }
            },
          },
          {
            name: "manufacturer",
            data: "manufacturer",
            orderable: true,
            searchable: true,
          },
          { name: "model", data: "model", orderable: true, searchable: true },
          { name: "version", data: "version", orderable: true, searchable: true },
          { name: "platformSerial",data: "platformSerial", orderable: true, searchable: true },
          {
            name: "beginValidity",
            data: "beginValidity",
            orderable: false,
            searchable: false,
            render: function (data, type, full, meta) {
              return formatCertificateDate(full.beginValidity);
            },
          },
          {
            name: "endValidity",
            data: "endValidity",
            orderable: false,
            searchable: false,
            render: function (data, type, full, meta) {
              return formatCertificateDate(full.endValidity);
            },
          },
          {
            data: "id",
            orderable: false,
            searchable: false,
            render: function (data, type, full, meta) {
              //Display endorsement credential
              if (full.endorsementCredential === null) return "";
              let html = "";

              let id = full.endorsementCredential.id;
              html =
                certificateDetailsLink("endorsement", id, false) + "&nbsp;";

              return html;
            },
          },
          {
            data: "id",
            orderable: false,
            searchable: false,
            render: function (data, type, full, meta) {
              // Set up a delete icon with link to handleDeleteRequest().
              // sets up a hidden input field containing the ID which is
              // used as a parameter to the REST POST call to delete
              let html = "";
              html += certificateDetailsLink("platform", full.id, true);
              html += certificateDownloadLink(full.id, pagePath);
              html += certificateDeleteLink(full.id, pagePath);

              return html;
            },
          },
        ];

        //Set data tables
        setDataTables("#platformTable", url, columns);
      });
    </script>
  </jsp:body>
</my:page>
