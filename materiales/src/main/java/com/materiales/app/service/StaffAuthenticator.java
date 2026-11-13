package com.materiales.app.service;

import java.util.List;
import java.util.stream.Collectors;

import org.flowable.engine.IdentityService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;

/**
 * Identificación del administrativo contra el IDM del motor (enunciado 3.3: "ingresarán a
 * las aplicaciones externas utilizando su ID de usuario y contraseña de Flowable").
 *
 * <p>Además del usuario y la contraseña, exige pertenecer al grupo Administración, que es
 * el responsable de <i>Preparar materiales</i> según el enunciado 2.
 */
public class StaffAuthenticator {

    public static final String GROUP_ADMINISTRACION = "administracion";

    private final IdentityService identityService;

    public StaffAuthenticator(IdentityService identityService) {
        this.identityService = identityService;
    }

    public StaffUser login(String userId, String password) throws AuthenticationFailedException {
        if (userId == null || userId.isBlank()) {
            throw new AuthenticationFailedException("Ingresá tu usuario de Flowable");
        }
        if (password == null || password.isEmpty()) {
            throw new AuthenticationFailedException("Ingresá tu contraseña");
        }

        String id = userId.trim();
        if (!identityService.checkPassword(id, password)) {
            throw new AuthenticationFailedException("Usuario o contraseña incorrectos");
        }

        List<String> groups = identityService.createGroupQuery()
                .groupMember(id)
                .list()
                .stream()
                .map(Group::getId)
                .collect(Collectors.toList());

        StaffUser user = new StaffUser(id, displayName(id), groups);

        if (!user.belongsTo(GROUP_ADMINISTRACION)) {
            throw new AuthenticationFailedException(
                    "'" + id + "' no es del grupo Administración: Preparar materiales no le corresponde");
        }
        return user;
    }

    private String displayName(String userId) {
        User user = identityService.createUserQuery().userId(userId).singleResult();
        if (user == null) {
            return userId;
        }
        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return fullName.isEmpty() ? userId : fullName;
    }
}
