package com.backend.proxy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.backend.proxy.entity.Course;
import com.backend.proxy.types.CourseStatus;

/**
 * Vista del curso que se devuelve a los clientes. Mantiene la entidad JPA fuera de la
 * capa HTTP para que la asociacion lazy {@code company} no se serialice por accidente.
 *
 * <p>Los campos que el proceso todavia no produjo viajan en null: el programa y el
 * presupuesto aparecen al enviarse la propuesta, la fecha al aceptarse y el informe al
 * cerrarse el curso.
 */
public record CourseResponseDTO(
    Long id,
    String processInstanceId,
    String name,
    String description,
    CourseStatus status,
    String companyName,
    String program,
    BigDecimal budget,
    LocalDate agreedDate,
    List<LocalDate> possibleDates,
    String report
) {

    /**
     * Debe invocarse dentro de la transaccion: toca la empresa, que es lazy.
     */
    public static CourseResponseDTO from(Course course) {
        return new CourseResponseDTO(
                course.getId(),
                course.getProcessInstanceId(),
                course.getName(),
                course.getDescription(),
                course.getStatus(),
                course.getCompany() != null ? course.getCompany().getName() : null,
                course.getProgram(),
                course.getBudget(),
                course.getAgreedDate(),
                course.getPossibleDates(),
                course.getReport()
        );
    }
}
