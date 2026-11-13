package com.materiales.app.service;

import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enunciado 3.3: el personal entra a las apps externas con su usuario y contraseña de
 * Flowable. Y según el enunciado 2, <i>Preparar materiales</i> es de Administración.
 */
class StaffAuthenticatorTest {

    private static ProcessEngine engine;
    private static StaffAuthenticator authenticator;

    @BeforeAll
    static void startEngine() {
        engine = MaterialsAppTestEngine.start("materiales_auth");
        authenticator = new StaffAuthenticator(engine.getIdentityService());
    }

    @AfterAll
    static void stopEngine() {
        engine.close();
    }

    @Test
    @DisplayName("Login: an administrative of the group gets in, with their groups resolved")
    void testAdministrativeLogsIn() throws Exception {
        StaffUser user = authenticator.login("administrativo1", MaterialsAppTestEngine.PASSWORD);

        assertEquals("administrativo1", user.id());
        assertTrue(user.belongsTo(StaffAuthenticator.GROUP_ADMINISTRACION));
    }

    @Test
    @DisplayName("Login: a wrong password is rejected")
    void testWrongPasswordIsRejected() {
        AuthenticationFailedException error = assertThrows(AuthenticationFailedException.class,
                () -> authenticator.login("administrativo1", "incorrecta"));
        assertTrue(error.getMessage().contains("incorrectos"), error.getMessage());
    }

    @Test
    @DisplayName("Login: an unknown user is rejected")
    void testUnknownUserIsRejected() {
        assertThrows(AuthenticationFailedException.class,
                () -> authenticator.login("nadie", MaterialsAppTestEngine.PASSWORD));
    }

    @Test
    @DisplayName("Login: a teacher is rejected, materials belong to Administration")
    void testTeacherIsRejected() {
        AuthenticationFailedException error = assertThrows(AuthenticationFailedException.class,
                () -> authenticator.login("docente1", MaterialsAppTestEngine.PASSWORD));
        assertTrue(error.getMessage().contains("Administración"), error.getMessage());
    }

    @Test
    @DisplayName("Login: blank credentials are rejected before hitting the engine")
    void testBlankCredentialsAreRejected() {
        assertThrows(AuthenticationFailedException.class, () -> authenticator.login("  ", "password"));
        assertThrows(AuthenticationFailedException.class, () -> authenticator.login("administrativo1", ""));
    }
}
