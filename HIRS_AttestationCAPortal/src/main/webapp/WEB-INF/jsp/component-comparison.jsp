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
                            <div>Platform Certificate:&nbsp;
                                <span>
                                    <a href="${portal}/certificate-details?id=${initialData.certificateId}&type=platform">${initialData.boardNumber}</a>
                                </span>
                            </div>
                        </c:if>
                    </div>
                </div>
                <br />
                <div class="row" style="margin: auto 260px auto 125px">
                    <div class="panel panel-default" style="flex: 1">
                        <div class="panel-heading">Certificate Component</div>
                    </div>
                    <div class="panel panel-default" style="flex: 2">
                        <div class="panel-heading">Device Components</div>
                    </div>
                    <div style="display: flex;">
                        <c:forEach var = "i" begin = "1" end = "${totalSize}">
                            <div style="display: flex 1; margin: auto 1rem auto 1rem">
                                <span class="mappedData">Manufacturer:</span> ${componentResults.get(i).getManufacturer()}<br />
                                <span class="mappedData">Model:</span> ${componentResults.get(i).getModel()}<br />
                                <span class="mappedData">Serial Number:</span> ${componentResults.get(i).getSerialNumber()}<br />
                                <span class="mappedData">Revision:</span> ${componentResults.get(i).getRevisionNumber()}<br />
                            </div>
                            <div style="display: flex 2; margin: 2px auto 2px 25px">
                                <span class="mappedData">Manufacturer:</span> ${componentInfos.get(i).getComponentManufacturer()}<br />
                                <span class="mappedData">Model:</span> ${componentInfos.get(i).getComponentModel()}<br />
                                <span class="mappedData">Serial Number:</span> ${componentInfos.get(i).getComponentSerial()}<br />
                                <span class="mappedData">Revision:</span> ${componentInfos.get(i).getComponentRevision()}<br />
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </div>
    </div>
</jsp:body>
</my:page>