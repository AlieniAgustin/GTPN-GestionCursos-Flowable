package com.backend.proxy.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.CompanyContactDTO;
import com.backend.proxy.dto.CourseDetailsUpdateDTO;
import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.dto.FlowableProcessInstanceResponse;
import com.backend.proxy.dto.FlowableVariable;
import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.Course;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.repository.CompanyRepository;
import com.backend.proxy.repository.CourseRepository;
import com.backend.proxy.types.CourseStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Enunciado 3.2 (la BD de la institución guarda la información del curso y lo vincula con
 * su instancia) y 4.1.2 (cada empresa consulta solo sus cursos).
 */
class CourseServiceTest {

    private CourseRepository courseRepository;
    private CompanyRepository companyRepository;
    private FlowableClient flowableClient;
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseRepository = Mockito.mock(CourseRepository.class);
        companyRepository = Mockito.mock(CompanyRepository.class);
        flowableClient = Mockito.mock(FlowableClient.class);
        courseService = new CourseService(courseRepository, companyRepository, flowableClient);

        // save() devuelve lo que recibe, como haría JPA con una entidad ya gestionada
        when(courseRepository.save(any(Course.class))).thenAnswer(call -> call.getArgument(0));
        when(companyRepository.save(any(Company.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("4.1.1 Alta: registra la solicitud, arranca la instancia y la vincula al curso")
    void testCourseCreationLinksProcessInstance() {
        when(companyRepository.findByName("ACME SA")).thenReturn(Optional.empty());
        when(flowableClient.startProcessInstance(anyString(), anyString(), anyList()))
                .thenReturn(new FlowableProcessInstanceResponse("proc-1", null, "1", false, false, "Process_Curso:1:1"));

        CourseResponseDTO created = courseService.processCourse(request());

        assertEquals("proc-1", created.processInstanceId(), "El curso queda vinculado a su instancia");
        assertEquals("ACME SA", created.companyName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FlowableVariable>> variables = ArgumentCaptor.forClass(List.class);
        Mockito.verify(flowableClient).startProcessInstance(anyString(), anyString(), variables.capture());
        assertEquals(
                List.of("courseId", "courseName", "companyName", "companyEmail"),
                variables.getValue().stream().map(FlowableVariable::name).toList(),
                "La instancia arranca con el contexto del curso");
    }

    @Test
    @DisplayName("3.2 Alta: una empresa que ya contrató antes se reutiliza, no se duplica")
    void testExistingCompanyIsReused() {
        Company existing = new Company("ACME SA", "+543581234567", "contacto@acme.com");
        when(companyRepository.findByName("ACME SA")).thenReturn(Optional.of(existing));
        when(flowableClient.startProcessInstance(anyString(), anyString(), anyList()))
                .thenReturn(new FlowableProcessInstanceResponse("proc-2", null, "1", false, false, "Process_Curso:1:1"));

        courseService.processCourse(request());

        Mockito.verify(companyRepository, Mockito.never()).save(any(Company.class));
    }

    @Test
    @DisplayName("3.2 Detalles: solo se pisan los campos que llegan con valor")
    void testDetailsUpdateIsPartial() {
        Course course = courseWithCompany("acme-sa");
        course.applyDetails("Programa original", new BigDecimal("1000"), LocalDate.of(2027, 3, 1), null);
        when(courseRepository.findByprocessInstanceId("proc-1")).thenReturn(Optional.of(course));

        // Etapa posterior: solo llega el informe
        courseService.updateCourseDetails("proc-1", new CourseDetailsUpdateDTO(null, null, null, "Informe final"));

        assertEquals("Programa original", course.getProgram(), "El programa no se pierde");
        assertEquals(new BigDecimal("1000"), course.getBudget(), "El presupuesto no se pierde");
        assertEquals(LocalDate.of(2027, 3, 1), course.getAgreedDate(), "La fecha no se pierde");
        assertEquals("Informe final", course.getReport());
    }

    @Test
    @DisplayName("3.2 Detalles: un payload vacío no toca la base")
    void testEmptyDetailsAreIgnored() {
        courseService.updateCourseDetails("proc-1", new CourseDetailsUpdateDTO(null, null, null, null));

        Mockito.verify(courseRepository, Mockito.never()).findByprocessInstanceId(anyString());
    }

    @Test
    @DisplayName("3.2 Detalles: una instancia sin curso asociado da 404")
    void testDetailsForUnknownInstance() {
        when(courseRepository.findByprocessInstanceId("no-existe")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.updateCourseDetails(
                "no-existe", new CourseDetailsUpdateDTO("Programa", null, null, null)));
    }

    @Test
    @DisplayName("4.1.2 Consulta: la empresa ve solo sus cursos; la institución los ve todos")
    void testVisibilityDependsOnTheRole() {
        when(courseRepository.findByCompanyAccessUsernameOrderByIdDesc("acme-sa"))
                .thenReturn(List.of(courseWithCompany("acme-sa")));
        when(courseRepository.findAllByOrderByIdDesc())
                .thenReturn(List.of(courseWithCompany("acme-sa"), courseWithCompany("otra-sa")));

        assertEquals(1, courseService.findVisibleFor("acme-sa", true).size());
        assertEquals(2, courseService.findVisibleFor("jefeadministrativo", false).size());
    }

    @Test
    @DisplayName("4.1.2 Consulta: el curso de otra empresa da 404, no 403")
    void testCourseOfAnotherCompanyIsNotFound() {
        when(courseRepository.findById(7L)).thenReturn(Optional.of(courseWithCompany("otra-sa")));

        // 404 a propósito: una empresa no tiene por qué enterarse de que el curso existe
        assertThrows(ResourceNotFoundException.class,
                () -> courseService.findOneFor(7L, "acme-sa", true));

        // El personal de la institución sí lo ve
        assertEquals("Curso de prueba", courseService.findOneFor(7L, "jefeadministrativo", false).name());
    }

    @Test
    @DisplayName("3.4 Estado: no se puede transicionar desde un estado terminal")
    void testTerminalStatusCannotTransition() {
        Course course = courseWithCompany("acme-sa");
        course.transitionTo(CourseStatus.FINALIZADO);
        when(courseRepository.findByprocessInstanceId("proc-1")).thenReturn(Optional.of(course));

        assertThrows(RuntimeException.class,
                () -> courseService.updateCourse("proc-1", CourseStatus.CANCELADO));
        assertEquals(CourseStatus.FINALIZADO, course.getStatus());
    }

    @Test
    @DisplayName("3.2 Alta: si Flowable no devuelve instancia, el curso queda registrado igual")
    void testCourseSurvivesEngineWithoutResponse() {
        when(companyRepository.findByName("ACME SA")).thenReturn(Optional.empty());
        when(flowableClient.startProcessInstance(anyString(), anyString(), anyList())).thenReturn(null);

        CourseResponseDTO created = courseService.processCourse(request());

        assertNull(created.processInstanceId(), "Queda sin vincular, pero la solicitud no se pierde");
    }

    private CourseRequestDTO request() {
        return new CourseRequestDTO(
                new CompanyContactDTO("ACME SA", "contacto@acme.com", "+543581234567"),
                "Curso de prueba",
                "Descripcion del curso",
                12,
                List.of(LocalDate.of(2027, 3, 1)));
    }

    private Course courseWithCompany(String accessUsername) {
        Company company = new Company("ACME SA", "+543581234567", "contacto@acme.com");
        company.enableAccess(accessUsername, "$2a$10$hash");
        return new Course(company, "Curso de prueba", "Descripcion", List.of(LocalDate.of(2027, 3, 1)));
    }
}
