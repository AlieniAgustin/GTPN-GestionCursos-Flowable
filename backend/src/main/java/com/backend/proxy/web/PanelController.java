package com.backend.proxy.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.backend.proxy.security.Roles;

/**
 * Punto de entrada después del login: deriva a cada rol a su pantalla. La empresa va a
 * sus cursos (enunciado 4.1.2) y el personal de la institución a su bandeja de tareas
 * (4.1.3 y 4.1.4).
 */
@Controller
public class PanelController {

    @GetMapping("/panel")
    public String panel(Authentication authentication) {
        // La empresa no tiene más pantallas que sus cursos: se va derecho ahí
        if (Roles.isCompany(authentication)) {
            return "redirect:/mis-cursos";
        }

        return "redirect:/tareas";
    }
}
