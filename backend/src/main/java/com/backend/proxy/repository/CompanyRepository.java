package com.backend.proxy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.backend.proxy.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);

    Optional<Company> findByAccessUsername(String accessUsername);

    boolean existsByAccessUsername(String accessUsername);
}
