package com.backend.proxy.service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.proxy.dto.CompanyAccessResponseDTO;
import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.CompanyAccess;
import com.backend.proxy.entity.Course;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.repository.CompanyAccessRepository;
import com.backend.proxy.repository.CourseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessService {

    private static final Logger log = LoggerFactory.getLogger(AccessService.class);
    private static final String PASSWORD_ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int PASSWORD_LENGTH = 12;

    private final CourseRepository courseRepository;
    private final CompanyAccessRepository companyAccessRepository;
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

        Optional<CompanyAccess> existingAccess = companyAccessRepository.findByCompany(company);
        if (existingAccess.isPresent()) {
            String username = existingAccess.get().getUsername();
            log.info("[INFO] Reutilizando cuenta existente para '{}': usuario='{}'", company.getName(), username);
            return new CompanyAccessResponseDTO(username, false);
        }

        String username = buildUsername(company);
        String password = generatePassword();
        CompanyAccess access = new CompanyAccess(company, username, passwordEncoder.encode(password));
        companyAccessRepository.save(access);

        log.info("Cuenta creada para '{}': usuario='{}' | password='{}'", company.getName(), username, password);

        return new CompanyAccessResponseDTO(username, true);
    }

    public boolean validateCredentials(String username, String rawPassword) {
        return companyAccessRepository.findByUsername(username)
                .map(access -> passwordEncoder.matches(rawPassword, access.getPasswordHash()))
                .orElse(false);
    }

    public Optional<CompanyAccess> findByUsername(String username) {
        return companyAccessRepository.findByUsername(username);
    }

    private String buildUsername(Company company) {
        String slug = slugify(company.getName());
        if (slug.isBlank()) {
            slug = "empresa";
        }
        if (companyAccessRepository.existsByUsername(slug)) {
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
