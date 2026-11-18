package com.backend.proxy.service;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.backend.proxy.dto.CompanyAccessResponseDTO;
import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.CompanyAccess;
import com.backend.proxy.entity.Course;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.repository.CompanyAccessRepository;
import com.backend.proxy.repository.CompanyRepository;
import com.backend.proxy.repository.CourseRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class AccessServiceTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyAccessRepository companyAccessRepository;

    @Autowired
    private CourseRepository courseRepository;

    private PasswordEncoder passwordEncoder;
    private AccessService accessService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        accessService = new AccessService(courseRepository, companyAccessRepository, passwordEncoder);
        companyAccessRepository.deleteAll();
        courseRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    @DisplayName("Enable access creates a new account for a company linked to a course")
    void testEnableAccessCreatesNewAccount() {
        Company company = companyRepository.save(new Company("Tech Solutions", "+5411223344", "info@tech.com"));
        Course course = new Course(company, "Spring Boot", "Desc", List.of(LocalDate.now().plusDays(10)));
        course.setProcessInstanceId("proc-100");
        courseRepository.save(course);

        CompanyAccessResponseDTO response = accessService.enableAccess("proc-100");

        assertNotNull(response.username());
        assertTrue(response.created());
        assertTrue(response.username().startsWith("tech-solutions"));

        CompanyAccess access = companyAccessRepository.findByUsername(response.username()).orElseThrow();
        assertEquals(company.getId(), access.getCompany().getId());
        assertNotNull(access.getPasswordHash());
    }

    @Test
    @DisplayName("Enable access reuses existing account if company already has access")
    void testEnableAccessReusesExisting() {
        Company company = companyRepository.save(new Company("Tech Solutions", "+5411223344", "info@tech.com"));
        companyAccessRepository.save(new CompanyAccess(company, "tech-solutions", passwordEncoder.encode("secret123")));

        Course course = new Course(company, "Spring Boot", "Desc", List.of(LocalDate.now().plusDays(10)));
        course.setProcessInstanceId("proc-200");
        courseRepository.save(course);

        CompanyAccessResponseDTO response = accessService.enableAccess("proc-200");

        assertEquals("tech-solutions", response.username());
        assertFalse(response.created());
        assertEquals(1, companyAccessRepository.count());
    }

    @Test
    @DisplayName("Enable access fails if process instance is unknown")
    void testEnableAccessUnknownProcess() {
        assertThrows(ResourceNotFoundException.class, () -> accessService.enableAccess("unknown-proc"));
    }

    @Test
    @DisplayName("Validate credentials returns true for correct credentials and false otherwise")
    void testValidateCredentials() {
        Company company = companyRepository.save(new Company("Globex", "+5411000000", "globex@corp.com"));
        companyAccessRepository.save(new CompanyAccess(company, "globex", passwordEncoder.encode("secretPass")));

        assertTrue(accessService.validateCredentials("globex", "secretPass"));
        assertFalse(accessService.validateCredentials("globex", "wrongPass"));
        assertFalse(accessService.validateCredentials("unknown", "secretPass"));
    }
}
