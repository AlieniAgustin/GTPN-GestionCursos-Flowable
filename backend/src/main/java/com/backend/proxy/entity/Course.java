package com.backend.proxy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.backend.proxy.exception.InvalidCourseStateException;
import com.backend.proxy.types.CourseStatus;

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
import lombok.Setter;

@Entity
@Table(name = "courses")
@Getter
@Setter 
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    /**
     * Programa del curso, redactado al elaborar la propuesta (enunciado 1.1).
     */
    @Column(columnDefinition = "text")
    private String program;

    /**
     * Presupuesto de la propuesta (enunciado 1.1).
     */
    private BigDecimal budget;

    /**
     * Fecha de inicio acordada con la empresa al aceptar la propuesta (enunciado 3.5).
     * Es la que alimenta el temporizador del modelo.
     */
    @Column(name = "agreed_date")
    private LocalDate agreedDate;

    /**
     * Contenido del informe de resultados, escrito por el jefe del equipo docente.
     */
    @Column(columnDefinition = "text")
    private String report;

    private CourseStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<LocalDate> possibleDates = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    protected Course() {
    }

    public Course(Company company, String name, String description, List<LocalDate> possibleDates) {
        this.company = company;
        this.name = name;
        this.description = description;
        this.budget = new BigDecimal("0.0");
        this.possibleDates = possibleDates;
    }

    /**
     * Vuelca los datos que el proceso fue produciendo. Cada campo se actualiza solo si
     * llegó con valor, porque el proceso los completa en etapas distintas: el programa y
     * el presupuesto al elaborar la propuesta, la fecha al aceptarla y el informe al
     * cerrar el curso.
     */
    public void applyDetails(String program, BigDecimal budget, LocalDate agreedDate, String report) {
        if (program != null && !program.isBlank()) {
            this.program = program;
        }
        if (budget != null) {
            this.budget = budget;
        }
        if (agreedDate != null) {
            this.agreedDate = agreedDate;
        }
        if (report != null && !report.isBlank()) {
            this.report = report;
        }
    }

    public void transitionTo(CourseStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Course status cannot be null");
        }
        if (this.status != null && this.status.isTerminal()) {
            throw new InvalidCourseStateException("Cannot transition from terminal status: " + this.status);
        }
        this.status = newStatus;
    }
}
