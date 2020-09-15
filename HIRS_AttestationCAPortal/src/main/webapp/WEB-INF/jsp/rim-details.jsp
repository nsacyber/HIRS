<%@ page contentType="text/html"%>
<%@ page pageEncoding="UTF-8"%><%-- JSP TAGS--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="fn" uri = "http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%><%--CONTENT--%>
<my:page>
    <jsp:attribute name="style">
        <link type="text/css" rel="stylesheet" href="${common}/certificate_details.css"/>
        <link type="text/css" rel="stylesheet" href="${common}/rim_details.css"/>
    </jsp:attribute>
    <jsp:attribute name="pageHeaderTitle">
        ${initialData.rimType} Reference Integrity Manifest
        <a href="${portal}/reference-manifests/download?id=${param.id}">
            <img src="${icons}/ic_file_download_black_24dp.png" title="Download ${initialData.rimType} RIM">
        </a>
    </jsp:attribute>
    <jsp:body>
        <div id="certificate-details-page" class="container-fluid">
            <c:choose>
                <c:when test="${initialData.rimType=='Support'}">
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Base RIM</span></div>
                        <div id="baseRim" class="col col-md-8">
                            <c:choose>
                                <c:when test="${not empty initialData.associatedRim}">
                                    <a href="${portal}/rim-details?id=${initialData.associatedRim}">
                                        ${initialData.tagId}
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    <div class="component col col-md-10" style="color: red; padding-left: 20px">Base RIM not uploaded from the ACA RIM Page</div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <div id="tableDivTag">
                        <input type="text" id="eventInput" onkeyup="eventSearch()" placeholder="Search for text..." /><br />                        
                        <table id="eventLog">
                            <thead>
                                <tr class="header">
                                    <th>Event #</th>
                                    <th>PCR Index</th>
                                    <th style="width: 20%">Event Type</th>
                                    <th>Digest</th>
                                    <th style="width: 50%">Event Content</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:if test="${not empty initialData.events}">
                                    <c:set var="count" value="1" scope="page"/>
                                    <c:forEach items="${initialData.events}" var="event">
                                        <tr>
                                            <td style="width: 75px">${count}</td>
                                            <td class="pcrCell">PCR${event.getPcrIndex()}</td>
                                            <td>${event.getEventTypeStr()}</td>
                                            <td class="digestCell">${event.getEventDigestStr()}</td>
                                            <td title="${event.getEventContentStr()}"><div style="height: 50px; overflow: auto">${event.getEventContentStr()}</div></td>
                                        </tr>
                                        <c:set var="count" value="${count + 1}" scope="page"/>
                                    </c:forEach>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                    <div class="col-md-a col-md-offset-1"><span class="colHeader">${initialData.events.size()} entries</span></div>
                </c:when>
                <c:otherwise>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Software Identity</span></div>
                        <div id="softwareIdentity" class="col col-md-8">
                            <div>SWID Name:&nbsp;<span>${initialData.swidName}</span></div>
                            <div>SWID Version:&nbsp;<span>${initialData.swidVersion}</span></div>
                            <div>SWID Tag ID:&nbsp;<span>${initialData.swidTagId}</span></div>
                            <div>SWID Tag Version:&nbsp;<span>${initialData.swidTagVersion}</span></div>
                            <c:if test="${initialData.swidCorpus}">
                                <div>SWID Corpus:&nbsp;<span><img src="${icons}/ic_checkbox_marked_circle_black_green_24dp.png" title="Corpus Flag"></span>
                                </div>
                            </c:if>
                            <c:if test="${initialData.swidPatch}">
                                <div>SWID Patch:&nbsp;<span><img src="${icons}/ic_checkbox_marked_circle_black_green_24dp.png" title="Patch Flag"></span>
                                </div>
                            </c:if>
                            <c:if test="${initialData.swidSupplemental}">
                                <div>SWID Supplemental:&nbsp;<span><img src="${icons}/ic_checkbox_marked_circle_black_green_24dp.png" title="Supplemental Flag"></span>
                                </div>
                            </c:if>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Entity</span></div>
                        <div id="entity" class="col col-md-8">                        
                            <div>Entity Name:&nbsp;<span>${initialData.entityName}</span></div>
                            <c:if test="${not empty initialData.entityRegId}">
                                <div>Entity Reg ID:&nbsp;<span>${initialData.entityRegId}</span></div>
                            </c:if>
                            <div>Entity Role:&nbsp;<span>${initialData.entityRole}</span></div>
                            <div>Entity Thumbprint:&nbsp;<span>${initialData.entityThumbprint}</span></div>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Link</span></div>
                        <div id="link" class="col col-md-8">
                            <c:if test="${not empty initialData.linkHref}">
                                <div><span><a href="${initialData.linkHref}" rel="${initialData.linkRel}">${initialData.linkHref}</a></span>
                                </div>
                                <div>Rel:&nbsp;<span>${initialData.linkRel}</span>
                                </div>
                            </c:if>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Meta</span></div>
                        <div id="link" class="col col-md-8">
                            <div>Platform Manufacturer ID:&nbsp;<span>${initialData.platformManufacturerId}</span></div>
                            <div>Platform Manufacturer:&nbsp;<span>${initialData.platformManufacturer}</span></div>
                            <div>Platform Model:&nbsp;<span>${initialData.platformModel}</span></div>
                            <c:if test="${not empty initialData.platformVersion}">
                                <div>Platform Version:&nbsp;<span>${initialData.platformVersion}</span></div>
                            </c:if>
                            <div>Colloquial Version:&nbsp;<span>${initialData.colloquialVersion}</span></div>
                            <div>Edition:&nbsp;<span>${initialData.edition}</span></div>
                            <div>Product:&nbsp;<span>${initialData.product}</span></div>
                            <div>Revision:&nbsp;<span>${initialData.revision}</span></div>

                            <c:if test="${not empty initialData.payloadType}">
                                <div>Payload Type:&nbsp;<span>${initialData.payloadType}</span></div>                        
                            </c:if>
                            <div>Binding Spec:&nbsp;<span>${initialData.bindingSpec}</span></div>
                            <div>Binding Spec Version:&nbsp;<span>${initialData.bindingSpecVersion}</span></div>
                            <c:if test="${not empty initiaData.pcUriGlobal}">
                                <div>PC URI Global:&nbsp;<span>${initialData.pcUriGlobal}</span></div>
                            </c:if>
                            <c:if test="${not empty initiaData.pcUriLocal}">
                                <div>PC URI Local:&nbsp;<span>${initialData.pcUriLocal}</span></div>
                            </c:if>
                            <div>Rim Link Hash:&nbsp;<span>${initialData.rimLinkHash}</span></div>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Payload/Support RIM(s)</span></div>
                        <div id="platformConfiguration" class="col col-md-8">                    
                            <div class="panel panel-default">
                                <div class="panel-heading" role="tab" id="headingOne">
                                    <h4 class="panel-title">
                                        <a role="button" data-toggle="collapse" data-parent="#platformConfiguration" class="collapsed"
                                           href="#directorycollapse" aria-expanded="true" aria-controls="directorycollapse">
                                            Directory
                                        </a>
                                    </h4>
                                </div>
                                <div id="directorycollapse" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="headingOne" aria-expanded="true">
                                    <div class="panel-body">
                                        <div class="panel-heading" role="tab" id="headingThree">
                                            <h3 class="panel-title">
                                                <a role="button" data-toggle="collapse" data-parent="#directorycollapse" class="collapsed"
                                                   href="#filescollapse" aria-expanded="false" aria-controls="filescollapse">
                                                    Files
                                                </a>
                                            </h3>
                                        </div>
                                        <div id="filescollapse" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingThree" aria-expanded="true">
                                            <c:if test="${not empty initialData.swidFiles}">
                                                <div id="componentIdentifier" class="row">
                                                    <c:forEach items="${initialData.swidFiles}" var="resource">
                                                        <div class="component col col-md-10" style="padding-left: 20px">
                                                            <div class="panel panel-default">     
                                                                <div class="panel-heading">
                                                                    <span data-toggle="tooltip" data-placement="top" title="Resource File">
                                                                        <c:choose>
                                                                            <c:when test="${not empty initialData.associatedRim}">
                                                                                <a href="${portal}/rim-details?id=${initialData.associatedRim}">${resource.getName()}</a>
                                                                            </c:when>
                                                                            <c:otherwise>
                                                                                ${resource.getName()}
                                                                            </c:otherwise>
                                                                        </c:choose>
                                                                    </span>
                                                                </div>
                                                                <c:choose>
                                                                    <c:when test="${not empty resource.getPcrValues()}">
                                                                        <div class="component col col-md-10">
                                                                            <span class="fieldHeader">File Size:</span>
                                                                            <span class="fieldValue">${resource.getSize()}</span><br/>
                                                                            <span class="fieldHeader">Hash:</span>
                                                                            <span class="fieldValue" style="overflow-wrap: break-word">${resource.getHashValue()}</span><br/>
                                                                            <c:if test="${not empty resource.getRimFormat()}">
                                                                                <span class="fieldHeader">RIM Format:</span>
                                                                                <span class="fieldValue">${resource.getRimFormat()}</span><br/>
                                                                            </c:if>
                                                                            <c:if test="${not empty resource.getRimType()}">
                                                                                <span class="fieldHeader">RIM Type:</span>
                                                                                <span class="fieldValue">${resource.getRimType()}</span><br/>
                                                                            </c:if>
                                                                            <c:if test="${not empty resource.getRimUriGlobal()}">
                                                                                <span class="fieldHeader">URI Global:</span>
                                                                                <span class="fieldValue">${resource.getRimUriGlobal()}</span><br/>
                                                                            </c:if>
                                                                            <c:if test="${not empty resource.getPcrValues()}"> 
                                                                                <div class="panel-body">
                                                                                    <div class="component" role="tab" id="pcrValues">
                                                                                        <a role="button" data-toggle="collapse" data-parent="#directorycollapse" class="collapsed"
                                                                                           href="#pcrscollapse" aria-expanded="false" aria-controls="pcrscollapse">
                                                                                            Expected PCR Values
                                                                                        </a>                                                         
                                                                                    </div>
                                                                                    <div id="pcrscollapse" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingThree" aria-expanded="true">
                                                                                        <div>
                                                                                            <c:forEach items="${resource.getPcrMap()}" var="pcrValue">
                                                                                                <div id="componentIdentifier" class="row">
                                                                                                    <div>                                                                                    
                                                                                                        <span>${pcrValue.key}</span>
                                                                                                        <span style="overflow-wrap: break-word">${pcrValue.value}</span>
                                                                                                    </div>
                                                                                                </div>
                                                                                            </c:forEach>
                                                                                        </div>
                                                                                    </div>
                                                                                </div>
                                                                            </c:if>
                                                                        </div>
                                                                    </c:when>
                                                                    <c:otherwise>
                                                                        <div class="component col col-md-10" style="color: red; padding-left: 20px">Support RIM file named ${resource.getName()} was not imported via the Reference Integrity Manifest page.</div>
                                                                    </c:otherwise>
                                                                </c:choose>
                                                            </div>                                                    
                                                        </div>
                                                    </c:forEach>
                                                </div>                                    
                                            </c:if>
                                        </div>                                

                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
        <div class="row">
            <div class="col-md-1 col-md-offset-1"><span class="colHeader">Signature</span></div>
            <div id="signature" class="col col-md-8">
                <div>Validity:&nbsp;<span>${initialData.signatureValid}</span></div>
                <div>Signer:&nbsp;<span>Link to signing certificate</span></div>
            </div>
        </div>
    </div>
