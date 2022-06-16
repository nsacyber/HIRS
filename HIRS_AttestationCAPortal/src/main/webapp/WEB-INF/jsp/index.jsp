<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%-- JSP TAGS --%>
<%@taglib prefix="my" tagdir="/WEB-INF/tags"%>

<%-- CONTENT --%>
<my:page>
    <jsp:attribute name="pageHeaderTitle">Welcome to the HIRS Attestation CA</jsp:attribute>

    <jsp:body>
        <div id="index-page" class="container-fluid">
            <div class="row">
                <div class="col col-md-6 index-left-side">
                    <div class="row">
                        <div class="col col-md-12">
                            <h1>Configuration</h1>
                        </div>
                    </div>
                    <h3>
                        <a href="${portal}/policy">
                            <img src="${icons}/ic_store_black_24dp.png" /> Policy
                        </a>
                    </h3>
                    <h4>Configure Identity CA and Supply Chain validation policies.</h4>
                    <h3>
                        <a href="${certificateRequest}/trust-chain">
                            <img src="${icons}/ic_subtitles_black_24dp.png" /> Trust Chain Management
                        </a>
                    </h3>
                    <h4>Upload, view and manage CA certificates that complete trust chains for hardware credentials.</h4>
                    <h3>
                        <a href="${certificateRequest}/platform-credentials">
                            <img src="${icons}/ic_important_devices_black_24dp.png" /> Platform Certificates
                        </a>
                    </h3>
                    <h4>Upload, view and manage platform credentials.</h4>
                    <h3>
                        <a href="${certificateRequest}/endorsement-key-credentials">
                            <img src="${icons}/ic_vpn_key_black_24dp.png" /> Endorsement Certificates
                        </a>
                    </h3> 
                    <h4>Upload, view and manage endorsement credentials.</h4>
                    <h3>
                        <a href="${portal}/reference-manifests">
                            <img src="${icons}/ic_important_devices_black_24dp.png" /> Reference Integrity Manifests
                        </a>
                    </h3> 
                    <h4>Upload, view and manage reference integrity manifests.</h4>
                </div>
                <div class="col col-md-6 index-right-side">
                    <div class="row">
                        <div class="col col-md-12">
                            <h1>Status</h1>
                        </div>
                    </div>
                    <h3>
                        <a href="${certificateRequest}/issued-certificates">
                            <img src="${icons}/ic_library_books_black_24dp.png" /> Issued Certificates
                        </a>
                    </h3>        
                    <h4>View Certificates issued by this CA</h4>
                    <h3>
                        <a href="${portal}/validation-reports"><img src="${icons}/ic_assignment_black_24dp.png" /> Validation Reports</a>
                    </h3> 
                    <h4>View a list of device validations carried out by this CA.</h4>
                    <h3>
                        <a href="${portal}/devices"><img src="${icons}/ic_devices_black_24dp.png" /> Devices</a>
                    </h3>  
                    <h4>View devices covered by this CA for supply chain validation.</h4>
                    <h3>
                        <a href="${portal}/rim-database"><img src="${icons}/ic_devices_black_24dp.png" /> RIM Database</a>
                    </h3>
                    <h4>View a list of Reference Integrity Measurements</h4>
                </div>
            </div>
        </div>
    </jsp:body>
</my:page>
