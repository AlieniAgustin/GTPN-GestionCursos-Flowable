package com.backend.proxy.dto.task;

/**
 * Query payload contract for 'enviarinformeaempresayarchivarexpediente'.
 * Shows the administrative lead the completed report and client company info prior to final dispatch.
 */
public record SendReportDataDTO(
    String courseName,
    String companyName,
    String companyEmail,
    String report
) {}