</div>
<script>
    function eventSearch() {
        // Declare variables
        var input, filter, table, tr, td, i, txtValue, txtFound;
        input = document.getElementById("eventInput");
        filter = input.value.toUpperCase();
        table = document.getElementById("eventLog");
        tr = table.getElementsByTagName("tr");

        // Loop through all table rows, and hide those who don't match the search query
        for (i = 0; i < tr.length; i++) {
            txtFound = false;
            tds = tr[i].getElementsByTagName("td");
            for (j = 0; j < tds.length; j++) {
                td = tds[j];

                if (td) {
                    txtValue = td.textContent || td.innerText;
                    if (txtValue.toUpperCase().indexOf(filter) > -1) {
                        txtFound = true;
                        break;
                    } else {
                        txtFound = false;
                    }
                }
            }
            if (txtFound) {
                tr[i].style.display = "";
            } else {
                tr[i].style.display = "none";
            }
        }
    }
    window.onload = function() {
        // Constant retrieved from server-side via JSP
        var maxRows = 11;

        var table = document.getElementById('eventLog');
        var wrapper = table.parentNode;
        var rowsInTable = table.rows.length;
        var height = 0;
        if (rowsInTable > maxRows) {
            for (var i = 0; i < maxRows; i++) {
                height += table.rows[i].clientHeight;
            }
            wrapper.style.height = height + "px";
        }
    }
</script>
</jsp:body>
</my:page>
