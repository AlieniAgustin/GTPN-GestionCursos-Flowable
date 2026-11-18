package com.backend.proxy.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.Course;
import com.backend.proxy.entity.ProposalDocument;
import com.backend.proxy.repository.CourseRepository;
import com.backend.proxy.repository.ProposalDocumentRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Elaboración de la propuesta con su PDF (enunciado 4.1.3).
 */
class ProposalServiceTest {

    private static final String PROCESO = "proc-1";
    private static final List<LocalDate> FECHAS = List.of(LocalDate.of(2027, 3, 1));

    private CourseRepository courseRepository;
    private ProposalDocumentRepository proposalRepository;
    private TaskService taskService;
    private ProposalService service;

    @BeforeEach
    void setUp() {
        courseRepository = Mockito.mock(CourseRepository.class);
        proposalRepository = Mockito.mock(ProposalDocumentRepository.class);
        taskService = Mockito.mock(TaskService.class);
        // El PDF se arma de verdad: así el test también prueba que OpenPDF genera el archivo
        service = new ProposalService(courseRepository, proposalRepository, new ProposalPdfBuilder(), taskService);

        Course curso = new Course(new Company("ACME SA", "+54358", "rrhh@acme.com"),
                "Introducción a BPMN", "Modelado de procesos", FECHAS);
        when(courseRepository.findByprocessInstanceId(PROCESO)).thenReturn(Optional.of(curso));
    }

    @Test
    @DisplayName("Primera propuesta: genera la versión 1 con un PDF válido")
    void testFirstProposalIsVersionOne() {
        when(proposalRepository.countByCourseId(any())).thenReturn(0);

        int version = service.enviarPropuesta("task-1", PROCESO, "4 módulos", new BigDecimal("180000"), FECHAS, "jefeadministrativo");

        assertEquals(1, version);
        ArgumentCaptor<ProposalDocument> guardada = ArgumentCaptor.forClass(ProposalDocument.class);
        verify(proposalRepository).save(guardada.capture());
        assertEquals(1, guardada.getValue().getVersion());
        assertTrue(new String(guardada.getValue().getPdf(), 0, 4).startsWith("%PDF"),
                "el archivo guardado tiene que ser un PDF");
    }

    @Test
    @DisplayName("Si ya hubo versiones, la nueva es la siguiente")
    void testNextVersion() {
        when(proposalRepository.countByCourseId(any())).thenReturn(2);

        int version = service.enviarPropuesta("task-1", PROCESO, "4 módulos", new BigDecimal("180000"), FECHAS, "jefeadministrativo");

        assertEquals(3, version);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Completa la tarea con las variables que usa el proceso")
    void testCompletesTaskWithProcessVariables() {
        when(proposalRepository.countByCourseId(any())).thenReturn(0);

        service.enviarPropuesta("task-1", PROCESO, "4 módulos", new BigDecimal("180000"), FECHAS, "jefeadministrativo");

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(taskService).complete(eq("task-1"), variables.capture(), eq("jefeadministrativo"));
        assertEquals("4 módulos", variables.getValue().get("programa"));
        assertEquals(new BigDecimal("180000"), variables.getValue().get("presupuesto"));
        assertEquals("2027-03-01", variables.getValue().get("fechasPropuestas"));
        assertEquals(1, variables.getValue().get("versionPropuesta"));
        assertEquals("Propuesta versión 1", variables.getValue().get("propuesta"));
    }

    @Test
    @DisplayName("Si el motor rechaza el completado, el error sube y la transacción no se confirma")
    void testEngineFailurePropagates() {
        when(proposalRepository.countByCourseId(any())).thenReturn(0);
        doThrow(new IllegalStateException("motor caído"))
                .when(taskService).complete(anyString(), anyMap(), anyString());

        assertThrows(IllegalStateException.class, () -> service.enviarPropuesta(
                "task-1", PROCESO, "4 módulos", new BigDecimal("180000"), FECHAS, "jefeadministrativo"));
    }

    @Test
    @DisplayName("Revisar: la última versión trae lo que se envió, para precargar el formulario")
    void testLastVersion() {
        Course curso = courseRepository.findByprocessInstanceId(PROCESO).orElseThrow();
        when(proposalRepository.findByCourseIdOrderByVersionDesc(any())).thenReturn(List.of(
                new ProposalDocument(curso, 2, "v2", new BigDecimal("150000"), FECHAS, "jefeadministrativo", new byte[0]),
                new ProposalDocument(curso, 1, "v1", new BigDecimal("180000"), FECHAS, "jefeadministrativo", new byte[0])));

        ProposalService.Version ultima = service.ultimaVersion(1L).orElseThrow();

        assertEquals(2, ultima.numero());
        assertEquals("v2", ultima.programa());
        assertEquals(new BigDecimal("150000"), ultima.presupuesto());
    }

    @Test
    @DisplayName("Una versión que no existe da 404")
    void testMissingVersion() {
        when(proposalRepository.findByCourseIdAndVersion(anyLong(), eq(9))).thenReturn(Optional.empty());

        assertThrows(com.backend.proxy.exception.ResourceNotFoundException.class,
                () -> service.pdfDe(1L, 9));
    }
}
