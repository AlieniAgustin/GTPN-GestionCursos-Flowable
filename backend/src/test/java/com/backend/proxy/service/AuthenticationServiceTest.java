package com.backend.proxy.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.backend.proxy.dto.auth.LoginResponseDTO;
import com.backend.proxy.entity.Company;
import com.backend.proxy.repository.CompanyRepository;
import com.backend.proxy.security.Roles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Enunciado 4.1: al sitio entran dos tipos de usuario. El personal de la institución con
 * su usuario y contraseña de Flowable (§3.3) y las empresas con la cuenta que les crea
 * "Habilitar acceso de la empresa" (§4.1.2).
 */
@DataJpaTest
class AuthenticationServiceTest {

    @Autowired
    private CompanyRepository companyRepository;

    private FlowableAuthService flowableAuthService;
    private PasswordEncoder passwordEncoder;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        flowableAuthService = Mockito.mock(FlowableAuthService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authenticationService = new AuthenticationService(flowableAuthService, companyRepository, passwordEncoder);
        companyRepository.deleteAll();
    }

    @Test
    @DisplayName("4.1 Login: institutional staff authenticate against the Flowable IDM")
    void testInstitutionalLogin() {
        when(flowableAuthService.authenticates("jefeadministrativo", "password")).thenReturn(true);

        Optional<LoginResponseDTO> result =
                authenticationService.authenticate("jefeadministrativo", "password");

        assertTrue(result.isPresent());
        assertEquals("jefeadministrativo", result.get().username());
        assertEquals(Roles.INSTITUCION, result.get().role());
    }

    @Test
    @DisplayName("4.1 Login: a company authenticates against the institution database")
    void testCompanyLogin() {
        when(flowableAuthService.authenticates(anyString(), anyString())).thenReturn(false);
        saveCompanyWithPassword("acme-sa", "secreto123");

        Optional<LoginResponseDTO> result = authenticationService.authenticate("acme-sa", "secreto123");

        assertTrue(result.isPresent());
        assertEquals(Roles.EMPRESA, result.get().role());
    }

    @Test
    @DisplayName("4.1 Login: a company with the wrong password is rejected")
    void testCompanyWithWrongPassword() {
        when(flowableAuthService.authenticates(anyString(), anyString())).thenReturn(false);
        saveCompanyWithPassword("acme-sa", "secreto123");

        assertTrue(authenticationService.authenticate("acme-sa", "otra").isEmpty());
    }

    @Test
    @DisplayName("4.1 Login: an unknown user is rejected without authentication")
    void testUnknownUser() {
        when(flowableAuthService.authenticates(anyString(), anyString())).thenReturn(false);

        assertTrue(authenticationService.authenticate("nadie", "nada").isEmpty());
    }

    @Test
    @DisplayName("4.1 Login: a company that never got credentials cannot authenticate")
    void testCompanyWithoutAccessCannotLogin() {
        when(flowableAuthService.authenticates(anyString(), anyString())).thenReturn(false);
        companyRepository.save(new Company("ACME SA", "+543581234567", "contacto@acme.com"));

        assertTrue(authenticationService.authenticate("acme-sa", "loquesea").isEmpty());
    }

    private Company saveCompanyWithPassword(String username, String rawPassword) {
        Company company = new Company("ACME SA", "+543581234567", "contacto@acme.com");
        company.enableAccess(username, passwordEncoder.encode(rawPassword));
        return companyRepository.save(company);
    }
}
