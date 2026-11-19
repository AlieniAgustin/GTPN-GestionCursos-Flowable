package com.backend.proxy.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.CompanyContactDTO;
import com.backend.proxy.dto.CourseDetailsUpdateDTO;
import com.backend.proxy.dto.CourseProcessStartRequest;
import com.backend.proxy.dto.CourseProcessStartResponse;
import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.Course;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.repository.CompanyRepository;
import com.backend.proxy.repository.CourseRepository;
import com.backend.proxy.types.CourseStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Enunciado 3.2 (la BD de la institución guarda la información del curso y lo vincula con
 * su instancia) y 4.1.2 (cada empresa consulta solo sus cursos).
 */
@DataJpaTest
class CourseServiceTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private FlowableClient flowableClient;
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        flowableClient = Mockito.mock(FlowableClient.class);
        courseService = new CourseService(courseRepository, companyRepository, flowableClient);
        courseRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    @DisplayName("4.1.1 Alta: registra la solicitud, arranca la instancia y la vincula al curso")
    void testCourseCreationLinksProcessInstance() {
        when(flowableClient.startCourseProcess(any(CourseProcessStartRequest.class)))
                .thenReturn(new CourseProcessStartResponse("proc-1"));

        CourseResponseDTO created = courseService.processCourse(request());

        assertEquals("proc-1", created.processInstanceId(), "El curso queda vinculado a su instancia");
        assertEquals("ACME SA", created.companyName());

        Course persisted = courseRepository.findById(created.id()).orElseThrow();
        assertEquals("proc-1", persisted.getProcessInstanceId());
        assertEquals("ACME SA", persisted.getCompany().getName());

        ArgumentCaptor<CourseProcessStartRequest> startRequest = ArgumentCaptor.forClass(CourseProcessStartRequest.class);
        Mockito.verify(flowableClient).startCourseProcess(startRequest.capture());
        assertEquals(
                new CourseProcessStartRequest(created.id(), "Curso de prueba", "ACME SA", "contacto@acme.com"),
                startRequest.getValue());
    }

    @Test
    @DisplayName("3.2 Alta: una empresa que ya contrató antes se reutiliza, no se duplica")
    void testExistingCompanyIsReused() {
        Company existing = companyRepository.save(new Company("ACME SA", "+543581234567", "contacto@acme.com"));
        when(flowableClient.startCourseProcess(any(CourseProcessStartRequest.class)))
                .thenReturn(new CourseProcessStartResponse("proc-2"));

        courseService.processCourse(request());

        assertEquals(1, companyRepository.count(), "No se crea una empresa duplicada");
        Course course = courseRepository.findAll().get(0);
        assertEquals(existing.getId(), course.getCompany().getId());
    }

    @Test
    @DisplayName("3.2 Detalles: solo se pisan los campos que llegan con valor")
    void testDetailsUpdateIsPartial() {
        Course course = saveCourseWithCompany("acme-sa", "proc-1");
        course.applyDetails("Programa original", new BigDecimal("1000"), LocalDate.of(2027, 3, 1), null);
        courseRepository.save(course);

        // Etapa posterior: solo llega el informe
        courseService.updateCourseDetails("proc-1", new CourseDetailsUpdateDTO(null, null, null, "Informe final"));

        Course reloaded = courseRepository.findByprocessInstanceId("proc-1").orElseThrow();
        assertEquals("Programa original", reloaded.getProgram(), "El programa no se pierde");
        assertEquals(new BigDecimal("1000"), reloaded.getBudget(), "El presupuesto no se pierde");
        assertEquals(LocalDate.of(2027, 3, 1), reloaded.getAgreedDate(), "La fecha no se pierde");
        assertEquals("Informe final", reloaded.getReport());
    }

    @Test
    @DisplayName("3.2 Detalles: un payload vacío no toca la base")
    void testEmptyDetailsAreIgnored() {
        saveCourseWithCompany("acme-sa", "proc-1");

        courseService.updateCourseDetails("proc-1", new CourseDetailsUpdateDTO(null, null, null, null));

        Course reloaded = courseRepository.findByprocessInstanceId("proc-1").orElseThrow();
        assertNull(reloaded.getProgram());
        assertNull(reloaded.getReport());
    }

    @Test
    @DisplayName("3.2 Detalles: una instancia sin curso asociado da 404")
    void testDetailsForUnknownInstance() {
        assertThrows(ResourceNotFoundException.class, () -> courseService.updateCourseDetails(
                "no-existe", new CourseDetailsUpdateDTO("Programa", null, null, null)));
    }

    @Test
    @DisplayName("4.1.2 Consulta: la empresa ve solo sus cursos; la institución los ve todos")
    void testVisibilityDependsOnTheRole() {
        saveCourseWithCompany("acme-sa", "proc-1");
        saveCourseWithCompany("otra-sa", "proc-2");

        assertEquals(1, courseService.findVisibleFor("acme-sa", true).size());
        assertEquals(2, courseService.findVisibleFor("jefeadministrativo", false).size());
    }

    @Test
    @DisplayName("4.1.2 Consulta: el curso de otra empresa da 404, no 403")
    void testCourseOfAnotherCompanyIsNotFound() {
        Course otherCourse = saveCourseWithCompany("otra-sa", "proc-2");

        // 404 a propósito: una empresa no tiene por qué enterarse de que el curso existe
        assertThrows(ResourceNotFoundException.class,
                () -> courseService.findOneFor(otherCourse.getId(), "acme-sa", true));

        // El personal de la institución sí lo ve
        assertEquals("Curso de prueba", courseService.findOneFor(otherCourse.getId(), "jefeadministrativo", false).name());
    }

    @Test
    @DisplayName("3.4 Estado: no se puede transicionar desde un estado terminal")
    void testTerminalStatusCannotTransition() {
        Course course = saveCourseWithCompany("acme-sa", "proc-1");
        course.transitionTo(CourseStatus.FINALIZADO);
        courseRepository.save(course);

        assertThrows(RuntimeException.class,
                () -> courseService.updateCourse("proc-1", CourseStatus.CANCELADO));
        
        Course reloaded = courseRepository.findByprocessInstanceId("proc-1").orElseThrow();
        assertEquals(CourseStatus.FINALIZADO, reloaded.getStatus());
    }

    @Test
    @DisplayName("3.2 Alta: si Flowable no devuelve instancia, el curso queda registrado igual")
    void testCourseSurvivesEngineWithoutResponse() {
        when(flowableClient.startCourseProcess(any(CourseProcessStartRequest.class))).thenReturn(null);

        CourseResponseDTO created = courseService.processCourse(request());

        assertNull(created.processInstanceId(), "Queda sin vincular, pero la solicitud no se pierde");
        assertNotNull(created.id());
        assertEquals(1, courseRepository.count());
    }

    private CourseRequestDTO request() {
        return new CourseRequestDTO(
                new CompanyContactDTO("ACME SA", "contacto@acme.com", "+543581234567"),
                "Curso de prueba",
                "Descripcion del curso",
                12,
                List.of(LocalDate.of(2027, 3, 1)));
    }

    private Course saveCourseWithCompany(String accessUsername, String processInstanceId) {
        Company company = new Company(accessUsername + " Corp", "+543581234567", accessUsername + "@corp.com");
        company.enableAccess(accessUsername, "$2a$10$hash");
        companyRepository.save(company);

        Course course = new Course(company, "Curso de prueba", "Descripcion", List.of(LocalDate.of(2027, 3, 1)));
        course.setProcessInstanceId(processInstanceId);
        return courseRepository.save(course);
    }
}
