package com.backend.proxy.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.backend.proxy.dto.task.FlowableTaskDTO;
import com.backend.proxy.exception.ResourceNotFoundException;
import com.backend.proxy.exception.TaskAlreadyClaimedException;
import com.backend.proxy.exception.TaskNotClaimedException;
import com.backend.proxy.security.Roles;
import com.backend.proxy.service.CourseService;
import com.backend.proxy.service.ProposalService;
import com.backend.proxy.service.TaskService;

import lombok.RequiredArgsConstructor;

/**
 * Pantallas de <em>Registrar y elaborar propuesta</em> y <em>Revisar propuesta</em>, y
 * descarga de los PDF de la propuesta (enunciado 4.1.3).
 *
 * <p>Las dos tareas usan el mismo formulario: el GET lo muestra y el POST valida, genera
 * el PDF de la versión siguiente y completa la tarea. Revisar además muestra las
 * observaciones de la empresa y arranca con los datos de la última versión.
 */
@Controller
@RequiredArgsConstructor
public class ProposalSiteController {

    private static final String ELABORAR_PROPUESTA = "registraryelaborarpropuesta";
    private static final String REVISAR_PROPUESTA = "revisarpropuesta";
    private static final String TAREA_EQUIVOCADA = "Esa pantalla no corresponde a la tarea elegida.";

    private final TaskService taskService;
    private final ProposalService proposalService;
    private final CourseService courseService;

    // ------------------------------------------------------------- Elaborar propuesta

    @GetMapping("/tareas/{taskId}/elaborar-propuesta")
    public String formularioElaborar(@PathVariable String taskId, Authentication authentication,
                                     Model model, RedirectAttributes redireccion) {
        return mostrar(taskId, ELABORAR_PROPUESTA, authentication.getName(), model, redireccion);
    }

    @PostMapping("/tareas/{taskId}/elaborar-propuesta")
    public String enviarElaborar(@PathVariable String taskId,
                                 @RequestParam(defaultValue = "") String programa,
                                 @RequestParam(defaultValue = "") String presupuesto,
                                 @RequestParam(defaultValue = "") String fechas,
                                 Authentication authentication, Model model,
                                 RedirectAttributes redireccion) {
        return enviar(taskId, ELABORAR_PROPUESTA, programa, presupuesto, fechas,
                authentication.getName(), model, redireccion);
    }

    // --------------------------------------------------------------- Revisar propuesta

    @GetMapping("/tareas/{taskId}/revisar-propuesta")
    public String formularioRevisar(@PathVariable String taskId, Authentication authentication,
                                    Model model, RedirectAttributes redireccion) {
        return mostrar(taskId, REVISAR_PROPUESTA, authentication.getName(), model, redireccion);
    }

    @PostMapping("/tareas/{taskId}/revisar-propuesta")
    public String enviarRevisar(@PathVariable String taskId,
                                @RequestParam(defaultValue = "") String programa,
                                @RequestParam(defaultValue = "") String presupuesto,
                                @RequestParam(defaultValue = "") String fechas,
                                Authentication authentication, Model model,
                                RedirectAttributes redireccion) {
        return enviar(taskId, REVISAR_PROPUESTA, programa, presupuesto, fechas,
                authentication.getName(), model, redireccion);
    }

    // ------------------------------------------------- Lógica común a las dos pantallas

