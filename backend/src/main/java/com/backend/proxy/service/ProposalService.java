package com.backend.proxy.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.proxy.entity.Course;
import com.backend.proxy.entity.ProposalDocument;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.repository.CourseRepository;
import com.backend.proxy.repository.ProposalDocumentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Propuestas de un curso y sus PDF (enunciado 4.1.3).
 *
 * <p>Cada propuesta enviada es una versión nueva: la primera la arma
 * <em>Registrar y elaborar propuesta</em> y las siguientes <em>Revisar propuesta</em>.
 * Las dos usan {@link #enviarPropuesta}, que siempre genera la versión siguiente.
 */
@Service
@RequiredArgsConstructor
public class ProposalService {

    private final CourseRepository courseRepository;
    private final ProposalDocumentRepository proposalRepository;
    private final ProposalPdfBuilder pdfBuilder;
    private final TaskService taskService;

    /** Lo que el formulario necesita mostrar del curso. */
    public record Contexto(
            Long courseId,
            String curso,
            String empresa,
            String descripcion,
            List<LocalDate> fechasSolicitadas,
            int proximaVersion
    ) {}

    @Transactional(readOnly = true)
    public Contexto contextoDe(String processInstanceId) {
        Course course = cursoDe(processInstanceId);
        return new Contexto(
                course.getId(),
                course.getName(),
                course.getCompany().getName(),
                course.getDescription(),
                course.getPossibleDates(),
                proposalRepository.countByCourseId(course.getId()) + 1);
    }

    /**
     * Genera el PDF de la versión siguiente, lo guarda y completa la tarea.
     *
     * <p>Todo pasa en una transacción: si el motor rechaza el completado, la versión
     * no queda guardada y el usuario puede volver a intentar.
     *
     * @return el número de versión generado
     */
    @Transactional
    public int enviarPropuesta(String taskId, String processInstanceId, String programa,
                               BigDecimal presupuesto, List<LocalDate> fechas, String usuario) {
        Course course = cursoDe(processInstanceId);
        int version = proposalRepository.countByCourseId(course.getId()) + 1;

        byte[] pdf = pdfBuilder.build(new ProposalPdfBuilder.Datos(
                course.getCompany().getName(),
                course.getName(),
                course.getDescription(),
                programa,
                presupuesto,
                fechas,
                version,
                usuario,
                LocalDate.now()));

        proposalRepository.save(new ProposalDocument(course, version, programa, presupuesto, fechas, usuario, pdf));

        // Variables que usa el proceso: programa y presupuesto los vuelca a la BD
        // SyncCourseDetailsListener al pasar a "Enviar propuesta"
        taskService.complete(taskId, Map.of(
                "propuesta", "Propuesta versión " + version,
                "programa", programa,
                "presupuesto", presupuesto,
                "fechasPropuestas", fechas.stream().map(LocalDate::toString).collect(Collectors.joining(", ")),
                "versionPropuesta", version), usuario);

        return version;
    }

    /** Lo que tenía la última versión enviada, para precargar <em>Revisar propuesta</em>. */
    public record Version(int numero, String programa, BigDecimal presupuesto, List<LocalDate> fechas) {}

    @Transactional(readOnly = true)
    public Optional<Version> ultimaVersion(Long courseId) {
        return proposalRepository.findByCourseIdOrderByVersionDesc(courseId).stream()
                .findFirst()
                .map(doc -> new Version(doc.getVersion(), doc.getProgram(), doc.getBudget(),
                        List.copyOf(doc.getProposedDates())));
    }

    /** Números de versión de la propuesta de un curso, de la más nueva a la más vieja. */
    @Transactional(readOnly = true)
    public List<Integer> versionesDe(Long courseId) {
        return proposalRepository.findByCourseIdOrderByVersionDesc(courseId).stream()
                .map(ProposalDocument::getVersion)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] pdfDe(Long courseId, int version) {
        return proposalRepository.findByCourseIdAndVersion(courseId, version)
                .map(ProposalDocument::getPdf)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proposal " + version + " not found for course " + courseId));
    }

    private Course cursoDe(String processInstanceId) {
        return courseRepository.findByprocessInstanceId(processInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course not found for process " + processInstanceId));
    }
}
