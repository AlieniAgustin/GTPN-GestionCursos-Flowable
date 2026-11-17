package com.backend.proxy.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.backend.proxy.dto.CompanyContactDTO;
import com.backend.proxy.dto.CourseRequestDTO;
import com.backend.proxy.dto.CourseResponseDTO;
import com.backend.proxy.service.CourseService;
import com.backend.proxy.web.form.SolicitudForm;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Sección pública del sitio institucional (enunciado 4.1.1): cualquier empresa puede
 * pedir una capacitación sin tener cuenta. Al enviar el formulario se registra la
 * solicitud en la BD de la institución y se inicia la instancia del proceso.
 */
@Controller
@RequiredArgsConstructor
public class PublicSiteController {

    private static final String VISTA_INICIO = "publico/inicio";

    private final CourseService courseService;

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("solicitud", new SolicitudForm());
        return VISTA_INICIO;
    }

    @GetMapping("/login")
    public String login() {
        return "publico/login";
    }

    @PostMapping("/solicitudes")
    public String registrarSolicitud(
            @Valid @ModelAttribute("solicitud") SolicitudForm solicitud,
            BindingResult errores,
            Model model,
            RedirectAttributes redireccion
    ) {
        List<LocalDate> fechas = List.of();

        // Las fechas se parsean aparte porque vienen como texto libre
        if (!errores.hasFieldErrors("preferredDates")) {
            try {
                fechas = solicitud.parsedDates();
                rejectPastDates(fechas, errores);
            } catch (SolicitudForm.DateFormatException e) {
                errores.rejectValue("preferredDates", "formato",
                        "No se entendió la fecha '" + e.getMessage() + "'. Usá el formato AAAA-MM-DD");
            }
        }

        if (errores.hasErrors()) {
            return VISTA_INICIO;
        }

        CourseResponseDTO curso;
        try {
            curso = courseService.processCourse(toRequest(solicitud, fechas));
        } catch (Exception e) {
            // El motor o la base no respondieron: se avisa sin perder lo que cargó el usuario
            model.addAttribute("errorGeneral",
                    "No pudimos registrar la solicitud en este momento. Probá de nuevo en unos minutos.");
            return VISTA_INICIO;
        }

        redireccion.addFlashAttribute("curso", curso);
        return "redirect:/solicitudes/enviada";
    }

    @GetMapping("/solicitudes/enviada")
    public String solicitudEnviada(Model model) {
        // Sin el flash attribute no hay nada que mostrar: pasa si recargan la página
        if (!model.containsAttribute("curso")) {
            return "redirect:/";
        }
        return "publico/enviada";
    }

    private void rejectPastDates(List<LocalDate> fechas, BindingResult errores) {
        LocalDate hoy = LocalDate.now();
        boolean hayPasadas = fechas.stream().anyMatch(fecha -> !fecha.isAfter(hoy));

        if (hayPasadas) {
            errores.rejectValue("preferredDates", "pasado", "Las fechas tienen que ser futuras");
        }
    }

    private CourseRequestDTO toRequest(SolicitudForm solicitud, List<LocalDate> fechas) {
        return new CourseRequestDTO(
                new CompanyContactDTO(
                        solicitud.getCompanyName(),
                        solicitud.getCompanyEmail(),
                        solicitud.getCompanyPhone()),
                solicitud.getName(),
                solicitud.getDescription(),
                solicitud.getParticipantCount(),
                fechas);
    }
}
