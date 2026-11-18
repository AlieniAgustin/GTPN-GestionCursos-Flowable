package com.backend.proxy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una versión de la propuesta enviada a la empresa, con su PDF (enunciado 4.1.3).
 *
 * <p>Cada vez que se elabora o se revisa la propuesta se guarda una fila nueva con el
 * número de versión siguiente, así queda el historial completo de la negociación.
 */
@Entity
@Table(name = "proposal_documents")
@Getter
@NoArgsConstructor
public class ProposalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "text", nullable = false)
    private String program;

    @Column(nullable = false)
    private BigDecimal budget;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_dates", columnDefinition = "jsonb")
    private List<LocalDate> proposedDates = new ArrayList<>();

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private byte[] pdf;

    public ProposalDocument(Course course, int version, String program, BigDecimal budget,
                            List<LocalDate> proposedDates, String createdBy, byte[] pdf) {
        this.course = course;
        this.version = version;
        this.program = program;
        this.budget = budget;
        this.proposedDates = new ArrayList<>(proposedDates);
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.pdf = pdf;
    }
}
