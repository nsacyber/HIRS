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
                    <div class="col-md-1 col-md-offset-1"><span class="colHeader">Platform Information</span></div>
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
                <div class="panel-heading" role="tab" id="headingOne">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" class="collapsed"
                           href="#componentListCollapse" aria-expanded="true" aria-controls="componentListCollapse">
                            Components
                        </a>
                    </h4>
                </div>
                <div id="componentListCollapse" class="row" style="margin: auto 260px auto 125px">                    
                    <div class="row" style="display: flex;">
                        <div class="panel panel-default" style="flex: 1">
                            <div class="panel-heading"><span class="fieldHeader">Certificate Component</span></div>

                            <div style="display: flex 1; margin: 2px auto 2px 25px">
                                <c:forEach items="${initialData.componentResults}" var="componentResult">
                                    <div class="panel-body" style="background-color: greenyellow">                                        
                                        <span class="fieldHeader">Component Class: </span> ${componentResult.getComponentClassStr()}<br />
                                        <span class="fieldHeader">Manufacturer:</span> ${componentResult.getManufacturer()}<br />
                                        <span class="fieldHeader">Model:</span> ${componentResult.getModel()}<br />
                                        <span class="fieldHeader">Serial Number:</span> ${componentResult.getSerialNumber()}<br />
                                        <span class="fieldHeader">Revision:</span> ${componentResult.getRevisionNumber()}<br />
                                    </div>
                                </c:forEach>
                                <c:forEach items="${initialData.misMatchedCmponentResults}" var="componentResult">
                                    <div class="panel-body" style="background-color: lightcoral">                                        
                                        <span class="fieldHeader">Component Class: </span> ${componentResult.getComponentClassStr()}<br />
                                        <span class="fieldHeader">Manufacturer:</span> ${componentResult.getManufacturer()}<br />
                                        <span class="fieldHeader">Model:</span> ${componentResult.getModel()}<br />
                                        <span class="fieldHeader">Serial Number:</span> ${componentResult.getSerialNumber()}<br />
                                        <span class="fieldHeader">Revision:</span> ${componentResult.getRevisionNumber()}<br />
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                        <div class="panel panel-default" style="flex: 2">
                            <div class="panel-heading"><span class="fieldHeader">Device Components</span></div>     

                            <div style="display: flex 2; margin: 2px auto 2px 25px">
                                <c:forEach items="${initialData.componentInfos}" var="componentInfo">
                                    <div class="panel-body" style="background-color: greenyellow">
                                        <span class="fieldHeader">Component Class: </span> ${componentInfo.getComponentClassStr()}<br />
                                        <span class="fieldHeader">Manufacturer:</span> ${componentInfo.getComponentManufacturer()}<br />
                                        <span class="fieldHeader">Model:</span> ${componentInfo.getComponentModel()}<br />
                                        <span class="fieldHeader">Serial Number:</span> ${componentInfo.getComponentSerial()}<br />
                                        <span class="fieldHeader">Revision:</span> ${componentInfo.getComponentRevision()}<br />
                                    </div>
                                </c:forEach>
                                <c:forEach items="${initialData.misMatchedComponentInfos}" var="componentInfo">
                                    <div class="panel-body" style="background-color: lightcoral">
                                        <span class="fieldHeader">Component Class: </span> ${componentInfo.getComponentClassStr()}<br />
                                        <span class="fieldHeader">Manufacturer:</span> ${componentInfo.getComponentManufacturer()}<br />
                                        <span class="fieldHeader">Model:</span> ${componentInfo.getComponentModel()}<br />
                                        <span class="fieldHeader">Serial Number:</span> ${componentInfo.getComponentSerial()}<br />
                                        <span class="fieldHeader">Revision:</span> ${componentInfo.getComponentRevision()}<br />
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</jsp:body>
</my:page>