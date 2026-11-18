package com.backend.proxy.repository;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.task.IdentityUserDTO;
import com.backend.proxy.dto.task.TeacherDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El equipo docente sale del grupo {@code docentes} del IDM de Flowable, no de una lista
 * fija: el enunciado 3.3 dice que el personal de la institución vive en el motor.
 */
class FlowableTeacherRepositoryTest {

    private FlowableClient flowableClient;
    private FlowableTeacherRepository repository;

    @BeforeEach
    void setUp() {
        flowableClient = Mockito.mock(FlowableClient.class, Mockito.RETURNS_DEEP_STUBS);
        repository = new FlowableTeacherRepository(flowableClient);
    }

    @Test
    @DisplayName("3.3 Docentes: se consulta el grupo docentes del motor")
    void testQueriesTheTeachersGroup() {
        stub(new IdentityUserDTO.Page(List.of(
                new IdentityUserDTO("docente1", "Hernan", "Rossi")), 1));

        repository.findAvailableTeachers();

        verify(flowableClient).get(contains("memberOfGroup=docentes"));
    }

    @Test
    @DisplayName("3.3 Docentes: se muestran por nombre y apellido, ordenados por id")
    void testNamesAndStableOrder() {
        stub(new IdentityUserDTO.Page(List.of(
                new IdentityUserDTO("jefedocente", "Marcela", "Ibanez"),
                new IdentityUserDTO("docente2", "Sofia", "Quiroga"),
                new IdentityUserDTO("docente1", "Hernan", "Rossi")), 3));

        List<TeacherDTO> docentes = repository.findAvailableTeachers();

        assertEquals(List.of("docente1", "docente2", "jefedocente"),
                docentes.stream().map(TeacherDTO::userId).toList());
        assertEquals("Hernan Rossi", docentes.get(0).name());
    }

    @Test
    @DisplayName("3.3 Docentes: un usuario sin nombre cargado se muestra por su id")
    void testFallsBackToTheUserId() {
        stub(new IdentityUserDTO.Page(List.of(
                new IdentityUserDTO("docente3", null, null)), 1));

        assertEquals("docente3", repository.findAvailableTeachers().get(0).name());
    }

    @Test
    @DisplayName("3.3 Docentes: si el motor no devuelve nada, la lista va vacía")
    void testEmptyResponse() {
        stub(null);
        assertTrue(repository.findAvailableTeachers().isEmpty());
    }

    private void stub(IdentityUserDTO.Page pagina) {
        RestClient.RequestHeadersSpec<?> spec = flowableClient.get(anyString());
        when(spec.retrieve().body(IdentityUserDTO.Page.class)).thenReturn(pagina);
    }
}
