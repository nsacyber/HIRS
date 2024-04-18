//Convert a byte to HEX
function byteToHexString(arr){
    var str = "";
    $.each(arr, function(index, value){
        str += ('0' + (value & 0xFF).toString(16)).slice(-2) + ":​";
    });
    return (str.substring(0, str.length - 2)).toUpperCase();
}

//Parse hex string for display
function parseHexString(hexString) {
    var str = hexString.toUpperCase();
    //Do not parse if there is 2 characters
    if(str.length === 2) {
        return str;
    }
    return str.match(/.{2}/g).join(':​');
}

//Parse the HEX string value to display as byte hex string
function parseSerialNumber(hexString){
    var str = hexString.toUpperCase();
    if(str.length % 2 !== 0) {
        str = '0' + hexString;
    }
    //Do not parse if there is 2 characters
    if(str.length === 2) {
        return str;
    }
    //Parse and return
    return newString = str.match(/.{2}/g).join(':​');

}

/**
* Handles user request to delete a cert. Prompts user to confirm.
* Upon confirmation, submits the delete form which is required to make
* a POST call to delete the credential.
*/
function handleDeleteRequest(id) {
   if (confirm("Delete certificate?")) {
       $('#deleteForm' + id).submit();
   }
}

/**
* Handles user request to delete a cert. Prompts user to confirm.
* Upon confirmation, submits the delete form which is required to make
* a POST call to delete the reference integrity manifest.
*/
function handleRimDeleteRequest(id) {
   if (confirm("Delete RIM?")) {
       $('#deleteForm' + id).submit();
   }
}

/**
* Set the data tables using the columns definition, the ajax URL and
* the ID of the table.
* @param id of the data table
* @param url for the AJAX call
* @param columns definition of the table to render
* 
*/
function setDataTables(id, url, columns) {
    var dtable = $(id).DataTable({
        processing: true,
        serverSide: true,
        ajax: {
            url: url,
            dataSrc: function (json) {
                formatElementDates('.date');
                return json.data;
            }
        },
        columns: columns
    });

    return dtable;
}

/**
* Create a certificate details like for the specified certificate
* type and ID with the corresponding icon.
* @param type of certificate
* @param id of the certificate
* @param sameType boolean indicating if the details is the same
*       certificate type.
*/
function certificateDetailsLink(type, id, sameType){
    var href = portal + '/certificate-details?id=' + id + '&type=' + type;
    var title = ""; 
    var icon = icons;

    //If the details is the same certificate type use assignment icon,
    //otherwise use the icon for the certificate type.
    if(sameType){
        title = "Details"; 
        icon += '/ic_assignment_black_24dp.png';
    } else {
        switch(type){
            case "issued":
                icon += "/ic_library_books_black_24dp.png";
                title="View Issued Attestation Certificate Details"
                break;
            case "platform":
                icon += "/ic_important_devices_black_24dp.png";
                title = "View Platform Certificate Details";
                break;
            case "endorsement":
                icon += "/ic_vpn_key_black_24dp.png";
                title = "View Endorsement Certificate Details";
                break;
            case "idevid":
                icon += "/ic_vpn_key_black_24dp.png";
                title = "View IDevID Certificate Details";
                break;
        }
    }
    var html = '<a href=' + href + '>'
                + '<img src="' + icon + '" title="' + title + '"></a>';
    return html;
}

/**
* Create a RIM details like for the specified rim.
* type and ID with the corresponding icon.
* @param id of the rim
*/
function rimDetailsLink(id){
    var href = portal + '/rim-details?id=' + id;
    var title = ""; 
    var icon = icons;

    title = "Details"; 
    icon += '/ic_assignment_black_24dp.png';

    var html = '<a href=' + href + '>'
                + '<img src="' + icon + '" title="' + title + '"></a>';
    return html;
}

/**
* Create a certificate delete link for the specified ID
* @param id of the certificate
* @param pagePath path to the link
*/
function certificateDeleteLink(id, pagePath){
    var icon = icons + '/ic_delete_black_24dp.png';
    var formURL = pagePath + "/delete";

    var html = '<a href="#!" onclick="handleDeleteRequest(\'' + id + '\')">'
               + '<img src="' + icon + '" title="Delete"></a>'
               + '<form id="deleteForm' + id + '" action="' + formURL + '" method="post">'
               + '<input name="id" type="hidden" value="' + id + '"></form>';
    return html;
}

/**
* Create a RIM delete link for the specified ID
* @param id of the RIM
* @param pagePath path to the link
*/
function rimDeleteLink(id, pagePath){
    var icon = icons + '/ic_delete_black_24dp.png';
    var formURL = pagePath + "/delete";

    var html = '<a href="#!" onclick="handleRimDeleteRequest(\'' + id + '\')">'
               + '<img src="' + icon + '" title="Delete"></a>'
               + '<form id="deleteForm' + id + '" action="' + formURL + '" method="post">'
               + '<input name="id" type="hidden" value="' + id + '"></form>';
    return html;
}

/**
* Create a certificate download link for the specified ID
* @param id of the certificate
* @param pagePath path to the link
*/
function certificateDownloadLink(id, pagePath){
    var icon = icons + '/ic_file_download_black_24dp.png';
    var href = pagePath + '/download?id=' + id;
    
    var html = '<a href="' + href + '">'
            + '<img src="' + icon + '" title="Download Certificate"></a>';

    return html;
}

/**
* Create a rim download link for the specified ID
* @param id of the rim
* @param pagePath path to the link
*/
function rimDownloadLink(id, pagePath){
    var icon = icons + '/ic_file_download_black_24dp.png';
    var href = pagePath + '/download?id=' + id;
    
    var html = '<a href="' + href + '">'
            + '<img src="' + icon + '" title="Download Reference Integrity Manifest"></a>';

    return html;
}

/**
* Formats a given date to a UTC string, or returns an indefinite icon
* @param date to format
*/
function formatCertificateDate(dateText) {
    var date = +dateText; // Convert to numeric

    if (date == 253402300799000)
    {
        return 'Indefinite';
    }

    return new Date(date).toUTCString();
}