    /**
     * Muestra el formulario. Al elaborar arranca con las fechas que pidió la empresa; al
     * revisar arranca con lo que tenía la última versión, para cambiar solo lo pedido.
     */
    private String mostrar(String taskId, String tipo, String usuario, Model model,
                           RedirectAttributes redireccion) {
        try {
            if (!tipo.equals(taskService.findTaskDefinitionKey(taskId, usuario))) {
                redireccion.addFlashAttribute("error", TAREA_EQUIVOCADA);
                return "redirect:/tareas";
            }
            ProposalService.Contexto contexto = proposalService.contextoDe(instanciaDe(taskId, usuario));

            String programa = "";
            String presupuesto = "";
            List<LocalDate> fechas = contexto.fechasSolicitadas();

            Optional<ProposalService.Version> anterior = proposalService.ultimaVersion(contexto.courseId());
            if (tipo.equals(REVISAR_PROPUESTA) && anterior.isPresent()) {
                programa = anterior.get().programa();
                presupuesto = anterior.get().presupuesto().toPlainString();
                fechas = anterior.get().fechas();
            }

            preparar(model, taskId, tipo, usuario, contexto, programa, presupuesto, unirFechas(fechas), Map.of());
            return "tareas/elaborar-propuesta";
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e));
            return "redirect:/tareas";
        }
    }

    /** Valida, genera el PDF de la versión siguiente y completa la tarea. */
    private String enviar(String taskId, String tipo, String programa, String presupuesto,
                          String fechas, String usuario, Model model, RedirectAttributes redireccion) {
        try {
            if (!tipo.equals(taskService.findTaskDefinitionKey(taskId, usuario))) {
                redireccion.addFlashAttribute("error", TAREA_EQUIVOCADA);
                return "redirect:/tareas";
            }
            String instancia = instanciaDe(taskId, usuario);
            ProposalService.Contexto contexto = proposalService.contextoDe(instancia);

            // Validación campo por campo, para mostrar cada error al lado de su campo
            Map<String, String> errores = new LinkedHashMap<>();
            if (programa.isBlank()) {
                errores.put("programa", "Escribí el programa del curso.");
            }
            BigDecimal monto = leerPresupuesto(presupuesto, errores);
            List<LocalDate> listaFechas = leerFechas(fechas, errores);

            if (!errores.isEmpty()) {
                preparar(model, taskId, tipo, usuario, contexto, programa, presupuesto, fechas, errores);
                return "tareas/elaborar-propuesta";
            }

            int version = proposalService.enviarPropuesta(
                    taskId, instancia, programa.trim(), monto, listaFechas, usuario);

            redireccion.addFlashAttribute("mensaje", version == 1
                    ? "Propuesta enviada a la empresa (versión 1)."
                    : "Nueva versión de la propuesta enviada a la empresa (versión " + version + ").");
            redireccion.addFlashAttribute("enlace",
                    "/mis-cursos/" + contexto.courseId() + "/propuestas/" + version + ".pdf");
            redireccion.addFlashAttribute("enlaceTexto", "Ver el PDF");
        } catch (Exception e) {
            redireccion.addFlashAttribute("error", mensajeDe(e));
        }
        return "redirect:/tareas";
    }

    private void preparar(Model model, String taskId, String tipo, String usuario,
                          ProposalService.Contexto contexto, String programa, String presupuesto,
                          String fechas, Map<String, String> errores) {
        boolean revisar = tipo.equals(REVISAR_PROPUESTA);
        model.addAttribute("taskId", taskId);
        model.addAttribute("revisar", revisar);
        model.addAttribute("accion", revisar ? "revisar-propuesta" : "elaborar-propuesta");
        model.addAttribute("observaciones", revisar ? observacionesDe(taskId, usuario) : null);
        model.addAttribute("contexto", contexto);
        model.addAttribute("programa", programa);
        model.addAttribute("presupuesto", presupuesto);
        model.addAttribute("fechas", fechas);
        model.addAttribute("errores", errores);
    }

    /** Lo que escribió la empresa al pedir modificaciones (app de comunicaciones). */
    private String observacionesDe(String taskId, String usuario) {
        Object valor = taskService.findVariablesOfTask(taskId, usuario).get("observaciones");
        return valor == null || valor.toString().isBlank() ? null : valor.toString();
    }

    private String unirFechas(List<LocalDate> fechas) {
        return fechas.stream().map(LocalDate::toString).collect(Collectors.joining(", "));
    }

    // ---------------------------------------------------------------- Descarga del PDF

    /**
     * Descarga una versión de la propuesta. Usa la misma visibilidad que el detalle del
     * curso: una empresa solo baja las de sus cursos, y si no es suyo recibe 404.
     */
    @GetMapping("/mis-cursos/{courseId}/propuestas/{version}.pdf")
    public ResponseEntity<byte[]> descargar(@PathVariable Long courseId, @PathVariable int version,
                                            Authentication authentication) {
        try {
            courseService.findOneFor(courseId, authentication.getName(), Roles.isCompany(authentication));
            byte[] pdf = proposalService.pdfDe(courseId, version);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                            .filename("propuesta-curso-" + courseId + "-v" + version + ".pdf")
                            .build().toString())
                    .body(pdf);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ------------------------------------------------------------------------ Comunes

    /** Instancia de proceso de la tarea, buscándola en la bandeja del usuario. */
    private String instanciaDe(String taskId, String usuario) {
        for (FlowableTaskDTO tarea : taskService.findInbox(usuario)) {
            if (taskId.equals(tarea.id()) && tarea.processInstanceId() != null) {
                return tarea.processInstanceId();
            }
        }
        throw new ResourceNotFoundException("Task " + taskId + " is not in the inbox of " + usuario);
    }

    private BigDecimal leerPresupuesto(String texto, Map<String, String> errores) {
        try {
            BigDecimal monto = new BigDecimal(texto.trim().replace(",", "."));
            if (monto.signum() <= 0) {
                errores.put("presupuesto", "El presupuesto tiene que ser mayor que cero.");
            }
            return monto;
        } catch (NumberFormatException e) {
            errores.put("presupuesto", "Escribí el presupuesto como número, por ejemplo 180000.");
            return null;
        }
    }

    private List<LocalDate> leerFechas(String texto, Map<String, String> errores) {
        List<LocalDate> fechas = new ArrayList<>();
        for (String parte : texto.split(",")) {
            String limpia = parte.trim();
            if (limpia.isEmpty()) {
                continue;
            }
            try {
                fechas.add(LocalDate.parse(limpia));
            } catch (DateTimeParseException e) {
                errores.put("fechas", "No se entendió la fecha '" + limpia + "'. Usá el formato 2027-03-01.");
                return fechas;
            }
        }
        if (fechas.isEmpty()) {
            errores.put("fechas", "Indicá al menos una fecha posible.");
        } else if (fechas.stream().anyMatch(fecha -> !fecha.isAfter(LocalDate.now()))) {
            errores.put("fechas", "Las fechas tienen que ser futuras.");
        }
        return fechas;
    }

    /** Mismo criterio que la bandeja para traducir los errores. */
    private String mensajeDe(Exception e) {
        if (e instanceof TaskNotClaimedException || e instanceof TaskAlreadyClaimedException) {
            return e.getMessage();
        }
        if (e instanceof ResourceNotFoundException) {
            return "Esa tarea ya no está disponible para vos.";
        }
        return "No se pudo enviar la propuesta.";
    }
}
