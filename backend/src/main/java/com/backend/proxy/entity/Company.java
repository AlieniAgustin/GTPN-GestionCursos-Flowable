package com.backend.proxy.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    /**
     * Credenciales de acceso de la empresa al sitio (enunciado 4.1.2). Las crea la
     * actividad automatica "Habilitar acceso de la empresa" la primera vez que la
     * empresa contrata un curso; en contrataciones posteriores se reutilizan.
     */
    @Column(name = "access_username", unique = true)
    private String accessUsername;

    @Column(name = "access_password_hash")
    private String accessPasswordHash;

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
        return accessUsername != null && accessPasswordHash != null;
    }

    public void enableAccess(String username, String passwordHash) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Company access username cannot be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Company access password cannot be blank");
        }
        this.accessUsername = username;
        this.accessPasswordHash = passwordHash;
    }
}
