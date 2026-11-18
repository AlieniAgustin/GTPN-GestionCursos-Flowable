package com.backend.proxy.dto.task;

import java.util.List;

/**
 * Usuario del IDM de Flowable, tal como lo devuelve
 * {@code GET /process-api/identity/users}.
 */
public record IdentityUserDTO(
    String id,
    String firstName,
    String lastName
) {

    /**
     * Nombre y apellido si el usuario los tiene cargados; si no, el id, que siempre está.
     */
    public String displayName() {
        String nombre = ((firstName != null ? firstName : "") + " "
                + (lastName != null ? lastName : "")).trim();
        return nombre.isEmpty() ? id : nombre;
    }

    /** Respuesta paginada del listado de usuarios. */
    public record Page(List<IdentityUserDTO> data, int total) {}
}
