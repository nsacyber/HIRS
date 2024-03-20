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
                                        <c:if test="${not empty initialData.deviceName}">
                                            <div>Device:&nbsp;<span>${initialData.deviceName}</span>
                                            </div>
                                        </c:if>
                                        <c:if test="${not empty initialData.certificateId}">
                                            <div>Platform Certificate:&nbsp;<span><a href="${portal}/certificate-details?id=${initialData.certificateId}">${initialData.boardNumber}</a></span>
                                            </div>
                                        </c:if>
                                    </div>
                                </div>
                                <br />
                                <div class="row" style="margin: auto 260px auto 125px">
                                    <div class="panel panel-default" style="flex: 1">
                                        <div class="panel-heading">Certificate Component</div>
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
                                                            <span class="mappedData">Serial Number:</span> ${componentResult.getSerialNumber()}<br />
                                                            <span class="mappedData">Revision:</span> ${componentResult.getRevisionNumber()}<br />
                                                        </div>
                                                    </div>
                                                </div>
                                                <div style="display: flex;">
                                                    <div class="mappedButton">
                                                        Expected Events from RIM DB:<br />
                                                        <span style="word-wrap: break-word"><a role="button" data-toggle="collapse" href="#eventContent${iterator}">${lEvent.getEventTypeString()}</a></span>
                                                    </div>
                                                    <div id="eventContent${iterator}" class="panel-collapse collapse in" style="flex: 2">
                                                        <c:forEach items="${initialData.componentInfos}" var="componentInfos">
                                                            <span class="mappedData">Manufacturer:</span> ${componentInfos.getComponentManufacturer()}<br />
                                                            <span class="mappedData">Model:</span> ${componentInfos.getComponentModel()}<br />
                                                            <span class="mappedData">Serial Number:</span> ${componentInfos.getComponentSerial()}<br />
                                                            <span class="mappedData">Revision:</span> ${componentInfos.getComponentRevision()}<br />
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