package com.flowable.app.dto;

import java.math.BigDecimal;

public record CourseDetailsUpdateDTO(
    String program,
    BigDecimal budget,
    String agreedDate,
    String report
) {}
