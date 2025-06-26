<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="c" uri="jakarta.tags.core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>
<my:page>
    <jsp:attribute name="pageHeaderTitle">Attestation Identity CA Policy Options</jsp:attribute>

    <jsp:body>
        <ul>
            <%-- Endorsement validation --%>
            <div class="aca-input-box">
                <form:form method="POST" modelAttribute="initialData" action="policy/update-ec-validation">
                    <li>Endorsement Credential Validation: ${initialData.enableEcValidation ? 'Enabled' : 'Disabled'}
                        <my:editor id="ecPolicyEditor" label="Edit Settings">
                            <div class="radio">
                                <label><input id="ecTop" type="radio" name="ecValidate" ${initialData.enableEcValidation ? 'checked' : ''}  value="checked"/> Endorsement Credentials will be validated</label>
                            </div>
                            <div class="radio">
                                <label><input id="ecBot" type="radio" name="ecValidate" ${initialData.enableEcValidation ? '' : 'checked'} value="unchecked"/> Endorsement Credentials will not be validated</label>
                            </div>
                        </my:editor>
                    </li>
                </form:form>
            </div>

            <%-- Platform validation --%>
            <div class="aca-input-box">
                <form:form method="POST" modelAttribute="initialData" action="policy/update-pc-validation">
                    <li>Platform Credential Validation: ${initialData.enablePcCertificateValidation ? 'Enabled' : 'Disabled'}
                        <my:editor id="pcPolicyEditor" label="Edit Settings">
                            <div class="radio">
                                <label><input id="pcTop" type="radio" name="pcValidate" ${initialData.enablePcCertificateValidation ? 'checked' : ''} value="checked"/> Platform Credentials will be validated</label>
                            </div>
                            <div class="radio">
                                <label><input id="pcBot" type="radio" name="pcValidate" ${initialData.enablePcCertificateValidation ? '' : 'checked'}  value="unchecked"/> Platform Credentials will not be validated</label>
                            </div>
                        </my:editor>
                    </li>
                </form:form>
            </div>

            <%-- Platform attribute validation --%>
            <div class="aca-input-box">
                <form:form method="POST" modelAttribute="initialData" action="policy/update-pc-attribute-validation">
                    <ul>
                        <li>Platform Attribute Credential Validation: ${initialData.enablePcCertificateAttributeValidation ? 'Enabled' : 'Disabled'}
                            <my:editor id="pcAttributePolicyEditor" label="Edit Settings">
                                <div class="radio">
                                    <label><input id="pcAttrTop" type="radio" name="pcAttributeValidate" ${initialData.enablePcCertificateAttributeValidation ? 'checked' : ''} value="checked"/> Platform Credential Attributes will be validated</label>
                                </div>
                                <div class="radio">
                                    <label><input id="pcAttrBot" type="radio" name="pcAttributeValidate" ${initialData.enablePcCertificateAttributeValidation ? '' : 'checked'}  value="unchecked"/> Platform Credential Attributes will not be validated</label>
                                </div>
                            </my:editor>
                        </li>
                </form:form>
                        <form:form method="POST" modelAttribute="initialData" action="policy/update-revision-ignore">
                            <ul>
                                <li>Ignore Component Revision Attribute: ${initialData.enableIgnoreRevisionAttribute ? 'Enabled' : 'Disabled'}
                                    <my:editor id="ignoreRevisionPolicyEditor" label="Edit Settings">
                                        <div class="radio">
                                          <label><input id="revisionTop" type="radio" name="ignoreRevisionAttribute" ${initialData.enableIgnoreRevisionAttribute ? 'checked' : ''} value="checked"/> Ignore Component Revision Attribute enabled</label>
                                        </div>
                                        <div class="radio">
                                          <label><input id="revisionBot" type="radio" name="ignoreRevisionAttribute" ${initialData.enableIgnoreRevisionAttribute ? '' : 'checked'}  value="unchecked"/> Ignore Component Revision Attribute disabled</label>
                                        </div>
                                    </my:editor>
                                </li>
                            </ul>
                        </form:form>
                    </ul>
            </div>

            <%-- Firmware validation --%>
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
                    </form:form>
                    <ul>
                        <form:form method="POST" modelAttribute="initialData" action="policy/update-ima-ignore">
                            <li>Ignore IMA PCR Entry: ${initialData.enableIgnoreIma ? 'Enabled' : 'Disabled'}
                                <my:editor id="ignoreImaPolicyEditor" label="Edit Settings">
                                    <div class="radio">
                                        <label><input id="imaTop" type="radio" name="ignoreIma" ${initialData.enableIgnoreIma ? 'checked' : ''} value="checked"/> Ignore IMA enabled</label>
                                    </div>
                                    <div class="radio">
                                        <label><input id="imaBot" type="radio" name="ignoreIma" ${initialData.enableIgnoreIma ? '' : 'checked'}  value="unchecked"/> Ignore IMA disabled</label>
                                    </div>
                                </my:editor>
                            </li>
                        </form:form>
                        <form:form method="POST" modelAttribute="initialData" action="policy/update-tboot-ignore">
                            <li>Ignore TBOOT PCRs Entry: ${initialData.enableIgnoreTboot ? 'Enabled' : 'Disabled'}
                                <my:editor id="ignoreTbootPolicyEditor" label="Edit Settings">
                                    <div class="radio">
                                        <label><input id="tbootTop" type="radio" name="ignoretBoot" ${initialData.enableIgnoreTboot ? 'checked' : ''} value="checked"/> Ignore TBoot enabled</label>
                                    </div>
                                    <div class="radio">
                                        <label><input id="tbootBot" type="radio" name="ignoretBoot" ${initialData.enableIgnoreTboot ? '' : 'checked'}  value="unchecked"/> Ignore TBoot disabled</label>
                                    </div>
                                </my:editor>
                            </li>
                        </form:form>
                        <form:form method="POST" modelAttribute="initialData" action="policy/update-gpt-ignore">
                            <li>Ignore GPT PCRs Entry: ${initialData.enableIgnoreGpt ? 'Enabled' : 'Disabled'}
                                <my:editor id="ignoreGptPolicyEditor" label="Edit Settings">
                                    <div class="radio">
                                        <label><input id="gptTop" type="radio" name="ignoreGpt" ${initialData.enableIgnoreGpt ? 'checked' : ''} value="checked"/> Ignore GPT enabled</label>
                                    </div>
                                    <div class="radio">
                                        <label><input id="gptBot" type="radio" name="ignoreGpt" ${initialData.enableIgnoreGpt ? '' : 'checked'} value="unchecked"/> Ignore GPT disabled</label>
                                    </div>
                                </my:editor>
                            </li>
                        </form:form>
                        <form:form method="POST" modelAttribute="initialData" action="policy/update-os-evt-ignore">
                            <li>Ignore OS Events: ${initialData.enableIgnoreOsEvt ? 'Enabled' : 'Disabled'}
                                <my:editor id="ignoreOsEvtPolicyEditor" label="Edit Settings">
                                    <div class="radio">
                                        <label><input id="osTop" type="radio" name="ignoreOsEvt" ${initialData.enableIgnoreOsEvt ? 'checked' : ''} value="checked"/> Ignore Os Events enabled</label>
                                    </div>
                                    <div class="radio">
                                        <label><input id="osBot" type="radio" name="ignoreOsEvt" ${initialData.enableIgnoreOsEvt ? '' : 'checked'} value="unchecked"/> Ignore Os Events disabled</label>
                                    </div>
                                </my:editor>
                            </li>
                        </form:form>
                    </ul>
                </li>
            </div>
            <br />

            <%-- Generate Attestation Certificate--%>
            <div class="aca-input-box">
                <form:form method="POST" modelAttribute="initialData" action="policy/update-issued-attestation-generation">
                    <li>Generate Attestation Certificate: ${initialData.issueAttestationCertificate ? 'Enabled' : 'Disabled'}
                        <my:editor id="issuedCertificatePolicyEditor" label="Edit Settings">
                            <div class="radio">
                                <label><input id="aicTop" type="radio" name="attestationCertificateIssued" ${initialData.issueAttestationCertificate ? '' : 'checked'} value="unchecked"/> Never generate an Attestation Certificate</label>
                            </div>
                            <div class="radio">
                                <label><input id="aicMid" type="radio" name="attestationCertificateIssued" ${initialData.issueAttestationCertificate ? 'checked' : ''} value="checked"/> Conditionally generate an Attestation Certificate before 'Not After' expiration date</label>
                            </div>
                        </my:editor>
                </form:form>
                    <ul>
                        <form:form method="POST" modelAttribute="initialData" action="policy/update-attestation-certificate-expiration">
                            <li>Attestation Certificate Validity period: ${initialData.generateOnExpiration ? 'Enabled' : 'Disabled'}
                                <my:editor id="issuedCertificatePolicyExpirationEditor" label="Edit Settings">
                                    <div class="radio">
                                        <label>
                                            <input id="aicBot" type="checkbox" name="generationExpirationOn" ${initialData.generateOnExpiration ? 'checked' : ''} value="checked" />
                                                Attestation Certificate validity period (Default 3651 days)<br />
                                                Select period in days: <input id="expirationValue" type="text" name="expirationValue" value="${initialData.expirationValue}" />
                                        </label>
                                    </div>
                                </my:editor>
                            </li>
                        </form:form>
                        <form:form method="POST" modelAttribute="initialData" action="policy/update-issued-cert-threshold">
                            <li>Attestation Certificate Renewal period: ${initialData.generateOnExpiration ? 'Enabled' : 'Disabled'}
                                <my:editor id="issuedCertificatePolicyGenerateEditor" label="Edit Settings">
                                    <div class="radio">
                                        <label>
                                            <input id="aicBot" type="checkbox" name="generationExpirationOn" ${initialData.generateOnExpiration ? 'checked' : ''} value="checked" />
                                                Renew 'n' days before Attestation Certificate's  'Not After' Validity date (Default 365 days)<br />
                                                Select 'n' period in days: <input id="thresholdValue" type="text" name="thresholdValue" value="${initialData.thresholdValue}" />
                                        </label>
                                    </div>
                                </my:editor>
                            </li>
                        </form:form>
                    </ul>
                </li>
            </div>
            <br />

            <%-- Generate LDevID Certificate--%>
            <div class="aca-input-box">
                <form:form method="POST" modelAttribute="initialData" action="policy/update-issued-ldevid-generation">
                    <li>Generate LDevID Certificate: ${initialData.issueDevIdCertificate ? 'Enabled' : 'Disabled'}
                        <my:editor id="issuedDevIdCertificatePolicyEditor" label="Edit Settings">
                            <div class="radio">
                                <label><input id="devIdTop" type="radio" name="devIdCertificateIssued" ${initialData.issueDevIdCertificate ? '' : 'checked'} value="unchecked"/> Never generate a DevID Certificate</label>
                            </div>
                            <div class="radio">
                                <label><input id="devIdMid" type="radio" name="devIdCertificateIssued" ${initialData.issueDevIdCertificate ? 'checked' : ''} value="checked"/> Conditionally generate an DevID Certificate before 'Not After' expiration date</label>
                            </div>
                        </my:editor>
                </form:form>
                    <ul>
                        <form:form method="POST" modelAttribute="initialData" action="policy/update-ldevid-certificate-expiration">
                            <li>LDevID Certificate Validity period: ${initialData.devIdExpirationFlag ? 'Enabled' : 'Disabled'}
                                <my:editor id="issuedDevIdCertificatePolicyExpirationEditor" label="Edit Settings">
                                    <div class="radio">
                                        <label>
                                        <input id="devIdBot" type="checkbox" name="devIdExpirationChecked" ${initialData.devIdExpirationFlag ? 'checked' : ''} value="checked" />
                                        LDevID Certificate validity period (Default 3651 days)<br />
                                        Select period in days: <input id="devIdExpirationValue" type="text" name="devIdExpirationValue" value="${initialData.devIdExpirationValue}" />
                                        </label>
                                    </div>
                                </my:editor>
                            </li>
                        </form:form>
                        <form:form method="POST" modelAttribute="initialData" action="policy/update-ldevid-threshold">
                            <li>LDevID Certificate Renewal period: ${initialData.devIdExpirationFlag ? 'Enabled' : 'Disabled'}
                                <my:editor id="issuedDevIdCertificatePolicyGenerateEditor" label="Edit Settings">
                                    <div class="radio">
                                        <label>
                                        <input id="devIdBot" type="checkbox" name="devIdExpirationChecked" ${initialData.devIdExpirationFlag ? 'checked' : ''} value="checked" />
                                        Renew 'n' days before LDevID Certificate's  'Not After' Validity date (Default 365 days)<br />
                                        Select 'n' period in days: <input id="devIdThresholdValue" type="text" name="devIdThresholdValue" value="${initialData.devIdThresholdValue}" />
                                        </label>
                                    </div>
                                </my:editor>
                            </li>
                        </form:form>
                    </ul>
                </li>
            </div>
            <br />

            <%-- Save ProtoBuf Data To ACA Log After Validation --%>
            <div class="aca-input-box">
                <form:form method="POST" modelAttribute="initialData" action="policy/update-save-protobuf-data-to-log">
                    <li>Save Protobuf Data To ACA Log: ${initialData.enableSaveProtobufToLog ? 'Enabled' : 'Disabled'}
                        <my:editor id="saveProtoBufDataPolicyEditor" label="Edit Settings">
                            <div class="radio">
                                <label><input id="protoTop" type="radio" name="saveProtobufToLogValue" ${initialData.enableSaveProtobufToLog ? 'checked' : ''}  value="checked"/> ProtoBuf Data Will Be Saved To The ACA Log After Validation </label>
                            </div>
                            <div class="radio">
                                <label><input id="protoBot" type="radio" name="saveProtobufToLogValue" ${initialData.enableSaveProtobufToLog ? '' : 'checked'} value="unchecked"/> ProtoBuf Data Will Not Be Saved To The ACA Log After Validation</label>
                            </div>
                            </my:editor>
                </form:form>
                <ul>
                    <form:form method="POST" modelAttribute="initialData" action="policy/update-save-protobuf-data-on-success-failure">
                        <li>Save Protobuf data to ACA log on failed validations: ${initialData.enableSaveProtobufToLogOnFailedVal ? 'Enabled' : 'Disabled'}
                                <my:editor id="saveProtoBufDataOnFailedValPolicyEditor" label="Edit Settings">
                                    <div class="radio">
                                        <label><input id="protoFailValTop" type="radio" name="saveFailedProtobufToLogValue" ${initialData.enableSaveProtobufToLogOnFailedVal ? 'checked' : ''}  value="checked"/> ProtoBuf Data Will Be Saved To The ACA Log After Failed Validations </label>
                                    </div>
                                    <div class="radio">
                                        <label><input id="protoFailValBot" type="radio" name="saveFailedProtobufToLogValue" ${initialData.enableSaveProtobufToLogOnFailedVal ? '' : 'checked'} value="unchecked"/> ProtoBuf Data Will Not Be Saved To The ACA Log After Failed Validations</label>
                                    </div>
                            </my:editor>
                        </li>
                    </form:form>
                    <form:form method="POST" modelAttribute="initialData" action="policy/update-save-protobuf-data-on-success-failure">
                        <li>Save Protobuf data to ACA log on successful validations: ${initialData.enableSaveProtobufToLogOnSuccessVal ? 'Enabled' : 'Disabled'}
                            <my:editor id="saveProtoBufDataOnSuccessfulValPolicyEditor" label="Edit Settings">
                                <div class="radio">
                                    <label><input id="protoSuccessValTop" type="radio" name="saveSuccessProtobufToLogValue" ${initialData.enableSaveProtobufToLogOnSuccessVal ? 'checked' : ''}  value="checked"/> ProtoBuf Data Will Be Saved To The ACA Log After Successful Validations </label>
                                </div>
                                <div class="radio">
                                    <label><input id="protoSuccessValBot" type="radio" name="saveSuccessProtobufToLogValue" ${initialData.enableSaveProtobufToLogOnSuccessVal ? '' : 'checked'} value="unchecked"/> ProtoBuf Data Will Not Be Saved To The ACA Log After Successful Validations</label>
                                </div>
                            </my:editor>
                        </li>
                    </form:form>
                </ul>
            </div>
        </ul>
    </jsp:body>
</my:page>
