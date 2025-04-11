<!-- <%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%> -->
<my:page>
  <jsp:attribute name="script">
    <script
      type="text/javascript"
      src="${lib}/jquery.spring-friendly/jquery.spring-friendly.js"
    ></script>
  </jsp:attribute>
  <jsp:attribute name="pageHeaderTitle">Device Listing</jsp:attribute>

  <jsp:body>
    <!-- text and icon resource variables -->
    <c:set
      var="passIcon"
      value="${icons}/ic_checkbox_marked_circle_black_green_24dp.png"
    />
    <c:set var="failIcon" value="${icons}/ic_error_red_24dp.png" />
    <c:set var="errorIcon" value=".${icons}/ic_error_black_24dp.png" />
    <c:set
      var="unknownIcon"
      value="${icons}/ic_questionmark_circle_orange_24dp.png"
    />
    <c:set var="passText" value="Validation Passed" />
    <c:set var="failText" value="Validation Failed" />
    <c:set var="errorText" value="Validation Error" />
    <c:set var="unknownText" value="Unknown Validation Status" />

    <div class="aca-data-table">
      <table id="deviceTable" class="display" width="100%">
        <thead>
          <tr>
            <th rowspan="2">Validation Status</th>
            <th rowspan="2">Hostname</th>
            <th colspan="3">Credentials</th>
          </tr>
          <tr>
            <th>Issued Attestation</th>
            <th>Platform</th>
            <th>Endorsement</th>
          </tr>
        </thead>
      </table>
    </div>
    <script>
      $(document).ready(function () {
        let url = portal + "/devices/list";
        let columns = [
          {
            data: "supplyChainValidationStatus",
            searchable: false,
            render: function (data, type, full, meta) {
              let html = "";
              switch (full.device.supplyChainValidationStatus) {
                case "PASS":
                  html = '<img src="${passIcon}" title="${passText}">';
                  break;
                case "FAIL":
                  html = '<img src="${failIcon}" title="${failText}"/>';
                  break;
                case "ERROR":
                  html = '<img src="${errorIcon}" title="${errorText}">';
                  break;
                default:
                  html = '<img src="${unknownIcon}" title="${unknownText}">';
                  break;
              }
              return html;
            },
          },
          {
            name: "name",
            data: "name",
            searchable: true,
            orderable: true,
            render: function (data, type, full, meta) {
              return full.device.name;
            },
          },
          {
            data: "id",
            orderable: false,
            searchable: false,
            render: function (data, type, full, meta) {
              //Display issued attestation certificate
              if (full.IssuedAttestationCertificate === undefined) return "";
              let size = full.IssuedAttestationCertificate.length;
              let html = "";

              for (let i = 0; i < size; i++) {
                let id = full.IssuedAttestationCertificate[i].id;
                html += certificateDetailsLink("issued", id, false);
              }

              return html;
            },
          },
          {
            data: "id",
            orderable: false,
            searchable: false,
            render: function (data, type, full, meta) {
              //Display platform credential
              if (full.PlatformCredential === undefined) return "";
              let size = full.PlatformCredential.length;
              let html = "";

              for (let i = 0; i < size; i++) {
                let id = full.PlatformCredential[i].id;
                html +=
                  certificateDetailsLink("platform", id, false) + "&nbsp;";
              }

              return html;
            },
          },
          {
            data: "id",
            orderable: false,
            searchable: false,
            render: function (data, type, full, meta) {
              //Display endorsement credential
              if (full.EndorsementCredential === undefined) return "";
              let size = full.EndorsementCredential.length;
              let html = "";

              for (let i = 0; i < size; i++) {
                let id = full.EndorsementCredential[i].id;
                html +=
                  certificateDetailsLink("endorsement", id, false) + "&nbsp;";
              }

              return html;
            },
          },
        ];

        //Set data tables
        setDataTables("#deviceTable", url, columns);
      });
    </script>
  </jsp:body>
</my:page>
