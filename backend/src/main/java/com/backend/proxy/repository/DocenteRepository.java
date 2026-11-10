package com.backend.proxy.repository;

import java.util.List;

import com.backend.proxy.dto.task.DocenteDTO;

/**
 * Repositorio stub de docentes institucionales.
 * Permite consultar el catálogo de docentes disponibles/habilitados para la asignación de cursos.
 */
public interface DocenteRepository {
    List<DocenteDTO> findDocentesHabilitados();
}
