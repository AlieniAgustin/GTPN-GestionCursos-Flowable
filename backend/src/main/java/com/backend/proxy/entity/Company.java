package com.backend.proxy.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "companies")
@Getter
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private CompanyAccess access;

    @OneToMany(mappedBy = "company")
    private List<Course> courses = new ArrayList<>();

    protected Company() {
    }

    public Company(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    public boolean hasAccess() {
        return access != null;
    }

    public String getAccessUsername() {
        return access != null ? access.getUsername() : null;
    }

    public void enableAccess(String username, String passwordHash) {
        this.access = new CompanyAccess(this, username, passwordHash);
    }
}
