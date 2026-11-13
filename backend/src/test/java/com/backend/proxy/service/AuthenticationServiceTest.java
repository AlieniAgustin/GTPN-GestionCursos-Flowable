package com.backend.proxy.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.backend.proxy.dto.auth.LoginResponseDTO;
import com.backend.proxy.entity.Company;
import com.backend.proxy.repository.CompanyRepository;
import com.backend.proxy.security.Roles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Enunciado 4.1: al sitio entran dos tipos de usuario. El personal de la institución con
 * su usuario y contraseña de Flowable (§3.3) y las empresas con la cuenta que les crea
 * "Habilitar acceso de la empresa" (§4.1.2). El rol que queda en la sesión es lo que
 * después decide qué cursos ve cada uno.
 */
class AuthenticationServiceTest {

    private FlowableAuthService flowableAuthService;
    private CompanyRepository companyRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationService authenticationService;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        flowableAuthService = Mockito.mock(FlowableAuthService.class);
        companyRepository = Mockito.mock(CompanyRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authenticationService = new AuthenticationService(flowableAuthService, companyRepository, passwordEncoder);
        request = new MockHttpServletRequest();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("4.1 Login: institutional staff authenticate against the Flowable IDM")
    void testInstitutionalLogin() {
        when(flowableAuthService.authenticates("jefeadministrativo", "password")).thenReturn(true);

        Optional<LoginResponseDTO> result =
                authenticationService.login("jefeadministrativo", "password", request);

        assertTrue(result.isPresent());
        assertEquals("jefeadministrativo", result.get().username());
        assertEquals(Roles.INSTITUCION, result.get().role());
        assertSessionHolds("jefeadministrativo", Roles.ROLE_INSTITUCION);
        // Si Flowable ya validó, no hace falta ir a la base
        verify(companyRepository, never()).findByAccessUsername(anyString());
    }

    @Test
    @DisplayName("4.1 Login: a company authenticates against the institution database")
    void testCompanyLogin() {
        when(flowableAuthService.authenticates(anyString(), anyString())).thenReturn(false);
        when(companyRepository.findByAccessUsername("acme-sa"))
                .thenReturn(Optional.of(companyWithPassword("secreto123")));

        Optional<LoginResponseDTO> result = authenticationService.login("acme-sa", "secreto123", request);

        assertTrue(result.isPresent());
        assertEquals(Roles.EMPRESA, result.get().role());
        assertSessionHolds("acme-sa", Roles.ROLE_EMPRESA);
    }

    @Test
    @DisplayName("4.1 Login: a company with the wrong password is rejected")
    void testCompanyWithWrongPassword() {
        when(flowableAuthService.authenticates(anyString(), anyString())).thenReturn(false);
        when(companyRepository.findByAccessUsername("acme-sa"))
                .thenReturn(Optional.of(companyWithPassword("secreto123")));

        assertTrue(authenticationService.login("acme-sa", "otra", request).isEmpty());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("4.1 Login: an unknown user is rejected without a session")
    void testUnknownUser() {
        when(flowableAuthService.authenticates(anyString(), anyString())).thenReturn(false);
        when(companyRepository.findByAccessUsername(anyString())).thenReturn(Optional.empty());

        assertTrue(authenticationService.login("nadie", "nada", request).isEmpty());
        assertNull(request.getSession(false), "A failed login must not open a session");
    }

    @Test
    @DisplayName("4.1 Login: a company that never got credentials cannot authenticate")
    void testCompanyWithoutAccessCannotLogin() {
        when(flowableAuthService.authenticates(anyString(), anyString())).thenReturn(false);
        // Empresa dada de alta pero que todavía no pasó por "Habilitar acceso"
        when(companyRepository.findByAccessUsername("acme-sa"))
                .thenReturn(Optional.of(new Company("ACME SA", "+543581234567", "contacto@acme.com")));

        assertTrue(authenticationService.login("acme-sa", "loquesea", request).isEmpty());
    }

    @Test
    @DisplayName("4.1 Logout: the session is invalidated")
    void testLogoutInvalidatesSession() {
        when(flowableAuthService.authenticates("docente1", "password")).thenReturn(true);
        authenticationService.login("docente1", "password", request);
        assertNotNull(request.getSession(false));

        authenticationService.logout(request);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private Company companyWithPassword(String rawPassword) {
        Company company = new Company("ACME SA", "+543581234567", "contacto@acme.com");
        company.enableAccess("acme-sa", passwordEncoder.encode(rawPassword));
        return company;
    }

    /**
     * La sesión tiene que quedar con el usuario y el rol correctos, y guardada donde la
     * cadena de filtros la busca en las llamadas siguientes.
     */
    private void assertSessionHolds(String username, String role) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication, "The SecurityContext must hold the authentication");
        assertEquals(username, authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                        .anyMatch(granted -> role.equals(granted.getAuthority())),
                "Expected authority " + role);

        assertNotNull(request.getSession(false), "The session must exist");
        assertNotNull(
                request.getSession(false)
                        .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY),
                "The SecurityContext must be stored in the HttpSession");
    }
}
