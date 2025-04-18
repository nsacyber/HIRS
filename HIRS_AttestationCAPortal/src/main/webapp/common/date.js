/*
 * Date formatting and parsing functions.
 *
 * Requires moment.js
 */

let dateTimeOutputFormat = "YYYY-MM-DD HH:mm:ss";

let dateInputFormat = ["YYYY-MM-DD","YYYY/MM/DD","MM-DD-YYYY","MM/DD/YYYY"];
let dateOutputFormat = "YYYY-MM-DD";

let timeInputFormat = "HH:mm:ss.SSS";
let timeOutputFormat = "HH:mm";

function parseDate(dt, inputFormat) {
    return moment(dt, inputFormat);
}

function formatDateTime(dt, format, inputFormat) {
    if (!dt) {
        return '';
    } else {
        return moment(dt, inputFormat).local().format(format ? format : dateTimeOutputFormat);
    }
}

function formatDate(dt) {
    return formatDateTime(dt, dateOutputFormat, dateInputFormat);
}

function formatTime(dt) {
    return formatDateTime(dt, timeOutputFormat, timeInputFormat);
}

function formatElementDateTime(e, format, inputFormat) {
    if ($(e).is(":input")) {
        $(e).val(formatDateTime($(e).val(), format, inputFormat));
    } else {
        $(e).text(formatDateTime($(e).text(), format, inputFormat));
    }
}

function formatElementDate(e) {
    formatElementDateTime(e, dateOutputFormat);
}

function formatElementTime(e) {
    formatElementDateTime(e, timeOutputFormat, timeInputFormat);
}

function formatElementDateTimes(selector, format, inputFormat) {
    $(selector).each(function (i, e) {
        formatElementDateTime(e, format, inputFormat);
    });
}

function formatElementDates(selector) {
    formatElementDateTimes(selector, dateOutputFormat);
}

function formatElementTimes(selector) {
    formatElementDateTimes(selector, timeOutputFormat, timeInputFormat);
}

$(function () {
    formatElementDateTimes(".datetime");
    formatElementDates(".date");
    formatElementTimes(".time");
});