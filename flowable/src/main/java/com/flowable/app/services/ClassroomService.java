package com.flowable.app.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Simula el web service de reserva de aulas físicas de la institución.
 * Las aulas y su ocupación se mantienen en memoria: al reiniciar la app quedan todas libres.
 * La cantidad de aulas se configura con la propiedad aulas.disponibles (por defecto 3).
 */
@Service
public class ClassroomService {

    // Aula -> instancia de proceso que la ocupa (null si está libre)
    private final Map<String, String> classrooms = new LinkedHashMap<>();

    public ClassroomService(@Value("${aulas.disponibles:3}") int availableClassrooms) {
        for (int i = 1; i <= availableClassrooms; i++) {
            classrooms.put("AULA-" + i, null);
        }
        System.out.println("Web service de aulas simulado con " + availableClassrooms + " aulas");
    }

    // Reserva la primera aula libre para la instancia. Vacío si están todas ocupadas.
    public synchronized Optional<String> reserve(String processInstanceId) {
        for (Map.Entry<String, String> entry : classrooms.entrySet()) {
            if (entry.getValue() == null) {
                entry.setValue(processInstanceId);
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    // Libera el aula para que la pueda usar otro curso
    public synchronized void release(String classroom) {
        if (classroom != null && classrooms.containsKey(classroom)) {
            classrooms.put(classroom, null);
        }
    }
}
