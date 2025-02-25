<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>
<my:page>

    <jsp:attribute name="script">
        <script type="text/javascript" src="${lib}/jquery.spring-friendly/jquery.spring-friendly.js"></script>
    </jsp:attribute>
    <jsp:attribute name="pageHeaderTitle">Validation Reports</jsp:attribute>

    <jsp:body>
        <!-- text and icon resource variables -->
        <c:set var="passIcon" value="${icons}/ic_checkbox_marked_circle_black_green_24dp.png"/>
        <c:set var="failIcon" value="${icons}/ic_error_red_24dp.png"/>
        <c:set var="errorIcon" value="${icons}/ic_error_black_24dp.png"/>
        <c:set var="unknownIcon" value="${icons}/ic_questionmark_circle_orange_24dp.png"/>
        <c:set var="passText" value="Validation Passed"/>
        <c:set var="failText" value="Validation Failed"/>
        <c:set var="errorText" value="Validation Error"/>
        <c:set var="unknownText" value="Unknown Validation Status"/>

        <form:form id="download" method="POST" action="${portal}/validation-reports/download">
            Download Validation Reports
            <my:download-info id="validationReportsDownload" label="Download Validation Reports">
                <label>Company<input id="company" type="text" pattern="^\w*$"
                        title="Letters, numbers, and spaces only" name="company" /></label>
                <label>Contract #<input id="contract" type="text" pattern="^\w*$"
                        title="Letters, numbers, and spaces only" name="contract" /></label>
                <br>
                <label>Date range start<input id="dateStart" type="date" name="dateStart" /></label>
                <label>Date range end<input id="dateEnd" type="date" name="dateEnd" /></label>
            </my:download-info>
        </form:form>

        <div class="aca-data-table">
            <table id="reportTable" class="display" width="100%">
                <thead>
                    <tr>
                        <th rowspan="2">Result</th>
                        <th rowspan="2">Timestamp</th>
                        <th rowspan="2">Device</th>
                        <th colspan="3">Credential Validations</th>
                    </tr>
                    <tr>
                        <th style="text-align:center">Endorsement</th>
                        <th style="text-align:center">Platform</th>
                        <th style="text-align:center">Firmware</th>
                    </tr>
                </thead>
            </table>
        </div>
        <script>
            $(document).ready(function () {
                let url = portal + '/validation-reports/list';
                let columns = [
                    {
                        data: 'overallValidationResult',
                        searchable: false,
                        render: function (data, type, full, meta) {
                            let html = '';
                            let unknownStatus = '<img class="icon" src="${unknownIcon}" title="${unknownText}"/>';

                            // create status icon
                            let result = full.overallValidationResult;
                            let overallMessage = full.message;
                            if (result) {
                                switch (result) {
                                    case "PASS":
                                        html += '<img src="${passIcon}" title="${passText}"/>';
                                        break;
                                    case "FAIL":
                                        html += '<img src="${failIcon}" title="' + overallMessage + '"/>';
                                        break;
                                    case "ERROR":
                                        html += '<img src="${errorIcon}" title="' + overallMessage + '"/>';
                                        break;
                                    default:
                                        html += unknownStatus;
                                        break;
                                }
                            } else {
                                html += unknownStatus;
                            }

                            return html;
                        }
                    },
                    {
                        // Note: DB column is create_time, while the
                        // JSON property / java property is createTime. Need to sort
                        // on the field createTime, but the column's
                        // date source is create_time.
                        data: 'create_time',
                        name: 'createTime',
                        searchable: false,
                        render: function (data, type, full, meta) {
                            return formatCertificateDate(full.createTime);
                        }
                    },
                    {
                        // TODO render a link to a device details page,
                        // passing the device.id
                        data: 'device.name'
                    },
                    {
                        data: 'id',
                        searchable: false,
                        orderable: false,
                        render: function (data, type, full, meta) {
                            return getValidationDisplayHtml(full, "ENDORSEMENT_CREDENTIAL")
                        }
                    },
                    {
                        data: 'id',
                        searchable: false,
                        orderable: false,
                        render: function (data, type, full, meta) {
                            return getValidationDisplayHtml(full, "PLATFORM_CREDENTIAL")
                        }
                    },
                    {
                        data: 'id',
                        searchable: false,
                        orderable: false,
                        render: function (data, type, full, meta) {
                            return getValidationDisplayHtml(full, "FIRMWARE")
                        }
                    }
                ];

                //Set data tables
                let dataTable = setDataTables("#reportTable", url, columns);
                dataTable.order([1, 'desc']).draw();    //order by createTime
            });

            $("#download").submit(function(e) {
                let tableLength = $("#reportTable").rows;
                let createTimes = "";
                let deviceNames = "";
                $('#reportTable tr').not('thead tr').each(function() {
                    createTimes += $(this).find("td").eq(1).html() + ",";
                    deviceNames += $(this).find("td").eq(2).html() + ",";
                });
                createTimes = createTimes.substring(0, createTimes.length - 1);
                deviceNames = deviceNames.substring(0, deviceNames.length - 1);
                let params = [
                                {
                                    name: 'createTimes',
                                    value: createTimes
                                },
                                {
                                    name: 'deviceNames',
                                    value: deviceNames
                                }
                            ];
                $(this).append($.map(params, function(param) {
                    return $('<input>', {
                        type: 'hidden',
                        name: param.name,
                        value: param.value
                    });
                }));
            });

            $(".btn-primary").click(function() {
                $("#validationReportsDownload").modal('hide');
            });

            /**
             * Gets HTML to display (icon tag) for the specified validation type.
             * If a validation for the requested type is not found, an empty
             * string is returned (and no icon will be displayed).
             */
            function getValidationDisplayHtml(full, validation_type) {
                let html = '';
                // loop through all the validations, looking for the one matching
                // the validation_type.
                for (let i = 0; i < full.validations.length; i++) {
                    let curValidation = full.validations[i];
                    let curResult = curValidation.validationResult;
                    let curMessage = curValidation.message;

                    if (curValidation.validationType === validation_type) {
                        let unknownStatus = '<img class="icon" src="${unknownIcon}" title="${unknownText}"/>';

                        // display appropriate icon based on result
                        if (curResult) {

                            // if this validation is associated with a certificate,
                            // link to the details page
                            if (curValidation.certificatesUsed.length > 0) {
                                let certType = '';
                                switch (validation_type) {
                                    case "PLATFORM_CREDENTIAL":
                                    case "PLATFORM_CREDENTIAL_ATTRIBUTES":
                                        certType = "platform";
                                        break;
                                    case "ENDORSEMENT_CREDENTIAL":
                                        certType = "endorsement";
                                        break;
                                }

                                switch (validation_type) {
                                    case "PLATFORM_CREDENTIAL":
                                    case "PLATFORM_CREDENTIAL_ATTRIBUTES":
                                    case "ENDORSEMENT_CREDENTIAL":
                                        html += '<a href="${portal}/certificate-details?id='
                                                + curValidation.certificatesUsed[0].id
                                                + '&type=' + certType + '">';
                                        break;
                                }
                            }

                            switch (validation_type) {
                                case "FIRMWARE":
                                    html += '<a href="${portal}/rim-details?id='
                                            + curValidation.rimId + '">';
                                    break;
                            }

                            switch (curResult) {
                                case "PASS":
                                    html += '<img src="${passIcon}" title="' + curMessage + '"/>';
                                    break;
                                case "FAIL":
                                    html += '<img src="${failIcon}" title="' + curMessage + '"/>';
                                    break;
                                case "ERROR":
                                    html += '<img src="${errorIcon}" title="' + curMessage + '"/>';
                                    break;
                                default:
                                    html += unknownStatus;
                                    break;
                            }

                            // add closing tag for href tag if needed.
                            if (curValidation.certificatesUsed.length > 0 || curValidation.rimId !== "") {
                                html += '</a>';
                            }
                        } else {
                            html += unknownStatus;
                        }
                    }
                }
                return html;
            }
        </script>
    </jsp:body>

</my:page>