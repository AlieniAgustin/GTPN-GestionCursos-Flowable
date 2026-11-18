package com.backend.proxy.web;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.exception.TaskAlreadyClaimedException;
import com.backend.proxy.exception.TaskNotClaimedException;
import com.backend.proxy.repository.TeacherRepository;
import com.backend.proxy.service.CourseService;
import com.backend.proxy.service.TaskService;

import lombok.RequiredArgsConstructor;

/**
 * Pantallas del sitio para las tareas que tienen que producir variables del proceso
 * (enunciado 4.1.3 y 4.1.4): <em>Evaluar recursos</em>, <em>Definir docente</em>,
 * <em>Elaborar informe de resultados</em> y <em>Enviar informe a empresa y archivar
 * expediente</em>.
 *
 * <p>Cada pantalla tiene un GET que muestra el formulario y un POST que lo recibe.
 * Los dos primero verifican que la tarea sea del tipo esperado y que el usuario la
 * pueda ver, así que una URL armada a mano con otra tarea no sirve para completarla.
 */
@Controller
@RequiredArgsConstructor
public class TaskFormSiteController {

    private static final String EVALUAR_RECURSOS = "evaluarrecursos";
    private static final String DEFINIR_DOCENTE = "definirdocentequedesarrollaelcurso";
    private static final String ELABORAR_INFORME = "elaborarinformederesultados";
    private static final String ENVIAR_INFORME = "enviarinformeaempresayarchivarexpediente";
    private static final String TAREA_EQUIVOCADA = "Esa pantalla no corresponde a la tarea elegida.";

    private final TaskService taskService;
    private final CourseService courseService;
    private final TeacherRepository teacherRepository;

    // ---------------------------------------------------------------- Evaluar recursos

