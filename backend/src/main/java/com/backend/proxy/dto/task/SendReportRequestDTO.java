package com.backend.proxy.dto.task;

/**
 * Command payload contract for sending the report to the client and archiving the file.
 */
public record SendReportRequestDTO(
    boolean confirmSend,
    String archiveNotes
) {}
