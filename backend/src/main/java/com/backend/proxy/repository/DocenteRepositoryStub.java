package com.backend.proxy.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.backend.proxy.dto.task.DocenteDTO;

/**
 * Implementación stub en memoria de {@link DocenteRepository} para pruebas y desacople.
 */
@Repository
public class DocenteRepositoryStub implements DocenteRepository {

    @Override
    public List<DocenteDTO> findDocentesHabilitados() {
        /*
         * PASOS A IMPLEMENTAR:
         * 1. Consultar en la base de datos institucional o servicio de recursos humanos
         *    a los docentes que integran el equipo pedagógico capacitado.
         * 2. Retornar la lista proyectada al DTO DocenteDTO(userId, name).
         */
        return List.of();
    }
}
