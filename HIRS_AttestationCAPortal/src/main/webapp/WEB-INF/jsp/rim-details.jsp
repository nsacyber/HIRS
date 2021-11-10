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
    <c:choose>
        <c:when test="${initialData.rimType=='Measurement'}">
            TCG Event Log
            <a href="${portal}/reference-manifests/download?id=${param.id}">
                <img src="${icons}/ic_file_download_black_24dp.png" title="Download ${initialData.rimType} RIM">
            </a>
        </c:when>
        <c:otherwise>
            ${initialData.rimType} Reference Integrity Manifest
            <a href="${portal}/reference-manifests/download?id=${param.id}">
                <img src="${icons}/ic_file_download_black_24dp.png" title="Download ${initialData.rimType} RIM">
            </a>
        </c:otherwise>
    </c:choose>
    </jsp:attribute>
    <jsp:body>
        <c:set var="passIcon" value="${icons}/ic_checkbox_marked_circle_black_green_24dp.png"/>
        <c:set var="failIcon" value="${icons}/ic_error_red_24dp.png"/>
        <c:set var="signatureValidText" value="Signature valid!"/>
        <c:set var="signatureInvalidText" value="Signature not valid!"/>
        <c:set var="supportRimHashValidText" value="Support RIM hash valid!"/>
        <c:set var="supportRimHashInvalidText" value="Support RIM hash not valid!"/>
        <div id="certificate-details-page" class="container-fluid">
            <c:choose>
                <c:when test="${initialData.rimType=='Support' || (initialData.rimType=='Measurement' && initialData.validationResult=='PASS')}">
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Additional<br />RIM Info</span></div>
                        <div id="baseRim" class="col col-md-8">
                            <c:choose>
                                <c:when test="${not empty initialData.associatedRim}">
                                    <a href="${portal}/rim-details?id=${initialData.associatedRim}">
                                        ${initialData.tagId}
                                    </a>
                            <c:if test="${not empty initialData.hostName}">
                                <div>Device:&nbsp;<span>${initialData.hostName}</span></div>
                            </c:if>
                            <c:if test="${not empty initialData.supportId}">
                                <div>Support:&nbsp;<span><a href="${portal}/rim-details?id=${initialData.supportId}">${initialData.supportFilename}</a></span>
                                </div>
                            </c:if>
                                </c:when>
                                <c:otherwise>
                                    <div class="component col col-md-10" style="color: red; padding-left: 20px">RIM not uploaded from the ACA RIM Page</div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <c:if test="${not initialData.swidBase}">
                        <div class="row">
                            <div class="col-md-1 col-md-offset-1">
                                <span class="colHeader">RIM Type</span>
                            </div>
                            <div id="baseRim" class="col col-md-8">
                                <c:if test="${initialData.swidCorpus}">
                                    <div>SWID Corpus</div>
                                </c:if>
                                <c:if test="${initialData.swidPatch}">
                                    <div>SWID Patch</div>
                                </c:if>
                                <c:if test="${initialData.swidSupplemental}">
                                    <div>SWID Supplemental</div>
                                </c:if>
                            </div>
                        </div>
                    </c:if>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1">
                            <span class="colRimHeader">
                                <a role="button" data-toggle="collapse" class="collapsed" href="#eventOptions"
                                   aria-expanded="true" data-placement="top" aria-controls="eventOptions">
                                    Event Summary
                                </a>
                            </span>
                        </div>
                        <div id="eventsCol" class="col col-md-8">
                            <div id="eventOptions" class="collapse" class="collapsed" aria-expanded="false">
                                <ul>
                                <c:choose>
                                    <c:when test="${initialData.rimType=='Support'}">
                                        <li>This Support RIM file covers the following critical items:</li>
                                    </c:when>
                                    <c:otherwise>
                                        <li>This Event Log file covers the following critical items:</li>
                                    </c:otherwise>
                                </c:choose>
                                    <ul>
                                        <c:if test="${initialData.crtm || initialData.bootManager || initialData.osLoader || initialData.osKernel}">
                                            <li>PC Client Boot path</li>
                                        </c:if>
                                        <ul>
                                            <c:if test="${initialData.crtm}">
                                                <li>Software Core Root of Trust for Measurement (SRTM)</li>
                                                </c:if>
                                                <c:if test="${initialData.bootManager}">
                                                <li>Boot Manager</li>
                                                </c:if>
                                                <c:if test="${initialData.osLoader}">
                                                <li>OS Loader</li>
                                                </c:if>
                                                <c:if test="${initialData.osKernel}">
                                                <li>OS Kernel</li>
                                            </c:if>
                                        </ul>
                                        <c:if test="${initialData.acpiTables || initialData.smbiosTables || initialData.gptTable || initialData.defaultBootDevice}">
                                            <li>Device Configuration</li>
                                            </c:if>
                                        <ul>
                                            <c:if test="${initialData.acpiTables}">
                                                <li>ACPI Tables</li>
                                                </c:if>
                                                <c:if test="${initialData.smbiosTables}">
                                                <li>SMBIOS Tables</li>
                                                </c:if>
                                                <c:if test="${initialData.gptTable}">
                                                <li>GPT Table</li>
                                                </c:if>
                                                <c:if test="${initialData.bootOrder}">
                                                <li>Boot Order</li>
                                                </c:if>
                                                <c:if test="${initialData.defaultBootDevice}">
                                                <li>Default boot device</li>
                                            </c:if>
                                        </ul>
                                        <c:if test="${initialData.secureBoot || initialData.pk || initialData.kek || initialData.sigDb || initialData.forbiddenDbx}">
                                            <li>Secure Boot Variables</li>
                                            </c:if>
                                        <ul>
                                            <c:if test="${initialData.secureBoot}">
                                                <li>Secure Boot Enabled</li>
                                                </c:if>
                                                <c:if test="${initialData.pk}">
                                                <li>Platform Key (PK)</li>
                                                </c:if>
                                                <c:if test="${initialData.kek}">
                                                <li>Key Exchange Key (KEK)</li>
                                                </c:if>
                                                <c:if test="${initialData.sigDb}">
                                                <li>Signature Database (db)</li>
                                                </c:if>
                                                <c:if test="${initialData.forbiddenDbx}">
                                                <li>Forbidden Signatures Database (dbx)</li>
                                            </c:if>
                                        </ul>
                                    </ul>
                                </ul>
                                <ul>
                                <c:choose>
                                    <c:when test="${initialData.rimType=='Support'}">
                                        <li>This Support RIM file covers the following critical items:</li>
                                    </c:when>
                                    <c:otherwise>
                                        <li>This Event Log file covers the following critical items:</li>
                                    </c:otherwise>
                                </c:choose>
                                    <ul>
                                        <c:if test="${not initialData.crtm || not initialData.bootManager || not initialData.osLoader || not initialData.osKernel}">
                                            <li>PC Client Boot path</li>
                                            </c:if>
                                        <ul>
                                            <c:if test="${not initialData.crtm}">
                                                <li>Software Core Root of Trust for Measurement (SRTM)</li>
                                                </c:if>
                                                <c:if test="${not initialData.bootManager}">
                                                <li>Boot Manager</li>
                                                </c:if>
                                                <c:if test="${not initialData.osLoader}">
                                                <li>OS Loader</li>
                                                </c:if>
                                                <c:if test="${not initialData.osKernel}">
                                                <li>OS Kernel</li>
                                                </c:if>
                                        </ul>
                                        <c:if test="${not initialData.acpiTables || not initialData.smbiosTables || not initialData.gptTable || not initialData.bootOrder || not initialData.defaultBootDevice}">
                                            <li>Device Configuration</li>
                                            </c:if>
                                        <ul>
                                            <c:if test="${not initialData.acpiTables}">
                                                <li>ACPI Tables</li>
                                                </c:if>
                                                <c:if test="${not initialData.smbiosTables}">
                                                <li>SMBIOS Tables</li>
                                                </c:if>
                                                <c:if test="${not initialData.gptTable}">
                                                <li>GPT Table</li>
                                                </c:if>
                                                <c:if test="${not initialData.bootOrder}">
                                                <li>Boot Order</li>
                                                </c:if>
                                                <c:if test="${not initialData.defaultBootDevice}">
                                                <li>Default boot device</li>
                                                </c:if>
                                        </ul>
                                        <c:if test="${not initialData.secureBoot || not initialData.pk || not initialData.kek || not initialData.sigDb || not initialData.forbiddenDbx}">
                                            <li>Secure Boot Variables</li>
                                            </c:if>
                                        <ul>
                                            <c:if test="${not initialData.secureBoot}">
                                                <li>Secure Boot Enabled</li>
                                                </c:if>
                                                <c:if test="${not initialData.pk}">
                                                <li>Platform Key (PK)</li>
                                                </c:if>
                                                <c:if test="${not initialData.kek}">
                                                <li>Key Exchange Key (KEK)</li>
                                                </c:if>
                                                <c:if test="${not initialData.sigDb}">
                                                <li>Signature Database (db)</li>
                                                </c:if>
                                                <c:if test="${not initialData.forbiddenDbx}">
                                                <li>Forbidden Signatures Database (dbx)</li>
                                            </c:if>
                                        </ul>
                                    </ul>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="tableDivTag">
                    <input type="text" id="eventInput" onkeyup="eventSearch(null)" placeholder="Search for text..." /><br />
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
                                    <c:choose>
                                        <c:when test="${event.isError()}">
                                            <tr style="background: tomato">
                                        </c:when>
                                        <c:otherwise>
                                            <tr>
                                        </c:otherwise>
                                    </c:choose>
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
            <c:when test="${initialData.rimType=='Measurement' && initialData.validationResult=='FAIL'}">
                <div style="display: inline">
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Base/Support</span></div>
                        <div id="measurements" class="col col-md-8">
                            <c:if test="${not empty initialData.hostName}">
                                <div>Device:&nbsp;<span>${initialData.hostName}</span>
                                </div>
                            </c:if>
                            <c:if test="${not empty initialData.tagId}">
                                <div>Base:&nbsp;<span><a href="${portal}/rim-details?id=${initialData.associatedRim}">${initialData.tagId}</a></span>
                                </div>
                            </c:if>
                            <c:if test="${not empty initialData.supportId}">
                                <div>Support:&nbsp;<span><a href="${portal}/rim-details?id=${initialData.supportId}">${initialData.supportFilename}</a></span>
                                </div>
                            </c:if>
                        </div>
                    </div>
                    <br />
                    <div class="row" style="margin: auto 260px auto 125px">
                        <div class="panel panel-default" style="flex: 1">
                            <div class="panel-heading">Client Log</div>
                            <c:if test="${not empty initialData.livelogEvents}">
                                <c:set var="iterator" value="0" scope="page"/>
                                <c:forEach items="${initialData.livelogEvents}" var="lEvent">
                                    <div>
                                        <div style="display: flex; background: lightgray;">
                                            <div style="display: flex 1; font-weight: bold; margin: auto 1rem auto 1rem">Failed Event Digest:<br />
                                            </div>
                                            <div style="display: flex 2; margin: 2px auto 2px 25px">
                                                <span class="mappedData">PCR Index:</span> ${lEvent.getPcrIndex()}<br />
                                                <span class="mappedData">Digest:</span> ${lEvent.getEventDigestStr()}<br />
                                                <span class="mappedData">Event Content:</span> ${lEvent.getEventContentStr()}
                                            </div>
                                        </div>
                                    </div>
                                    <div style="display: flex;">
                                        <div class="mappedButton">
                                            Baseline Events of Type:<br />
                                            <span style="word-wrap: break-word"><a role="button" data-toggle="collapse" href="#eventContent${iterator}">${lEvent.getEventTypeString()}</a></span>
                                        </div>
                                        <div id="eventContent${iterator}" class="panel-collapse collapse in" style="flex: 2">
                                            <c:forEach items="${initialData.eventTypeMap}" var="mappedDigest">
                                                <c:if test="${mappedDigest.key == lEvent.getEventDigestStr()}">
                                                    <c:set var="event" value="${mappedDigest.value}" scope="page"/>
                                                    <c:forEach items="${mappedDigest.value}" var="event">
                                                        <div class="mappedOverhead">
                                                            <div><span class="mappedData">PCR Index:</span> ${event.getPcrIndex()}</div>
                                                            <div><span class="mappedData">Digest:</span> ${event.getEventDigestStr()}</div>
                                                            <div><span class="mappedData">Event Content:</span> ${event.getEventContentStr()}</div>
                                                        </div>
                                                    </c:forEach>
                                                </c:if>
                                            </c:forEach>
                                        </div>
                                    </div>
                                    <c:set var="iterator" value="${iterator+1}" scope="page"/>
                                </c:forEach>
                            </c:if>
                        </div>
                    </div>
                </div>
            </c:when>
            <c:when test="${initialData.rimType=='Base'}">
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
                            <div>
                                <span>
                                    <c:choose>
                                        <c:when test="${not empty initialData.linkHrefLink}">
                                            <a href="${portal}/rim-details?id=${initialData.linkHrefLink}" rel="${initialData.linkRel}">${initialData.linkHref}</a>
                                        </c:when>
                                        <c:otherwise>
                                            <a href="${initialData.linkHref}" rel="${initialData.linkRel}">${initialData.linkHref}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </span>
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
                        <c:if test="${not empty initialData.pcUriGlobal}">
                            <div>PC URI Global:&nbsp;<span>${initialData.pcUriGlobal}</span></div>
                        </c:if>
                        <c:if test="${not empty initialData.pcUriLocal}">
                            <div>PC URI Local:&nbsp;<span>${initialData.pcUriLocal}</span></div>
                        </c:if>
                        <c:choose>
                            <c:when test="${not empty initialData.rimLinkId}">
                                <div>Rim Link Hash:&nbsp;<span><a href="${portal}/rim-details?id=${initialData.rimLinkId}">${initialData.rimLinkHash}</a></span>
                            </c:when>
                            <c:otherwise>
                                <div>Rim Link Hash:&nbsp;<span>${initialData.rimLinkHash}</span>
                            </c:otherwise>
                        </c:choose>
                        <c:if test="${not empty initialData.rimLinkHash}">
                            <span>
                                <c:choose>
                                <c:when test="${initialData.linkHashValid}">
                                    <img src="${passIcon}" title="SWID Tag exist.">
                                </c:when>
                                <c:otherwise>
                                    <img src="${failIcon}" title="SWID Tag doesn't exist.">
                                </c:otherwise>
                                </c:choose>
                            </span>
                            </c:if>
                        </div>
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
                                                                            <c:choose>
                                                                                <c:when test="${not empty initialData.supportRimHashValid}">
                                                                                    <img src="${passIcon}" title="${supportRimHashValidText}"/>
                                                                                </c:when>
                                                                                <c:otherwise>
                                                                                    <img src="${failIcon}" title="${supportRimHashInvalidText}"/>
                                                                                </c:otherwise>
                                                                            </c:choose>
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            ${resource.getName()}
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                </span>
                                                            </div>
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
                                                            </div>
                                                            <c:choose>
                                                                <c:when test="${not empty initialData.pcrList}">
                                                                    <div class="component col col-md-10">
                                                                            <div class="panel-body">
                                                                                <div class="component" role="tab" id="pcrValues">
                                                                                    <a role="button" data-toggle="collapse" data-parent="#directorycollapse" class="collapsed"
                                                                                       href="#pcrscollapse" aria-expanded="false" aria-controls="pcrscollapse">
                                                                                        Expected PCR Values
                                                                                    </a>                                                         
                                                                                </div>
                                                                                <div id="pcrscollapse" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingThree" aria-expanded="true">
                                                                                    <div>
                                                                                        <c:set var="count" value="0" scope="page"/>
                                                                                        <c:forEach items="${initialData.pcrList}" var="pcrValue">
                                                                                            <div id="componentIdentifier" class="row">
                                                                                                <div>
                                                                                                    <span>PCR ${count} - </span>
                                                                                                    <span style="overflow-wrap: break-word">${pcrValue}</span>
                                                                                                </div>
                                                                                            </div>
                                                                                            <c:set var="count" value="${count + 1}" scope="page"/>
                                                                                        </c:forEach>
                                                                                    </div>
                                                                                </div>
                                                                            </div>
                                                                    </div>
                                                                </c:when>
                                                                <c:otherwise>
                                                                <c:if test="${not initialData.swidPatch and not initialData.swidSupplemental}">
                                                                    <div class="component col col-md-10" style="color: red; padding-left: 20px">Support RIM file named ${resource.getName()} was not imported via the Reference Integrity Manifest page.</div>
                                                                </c:if>
                                                                </c:otherwise>
                                                            </c:choose>
                                                            </div>
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
                <div class="row">
                    <div class="col-md-1 col-md-offset-1"><span class="colHeader">Signature</span></div>
                    <div id="signature" class="col col-md-8">
                        <div>Validity:&nbsp;<span>
                                <c:choose>
                                    <c:when test="${initialData.signatureValid}">
                                        <img src="${passIcon}" title="${signatureValidText}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <img src="${failIcon}" title="${signatureInvalidText}"/>
                                    </c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                        <div>
                            <span>
                                <c:if test="${not empty initialData.issuerID}">
                                    <div><a href="${portal}/certificate-details?id=${initialData.issuerID}&type=certificateauthority">Signing certificate</a></div>
                                </c:if>
                            </span>
                        </div>
                        <div>
                            <span>
                                <c:if test="${not empty initialData.skID}">
                                    <div>Subject Key Identifier: ${initialData.skID}</div>
                                </c:if>
                            </span>
                        </div>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
            </c:otherwise>
        </c:choose>
    </div>
</div>
</div>
<script>
    function eventSearch(txtInput) {
        // Declare variables
        var input, filter, table, tr, td, i, txtValue, txtFound;

        if (txtInput === null) {
            input = document.getElementById("eventInput");
            filter = input.value.toUpperCase();
        } else {
            filter = txtInput;
        }

        table = document.getElementById("eventLog");
        tr = table.getElementsByTagName("tr");

        // Loop through all table rows, and hide those who don't match the search query
        for (i = 0; i < tr.length; i++) {
            txtFound = true;
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
    window.onload = function () {
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
