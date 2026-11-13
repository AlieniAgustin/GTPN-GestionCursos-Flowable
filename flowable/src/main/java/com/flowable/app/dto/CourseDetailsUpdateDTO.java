package com.flowable.app.dto;

import java.math.BigDecimal;

/**
 * Datos del curso que el proceso produce y que se vuelcan a la BD de la institución
 * (enunciado 3.2). Los campos que todavía no existen viajan en null y el BFF los ignora.
 *
 * <p>La fecha va como texto {@code yyyy-MM-dd} para no depender de cómo serialice las
 * fechas cada lado; el BFF la recibe como {@code LocalDate}.
 */
public record CourseDetailsUpdateDTO(
    String program,
    BigDecimal budget,
    String agreedDate,
    String report
) {}
