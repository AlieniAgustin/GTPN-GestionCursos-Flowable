package com.backend.proxy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.backend.proxy.entity.ProposalDocument;

/**
 * Una versión de la propuesta tal como se devuelve dentro del curso.
 *
 * <p>No incluye los bytes del PDF: con varias versiones la respuesta se haría pesada.
 * En su lugar trae {@code pdfUrl}, la ruta donde se descarga ese PDF.
 */
public record ProposalSummaryDTO(
    int version,
    String program,
    BigDecimal budget,
    List<LocalDate> proposedDates,
    String createdBy,
    LocalDateTime createdAt,
    String pdfUrl
) {

    public static ProposalSummaryDTO from(Long courseId, ProposalDocument proposal) {
        return new ProposalSummaryDTO(
                proposal.getVersion(),
                proposal.getProgram(),
                proposal.getBudget(),
                List.copyOf(proposal.getProposedDates()),
                proposal.getCreatedBy(),
                proposal.getCreatedAt(),
                "/mis-cursos/" + courseId + "/propuestas/" + proposal.getVersion() + ".pdf");
    }
}
