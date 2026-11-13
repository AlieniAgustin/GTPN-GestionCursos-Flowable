package com.backend.proxy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos del curso que el proceso va produciendo y que se vuelcan a la BD de la
 * institucion (enunciado 3.2).
 *
 * <p>Todos los campos son opcionales: el motor manda los que ya tiene en cada etapa.
 * Programa y presupuesto salen de la propuesta, la fecha acordada de la aceptacion y
 * el informe de la tarea del jefe docente.
 */
public record CourseDetailsUpdateDTO(
    String program,
    BigDecimal budget,
    LocalDate agreedDate,
    String report
) {

    /** Evita un viaje a la base cuando el proceso todavia no produjo ningun dato. */
    public boolean isEmpty() {
        return program == null && budget == null && agreedDate == null && report == null;
    }
}
