package com.backend.proxy.repository;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.backend.proxy.client.FlowableClient;
import com.backend.proxy.dto.task.IdentityUserDTO;
import com.backend.proxy.dto.task.TeacherDTO;

import lombok.RequiredArgsConstructor;

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
