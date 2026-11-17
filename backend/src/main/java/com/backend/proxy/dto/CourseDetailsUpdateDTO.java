package com.backend.proxy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;


public record CourseDetailsUpdateDTO(
    String program,
    BigDecimal budget,
    LocalDate agreedDate,
    String report
) {

    public boolean isEmpty() {
        return program == null && budget == null && agreedDate == null && report == null;
    }
}
