package com.backend.proxy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.proxy.entity.Company;
import com.backend.proxy.entity.CompanyAccess;

public interface CompanyAccessRepository extends JpaRepository<CompanyAccess, Long> {

    Optional<CompanyAccess> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<CompanyAccess> findByCompany(Company company);

    boolean existsByCompany(Company company);
}
