package com.backend.proxy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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

@Entity
@Table(name = "courses")
@Getter
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

    private BigDecimal budget;

    private String reportUrl;

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
}