    @GetMapping("/tareas/{taskId}/evaluar-recursos")
    public String formularioRecursos(@PathVariable String taskId, Authentication authentication,
                                     Model model, RedirectAttributes redireccion) {
        String usuario = authentication.getName();
        try {
            if (!esDelTipo(taskId, usuario, EVALUAR_RECURSOS)) {
                redireccion.addFlashAttribute("error", TAREA_EQUIVOCADA);
                return "redirect:/tareas";
            }
            prepararRecursos(model, taskId, usuario);
            return "tareas/evaluar-recursos";
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e));
            return "redirect:/tareas";
        }
    }

    /**
     * Completa <em>Evaluar recursos</em>. El gateway inclusivo del modelo evalúa
     * {@code ${requiereAula}}, {@code ${requierePlataforma}} y
     * {@code ${requiereEquipamiento}}, así que las tres variables viajan siempre, aunque
     * sean {@code false}: si faltara una, el motor fallaría con "Unknown property".
     * Una casilla sin marcar no llega en el POST, por eso el default es {@code false}.
     */
    @PostMapping("/tareas/{taskId}/evaluar-recursos")
    public String completarRecursos(@PathVariable String taskId,
                                    @RequestParam(defaultValue = "false") boolean requiereAula,
                                    @RequestParam(defaultValue = "false") boolean requierePlataforma,
                                    @RequestParam(defaultValue = "false") boolean requiereEquipamiento,
                                    Authentication authentication, Model model,
                                    RedirectAttributes redireccion) {
        String usuario = authentication.getName();
        try {
            if (!esDelTipo(taskId, usuario, EVALUAR_RECURSOS)) {
                redireccion.addFlashAttribute("error", TAREA_EQUIVOCADA);
                return "redirect:/tareas";
            }

            if (!requiereAula && !requierePlataforma && !requiereEquipamiento) {
                prepararRecursos(model, taskId, usuario);
                model.addAttribute("error", "Elegí al menos un recurso para el curso.");
                return "tareas/evaluar-recursos";
            }

            taskService.complete(taskId, Map.of(
                    "requiereAula", requiereAula,
                    "requierePlataforma", requierePlataforma,
                    "requiereEquipamiento", requiereEquipamiento), usuario);
            redireccion.addFlashAttribute("mensaje", "Recursos definidos. La tarea quedó completada.");
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e));
        }
        return "redirect:/tareas";
    }

    private void prepararRecursos(Model model, String taskId, String usuario) {
        model.addAttribute("taskId", taskId);
        model.addAttribute("curso", cursoDe(taskId, usuario));
    }

    // ----------------------------------------------------------------- Definir docente

    @GetMapping("/tareas/{taskId}/definir-docente")
    public String formularioDocente(@PathVariable String taskId, Authentication authentication,
                                    Model model, RedirectAttributes redireccion) {
        String usuario = authentication.getName();
        try {
            if (!esDelTipo(taskId, usuario, DEFINIR_DOCENTE)) {
                redireccion.addFlashAttribute("error", TAREA_EQUIVOCADA);
                return "redirect:/tareas";
            }
            prepararDocente(model, taskId, usuario);
            return "tareas/definir-docente";
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e));
            return "redirect:/tareas";
        }
    }

    /**
     * Completa <em>Definir docente</em> con {@code docenteSeleccionado}, que el modelo
     * usa como responsable de <em>Desarrollar curso</em>. Solo se acepta alguien del
     * equipo docente disponible.
     */
    @PostMapping("/tareas/{taskId}/definir-docente")
    public String completarDocente(@PathVariable String taskId,
                                   @RequestParam(required = false) String userId,
                                   Authentication authentication, Model model,
                                   RedirectAttributes redireccion) {
        String usuario = authentication.getName();
        try {
            if (!esDelTipo(taskId, usuario, DEFINIR_DOCENTE)) {
                redireccion.addFlashAttribute("error", TAREA_EQUIVOCADA);
                return "redirect:/tareas";
            }

            boolean esDocente = teacherRepository.findAvailableTeachers().stream()
                    .anyMatch(docente -> docente.userId().equals(userId));
            if (!esDocente) {
                prepararDocente(model, taskId, usuario);
                model.addAttribute("error", "Elegí un docente de la lista.");
                return "tareas/definir-docente";
            }

            taskService.complete(taskId, Map.of("docenteSeleccionado", userId), usuario);
            redireccion.addFlashAttribute("mensaje", "Docente asignado: " + userId + ".");
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e));
        }
        return "redirect:/tareas";
    }

    private void prepararDocente(Model model, String taskId, String usuario) {
        model.addAttribute("taskId", taskId);
        model.addAttribute("curso", cursoDe(taskId, usuario));
        model.addAttribute("docentes", teacherRepository.findAvailableTeachers());
    }

    // ------------------------------------------------------------- Elaborar informe

    @GetMapping("/tareas/{taskId}/elaborar-informe")
    public String formularioInforme(@PathVariable String taskId, Authentication authentication,
                                    Model model, RedirectAttributes redireccion) {
        String usuario = authentication.getName();
        try {
            if (!esDelTipo(taskId, usuario, ELABORAR_INFORME)) {
                redireccion.addFlashAttribute("error", TAREA_EQUIVOCADA);
                return "redirect:/tareas";
            }
            prepararInforme(model, taskId, usuario, informeDe(taskId, usuario));
            return "tareas/elaborar-informe";
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e));
            return "redirect:/tareas";
        }
    }

    /**
     * Completa <em>Elaborar informe de resultados</em> con la variable {@code informe},
     * que después lee el jefe administrativo para enviarlo a la empresa.
     */
    @PostMapping("/tareas/{taskId}/elaborar-informe")
    public String completarInforme(@PathVariable String taskId,
                                   @RequestParam(required = false) String informe,
                                   Authentication authentication, Model model,
                                   RedirectAttributes redireccion) {
        String usuario = authentication.getName();
        try {
            if (!esDelTipo(taskId, usuario, ELABORAR_INFORME)) {
                redireccion.addFlashAttribute("error", TAREA_EQUIVOCADA);
                return "redirect:/tareas";
            }

            if (informe == null || informe.isBlank()) {
                prepararInforme(model, taskId, usuario, informe);
                model.addAttribute("error", "Escribí el informe de resultados del curso.");
                return "tareas/elaborar-informe";
            }

            taskService.complete(taskId, Map.of("informe", informe.trim()), usuario);
            redireccion.addFlashAttribute("mensaje",
                    "Informe elaborado. Pasa al jefe administrativo para su envío.");
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e));
        }
        return "redirect:/tareas";
    }

    private void prepararInforme(Model model, String taskId, String usuario, String informe) {
        model.addAttribute("taskId", taskId);
        model.addAttribute("curso", cursoDe(taskId, usuario));
        model.addAttribute("informe", informe);
    }

    // ---------------------------------------------------------------- Enviar informe

    @GetMapping("/tareas/{taskId}/enviar-informe")
    public String formularioEnvio(@PathVariable String taskId, Authentication authentication,
                                  Model model, RedirectAttributes redireccion) {
        String usuario = authentication.getName();
        try {
            if (!esDelTipo(taskId, usuario, ENVIAR_INFORME)) {
                redireccion.addFlashAttribute("error", TAREA_EQUIVOCADA);
                return "redirect:/tareas";
            }
            prepararEnvio(model, taskId, usuario);
            return "tareas/enviar-informe";
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e));
            return "redirect:/tareas";
        }
    }

    /**
     * Completa <em>Enviar informe a empresa y archivar expediente</em>. Es el último paso
     * del camino normal: al completarse el modelo dispara la compensación que libera los
     * recursos reservados y la instancia termina como FINALIZADO.
     *
     * <p>Pide una confirmación explícita porque la acción no tiene vuelta atrás.
     */
    @PostMapping("/tareas/{taskId}/enviar-informe")
    public String completarEnvio(@PathVariable String taskId,
                                 @RequestParam(defaultValue = "false") boolean confirmar,
                                 Authentication authentication, Model model,
                                 RedirectAttributes redireccion) {
        String usuario = authentication.getName();
        try {
            if (!esDelTipo(taskId, usuario, ENVIAR_INFORME)) {
                redireccion.addFlashAttribute("error", TAREA_EQUIVOCADA);
                return "redirect:/tareas";
            }

            if (!confirmar) {
                prepararEnvio(model, taskId, usuario);
                model.addAttribute("error",
                        "Confirmá el envío del informe para cerrar el curso.");
                return "tareas/enviar-informe";
            }

            taskService.complete(taskId, Map.of(
                    "informeEnviado", true,
                    "expedienteArchivado", true), usuario);
            redireccion.addFlashAttribute("mensaje",
                    "Informe enviado y expediente archivado. El curso quedó finalizado.");
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e));
        }
        return "redirect:/tareas";
    }

    private void prepararEnvio(Model model, String taskId, String usuario) {
        model.addAttribute("taskId", taskId);
        model.addAttribute("curso", cursoDe(taskId, usuario));
        model.addAttribute("informe", informeDe(taskId, usuario));
    }

    /**
     * El informe se lee de la variable del proceso, que es donde lo dejó la tarea
     * anterior: es la fuente de verdad mientras la instancia está viva, y no depende de
     * que la sincronización con la base de la institución haya llegado.
     */
    private String informeDe(String taskId, String usuario) {
        Object valor = taskService.findVariablesOfTask(taskId, usuario).get("informe");
        return valor != null && !valor.toString().isBlank() ? valor.toString() : null;
    }

    // ------------------------------------------------------------------------ Comunes

    /**
     * ¿La tarea es del tipo que atiende esta pantalla? {@code findTaskDefinitionKey}
     * además falla si el usuario no puede ver la tarea.
     */
    private boolean esDelTipo(String taskId, String usuario, String tipoEsperado) {
        return tipoEsperado.equals(taskService.findTaskDefinitionKey(taskId, usuario));
    }

    /**
     * Nombre del curso, solo para mostrarlo en el formulario: busca la tarea en la
     * bandeja del usuario para saber su instancia de proceso, y después el curso que
     * tiene esa instancia. Si no lo encuentra devuelve {@code null} y la pantalla se
     * muestra igual, sin el nombre.
     */
    private String cursoDe(String taskId, String usuario) {
        String instancia = null;
        for (FlowableTaskDTO tarea : taskService.findInbox(usuario)) {
            if (taskId.equals(tarea.id())) {
                instancia = tarea.processInstanceId();
            }
        }
        if (instancia == null) {
            return null;
        }
        for (CourseResponseDTO curso : courseService.findVisibleFor(usuario, false)) {
            if (instancia.equals(curso.processInstanceId())) {
                return curso.name();
            }
        }
        return null;
    }

    /**
     * Mismo criterio que la bandeja: los mensajes de reclamo están escritos para el
     * usuario y se muestran tal cual; los demás se traducen.
     */
    private String mensajeDe(Exception e) {
        if (e instanceof TaskNotClaimedException || e instanceof TaskAlreadyClaimedException) {
            return e.getMessage();
        }
        if (e instanceof ResourceNotFoundException) {
            return "Esa tarea ya no está disponible para vos.";
        }
        return "No se pudo completar la tarea.";
    }
}
