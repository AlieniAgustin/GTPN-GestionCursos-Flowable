package com.backend.proxy.web;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.dto.course.ActivityDTO;
import com.backend.proxy.security.Roles;
import com.backend.proxy.service.CourseProgressService;
import com.backend.proxy.service.CourseService;
import com.backend.proxy.web.view.CursoVista;

import lombok.RequiredArgsConstructor;

/**
 * Consulta de cursos desde el sitio (enunciado 4.1.2).
 *
 * <p>Una empresa ve solo los suyos; el personal de la institución los ve todos. El
 * filtrado lo resuelve {@code CourseService}, así que acá no hay lógica de visibilidad
 * duplicada: si el curso no corresponde, el servicio tira 404.
 */
@Controller
@RequiredArgsConstructor
public class CourseSiteController {

    private final CourseService courseService;
    private final CourseProgressService progressService;

    @GetMapping("/mis-cursos")
    public String listado(Authentication authentication, Model model) {
        boolean esEmpresa = Roles.isCompany(authentication);

        List<CourseResponseDTO> cursos =
                courseService.findVisibleFor(authentication.getName(), esEmpresa);

        // Una sola consulta al motor para todos los cursos de la lista
        Map<String, List<ActivityDTO>> actividades = progressService.findActiveActivities();

        model.addAttribute("esEmpresa", esEmpresa);
        model.addAttribute("cursos", cursos.stream().map(curso -> toVista(curso, actividades)).toList());
        return "cursos/listado";
    }

    @GetMapping("/mis-cursos/{courseId}")
    public String detalle(
            @PathVariable Long courseId,
            Authentication authentication,
            Model model
    ) {
        boolean esEmpresa = Roles.isCompany(authentication);

        CourseResponseDTO curso =
                courseService.findOneFor(courseId, authentication.getName(), esEmpresa);

        model.addAttribute("esEmpresa", esEmpresa);
        model.addAttribute("vista", toVista(curso, progressService.findActiveActivities()));
        return "cursos/detalle";
    }

    private CursoVista toVista(CourseResponseDTO curso, Map<String, List<ActivityDTO>> porInstancia) {
        List<ActivityDTO> activas = curso.processInstanceId() != null
                ? porInstancia.getOrDefault(curso.processInstanceId(), List.of())
                : List.of();

        return new CursoVista(
                curso,
                progressService.resolveStage(curso.status(), activas),
                progressService.visibleActivities(activas));
    }
}
