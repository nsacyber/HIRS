<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>
<my:page>
    <jsp:attribute name="script">
        <script type="text/javascript" src="${lib}/jquery.spring-friendly/jquery.spring-friendly.js"></script>
    </jsp:attribute>
    <jsp:attribute name="pageHeaderTitle">Trust Chain Management</jsp:attribute>

    <jsp:body>
        <span class="aca-input-box-header">
            HIRS Attestation CA Certificate
        </span>
        <my:details-viewer id="aca-cert-viewer" label="HIRS Attestation CA Certificate">
            <div class="container-fluid">
                <div class="row">
                    <div class="col-md-2 col-md-offset-1"><span class="colHeader">Issuer</span></div>
                    <div id="issuer" class="col col-md-8">
                        <!-- Display the issuer, and provide a link to the issuer details if provided -->
                        <c:choose>
                            <c:when test="${not empty acaCertData.issuerID}">
                                <a href="${portal}/certificate-details?id=${acaCertData.issuerID}&type=certificateauthority">
                                    ${acaCertData.issuer}
                                </a>
                            </c:when>
                            <c:otherwise>
                                ${acaCertData.issuer}
                              </c:otherwise>
                        </c:choose>
                    </div>
                </div>
                <c:if test="${not empty acaCertData.subject}">
                    <div class="row">
                        <div class="col-md-2 col-md-offset-1"><span class="colHeader">Subject</span></div>
                        <div id="subject" class="col col-md-8">${acaCertData.subject}</div>
                    </div>
                </c:if>
                <div class="row">
                    <div class="col-md-2 col-md-offset-1"><span class="colHeader">Serial Number</span></div>
                    <div id="serialNumber" class="col col-md-8">${acaCertData.serialNumber}</div>
                </div>
                <div class="row">
                    <div class="col-md-2 col-md-offset-1"><span class="colHeader">Validity</span></div>
                    <div id="validity" class="col col-md-8">
                        <div>Not Before:&nbsp;<span>${acaCertData.beginValidity}</span></div>
                        <div>Not After:&nbsp;<span>${acaCertData.endValidity}</span></div>
                    </div>
                </div>
                <div class="row">
                    <div class="col-md-2 col-md-offset-1"><span class="colHeader">Signature</span></div>
                    <div id="signature" class="col col-md-8"></div>
                </div>
                <c:if test="${not empty acaCertData.encodedPublicKey}">
                    <div class="row">
                        <div class="col-md-2 col-md-offset-1"><span class="colHeader">Public Key</span></div>
                        <div id="encodedPublicKey" class="col col-md-8"></div>
                    </div>
                </c:if>
                <div class="spacer"></div>
            </div>
        </my:details-viewer>

        <a href="${portal}/certificate-request/trust-chain/download-aca-cert">
            <img src="${baseURL}/images/icons/ic_file_download_black_24dp.png" title="Download ACA Certificate">
        </a>
        <div class="aca-input-box-header">
            <form:form method="POST" action="${portal}/certificate-request/trust-chain/upload" enctype="multipart/form-data">
                Trust Chain CA Certificates
                    <my:file-chooser id="tc-editor" label="Import Trust Chain Certificates">
                        <input id="importFile" type="file" name="file" multiple="multiple" />
                    </my:file-chooser>
                <a href="${portal}/certificate-request/trust-chain/bulk">
                    <img src="${icons}/ic_file_download_black_24dp.png" title="Download All Trust Chain Certificates">
                </a>
            </form:form>
        </div>
        <br/>
        <div class="aca-data-table">
            <table id="trustChainTable" class="display" width="100%">
                <thead>
                    <tr>
                        <th>Issuer</th>
                        <th>Subject</th>
                        <th>Valid (begin)</th>
                        <th>Valid (end)</th>
                        <th>Options</th>
                    </tr>
                </thead>
            </table>
        </div>
        <script>
            $(document).ready(function() {
                var url = pagePath +'/list';
                var signature = ${acaCertData.signature};

                //Format validity time
                $("#validity span").each(function(){
                    $(this).text(formatDateTime($(this).text()));
                });

                //Convert byte array to string
                $("#signature").html(byteToHexString(signature));

                <c:if test="${not empty acaCertData.encodedPublicKey}">
                    //Change publick key byte to hex
                    var publicKey = ${acaCertData.encodedPublicKey};
                    $("#encodedPublicKey").html(byteToHexString(publicKey));
                </c:if>

                var columns = [
                        {data: 'issuer'},
                        {data: 'subject'},
                        {
                            data: 'beginValidity',
                            searchable:false,
                            render: function (data, type, full, meta) {
                                return formatDateTime(full.beginValidity);
                            }
                        },
                        {
                            data: 'endValidity',
                            searchable:false,
                            render: function (data, type, full, meta) {
                                return formatDateTime(full.endValidity);
                            }
                        },
                        {
                            data: 'id',
                            orderable: false,
                            searchable:false,
                            render: function(data, type, full, meta) {
                                // Set up a delete icon with link to handleDeleteRequest().
                                // sets up a hidden input field containing the ID which is
                                // used as a parameter to the REST POST call to delete
                                var html = '';
                                html += certificateDetailsLink('certificateauthority', full.id, true);
                                html += certificateDownloadLink(full.id, pagePath);
                                html += certificateDeleteLink(full.id, pagePath);
                                return html;
                            }
                        }
                    ];
                //Set data tables
                setDataTables("#trustChainTable", url, columns);
            });
        </script>
    </jsp:body>

</my:page>
