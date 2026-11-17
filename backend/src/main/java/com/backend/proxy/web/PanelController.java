package com.backend.proxy.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.backend.proxy.security.Roles;

/**
 * Punto de entrada después del login. Según el rol deriva a la vista que corresponde:
 * la empresa a sus cursos (enunciado 4.1.2) y el personal de la institución a su bandeja
 * de tareas (4.1.3 y 4.1.4).
 *
 * <p>Por ahora solo muestra con qué rol entró cada uno; las pantallas de cada rol llegan
 * en las próximas iteraciones del bloque 4.
 */
@Controller
public class PanelController {

    @GetMapping("/panel")
    public String panel(Authentication authentication, Model model) {
        model.addAttribute("usuario", authentication.getName());
        model.addAttribute("esEmpresa", tieneRol(authentication, Roles.ROLE_EMPRESA));
        return "panel/inicio";
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> rol.equals(authority.getAuthority()));
    }
}
