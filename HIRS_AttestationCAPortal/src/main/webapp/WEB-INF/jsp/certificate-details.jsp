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
        <c:choose>
            <c:when test="${param.type=='certificateauthority'}">
                Certificate Authority
                <a href="${portal}/certificate-request/trust-chain/download?id=${param.id}">
                    <img src="${icons}/ic_file_download_black_24dp.png" title="Download Certificate">
                </a>
            </c:when>
            <c:when test="${param.type=='endorsement'}">
                Endorsement Certificate
                <a href="${portal}/certificate-request/endorsement-key-credentials/download?id=${param.id}">
                    <img src="${icons}/ic_file_download_black_24dp.png" title="Download Certificate">
                </a>
            </c:when>
            <c:when test="${param.type=='platform'}">
                Platform Certificate
                <a href="${portal}/certificate-request/platform-credentials/download?id=${param.id}">
                    <img src="${icons}/ic_file_download_black_24dp.png" title="Download Certificate">
                </a>
            </c:when>
            <c:when test="${param.type=='issued'}">
                Issued Attestation Certificates
                <a href="${portal}/certificate-request/issued-certificates/download?id=${param.id}">
                    <img src="${icons}/ic_file_download_black_24dp.png" title="Download Certificate">
                </a>
            </c:when>
            <c:otherwise>
                Unknown Certificate
            </c:otherwise>
        </c:choose>
    </jsp:attribute>

    <jsp:body>
        <div id="certificate-details-page" class="container-fluid">
            <div class="row">
                <div class="col-md-1 col-md-offset-1"><span class="colHeader">Issuer</span></div>
                <div id="issuer" class="col col-md-8">
                    <!-- Display the issuer, and provide a link to the issuer details if provided -->
                    <div>Distinguished Name:&nbsp;<span>
                            <c:choose>
                                <c:when test="${not empty initialData.issuerID}">
                                    <a href="${portal}/certificate-details?id=${initialData.issuerID}&type=certificateauthority">
                                        ${initialData.issuer}
                                    </a>
                                </c:when>
                                <c:otherwise>
                                    ${initialData.issuer}
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                    <div>Authority Key Identifier:&nbsp;
                        <span id="authorityKeyIdentifier"></span>
                    </div>
                    <c:if test="${not empty initialData.authInfoAccess}">
                        <div>Authority Info Access:&nbsp;<span>
                                <a href="${initialData.authInfoAccess}">${initialData.authInfoAccess}</a>
                            </span>
                        </div>
                    </c:if>
                    <c:if test="${not empty initialData.authSerialNumber}">
                        <div>Authority Serial Number:&nbsp;
                            <span id="authSerialNumber"></span>
                        </div>
                    </c:if>
                    <c:if test="${param.type!='issued'}">
                        <span class="chainIcon">
                            <!-- Icon with link for missing certificate for the chain -->
                            <c:choose>
                                <c:when test="${initialData.isSelfSigned == 'true'}">
                                    <img src="${icons}/ic_all_inclusive_black_24dp.png"
                                         title="Self sign certificate.">
                                </c:when>
                                <c:when test="${empty initialData.missingChainIssuer}">
                                    <img src="${icons}/ic_checkbox_marked_circle_black_green_24dp.png"
                                         title="All certificates in the chain were found.">
                                </c:when>
                                <c:otherwise>
                                    <img src="${icons}/ic_error_red_24dp.png"
                                         title="Missing ${initialData.missingChainIssuer} from the chain.">
                                </c:otherwise>
                            </c:choose>
                        </span>
                    </c:if>
                </div>
            </div>
            <c:if test="${not empty initialData.subject}">
                <div class="row">
                    <div class="col-md-1 col-md-offset-1"><span class="colHeader">Subject</span></div>
                    <div id="subject" class="col col-md-8">${initialData.subject}</div>
                </div>
            </c:if>            
            <c:if test="${not empty initialData.serialNumber}">
                <div class="row">
                    <div class="col-md-1 col-md-offset-1"><span class="colHeader">Serial Number</span></div>
                    <div id="serialNumber" class="col col-md-8 vertical"></div>
                </div>
            </c:if>
            <c:if test="${not empty initialData.beginValidity}">
                <div class="row">
                    <div class="col-md-1 col-md-offset-1"><span class="colHeader">Validity</span></div>
                    <div id="validity" class="col col-md-8">
                        <div>Not Before:&nbsp;<span>${initialData.beginValidity}</span></div>
                        <div>Not After:&nbsp;<span>${initialData.endValidity}</span></div>
                    </div>
            </div>
            </c:if>
            <div class="row">
                <div class="col-md-1 col-md-offset-1"><span class="colHeader">Signature</span></div><div id="signatureSection" class="col col-md-8">
                    <div class="panel-body">
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <a role="button" data-toggle="collapse" class="collapsed" href="#signatureComponentcollapse"
                                   aria-expanded="true" data-placement="top" aria-controls="signatureComponentcollapse">
                                    Signature
                                </a>
                            </div>
                            <div id="signatureComponentcollapse" class="panel-body collapse" role="tabpanel" aria-labelledby="headingOne" aria-expanded="false">
                                <div id="signature" class="fieldValue"></div>                                                                  
                            </div>
                        </div>
                        <div class="panel panel-default">
                            <div class="panel-heading">
                                <a role="button" data-toggle="collapse" class="collapsed" href="#signatureSizecollapse"
                                   aria-expanded="true" data-placement="top" aria-controls="signatureSizecollapse">
                                    Algorithm
                                </a>                                                                      
                            </div>
                            <div id="signatureSizecollapse" class="panel-body collapse" role="tabpanel" aria-labelledby="headingOne" aria-expanded="false">
                                <div>
                                    <span class="fieldValue">
                                        ${initialData.signatureAlgorithm} / ${initialData.signatureSize}
                                    </span>
                                </div>                                                               
                            </div>
                        </div>
                    </div>
                </div>
            </div>    
            <c:if test="${not empty initialData.encodedPublicKey}">
                <div class="row">
                    <div class="col-md-1 col-md-offset-1"><span class="colHeader">Public Key</span></div>
                    <div id="publicKeySection" class="col col-md-8">
                        <div class="panel-body">
                            <div class="panel panel-default">
                                <div class="panel-heading">
                                    <a role="button" data-toggle="collapse" class="collapsed" href="#publicKeycollapse"
                                       aria-expanded="true" data-placement="top" aria-controls="publicKeycollapse">
                                        Public Key
                                    </a>                                                                    
                                </div>
                                <div id="publicKeycollapse" class="panel-body collapse" role="tabpanel" aria-labelledby="headingOne" aria-expanded="false">
                                    <c:choose>
                                        <c:when test="${not empty initialData.publicKeyValue}">
                                            <div id="encodedPublicKey" class="fieldValue"></div>                        
                                        </c:when>
                                        <c:otherwise>
                                            <div id="encodedPublicKey" class="fieldValue"></div>                                    
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                            <div class="panel panel-default">
                                <div class="panel-heading">
                                    <a role="button" data-toggle="collapse" class="collapsed" href="#publicKeySizecollapse"
                                       aria-expanded="true" data-placement="top" aria-controls="publicKeySizecollapse">
                                        Algorithm
                                    </a>                                                                      
                                </div>
                                <div id="publicKeySizecollapse" class="panel-body collapse" role="tabpanel" aria-expanded="false">
                                    <div>
                                        <span class="fieldValue">
                                            ${initialData.publicKeyAlgorithm} / ${initialData.publicKeySize}
                                        </span>
                                    </div>                                                               
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </c:if>
            <div class="row">
                <div class="col-md-1 col-md-offset-1"><span class="colHeader">X509 Credential Version</span></div>
                <div id="credentialVersion" class="col col-md-8 vertical">${initialData.x509Version} (v${initialData.x509Version + 1})</div>
            </div>
            <div class="row">
                <div class="col-md-1 col-md-offset-1"><span class="colHeader">Credential Type</span></div>
                <div id="credentialType" class="col col-md-8 vertical">${initialData.credentialType}</div>
            </div>
            <!-- Add the different fields based on the certificate type -->
            <c:choose>
                <c:when test="${param.type=='certificateauthority'}">
                    <c:choose>
                        <c:when test="${not empty initialData.subjectKeyIdentifier}">
                            <div class="row">
                                <div class="col-md-1 col-md-offset-1"><span class="colHeader">Subject Key Identifier</span></div>
                                <div id="subjectKeyIdentifier" class="col col-md-8 vertical"></div>
                            </div>
                        </c:when>
                    </c:choose>
                    <c:if test="${initialData.crlPoints}">
                        <div class="row">
                            <div class="col-md-1 col-md-offset-1"><span class="colHeader">Revocation Locator</span></div>
                            <div id="revocationLocator" class="col col-md-8"><a href="${initialData.crlPoints}">${initialData.crlPoints}</div>
                        </div>
                    </c:if>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Key Usage</span></div>                        
                        <c:choose>
                            <c:when test="${not empty initialData.keyUsage}">
                                <div id="keyUsage" class="col col-md-8 vertical">${initialData.keyUsage}</div>
                            </c:when>
                            <c:otherwise>
                                <div id="keyUsage" class="col col-md-8 vertical">Not Specified</div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <c:choose>
                        <c:when test="${not empty initialData.extendedKeyUsage}">
                            <div class="row">
                                <div class="col-md-1 col-md-offset-1"><span class="colHeader">Extended Key Usage</span></div>
                                <div id="extendedKeyUsage" class="col col-md-8 vertical">${initialData.extendedKeyUsage}</div>
                            </div>
                        </c:when>
                    </c:choose>
                </c:when>
                <c:when test="${param.type=='endorsement'}">
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">System Information</span></div>
                        <div id="subjectAltName" class="col col-md-8">
                            <div id="manufacturer">Manufacturer:&nbsp;<span>${initialData.manufacturer}</span></div>
                            <div id="model">Model:&nbsp;<span>${initialData.model}</span></div>
                            <div id="version">Version:&nbsp;<span>${initialData.version}</span></div>
                        </div>  
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Policy Reference</span></div>
                        <div id="policyReference" class="col col-md-8 vertical">
                            <c:choose>
                                <c:when test="${not empty initialData.policyReference}">
                                    ${initialData.policyReference}
                                </c:when>
                                <c:otherwise>
                                    Not Specified
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <c:if test="${initialData.crlPoints}">
                        <div class="row">
                            <div class="col-md-1 col-md-offset-1"><span class="colHeader">Revocation Locator</span></div>
                            <div id="revocationLocator" class="col col-md-8"><a href="${initialData.crlPoints}">${initialData.crlPoints}</div>
                        </div>
                    </c:if>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Key Usage</span></div>                        
                        <c:choose>
                            <c:when test="${not empty initialData.keyUsage}">
                                <div id="keyUsage" class="col col-md-8 vertical">${initialData.keyUsage}</div>
                            </c:when>
                            <c:otherwise>
                                <div id="keyUsage" class="col col-md-8 vertical">Not Specified</div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <c:choose>
                        <c:when test="${not empty initialData.extendedKeyUsage}">
                            <div class="row">
                                <div class="col-md-1 col-md-offset-1"><span class="colHeader">Extended Key Usage</span></div>
                                <div id="extendedKeyUsage" class="col col-md-8 vertical">${initialData.extendedKeyUsage}</div>
                            </div>
                        </c:when>
                    </c:choose>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1">
                            <span class="colHeader">
                                <a role="button" data-toggle="collapse" class="collapsed" href="#tpmSpecificationInner"
                                   aria-expanded="true" data-placement="top" aria-controls="tpmSpecificationInner">
                                    TPM Specification
                                </a>
                            </span>
                        </div>
                        <div id="tpmSpecification" class="col col-md-8">
                            <div id="tpmSpecificationInner" class="panel-body collapse" role="tabpanel" aria-expanded="false">
                                <div>Family:&nbsp;<span>${initialData.TPMSpecificationFamily}</span></div>
                                <div>Level:&nbsp;<span>${initialData.TPMSpecificationLevel}</span></div>
                                <div>Revision:&nbsp;<span>${initialData.TPMSpecificationRevision}</span></div>
                            </div>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1">
                            <span class="colHeader">
                                <a role="button" data-toggle="collapse" class="collapsed" href="#tpmSecurityAssertionInner"
                                   aria-expanded="true" data-placement="top" aria-controls="tpmSecurityAssertionInner">
                                    TPM Security Assertion
                                </a>
                            </span>
                        </div>
                        <div id="tpmSecurityAssertion" class="col col-md-8">
                            <div id="tpmSecurityAssertionInner" class="panel-body collapse" role="tabpanel" aria-expanded="false">
                                <div>Version:&nbsp;<span>${initialData.TPMSecurityAssertionsVersion}</span></div>
                                <div>Field Upgradeable:&nbsp;<span>${initialData.TPMSecurityAssertionsFieldUpgradeable}</span></div>
                                <div>ek Generation Type:&nbsp;<span>${initialData.TPMSecurityAssertionsEkGenType}</span></div>
                                <div>ek Generation Location:&nbsp;<span>${initialData.TPMSecurityAssertionsEkGenLoc}</span></div>
                                <div>ek Certificate Generation Location:&nbsp;<span>${initialData.TPMSecurityAssertionsEkCertGenLoc}</span></div>
                            </div>
                        </div>
                    </div>
                </c:when>
                <c:when test="${param.type=='platform'}">
                    <c:if test="${not empty initialData.platformType}">
                        <div class="row">
                            <div class="col-md-1 col-md-offset-1"><span class="colHeader">Platform Type</span></div>
                            <div id="platformType" class="col col-md-8 vertical">${initialData.platformType}</div>
                        </div>
                        <div class="row">
                            <div class="col-md-1 col-md-offset-1"><span class="colHeader">Platform Chain</span></div>
                            <div id="platformType" class="col col-md-8 vertical">
                                <span>
                                    <c:forEach items="${initialData.chainCertificates}" var="credential" varStatus="loop">
                                        <c:choose>
                                            <c:when test="${initialData.certificateId==credential.getId().toString()}">
                                                ${loop.index}&nbsp;
                                            </c:when>
                                            <c:otherwise>
                                                <a href="${portal}/certificate-details?id=${credential.getId()}&type=platform">${loop.index}</a>&nbsp;                                            
                                            </c:otherwise>
                                        </c:choose>
                                    </c:forEach>
                                </span>
                            </div>
                        </div>                        
                    </c:if>
                    <c:if test="${not empty initialData.CPSuri}">
                        <div class="row">
                            <div class="col-md-1 col-md-offset-1"><span class="colHeader">Certification Practice Statement URI</span></div>
                            <div id="certificateCPSU" class="col col-md-8 vertical">
                                <a href="${initialData.CPSuri}"> ${initialData.CPSuri}</a>
                            </div>
                        </div>
                    </c:if>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Holder</span></div>
                        <div id="holder" class="col col-md-8">
                            <c:if test="${not empty initialData.holderIssuer}">
                                <div>Holder Certificate:&nbsp;<span>${initialData.holderIssuer}</span></div>
                            </c:if>
                            <div id="certificateid">
                                <div>Holder Identifier:&nbsp;
                                    <c:choose>
                                        <c:when test="${not empty initialData.holderId}">
                                            <span>
                                                <c:choose>
                                                    <c:when test="${(not empty initialData.platformType) and (initialData.platformType=='Delta')}">
                                                        <a href="${portal}/certificate-details?id=${initialData.holderId}&type=platform">
                                                            ${initialData.holderSerialNumber}
                                                        </a>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <a href="${portal}/certificate-details?id=${initialData.holderId}&type=endorsement">
                                                            ${initialData.holderSerialNumber}
                                                        </a>
                                                    </c:otherwise>
                                                </c:choose>                                                
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span>${initialData.holderSerialNumber}</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>                                
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">System Platform Information</span></div>
                        <div id="subjectAltName" class="col col-md-8">
                            <div id="manufacturer">Manufacturer:&nbsp;<span>${initialData.manufacturer}</span></div>
                            <div id="model">Model:&nbsp;<span>${initialData.model}</span></div>
                            <div id="version">Version:&nbsp;<span>${initialData.version}</span></div>
                            <div id="serial">Serial Number:&nbsp;<span>${initialData.platformSerial}</span></div>
                        </div>                        
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">TCG Platform Specification Version</span></div>
                        <div id="majorVersion" class="col col-md-8 vertical">${initialData.majorVersion}.${initialData.minorVersion}.${initialData.revisionLevel}</div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Platform Class</span></div>
                        <div id="platformClass" class="col col-md-8 vertical">${initialData.platformClass}</div>
                    </div>
                    <!-- TBB Security Assertion-->
                    <c:if test="${not empty initialData.tbbSecurityAssertion}">
                        <div class="row">
                            <div class="col-md-1 col-md-offset-1"><span class="colHeader">TBB Security Assertion</span></div>
                            <div id="tbbsecurity" class="col col-md-8">
                                <div class="tbbsecurityLine">
                                    <span class="fieldHeader">Version:</span>
                                    <span class="fieldValue">${initialData.tbbSecurityAssertion.getVersion()}&nbsp;(v${initialData.tbbSecurityAssertion.getVersion().getValue() + 1})</span>
                                </div>
                                <div class="tbbsecurityLine">
                                    <span class="fieldHeader">RTM (Root of Trust of Measurement):</span>
                                    <span class="fieldValue">${fn:toUpperCase(initialData.tbbSecurityAssertion.getRtmType().getValue())}</span>
                                </div>
                                <!-- CCINFO -->
                                <c:if test="${not empty initialData.tbbSecurityAssertion.getCcInfo()}">
                                    <c:set var="ccinfo" value="${initialData.tbbSecurityAssertion.getCcInfo()}" />
                                    <div class="panel panel-default">
                                        <div class="panel-heading" role="tab" id="headingOne">
                                            <h4 class="panel-title">
                                                <a role="button" data-toggle="collapse" data-parent="#tbbsecurity" class="collapsed"
                                                   href="#ccinfocollapse" aria-expanded="false" aria-controls="ccinfocollapse">
                                                    Common Criteria Measures Information
                                                </a>
                                            </h4>
                                        </div>
                                        <div id="ccinfocollapse" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingOne">
                                            <div class="panel-body">
                                                <div id="ccinfo" class="row">
                                                    <div class="tbbsecurityLine">
                                                        <span class="fieldHeader">Version:</span>
                                                        <span class="fieldValue">${ccinfo.getVersion()}</span>
                                                    </div>
                                                    <div class="tbbsecurityLine">
                                                        <span class="fieldHeader">Assurance Level:</span>
                                                        <span class="fieldValue">${fn:toUpperCase(ccinfo.getAssurancelevel().getValue())}</span>
                                                    </div>
                                                    <div class="tbbsecurityLine">
                                                        <span class="fieldHeader">Evaluation Status:</span>
                                                        <span class="fieldValue">${fn:toUpperCase(ccinfo.getEvaluationStatus().getValue())}</span>
                                                    </div>
                                                    <div class="tbbsecurityLine">
                                                        <c:choose>
                                                            <c:when test="${ccinfo.getPlus()=='TRUE'}">
                                                                <span class="label label-success">Plus</span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span class="label label-danger">Not Plus</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                    <div class="tbbsecurityLine">
                                                        <span class="fieldHeader">Strength of Function:</span>
                                                        <span class="fieldValue">${fn:toUpperCase(ccinfo.getStrengthOfFunction().getValue())}</span>
                                                    </div>
                                                    <c:if test="${not empty ccinfo.getProfileOid()}">
                                                        <div class="tbbsecurityLine">
                                                            <span class="fieldHeader">Profile OID:</span>
                                                            <span class="fieldValue">${ccinfo.getProfileOid()}</span>
                                                        </div>
                                                    </c:if>
                                                    <c:if test="${not empty ccinfo.getProfileUri()}">
                                                        <div class="tbbsecurityLine">
                                                            <span class="fieldHeader">Profile URI:</span>
                                                            <span class="fieldValue">
                                                                <a href="${ccinfo.getProfileUri().getUniformResourceIdentifier()}">
                                                                    ${ccinfo.getProfileUri().getUniformResourceIdentifier()}
                                                                </a>
                                                            </span>
                                                        </div>
                                                        <c:if test="${not empty ccinfo.getProfileUri().getHashAlgorithm()}">
                                                            <div class="tbbsecurityLine">
                                                                <span class="fieldHeader">Profile Hash Algorithm:</span>
                                                                <span class="fieldValue">${ccinfo.getProfileUri().getHashAlgorithm()}</span>
                                                            </div>
                                                        </c:if>
                                                        <c:if test="${not empty ccinfo.getProfileUri().getHashValue()}">
                                                            <div class="tbbsecurityLine">
                                                                <span class="fieldHeader">Profile Hash Value:</span>
                                                                <span class="fieldValue">${ccinfo.getProfileUri().getHashValue()}</span>
                                                            </div>
                                                        </c:if>
                                                    </c:if>
                                                    <c:if test="${not empty ccinfo.getTargetOid()}">
                                                        <div class="tbbsecurityLine">
                                                            <span class="fieldHeader">Target OID:</span>
                                                            <span class="fieldValue">${ccinfo.getTargetOid()}</span>
                                                        </div>
                                                    </c:if>
                                                    <c:if test="${not empty ccinfo.getTargetUri()}">
                                                        <div class="tbbsecurityLine">
                                                            <span class="fieldHeader">Target URI:</span>
                                                            <span class="fieldValue">
                                                                <a href="${ccinfo.getTargetUri().getUniformResourceIdentifier()}">
                                                                    ${ccinfo.getTargetUri().getUniformResourceIdentifier()}
                                                                </a>
                                                            </span>
                                                        </div>
                                                        <c:if test="${not empty ccinfo.getTargetUri().getHashAlgorithm()}">
                                                            <div class="tbbsecurityLine">
                                                                <span class="fieldHeader">Target Hash Algorithm:</span>
                                                                <span class="fieldValue">${ccinfo.getTargetUri().getHashAlgorithm()}</span>
                                                            </div>
                                                        </c:if>
                                                        <c:if test="${not empty ccinfo.getTargetUri().getHashValue()}">
                                                            <div class="tbbsecurityLine">
                                                                <span class="fieldHeader">Target Hash Value:</span>
                                                                <span class="fieldValue">${ccinfo.getTargetUri().getHashValue()}</span>
                                                            </div>
                                                        </c:if>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </c:if>
                                <!-- FIPS Level -->
                                <c:if test="${not empty initialData.tbbSecurityAssertion.getFipsLevel()}">
                                    <c:set var="fipslevel" value="${initialData.tbbSecurityAssertion.getFipsLevel()}" />
                                    <div class="panel panel-default">
                                        <div class="panel-heading" role="tab" id="headingThree">
                                            <h4 class="panel-title">
                                                <a role="button" data-toggle="collapse" data-parent="#tbbsecurity" class="collapsed"
                                                   href="#fipscollapse" aria-expanded="false" aria-controls="fipscollapse">
                                                    FIPS Level
                                                </a>
                                            </h4>
                                        </div>
                                        <div id="fipscollapse" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingTwo">
                                            <div class="panel-body">
                                                <div id="fipsLevel" class="row">
                                                    <div class="tbbsecurityLine">
                                                        <span class="fieldHeader">Version:</span>
                                                        <span class="fieldValue">${fipslevel.getVersion()}</span>
                                                    </div>
                                                    <div class="tbbsecurityLine">
                                                        <span class="fieldHeader">Level:</span>
                                                        <span class="fieldValue">${fn:toUpperCase(fipslevel.getLevel().getValue())}</span>
                                                    </div>
                                                    <div class="tbbsecurityLine">
                                                        <c:choose>
                                                            <c:when test="${fipslevel.getPlus()=='TRUE'}">
                                                                <span class="label label-success">Plus</span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span class="label label-danger">Not Plus</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </c:if>
                                <!-- ISO9000 isCertified and URI -->
                                <div class="panel panel-default">
                                    <div class="panel-heading" role="tab" id="headingThree">
                                        <h4 class="panel-title">
                                            <a role="button" data-toggle="collapse" data-parent="#tbbsecurity" class="collapsed"
                                               href="#iso9000collapse" aria-expanded="false" aria-controls="iso9000collapse">
                                                ISO 9000
                                            </a>
                                        </h4>
                                    </div>
                                    <div id="iso9000collapse" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingThree">
                                        <div class="panel-body">
                                            <div id="iso9000" class="row">
                                                <div class="tbbsecurityLine">
                                                    <c:choose>
                                                        <c:when test="${initialData.tbbSecurityAssertion.getIso9000Certified()=='TRUE'}">
                                                            <span class="label label-success">ISO 9000 Certified</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="label label-danger">ISO 9000 Not Certified</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                                <c:if test="${not empty initialData.tbbSecurityAssertion.getIso9000Uri()}">
                                                    <div class="tbbsecurityLine">
                                                        <span class="fieldHeader">URI:</span>
                                                        <span class="fieldValue">
                                                            <a href="${initialData.tbbSecurityAssertion.getIso9000Uri()}">
                                                                ${initialData.tbbSecurityAssertion.getIso9000Uri()}
                                                            </a>
                                                        </span>
                                                    </div>
                                                </c:if>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </c:if>
                    <!-- For PC 2.0 -->
                    <c:if test="${fn:contains(initialData.credentialType, 'TCG')}">
                        <div class="row">
                            <div class="col-md-1 col-md-offset-1"><span class="colHeader">TCG Platform Configuration</span></div>
                            <div id="platformConfiguration" class="col col-md-8">
                                <c:if test="${not empty initialData.componentsIdentifier}">
                                    <!-- Component Identifier -->
                                    <div class="panel panel-default">
                                        <div class="panel-heading" role="tab" id="headingOne">
                                            <h4 class="panel-title">
                                                <a role="button" data-toggle="collapse" data-parent="#platformConfiguration" class="collapsed"
                                                   href="#componentIdentifiercollapse" aria-expanded="true" aria-controls="componentIdentifiercollapse">
                                                    Components
                                                </a>
                                            </h4>
                                        </div>
                                        <div id="componentIdentifiercollapse" class="panel-collapse collapse in" role="tabpanel" aria-labelledby="headingOne" aria-expanded="true">
                                            <div class="panel-body">
                                                <div id="componentIdentifier" class="row">
                                                    <c:forEach items="${initialData.componentsIdentifier}" var="component">
                                                        <div class="component col col-md-4">
                                                            <div class="panel panel-default">
                                                                <c:choose>
                                                                    <c:when test="${fn:contains(initialData.failures, component.getComponentSerial()) && not empty fn:trim(component.getComponentSerial())}">
                                                                        <div class="panel-heading" style="background-color: red; color: white">
                                                                    </c:when>
                                                                    <c:otherwise>
                                                                        <div class="panel-heading">
                                                                    </c:otherwise>
                                                                </c:choose>
                                                                    <c:choose>
                                                                        <c:when test="${component.isVersion2()=='TRUE'}">
                                                                            <span data-toggle="tooltip" data-placement="top" title="Component Class">${component.getComponentClass()}</span>
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <span data-toggle="tooltip" data-placement="top" title="Component Class">Platform Components</span>
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                </div>
                                                                <div class="panel-body">
                                                                    <span class="fieldHeader">Manufacturer:</span>
                                                                    <span class="fieldValue">${component.getComponentManufacturer()}</span><br/>
                                                                    <span class="fieldHeader">Model:</span>
                                                                    <span class="fieldValue">${component.getComponentModel()}</span><br/>
                                                                    <c:if test="${not empty fn:trim(component.getComponentSerial())}">
                                                                        <span class="fieldHeader">Serial Number:</span>
                                                                        <span class="fieldValue">${component.getComponentSerial()}</span><br/>
                                                                    </c:if>
                                                                    <c:if test="${not empty fn:trim(component.getComponentRevision())}">
                                                                        <span class="fieldHeader">Revision:</span>
                                                                        <span class="fieldValue">${component.getComponentRevision()}</span><br/>
                                                                    </c:if>
                                                                    <c:forEach items="${component.getComponentAddress()}" var="address">
                                                                        <span class="fieldHeader">${address.getAddressTypeValue()} address:</span>
                                                                        <span class="fieldValue">${address.getAddressValue()}</span><br/>
                                                                    </c:forEach>
                                                                    <c:choose>
                                                                        <c:when test="${component.getFieldReplaceable()=='TRUE'}">
                                                                            <span class="label label-success">Replaceable</span><br/>
                                                                        </c:when>
                                                                        <c:otherwise>
                                                                            <span class="label label-danger">Irreplaceable</span><br/>
                                                                        </c:otherwise>
                                                                    </c:choose>
                                                                    <c:if test="${component.isVersion2()}">
                                                                        <c:if test="${not empty component.getCertificateIdentifier()}">
                                                                            <span class="fieldHeader">Platform Certificate Issuer:</span>
                                                                            <span class="fieldValue">${component.getCertificateIdentifier().getIssuerDN()}</span><br />
                                                                            <span class="fieldHeader">Platform Certificate Serial Number:</span>
                                                                            <span class="fieldValue">${component.getCertificateIdentifier().getCertificateSerialNumber()}</span><br />
                                                                            <span class="fieldHeader">Platform Certificate URI:</span>  
                                                                        </c:if>
                                                                        <span class="fieldValue">
                                                                            <a href="${component.getComponentPlatformUri().getUniformResourceIdentifier()}">
                                                                                ${component.getComponentPlatformUri().getUniformResourceIdentifier()}
                                                                            </a>
                                                                        </span><br />
                                                                        <span class="fieldHeader">Status:</span>
                                                                        <span class="fieldValue">${component.getAttributeStatus()}</span><br/>
                                                                    </c:if>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </c:forEach>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </c:if>
                                <c:if test="${not empty initialData.componentsIdentifierURI}">
                                    <!-- Components Identifier URI -->
                                    <div class="panel panel-default">
                                        <div class="panel-heading" role="tab" id="headingTwo">
                                            <h4 class="panel-title">
                                                <a role="button" data-toggle="collapse" data-parent="#platformConfiguration" class="collapsed"
                                                   href="#componentIdentifierURIcollapse" aria-expanded="false" aria-controls="componentIdentifierURIcollapse">
                                                    Components Identifier URI
                                                </a>
                                            </h4>
                                        </div>
                                        <div id="componentIdentifierURIcollapse" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingTwo">
                                            <div class="panel-body">
                                                <div id="componentIdentifierURI" class="row">
                                                    <span class="fieldHeader">URI:</span>
                                                    <a href="${initialData.componentsIdentifierURI.getUniformResourceIdentifier()}">
                                                        ${initialData.componentsIdentifierURI.getUniformResourceIdentifier()}
                                                    </a>
                                                    <c:if test="${not empty initialData.componentsIdentifierURI.getHashAlgorithm()}">
                                                        <span class="fieldHeader">Hash Algorithm:</span>
                                                        <span>${initialData.componentsIdentifierURI.getHashAlgorithm()}</span>
                                                    </c:if>
                                                    <c:if test="${not empty initialData.componentsIdentifierURI.getHashValue()}">
                                                        <span class="fieldHeader">Hash Value:</span>
                                                        <span>${initialData.componentsIdentifierURI.getHashValue()}</span>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </c:if>
                                <c:if test="${not empty initialData.platformProperties}">
                                    <!-- Platform Properties -->
                                    <div class="panel panel-default">
                                        <div class="panel-heading" role="tab" id="headingThree">
                                            <h4 class="panel-title">
                                                <a role="button" data-toggle="collapse" data-parent="#platformConfiguration" class="collapsed"
                                                   href="#platformPropertiescollapse" aria-expanded="false" aria-controls="platformPropertiescollapse">
                                                    Platform Properties
                                                </a>
                                            </h4>
                                        </div>
                                        <div id="platformPropertiescollapse" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingThree">
                                            <div class="panel-body">
                                                <div id="platformProperties" class="row">
                                                    <c:forEach items="${initialData.platformProperties}" var="property">
                                                        <div class="component col col-md-4">
                                                            <div class="panel panel-default">
                                                                <div class="panel-body">
                                                                    <span class="fieldHeader">Name:</span>
                                                                    <span class="fieldValue">${property.getPropertyName()}</span><br/>
                                                                    <span class="fieldHeader">Value:</span>
                                                                    <span class="fieldValue">${property.getPropertyValue()}</span><br/>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </c:forEach>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </c:if>
                                <c:if test="${not empty initialData.platformPropertiesURI}">
                                    <!-- Platform Properties URI -->
                                    <div class="panel panel-default">
                                        <div class="panel-heading" role="tab" id="headingFour">
                                            <h4 class="panel-title">
                                                <a role="button" data-toggle="collapse" data-parent="#platformConfiguration" class="collapsed"
                                                   href="#platformPropertiesURIcollapse" aria-expanded="false" aria-controls="platformPropertiesURIcollapse">
                                                    Platform Properties URI
                                                </a>
                                            </h4>
                                        </div>
                                        <div id="platformPropertiesURIcollapse" class="panel-collapse collapse" role="tabpanel" aria-labelledby="headingFour">
                                            <div class="panel-body">
                                                <div id="platformPropertiesURI" class="row">
                                                    <span class="fieldHeader">URI:</span>
                                                    <a href="${initialData.platformPropertiesURI.getUniformResourceIdentifier()}">
                                                        ${initialData.platformPropertiesURI.getUniformResourceIdentifier()}
                                                    </a>
                                                    <c:if test="${not empty initialData.platformPropertiesURI.getHashAlgorithm()}">
                                                        <span class="fieldHeader">Hash Algorithm:</span>
                                                        <span>${initialData.platformPropertiesURI.getHashAlgorithm()}</span>
                                                    </c:if>
                                                    <c:if test="${not empty initialData.platformPropertiesURI.getHashValue()}">
                                                        <span class="fieldHeader">Hash Value:</span>
                                                        <span>${initialData.platformPropertiesURI.getHashValue()}</span>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </c:if>
                            </div><!-- close platformConfiguration -->
                        </div> <!-- Close row -->
                    </c:if>
                </c:when>
                <c:when test="${param.type=='issued'}">
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">System Information</span></div>
                        <div id="subjectAltName" class="col col-md-8">
                            <div id="manufacturer">Manufacturer:&nbsp;<span>${initialData.manufacturer}</span></div>
                            <div id="model">Model:&nbsp;<span>${initialData.model}</span></div>
                            <div id="version">Version:&nbsp;<span>${initialData.version}</span></div>
                            <div id="serial">Serial Number:&nbsp;<span>${initialData.platformSerial}</span></div>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Policy Reference</span></div>
                        <div id="policyReference" class="col col-md-8 vertical">
                            <c:choose>
                                <c:when test="${not empty initialData.policyReference}">
                                    ${initialData.policyReference}
                                </c:when>
                                <c:otherwise>
                                    Not Specified
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <c:if test="${initialData.crlPoints}">
                        <div class="row">
                            <div class="col-md-1 col-md-offset-1"><span class="colHeader">Revocation Locator</span></div>
                            <div id="revocationLocator" class="col col-md-8"><a href="${initialData.crlPoints}">${initialData.crlPoints}</div>
                        </div>
                    </c:if>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Endorsement Credential</span></div>
                        <div id="endorsementID" class="col col-md-8">
                            <c:if test="${not empty initialData.endorsementID}">
                                <a href="${portal}/certificate-details?id=${initialData.endorsementID}&type=endorsement">
                                    <img src="${icons}/ic_vpn_key_black_24dp.png">
                                </a>
                            </c:if>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">Platform Credentials</span></div>
                        <div id="platformID" class="col col-md-8">
                            <c:if test="${not empty initialData.platformID}">
                                <c:forTokens items = "${initialData.platformID}" delims = "," var = "pcID">
                                    <a href="${portal}/certificate-details?id=${pcID}&type=platform">
                                        <img src="${icons}/ic_important_devices_black_24dp.png">
                                    </a>
                                </c:forTokens>
                            </c:if>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">TCG Platform Specification Version</span></div>
                        <div id="majorVersion" class="col col-md-8 vertical">${initialData.majorVersion}.${initialData.minorVersion}.${initialData.revisionLevel}</div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1"><span class="colHeader">TCG Credential Specification Version</span></div>
                        <div id="majorVersion" class="col col-md-8 vertical">${initialData.tcgMajorVersion}.${initialData.tcgMinorVersion}.${initialData.tcgRevisionLevel}</div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1">
                            <span class="colHeader">
                                <a role="button" data-toggle="collapse" class="collapsed" href="#tpmSpecificationInner"
                                   aria-expanded="true" data-placement="top" aria-controls="tpmSpecificationInner">
                                    TPM Specification
                                </a>
                            </span>
                        </div>
                        <div id="tpmSpecification" class="col col-md-8">
                            <div id="tpmSpecificationInner" class="panel-body collapse" role="tabpanel" aria-expanded="false">
                                <div>Family:&nbsp;<span>${initialData.TPMSpecificationFamily}</span></div>
                                <div>Level:&nbsp;<span>${initialData.TPMSpecificationLevel}</span></div>
                                <div>Revision:&nbsp;<span>${initialData.TPMSpecificationRevision}</span></div>
                            </div>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-1 col-md-offset-1">
                            <span class="colHeader">
                                <a role="button" data-toggle="collapse" class="collapsed" href="#tpmSecurityAssertionInner"
                                   aria-expanded="true" data-placement="top" aria-controls="tpmSecurityAssertionInner">
                                    TPM Security Assertion
                                </a>
                            </span>
                        </div>
                        <div id="tpmSecurityAssertion" class="col col-md-8">
                            <div id="tpmSecurityAssertionInner" class="panel-body collapse" role="tabpanel" aria-expanded="false">
                                <div>Version:&nbsp;<span>${initialData.TPMSecurityAssertionsVersion}</span></div>
                                <div>Field Upgradeable:&nbsp;<span>${initialData.TPMSecurityAssertionsFieldUpgradeable}</span></div>
                                <div>ek Generation Type:&nbsp;<span>${initialData.TPMSecurityAssertionsEkGenType}</span></div>
                                <div>ek Generation Location:&nbsp;<span>${initialData.TPMSecurityAssertionsEkGenLoc}</span></div>
                                <div>ek Certificate Generation Location:&nbsp;<span>${initialData.TPMSecurityAssertionsEkCertGenLoc}</span></div>
                            </div>
                        </div>
                    </div>
                </c:when>
            </c:choose>
        </div>
        <script>
            $(document).ready(function () {
                var type = "${param.type}";
                var signature = ${initialData.signature};
                var serialNumber = '${initialData.serialNumber}';
                var authorityKeyIdentifier = '${initialData.authKeyId}';
                var authoritySerialNumber = '${initialData.authSerialNumber}';

                //Format validity time
                $("#validity span").each(function () {
                    $(this).text(formatDateTime($(this).text()));
                });

                //Convert byte array to string
                $("#signature").html(byteToHexString(signature));

                //Convert byte array to string
                $("#serialNumber").html(parseSerialNumber(serialNumber));

                // authority key ID
            <c:choose>
                <c:when test="${not empty initialData.authKeyId}">
                $("#authorityKeyIdentifier").html(parseSerialNumber(authorityKeyIdentifier));
                </c:when>
                <c:otherwise>
                $("#authorityKeyIdentifier").html("Not Specified");
                </c:otherwise>
            </c:choose>

            <c:if test="${not empty initialData.authSerialNumber}">
                //Convert string to serial String
                $("#authSerialNumber").html(parseSerialNumber(authoritySerialNumber));
            </c:if>
            <c:choose>
                <c:when test="${not empty initialData.publicKeyValue}">
                var publicKey = '${initialData.publicKeyValue}';
                $("#encodedPublicKey").html(parseHexString(publicKey));
                </c:when>
                <c:otherwise>
                    <c:if test="${not empty initialData.encodedPublicKey}">
                //Change public key byte to hex
                var encPublicKey = ${initialData.encodedPublicKey};
                $("#encodedPublicKey").html(byteToHexString(encPublicKey));
                    </c:if>
                </c:otherwise>
            </c:choose>

            <c:if test="${not empty initialData.subjectKeyIdentifier}">
                //Change subject byte to hex only for CACertificate
                if (type === "certificateauthority") {
                    var subjectKeyIdentifier = ${initialData.subjectKeyIdentifier};
                    $("#subjectKeyIdentifier").html(byteToHexString(subjectKeyIdentifier));
                }
            </c:if>            

                //Initilize tooltips
                $('[data-toggle="tooltip"]').tooltip();

                //Vertical alignment on data columns
                $('.vertical').each(function () {
                    $(this).css({
                        'line-height': $(this).height() + 'px'
                    });
                });

                //Change link width
                $("#headingOne, #headingTwo, #headingThree").each(function (e) {
                    var width = $(this).width();
                    //Get link width
                    var linkWidth = $(this).find('a').width();

                    //Change width for the link
                    $(this).find('a').css({
                        "padding-right": (width - linkWidth) + "px"
                    });
                });
            });
        </script>
    </jsp:body>

</my:page>
