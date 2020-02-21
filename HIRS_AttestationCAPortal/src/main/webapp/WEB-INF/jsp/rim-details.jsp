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
    </jsp:attribute>
    <jsp:attribute name="pageHeaderTitle">
        Reference Integrity Manifest
        <a href="${portal}/reference-manifests/download?id=${param.id}">
            <img src="${icons}/ic_file_download_black_24dp.png" title="Download Certificate">
        </a>
    </jsp:attribute>
    <jsp:body>

        <div id="certificate-details-page" class="container-fluid">
            <div class="row">
                <div class="col-md-1 col-md-offset-1"><span class="colHeader">Software Identity</span></div>
                <div id="softwareIdentity" class="col col-md-8">
                    <div>SWID Name:&nbsp;<span>${initialData.swidName}</span></div>
                    <div>SWID Version:&nbsp;<span>${initialData.swidVersion}</span></div>
                    <div>SWID Tag ID:&nbsp;<span>${initialData.swidTagId}</span></div>
                    <div>SWID Tag Version:&nbsp;<span></span></div>
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
                    <c:if test="${not empty initialData.firmwareVersion}">
                        <div>Firmware Version:&nbsp;<span>${initialData.firmwareVersion}</span></div>
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
        </div>
        <div class="row">
            <div class="col-md-1 col-md-offset-1"><span class="colHeader">Support RIM(s)</span></div>
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
                                                            <span data-toggle="tooltip" data-placement="top" title="Resource File">${resource.getName()}
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
    </div>
</div>
</jsp:body>
</my:page>