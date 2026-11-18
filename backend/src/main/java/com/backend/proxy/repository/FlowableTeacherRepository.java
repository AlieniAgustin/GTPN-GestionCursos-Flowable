package com.backend.proxy.repository;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.task.IdentityUserDTO;
import com.backend.proxy.dto.task.TeacherDTO;

import lombok.RequiredArgsConstructor;

/**
 * Equipo docente, leído del IDM de Flowable.
 *
 * <p>El enunciado (3.3) dice que el personal de la institución no tiene cuentas propias
 * en la base de la institución: vive en el motor. Por eso la lista sale del grupo
 * {@code docentes} y no de una tabla local ni de una constante: si el equipo cambia en
 * Flowable, el desplegable de <em>Definir docente</em> lo refleja sin tocar código.
 */
@Repository
@RequiredArgsConstructor
public class FlowableTeacherRepository implements TeacherRepository {

    private static final String GRUPO_DOCENTES = "docentes";
    private static final String USUARIOS_URI =
            "/process-api/identity/users?memberOfGroup=" + GRUPO_DOCENTES + "&size=100";

    private final FlowableClient flowableClient;

    @Override
    public List<TeacherDTO> findAvailableTeachers() {
        IdentityUserDTO.Page pagina = flowableClient.get(USUARIOS_URI)
                .retrieve()
                .body(IdentityUserDTO.Page.class);

        if (pagina == null || pagina.data() == null) {
            return List.of();
        }

        // Orden estable por id: el motor no garantiza uno y el desplegable no debería
        // cambiar de orden entre pantallas
        return pagina.data().stream()
                .sorted(Comparator.comparing(IdentityUserDTO::id))
                .map(usuario -> new TeacherDTO(usuario.id(), usuario.displayName()))
                .toList();
    }
}
