package com.flowable;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import com.flowable.app.App;
import com.flowable.app.dto.CourseProcessStartRequestDTO;
import com.flowable.app.services.CourseProcessService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * El contrato que el BFF consume: pedir el alta de una solicitud alcanza para arrancar
 * la instancia, con el curso como business key y su contexto como variables.
 */
@SpringBootTest(classes = App.class)
@DirtiesContext
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:course_start;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "flowable.database-schema-update=true",
    "flowable.async-executor-activate=false"
})
class CourseProcessStartTest {

    @Autowired
    private CourseProcessService courseProcessService;

    @Autowired
    private RuntimeService runtimeService;

    @MockBean(name = "backendRestTemplate")
    private RestTemplate backendRestTemplate;

    @Test
    @DisplayName("4.1.1 Alta: el mensaje del modelo arranca la instancia con el curso vinculado")
    void testStartCourseRequest() {
        String processInstanceId = courseProcessService.startCourseRequest(
                new CourseProcessStartRequestDTO(42L, "Curso de prueba", "ACME SA", "contacto@acme.com"));

        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        assertNotNull(instance, "La instancia tiene que existir");
        assertEquals("42", instance.getBusinessKey(), "El curso queda vinculado por business key");
        assertEquals("Curso de prueba", runtimeService.getVariable(processInstanceId, "courseName"));
        assertEquals("ACME SA", runtimeService.getVariable(processInstanceId, "companyName"));
        assertEquals("contacto@acme.com", runtimeService.getVariable(processInstanceId, "companyEmail"));
        assertEquals(42L, ((Number) runtimeService.getVariable(processInstanceId, "courseId")).longValue());
    }
}
