<%@ page contentType="text/html"%>
<%@ page pageEncoding="UTF-8"%><%-- JSP TAGS--%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%><%-- CONTENT--%>
<my:page>
    <jsp:attribute name="pageHeaderTitle">Attestation Identity CA Policy Options</jsp:attribute>

    <jsp:body>
        <ul><%-- Endorsement validation--%>
            <div class="aca-input-box">
                <form:form method="POST" modelAttribute="initialData" action="policy/update-ec-validation">
                    <li>Endorsement Credential Validation: ${initialData.enableEcValidation ? 'Enabled' : 'Disabled'}
                        <my:editor id="ecPolicyEditor" label="Edit Settings">
                            <div class="radio">
                                <label><input id="ecTop" type="radio" name="ecValidate" ${initialData.enableEcValidation ? 'checked' : ''} value="checked"/> Endorsement Credentials will be validated</label>
                            </div>
                            <div class="radio">
                                <label><input id="ecBot" type="radio" name="ecValidate" ${initialData.enableEcValidation ? '' : 'checked'} value="unchecked"/> Endorsement Credentials will not be validated</label>
                            </div>
                        </my:editor>
                    </li>
                </form:form>
            </div>
            <%-- Platform validation--%>
            <div class="aca-input-box">
                <form:form method="POST" modelAttribute="initialData" action="policy/update-pc-validation">
                    <li>Platform Credential Validation: ${initialData.enablePcCertificateValidation ? 'Enabled' : 'Disabled'}
                        <my:editor id="pcPolicyEditor" label="Edit Settings">
                            <div class="radio">
                                <label><input id="pcTop" type="radio" name="pcValidate" ${initialData.enablePcCertificateValidation ? 'checked' : ''} value="checked"/> Platform Credentials will be validated</label>
                            </div>
                            <div class="radio">
                                <label><input id="pcBot" type="radio" name="pcValidate" ${initialData.enablePcCertificateValidation ? '' : 'checked'} value="unchecked"/> Platform Credentials will not be validated</label>
                            </div>
                        </my:editor>
                    </li>
                </form:form>
            </div><%-- Platform attribute validation--%>
            <div class="aca-input-box">
                <form:form method="POST" modelAttribute="initialData" action="policy/update-pc-attribute-validation">
                    <ul>
                    <li>Platform Attribute Credential Validation: ${initialData.enablePcCertificateAttributeValidation ? 'Enabled' : 'Disabled'}
                        <my:editor id="pcAttributePolicyEditor" label="Edit Settings">
                            <div class="radio">
                                <label><input id="pcAttrTop" type="radio" name="pcAttributeValidate" ${initialData.enablePcCertificateAttributeValidation ? 'checked' : ''} value="checked"/> Platform Credential Attributes will be validated</label>
                            </div>
                            <div class="radio">
                                <label><input id="pcAttrBot" type="radio" name="pcAttributeValidate" ${initialData.enablePcCertificateAttributeValidation ? '' : 'checked'} value="unchecked"/> Platform Credential Attributes will not be validated</label>
                            </div>
                        </my:editor>
                    </li>
                </form:form>
            </div>
            <%-- Generate Attestation Certificate--%>
            <div class="aca-input-box">
                <br />
                <form:form method="POST" modelAttribute="initialData" action="policy/update-issue-attestation">
                    <li>Generate Attestation Certificate: ${(initialData.issueAttestationCertificate || initialData.generateOnExpiration) ? 'Enabled' : 'Disabled'}
                        <my:editor id="issuedCertificatePolicyEditor" label="Edit Settings">
                            <div class="radio">
                                <label><input id="aicTop" type="radio" name="attestationCertificateIssued" ${initialData.issueAttestationCertificate ? '' : 'checked'} value="unchecked"/> Never generate an Attestation Certificate</label>
                            </div>
                            <div class="radio">
                                <label><input id="aicMid" type="radio" name="attestationCertificateIssued" ${initialData.issueAttestationCertificate ? 'checked' : ''} value="checked"/> Always generate an Attestation Certificate</label>
                            </div>
                            <div class="radio">
                                <label>
                                    <input id="aicBot" type="radio" name="attestationCertificateIssued" ${initialData.generateOnExpiration ? 'checked' : ''} value="expires"/> Only Generate when current Attestation Certificate expires<br /> 
                                    ** Validity period for the Attestation Certificate
                                    <input id="validLen" type="text" name="numOfValidDays" value="3650" size="6" maxlength="6" ${(initialData.generateOnExpiration) ? '' : 'disabled'} />
                                </label>
                            </div>
                        </my:editor>
                    </li>
                    </ul>
                </form:form>
            </div>
            
            <div class="aca-input-box">
                <form:form method="POST" modelAttribute="initialData" action="policy/update-firmware-validation">
                    <li>Firmware Validation: ${initialData.enableFirmwareValidation ? 'Enabled' : 'Disabled'}
                        <my:editor id="firmwarePolicyEditor" label="Edit Settings">
                            <div class="radio">
                                <label><input id="firmwareTop" type="radio" name="fmValidate" ${initialData.enableFirmwareValidation ? 'checked' : ''} value="checked"/> Firmware will be validated</label>
                            </div>
                            <div class="radio">
                                <label><input id="firmwareBot" type="radio" name="fmValidate" ${initialData.enableFirmwareValidation ? '' : 'checked'}  value="unchecked"/> Firmware will not be validated</label>
                            </div>
                        </my:editor>
                    </li>
                </form:form>
            </div>
        </ul>
    </jsp:body>
</my:page>
