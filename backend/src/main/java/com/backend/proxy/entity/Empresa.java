package com.backend.proxy.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
@Entity 
public class Empresa {

    @Id 
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    String nombre;
    String email;   
    String telefono;
    @OneToMany(mappedBy = "empresa")
    private List<Curso> cursos = new ArrayList<>();
}
