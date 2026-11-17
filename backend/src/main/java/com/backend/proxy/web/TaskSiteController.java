package com.backend.proxy.web;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.exception.TaskAlreadyClaimedException;
import com.backend.proxy.exception.TaskNotClaimedException;
import com.backend.proxy.service.CourseService;
import com.backend.proxy.service.TaskService;
import com.backend.proxy.types.TaskCatalog;
import com.backend.proxy.web.view.TareaVista;

import lombok.RequiredArgsConstructor;

/**
 * Bandeja de tareas del personal de la institución (enunciado 4.1.3 y 4.1.4).
 *
 * <p>Muestra separadas las tareas que el usuario tiene asignadas de las que puede
 * reclamar por su grupo, porque el enunciado trata los dos casos distinto: las de grupo
 * hay que reclamarlas antes de completarlas y las asignadas directamente no.
 */
@Controller
@RequiredArgsConstructor
public class TaskSiteController {

    private final TaskService taskService;
    private final CourseService courseService;

    @GetMapping("/tareas")
    public String bandeja(Authentication authentication, Model model) {
        String usuario = authentication.getName();
        List<FlowableTaskDTO> tareas = taskService.findInbox(usuario);

        // Una sola consulta de cursos para poder decir a qué capacitación pertenece cada tarea
        Map<String, String> cursosPorInstancia = courseService.findVisibleFor(usuario, false).stream()
                .filter(curso -> curso.processInstanceId() != null)
                .collect(Collectors.toMap(
                        CourseResponseDTO::processInstanceId,
                        CourseResponseDTO::name,
                        (uno, otro) -> uno));

        Function<FlowableTaskDTO, TareaVista> aVista = tarea -> new TareaVista(
                tarea, cursosPorInstancia.get(tarea.processInstanceId()));

        model.addAttribute("mias", tareas.stream()
                .filter(tarea -> usuario.equals(tarea.assignee()))
                .map(aVista)
                .toList());

        model.addAttribute("delGrupo", tareas.stream()
                .filter(tarea -> tarea.assignee() == null || tarea.assignee().isBlank())
                .map(aVista)
                .toList());

        return "tareas/bandeja";
    }

    @PostMapping("/tareas/{taskId}/reclamar")
    public String reclamar(
            @PathVariable String taskId,
            Authentication authentication,
            RedirectAttributes redireccion
    ) {
        try {
            taskService.claim(taskId, authentication.getName());
            redireccion.addFlashAttribute("mensaje", "Reclamaste la tarea.");
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e, "No se pudo reclamar la tarea."));
        }
        return "redirect:/tareas";
    }

    @PostMapping("/tareas/{taskId}/completar")
    public String completar(
            @PathVariable String taskId,
            Authentication authentication,
            RedirectAttributes redireccion
    ) {
        try {
            String key = taskService.findTaskDefinitionKey(taskId, authentication.getName());

            if (!TaskCatalog.seCompletaDirecto(key)) {
                redireccion.addFlashAttribute("error",
                        "Esa tarea necesita cargar datos: completala desde Flowable UI por ahora.");
                return "redirect:/tareas";
            }

            taskService.complete(taskId, TaskCatalog.variablesDe(key), authentication.getName());
            redireccion.addFlashAttribute("mensaje", "Tarea completada.");
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e, "No se pudo completar la tarea."));
        }
        return "redirect:/tareas";
    }

    /**
     * Las excepciones de reclamo traen un mensaje escrito para el usuario y se muestran
     * tal cual. Las demás se traducen: la de "no visible" lleva el id de la tarea y un
     * texto en inglés pensado para la API, que no tiene sentido mostrar en el sitio.
     */
    private String mensajeDe(Exception e, String porDefecto) {
        if (e instanceof TaskNotClaimedException || e instanceof TaskAlreadyClaimedException) {
            return e.getMessage();
        }
        if (e instanceof ResourceNotFoundException) {
            return "Esa tarea ya no está disponible para vos.";
        }
        return porDefecto;
    }
}
