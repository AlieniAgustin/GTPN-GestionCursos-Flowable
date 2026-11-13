package com.backend.proxy.service;

import java.security.SecureRandom;
import java.text.Normalizer;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.proxy.dto.CompanyAccessResponseDTO;
import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.Course;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.repository.CompanyRepository;
import com.backend.proxy.repository.CourseRepository;

import lombok.RequiredArgsConstructor;

/**
 * Habilita el acceso de la empresa al sitio (enunciado 4.1.2).
 *
 * <p>La llama la actividad automatica del proceso, por la API interna, una vez que la
 * empresa acepto la propuesta y antes de empezar la preparacion. Crea la cuenta si la
 * empresa todavia no tiene una, o reutiliza la que ya quedo de una contratacion
 * anterior. El curso queda vinculado a esa cuenta por su empresa.
 */
@Service
@RequiredArgsConstructor
public class CompanyAccessService {

    private static final String PASSWORD_ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int PASSWORD_LENGTH = 12;

    private final CourseRepository courseRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public CompanyAccessResponseDTO enableAccess(String processInstanceId) {
        if (processInstanceId == null || processInstanceId.isBlank()) {
            throw new IllegalArgumentException("Process instance ID must not be blank");
        }

        Course course = courseRepository.findByprocessInstanceId(processInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No course linked to process instance " + processInstanceId));
        Company company = course.getCompany();

        // Ya contrato antes: se reutiliza la cuenta y el curso nuevo queda vinculado a ella
        if (company.hasAccess()) {
            return new CompanyAccessResponseDTO(
                    company.getId(), company.getName(), company.getAccessUsername(), null, false);
        }

        String username = buildUsername(company);
        String password = generatePassword();
        company.enableAccess(username, passwordEncoder.encode(password));
        companyRepository.save(company);

        return new CompanyAccessResponseDTO(
                company.getId(), company.getName(), username, password, true);
    }

    /**
     * Usuario derivado de la razon social. Si ya lo tomo otra empresa, se desambigua
     * con el id, que es unico.
     */
    private String buildUsername(Company company) {
        String slug = slugify(company.getName());
        if (slug.isBlank()) {
            slug = "empresa";
        }
        if (companyRepository.existsByAccessUsername(slug)) {
            return slug + "-" + company.getId();
        }
        return slug;
    }

    private String slugify(String value) {
        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String generatePassword() {
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }
}
