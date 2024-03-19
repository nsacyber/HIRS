<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="fn" uri="jakarta.tags.functions"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%--CONTENT--%>
<my:page>
    <jsp:attribute name="style">
        <link type="text/css" rel="stylesheet" href="${common}/certificate_details.css"/>
        <link type="text/css" rel="stylesheet" href="${common}/rim_details.css"/>
    </jsp:attribute>
    <jsp:attribute name="pageHeaderTitle">Platform Component Failure Comparison
        <c:if test="${param.sessionId==null}">
            <c:redirect url = "${portal}/validation-reports"/>
        </c:if>
    </jsp:attribute>

    <jsp:body>
        <div id="certificate-details-page" class="container-fluid">
            <div style="display: inline">
                                <div class="row">
                                    <div class="col-md-1 col-md-offset-1"><span class="colHeader">Support Component Objects</span></div>
                                    <div id="measurements" class="col col-md-8">
                                        <c:if test="${not empty initialData.hostName}">
                                            <div>Device:&nbsp;<span>${initialData.hostName}</span>
                                            </div>
                                        </c:if>
                                        <c:if test="${not empty initialData.certificateId}">
                                            <div>Platform Certificate:&nbsp;<span><a href="${portal}/certificate-details?id=${initialData.certificateId}">${initialData.certificateFileName}</a></span>
                                            </div>
                                        </c:if>
                                    </div>
                                </div>
                                <br />
                                <div class="row" style="margin: auto 260px auto 125px">
                                    <div class="panel panel-default" style="flex: 1">
                                        <div class="panel-heading">Client Log</div>
                                        <c:if test="${not empty initialData.componentResults}">
                                            <c:set var="iterator" value="0" scope="page"/>
                                            <c:forEach items="${initialData.componentResults}" var="componentResult">
                                                <div>
                                                    <div style="display: flex; background: lightgray;">
                                                        <div style="display: flex 1; font-weight: bold; margin: auto 1rem auto 1rem">Failed Event Digest:<br />
                                                        </div>
                                                        <div style="display: flex 2; margin: 2px auto 2px 25px">
                                                            <span class="mappedData">Manufacturer:</span> ${componentResult.getManufacturer()}<br />
                                                            <span class="mappedData">Model:</span> ${componentResult.getModel()}<br />
                                                            <c:if test="${not empty fn:trim(componentResult.getSerialNumber())}">
                                                                  <span class="mappedData">Serial Number:</span> ${component.getSerialNumber()}<br />
                                                            </c:if>
                                                            <c:if test="${not empty fn:trim(componentResult.getRevisionNumber())}">
                                                                  <span class="mappedData">Revision:</span> ${component.getRevisionNumber()}<br />
                                                            </c:if>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div style="display: flex;">
                                                    <div class="mappedButton">
                                                        Expected Events from RIM DB:<br />
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
        </div>
    </jsp:body>
</my:page>