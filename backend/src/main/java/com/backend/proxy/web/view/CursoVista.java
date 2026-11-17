package com.backend.proxy.web.view;

import java.util.List;

import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.dto.course.ActivityDTO;
import com.backend.proxy.types.CourseStage;

/**
 * Lo que el sitio necesita para mostrar un curso: los datos que están en la base más el
 * estado y las actividades en curso, que salen del motor (enunciado 4.1.2).
 */
public record CursoVista(
    CourseResponseDTO curso,
    CourseStage estado,
    List<ActivityDTO> actividades
) {

    /**
     * Solo tiene sentido listar las actividades mientras el curso sigue en marcha.
     */
    public boolean tieneActividades() {
        return !estado.isClosed() && !actividades.isEmpty();
    }
}
